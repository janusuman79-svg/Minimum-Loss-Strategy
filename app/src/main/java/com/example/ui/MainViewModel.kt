package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.OptionType
import com.example.data.model.SetupType
import com.example.data.model.SignalStatus
import com.example.data.model.TradeSignal
import com.example.data.repository.SignalRepository
import com.example.data.scanner.FnoStock
import com.example.data.scanner.StockUniverse
import com.example.service.ScannerForegroundService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class AppTab {
    INTRADAY,
    POSITIONAL,
    SCANNER,
    TELEGRAM,
    PERFORMANCE
}

data class MarketIndices(
    val niftySpot: Double = 24855.40,
    val niftyChange: Double = 142.30,
    val niftyChangePercent: Double = 0.58,
    val bankNiftySpot: Double = 51240.80,
    val bankNiftyChange: Double = 318.50,
    val bankNiftyChangePercent: Double = 0.63,
    val pcr: Double = 1.24,
    val fiiNetFlowCr: Double = 2640.0,
    val isMarketOpen: Boolean = true
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SignalRepository(application)
    val telegramManager = repository.telegramManager

    private val _selectedTab = MutableStateFlow(AppTab.INTRADAY)
    val selectedTab: StateFlow<AppTab> = _selectedTab.asStateFlow()

    private val _optionTypeFilter = MutableStateFlow<OptionType?>(null) // null = all
    val optionTypeFilter: StateFlow<OptionType?> = _optionTypeFilter.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _marketIndices = MutableStateFlow(MarketIndices())
    val marketIndices: StateFlow<MarketIndices> = _marketIndices.asStateFlow()

    private val _telegramStatusMessage = MutableStateFlow<String?>(null)
    val telegramStatusMessage: StateFlow<String?> = _telegramStatusMessage.asStateFlow()

    private val _snackbarEvent = MutableSharedFlow<String>()
    val snackbarEvent: SharedFlow<String> = _snackbarEvent.asSharedFlow()

    private val _selectedSignalForCalculator = MutableStateFlow<TradeSignal?>(null)
    val selectedSignalForCalculator: StateFlow<TradeSignal?> = _selectedSignalForCalculator.asStateFlow()

    // F&O Stock Radar list
    val fnoUniverse: List<FnoStock> = StockUniverse.STOCKS

    val allSignals: StateFlow<List<TradeSignal>> = repository.allSignals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val intradaySignals: StateFlow<List<TradeSignal>> = repository.intradaySignals
        .combine(_optionTypeFilter) { list, filter ->
            if (filter == null) list else list.filter { it.optionType == filter }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val positionalSignals: StateFlow<List<TradeSignal>> = repository.positionalSignals
        .combine(_optionTypeFilter) { list, filter ->
            if (filter == null) list else list.filter { it.optionType == filter }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isBackgroundServiceRunning: StateFlow<Boolean> = ScannerForegroundService.isServiceRunning
    val totalAlertsSent: StateFlow<Int> = ScannerForegroundService.totalAlertsSent

    init {
        viewModelScope.launch {
            repository.populateInitialDataIfEmpty()
        }

        // Live market tick simulation for realistic trading terminal dynamics
        viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(3000)
                updateMarketTicks()
            }
        }
    }

    private fun updateMarketTicks() {
        val current = _marketIndices.value
        val deltaNifty = (Random.nextDouble(-1.5, 2.2) * 10).toInt() / 10.0
        val deltaBank = (Random.nextDouble(-3.0, 4.5) * 10).toInt() / 10.0

        val newNifty = (current.niftySpot + deltaNifty)
        val newBank = (current.bankNiftySpot + deltaBank)

        _marketIndices.value = current.copy(
            niftySpot = newNifty,
            niftyChange = current.niftyChange + deltaNifty,
            bankNiftySpot = newBank,
            bankNiftyChange = current.bankNiftyChange + deltaBank
        )
    }

    fun selectTab(tab: AppTab) {
        _selectedTab.value = tab
    }

    fun setOptionFilter(type: OptionType?) {
        _optionTypeFilter.value = type
    }

    fun toggleBackgroundService() {
        val context = getApplication<Application>()
        if (isBackgroundServiceRunning.value) {
            ScannerForegroundService.stop(context)
            viewModelScope.launch {
                _snackbarEvent.emit("Background Scanner Stopped")
            }
        } else {
            ScannerForegroundService.start(context)
            viewModelScope.launch {
                _snackbarEvent.emit("Background Scanner Started! Running in background & minimized.")
            }
        }
    }

    fun triggerScanNow(setupType: SetupType? = null) {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                val newSignal = repository.triggerInstantAlert(setupType)
                _snackbarEvent.emit("Found setup: ${newSignal.symbol} ${newSignal.strikePrice.toInt()} ${newSignal.optionType} (${newSignal.setupType})")
            } catch (e: Exception) {
                _snackbarEvent.emit("Scan error: ${e.localizedMessage}")
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun sendToTelegram(signal: TradeSignal) {
        viewModelScope.launch {
            val result = repository.sendTelegramAlertForSignal(signal)
            result.onSuccess {
                _snackbarEvent.emit("Sent to Telegram: ${signal.symbol} ${signal.strikePrice.toInt()} ${signal.optionType}")
            }.onFailure { err ->
                _snackbarEvent.emit("Telegram delivery failed: ${err.message}")
            }
        }
    }

    fun saveTelegramSettings(botToken: String, chatId: String, autoSend: Boolean, minConfidence: Int) {
        telegramManager.botToken = botToken
        telegramManager.chatId = chatId
        telegramManager.isAutoSendEnabled = autoSend
        telegramManager.minConfidenceToSend = minConfidence
        viewModelScope.launch {
            _snackbarEvent.emit("Telegram settings saved successfully!")
        }
    }

    fun testTelegramConnection() {
        viewModelScope.launch {
            _telegramStatusMessage.value = "Sending test message..."
            val res = telegramManager.testConnection()
            res.onSuccess { msg ->
                _telegramStatusMessage.value = "✅ Connected: $msg"
                _snackbarEvent.emit("Telegram test message sent successfully!")
            }.onFailure { err ->
                _telegramStatusMessage.value = "❌ Connection Failed: ${err.localizedMessage}"
                _snackbarEvent.emit("Error: ${err.localizedMessage}")
            }
        }
    }

    fun deleteSignal(id: Long) {
        viewModelScope.launch {
            repository.deleteSignal(id)
            _snackbarEvent.emit("Signal removed")
        }
    }

    fun openCalculator(signal: TradeSignal) {
        _selectedSignalForCalculator.value = signal
    }

    fun closeCalculator() {
        _selectedSignalForCalculator.value = null
    }

    fun trailStopLossToCost(signalId: Long, entryPrice: Double) {
        viewModelScope.launch {
            repository.trailStopLossToCost(signalId, entryPrice)
            _snackbarEvent.emit("🛡️ Trailed SL to Cost (₹${String.format(java.util.Locale.US, "%.2f", entryPrice)}) - ZERO RISK TRADE!")
        }
    }

    fun updateTrailingStopLoss(signalId: Long, trailedPrice: Double) {
        viewModelScope.launch {
            repository.updateTrailingStopLoss(signalId, trailedPrice)
            _snackbarEvent.emit("📈 Trailed SL adjusted to ₹${String.format(java.util.Locale.US, "%.2f", trailedPrice)}")
        }
    }

    fun clearAllSignals() {
        viewModelScope.launch {
            repository.clearAll()
            _snackbarEvent.emit("All signals cleared")
        }
    }
}
