package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.SetupType
import com.example.ui.AppTab
import com.example.ui.MainViewModel
import com.example.ui.components.MarketTickerBar
import com.example.ui.components.PositionCalculatorDialog
import com.example.ui.screens.IntradayScreen
import com.example.ui.screens.PerformanceScreen
import com.example.ui.screens.PositionalScreen
import com.example.ui.screens.ScannerRadarScreen
import com.example.ui.screens.TelegramSettingsScreen
import com.example.ui.theme.CallGreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SurfaceBorderDark
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle navigation intent if launched from notification
        val setupExtra = intent?.getStringExtra("SETUP_TYPE")
        if (setupExtra == SetupType.POSITIONAL.name) {
            viewModel.selectTab(AppTab.POSITIONAL)
        } else if (setupExtra == SetupType.INTRADAY.name) {
            viewModel.selectTab(AppTab.INTRADAY)
        }

        setContent {
            MyApplicationTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(viewModel: MainViewModel) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val optionFilter by viewModel.optionTypeFilter.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val marketIndices by viewModel.marketIndices.collectAsStateWithLifecycle()
    val fnoUniverse by viewModel.fnoUniverse.collectAsStateWithLifecycle()
    val upstoxStatus by viewModel.upstoxStatusMessage.collectAsStateWithLifecycle()
    val isLiveMarketConnected by viewModel.isLiveMarketConnected.collectAsStateWithLifecycle()
    val intradaySignals by viewModel.intradaySignals.collectAsStateWithLifecycle()
    val positionalSignals by viewModel.positionalSignals.collectAsStateWithLifecycle()
    val allSignals by viewModel.allSignals.collectAsStateWithLifecycle()
    val isBgRunning by viewModel.isBackgroundServiceRunning.collectAsStateWithLifecycle()
    val telegramStatus by viewModel.telegramStatusMessage.collectAsStateWithLifecycle()
    val selectedSignalForCalc by viewModel.selectedSignalForCalculator.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // Request Notification permission for Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            MarketTickerBar(
                indices = marketIndices,
                isLiveMarketConnected = isLiveMarketConnected,
                isBackgroundRunning = isBgRunning,
                onToggleBackground = { viewModel.toggleBackgroundService() }
            )
        },
        bottomBar = {
            AppBottomNav(
                currentTab = selectedTab,
                onTabSelect = { viewModel.selectTab(it) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = SurfaceDark
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                AppTab.INTRADAY -> {
                    IntradayScreen(
                        signals = intradaySignals,
                        selectedFilter = optionFilter,
                        onFilterSelect = { viewModel.setOptionFilter(it) },
                        isScanning = isScanning,
                        onTriggerScan = { viewModel.triggerScanNow(SetupType.INTRADAY) },
                        onSendTelegram = { viewModel.sendToTelegram(it) },
                        onDelete = { viewModel.deleteSignal(it) },
                        onTrailToCost = { id, entry -> viewModel.trailStopLossToCost(id, entry) },
                        onOpenCalculator = { signal -> viewModel.openCalculator(signal) }
                    )
                }
                AppTab.POSITIONAL -> {
                    PositionalScreen(
                        signals = positionalSignals,
                        selectedFilter = optionFilter,
                        onFilterSelect = { viewModel.setOptionFilter(it) },
                        isScanning = isScanning,
                        onTriggerScan = { viewModel.triggerScanNow(SetupType.POSITIONAL) },
                        onSendTelegram = { viewModel.sendToTelegram(it) },
                        onDelete = { viewModel.deleteSignal(it) },
                        onTrailToCost = { id, entry -> viewModel.trailStopLossToCost(id, entry) },
                        onOpenCalculator = { signal -> viewModel.openCalculator(signal) }
                    )
                }
                AppTab.SCANNER -> {
                    ScannerRadarScreen(
                        stocks = fnoUniverse,
                        onTriggerScanForStock = { setupType -> viewModel.triggerScanNow(setupType) }
                    )
                }
                AppTab.TELEGRAM -> {
                    TelegramSettingsScreen(
                        telegramManager = viewModel.telegramManager,
                        upstoxMarketData = viewModel.upstoxMarketData,
                        upstoxStatusMessage = upstoxStatus,
                        onSaveUpstoxToken = { viewModel.saveUpstoxToken(it) },
                        onTestUpstoxConnection = { viewModel.testUpstoxConnection() },
                        isBackgroundRunning = isBgRunning,
                        onToggleBackground = { viewModel.toggleBackgroundService() },
                        statusMessage = telegramStatus,
                        onSaveSettings = { token, chat, auto, conf ->
                            viewModel.saveTelegramSettings(token, chat, auto, conf)
                        },
                        onTestConnection = { viewModel.testTelegramConnection() }
                    )
                }
                AppTab.PERFORMANCE -> {
                    PerformanceScreen(
                        signals = allSignals,
                        onClearAll = { viewModel.clearAllSignals() }
                    )
                }
            }

            // Auto-Sizing & Position Calculator Dialog
            selectedSignalForCalc?.let { signal ->
                PositionCalculatorDialog(
                    signal = signal,
                    onDismiss = { viewModel.closeCalculator() }
                )
            }
        }
    }
}

@Composable
fun AppBottomNav(
    currentTab: AppTab,
    onTabSelect: (AppTab) -> Unit
) {
    NavigationBar(
        containerColor = SurfaceDark,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = currentTab == AppTab.INTRADAY,
            onClick = { onTabSelect(AppTab.INTRADAY) },
            icon = { Icon(Icons.Default.ElectricBolt, contentDescription = "Intraday") },
            label = { Text("Intraday", fontSize = 10.sp) },
            modifier = Modifier.testTag("nav_intraday"),
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = CallGreen,
                indicatorColor = CallGreen,
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted
            )
        )
        NavigationBarItem(
            selected = currentTab == AppTab.POSITIONAL,
            onClick = { onTabSelect(AppTab.POSITIONAL) },
            icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Positional") },
            label = { Text("Positional", fontSize = 10.sp) },
            modifier = Modifier.testTag("nav_positional"),
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = CallGreen,
                indicatorColor = CallGreen,
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted
            )
        )
        NavigationBarItem(
            selected = currentTab == AppTab.SCANNER,
            onClick = { onTabSelect(AppTab.SCANNER) },
            icon = { Icon(Icons.Default.Radar, contentDescription = "Radar") },
            label = { Text("F&O Radar", fontSize = 10.sp) },
            modifier = Modifier.testTag("nav_radar"),
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = CallGreen,
                indicatorColor = CallGreen,
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted
            )
        )
        NavigationBarItem(
            selected = currentTab == AppTab.TELEGRAM,
            onClick = { onTabSelect(AppTab.TELEGRAM) },
            icon = { Icon(Icons.Default.Send, contentDescription = "Telegram") },
            label = { Text("Telegram", fontSize = 10.sp) },
            modifier = Modifier.testTag("nav_telegram"),
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = CallGreen,
                indicatorColor = CallGreen,
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted
            )
        )
        NavigationBarItem(
            selected = currentTab == AppTab.PERFORMANCE,
            onClick = { onTabSelect(AppTab.PERFORMANCE) },
            icon = { Icon(Icons.Default.Analytics, contentDescription = "Stats") },
            label = { Text("Performance", fontSize = 10.sp) },
            modifier = Modifier.testTag("nav_performance"),
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = CallGreen,
                indicatorColor = CallGreen,
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted
            )
        )
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
