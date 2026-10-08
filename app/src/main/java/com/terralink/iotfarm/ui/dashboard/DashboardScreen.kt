package com.terralink.iotfarm.ui.dashboard

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.terralink.iotfarm.ui.buzzer.BuzzerStudioScreen
import com.terralink.iotfarm.ui.debug.DebugLogScreen

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val haptic = LocalHapticFeedback.current
    var showDebugScreen by remember { mutableStateOf(false) }
    var showBuzzerStudio by remember { mutableStateOf(false) }

    if (showDebugScreen) {
        DebugLogScreen(
            debugLogManager = viewModel.debugLogManager,
            onBackClick = { showDebugScreen = false }
        )
        return
    }

    if (showBuzzerStudio) {
        BuzzerStudioScreen(
            viewModel = viewModel,
            onBackClick = { showBuzzerStudio = false }
        )
        return
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { }
        )
        LaunchedEffect(Unit) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val isConnected by viewModel.connectionStatus.collectAsState()
    val telemetry by viewModel.telemetry.collectAsState()
    val soilHistory by viewModel.soilHistory.collectAsState()
    val tempHistory by viewModel.tempHistory.collectAsState()
    val timeFrame by viewModel.chartTimeFrame.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scannedIp by viewModel.scannedIp.collectAsState()

    val isLedOn by viewModel.isLedOn.collectAsState()
    val isFanOn by viewModel.isFanOn.collectAsState()
    val isRelayOn by viewModel.isRelayOn.collectAsState()
    val isPumpOn by viewModel.isPumpOn.collectAsState()
    val isBuzzerOn by viewModel.isBuzzerOn.collectAsState()
    val servoAngle by viewModel.servoAngle.collectAsState()
    val lcdText by viewModel.lcdText.collectAsState()

    var ipAddress by remember { mutableStateOf("10.0.2.2") }

    LaunchedEffect(scannedIp) {
        scannedIp?.let {
            ipAddress = it
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status Bar Top & TCP Connection Status
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isConnected) Color(0xFF2E7D32) else Color(0xFFC62828)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isConnected) "Status: Connected (ESP32 PLUS)" else "Status: Disconnected",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = if (isConnected) "Live telemetry streaming active" else "Waiting for TCP connection...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Button to open Debug & Network Packet Inspector
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                showDebugScreen = true
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Open Debug & Network Packet Inspector")
        }

        // Button to open Buzzer Studio
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                showBuzzerStudio = true
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("🎵 Open Buzzer Studio (Melodies & Notes)")
        }

        // Manual Connection & UDP IP Scanner
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "IoT Connection Setup",
                    style = MaterialTheme.typography.titleLarge
                )
                OutlinedTextField(
                    value = ipAddress,
                    onValueChange = { ipAddress = it },
                    label = { Text("IoT Board IP Address") },
                    singleLine = true,
                    enabled = !isConnected,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (isConnected) viewModel.disconnect() else viewModel.connectToFarm(ipAddress)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isConnected) "Disconnect" else "Connect to Control Unit")
                }
            }
        }

        // UDP IP Scanner Component
        IpScannerCard(
            isScanning = isScanning,
            scannedIp = scannedIp,
            onStartScan = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.scanForFarmIp()
            },
            onUseIp = { ip ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                ipAddress = ip
                viewModel.connectToFarm(ip)
            }
        )

        // SECTION 1: Climate & Soil
        Text(
            text = "Climate & Soil (DHT11 & Soil)",
            style = MaterialTheme.typography.titleMedium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TelemetryMetricCard(
                title = "Air Temp",
                value = "${telemetry.airTemperature} °C",
                modifier = Modifier.weight(1f)
            )
            TelemetryMetricCard(
                title = "Air Humidity",
                value = "${telemetry.airHumidity} %",
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TelemetryMetricCard(
                title = "Light Intensity",
                value = "${telemetry.lightIntensity} lx",
                modifier = Modifier.weight(1f)
            )
            TelemetryMetricCard(
                title = "Physical Button",
                value = if (telemetry.buttonState) "Pressed" else "Released",
                modifier = Modifier.weight(1f)
            )
        }

        AdvancedTelemetryChartCard(
            soilHistory = soilHistory,
            tempHistory = tempHistory,
            timeFrame = timeFrame,
            onTimeFrameSelected = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.setTimeFrame(it)
            }
        )

        // SECTION 2: Levels, Rain & Power
        Text(
            text = "Levels, Rain & Power",
            style = MaterialTheme.typography.titleMedium
        )

        WaterLevelCard(waterLevel = telemetry.waterLevel)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TelemetryMetricCard(
                title = "Rain Sensor",
                value = if (telemetry.rainSensor) "Detected" else "None",
                modifier = Modifier.weight(1f)
            )
            TelemetryMetricCard(
                title = "Solar Panel",
                value = "${telemetry.solarVoltage} V",
                modifier = Modifier.weight(1f)
            )
        }

        // SECTION 3: Security & Motion
        Text(
            text = "Security & Motion",
            style = MaterialTheme.typography.titleMedium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TelemetryMetricCard(
                title = "PIR Sensor",
                value = if (telemetry.motionSensor) "Motion Detected!" else "Idle",
                modifier = Modifier.weight(1f)
            )
            TelemetryMetricCard(
                title = "Distance (Ultrasonic)",
                value = "${telemetry.distance} cm",
                modifier = Modifier.weight(1f)
            )
        }

        // SECTION 4: Actuators & Controls (ESP32 PLUS)
        Text(
            text = "ESP32 PLUS Actuators & Controls",
            style = MaterialTheme.typography.titleMedium
        )

        Esp32PlusActuatorsCard(
            isConnected = isConnected,
            isLedOn = isLedOn,
            isFanOn = isFanOn,
            isRelayOn = isRelayOn,
            isPumpOn = isPumpOn,
            isBuzzerOn = isBuzzerOn,
            servoAngle = servoAngle,
            lcdText = lcdText,
            onLedToggle = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.toggleLed(it)
            },
            onFanToggle = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.toggleFan(it)
            },
            onRelayToggle = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.toggleRelay(it)
            },
            onPumpToggle = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.togglePump(it)
            },
            onBuzzerToggle = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.toggleBuzzer(it)
            },
            onServoChange = {
                viewModel.updateServoAngle(it)
            },
            onLcdSubmit = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.updateLcdText(it)
            }
        )
    }
}

@Composable
fun WaterLevelCard(waterLevel: Float) {
    val progressAnimated by animateFloatAsState(
        targetValue = (waterLevel / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 500),
        label = "WaterLevelAnimation"
    )

    val levelColor = when {
        waterLevel < 20f -> Color(0xFFD32F2F)
        waterLevel < 50f -> Color(0xFFF57C00)
        else -> Color(0xFF0288D1)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Water Tank Level", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${waterLevel.toInt()}%",
                    style = MaterialTheme.typography.titleLarge,
                    color = levelColor
                )
            }

            LinearProgressIndicator(
                progress = { progressAnimated },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp),
                color = levelColor,
                trackColor = levelColor.copy(alpha = 0.2f),
            )
        }
    }
}

@Composable
fun AdvancedTelemetryChartCard(
    soilHistory: List<Float>,
    tempHistory: List<Float>,
    timeFrame: DashboardViewModel.ChartTimeFrame,
    onTimeFrameSelected: (DashboardViewModel.ChartTimeFrame) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Advanced Telemetry Trends", style = MaterialTheme.typography.titleMedium)
                
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = timeFrame == DashboardViewModel.ChartTimeFrame.HOURLY,
                        onClick = { onTimeFrameSelected(DashboardViewModel.ChartTimeFrame.HOURLY) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Hourly")
                    }
                    SegmentedButton(
                        selected = timeFrame == DashboardViewModel.ChartTimeFrame.DAILY,
                        onClick = { onTimeFrameSelected(DashboardViewModel.ChartTimeFrame.DAILY) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("Daily")
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(12.dp).background(Color(0xFF388E3C), RoundedCornerShape(2.dp)))
                    Text(text = "Soil Humidity (%)", style = MaterialTheme.typography.bodySmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(12.dp).background(Color(0xFFF57C00), RoundedCornerShape(2.dp)))
                    Text(text = "Air Temp (°C)", style = MaterialTheme.typography.bodySmall)
                }
            }

            val hasData = soilHistory.size > 1 || tempHistory.size > 1

            if (hasData) {
                val soilColor = Color(0xFF388E3C)
                val tempColor = Color(0xFFF57C00)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    val width = size.width
                    val height = size.height

                    val gridColor = Color.LightGray.copy(alpha = 0.5f)
                    for (i in 0..4) {
                        val y = height * (i / 4f)
                        drawLine(
                            color = gridColor,
                            start = androidx.compose.ui.geometry.Offset(0f, y),
                            end = androidx.compose.ui.geometry.Offset(width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    if (soilHistory.size > 1) {
                        val spacing = width / (soilHistory.size - 1)
                        val soilPath = Path()
                        soilHistory.forEachIndexed { index, value ->
                            val x = index * spacing
                            val y = height - ((value.coerceIn(0f, 100f) / 100f) * height)
                            if (index == 0) soilPath.moveTo(x, y) else soilPath.lineTo(x, y)
                        }
                        drawPath(
                            path = soilPath,
                            color = soilColor,
                            style = Stroke(width = 2.5.dp.toPx())
                        )
                    }

                    if (tempHistory.size > 1) {
                        val spacing = width / (tempHistory.size - 1)
                        val tempPath = Path()
                        tempHistory.forEachIndexed { index, value ->
                            val x = index * spacing
                            val y = height - ((value.coerceIn(0f, 50f) / 50f) * height)
                            if (index == 0) tempPath.moveTo(x, y) else tempPath.lineTo(x, y)
                        }
                        drawPath(
                            path = tempPath,
                            color = tempColor,
                            style = Stroke(width = 2.5.dp.toPx())
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (timeFrame == DashboardViewModel.ChartTimeFrame.HOURLY) "-24h" else "-30d",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    Text(
                        text = "Now",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            } else {
                Text(
                    text = "Collecting telemetry data for chart...",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun Esp32PlusActuatorsCard(
    isConnected: Boolean,
    isLedOn: Boolean,
    isFanOn: Boolean,
    isRelayOn: Boolean,
    isPumpOn: Boolean,
    isBuzzerOn: Boolean,
    servoAngle: Int,
    lcdText: String,
    onLedToggle: (Boolean) -> Unit,
    onFanToggle: (Boolean) -> Unit,
    onRelayToggle: (Boolean) -> Unit,
    onPumpToggle: (Boolean) -> Unit,
    onBuzzerToggle: (Boolean) -> Unit,
    onServoChange: (Int) -> Unit,
    onLcdSubmit: (String) -> Unit
) {
    var textInput by remember(lcdText) { mutableStateOf(lcdText) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // LED
            ActuatorRow(
                title = "LED Lighting",
                subtitle = if (isLedOn) "ON" else "OFF",
                checked = isLedOn,
                enabled = isConnected,
                onToggle = onLedToggle
            )
            HorizontalDivider()

            // FAN
            ActuatorRow(
                title = "Cooling Fan (FAN)",
                subtitle = if (isFanOn) "ACTIVE" else "INACTIVE",
                checked = isFanOn,
                enabled = isConnected,
                onToggle = onFanToggle
            )
            HorizontalDivider()

            // RELAY
            ActuatorRow(
                title = "5V Relay Module",
                subtitle = if (isRelayOn) "CLOSED (ON)" else "OPEN (OFF)",
                checked = isRelayOn,
                enabled = isConnected,
                onToggle = onRelayToggle
            )
            HorizontalDivider()

            // PUMP
            ActuatorRow(
                title = "DC 3V Water Pump",
                subtitle = if (isPumpOn) "IRRIGATING (ON)" else "STOPPED",
                checked = isPumpOn,
                enabled = isConnected,
                onToggle = onPumpToggle
            )
            HorizontalDivider()

            // BUZZER
            ActuatorRow(
                title = "Passive Buzzer",
                subtitle = if (isBuzzerOn) "SOUND ON" else "SILENT",
                checked = isBuzzerOn,
                enabled = isConnected,
                onToggle = onBuzzerToggle
            )
            HorizontalDivider()

            // SERVO MOTOR SLIDER
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Servo Motor (Angle)", style = MaterialTheme.typography.bodyLarge)
                    Text("$servoAngle°", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = servoAngle.toFloat(),
                    onValueChange = { onServoChange(it.toInt()) },
                    valueRange = 0f..180f,
                    steps = 17,
                    enabled = isConnected
                )
            }

            HorizontalDivider()

            // LCD DISPLAY I2C 1602 TEXT INPUT
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("I2C 1602 LCD Display", style = MaterialTheme.typography.bodyLarge)
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    label = { Text("Text to send to Display") },
                    singleLine = true,
                    enabled = isConnected,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { onLcdSubmit(textInput) },
                    enabled = isConnected,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Update LCD Display")
                }
            }
        }
    }
}

@Composable
fun ActuatorRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            enabled = enabled
        )
    }
}

@Composable
fun TelemetryMetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(text = title, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
fun IpScannerCard(
    isScanning: Boolean,
    scannedIp: String?,
    onStartScan: () -> Unit,
    onUseIp: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Automatic IP Discovery (UDP)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Detect ESP32 PLUS control unit on local network",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            if (isScanning) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    text = "Broadcast scanning in progress...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Button(
                    onClick = onStartScan,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text("Start UDP Network Discovery")
                }

                if (scannedIp != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Device Found: $scannedIp",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF2E7D32)
                            )
                            Button(
                                onClick = { onUseIp(scannedIp) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                            ) {
                                Text("Connect to Discovered IP")
                            }
                        }
                    }
                }
            }
        }
    }
}
