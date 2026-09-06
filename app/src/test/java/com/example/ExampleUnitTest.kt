package com.example

import com.example.data.model.OptionType
import com.example.data.model.SetupType
import com.example.data.model.TradeSignal
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testTradeSignal_trailingStopLoss_becomesZeroRisk() {
        val signal = TradeSignal(
            symbol = "RELIANCE",
            optionType = OptionType.CE,
            strikePrice = 3000.0,
            expiry = "CURRENT_WEEKLY",
            setupType = SetupType.INTRADAY,
            spotPrice = 2995.0,
            entryPrice = 50.0,
            stopLoss = 47.5,
            target1 = 53.75,
            target2 = 56.25,
            target3 = 60.0,
            currentPrice = 54.0,
            lotSize = 250,
            confidenceScore = 92,
            scannerReasons = listOf("15m ORB Breakout"),
            timestamp = System.currentTimeMillis(),
            delta = 0.52,
            ivPercentile = 42.0,
            gannLevel = "180° Vibration",
            astroStatus = "VOC Cleared"
        )

        // Before trailing
        assertFalse(signal.isRiskFree)
        assertEquals(47.5, signal.effectiveStopLoss, 0.01)
        assertEquals(2.5, signal.riskAmount, 0.01)
        assertEquals(625.0, signal.maxLotRisk, 0.01)

        // After trailing to cost
        val trailedSignal = signal.copy(trailedStopLoss = 50.0, isTrailedToCost = true)
        assertTrue(trailedSignal.isRiskFree)
        assertEquals(50.0, trailedSignal.effectiveStopLoss, 0.01)
        assertEquals(0.0, trailedSignal.effectiveRiskAmount, 0.01)
        assertEquals(0.0, trailedSignal.maxLotRisk, 0.01)
        assertEquals("Risk-Free", trailedSignal.riskRewardRatio)
    }

    @Test
    fun testPositionSizing_formula_respectsRiskBudget() {
        val capital = 100000.0 // 1 Lakh
        val riskPercent = 2.0 // 2% max risk = 2000 INR
        val maxAllowedRisk = capital * (riskPercent / 100.0) // 2000 INR

        val entryPrice = 40.0
        val stopLoss = 38.0
        val lotSize = 500
        val riskPerShare = entryPrice - stopLoss // 2.0 INR
        val riskPerLot = riskPerShare * lotSize // 1000 INR per lot
        val costPerLot = entryPrice * lotSize // 20,000 INR per lot

        val lotsByRisk = max(1, floor(maxAllowedRisk / riskPerLot).toInt()) // 2 lots
        val lotsByCapital = max(1, floor(capital / costPerLot).toInt()) // 5 lots
        val recommendedLots = min(lotsByRisk, lotsByCapital) // 2 lots

        assertEquals(2, recommendedLots)
        val totalQty = recommendedLots * lotSize // 1000 qty
        val totalActualRisk = totalQty * riskPerShare // 2000 INR
        assertTrue(totalActualRisk <= maxAllowedRisk)
    }
}
