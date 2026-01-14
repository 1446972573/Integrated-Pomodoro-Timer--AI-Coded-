package com.example.myapplication.data.repository

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    private val _isBuiltInMusicEnabled = MutableStateFlow(prefs.getBoolean(KEY_BUILT_IN_MUSIC, true))
    val isBuiltInMusicEnabled = _isBuiltInMusicEnabled.asStateFlow()

    fun setBuiltInMusicEnabled(isEnabled: Boolean) {
        _isBuiltInMusicEnabled.value = isEnabled
        prefs.edit {
            putBoolean(KEY_BUILT_IN_MUSIC, isEnabled)
        }
    }

    companion object {
        private const val KEY_BUILT_IN_MUSIC = "built_in_music_enabled"
    }
}
