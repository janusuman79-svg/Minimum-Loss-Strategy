"""
========================================================================================
Telegram User-Bot & NLP Parsing Microservice (Telethon + FastAPI)
========================================================================================
Monitors Telegram alert channels, extracts unstructured trading calls across 210+
NSE F&O stocks, validates symbols against the F&O master list, normalizes into structured
JSON with product_type (MIS/NRML), and dispatches to FastAPI and Firebase Cloud Messaging.
========================================================================================
"""

import asyncio
import datetime
import json
import logging
import os
import re
import sys
from typing import Any, Dict, List, Optional

try:
    import aiohttp
    HAS_AIOHTTP = True
except ImportError:
    HAS_AIOHTTP = False

try:
    from fastapi import BackgroundTasks, FastAPI, HTTPException
    from pydantic import BaseModel
    HAS_FASTAPI = True
except ImportError:
    HAS_FASTAPI = False
    class BaseModel:
        def __init__(self, **kwargs):
            for k, v in kwargs.items():
                setattr(self, k, v)
        def dict(self):
            return self.__dict__

# Try importing Telethon; if not installed, keep graceful stub for standalone tests
try:
    from telethon import TelegramClient, events
    HAS_TELETHON = True
except ImportError:
    HAS_TELETHON = False

# Import 210+ F&O symbols validator
try:
    from fno_symbols import NSE_FNO_SYMBOLS, is_valid_fno_symbol
except ImportError:
    from .fno_symbols import NSE_FNO_SYMBOLS, is_valid_fno_symbol

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] [TeleParser]: %(message)s",
    handlers=[logging.StreamHandler(sys.stdout)]
)
logger = logging.getLogger("TelegramParserService")

# Configuration
TELEGRAM_API_ID = int(os.getenv("TELEGRAM_API_ID", "1234567"))
TELEGRAM_API_HASH = os.getenv("TELEGRAM_API_HASH", "your_api_hash_here")
TELEGRAM_SESSION_NAME = os.getenv("TELEGRAM_SESSION_NAME", "fno_userbot_session")
TARGET_CHANNELS = os.getenv("TARGET_CHANNELS", "@nsetraders,@fnoalerts").split(",")

APP_BACKEND_WEBHOOK_URL = os.getenv("APP_BACKEND_WEBHOOK_URL", "http://localhost:3000/api/signals")
FCM_SERVER_KEY = os.getenv("FCM_SERVER_KEY", "")


class ParsedTradeSignal(BaseModel):
    action: str              # "BUY"
    symbol: str              # e.g. "TATAMOTORS"
    strike: float            # e.g. 1020.0
    option_type: str         # "CE" | "PE"
    setup_type: str          # "INTRADAY" | "POSITIONAL"
    product_type: str        # "MIS" | "NRML"
    entry_price: float       # e.g. 24.5
    stop_loss: float         # e.g. 23.2
    target_1: float          # e.g. 26.5
    target_2: float          # e.g. 28.0
    target_3: Optional[float] = None
    raw_message: str
    timestamp: str


class TelegramSignalParser:
    """
    Regex & NLP Parsing Engine for unstructured options trade alerts.
    Handles varied syntaxes from leading Telegram advisory channels.
    """

    # Comprehensive multi-pattern regex matching variations
    PATTERN_BUY_BASIC = re.compile(
        r'(?:BUY|BUY\s+ABOVE|ENTRY)\s+([A-Z0-9&-]+)\s+(\d+(?:\.\d+)?)\s+(CE|PE)\s*'
        r'(?:@|AT|ABOVE|CMP)?\s*(\d+(?:\.\d+)?)\s*'
        r'(?:SL|STOPLOSS|STOP\s+LOSS|S/L)\s*[:=]?\s*(\d+(?:\.\d+)?)\s*'
        r'(?:TGT|TARGETS?|TARGET)\s*[:=]?\s*(\d+(?:\.\d+)?)(?:[/,\s]+(\d+(?:\.\d+)?))?',
        re.IGNORECASE
    )

    PATTERN_PREFIX_SETUP = re.compile(
        r'(?:(POSITIONAL|INTRADAY|SWING)\s+(?:CALL|TRADE|SETUP)?[:\s-]*)?'
        r'([A-Z0-9&-]+)\s+(\d+(?:\.\d+)?)\s+(CE|PE)\s*'
        r'(?:BUY|BUY\s+ABOVE)?\s*(?:@|AT|ABOVE)?\s*(\d+(?:\.\d+)?)\s*'
        r'(?:SL|STOPLOSS|STOP\s+LOSS|S/L)\s*[:=]?\s*(\d+(?:\.\d+)?)\s*'
        r'(?:TGT|TARGETS?|TARGET)\s*[:=]?\s*(\d+(?:\.\d+)?)(?:[/,\s]+(\d+(?:\.\d+)?))?',
        re.IGNORECASE
    )

    @classmethod
    def parse_message(cls, raw_text: str) -> Optional[ParsedTradeSignal]:
        text = raw_text.strip().replace("\n", " ").replace(",", " ")

        # 1. Determine Setup Type (Default to INTRADAY if unspecified)
        setup_type = "INTRADAY"
        if re.search(r'\b(POSITIONAL|SWING|HOLDING|BTST|STBT)\b', text, re.IGNORECASE):
            setup_type = "POSITIONAL"
        elif re.search(r'\b(INTRADAY|MIS|QUICK)\b', text, re.IGNORECASE):
            setup_type = "INTRADAY"

        product_type = "MIS" if setup_type == "INTRADAY" else "NRML"

        # 2. Try Pattern 1 (Standard Action-first format)
        m = cls.PATTERN_BUY_BASIC.search(text)
        if m:
            sym, strike_s, opt_type, entry_s, sl_s, tgt1_s, tgt2_s = m.groups()
            sym = sym.upper()

            if not is_valid_fno_symbol(sym):
                logger.warning("Rejected alert: %s is not in the 210+ NSE F&O universe.", sym)
                return None

            entry = float(entry_s)
            sl = float(sl_s)
            t1 = float(tgt1_s)
            t2 = float(tgt2_s) if tgt2_s else round(entry + ((entry - sl) * 2.5), 2)

            return ParsedTradeSignal(
                action="BUY",
                symbol=sym,
                strike=float(strike_s),
                option_type=opt_type.upper(),
                setup_type=setup_type,
                product_type=product_type,
                entry_price=entry,
                stop_loss=sl,
                target_1=t1,
                target_2=t2,
                target_3=round(entry + ((entry - sl) * 4.0), 2),
                raw_message=raw_text,
                timestamp=datetime.datetime.now(datetime.timezone.utc).isoformat()
            )

        # 3. Try Pattern 2 (Prefix setup or symbol-first format)
        m2 = cls.PATTERN_PREFIX_SETUP.search(text)
        if m2:
            prefix_setup, sym, strike_s, opt_type, entry_s, sl_s, tgt1_s, tgt2_s = m2.groups()
            sym = sym.upper()

            if not is_valid_fno_symbol(sym):
                logger.warning("Rejected alert: %s is not in the 210+ NSE F&O universe.", sym)
                return None

            if prefix_setup:
                setup_type = "POSITIONAL" if prefix_setup.upper() in ("POSITIONAL", "SWING") else "INTRADAY"
                product_type = "MIS" if setup_type == "INTRADAY" else "NRML"

            entry = float(entry_s)
            sl = float(sl_s)
            t1 = float(tgt1_s)
            t2 = float(tgt2_s) if tgt2_s else round(entry + ((entry - sl) * 2.5), 2)

            return ParsedTradeSignal(
                action="BUY",
                symbol=sym,
                strike=float(strike_s),
                option_type=opt_type.upper(),
                setup_type=setup_type,
                product_type=product_type,
                entry_price=entry,
                stop_loss=sl,
                target_1=t1,
                target_2=t2,
                target_3=round(entry + ((entry - sl) * 4.0), 2),
                raw_message=raw_text,
                timestamp=datetime.datetime.now(datetime.timezone.utc).isoformat()
            )

        return None



async def forward_to_app_and_fcm(signal: ParsedTradeSignal):
    """
    Direct FCM Engine integration:
    Formats a high-priority FCM payload with priority='high' and ttl=0
    to force wake up the Android phone even when locked or in Doze Mode.
    """
    logger.info("Forwarding parsed signal %s %s to Android app and Direct FCM Engine...", signal.symbol, signal.setup_type)

    # Format Direct FCM Payload with priority='high' and ttl=0
    fcm_payload = {
        "to": "/topics/nse_fno_alerts",
        "priority": "high",
        "time_to_live": 0,
        "ttl": 0,
        "content_available": True,
        "direct_boot_ok": True,
        "android": {
            "priority": "high",
            "ttl": "0s",
            "direct_boot_ok": True,
            "notification": {
                "channel_id": "nse_scanner_signals_channel",
                "notification_priority": "PRIORITY_MAX",
                "sound": "default",
                "default_sound": True,
                "default_vibrate_timings": True,
                "visibility": "PUBLIC",
                "click_action": "FLUTTER_NOTIFICATION_CLICK"
            }
        },
        "notification": {
            "title": f"🚨 [{signal.setup_type}] {signal.symbol} {int(signal.strike)} {signal.option_type}",
            "body": f"Entry: ₹{signal.entry_price} | SL: ₹{signal.stop_loss} | TGT: ₹{signal.target_2}",
            "sound": "default",
            "android_channel_id": "nse_scanner_signals_channel"
        },
        "data": {
            "force_wake_device": "true",
            "wake_screen": "true",
            "doze_bypass": "true",
            "priority": "high",
            "ttl": "0",
            "time_to_live": "0",
            "symbol": signal.symbol,
            "strike": str(signal.strike),
            "option_type": signal.option_type,
            "setup_type": signal.setup_type,
            "product_type": signal.product_type,
            "entry_price": str(signal.entry_price),
            "stop_loss": str(signal.stop_loss),
            "target_1": str(signal.target_1),
            "target_2": str(signal.target_2),
            "target_3": str(signal.target_3 or 0.0),
            "timestamp": signal.timestamp,
            "payload_json": signal.json()
        }
    }

    if not FCM_SERVER_KEY:
        logger.debug("FCM_SERVER_KEY not set. Direct FCM wakeup payload formatted successfully.")
        return

    fcm_url = "https://fcm.googleapis.com/fcm/send"
    headers = {
        "Authorization": f"key={FCM_SERVER_KEY}",
        "Content-Type": "application/json"
    }

    try:
        if HAS_AIOHTTP:
            async with aiohttp.ClientSession() as session:
                async with session.post(fcm_url, headers=headers, json=fcm_payload, timeout=aiohttp.ClientTimeout(total=8)) as resp:
                    logger.info("Direct FCM push response status: %d", resp.status)
    except Exception as e:
        logger.error("Error dispatching Direct FCM payload: %s", e)


# FastAPI Application Microservice
if HAS_FASTAPI:
    app = FastAPI(title="Telegram F&O Parser Microservice", version="1.0.0")

    @app.post("/api/parse-raw-message")
    async def parse_raw_message_endpoint(payload: Dict[str, str], background_tasks: BackgroundTasks):
        raw_text = payload.get("message", "")
        if not raw_text:
            raise HTTPException(status_code=400, detail="Empty message")

        signal = TelegramSignalParser.parse_message(raw_text)
        if not signal:
            raise HTTPException(status_code=422, detail="Message could not be parsed or symbol not in 210+ F&O universe")

        background_tasks.add_task(forward_to_app_and_fcm, signal)
        return {"status": "success", "data": signal.dict()}

    @app.get("/api/health")
    async def health_check():
        return {
            "status": "healthy",
            "fno_universe_count": len(NSE_FNO_SYMBOLS),
            "telethon_available": HAS_TELETHON,
            "sample_symbols": sorted(list(NSE_FNO_SYMBOLS))[:8]
        }
else:
    app = None


# Telethon User-Bot Background Listener
async def start_telethon_userbot():
    if not HAS_TELETHON:
        logger.warning("Telethon package not installed. User-bot auto-listening disabled.")
        return

    logger.info("Initializing Telethon User-Bot on channels: %s", TARGET_CHANNELS)
    client = TelegramClient(TELEGRAM_SESSION_NAME, TELEGRAM_API_ID, TELEGRAM_API_HASH)

    @client.on(events.NewMessage(chats=TARGET_CHANNELS))
    async def handler(event):
        raw_text = event.message.message
        logger.info("Received raw Telegram post: %s", raw_text)
        signal = TelegramSignalParser.parse_message(raw_text)
        if signal:
            await forward_to_app_and_fcm(signal)

    try:
        await client.start()
        logger.info("✅ Telethon userbot listening for live F&O alerts...")
        await client.run_until_disconnected()
    except Exception as e:
        logger.error("Telethon error: %s", e)


if __name__ == "__main__":
    # Self-test parsing test cases
    test_1 = "BUY TATAMOTORS 1020 CE @ 24.5 SL 23.2 TGT 26.5/28 INTRADAY"
    test_2 = "POSITIONAL CALL: RELIANCE 2900 PE BUY ABOVE 68 SL 59 TARGET 86/104"
    test_3 = "BUY UNLISTEDSTOCK 500 CE @ 12 SL 10 TGT 15"  # Should be rejected!

    p1 = TelegramSignalParser.parse_message(test_1)
    p2 = TelegramSignalParser.parse_message(test_2)
    p3 = TelegramSignalParser.parse_message(test_3)

    print("Test 1 Result:", p1.dict() if p1 else "None")
    print("Test 2 Result:", p2.dict() if p2 else "None")
    print("Test 3 Result (Should be Rejected):", p3)

    if app:
        try:
            import uvicorn
            uvicorn.run(app, host="0.0.0.0", port=8000)
        except ImportError:
            print("uvicorn not installed; API server skipped for self-test.")
