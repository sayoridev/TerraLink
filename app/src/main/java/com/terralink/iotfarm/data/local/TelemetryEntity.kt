package com.terralink.iotfarm.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.terralink.iotfarm.domain.model.TelemetryState

@Entity(tableName = "telemetry_history")
data class TelemetryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val airTemperature: Float,
    val airHumidity: Float,
    val soilHumidity: Float,
    val rainSensor: Boolean,
    val waterLevel: Float,
    val lightIntensity: Int,
    val motionSensor: Boolean,
    val distance: Float,
    val solarVoltage: Float
) {
    fun toDomainModel(): TelemetryState {
        return TelemetryState(
            airTemperature = airTemperature,
            airHumidity = airHumidity,
            soilHumidity = soilHumidity,
            rainSensor = rainSensor,
            waterLevel = waterLevel,
            lightIntensity = lightIntensity,
            motionSensor = motionSensor,
            distance = distance,
            solarVoltage = solarVoltage
        )
    }

    companion object {
        fun fromDomainModel(state: TelemetryState): TelemetryEntity {
            return TelemetryEntity(
                airTemperature = state.airTemperature,
                airHumidity = state.airHumidity,
                soilHumidity = state.soilHumidity,
                rainSensor = state.rainSensor,
                waterLevel = state.waterLevel,
                lightIntensity = state.lightIntensity,
                motionSensor = state.motionSensor,
                distance = state.distance,
                solarVoltage = state.solarVoltage
            )
        }
    }
}
