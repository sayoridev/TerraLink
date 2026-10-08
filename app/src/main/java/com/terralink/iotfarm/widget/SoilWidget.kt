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
import androidx.glance.appwidget.LinearProgressIndicator
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

class SoilWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<Preferences> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val soilHumidity = prefs[WidgetKeys.SOIL_HUMIDITY] ?: 45f

            GlanceTheme {
                SoilWidgetContent(soilHumidity)
            }
        }
    }

    @Composable
    private fun SoilWidgetContent(soilHumidity: Float) {
        val soilColor = Color(0xFF2ECC71)

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
                text = "🌱 SOIL HUMIDITY",
                style = TextStyle(color = ColorProvider(Color.White), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = GlanceModifier.height(16.dp))
            Text(
                text = "${soilHumidity.toInt()}%",
                style = TextStyle(color = ColorProvider(soilColor), fontSize = 40.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = GlanceModifier.height(16.dp))
            LinearProgressIndicator(
                progress = (soilHumidity / 100f).coerceIn(0f, 1f),
                modifier = GlanceModifier.fillMaxWidth().height(12.dp).cornerRadius(8.dp),
                color = ColorProvider(soilColor),
                backgroundColor = ColorProvider(soilColor.copy(alpha = 0.3f))
            )
        }
    }
}

class SoilWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SoilWidget()
}
