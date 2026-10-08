package com.terralink.iotfarm.data.network

import com.terralink.iotfarm.data.debug.DebugLogManager
import com.terralink.iotfarm.data.debug.LogEntry
import com.terralink.iotfarm.domain.model.TelemetryState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TcpSocketManager @Inject constructor(
    private val debugLogManager: DebugLogManager
) {

    private val _connectionStatus = MutableStateFlow(false)
    val connectionStatus: StateFlow<Boolean> = _connectionStatus.asStateFlow()

    private val _telemetryState = MutableStateFlow(TelemetryState())
    val telemetryState: StateFlow<TelemetryState> = _telemetryState.asStateFlow()

    private val _isReconnecting = MutableStateFlow(false)
    val isReconnecting: StateFlow<Boolean> = _isReconnecting.asStateFlow()

    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null

    private var connectionJob: Job? = null
    private var isManualDisconnect = false
    private var currentIp: String? = null
    private var currentPort: Int = 8080

    private val scope = CoroutineScope(Dispatchers.IO)

    fun connect(ipAddress: String, port: Int = 8080) {
        isManualDisconnect = false
        currentIp = ipAddress
        currentPort = port

        debugLogManager.log(LogEntry.LogType.SYSTEM, "Connecting to farm at $ipAddress:$port...")
        connectionJob?.cancel()
        connectionJob = scope.launch {
            connectWithExponentialBackoff()
        }
    }

    private suspend fun connectWithExponentialBackoff() {
        var delayMs = 1000L
        val maxDelayMs = 30000L
        val factor = 2.0

        while (scope.isActive && !isManualDisconnect) {
            val ip = currentIp ?: return
            val success = tryConnect(ip, currentPort)

            if (success) {
                _isReconnecting.value = false
                delayMs = 1000L
                debugLogManager.log(LogEntry.LogType.SYSTEM, "Successfully connected to farm!")

                listenToSocket()

                if (!isManualDisconnect) {
                    _connectionStatus.value = false
                    _isReconnecting.value = true
                    debugLogManager.log(LogEntry.LogType.SYSTEM, "Connection lost. Attempting reconnection...")
                }
            } else {
                _connectionStatus.value = false
                _isReconnecting.value = true

                delay(delayMs)
                delayMs = (delayMs * factor).toLong().coerceAtMost(maxDelayMs)
            }
        }
        _isReconnecting.value = false
    }

    private suspend fun tryConnect(ipAddress: String, port: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            closeResources()

            val newSocket = Socket()
            newSocket.connect(InetSocketAddress(ipAddress, port), 4000)
            socket = newSocket

            writer = PrintWriter(newSocket.getOutputStream(), true)
            reader = BufferedReader(InputStreamReader(newSocket.getInputStream()))

            _connectionStatus.value = true
            true
        } catch (e: Exception) {
            closeResources()
            false
        }
    }

    private fun listenToSocket() {
        try {
            val bufferedReader = reader ?: return
            var line: String? = bufferedReader.readLine()
            while (scope.isActive && !isManualDisconnect && line != null) {
                debugLogManager.log(LogEntry.LogType.TCP_RX, line)
                parseTelemetry(line)
                line = bufferedReader.readLine()
            }
        } catch (e: Exception) {
            debugLogManager.log(LogEntry.LogType.SYSTEM, "Error reading from socket: ${e.message}")
        } finally {
            _connectionStatus.value = false
        }
    }

    private fun parseTelemetry(data: String) {
        try {
            val parts = data.split(",")
            var airTemp = _telemetryState.value.airTemperature
            var airHum = _telemetryState.value.airHumidity
            var soilHum = _telemetryState.value.soilHumidity
            var light = _telemetryState.value.lightIntensity
            var water = _telemetryState.value.waterLevel
            var rain = _telemetryState.value.rainSensor
            var motion = _telemetryState.value.motionSensor
            var distance = _telemetryState.value.distance
            var solarVolt = _telemetryState.value.solarVoltage
            var button = _telemetryState.value.buttonState

            var led = _telemetryState.value.isLedOn
            var fan = _telemetryState.value.isFanOn
            var relay = _telemetryState.value.isRelayOn
            var pump = _telemetryState.value.isPumpOn
            var buzzer = _telemetryState.value.isBuzzerOn
            var servo = _telemetryState.value.servoAngle
            var lcdText = _telemetryState.value.lcdText

            for (part in parts) {
                val keyVal = part.split(":")
                if (keyVal.size == 2) {
                    val key = keyVal[0].trim().uppercase()
                    val value = keyVal[1].trim()
                    when (key) {
                        "TEMP", "AIR_TEMP" -> airTemp = value.toFloatOrNull() ?: airTemp
                        "HUM", "AIR_HUM" -> airHum = value.toFloatOrNull() ?: airHum
                        "SOIL" -> soilHum = value.toFloatOrNull() ?: soilHum
                        "STEAM", "RAIN" -> rain = parseBoolean(value)
                        "WATER" -> water = value.toFloatOrNull() ?: water
                        "LIGHT" -> light = value.toIntOrNull() ?: light
                        "MOTION", "PIR" -> motion = parseBoolean(value)
                        "DISTANCE" -> distance = value.toFloatOrNull() ?: distance
                        "SOLAR_VOLT", "SOLAR" -> solarVolt = value.toFloatOrNull() ?: solarVolt
                        "BUTTON" -> button = parseBoolean(value)
                        "LED" -> led = parseBoolean(value)
                        "FAN", "MOTOR" -> fan = parseBoolean(value)
                        "RELAY" -> relay = parseBoolean(value)
                        "PUMP" -> pump = parseBoolean(value)
                        "BUZZER" -> buzzer = parseBoolean(value)
                        "SERVO" -> servo = value.toIntOrNull()?.coerceIn(0, 180) ?: servo
                        "LCD_TEXT", "LCD" -> lcdText = value
                    }
                }
            }

            _telemetryState.value = TelemetryState(
                airTemperature = airTemp,
                airHumidity = airHum,
                soilHumidity = soilHum,
                rainSensor = rain,
                waterLevel = water,
                lightIntensity = light,
                motionSensor = motion,
                distance = distance,
                solarVoltage = solarVolt,
                buttonState = button,
                isLedOn = led,
                isFanOn = fan,
                isRelayOn = relay,
                isPumpOn = pump,
                isBuzzerOn = buzzer,
                servoAngle = servo,
                lcdText = lcdText,
                isConnected = _connectionStatus.value
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseBoolean(value: String): Boolean {
        return value.toBooleanStrictOrNull()
            ?: value.toIntOrNull()?.let { it != 0 }
            ?: value.toFloatOrNull()?.let { it > 0.5f }
            ?: (value.uppercase() in listOf("ON", "TRUE", "YES", "1"))
    }

    fun sendCommand(command: String) {
        scope.launch {
            try {
                if (_connectionStatus.value && writer != null) {
                    writer?.println(command)
                    debugLogManager.log(LogEntry.LogType.TCP_TX, command)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        isManualDisconnect = true
        connectionJob?.cancel()
        closeResources()
        _connectionStatus.value = false
        _isReconnecting.value = false
        debugLogManager.log(LogEntry.LogType.SYSTEM, "Manually disconnected from farm.")
    }

    private fun closeResources() {
        try {
            writer?.close()
            reader?.close()
            socket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            socket = null
            writer = null
            reader = null
        }
    }
}
