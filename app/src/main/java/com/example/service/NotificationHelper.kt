package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.OptionType
import com.example.data.model.SetupType
import com.example.data.model.TradeSignal
import java.util.Locale

class NotificationHelper(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // High importance channel for Live CE/PE Signals
            val signalChannel = NotificationChannel(
                CHANNEL_SIGNALS_ID,
                context.getString(R.string.scanner_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.scanner_channel_description)
                enableLights(true)
                lightColor = Color.GREEN
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 150, 300)
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            // Foreground ongoing service channel
            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE_ID,
                context.getString(R.string.foreground_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.foreground_channel_description)
                setShowBadge(false)
            }

            notificationManager.createNotificationChannel(signalChannel)
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }

    fun buildForegroundNotification(statusText: String = "Scanning 190+ NSE F&O Stocks in background..."): Notification {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_SERVICE_ID)
            .setContentTitle("🟢 NSE Options Scanner Active")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    fun showSignalNotification(signal: TradeSignal) {
        // Direct FCM / Local Trigger Wake-up: Bypass Doze mode and wake device screen
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            @Suppress("DEPRECATION")
            val wakeLock = powerManager?.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "NseScanner:ScreenWakeLock"
            )
            wakeLock?.acquire(3500L)
        } catch (_: Exception) {
            try {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                val partialWakeLock = powerManager?.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "NseScanner:PartialWakeLock"
                )
                partialWakeLock?.acquire(3500L)
            } catch (_: Exception) {}
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("SIGNAL_ID", signal.id)
            putExtra("SETUP_TYPE", signal.setupType.name)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            signal.id.toInt(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val emoji = if (signal.optionType == OptionType.CE) "🟢 [CALL CE]" else "🔴 [PUT PE]"
        val setupTag = if (signal.setupType == SetupType.INTRADAY) "INTRADAY (MIS)" else "POSITIONAL (NRML)"
        val riskTag = if (signal.isRiskFree) " [ZERO RISK]" else ""
        val title = "$emoji $setupTag$riskTag: ${signal.symbol} ${signal.strikePrice.toInt()} ${signal.optionType}"

        val shortSummary = if (signal.isRiskFree) {
            "🛡️ Trailed to Cost: ₹${String.format(Locale.US, "%.1f", signal.effectiveStopLoss)} (ZERO RISK) | T2: ₹${String.format(Locale.US, "%.1f", signal.target2)}"
        } else if (signal.setupType == SetupType.INTRADAY) {
            "Entry: ₹${String.format(Locale.US, "%.1f", signal.entryPrice)} | Tight SL: ₹${String.format(Locale.US, "%.1f", signal.stopLoss)} (<5%) | T2: ₹${String.format(Locale.US, "%.1f", signal.target2)} (1:2.5)"
        } else {
            "Entry: ₹${String.format(Locale.US, "%.1f", signal.entryPrice)} | SL: ₹${String.format(Locale.US, "%.1f", signal.stopLoss)} | T2: ₹${String.format(Locale.US, "%.1f", signal.target2)} (1:4 Monthly)"
        }

        val bigText = buildString {
            append("• Contract: ${signal.symbol} ${signal.strikePrice.toInt()} ${signal.optionType} (${signal.expiry})\n")
            append("• Entry Zone: ₹${String.format(Locale.US, "%.2f", signal.entryPrice)}\n")
            if (signal.isRiskFree) {
                append("• Stop Loss: 🛡️ ₹${String.format(Locale.US, "%.2f", signal.effectiveStopLoss)} (Trailed to Breakeven - Zero Risk!)\n")
            } else {
                append("• Stop Loss: ₹${String.format(Locale.US, "%.2f", signal.stopLoss)} (Risk: ₹${String.format(Locale.US, "%.2f", signal.riskAmount)})\n")
            }
            append("• Target 1: ₹${String.format(Locale.US, "%.2f", signal.target1)} | Target 2: ₹${String.format(Locale.US, "%.2f", signal.target2)}\n")
            append("• Target 3: ₹${String.format(Locale.US, "%.2f", signal.target3)}\n")
            append("• Greeks: Delta ${String.format(Locale.US, "%.2f", signal.delta)} | IVP: ${signal.ivPercentile.toInt()}%\n")
            append("• Gann 360°: ${signal.gannLevel} | Astro: ${signal.astroStatus}\n")
            append("• Lot Size: ${signal.lotSize} | Max Lot Risk: ₹${String.format(Locale.US, "%.0f", signal.maxLotRisk)}\n")
            append("• Confluences: ${signal.scannerReasons.joinToString("; ")}")
        }

        val trailIntent = Intent(context, TrailToCostReceiver::class.java).apply {
            action = TrailToCostReceiver.ACTION_TRAIL_TO_COST
            putExtra(TrailToCostReceiver.EXTRA_SIGNAL_ID, signal.id)
            putExtra(TrailToCostReceiver.EXTRA_ENTRY_PRICE, signal.entryPrice)
            putExtra(TrailToCostReceiver.EXTRA_SYMBOL, signal.symbol)
        }
        val trailPendingIntent = PendingIntent.getBroadcast(
            context,
            signal.id.toInt() + 200000,
            trailIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_SIGNALS_ID)
            .setContentTitle(title)
            .setContentText(shortSummary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText).setSummaryText("Direct FCM / Scanner Signal"))
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(pendingIntent, true)
            .setVibrate(longArrayOf(0, 400, 200, 400))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))

        if (!signal.isRiskFree) {
            notificationBuilder.addAction(
                android.R.drawable.ic_menu_rotate,
                "🛡️ TRAIL TO COST (₹${String.format(Locale.US, "%.1f", signal.entryPrice)})",
                trailPendingIntent
            )
        }

        val notification = notificationBuilder.build()

        notificationManager.notify(signal.id.toInt().let { if (it == 0) System.currentTimeMillis().toInt() else it }, notification)
    }

    companion object {
        const val CHANNEL_SIGNALS_ID = "nse_scanner_signals_channel"
        const val CHANNEL_SERVICE_ID = "nse_scanner_foreground_channel"
        const val FOREGROUND_NOTIFICATION_ID = 1001
    }
}
