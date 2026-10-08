package com.terralink.iotfarm.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.*
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

class SensorsWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<Preferences> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val temp = prefs[WidgetKeys.TEMP] ?: 0f
            val airHumidity = prefs[WidgetKeys.AIR_HUMIDITY] ?: 0f
            val light = prefs[WidgetKeys.LIGHT] ?: 0
            val rain = prefs[WidgetKeys.RAIN] ?: false
            val isLedOn = prefs[WidgetKeys.IS_LED_ON] ?: false
            val isFanOn = prefs[WidgetKeys.IS_FAN_ON] ?: false

            GlanceTheme {
                SensorsWidgetContent(temp, airHumidity, light, rain, isLedOn, isFanOn)
            }
        }
    }

    @Composable
    private fun SensorsWidgetContent(
        temp: Float, airHumidity: Float, light: Int, rain: Boolean, isLedOn: Boolean, isFanOn: Boolean
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(32.dp)
                .background(Color(0xFF0D2418))
                .padding(16.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically,
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally
        ) {
            Text(
                text = "📊 GLOBAL TELEMETRY",
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = GlanceModifier.height(12.dp))

            Row(modifier = GlanceModifier.fillMaxWidth()) {
                SensorMetricCard("🌡️ Temp", "$temp °C", GlanceModifier.defaultWeight())
                Spacer(modifier = GlanceModifier.width(8.dp))
                SensorMetricCard("💨 Humidity", "${airHumidity.toInt()}%", GlanceModifier.defaultWeight())
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            Row(modifier = GlanceModifier.fillMaxWidth()) {
                SensorMetricCard("☀️ Light", "$light lx", GlanceModifier.defaultWeight())
                Spacer(modifier = GlanceModifier.width(8.dp))
                SensorMetricCard("🌧️ Rain", if (rain) "Yes" else "No", GlanceModifier.defaultWeight())
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            Row(modifier = GlanceModifier.fillMaxWidth()) {
                SensorMetricCard("💡 LED", if (isLedOn) "ON" else "OFF", GlanceModifier.defaultWeight())
                Spacer(modifier = GlanceModifier.width(8.dp))
                SensorMetricCard("🌀 Fan", if (isFanOn) "ON" else "OFF", GlanceModifier.defaultWeight())
            }
        }
    }

    @Composable
    private fun SensorMetricCard(label: String, value: String, modifier: GlanceModifier) {
        Column(
            modifier = modifier
                .cornerRadius(24.dp)
                .background(Color(0xFF1B422B))
                .padding(vertical = 12.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally
        ) {
            Text(
                text = label,
                style = TextStyle(color = ColorProvider(Color(0xFFA3E635)), fontSize = 11.sp)
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = value,
                style = TextStyle(color = ColorProvider(Color.White), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            )
        }
    }
}

class SensorsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SensorsWidget()
}
