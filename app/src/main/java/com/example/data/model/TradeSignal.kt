package com.example.data.model

enum class OptionType {
    CE, PE
}

enum class SetupType {
    INTRADAY, POSITIONAL
}

enum class SignalStatus {
    ACTIVE,
    TGT1_HIT,
    TGT2_HIT,
    TGT3_HIT,
    SL_HIT,
    CLOSED
}

data class TradeSignal(
    val id: Long = 0,
    val symbol: String,
    val optionType: OptionType,
    val strikePrice: Double,
    val expiry: String,
    val setupType: SetupType,
    val spotPrice: Double,
    val entryPrice: Double,
    val stopLoss: Double,
    val target1: Double,
    val target2: Double,
    val target3: Double,
    val currentPrice: Double,
    val lotSize: Int,
    val confidenceScore: Int, // e.g. 92%
    val scannerReasons: List<String>,
    val status: SignalStatus = SignalStatus.ACTIVE,
    val timestamp: Long = System.currentTimeMillis(),
    val isTelegramSent: Boolean = false,
    val highPrice: Double = entryPrice,
    val lowPrice: Double = entryPrice,
    val holdingPeriod: String = if (setupType == SetupType.INTRADAY) "Auto-Squareoff: 3:15 PM IST" else "Holding Period: 2-5 Days",
    val delta: Double = 0.50,
    val ivPercentile: Double = 45.0,
    val gannLevel: String = "180° Vibration",
    val astroStatus: String = "Non-Bhadra / VOC Cleared",
    val trailedStopLoss: Double? = null,
    val isTrailedToCost: Boolean = false,

    // --- Institutional Confluence Scoring Model (0 - 100) ---
    val confluenceScore: Int = confidenceScore,
    val marketStructureScore: Int = 23, // out of 25
    val volatilityRegimeScore: Int = 18, // out of 20
    val orderFlowScore: Int = 23,        // out of 25
    val priceActionScore: Int = 14,      // out of 15
    val riskRewardScore: Int = 14,       // out of 15

    // --- 1. Derivatives Market Structure Filters ---
    val volumePcr: Double = 1.18,
    val oiPcr: Double = 1.22,
    val maxPainStrike: Double = strikePrice,
    val callOiWall: Double = strikePrice + 50.0, // Institutional Resistance
    val putOiWall: Double = strikePrice - 50.0,  // Institutional Support

    // --- 2. India VIX & Volatility Regime ---
    val indiaVix: Double = 13.2,
    val vixRegime: String = "Normal / Range (12-18)",
    val preferredStrategy: String = "Spreads / Intraday Scalps",
    val ivSkew: Double = 1.8,
    val ivSkewSentiment: String = "Neutral / Balanced Skew",

    // --- 3. Order Flow & Microstructure ---
    val vwapUpperBand: Double = spotPrice * 1.004, // +1σ
    val vwapLowerBand: Double = spotPrice * 0.996, // -1σ
    val orderBookDeltaPercent: Double = 42.0,       // Bid vs Ask imbalance %
    val volumeSpikeMultiple: Double = 2.6,         // >= 2.0x 10-period SMA
    val candlestickPattern: String = "15m ORB Breakout",

    // --- 4. Advanced Trade & Risk Management ---
    val partialExitPlan: String = "Exit 50% @ Target 1 (1:1.5 R:R) -> Shift SL to Breakeven -> Trail 50% via ATR",
    val timeToThetaDecayLimitMinutes: Int = 45,
    val atrTrailingStop: Double = stopLoss
) {
    val productType: String
        get() = if (setupType == SetupType.INTRADAY) "MIS" else "NRML"

    val effectiveStopLoss: Double
        get() = trailedStopLoss ?: stopLoss

    val isRiskFree: Boolean
        get() = isTrailedToCost || (trailedStopLoss != null && trailedStopLoss!! >= entryPrice)

    val riskAmount: Double
        get() = if (isRiskFree) 0.0 else (entryPrice - effectiveStopLoss).coerceAtLeast(0.1)

    val effectiveRiskAmount: Double
        get() = if (isRiskFree) 0.0 else (entryPrice - effectiveStopLoss).coerceAtLeast(0.0)

    val rewardAmountT1: Double
        get() = (target1 - entryPrice).coerceAtLeast(0.1)

    val rewardAmountT2: Double
        get() = (target2 - entryPrice).coerceAtLeast(0.1)

    val targetMultiplierT1: String
        get() = if (setupType == SetupType.INTRADAY) "1:1.5" else "1:2.0"

    val targetMultiplierT2: String
        get() = if (setupType == SetupType.INTRADAY) "1:2.5" else "1:4.0"

    val riskRewardRatio: String
        get() {
            if (isRiskFree) return "Risk-Free"
            val r = (rewardAmountT1 / riskAmount)
            return "1:${String.format(java.util.Locale.US, "%.1f", r)}"
        }

    val pnlPerLot: Double
        get() = (currentPrice - entryPrice) * lotSize

    val pnlPercent: Double
        get() = if (entryPrice > 0) ((currentPrice - entryPrice) / entryPrice) * 100.0 else 0.0

    val maxLotRisk: Double
        get() = if (isRiskFree) 0.0 else effectiveRiskAmount * lotSize

    val ageInMinutes: Long
        get() = ((System.currentTimeMillis() - timestamp) / (60 * 1000)).coerceAtLeast(0)

    val isThetaDecayRisk: Boolean
        get() = setupType == SetupType.INTRADAY && status == SignalStatus.ACTIVE && ageInMinutes >= timeToThetaDecayLimitMinutes && currentPrice < target1

    val thetaDecayMinutesRemaining: Long
        get() = (timeToThetaDecayLimitMinutes - ageInMinutes).coerceAtLeast(0)

    val pcrStatusText: String
        get() {
            return when {
                volumePcr > 1.4 -> "Overbought (>1.4)"
                volumePcr < 0.6 -> "Oversold (<0.6)"
                else -> "Confluent (${String.format(java.util.Locale.US, "%.2f", volumePcr)})"
            }
        }

    fun toBrokerOrderPayload(broker: String = "SmartAPI"): String {
        return """
            {
              "exchange": "NFO",
              "symbol": "$symbol",
              "tradingsymbol": "${symbol}${strikePrice.toInt()}${optionType}",
              "transaction_type": "BUY",
              "product_type": "$productType",
              "order_type": "LIMIT",
              "price": $entryPrice,
              "quantity": $lotSize,
              "stoploss": $stopLoss,
              "trigger_price": $stopLoss,
              "tag": "OPT_SCANNER_${setupType}"
            }
        """.trimIndent()
    }
}
