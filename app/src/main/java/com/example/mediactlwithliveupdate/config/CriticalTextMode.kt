package com.kuwa3sin.mediactlwithliveupdate.config

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

enum class ChipTextMode(val prefValue: String) {
    NONE("none"),
    APP_NAME_SHORT("app_name_short"),
    PLAYBACK_STATE("playback_state");

    companion object {
        fun fromPrefValue(value: String?): ChipTextMode {
            return values().firstOrNull { it.prefValue == value } ?: APP_NAME_SHORT
        }
    }
}

data class ChipPreferencesSnapshot(
    val indicatorMode: ChipIndicatorMode,
    val textMode: ChipTextMode,
    val liveUpdateEnabled: Boolean
)

object ChipIndicatorPreferences {
    const val PREFS_NAME: String = "live_media_control_preferences"
    private const val KEY_MODE: String = "chip_indicator_mode"
    private const val KEY_TEXT_MODE: String = "chip_text_mode"
    private const val KEY_LIVE_UPDATE_ENABLED: String = "live_update_enabled"

    fun observe(
        preferences: SharedPreferences,
        onChange: (ChipPreferencesSnapshot) -> Unit
    ): SharedPreferences.OnSharedPreferenceChangeListener {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == null ||
                key == KEY_MODE ||
                key == KEY_TEXT_MODE ||
                key == KEY_LIVE_UPDATE_ENABLED
            ) {
                onChange(readSnapshot(prefs))
            }
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onChange(readSnapshot(preferences))
        return listener
    }

    fun readIndicatorMode(context: Context): ChipIndicatorMode {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return readIndicatorMode(prefs)
    }

    fun readIndicatorMode(preferences: SharedPreferences): ChipIndicatorMode {
        val value = preferences.getString(KEY_MODE, null)
        return ChipIndicatorMode.fromPrefValue(value)
    }

    fun writeIndicatorMode(context: Context, mode: ChipIndicatorMode) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        writeIndicatorMode(prefs, mode)
    }

    fun writeIndicatorMode(preferences: SharedPreferences, mode: ChipIndicatorMode) {
        if (readIndicatorMode(preferences) == mode) return
        preferences.edit().putString(KEY_MODE, mode.prefValue).apply()
    }

    fun readTextMode(context: Context): ChipTextMode {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return readTextMode(prefs)
    }

    fun readTextMode(preferences: SharedPreferences): ChipTextMode {
        val value = preferences.getString(KEY_TEXT_MODE, null)
        return ChipTextMode.fromPrefValue(value)
    }

    fun writeTextMode(preferences: SharedPreferences, mode: ChipTextMode) {
        if (readTextMode(preferences) == mode) return
        preferences.edit().putString(KEY_TEXT_MODE, mode.prefValue).apply()
    }

    fun writeTextMode(context: Context, mode: ChipTextMode) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        writeTextMode(prefs, mode)
    }

    fun isLiveUpdateEnabled(preferences: SharedPreferences): Boolean {
        return preferences.getBoolean(KEY_LIVE_UPDATE_ENABLED, true)
    }

    fun isLiveUpdateEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return isLiveUpdateEnabled(prefs)
    }

    fun setLiveUpdateEnabled(preferences: SharedPreferences, enabled: Boolean) {
        if (isLiveUpdateEnabled(preferences) == enabled) return
        preferences.edit().putBoolean(KEY_LIVE_UPDATE_ENABLED, enabled).apply()
    }

    fun setLiveUpdateEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        setLiveUpdateEnabled(prefs, enabled)
    }

    private fun readSnapshot(preferences: SharedPreferences): ChipPreferencesSnapshot {
        return ChipPreferencesSnapshot(
            indicatorMode = readIndicatorMode(preferences),
            textMode = readTextMode(preferences),
            liveUpdateEnabled = isLiveUpdateEnabled(preferences)
        )
    }
}
