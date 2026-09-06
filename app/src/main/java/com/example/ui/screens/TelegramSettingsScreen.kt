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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.telegram.TelegramManager
import com.example.ui.theme.BluePositional
import com.example.ui.theme.BluePositionalContainer
import com.example.ui.theme.CallGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.PutRed
import com.example.ui.theme.SurfaceBorderDark
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevatedDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun TelegramSettingsScreen(
    telegramManager: TelegramManager,
    isBackgroundRunning: Boolean,
    onToggleBackground: () -> Unit,
    statusMessage: String?,
    onSaveSettings: (botToken: String, chatId: String, autoSend: Boolean, minConfidence: Int) -> Unit,
    onTestConnection: () -> Unit,
    modifier: Modifier = Modifier
) {
    var botToken by remember { mutableStateOf(telegramManager.botToken) }
    var chatId by remember { mutableStateOf(telegramManager.chatId) }
    var autoSend by remember { mutableStateOf(telegramManager.isAutoSendEnabled) }
    var minConfidence by remember { mutableFloatStateOf(telegramManager.minConfidenceToSend.toFloat()) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("telegram_settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Background Service Status & Control Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceDark)
                    .border(
                        1.dp,
                        if (isBackgroundRunning) CallGreen.copy(alpha = 0.5f) else SurfaceBorderDark,
                        RoundedCornerShape(14.dp)
                    )
                    .padding(14.dp)
            ) {
                Column {
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
                                    .background(if (isBackgroundRunning) CallGreen.copy(alpha = 0.15f) else SurfaceElevatedDark),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = if (isBackgroundRunning) CallGreen else TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Background Scanning Service",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (isBackgroundRunning) "Running in background & minimized" else "Stopped (foreground only)",
                                    fontSize = 11.sp,
                                    color = if (isBackgroundRunning) CallGreen else TextMuted
                                )
                            }
                        }

                        Switch(
                            checked = isBackgroundRunning,
                            onCheckedChange = { onToggleBackground() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CallGreen,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SurfaceBorderDark
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Allows scanner to continuously monitor F&O stocks while your phone is locked or app is minimized, and send alerts instantly.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // Telegram Bot Configuration Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceDark)
                    .border(1.dp, BluePositional.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BluePositionalContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = null,
                                tint = BluePositional,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Telegram Bot Integration",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Receive CE/PE alerts in your Telegram channel or chat",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Bot Token Field
                    OutlinedTextField(
                        value = botToken,
                        onValueChange = { botToken = it },
                        label = { Text("Telegram Bot Token (from @BotFather)") },
                        placeholder = { Text("e.g. 7123456789:AAHK...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("telegram_token_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BluePositional,
                            unfocusedBorderColor = SurfaceBorderDark,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Chat ID Field
                    OutlinedTextField(
                        value = chatId,
                        onValueChange = { chatId = it },
                        label = { Text("Chat ID or Channel ID") },
                        placeholder = { Text("e.g. -100123456789 or @YourChannel") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("telegram_chat_id_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BluePositional,
                            unfocusedBorderColor = SurfaceBorderDark,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Auto Send Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Auto-Broadcast Alerts",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Instantly push to Telegram when scanner detects setup",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }

                        Switch(
                            checked = autoSend,
                            onCheckedChange = { autoSend = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = BluePositional,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SurfaceBorderDark
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Min Confidence Threshold Slider (Low-SL guarantee)
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Minimum Confluence Confidence",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "${minConfidence.toInt()}% (Ultra Low-SL)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CallGreen
                            )
                        }

                        Slider(
                            value = minConfidence,
                            onValueChange = { minConfidence = it },
                            valueRange = 80f..98f,
                            steps = 18,
                            colors = SliderDefaults.colors(
                                thumbColor = CallGreen,
                                activeTrackColor = CallGreen,
                                inactiveTrackColor = SurfaceBorderDark
                            )
                        )
                        Text(
                            text = "Higher score filters out 90%+ of false breakouts and guarantees minimal stop losses.",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Save & Test Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ElevatedButton(
                            onClick = {
                                onSaveSettings(botToken, chatId, autoSend, minConfidence.toInt())
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("save_telegram_settings_button"),
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = BluePositional,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(text = "Save Settings", fontWeight = FontWeight.Bold)
                        }

                        ElevatedButton(
                            onClick = onTestConnection,
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("test_telegram_button"),
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = SurfaceElevatedDark,
                                contentColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(text = "Test Connection", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (statusMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = statusMessage,
                            fontSize = 11.sp,
                            color = if (statusMessage.startsWith("✅")) CallGreen else PutRed,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Live Telegram Message Format Preview
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceDark)
                    .border(1.dp, SurfaceBorderDark, RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = "TELEGRAM ALERT PREVIEW",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceElevatedDark)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = """
🚨 NSE F&O OPTIONS SCANNER ALERT 🚨
━━━━━━━━━━━━━━━━━━━━━
🎯 Setup: ⚡ INTRADAY HIGH-MOMENTUM
📌 Contract: RELIANCE 2980 CE
🏷 Type: 🟢 CALL (CE)
📅 Expiry: CURRENT | Lot: 250
📊 Spot Price: ₹2,980.00
━━━━━━━━━━━━━━━━━━━━━
💰 ENTRY ZONE: ₹65.50
🛑 STOP LOSS (Tight): ₹60.50 (Risk: ₹5.00/sh)
🎯 TARGET 1 (1:2): ₹75.50
🎯 TARGET 2 (1:3.5): ₹83.00
🎯 TARGET 3 (Runner): ₹90.50
⚖️ Risk:Reward: 1:2.0 | Max Lot Risk: ₹1,250
━━━━━━━━━━━━━━━━━━━━━
🛡️ LOW-SL CONFLUENCE: 94% Quality
 • Institutional Long Buildup: OI +28.6%
 • VWAP Bounce Confirmation (Spot > ₹2968)
 • Volume Surge: 3.4x 20-Day SMA
━━━━━━━━━━━━━━━━━━━━━
⚠️ Strictly follow SL. Trail SL to Cost after T1.
                            """.trimIndent(),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = TextSecondary,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}
