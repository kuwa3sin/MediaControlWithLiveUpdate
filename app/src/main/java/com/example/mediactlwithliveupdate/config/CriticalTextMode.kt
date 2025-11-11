package com.example.mediactlwithliveupdate.config

import android.content.Context
import android.content.SharedPreferences

enum class ChipIndicatorMode(val prefValue: String) {
    APP_ICON_ONLY("app_icon_only"),
    PLAYBACK_ICON_ONLY("playback_emoji_only");

    companion object {
        fun fromPrefValue(value: String?): ChipIndicatorMode {
            return when (value) {
                "app_icon_and_emoji" -> APP_ICON_ONLY
                else -> values().firstOrNull { it.prefValue == value } ?: APP_ICON_ONLY
            }
        }
    }
}

object ChipIndicatorPreferences {
    const val PREFS_NAME: String = "live_media_control_preferences"
    private const val KEY_MODE: String = "chip_indicator_mode"

    fun observe(preferences: SharedPreferences, onChange: (ChipIndicatorMode) -> Unit): SharedPreferences.OnSharedPreferenceChangeListener {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == KEY_MODE) {
                onChange(readFromPreferences(prefs))
            }
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        return listener
    }

    fun read(context: Context): ChipIndicatorMode {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return readFromPreferences(prefs)
    }

    fun read(preferences: SharedPreferences): ChipIndicatorMode {
        return readFromPreferences(preferences)
    }

    fun write(context: Context, mode: ChipIndicatorMode) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        write(prefs, mode)
    }

    fun write(preferences: SharedPreferences, mode: ChipIndicatorMode) {
        if (readFromPreferences(preferences) == mode) {
            return
        }
        preferences.edit().putString(KEY_MODE, mode.prefValue).apply()
    }

    private fun readFromPreferences(preferences: SharedPreferences): ChipIndicatorMode {
        val value = preferences.getString(KEY_MODE, null)
        return ChipIndicatorMode.fromPrefValue(value)
    }
}
