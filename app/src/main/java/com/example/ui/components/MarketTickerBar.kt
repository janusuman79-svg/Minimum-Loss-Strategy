package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MarketIndices
import com.example.ui.theme.CallGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.PutRed
import com.example.ui.theme.SurfaceBorderDark
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevatedDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun MarketTickerBar(
    indices: MarketIndices,
    isLiveMarketConnected: Boolean,
    isBackgroundRunning: Boolean,
    onToggleBackground: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("market_ticker_bar"),
        color = SurfaceDark,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Header Row: App Title & Background Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CallGreen.copy(alpha = 0.15f))
                            .border(1.dp, CallGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = "App Icon",
                            tint = CallGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "NSE F&O SCANNER",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val dotColor = if (isLiveMarketConnected) CallGreen else PutRed
                            val transition = rememberInfiniteTransition(label = "pulse")
                            val pulseScale by transition.animateFloat(
                                initialValue = 0.8f,
                                targetValue = 1.3f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(800),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "pulseScale"
                            )

                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .scale(if (isBackgroundRunning) pulseScale else 1.0f)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isLiveMarketConnected) "LIVE • Upstox" else "OFFLINE • Configure Upstox",
                                color = if (isLiveMarketConnected) CallGreen else PutRed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Background Service Switch
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceElevatedDark)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "BG SCAN",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isBackgroundRunning) CallGreen else TextMuted
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(
                        checked = isBackgroundRunning,
                        onCheckedChange = { onToggleBackground() },
                        modifier = Modifier
                            .scale(0.75f)
                            .testTag("toggle_background_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = CallGreen,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = SurfaceBorderDark
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Market Ticker Badges: NIFTY 50, BANKNIFTY, PCR, FII/DII
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IndexChip(
                    name = "NIFTY",
                    value = String.format(Locale.US, "%,.1f", indices.niftySpot),
                    change = "${if (indices.niftyChange >= 0) "+" else ""}${String.format(Locale.US, "%.1f", indices.niftyChange)} (${String.format(Locale.US, "%.2f", indices.niftyChangePercent)}%)",
                    isPositive = indices.niftyChange >= 0,
                    modifier = Modifier.weight(1f)
                )
                IndexChip(
                    name = "BANKNIFTY",
                    value = String.format(Locale.US, "%,.1f", indices.bankNiftySpot),
                    change = "${if (indices.bankNiftyChange >= 0) "+" else ""}${String.format(Locale.US, "%.1f", indices.bankNiftyChange)} (${String.format(Locale.US, "%.2f", indices.bankNiftyChangePercent)}%)",
                    isPositive = indices.bankNiftyChange >= 0,
                    modifier = Modifier.weight(1f)
                )
                SentimentBadge(
                    pcr = indices.pcr,
                    fiiNetCr = indices.fiiNetFlowCr,
                    modifier = Modifier.weight(0.9f)
                )
            }
        }
    }
}

@Composable
private fun IndexChip(
    name: String,
    value: String,
    change: String,
    isPositive: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceElevatedDark)
            .border(1.dp, SurfaceBorderDark, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column {
            Text(
                text = name,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextMuted
            )
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = TextPrimary
            )
            Text(
                text = change,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isPositive) CallGreen else PutRed
            )
        }
    }
}

@Composable
private fun SentimentBadge(
    pcr: Double,
    fiiNetCr: Double,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceElevatedDark)
            .border(1.dp, SurfaceBorderDark, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column {
            Text(
                text = "PCR / FII FLOW",
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextMuted
            )
            Text(
                text = "PCR $pcr (Bullish)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CallGreen
            )
            Text(
                text = "+₹${fiiNetCr.toInt()} Cr FII",
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
        }
    }
}
