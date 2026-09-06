package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.OptionType
import com.example.data.model.SetupType
import com.example.data.model.TradeSignal
import com.example.ui.theme.CallGreen
import com.example.ui.theme.CallGreenContainer
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.PutRed
import com.example.ui.theme.SurfaceBorderDark
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevatedDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PositionCalculatorDialog(
    signal: TradeSignal,
    initialCapital: Double = 50000.0,
    initialRiskPct: Double = 2.0,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var capitalInput by remember { mutableStateOf(initialCapital.toInt().toString()) }
    var riskPercent by remember { mutableDoubleStateOf(initialRiskPct) }

    val capital = capitalInput.toDoubleOrNull() ?: 50000.0
    val isCall = signal.optionType == OptionType.CE
    val isIntraday = signal.setupType == SetupType.INTRADAY

    // Sizing Calculation Model
    val maxAllowedRiskRupees = capital * (riskPercent / 100.0)
    val riskPerShare = if (signal.isRiskFree) 0.0 else max(signal.effectiveRiskAmount, 0.1)
    val riskPerLot = riskPerShare * signal.lotSize
    val costPerLot = signal.entryPrice * signal.lotSize

    // Number of lots allowed based on maximum risk tolerance
    val lotsByRisk = if (riskPerLot > 0) {
        max(1, floor(maxAllowedRiskRupees / riskPerLot).toInt())
    } else {
        1
    }

    // Number of lots bounded by total capital
    val lotsByCapital = if (costPerLot > 0) {
        max(1, floor(capital / costPerLot).toInt())
    } else {
        1
    }

    val recommendedLots = min(lotsByRisk, lotsByCapital)
    val totalQuantity = recommendedLots * signal.lotSize
    val capitalRequired = totalQuantity * signal.entryPrice
    val totalActualRisk = if (signal.isRiskFree) 0.0 else totalQuantity * riskPerShare
    val actualRiskPct = if (capital > 0) (totalActualRisk / capital) * 100.0 else 0.0

    val potentialProfitT1 = totalQuantity * (signal.target1 - signal.entryPrice)
    val potentialProfitT2 = totalQuantity * (signal.target2 - signal.entryPrice)

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("position_calculator_dialog"),
        containerColor = SurfaceDark,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(GoldAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Position Sizing Calculator",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${signal.symbol} ${signal.strikePrice.toInt()} ${signal.optionType} (${signal.productType})",
                            color = if (isCall) CallGreen else PutRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Trailed to Cost Banner
                if (signal.isRiskFree) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CallGreenContainer)
                            .border(1.dp, CallGreen, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = CallGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "TRAILED TO COST: Risk is ₹0.00 (Breakeven Locked!)",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Contract Basics Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceElevatedDark)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "ENTRY", color = TextMuted, fontSize = 10.sp)
                        Text(text = "₹${String.format(Locale.US, "%.2f", signal.entryPrice)}", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text(text = "STOP LOSS", color = TextMuted, fontSize = 10.sp)
                        Text(text = "₹${String.format(Locale.US, "%.2f", signal.effectiveStopLoss)}", color = PutRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text(text = "LOT SIZE", color = TextMuted, fontSize = 10.sp)
                        Text(text = "${signal.lotSize} qty", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text(text = "RISK/SHARE", color = TextMuted, fontSize = 10.sp)
                        Text(
                            text = if (signal.isRiskFree) "₹0.00" else "₹${String.format(Locale.US, "%.2f", signal.effectiveRiskAmount)}",
                            color = if (signal.isRiskFree) CallGreen else PutRed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Account Trading Capital Input
                Text(
                    text = "Total Account Trading Capital (₹)",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = capitalInput,
                    onValueChange = { capitalInput = it.filter { ch -> ch.isDigit() } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("capital_input_field"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldAccent,
                        unfocusedBorderColor = SurfaceBorderDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )

                // Quick Capital Preset Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(25000, 50000, 100000, 200000).forEach { preset ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (capital.toInt() == preset) GoldAccent.copy(alpha = 0.2f) else SurfaceElevatedDark)
                                .border(
                                    1.dp,
                                    if (capital.toInt() == preset) GoldAccent else SurfaceBorderDark,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { capitalInput = preset.toString() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "₹${preset / 1000}k",
                                color = if (capital.toInt() == preset) GoldAccent else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Max Risk % Per Trade
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Max Risk Per Trade: ${String.format(Locale.US, "%.1f", riskPercent)}%",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Max ₹${String.format(Locale.US, "%,.0f", maxAllowedRiskRupees)}",
                        color = PutRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Slider(
                    value = riskPercent.toFloat(),
                    onValueChange = { riskPercent = ((it * 10).toInt() / 10.0).coerceIn(0.5, 5.0) },
                    valueRange = 0.5f..5.0f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = GoldAccent,
                        activeTrackColor = GoldAccent,
                        inactiveTrackColor = SurfaceElevatedDark
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Recommended Sizing Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceElevatedDark),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(GoldAccent.copy(alpha = 0.5f)))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "RECOMMENDED SIZING",
                                color = GoldAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(GoldAccent)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "$recommendedLots LOTS ($totalQuantity QTY)",
                                    color = Color.Black,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "Capital Required", color = TextMuted, fontSize = 11.sp)
                                Text(
                                    text = "₹${String.format(Locale.US, "%,.1f", capitalRequired)}",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "Total Risk Exposure", color = TextMuted, fontSize = 11.sp)
                                Text(
                                    text = if (signal.isRiskFree) "₹0 (Zero Risk)" else "₹${String.format(Locale.US, "%,.1f", totalActualRisk)} (${String.format(Locale.US, "%.1f", actualRiskPct)}%)",
                                    color = if (signal.isRiskFree) CallGreen else PutRed,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "Profit @ Target 1 (1:1.5)", color = TextMuted, fontSize = 11.sp)
                                Text(
                                    text = "+₹${String.format(Locale.US, "%,.1f", potentialProfitT1)}",
                                    color = CallGreen,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "Profit @ Target 2 (${signal.targetMultiplierT2})", color = TextMuted, fontSize = 11.sp)
                                Text(
                                    text = "+₹${String.format(Locale.US, "%,.1f", potentialProfitT2)}",
                                    color = CallGreen,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText(
                        "Position Sizing",
                        """
                        🎯 ${signal.symbol} ${signal.strikePrice.toInt()} ${signal.optionType} SIZING:
                        • Capital: ₹${String.format(Locale.US, "%,.0f", capital)}
                        • Recommended: $recommendedLots Lots ($totalQuantity Qty)
                        • Margin: ₹${String.format(Locale.US, "%,.1f", capitalRequired)}
                        • Max Risk: ₹${String.format(Locale.US, "%,.1f", totalActualRisk)}
                        • Profit @ T1: +₹${String.format(Locale.US, "%,.1f", potentialProfitT1)}
                        • Profit @ T2: +₹${String.format(Locale.US, "%,.1f", potentialProfitT2)}
                        """.trimIndent()
                    )
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Copied Sizing ($recommendedLots Lots) to clipboard!", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldAccent,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color.Black
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copy Sizing ($recommendedLots Lots)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary)
            }
        }
    )
}
