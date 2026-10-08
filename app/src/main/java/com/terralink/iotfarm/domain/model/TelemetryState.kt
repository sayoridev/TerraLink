package com.terralink.iotfarm.domain.model

data class TelemetryState(
    // Sensori (Input)
    val airTemperature: Float = 0f,
    val airHumidity: Float = 0f,
    val soilHumidity: Float = 0f,
    val rainSensor: Boolean = false,
    val waterLevel: Float = 0f,
    val lightIntensity: Int = 0,
    val motionSensor: Boolean = false,
    val distance: Float = 0f,
    val solarVoltage: Float = 0f,
    val buttonState: Boolean = false,

    // Attuatori (Output / Feedback stati)
    val isLedOn: Boolean = false,
    val isFanOn: Boolean = false,
    val isRelayOn: Boolean = false,
    val isPumpOn: Boolean = false,
    val isBuzzerOn: Boolean = false,
    val servoAngle: Int = 0,
    val lcdText: String = "",

    // Stato Connessione
    val isConnected: Boolean = false
)
