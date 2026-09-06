package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.repository.SignalRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ScannerForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var scanJob: Job? = null
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var repository: SignalRepository
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        notificationHelper = NotificationHelper(this)
        repository = SignalRepository(this)

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "NseScanner::BackgroundWakeLock"
        )
        wakeLock?.acquire(10 * 60 * 1000L) // Safe 10min buffer
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        when (action) {
            ACTION_STOP -> {
                stopForegroundScanner()
            }
            ACTION_TRIGGER_NOW -> {
                triggerManualScanNow()
            }
            ACTION_START -> {
                startForegroundScanner()
            }
        }

        return START_STICKY
    }

    private fun startForegroundScanner() {
        val notification = notificationHelper.buildForegroundNotification("Monitoring 190+ NSE F&O Stocks (Background Mode)...")
        startForeground(NotificationHelper.FOREGROUND_NOTIFICATION_ID, notification)
        _isServiceRunning.value = true

        if (scanJob == null || scanJob?.isActive == false) {
            scanJob = serviceScope.launch {
                while (isActive) {
                    try {
                        _lastScanTime.value = System.currentTimeMillis()
                        // Scan for low-SL high probability CE/PE options
                        val newSignals = repository.runFullScanAndSave(minConfidence = 88)
                        if (newSignals.isNotEmpty()) {
                            // Pick the strongest signal to notify immediately
                            val topSignal = newSignals.maxByOrNull { it.confidenceScore }
                            if (topSignal != null) {
                                notificationHelper.showSignalNotification(topSignal)
                                _totalAlertsSent.value += 1
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("ScannerService", "Error during background scan", e)
                    }

                    // Background cycle delay (e.g. 45 seconds)
                    delay(45_000L)
                }
            }
        }
    }

    private fun triggerManualScanNow() {
        serviceScope.launch {
            try {
                _lastScanTime.value = System.currentTimeMillis()
                val signal = repository.triggerInstantAlert()
                notificationHelper.showSignalNotification(signal)
                _totalAlertsSent.value += 1
            } catch (e: Exception) {
                Log.e("ScannerService", "Manual scan failed", e)
            }
        }
    }

    private fun stopForegroundScanner() {
        scanJob?.cancel()
        scanJob = null
        _isServiceRunning.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        scanJob?.cancel()
        serviceScope.cancel()
        _isServiceRunning.value = false
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.e("ScannerService", "Failed to release wake lock", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"
        const val ACTION_TRIGGER_NOW = "com.example.service.ACTION_TRIGGER_NOW"

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        private val _lastScanTime = MutableStateFlow(System.currentTimeMillis())
        val lastScanTime: StateFlow<Long> = _lastScanTime.asStateFlow()

        private val _totalAlertsSent = MutableStateFlow(0)
        val totalAlertsSent: StateFlow<Int> = _totalAlertsSent.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, ScannerForegroundService::class.java).apply {
                action = ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, ScannerForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun triggerNow(context: Context) {
            val intent = Intent(context, ScannerForegroundService::class.java).apply {
                action = ACTION_TRIGGER_NOW
            }
            context.startService(intent)
        }
    }
}
