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

enum class AppTab {
    INTRADAY,
    POSITIONAL,
    SCANNER,
    TELEGRAM,
    PERFORMANCE
}

data class MarketIndices(
    val niftySpot: Double = 0.0,
    val niftyChange: Double = 0.0,
    val niftyChangePercent: Double = 0.0,
    val bankNiftySpot: Double = 0.0,
    val bankNiftyChange: Double = 0.0,
    val bankNiftyChangePercent: Double = 0.0,
    val pcr: Double = 0.0,
    val fiiNetFlowCr: Double = 0.0,
    val isMarketOpen: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SignalRepository(application)
    val telegramManager = repository.telegramManager
    val upstoxMarketData = repository.upstoxMarketData

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

    private val _upstoxStatusMessage = MutableStateFlow<String?>(null)
    val upstoxStatusMessage: StateFlow<String?> = _upstoxStatusMessage.asStateFlow()

    private val _isLiveMarketConnected = MutableStateFlow(false)
    val isLiveMarketConnected: StateFlow<Boolean> = _isLiveMarketConnected.asStateFlow()

    private val _snackbarEvent = MutableSharedFlow<String>()
    val snackbarEvent: SharedFlow<String> = _snackbarEvent.asSharedFlow()

    private val _selectedSignalForCalculator = MutableStateFlow<TradeSignal?>(null)
    val selectedSignalForCalculator: StateFlow<TradeSignal?> = _selectedSignalForCalculator.asStateFlow()

    // F&O Stock Radar list
    private val _fnoUniverse = MutableStateFlow(StockUniverse.STOCKS)
    val fnoUniverse: StateFlow<List<FnoStock>> = _fnoUniverse.asStateFlow()

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
            repository.prepareLiveDataMode()
        }

        // Pull exchange-backed Upstox quotes; never fabricate ticks.
        viewModelScope.launch {
            while (isActive) {
                if (upstoxMarketData.isConfigured) refreshLiveMarket(silent = true)
                delay(15_000)
            }
        }
    }

    private suspend fun refreshLiveMarket(silent: Boolean = false): Boolean {
        return runCatching { repository.fetchLiveMarket() }.fold({ snapshot ->
            _fnoUniverse.value = snapshot.stocks
            val n = snapshot.nifty
            val b = snapshot.bankNifty
            _marketIndices.value = _marketIndices.value.copy(
                niftySpot = n?.lastPrice ?: 0.0, niftyChange = n?.change ?: 0.0,
                niftyChangePercent = n?.changePercent ?: 0.0,
                bankNiftySpot = b?.lastPrice ?: 0.0, bankNiftyChange = b?.change ?: 0.0,
                bankNiftyChangePercent = b?.changePercent ?: 0.0
            )
            _isLiveMarketConnected.value = true
            _upstoxStatusMessage.value = "✅ LIVE Upstox market data connected"
            true
        }, { error ->
            _isLiveMarketConnected.value = false
            _upstoxStatusMessage.value = "❌ ${error.localizedMessage}"
            if (!silent) _snackbarEvent.emit("Upstox: ${error.localizedMessage}")
            false
        })
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
            if (!upstoxMarketData.isConfigured) {
                viewModelScope.launch { _snackbarEvent.emit("Save an Upstox access token before starting live background scanning.") }
                return
            }
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
                if (!refreshLiveMarket()) return@launch
                val newSignal = repository.triggerInstantAlert(setupType, _fnoUniverse.value)
                _snackbarEvent.emit("Found setup: ${newSignal.symbol} ${newSignal.strikePrice.toInt()} ${newSignal.optionType} (${newSignal.setupType})")
            } catch (e: Exception) {
                _snackbarEvent.emit("Scan error: ${e.localizedMessage}")
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun saveUpstoxToken(token: String) {
        upstoxMarketData.accessToken = token
        viewModelScope.launch {
            _upstoxStatusMessage.value = "Testing Upstox connection..."
            refreshLiveMarket()
        }
    }

    fun testUpstoxConnection() {
        viewModelScope.launch {
            _upstoxStatusMessage.value = "Testing Upstox connection..."
            upstoxMarketData.testConnection().onSuccess { message ->
                _upstoxStatusMessage.value = "✅ $message"
                refreshLiveMarket(silent = true)
            }.onFailure { error ->
                _isLiveMarketConnected.value = false
                _upstoxStatusMessage.value = "❌ ${error.localizedMessage}"
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
