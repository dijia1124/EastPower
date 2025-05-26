package com.dijia1124.eastpower

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PrefsRepository(context: Context) {
    companion object {
        private const val SETTINGS_NAME = "settings"
        private val KEY_BRIGHTNESS = intPreferencesKey("brightness")
        private const val DEFAULT_BRIGHTNESS = 4094
        private val KEY_MAX_BRIGHTNESS = intPreferencesKey("max_brightness")
        private const val DEFAULT_MAX_BRIGHTNESS = 4094
        private val BRIGHTNESS_SERVICE_RUNNING_KEY = booleanPreferencesKey("brightness_service_running")

        val Context.dataStore by preferencesDataStore(name = SETTINGS_NAME)
    }

    private val dataStore = context.dataStore

    val serviceRunningFlow: Flow<Boolean> =
        dataStore.data.map { it[BRIGHTNESS_SERVICE_RUNNING_KEY] ?: false }

    suspend fun setServiceRunning(running: Boolean) {
        dataStore.edit { it[BRIGHTNESS_SERVICE_RUNNING_KEY] = running }
    }

    val brightnessFlow: Flow<Int> = dataStore.data
        .map { prefs -> prefs[KEY_BRIGHTNESS] ?: DEFAULT_BRIGHTNESS }

    val maxBrightnessFlow: Flow<Int> = dataStore.data
        .map { prefs -> prefs[KEY_MAX_BRIGHTNESS] ?: DEFAULT_MAX_BRIGHTNESS }

    suspend fun setBrightness(value: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_BRIGHTNESS] = value.coerceIn(0, prefs[KEY_MAX_BRIGHTNESS] ?: DEFAULT_MAX_BRIGHTNESS)
        }
    }

    suspend fun setMaxBrightness(value: Int) {
        dataStore.edit { prefs ->
            val newMax = value.coerceAtLeast(1)
            prefs[KEY_MAX_BRIGHTNESS] = newMax
            val cur = prefs[KEY_BRIGHTNESS] ?: DEFAULT_BRIGHTNESS
            if (cur > newMax) {
                prefs[KEY_BRIGHTNESS] = newMax
            }
        }
    }
}
