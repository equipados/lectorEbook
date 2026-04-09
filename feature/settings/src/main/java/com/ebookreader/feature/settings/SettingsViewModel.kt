package com.ebookreader.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ebookreader.core.data.preferences.AppPrefs
import com.ebookreader.core.data.preferences.AppThemeType
import com.ebookreader.core.data.preferences.ReadingPrefs
import com.ebookreader.core.data.preferences.ReadingThemeType
import com.ebookreader.core.data.preferences.TtsEngineType
import com.ebookreader.core.data.preferences.TtsPrefs
import com.ebookreader.core.data.preferences.UserPreferences
import com.ebookreader.core.tts.controller.TtsController
import com.ebookreader.core.tts.model.TtsVoice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val ttsController: TtsController
) : ViewModel() {

    val readingPrefs: StateFlow<ReadingPrefs> = userPreferences.readingPrefs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ReadingPrefs()
        )

    val ttsPrefs: StateFlow<TtsPrefs> = userPreferences.ttsPrefs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TtsPrefs()
        )

    val appPrefs: StateFlow<AppPrefs> = userPreferences.appPrefs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppPrefs()
        )

    private val _availableVoices = MutableStateFlow<List<TtsVoice>>(emptyList())
    val availableVoices: StateFlow<List<TtsVoice>> = _availableVoices

    init {
        loadAvailableVoices()
    }

    private fun loadAvailableVoices() {
        viewModelScope.launch {
            _availableVoices.value = ttsController.getAvailableVoices()
        }
    }

    fun updateAppTheme(theme: AppThemeType) {
        viewModelScope.launch {
            userPreferences.updateAppPrefs(appPrefs.value.copy(appTheme = theme))
        }
    }

    fun updateFontSize(size: Int) {
        viewModelScope.launch {
            userPreferences.updateReadingPrefs(readingPrefs.value.copy(fontSize = size))
        }
    }

    fun updateReadingTheme(theme: ReadingThemeType) {
        viewModelScope.launch {
            userPreferences.updateReadingPrefs(readingPrefs.value.copy(theme = theme))
        }
    }

    fun updateLineSpacing(spacing: Float) {
        viewModelScope.launch {
            userPreferences.updateReadingPrefs(readingPrefs.value.copy(lineSpacing = spacing))
        }
    }

    fun updateKeepScreenOn(keep: Boolean) {
        viewModelScope.launch {
            userPreferences.updateReadingPrefs(readingPrefs.value.copy(keepScreenOn = keep))
        }
    }

    fun updateTtsEngine(engine: TtsEngineType) {
        viewModelScope.launch {
            userPreferences.updateTtsPrefs(ttsPrefs.value.copy(preferredEngine = engine))
        }
    }

    fun updateTtsSpeed(speed: Float) {
        viewModelScope.launch {
            userPreferences.updateTtsPrefs(ttsPrefs.value.copy(speed = speed))
        }
    }

    fun updateCloudApiKey(apiKey: String) {
        viewModelScope.launch {
            userPreferences.updateTtsPrefs(ttsPrefs.value.copy(cloudApiKey = apiKey))
        }
    }
}
