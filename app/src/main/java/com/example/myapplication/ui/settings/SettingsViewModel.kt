package com.example.myapplication.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class SettingsUiState(
    val isBuiltInMusicEnabled: Boolean = true
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)

    val uiState: StateFlow<SettingsUiState> = settingsRepository.isBuiltInMusicEnabled
        .map { isEnabled -> SettingsUiState(isBuiltInMusicEnabled = isEnabled) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingsUiState()
        )

    fun setBuiltInMusicEnabled(isEnabled: Boolean) {
        settingsRepository.setBuiltInMusicEnabled(isEnabled)
    }
}
