package com.example.data.market

import android.content.Context
import android.content.SharedPreferences
import com.example.data.scanner.FnoStock
import com.example.data.scanner.UpstoxInstruments
import com.example.data.model.SetupType
import com.example.data.model.TradeSignal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.concurrent.TimeUnit

data class IndexQuote(
    val lastPrice: Double,
    val change: Double,
    val changePercent: Double
)

data class LiveMarketSnapshot(
    val stocks: List<FnoStock>,
    val nifty: IndexQuote?,
    val bankNifty: IndexQuote?,
    val fetchedAt: Long = System.currentTimeMillis()
)

class UpstoxMarketData(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("upstox_market_data", Context.MODE_PRIVATE)
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    var accessToken: String
        get() = prefs.getString(KEY_ACCESS_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ACCESS_TOKEN, value.trim()).apply()

    val isConfigured: Boolean get() = accessToken.isNotBlank()

    suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val json = execute("https://api.upstox.com/v2/user/profile")
            val name = json.optJSONObject("data")?.optString("user_name").orEmpty()
            if (name.isBlank()) "Connected to Upstox" else "Connected as $name"
        }
    }

    suspend fun fetchSnapshot(baseStocks: List<FnoStock>): LiveMarketSnapshot = withContext(Dispatchers.IO) {
        check(isConfigured) { "Enter and save a valid Upstox access token in Settings." }
        val keys = baseStocks.map { it.instrumentKey }.distinct()
        val url = "https://api.upstox.com/v2/market-quote/quotes".toHttpUrl().newBuilder()
            .addQueryParameter("instrument_key", keys.joinToString(","))
            .build().toString()
        val data = execute(url).getJSONObject("data")
        val quotesByKey = buildMap<String, JSONObject> {
            data.keys().forEach { responseKey ->
                val quote = data.getJSONObject(responseKey)
                val key = quote.optString("instrument_token")
                if (key.isNotBlank()) put(key, quote)
            }
        }

        fun indexQuote(key: String): IndexQuote? {
            val quote = quotesByKey[key] ?: return null
            val last = quote.optDouble("last_price", Double.NaN)
            val apiChange = quote.optDouble("net_change", Double.NaN)
            val close = quote.optJSONObject("ohlc")?.optDouble("close", Double.NaN) ?: Double.NaN
            if (!last.isFinite() || last <= 0.0) return null
            val change = when {
                apiChange.isFinite() -> apiChange
                close.isFinite() && close > 0.0 -> last - close
                else -> 0.0
            }
            val previousClose = last - change
            return IndexQuote(last, change, if (previousClose > 0.0) change * 100.0 / previousClose else 0.0)
        }

        val updatedStocks = baseStocks.mapNotNull { stock ->
            val quote = quotesByKey[stock.instrumentKey] ?: return@mapNotNull null
            val last = quote.optDouble("last_price", Double.NaN)
            if (!last.isFinite() || last <= 0.0) return@mapNotNull null
            val average = quote.optDouble("average_price", last).takeIf { it > 0.0 } ?: last
            val buyQty = quote.optDouble("total_buy_quantity", 0.0)
            val sellQty = quote.optDouble("total_sell_quantity", 0.0)
            val imbalance = if (buyQty + sellQty > 0.0) (buyQty - sellQty) * 100.0 / (buyQty + sellQty) else 0.0
            val close = quote.optJSONObject("ohlc")?.optDouble("close", last) ?: last
            val pct = if (close > 0.0) (last - close) * 100.0 / close else 0.0
            stock.copy(
                spotPrice = last,
                vwap = average,
                vwapUpperBand = average * 1.004,
                vwapLowerBand = average * 0.996,
                orderBookDeltaPercent = imbalance,
                trend = when {
                    pct >= 0.75 -> "Strong Bullish"
                    pct > 0.0 -> "Moderate Bullish"
                    pct <= -0.75 -> "Strong Bearish"
                    pct < 0.0 -> "Moderate Bearish"
                    else -> "Sideways"
                }
            )
        }
        LiveMarketSnapshot(
            stocks = updatedStocks,
            nifty = indexQuote(UpstoxInstruments.NIFTY),
            bankNifty = indexQuote(UpstoxInstruments.BANK_NIFTY)
        )
    }

    suspend fun attachLiveOptionPrice(signal: TradeSignal, underlyingKey: String): TradeSignal = withContext(Dispatchers.IO) {
        val contractsUrl = "https://api.upstox.com/v2/option/contract".toHttpUrl().newBuilder()
            .addQueryParameter("instrument_key", underlyingKey)
            .build().toString()
        val contracts = execute(contractsUrl).getJSONArray("data")
        val wantedType = signal.optionType.name
        val today = LocalDate.now()
        val allContracts = (0 until contracts.length()).map { contracts.getJSONObject(it) }
            .filter { contract ->
                val instrumentType = contract.optString("instrument_type")
                val optionType = contract.optString("option_type")
                val symbol = contract.optString("trading_symbol")
                instrumentType.equals(wantedType, true) || optionType.equals(wantedType, true) ||
                    symbol.endsWith(wantedType, ignoreCase = true)
            }
            .mapNotNull { contract ->
                val date = try { LocalDate.parse(contract.optString("expiry")) }
                catch (_: DateTimeParseException) { null }
                if (date == null || date.isBefore(today)) null else contract to date
            }
        check(allContracts.isNotEmpty()) { "No active $wantedType option contracts available for ${signal.symbol}" }

        val expiries = allContracts.map { it.second }.distinct().sorted()
        val selectedExpiry = if (signal.setupType == SetupType.INTRADAY) {
            expiries.first()
        } else {
            val first = expiries.first()
            expiries.filter { it.year == first.year && it.month == first.month }.maxOrNull() ?: first
        }
        val contract = allContracts.asSequence()
            .filter { it.second == selectedExpiry }
            .map { it.first }
            .minByOrNull { kotlin.math.abs(it.optDouble("strike_price") - signal.strikePrice) }
            ?: error("No $wantedType contract found for $selectedExpiry")
        val optionKey = contract.getString("instrument_key")
        val quoteUrl = "https://api.upstox.com/v2/market-quote/quotes".toHttpUrl().newBuilder()
            .addQueryParameter("instrument_key", optionKey).build().toString()
        val quoteData = execute(quoteUrl).getJSONObject("data")
        val quote = quoteData.getJSONObject(quoteData.keys().next())
        val entry = quote.optDouble("last_price", 0.0)
        check(entry > 0.0) { "Upstox returned no traded option price" }
        val slPercent = if (signal.setupType == SetupType.INTRADAY) 0.044 else 0.105
        fun tick(value: Double) = kotlin.math.round(value * 20.0) / 20.0
        val sl = tick(entry * (1.0 - slPercent))
        val risk = entry - sl
        return@withContext signal.copy(
            strikePrice = contract.optDouble("strike_price", signal.strikePrice),
            expiry = contract.optString("expiry", signal.expiry),
            lotSize = contract.optInt("lot_size", signal.lotSize),
            entryPrice = tick(entry), currentPrice = tick(entry), stopLoss = sl,
            target1 = tick(entry + risk * if (signal.setupType == SetupType.INTRADAY) 1.5 else 2.0),
            target2 = tick(entry + risk * if (signal.setupType == SetupType.INTRADAY) 2.5 else 4.0),
            target3 = tick(entry + risk * if (signal.setupType == SetupType.INTRADAY) 3.5 else 6.0)
        )
    }

    private fun execute(url: String): JSONObject {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("Authorization", "Bearer $accessToken")
            .get().build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val message = runCatching {
                    JSONObject(body).optJSONArray("errors")?.optJSONObject(0)?.optString("message")
                }.getOrNull()
                error(message?.takeIf { it.isNotBlank() } ?: "Upstox request failed (HTTP ${response.code})")
            }
            return JSONObject(body)
        }
    }

    companion object { private const val KEY_ACCESS_TOKEN = "access_token" }
}
