package com.terralink.iotfarm.domain.rule

import com.terralink.iotfarm.data.debug.DebugLogManager
import com.terralink.iotfarm.data.debug.LogEntry
import com.terralink.iotfarm.domain.repository.FarmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutomationRuleManager @Inject constructor(
    private val repository: FarmRepository,
    private val debugLogManager: DebugLogManager
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var isAutomationEnabled = true

    init {
        scope.launch {
            repository.telemetryState.collectLatest { state ->
                if (isAutomationEnabled && repository.connectionStatus.value) {
                    // Rule 1: Auto-trigger water pump when soil humidity drops below 30% and rain sensor is false
                    if (state.soilHumidity < 30f && !state.rainSensor && !state.isPumpOn) {
                        debugLogManager.log(LogEntry.LogType.AUTOMATION, "Soil humidity low (${state.soilHumidity}%). Auto-triggering PUMP:ON")
                        repository.sendCommand("PUMP:ON")
                    } else if ((state.soilHumidity >= 40f || state.rainSensor) && state.isPumpOn) {
                        debugLogManager.log(LogEntry.LogType.AUTOMATION, "Soil humidity optimal or rain detected. Auto-triggering PUMP:OFF")
                        repository.sendCommand("PUMP:OFF")
                    }

                    // Rule 2: Auto-trigger cooling fan when air temperature exceeds 35°C
                    if (state.airTemperature > 35f && !state.isFanOn) {
                        debugLogManager.log(LogEntry.LogType.AUTOMATION, "Air temp high (${state.airTemperature}°C). Auto-triggering FAN:ON")
                        repository.sendCommand("FAN:ON")
                    } else if (state.airTemperature <= 30f && state.isFanOn) {
                        debugLogManager.log(LogEntry.LogType.AUTOMATION, "Air temp normal. Auto-triggering FAN:OFF")
                        repository.sendCommand("FAN:OFF")
                    }
                }
            }
        }
    }

    fun setAutomationEnabled(enabled: Boolean) {
        isAutomationEnabled = enabled
        debugLogManager.log(LogEntry.LogType.SYSTEM, "Automation rules enabled: $enabled")
    }

    fun isEnabled(): Boolean = isAutomationEnabled
}
