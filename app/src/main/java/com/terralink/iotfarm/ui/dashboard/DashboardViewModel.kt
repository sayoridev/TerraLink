package com.terralink.iotfarm.ui.dashboard

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.terralink.iotfarm.data.debug.DebugLogManager
import com.terralink.iotfarm.data.notification.NotificationHelper
import com.terralink.iotfarm.data.network.UdpDiscoveryManager
import com.terralink.iotfarm.domain.model.TelemetryState
import com.terralink.iotfarm.domain.repository.FarmRepository
import com.terralink.iotfarm.domain.usecase.ConnectFarmUseCase
import com.terralink.iotfarm.widget.SensorsWidget
import com.terralink.iotfarm.widget.SoilWidget
import com.terralink.iotfarm.widget.WaterWidget
import com.terralink.iotfarm.widget.WidgetKeys
import com.terralink.iotfarm.ui.buzzer.Mp3ToToneConverter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val connectFarmUseCase: ConnectFarmUseCase,
    private val repository: FarmRepository,
    private val notificationHelper: NotificationHelper,
    private val udpDiscoveryManager: UdpDiscoveryManager,
    val debugLogManager: DebugLogManager
) : ViewModel() {

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scannedIp = MutableStateFlow<String?>(null)
    val scannedIp: StateFlow<String?> = _scannedIp.asStateFlow()

    fun scanForFarmIp() {
        viewModelScope.launch {
            _isScanning.value = true
            _scannedIp.value = null
            val ip = udpDiscoveryManager.discoverFarmIp()
            _scannedIp.value = ip
            _isScanning.value = false
        }
    }

    val connectionStatus: StateFlow<Boolean> = repository.connectionStatus
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val telemetry: StateFlow<TelemetryState> = repository.telemetryState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TelemetryState())

    private val _soilHistory = MutableStateFlow<List<Float>>(emptyList())
    val soilHistory: StateFlow<List<Float>> = _soilHistory.asStateFlow()

    private val _tempHistory = MutableStateFlow<List<Float>>(emptyList())
    val tempHistory: StateFlow<List<Float>> = _tempHistory.asStateFlow()

    private val _chartTimeFrame = MutableStateFlow(ChartTimeFrame.HOURLY)
    val chartTimeFrame: StateFlow<ChartTimeFrame> = _chartTimeFrame.asStateFlow()

    enum class ChartTimeFrame {
        HOURLY, DAILY
    }

    fun setTimeFrame(timeFrame: ChartTimeFrame) {
        _chartTimeFrame.value = timeFrame
    }

    private val _isLedOn = MutableStateFlow(false)
    val isLedOn: StateFlow<Boolean> = _isLedOn.asStateFlow()

    private val _isFanOn = MutableStateFlow(false)
    val isFanOn: StateFlow<Boolean> = _isFanOn.asStateFlow()

    private val _isRelayOn = MutableStateFlow(false)
    val isRelayOn: StateFlow<Boolean> = _isRelayOn.asStateFlow()

    private val _isPumpOn = MutableStateFlow(false)
    val isPumpOn: StateFlow<Boolean> = _isPumpOn.asStateFlow()

    private val _isBuzzerOn = MutableStateFlow(false)
    val isBuzzerOn: StateFlow<Boolean> = _isBuzzerOn.asStateFlow()

    private val _servoAngle = MutableStateFlow(90)
    val servoAngle: StateFlow<Int> = _servoAngle.asStateFlow()

    private val _lcdText = MutableStateFlow("TerraLink IoT")
    val lcdText: StateFlow<String> = _lcdText.asStateFlow()

    private var isWaterAlertTriggered = false

    init {
        viewModelScope.launch {
            telemetry.collect { state ->
                _soilHistory.update { history ->
                    (history + state.soilHumidity).takeLast(24)
                }
                _tempHistory.update { history ->
                    (history + state.airTemperature).takeLast(24)
                }

                if (connectionStatus.value) {
                    if (state.waterLevel < 20f && !isWaterAlertTriggered) {
                        isWaterAlertTriggered = true
                        notificationHelper.showWaterLevelAlert(state.waterLevel)
                    } else if (state.waterLevel >= 25f) {
                        isWaterAlertTriggered = false
                    }
                }

                updateAllWidgets(state, _isLedOn.value, _isFanOn.value)
            }
        }
    }

    private suspend fun updateAllWidgets(state: TelemetryState, ledOn: Boolean, fanOn: Boolean) {
        try {
            val manager = GlanceAppWidgetManager(context)

            manager.getGlanceIds(WaterWidget::class.java).forEach { id ->
                updateAppWidgetState(context, id) { prefs ->
                    prefs[WidgetKeys.WATER_LEVEL] = state.waterLevel
                }
                WaterWidget().update(context, id)
            }

            manager.getGlanceIds(SoilWidget::class.java).forEach { id ->
                updateAppWidgetState(context, id) { prefs ->
                    prefs[WidgetKeys.SOIL_HUMIDITY] = state.soilHumidity
                }
                SoilWidget().update(context, id)
            }

            manager.getGlanceIds(SensorsWidget::class.java).forEach { id ->
                updateAppWidgetState(context, id) { prefs ->
                    prefs[WidgetKeys.TEMP] = state.airTemperature
                    prefs[WidgetKeys.AIR_HUMIDITY] = state.airHumidity
                    prefs[WidgetKeys.LIGHT] = state.lightIntensity
                    prefs[WidgetKeys.RAIN] = state.rainSensor
                    prefs[WidgetKeys.IS_LED_ON] = ledOn
                    prefs[WidgetKeys.IS_FAN_ON] = fanOn
                }
                SensorsWidget().update(context, id)
            }
        } catch (e: Exception) {
            // Ignored in unit tests or when AppWidgetManager is not mocked
        }
    }

    fun connectToFarm(ipAddress: String) {
        viewModelScope.launch {
            connectFarmUseCase(ipAddress)
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            repository.disconnect()
        }
    }

    fun toggleLed(enabled: Boolean) {
        _isLedOn.value = enabled
        repository.sendCommand(if (enabled) "LED:ON" else "LED:OFF")
        viewModelScope.launch {
            updateAllWidgets(telemetry.value, _isLedOn.value, _isFanOn.value)
        }
    }

    fun toggleFan(enabled: Boolean) {
        _isFanOn.value = enabled
        repository.sendCommand(if (enabled) "FAN:ON" else "FAN:OFF")
        viewModelScope.launch {
            updateAllWidgets(telemetry.value, _isLedOn.value, _isFanOn.value)
        }
    }

    fun toggleRelay(enabled: Boolean) {
        _isRelayOn.value = enabled
        repository.sendCommand(if (enabled) "RELAY:ON" else "RELAY:OFF")
    }

    fun togglePump(enabled: Boolean) {
        _isPumpOn.value = enabled
        repository.sendCommand(if (enabled) "PUMP:ON" else "PUMP:OFF")
    }

    fun toggleBuzzer(enabled: Boolean) {
        _isBuzzerOn.value = enabled
        repository.sendCommand(if (enabled) "BUZZER:ON" else "BUZZER:OFF")
    }

    fun updateServoAngle(angle: Int) {
        val clamped = angle.coerceIn(0, 180)
        _servoAngle.value = clamped
        repository.sendCommand("SERVO:$clamped")
    }

    fun updateLcdText(text: String) {
        _lcdText.value = text
        repository.sendCommand("LCD:$text")
    }

    // Buzzer Studio Melodies & Control
    private val _isBuzzerPlaying = MutableStateFlow(false)
    val isBuzzerPlaying: StateFlow<Boolean> = _isBuzzerPlaying.asStateFlow()

    private val _currentNoteIndex = MutableStateFlow(0)
    val currentNoteIndex: StateFlow<Int> = _currentNoteIndex.asStateFlow()

    private val _totalNotes = MutableStateFlow(0)
    val totalNotes: StateFlow<Int> = _totalNotes.asStateFlow()

    private val _selectedMelodyName = MutableStateFlow("No melody selected")
    val selectedMelodyName: StateFlow<String> = _selectedMelodyName.asStateFlow()

    fun playPresetMelody(melodyName: String, notes: IntArray, durations: IntArray) {
        _selectedMelodyName.value = melodyName
        _totalNotes.value = notes.size
        _currentNoteIndex.value = 0
        _isBuzzerPlaying.value = true

        val notesCsv = notes.joinToString(",")
        val durationsCsv = durations.joinToString(",")
        repository.sendCommand("PLAY_MELODY:$notesCsv|$durationsCsv")
    }

    fun loadAndConvertCustomMp3(fileName: String, bytes: ByteArray) {
        val (notes, durations) = Mp3ToToneConverter.convertMp3ToMelody(bytes)
        playPresetMelody("MP3: $fileName", notes, durations)
    }

    fun stopBuzzer() {
        _isBuzzerPlaying.value = false
        _currentNoteIndex.value = 0
        repository.sendCommand("BUZZER:STOP")
    }

    fun pauseBuzzer() {
        _isBuzzerPlaying.value = false
        repository.sendCommand("BUZZER:PAUSE")
    }

    fun seekBuzzer(index: Int) {
        _currentNoteIndex.value = index
        repository.sendCommand("BUZZER:SEEK:$index")
    }
}
