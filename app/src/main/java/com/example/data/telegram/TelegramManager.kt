package com.example.data.telegram

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.model.OptionType
import com.example.data.model.SetupType
import com.example.data.model.TradeSignal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class TelegramManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("telegram_prefs", Context.MODE_PRIVATE)

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    var botToken: String
        get() = prefs.getString(KEY_BOT_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_BOT_TOKEN, value.trim()).apply()

    var chatId: String
        get() = prefs.getString(KEY_CHAT_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CHAT_ID, value.trim()).apply()

    var isAutoSendEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SEND, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_SEND, value).apply()

    var minConfidenceToSend: Int
        get() = prefs.getInt(KEY_MIN_CONFIDENCE, 85)
        set(value) = prefs.edit().putInt(KEY_MIN_CONFIDENCE, value).apply()

    val isConfigured: Boolean
        get() = botToken.isNotBlank() && chatId.isNotBlank()

    fun formatSignalMessage(signal: TradeSignal): String {
        val typeEmoji = if (signal.optionType == OptionType.CE) "🟢 CALL (CE)" else "🔴 PUT (PE)"
        val setupTag = if (signal.setupType == SetupType.INTRADAY) "⚡ INTRADAY HIGH-MOMENTUM" else "📆 POSITIONAL MULTI-DAY SWING"
        val timeStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(signal.timestamp))

        return buildString {
            append("🚨 *INSTITUTIONAL OPTIONS ALERT* 🚨\n")
            append("━━━━━━━━━━━━━━━━━━━━━\n")
            append("🎯 *Setup:* $setupTag\n")
            append("📌 *Contract:* `${signal.symbol} ${signal.strikePrice.toInt()} ${signal.optionType}`\n")
            append("🏷 *Direction:* $typeEmoji\n")
            append("📅 *Expiry:* ${signal.expiry} | Lot: ${signal.lotSize}\n")
            append("📊 *Spot:* ₹${String.format(Locale.US, "%.2f", signal.spotPrice)}\n")
            append("━━━━━━━━━━━━━━━━━━━━━\n")
            append("🏛️ *CONFLUENCE SCORE:* *${signal.confluenceScore}/100* (Institutional Grade)\n")
            append(" • Structure: ${signal.marketStructureScore}/25 | Volatility: ${signal.volatilityRegimeScore}/20\n")
            append(" • Order Flow: ${signal.orderFlowScore}/25 | Price/Vol: ${signal.priceActionScore}/15 | R:R: ${signal.riskRewardScore}/15\n")
            append("━━━━━━━━━━━━━━━━━━━━━\n")
            append("📈 *DERIVATIVES MARKET STRUCTURE:*\n")
            append(" • PCR: Volume ${String.format(Locale.US, "%.2f", signal.volumePcr)} | OI ${String.format(Locale.US, "%.2f", signal.oiPcr)}\n")
            append(" • Max Pain Strike: ₹${signal.maxPainStrike.toInt()} (Magnet Level)\n")
            append(" • Call Resistance Wall: ₹${signal.callOiWall.toInt()} | Put Support Wall: ₹${signal.putOiWall.toInt()}\n")
            append("━━━━━━━━━━━━━━━━━━━━━\n")
            append("⚡ *VOLATILITY & ORDER FLOW:*\n")
            append(" • India VIX: ${String.format(Locale.US, "%.1f", signal.indiaVix)} (${signal.vixRegime})\n")
            append(" • Preferred Strategy: ${signal.preferredStrategy}\n")
            append(" • IV Skew: ${signal.ivSkewSentiment}\n")
            append(" • Order Book Delta: ${String.format(Locale.US, "%+.0f", signal.orderBookDeltaPercent)}% Imbalance\n")
            append(" • Volume Surge: ${String.format(Locale.US, "%.1f", signal.volumeSpikeMultiple)}x 10-SMA\n")
            append("━━━━━━━━━━━━━━━━━━━━━\n")
            append("💰 *TRADE EXECUTION LEVELS:*\n")
            append(" • Entry Zone: ₹${String.format(Locale.US, "%.2f", signal.entryPrice)}\n")
            append(" • Initial Stop Loss: ₹${String.format(Locale.US, "%.2f", signal.stopLoss)} (< 5% risk)\n")
            append(" • Target 1 (1:1.5 R:R): ₹${String.format(Locale.US, "%.2f", signal.target1)}\n")
            append(" • Target 2 (1:2.5 R:R): ₹${String.format(Locale.US, "%.2f", signal.target2)}\n")
            append(" • Runner Target 3: ₹${String.format(Locale.US, "%.2f", signal.target3)}\n")
            append("━━━━━━━━━━━━━━━━━━━━━\n")
            append("🛡️ *TRADE & RISK MANAGEMENT:*\n")
            append(" • ${signal.partialExitPlan}\n")
            append(" • ⏳ Theta Protection: 45-min decay limit for intraday buying\n")
            append("━━━━━━━━━━━━━━━━━━━━━\n")
            append("⏱ _Generated at: ${timeStr}_\n")
            append("⚠️ _Strict institutional discipline: Exit 50% at T1 & lock Breakeven immediately._")
        }
    }

    suspend fun sendTelegramMessage(text: String): Result<String> = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            return@withContext Result.failure(IllegalStateException("Telegram Bot Token or Chat ID is not configured."))
        }

        try {
            val url = "https://api.telegram.org/bot$botToken/sendMessage"
            val json = JSONObject().apply {
                put("chat_id", chatId)
                put("text", text)
                put("parse_mode", "Markdown")
            }

            val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    Result.success("Message delivered successfully to Telegram!")
                } else {
                    val errorMsg = try {
                        JSONObject(bodyStr).optString("description", "HTTP ${response.code}")
                    } catch (e: Exception) {
                        "HTTP ${response.code}: $bodyStr"
                    }
                    Result.failure(Exception(errorMsg))
                }
            }
        } catch (e: Exception) {
            Log.e("TelegramManager", "Failed to send telegram message", e)
            Result.failure(e)
        }
    }

    suspend fun testConnection(): Result<String> {
        val testMessage = "🔔 *NSE F&O Options Scanner* is successfully linked to your Telegram!\n\nLive CE/PE alerts with low-SL entries will be delivered here instantly."
        return sendTelegramMessage(testMessage)
    }

    companion object {
        private const val KEY_BOT_TOKEN = "key_bot_token"
        private const val KEY_CHAT_ID = "key_chat_id"
        private const val KEY_AUTO_SEND = "key_auto_send"
        private const val KEY_MIN_CONFIDENCE = "key_min_confidence"
    }
}
