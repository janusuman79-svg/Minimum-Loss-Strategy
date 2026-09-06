package com.example.data.repository

import android.content.Context
import com.example.data.db.AppDatabase
import com.example.data.db.SignalEntity
import com.example.data.model.OptionType
import com.example.data.model.SetupType
import com.example.data.model.SignalStatus
import com.example.data.model.TradeSignal
import com.example.data.scanner.OptionsScannerEngine
import com.example.data.telegram.TelegramManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.random.Random

class SignalRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val signalDao = db.signalDao()
    val telegramManager = TelegramManager(context)

    val allSignals: Flow<List<TradeSignal>> = signalDao.getAllSignals().map { list ->
        list.map { it.toDomain() }
    }

    val intradaySignals: Flow<List<TradeSignal>> = signalDao.getSignalsBySetup(SetupType.INTRADAY.name).map { list ->
        list.map { it.toDomain() }
    }

    val positionalSignals: Flow<List<TradeSignal>> = signalDao.getSignalsBySetup(SetupType.POSITIONAL.name).map { list ->
        list.map { it.toDomain() }
    }

    suspend fun populateInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        if (signalDao.getCount() == 0) {
            val generated = OptionsScannerEngine.scanUniverse(minConfidence = 86)
            val entities = generated.map { SignalEntity.fromDomain(it) }
            signalDao.insertSignals(entities)
        }
    }

    suspend fun runFullScanAndSave(minConfidence: Int = 85): List<TradeSignal> = withContext(Dispatchers.IO) {
        val detected = OptionsScannerEngine.scanUniverse(minConfidence = minConfidence)
        val savedSignals = mutableListOf<TradeSignal>()

        for (sig in detected) {
            val id = signalDao.insertSignal(SignalEntity.fromDomain(sig))
            val savedSignal = sig.copy(id = id)
            savedSignals.add(savedSignal)

            if (telegramManager.isConfigured && telegramManager.isAutoSendEnabled && sig.confidenceScore >= telegramManager.minConfidenceToSend) {
                val message = telegramManager.formatSignalMessage(savedSignal)
                val res = telegramManager.sendTelegramMessage(message)
                if (res.isSuccess) {
                    signalDao.markTelegramSent(id)
                }
            }
        }
        savedSignals
    }

    suspend fun triggerInstantAlert(setupType: SetupType? = null): TradeSignal = withContext(Dispatchers.IO) {
        val signal = OptionsScannerEngine.generateInstantSignal(setupType)
        val id = signalDao.insertSignal(SignalEntity.fromDomain(signal))
        val saved = signal.copy(id = id)

        if (telegramManager.isConfigured && telegramManager.isAutoSendEnabled) {
            val message = telegramManager.formatSignalMessage(saved)
            val res = telegramManager.sendTelegramMessage(message)
            if (res.isSuccess) {
                signalDao.markTelegramSent(id)
            }
        }
        saved
    }

    suspend fun simulateLiveMarketTick(): List<TradeSignal> = withContext(Dispatchers.IO) {
        emptyList()
    }

    suspend fun updatePriceForSignal(signalId: Long, newPrice: Double, newStatus: SignalStatus) = withContext(Dispatchers.IO) {
        signalDao.updatePriceAndStatus(signalId, newPrice, newStatus.name)
    }

    suspend fun trailStopLossToCost(id: Long, entryPrice: Double) = withContext(Dispatchers.IO) {
        signalDao.updateTrailingStopLoss(id, entryPrice, true)
    }

    suspend fun updateTrailingStopLoss(id: Long, trailedPrice: Double) = withContext(Dispatchers.IO) {
        signalDao.updateTrailingStopLoss(id, trailedPrice, false)
    }

    suspend fun sendTelegramAlertForSignal(signal: TradeSignal): Result<String> = withContext(Dispatchers.IO) {
        val message = telegramManager.formatSignalMessage(signal)
        val res = telegramManager.sendTelegramMessage(message)
        if (res.isSuccess && signal.id != 0L) {
            signalDao.markTelegramSent(signal.id)
        }
        res
    }

    suspend fun deleteSignal(id: Long) = withContext(Dispatchers.IO) {
        signalDao.deleteSignal(id)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        signalDao.clearAllSignals()
    }
}
