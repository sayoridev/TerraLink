package com.terralink.iotfarm.widget

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey

object WidgetKeys {
    val WATER_LEVEL = floatPreferencesKey("water_level")
    val SOIL_HUMIDITY = floatPreferencesKey("soil_humidity")

    val TEMP = floatPreferencesKey("temp")
    val AIR_HUMIDITY = floatPreferencesKey("air_humidity")
    val LIGHT = intPreferencesKey("light")
    val RAIN = booleanPreferencesKey("rain")
    val IS_LED_ON = booleanPreferencesKey("is_led_on")
    val IS_FAN_ON = booleanPreferencesKey("is_fan_on")
}