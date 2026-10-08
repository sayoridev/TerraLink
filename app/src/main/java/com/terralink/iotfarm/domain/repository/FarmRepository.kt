package com.terralink.iotfarm.domain.repository

import com.terralink.iotfarm.domain.model.TelemetryState
import kotlinx.coroutines.flow.StateFlow

interface FarmRepository {
    val connectionStatus: StateFlow<Boolean>
    val telemetryState: StateFlow<TelemetryState>

    suspend fun connect(ipAddress: String)
    suspend fun disconnect()

    // Aggiungi questo metodo:
    fun sendCommand(command: String)
}