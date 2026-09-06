package com.example.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.data.repository.SignalRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TrailToCostReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_TRAIL_TO_COST) {
            val signalId = intent.getLongExtra(EXTRA_SIGNAL_ID, 0L)
            val entryPrice = intent.getDoubleExtra(EXTRA_ENTRY_PRICE, 0.0)
            val symbol = intent.getStringExtra(EXTRA_SYMBOL) ?: "Contract"

            if (signalId > 0 && entryPrice > 0) {
                val repository = SignalRepository(context.applicationContext)
                CoroutineScope(Dispatchers.IO).launch {
                    repository.trailStopLossToCost(signalId, entryPrice)

                    // Post updated status notification
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    val updatedNotification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_SIGNALS_ID)
                        .setContentTitle("🛡️ [ZERO RISK] $symbol Trailed to Cost")
                        .setContentText("Stop-loss locked at entry price (₹${String.format(java.util.Locale.US, "%.2f", entryPrice)}). Downside eliminated!")
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .build()

                    notificationManager.notify(signalId.toInt(), updatedNotification)

                    // Show toast on Main thread
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(
                            context.applicationContext,
                            "🛡️ Stop-loss for $symbol trailed to breakeven (₹${String.format(java.util.Locale.US, "%.2f", entryPrice)}) - ZERO RISK!",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_TRAIL_TO_COST = "com.example.ACTION_TRAIL_TO_COST"
        const val EXTRA_SIGNAL_ID = "EXTRA_SIGNAL_ID"
        const val EXTRA_ENTRY_PRICE = "EXTRA_ENTRY_PRICE"
        const val EXTRA_SYMBOL = "EXTRA_SYMBOL"
    }
}
