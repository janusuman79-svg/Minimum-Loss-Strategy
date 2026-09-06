package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.OptionType
import com.example.data.model.SetupType
import com.example.data.model.SignalStatus
import com.example.data.model.TradeSignal

@Entity(tableName = "trade_signals")
data class SignalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val symbol: String,
    val optionType: String,
    val strikePrice: Double,
    val expiry: String,
    val setupType: String,
    val spotPrice: Double,
    val entryPrice: Double,
    val stopLoss: Double,
    val target1: Double,
    val target2: Double,
    val target3: Double,
    val currentPrice: Double,
    val lotSize: Int,
    val confidenceScore: Int,
    val scannerReasons: String, // Pipe-separated string
    val status: String,
    val timestamp: Long,
    val isTelegramSent: Boolean,
    val highPrice: Double,
    val lowPrice: Double,
    val delta: Double = 0.50,
    val ivPercentile: Double = 45.0,
    val gannLevel: String = "180° Vibration",
    val astroStatus: String = "Non-Bhadra / VOC Cleared",
    val trailedStopLoss: Double? = null,
    val isTrailedToCost: Boolean = false,

    // Confluence Scoring Model
    val confluenceScore: Int = confidenceScore,
    val marketStructureScore: Int = 23,
    val volatilityRegimeScore: Int = 18,
    val orderFlowScore: Int = 23,
    val priceActionScore: Int = 14,
    val riskRewardScore: Int = 14,

    // Derivatives Market Structure
    val volumePcr: Double = 1.18,
    val oiPcr: Double = 1.22,
    val maxPainStrike: Double = strikePrice,
    val callOiWall: Double = strikePrice + 50.0,
    val putOiWall: Double = strikePrice - 50.0,

    // Volatility Regime
    val indiaVix: Double = 13.2,
    val vixRegime: String = "Normal / Range (12-18)",
    val preferredStrategy: String = "Spreads / Intraday Scalps",
    val ivSkew: Double = 1.8,
    val ivSkewSentiment: String = "Neutral / Balanced Skew",

    // Order Flow
    val vwapUpperBand: Double = spotPrice * 1.004,
    val vwapLowerBand: Double = spotPrice * 0.996,
    val orderBookDeltaPercent: Double = 42.0,
    val volumeSpikeMultiple: Double = 2.6,
    val candlestickPattern: String = "15m ORB Breakout",

    // Advanced Trade Management
    val partialExitPlan: String = "Exit 50% @ Target 1 (1:1.5 R:R) -> Shift SL to Breakeven -> Trail 50% via ATR",
    val timeToThetaDecayLimitMinutes: Int = 45,
    val atrTrailingStop: Double = stopLoss
) {
    fun toDomain(): TradeSignal {
        return TradeSignal(
            id = id,
            symbol = symbol,
            optionType = try { OptionType.valueOf(optionType) } catch (e: Exception) { OptionType.CE },
            strikePrice = strikePrice,
            expiry = expiry,
            setupType = try { SetupType.valueOf(setupType) } catch (e: Exception) { SetupType.INTRADAY },
            spotPrice = spotPrice,
            entryPrice = entryPrice,
            stopLoss = stopLoss,
            target1 = target1,
            target2 = target2,
            target3 = target3,
            currentPrice = currentPrice,
            lotSize = lotSize,
            confidenceScore = confidenceScore,
            scannerReasons = scannerReasons.split("|").filter { it.isNotBlank() },
            status = try { SignalStatus.valueOf(status) } catch (e: Exception) { SignalStatus.ACTIVE },
            timestamp = timestamp,
            isTelegramSent = isTelegramSent,
            highPrice = highPrice,
            lowPrice = lowPrice,
            delta = delta,
            ivPercentile = ivPercentile,
            gannLevel = gannLevel,
            astroStatus = astroStatus,
            trailedStopLoss = trailedStopLoss,
            isTrailedToCost = isTrailedToCost,
            confluenceScore = confluenceScore,
            marketStructureScore = marketStructureScore,
            volatilityRegimeScore = volatilityRegimeScore,
            orderFlowScore = orderFlowScore,
            priceActionScore = priceActionScore,
            riskRewardScore = riskRewardScore,
            volumePcr = volumePcr,
            oiPcr = oiPcr,
            maxPainStrike = maxPainStrike,
            callOiWall = callOiWall,
            putOiWall = putOiWall,
            indiaVix = indiaVix,
            vixRegime = vixRegime,
            preferredStrategy = preferredStrategy,
            ivSkew = ivSkew,
            ivSkewSentiment = ivSkewSentiment,
            vwapUpperBand = vwapUpperBand,
            vwapLowerBand = vwapLowerBand,
            orderBookDeltaPercent = orderBookDeltaPercent,
            volumeSpikeMultiple = volumeSpikeMultiple,
            candlestickPattern = candlestickPattern,
            partialExitPlan = partialExitPlan,
            timeToThetaDecayLimitMinutes = timeToThetaDecayLimitMinutes,
            atrTrailingStop = atrTrailingStop
        )
    }

    companion object {
        fun fromDomain(signal: TradeSignal): SignalEntity {
            return SignalEntity(
                id = signal.id,
                symbol = signal.symbol,
                optionType = signal.optionType.name,
                strikePrice = signal.strikePrice,
                expiry = signal.expiry,
                setupType = signal.setupType.name,
                spotPrice = signal.spotPrice,
                entryPrice = signal.entryPrice,
                stopLoss = signal.stopLoss,
                target1 = signal.target1,
                target2 = signal.target2,
                target3 = signal.target3,
                currentPrice = signal.currentPrice,
                lotSize = signal.lotSize,
                confidenceScore = signal.confidenceScore,
                scannerReasons = signal.scannerReasons.joinToString("|"),
                status = signal.status.name,
                timestamp = signal.timestamp,
                isTelegramSent = signal.isTelegramSent,
                highPrice = signal.highPrice,
                lowPrice = signal.lowPrice,
                delta = signal.delta,
                ivPercentile = signal.ivPercentile,
                gannLevel = signal.gannLevel,
                astroStatus = signal.astroStatus,
                trailedStopLoss = signal.trailedStopLoss,
                isTrailedToCost = signal.isTrailedToCost,
                confluenceScore = signal.confluenceScore,
                marketStructureScore = signal.marketStructureScore,
                volatilityRegimeScore = signal.volatilityRegimeScore,
                orderFlowScore = signal.orderFlowScore,
                priceActionScore = signal.priceActionScore,
                riskRewardScore = signal.riskRewardScore,
                volumePcr = signal.volumePcr,
                oiPcr = signal.oiPcr,
                maxPainStrike = signal.maxPainStrike,
                callOiWall = signal.callOiWall,
                putOiWall = signal.putOiWall,
                indiaVix = signal.indiaVix,
                vixRegime = signal.vixRegime,
                preferredStrategy = signal.preferredStrategy,
                ivSkew = signal.ivSkew,
                ivSkewSentiment = signal.ivSkewSentiment,
                vwapUpperBand = signal.vwapUpperBand,
                vwapLowerBand = signal.vwapLowerBand,
                orderBookDeltaPercent = signal.orderBookDeltaPercent,
                volumeSpikeMultiple = signal.volumeSpikeMultiple,
                candlestickPattern = signal.candlestickPattern,
                partialExitPlan = signal.partialExitPlan,
                timeToThetaDecayLimitMinutes = signal.timeToThetaDecayLimitMinutes,
                atrTrailingStop = signal.atrTrailingStop
            )
        }
    }
}
