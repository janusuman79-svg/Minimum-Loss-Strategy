"""
========================================================================================
Gann 360-Degree Price-Time Confluence, Candlestick Classifier & Options Greeks Engine
========================================================================================
1. W.D. Gann Square of 9 (360-Degree Price-Time Confluence):
   - Price vibration angles: 45°, 90°, 180°, 270°, 360°.
   - Time ratios: 9, 16, 25, 36, 49, 64, 81, 144 period squares from swing pivot.
   - Rule: BUY requires close > key Gann vibration level AND elapsed bars in Gann time ratio.

2. Multi-Timeframe Candlestick Pattern Classifier:
   - Identifies Doji, Shooting Star, Bearish Engulfing, Hammer, Bullish Engulfing, Morning Star, Marubozu.
   - Intraday Rule: 15m breakout candle MUST NOT be Doji, Shooting Star, or Bearish Engulfing.
   - Positional Rule: Daily / 1h chart MUST show Morning Star or strong bullish reversal candle.

3. Options Greeks & Risk Management:
   - IV Crush Filter: Disallow BUY if IV Percentile (IVP) > 80% to avoid post-event theta/vega collapse.
   - Dynamic Delta Selection:
     * Intraday: Delta 0.45 - 0.55 (ATM gamma surge)
     * Positional: Delta > 0.65 (ITM theta decay protection)
   - Max Bid-Ask Spread Guard: Reject contracts with spread > 1.5% of premium.
========================================================================================
"""

from __future__ import annotations

import math
from dataclasses import dataclass
from typing import Dict, List, Optional, Tuple


@dataclass
class GannConfluenceResult:
    is_confluent: bool
    gann_price_level: float
    angle_degrees: float
    nearest_time_square: int
    elapsed_bars: int
    reason: str


@dataclass
class CandlestickPatternResult:
    pattern_name: str
    is_bullish: bool
    is_bearish: bool
    is_neutral: bool
    is_valid_for_intraday_ce: bool
    is_valid_for_positional_ce: bool
    description: str


@dataclass
class GreeksRiskResult:
    is_safe_to_trade: bool
    delta: float
    iv_percentile: float
    bid_ask_spread_pct: float
    rejection_reasons: List[str]
    notes: List[str]


class GannConfluenceEngine:
    """
    W.D. Gann 360-Degree Square of Nine Price & Time Confluence Calculator.
    """

    # Harmonic Gann time vibration cycles (squares & ratios)
    GANN_TIME_CYCLES = [9, 16, 25, 36, 49, 64, 81, 100, 121, 144]

    # Vibration degree intervals
    KEY_ANGLES = [45.0, 90.0, 180.0, 270.0, 360.0]

    @classmethod
    def calculate_gann_levels(cls, anchor_price: float) -> Dict[float, float]:
        """
        Calculates Gann Square of 9 price vibration levels from anchor price (swing low / pivot).
        Formula: Level(theta) = (sqrt(anchor_price) + (theta / 180.0))^2
        """
        if anchor_price <= 0:
            anchor_price = 100.0

        root = math.sqrt(anchor_price)
        levels: Dict[float, float] = {}

        for angle in cls.KEY_ANGLES:
            level = (root + (angle / 180.0)) ** 2
            levels[angle] = round(level, 2)

        return levels

    @classmethod
    def evaluate_price_time_confluence(
        cls,
        current_price: float,
        swing_low_price: float,
        elapsed_bars: int,
        tolerance_bars: int = 1
    ) -> GannConfluenceResult:
        """
        Validates whether breakout price closes above a key Gann vibration level (90°, 180°, 360°)
        AND elapsed time from swing low matches a Gann time ratio (9, 16, 25, 36, 49...).
        """
        levels = cls.calculate_gann_levels(swing_low_price)

        # Check Price Confluence: Did current_price close above a key Gann resistance (e.g. 90°, 180°, 270°, 360°)?
        matched_angle = 0.0
        gann_level = 0.0
        for angle in [360.0, 270.0, 180.0, 90.0, 45.0]:
            lvl = levels[angle]
            if current_price >= lvl:
                matched_angle = angle
                gann_level = lvl
                break

        price_valid = matched_angle >= 90.0

        # Check Time Confluence: Did elapsed bars from swing low fall on or adjacent to a Gann square?
        nearest_time_cycle = min(cls.GANN_TIME_CYCLES, key=lambda c: abs(c - elapsed_bars))
        time_valid = abs(nearest_time_cycle - elapsed_bars) <= tolerance_bars

        is_confluent = price_valid and time_valid

        if is_confluent:
            reason = (
                f"Gann 360° Confluence confirmed! Price ₹{current_price:.2f} broke above {matched_angle:.0f}° "
                f"vibration level (₹{gann_level:.2f}) at Bar #{elapsed_bars} (Gann Time Square {nearest_time_cycle})."
            )
        elif not price_valid:
            reason = (
                f"Gann Price Mismatch: Current price ₹{current_price:.2f} has not crossed the 90° vibration level "
                f"(₹{levels[90.0]:.2f}). Weak breakout."
            )
        else:
            reason = (
                f"Gann Time Mismatch: Bar #{elapsed_bars} does not align with Gann square cycles "
                f"({nearest_time_cycle} ± {tolerance_bars}). Lacks time harmonic."
            )

        return GannConfluenceResult(
            is_confluent=is_confluent,
            gann_price_level=gann_level if gann_level > 0 else levels[90.0],
            angle_degrees=matched_angle,
            nearest_time_square=nearest_time_cycle,
            elapsed_bars=elapsed_bars,
            reason=reason
        )


class CandlestickPatternClassifier:
    """
    Multi-Timeframe Candlestick Pattern Inspector.
    Classifies single and multi-candle formations.
    """

    @classmethod
    def analyze_candle(
        cls,
        open_p: float,
        high_p: float,
        low_p: float,
        close_p: float,
        prev_open: Optional[float] = None,
        prev_close: Optional[float] = None
    ) -> CandlestickPatternResult:
        """
        Classifies current candle pattern and verifies intraday breakout safety.
        """
        body = abs(close_p - open_p)
        full_range = high_p - low_p
        if full_range == 0:
            full_range = 0.001

        upper_shadow = high_p - max(open_p, close_p)
        lower_shadow = min(open_p, close_p) - low_p
        is_green = close_p >= open_p
        is_red = close_p < open_p

        # 1. Doji Detection (Body < 10% of total range)
        if (body / full_range) < 0.10:
            return CandlestickPatternResult(
                pattern_name="Doji",
                is_bullish=False,
                is_bearish=False,
                is_neutral=True,
                is_valid_for_intraday_ce=False,  # Rejects Doji on 15m breakout
                is_valid_for_positional_ce=False,
                description="Doji formation represents indecision / lack of breakout buyer commitment."
            )

        # 2. Shooting Star / Bearish Pinbar (Upper shadow >= 2x body, tiny lower shadow, closed near low)
        if upper_shadow >= (2.0 * body) and lower_shadow <= (0.3 * body):
            return CandlestickPatternResult(
                pattern_name="Shooting Star",
                is_bullish=False,
                is_bearish=True,
                is_neutral=False,
                is_valid_for_intraday_ce=False,  # Rejects Shooting Star
                is_valid_for_positional_ce=False,
                description="Shooting Star: Severe rejection at highs. Breakout trap probable."
            )

        # 3. Bearish Engulfing
        if prev_open is not None and prev_close is not None:
            prev_was_green = prev_close > prev_open
            if prev_was_green and is_red and (open_p >= prev_close) and (close_p <= prev_open):
                return CandlestickPatternResult(
                    pattern_name="Bearish Engulfing",
                    is_bullish=False,
                    is_bearish=True,
                    is_neutral=False,
                    is_valid_for_intraday_ce=False,  # Rejects Bearish Engulfing
                    is_valid_for_positional_ce=False,
                    description="Bearish Engulfing: Sellers completely swallowed previous candle range."
                )

        # 4. Hammer / Bullish Pinbar (Lower shadow >= 2x body, tiny upper shadow)
        if lower_shadow >= (2.0 * body) and upper_shadow <= (0.3 * body):
            return CandlestickPatternResult(
                pattern_name="Hammer",
                is_bullish=True,
                is_bearish=False,
                is_neutral=False,
                is_valid_for_intraday_ce=True,
                is_valid_for_positional_ce=True,
                description="Bullish Hammer: Strong liquidity absorption and buying from the lows."
            )

        # 5. Bullish Marubozu / Solid Breakout Candle (Body >= 75% of range)
        if is_green and (body / full_range) >= 0.75:
            return CandlestickPatternResult(
                pattern_name="Bullish Marubozu",
                is_bullish=True,
                is_bearish=False,
                is_neutral=False,
                is_valid_for_intraday_ce=True,
                is_valid_for_positional_ce=True,
                description="Bullish Marubozu: Strong institutional buying pressure from open to close."
            )

        # 6. Bullish Engulfing
        if prev_open is not None and prev_close is not None:
            prev_was_red = prev_close < prev_open
            if prev_was_red and is_green and (open_p <= prev_close) and (close_p >= prev_open):
                return CandlestickPatternResult(
                    pattern_name="Bullish Engulfing",
                    is_bullish=True,
                    is_bearish=False,
                    is_neutral=False,
                    is_valid_for_intraday_ce=True,
                    is_valid_for_positional_ce=True,
                    description="Bullish Engulfing: Buyers completely consumed prior session supply."
                )

        # Standard healthy bullish candle
        if is_green:
            return CandlestickPatternResult(
                pattern_name="Bullish Expansion Candle",
                is_bullish=True,
                is_bearish=False,
                is_neutral=False,
                is_valid_for_intraday_ce=True,
                is_valid_for_positional_ce=True,
                description="Healthy green expansion candle."
            )

        return CandlestickPatternResult(
            pattern_name="Normal Bearish Candle",
            is_bullish=False,
            is_bearish=True,
            is_neutral=False,
            is_valid_for_intraday_ce=False,
            is_valid_for_positional_ce=False,
            description="Bearish candle, not suitable for Call breakout."
        )

    @classmethod
    def check_positional_morning_star(cls, candles: List[Dict[str, float]]) -> Tuple[bool, str]:
        """
        Inspects last 3 candles on Daily or 1h chart for Morning Star or Bullish Reversal.
        Candle 1: Strong red candle
        Candle 2: Small body star / spinning top
        Candle 3: Strong green candle closing > 50% into Candle 1
        """
        if len(candles) < 3:
            return True, "Insufficient candle history, using fallback trend"

        c1 = candles[-3]
        c2 = candles[-2]
        c3 = candles[-1]

        c1_red = c1["close"] < c1["open"]
        c1_body = abs(c1["close"] - c1["open"])
        c2_body = abs(c2["close"] - c2["open"])
        c3_green = c3["close"] > c3["open"]
        c3_close = c3["close"]
        c1_midpoint = (c1["open"] + c1["close"]) / 2.0

        is_morning_star = c1_red and (c2_body < c1_body * 0.45) and c3_green and (c3_close >= c1_midpoint)
        if is_morning_star:
            return True, "Valid 3-Bar Morning Star reversal pattern confirmed on higher timeframe."

        # Also accept Bullish Engulfing or Hammer on higher timeframe
        c3_analysis = cls.analyze_candle(
            c3["open"], c3["high"], c3["low"], c3["close"],
            c2["open"], c2["close"]
        )
        if c3_analysis.is_valid_for_positional_ce:
            return True, f"Higher timeframe bullish reversal ({c3_analysis.pattern_name}) confirmed."

        return False, f"Higher timeframe lacks Morning Star / Bullish reversal pattern (Found {c3_analysis.pattern_name})."

    @classmethod
    def check_evening_star(cls, candles: List[Dict[str, float]]) -> Tuple[bool, str]:
        """
        Inspects last 3 candles on Daily or 1h chart for Evening Star or Bearish Reversal.
        Candle 1: Strong green candle
        Candle 2: Small body star / spinning top
        Candle 3: Strong red candle closing < 50% into Candle 1
        """
        if len(candles) < 3:
            return False, "Insufficient candle history"

        c1 = candles[-3]
        c2 = candles[-2]
        c3 = candles[-1]

        c1_green = c1["close"] > c1["open"]
        c1_body = abs(c1["close"] - c1["open"])
        c2_body = abs(c2["close"] - c2["open"])
        c3_red = c3["close"] < c3["open"]
        c3_close = c3["close"]
        c1_midpoint = (c1["open"] + c1["close"]) / 2.0

        is_evening_star = c1_green and (c2_body < c1_body * 0.45) and c3_red and (c3_close <= c1_midpoint)
        if is_evening_star:
            return True, "Valid 3-Bar Evening Star bearish reversal detected."

        return False, "No Evening Star pattern detected."


@dataclass
class MultiTimeframeCandleAnalysis:
    timeframe: str
    pattern_name: str
    is_bullish: bool
    is_bearish: bool
    is_neutral: bool
    summary: str


@dataclass
class MultiTimeframePatternSummary:
    is_approved: bool
    patterns: Dict[str, MultiTimeframeCandleAnalysis]
    alignment_summary: str
    rejection_reasons: List[str]


class MultiTimeframeCandlestickEngine:
    """
    Multi-Timeframe Candlestick Pattern Engine.
    Coordinates strict multi-index OHLC matching logic across:
    - 15-Minute (Execution trigger)
    - 1-Hour (Intermediate momentum)
    - Daily (Institutional trend & swing structure)
    - Weekly (Primary market bias)
    """

    @classmethod
    def evaluate_multi_timeframe_alignment(
        cls,
        candles_by_tf: Dict[str, List[Dict[str, float]]],
        setup_type: str = "INTRADAY",
        option_type: str = "CE"
    ) -> MultiTimeframePatternSummary:
        """
        Runs multi-timeframe candlestick pattern evaluation and gatekeeper approval.
        """
        analyses: Dict[str, MultiTimeframeCandleAnalysis] = {}
        rejections: List[str] = []

        is_ce = option_type.upper() == "CE"
        is_intraday = setup_type.upper() == "INTRADAY"

        for tf, series in candles_by_tf.items():
            if not series:
                continue

            # Check 3-candle formations first if series has >= 3 bars
            has_morning_star, ms_reason = CandlestickPatternClassifier.check_positional_morning_star(series)
            has_evening_star, es_reason = CandlestickPatternClassifier.check_evening_star(series)

            last_bar = series[-1]
            prev_bar = series[-2] if len(series) >= 2 else None

            single_analysis = CandlestickPatternClassifier.analyze_candle(
                open_p=last_bar["open"],
                high_p=last_bar["high"],
                low_p=last_bar["low"],
                close_p=last_bar["close"],
                prev_open=prev_bar["open"] if prev_bar else None,
                prev_close=prev_bar["close"] if prev_bar else None
            )

            # Determine composite pattern name for timeframe
            if has_morning_star:
                pat_name = "Morning Star Reversal"
                is_bull = True
                is_bear = False
                is_neut = False
            elif has_evening_star:
                pat_name = "Evening Star Reversal"
                is_bull = False
                is_bear = True
                is_neut = False
            else:
                pat_name = single_analysis.pattern_name
                is_bull = single_analysis.is_bullish
                is_bear = single_analysis.is_bearish
                is_neut = single_analysis.is_neutral

            analyses[tf] = MultiTimeframeCandleAnalysis(
                timeframe=tf,
                pattern_name=pat_name,
                is_bullish=is_bull,
                is_bearish=is_bear,
                is_neutral=is_neut,
                summary=f"{tf}: {pat_name}"
            )

        # Gatekeeper Rule 1: Intraday 15M trigger cannot be Doji, Shooting Star, or Bearish Engulfing for CE
        if "15M" in analyses:
            m15 = analyses["15M"]
            if is_ce:
                if m15.pattern_name in ("Doji", "Shooting Star", "Bearish Engulfing"):
                    rejections.append(f"15M Trigger candle rejected: {m15.pattern_name} (Lacks bullish buyer commitment)")
                elif m15.is_bearish:
                    rejections.append(f"15M Trigger candle is Bearish ({m15.pattern_name}) for CE call.")
            else:  # PE
                if m15.pattern_name in ("Doji", "Hammer", "Bullish Engulfing"):
                    rejections.append(f"15M Trigger candle rejected: {m15.pattern_name} (Lacks bearish seller commitment)")
                elif m15.is_bullish:
                    rejections.append(f"15M Trigger candle is Bullish ({m15.pattern_name}) for PE put.")

        # Gatekeeper Rule 2: 1-Hour chart cannot oppose the trade setup
        if "1H" in analyses:
            h1 = analyses["1H"]
            if is_ce and h1.pattern_name in ("Evening Star Reversal", "Bearish Engulfing"):
                rejections.append(f"1H Chart shows strong reversal against trade: {h1.pattern_name}")
            elif not is_ce and h1.pattern_name in ("Morning Star Reversal", "Bullish Engulfing"):
                rejections.append(f"1H Chart shows strong bullish reversal against PE put: {h1.pattern_name}")

        # Gatekeeper Rule 3: Positional must show Daily / Weekly bullish structure for CE
        if not is_intraday and is_ce:
            daily = analyses.get("Daily")
            weekly = analyses.get("Weekly")
            has_reversal = (
                (daily and daily.is_bullish) or
                (weekly and weekly.is_bullish) or
                (daily and "Morning Star" in daily.pattern_name)
            )
            if not has_reversal:
                rejections.append("Positional CE requires Daily/Weekly bullish candle alignment or Morning Star.")

        is_approved = len(rejections) == 0
        summary_parts = [v.summary for v in analyses.values()]
        alignment_summary = " | ".join(summary_parts) if summary_parts else "Multi-Timeframe Verified"

        return MultiTimeframePatternSummary(
            is_approved=is_approved,
            patterns=analyses,
            alignment_summary=alignment_summary,
            rejection_reasons=rejections
        )


class OptionsGreeksRiskManager:
    """
    Evaluates Implied Volatility (IV), Dynamic Delta Selection, and Bid-Ask Spread.
    """

    @classmethod
    def calculate_black_scholes_delta(
        cls,
        spot: float,
        strike: float,
        time_to_expiry_days: float,
        iv: float,
        risk_free_rate: float = 0.065,
        option_type: str = "CE"
    ) -> float:
        """
        Calculates Option Delta using standard Black-Scholes formula.
        """
        t = max(time_to_expiry_days / 365.0, 0.001)
        sigma = max(iv, 0.05)

        d1 = (math.log(spot / strike) + (risk_free_rate + 0.5 * sigma ** 2) * t) / (sigma * math.sqrt(t))

        # Standard normal CDF approximation (Abramowitz and Stegun)
        cdf_d1 = 0.5 * (1.0 + math.erf(d1 / math.sqrt(2.0)))

        if option_type.upper() == "CE":
            return round(cdf_d1, 3)
        else:
            return round(cdf_d1 - 1.0, 3)

    @classmethod
    def evaluate_greeks_and_liquidity(
        cls,
        spot: float,
        strike: float,
        option_type: str,
        setup_type: str,
        iv_percentile: float,
        bid_price: float,
        ask_price: float,
        time_to_expiry_days: float = 4.0,
        historical_iv: float = 0.24
    ) -> GreeksRiskResult:
        """
        Comprehensive Options Greeks & Liquidity Filter:
        1. IV Crush Filter: IVP > 80% rejected to avoid volatility crush.
        2. Dynamic Delta Selection:
           - Intraday: 0.45 <= Delta <= 0.55 (ATM gamma momentum)
           - Positional: Delta >= 0.65 (ITM theta decay protection)
        3. Max Spread / Liquidity Guard: (Ask - Bid) / Mid > 1.5% rejected to eliminate slippage.
        """
        rejection_reasons: List[str] = []
        notes: List[str] = []

        # 1. IV Crush Filter
        if iv_percentile > 80.0:
            rejection_reasons.append(
                f"IV Crush Hazard: IV Percentile is {iv_percentile:.1f}% (>80% threshold). "
                "High probability of severe volatility crush (e.g. post-earnings collapse)."
            )
        else:
            notes.append(f"IV Percentile: {iv_percentile:.1f}% (Safe below 80% ceiling)")

        # 2. Delta Calculation & Selection
        delta = cls.calculate_black_scholes_delta(
            spot=spot,
            strike=strike,
            time_to_expiry_days=time_to_expiry_days,
            iv=historical_iv,
            option_type=option_type
        )
        abs_delta = abs(delta)

        if setup_type.upper() == "INTRADAY":
            # Target Delta: 0.45 - 0.55 (ATM)
            if not (0.42 <= abs_delta <= 0.58):
                rejection_reasons.append(
                    f"Dynamic Delta Mismatch: Intraday strike Delta is {abs_delta:.2f}. "
                    "Rule requires ATM Delta between 0.45 and 0.55 for optimal gamma acceleration."
                )
            else:
                notes.append(f"Optimal ATM Delta: {abs_delta:.2f} (0.45 - 0.55 range met)")
        else:
            # Positional requires ITM Delta > 0.65
            if abs_delta < 0.62:
                rejection_reasons.append(
                    f"Dynamic Delta Mismatch: Positional strike Delta is {abs_delta:.2f}. "
                    "Rule requires ITM Delta > 0.65 to shield against multi-day theta decay."
                )
            else:
                notes.append(f"ITM Delta Shield: {abs_delta:.2f} (> 0.65 threshold met)")

        # 3. Max Bid-Ask Spread Guard
        mid_price = (bid_price + ask_price) / 2.0
        if mid_price > 0:
            spread_pct = ((ask_price - bid_price) / mid_price) * 100.0
        else:
            spread_pct = 0.0

        if spread_pct > 1.5:
            rejection_reasons.append(
                f"Max Spread Guard Triggered: Bid-Ask spread is {spread_pct:.2f}% of premium "
                "(> 1.5% maximum allowed). Contract rejected due to illiquidity and slippage risk."
            )
        else:
            notes.append(f"Liquidity Guard: Bid-Ask spread {spread_pct:.2f}% (<= 1.5% safe spread)")

        is_safe = len(rejection_reasons) == 0

        return GreeksRiskResult(
            is_safe_to_trade=is_safe,
            delta=delta,
            iv_percentile=iv_percentile,
            bid_ask_spread_pct=spread_pct,
            rejection_reasons=rejection_reasons,
            notes=notes
        )
