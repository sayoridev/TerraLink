package com.terralink.iotfarm.data.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurePreferencesHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "terralink_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var savedFarmIp: String?
        get() = sharedPreferences.getString(KEY_FARM_IP, null)
        set(value) = sharedPreferences.edit().putString(KEY_FARM_IP, value).apply()

    var apiToken: String?
        get() = sharedPreferences.getString(KEY_API_TOKEN, null)
        set(value) = sharedPreferences.edit().putString(KEY_API_TOKEN, value).apply()

    fun clear() {
        sharedPreferences.edit().clear().apply()
    }

    companion object {
        private const val KEY_FARM_IP = "key_farm_ip"
        private const val KEY_API_TOKEN = "key_api_token"
    }
}
