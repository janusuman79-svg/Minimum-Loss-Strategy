package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.OptionType
import com.example.data.model.SetupType
import com.example.data.model.TradeSignal
import com.example.ui.components.OptionFilterBar
import com.example.ui.components.SignalCard
import com.example.ui.theme.CallGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.SurfaceBorderDark
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevatedDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun IntradayScreen(
    signals: List<TradeSignal>,
    selectedFilter: OptionType?,
    onFilterSelect: (OptionType?) -> Unit,
    isScanning: Boolean,
    onTriggerScan: () -> Unit,
    onSendTelegram: (TradeSignal) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onTrailToCost: ((Long, Double) -> Unit)? = null,
    onOpenCalculator: ((TradeSignal) -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("intraday_screen")
    ) {
        // Option Filter Bar (ALL / CE / PE + Scan Now)
        OptionFilterBar(
            selectedFilter = selectedFilter,
            onFilterSelect = onFilterSelect,
            isScanning = isScanning,
            onTriggerScan = onTriggerScan
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Intraday Methodology Guidance Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceDark)
                        .border(1.dp, GoldAccent.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(GoldAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ElectricBolt,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "INTRADAY LOW-SL STRATEGY (CE/PE)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "• Confluence: 5m/15m VWAP Retest + Open Interest Expansion.\n• Risk Protocol: Tight 6-8% SL. Exit at SL or trail cost after T1 (1:2 R:R achieved).",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            if (signals.isEmpty()) {
                item {
                    EmptySignalsView(
                        title = "No Intraday Signals Found",
                        subtitle = "World's strongest scanner strictly filters false breakouts to safeguard capital. Run a scan to evaluate fresh 15m confluence.",
                        onScan = onTriggerScan
                    )
                }
            } else {
                items(signals, key = { it.id }) { signal ->
                    SignalCard(
                        signal = signal,
                        onSendTelegram = onSendTelegram,
                        onDelete = onDelete,
                        onTrailToCost = onTrailToCost,
                        onOpenCalculator = onOpenCalculator
                    )
                }
            }
        }
    }
}

@Composable
fun EmptySignalsView(
    title: String,
    subtitle: String,
    onScan: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .border(1.dp, SurfaceBorderDark, RoundedCornerShape(16.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceElevatedDark),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Radar,
                    contentDescription = null,
                    tint = CallGreen,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextMuted,
                lineHeight = 17.sp,
                modifier = Modifier.padding(horizontal = 16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(18.dp))
            ElevatedButton(
                onClick = onScan,
                colors = ButtonDefaults.elevatedButtonColors(containerColor = CallGreen, contentColor = androidx.compose.ui.graphics.Color.Black),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(text = "Scan F&O Universe Now", fontWeight = FontWeight.Bold)
            }
        }
    }
}
