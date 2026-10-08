package com.terralink.iotfarm.data.repository

import com.terralink.iotfarm.data.local.TelemetryDao
import com.terralink.iotfarm.data.local.TelemetryEntity
import com.terralink.iotfarm.data.network.TcpSocketManager
import com.terralink.iotfarm.domain.model.TelemetryState
import com.terralink.iotfarm.domain.repository.FarmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FarmRepositoryImpl @Inject constructor(
    private val tcpSocketManager: TcpSocketManager,
    private val telemetryDao: TelemetryDao
) : FarmRepository {

    override val connectionStatus: StateFlow<Boolean> = tcpSocketManager.connectionStatus
    override val telemetryState: StateFlow<TelemetryState> = tcpSocketManager.telemetryState

    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        scope.launch {
            telemetryState.collect { state ->
                // Salva nel DB locale Room se ci sono dati significativi o connessione attiva
                if (state.airTemperature != 0f || state.soilHumidity != 0f || state.waterLevel != 0f) {
                    try {
                        telemetryDao.insert(TelemetryEntity.fromDomainModel(state))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    override suspend fun connect(ipAddress: String) {
        tcpSocketManager.connect(ipAddress)
    }

    override suspend fun disconnect() {
        tcpSocketManager.disconnect()
    }

    override fun sendCommand(command: String) {
        tcpSocketManager.sendCommand(command)
    }
}
