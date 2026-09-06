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
import androidx.compose.material.icons.filled.CalendarMonth
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
import com.example.data.model.TradeSignal
import com.example.ui.components.OptionFilterBar
import com.example.ui.components.SignalCard
import com.example.ui.theme.BluePositional
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextSecondary

@Composable
fun PositionalScreen(
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
            .testTag("positional_screen")
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
            // Positional Swing Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceDark)
                        .border(1.dp, BluePositional.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(BluePositional.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = BluePositional,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "POSITIONAL MULTI-DAY SWING SETUP",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BluePositional
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "• Confluence: Daily + Hourly Supertrend Alignment with Heavy Multi-day OI Support.\n• Targets: 1:2.5 to 1:6.5+ risk-reward for capturing macro stock breakouts.",
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
                        title = "No Positional Signals Found",
                        subtitle = "World's strongest scanner looks for high-grade institutional accumulation. Hit scan to detect multi-day swing candidates.",
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
