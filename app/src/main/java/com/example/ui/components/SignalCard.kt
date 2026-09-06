package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.OptionType
import com.example.data.model.SetupType
import com.example.data.model.TradeSignal
import com.example.ui.theme.CallGreen
import com.example.ui.theme.CallGreenContainer
import com.example.ui.theme.CallGreenLight
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.PutRed
import com.example.ui.theme.PutRedContainer
import com.example.ui.theme.PutRedLight
import com.example.ui.theme.SurfaceBorderDark
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevatedDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// Visual theme constants for Trade Categories
val AmberIntradayBadge = Color(0xFFFF9800)
val AmberIntradayBg = Color(0x33FF9800)
val PurplePositionalBadge = Color(0xFF7C4DFF)
val PurplePositionalBg = Color(0x337C4DFF)

@Composable
fun SignalCard(
    signal: TradeSignal,
    onSendTelegram: (TradeSignal) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onExecuteTrade: ((TradeSignal, String) -> Unit)? = null,
    onTrailToCost: ((Long, Double) -> Unit)? = null,
    onOpenCalculator: ((TradeSignal) -> Unit)? = null
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    var showTradeDialog by remember { mutableStateOf(false) }

    val isIntraday = signal.setupType == SetupType.INTRADAY
    val isCall = signal.optionType == OptionType.CE
    val accentColor = if (isCall) CallGreen else PutRed
    val containerColor = if (isCall) CallGreenContainer else PutRedContainer
    val lightAccent = if (isCall) CallGreenLight else PutRedLight

    val timeFormatted = remember(signal.timestamp) {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(signal.timestamp))
    }

    // Dynamic 3:15 PM IST countdown timer for Intraday cards
    var countdownRemaining by remember { mutableStateOf(calculateTimeUntil315PM()) }
    LaunchedEffect(isIntraday) {
        if (isIntraday) {
            while (true) {
                countdownRemaining = calculateTimeUntil315PM()
                delay(1000)
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("signal_card_${signal.id}")
            .border(
                width = 1.dp,
                color = if (isIntraday) AmberIntradayBadge.copy(alpha = 0.25f) else PurplePositionalBadge.copy(alpha = 0.25f),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: Distinct Header Badge (Orange/Amber for MIS, Deep Blue/Purple for NRML)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Distinct Trade Category Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isIntraday) AmberIntradayBg else PurplePositionalBg)
                            .border(
                                1.dp,
                                if (isIntraday) AmberIntradayBadge else PurplePositionalBadge,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                            .testTag("setup_header_badge_${signal.id}")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isIntraday) Icons.Default.FlashOn else Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = if (isIntraday) AmberIntradayBadge else PurplePositionalBadge,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isIntraday) "INTRADAY (MIS)" else "POSITIONAL (NRML)",
                                color = if (isIntraday) AmberIntradayBadge else PurplePositionalBadge,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Option CE/PE Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(containerColor)
                            .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (isCall) "🟢 CE CALL" else "🔴 PE PUT",
                            color = lightAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = timeFormatted,
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Subheader: Intraday Countdown or Positional Holding Indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceElevatedDark)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isIntraday) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = AmberIntradayBadge,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Auto-Squareoff: $countdownRemaining (3:15 PM IST)",
                                color = AmberIntradayBadge,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = "Weekly Expiry",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = PurplePositionalBadge,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = signal.holdingPeriod,
                                color = PurplePositionalBadge,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = "Monthly Expiry (${signal.expiry})",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Contract Title & Spot Price
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${signal.symbol} ${signal.strikePrice.toInt()} ${signal.optionType}",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(
                        text = "Spot: ₹${String.format(Locale.US, "%.1f", signal.spotPrice)} • Lot: ${signal.lotSize} • Product: ${signal.productType}",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                // Low-SL Confidence Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (signal.confluenceScore >= 80) CallGreen.copy(alpha = 0.15f) else GoldAccent.copy(alpha = 0.15f))
                        .border(1.dp, if (signal.confluenceScore >= 80) CallGreen.copy(alpha = 0.4f) else GoldAccent.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (signal.confluenceScore >= 80) CallGreen else GoldAccent,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${signal.confluenceScore}/100 Institutional",
                            color = if (signal.confluenceScore >= 80) CallGreen else GoldAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Institutional Derivatives Market Structure & Volatility Regime Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceElevatedDark)
                    .border(1.dp, SurfaceBorderDark, RoundedCornerShape(10.dp))
                    .padding(8.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DERIVATIVES MARKET STRUCTURE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "India VIX: ${String.format(Locale.US, "%.1f", signal.indiaVix)} (${signal.vixRegime})",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (signal.indiaVix < 14) CallGreenLight else AmberIntradayBadge
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "PCR (Vol/OI): ${String.format(Locale.US, "%.2f", signal.volumePcr)} / ${String.format(Locale.US, "%.2f", signal.oiPcr)}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Max Pain Strike: ₹${signal.maxPainStrike.toInt()} (Magnet)",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }

                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Call Res: ₹${signal.callOiWall.toInt()} | Put Sup: ₹${signal.putOiWall.toInt()}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Skew: ${signal.ivSkewSentiment}",
                                fontSize = 10.sp,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Order Flow & Microstructure chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(SurfaceDark)
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "VWAP ±1σ: ₹${signal.vwapLowerBand.toInt()} - ₹${signal.vwapUpperBand.toInt()}",
                                fontSize = 9.sp,
                                color = TextSecondary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(SurfaceDark)
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Delta: ${String.format(Locale.US, "%+.0f", signal.orderBookDeltaPercent)}%",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (signal.orderBookDeltaPercent >= 0) CallGreen else PutRed
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(SurfaceDark)
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Vol: ${String.format(Locale.US, "%.1f", signal.volumeSpikeMultiple)}x SMA",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (signal.volumeSpikeMultiple >= 2.0) AmberIntradayBadge else TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Institutional Trade Management: Partial Exits & Theta Protection
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (signal.isThetaDecayRisk) PutRedContainer.copy(alpha = 0.35f) else SurfaceElevatedDark)
                    .border(
                        1.dp,
                        if (signal.isThetaDecayRisk) PutRed.copy(alpha = 0.6f) else SurfaceBorderDark,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🛡️ TRADE & RISK MANAGEMENT",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (signal.isThetaDecayRisk) PutRedLight else CallGreen
                        )
                        if (isIntraday) {
                            Text(
                                text = if (signal.isThetaDecayRisk) "THETA RISK (>45m)" else "Theta: ${signal.thetaDecayMinutesRemaining}m left",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (signal.isThetaDecayRisk) PutRedLight else TextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = signal.partialExitPlan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )

                    if (signal.isThetaDecayRisk) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = PutRedLight,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "45m elapsed without T1: Auto-close or tighten SL to curb option decay!",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = PutRedLight
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Trailed to Cost Zero-Risk Banner
            if (signal.isRiskFree) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CallGreenContainer)
                        .border(1.dp, CallGreen.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = CallGreen,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "🛡️ TRAILED TO COST: ₹${String.format(Locale.US, "%.2f", signal.effectiveStopLoss)} (ZERO RISK)",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Risk: ₹0.00",
                            color = CallGreenLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Advanced Filters Confluence Chips (Gann 360°, Greeks Delta / IVP, Astrology Filter)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceElevatedDark)
                        .border(1.dp, SurfaceBorderDark, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "📐 ${signal.gannLevel}",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceElevatedDark)
                        .border(1.dp, SurfaceBorderDark, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "⚡ Δ ${String.format(Locale.US, "%.2f", signal.delta)} • IVP ${signal.ivPercentile.toInt()}%",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceElevatedDark)
                        .border(1.dp, SurfaceBorderDark, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "🪐 ${signal.astroStatus}",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pricing Matrix
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceElevatedDark)
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    PriceColumn(
                        label = "ENTRY",
                        value = "₹${String.format(Locale.US, "%.2f", signal.entryPrice)}",
                        valueColor = TextPrimary,
                        subText = "Buy Zone"
                    )

                    PriceColumn(
                        label = if (signal.isRiskFree) "ZERO-RISK SL" else if (isIntraday) "TIGHT SL" else "WIDE SL",
                        value = "₹${String.format(Locale.US, "%.2f", signal.effectiveStopLoss)}",
                        valueColor = if (signal.isRiskFree) CallGreen else PutRed,
                        subText = if (signal.isRiskFree) "At Cost (₹0)" else if (isIntraday) "3-5% Risk" else "10-12% Risk"
                    )

                    PriceColumn(
                        label = "TARGET 1",
                        value = "₹${String.format(Locale.US, "%.2f", signal.target1)}",
                        valueColor = CallGreen,
                        subText = signal.targetMultiplierT1
                    )

                    PriceColumn(
                        label = "TARGET 2",
                        value = "₹${String.format(Locale.US, "%.2f", signal.target2)}",
                        valueColor = if (isIntraday) CallGreen else PurplePositionalBadge,
                        subText = signal.targetMultiplierT2
                    )

                    PriceColumn(
                        label = "RUNNER",
                        value = "₹${String.format(Locale.US, "%.2f", signal.target3)}",
                        valueColor = AmberIntradayBadge,
                        subText = "Trailing"
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Target Meters Progress Visual
            TargetMetersBar(
                entryPrice = signal.entryPrice,
                stopLoss = signal.stopLoss,
                target1 = signal.target1,
                target2 = signal.target2,
                currentPrice = signal.currentPrice,
                setupType = signal.setupType
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Risk:Reward & Lot Risk Overview Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Risk:Reward: ${signal.riskRewardRatio} | Max Lot Risk: ₹${String.format(Locale.US, "%,.0f", signal.maxLotRisk)}",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    modifier = Modifier.clickable { isExpanded = !isExpanded },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isExpanded) "Hide Logic" else "Scanner Logic",
                        color = if (isIntraday) AmberIntradayBadge else PurplePositionalBadge,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand",
                        tint = if (isIntraday) AmberIntradayBadge else PurplePositionalBadge,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Expandable Confluence reasons
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceDark.copy(alpha = 0.5f))
                        .border(1.dp, SurfaceBorderDark, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = if (isIntraday) "INTRADAY CONFLUENCE (3m/15m ORB + VWAP):" else "POSITIONAL CONFLUENCE (Daily Supertrend + OI):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isIntraday) AmberIntradayBadge else PurplePositionalBadge
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    signal.scannerReasons.forEach { reason ->
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(text = "✓ ", color = CallGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(text = reason, color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Row: "1-Click Trade" (Mapped to MIS / NRML) + Telegram + Copy + Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1-Click Trade Execution Button
                Button(
                    onClick = { showTradeDialog = true },
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("one_click_trade_button_${signal.id}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isIntraday) AmberIntradayBadge else PurplePositionalBadge,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = "Trade",
                        modifier = Modifier.size(14.dp),
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "1-Click Trade (${signal.productType})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Telegram Push Button
                    FilledTonalButton(
                        onClick = { onSendTelegram(signal) },
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("send_telegram_button_${signal.id}"),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (signal.isTelegramSent) CallGreenContainer else SurfaceElevatedDark,
                            contentColor = if (signal.isTelegramSent) CallGreenLight else TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = if (signal.isTelegramSent) Icons.Default.CheckCircle else Icons.Default.Send,
                            contentDescription = "Telegram",
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Copy Alert Button
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Signal", formatCopyText(signal))
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied signal details (${signal.productType})", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("copy_signal_button_${signal.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Delete Button
                    IconButton(
                        onClick = { onDelete(signal.id) },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("delete_signal_button_${signal.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Execution & Risk Management Actions: "TRAIL TO COST" & "AUTO SIZING"
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!signal.isRiskFree && onTrailToCost != null) {
                    OutlinedButton(
                        onClick = { onTrailToCost(signal.id, signal.entryPrice) },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("trail_to_cost_button_${signal.id}"),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = CallGreen
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CallGreen.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = CallGreen
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "TRAIL TO COST",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (onOpenCalculator != null) {
                    OutlinedButton(
                        onClick = { onOpenCalculator(signal) },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("open_calculator_button_${signal.id}"),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = GoldAccent
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = GoldAccent
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "AUTO SIZING",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // 1-Click Order Confirmation Dialog
    if (showTradeDialog) {
        AlertDialog(
            onDismissRequest = { showTradeDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = if (isIntraday) AmberIntradayBadge else PurplePositionalBadge
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "1-Click Order (${signal.productType})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Broker API Dispatch Payload:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceElevatedDark)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = signal.toBrokerOrderPayload(),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isIntraday)
                            "• Product MIS automatically applies for Intraday setup with auto-squareoff at 3:15 PM IST."
                        else
                            "• Product NRML automatically applies for Positional swing trade holding up to monthly expiry.",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showTradeDialog = false
                        onExecuteTrade?.invoke(signal, signal.productType)
                        Toast.makeText(
                            context,
                            "Order Placed with Broker! Product: ${signal.productType} • Price: ₹${signal.entryPrice}",
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isIntraday) AmberIntradayBadge else PurplePositionalBadge,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Confirm Buy Order", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showTradeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun TargetMetersBar(
    entryPrice: Double,
    stopLoss: Double,
    target1: Double,
    target2: Double,
    currentPrice: Double,
    setupType: SetupType
) {
    val isIntraday = setupType == SetupType.INTRADAY
    val totalRange = (target2 - stopLoss).coerceAtLeast(0.1)
    val progress = ((currentPrice - stopLoss) / totalRange).coerceIn(0.0, 1.0).toFloat()

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (isIntraday) "TARGET METERS (1:1.5 | 1:2.5)" else "TARGET METERS (1:2.0 | 1:4.0)",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted
            )
            Text(
                text = "LTP: ₹${String.format(Locale.US, "%.2f", currentPrice)}",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (currentPrice >= entryPrice) CallGreen else PutRed
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = if (isIntraday) AmberIntradayBadge else PurplePositionalBadge,
            trackColor = SurfaceElevatedDark
        )
    }
}

@Composable
private fun PriceColumn(
    label: String,
    value: String,
    valueColor: Color,
    subText: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            color = valueColor
        )
        Text(
            text = subText,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = TextMuted
        )
    }
}

private fun calculateTimeUntil315PM(): String {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"))
    val target = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata")).apply {
        set(Calendar.HOUR_OF_DAY, 15)
        set(Calendar.MINUTE, 15)
        set(Calendar.SECOND, 0)
    }

    var diff = target.timeInMillis - cal.timeInMillis
    if (diff <= 0) {
        return "Market Closed"
    }

    val hours = diff / (1000 * 60 * 60)
    diff %= (1000 * 60 * 60)
    val mins = diff / (1000 * 60)
    diff %= (1000 * 60)
    val secs = diff / 1000

    return String.format(Locale.US, "%02dh %02dm %02ds", hours, mins, secs)
}

private fun formatCopyText(signal: TradeSignal): String {
    return """
NSE F&O Alert: ${signal.symbol} ${signal.strikePrice.toInt()} ${signal.optionType}
Setup: ${if (signal.setupType == SetupType.INTRADAY) "INTRADAY (MIS)" else "POSITIONAL (NRML)"}
Product: ${signal.productType}
Entry: ₹${signal.entryPrice}
SL: ${if (signal.isRiskFree) "🛡️ ₹${signal.effectiveStopLoss} (Trailed to Cost - Zero Risk)" else "₹${signal.effectiveStopLoss} (Risk: ₹${signal.riskAmount})"}
Target 1 (${signal.targetMultiplierT1}): ₹${signal.target1}
Target 2 (${signal.targetMultiplierT2}): ₹${signal.target2}
Runner: ₹${signal.target3}
Greeks: Delta ${String.format(Locale.US, "%.2f", signal.delta)} | IVP: ${signal.ivPercentile.toInt()}%
Gann 360°: ${signal.gannLevel} | Astro: ${signal.astroStatus}
R:R: ${signal.riskRewardRatio} | Lot: ${signal.lotSize}
Holding/Expiry: ${signal.holdingPeriod}
    """.trimIndent()
}
