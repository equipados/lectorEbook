package com.ebookreader.core.data.preferences

import kotlinx.coroutines.flow.Flow

// ---------------------------------------------------------------------------
// Enums
// ---------------------------------------------------------------------------

enum class ReadingThemeType { LIGHT, DARK, SEPIA }

enum class TtsEngineType { LOCAL, CLOUD }

enum class AppThemeType { LIGHT, DARK, SYSTEM }

// ---------------------------------------------------------------------------
// Preference data classes
// ---------------------------------------------------------------------------

data class ReadingPrefs(
    val fontSize: Int = 16,
    val fontFamily: String = "default",
    val lineSpacing: Float = 1.5f,
    val theme: ReadingThemeType = ReadingThemeType.LIGHT,
    val keepScreenOn: Boolean = true
)

data class TtsPrefs(
    val preferredEngine: TtsEngineType = TtsEngineType.LOCAL,
    val localVoiceName: String = "",
    val cloudVoiceName: String = "es-ES-Standard-A",
    val speed: Float = 1.0f,
    val cloudApiKey: String = ""
)

data class AppPrefs(
    val appTheme: AppThemeType = AppThemeType.SYSTEM,
    val scanDirectories: List<String> = emptyList(),
    val language: String = "system",
    val onboardingCompleted: Boolean = false
)

// ---------------------------------------------------------------------------
// Interface
// ---------------------------------------------------------------------------

interface UserPreferences {

    val readingPrefs: Flow<ReadingPrefs>
    val ttsPrefs: Flow<TtsPrefs>
    val appPrefs: Flow<AppPrefs>

    suspend fun updateReadingPrefs(prefs: ReadingPrefs)
    suspend fun updateTtsPrefs(prefs: TtsPrefs)
    suspend fun updateAppPrefs(prefs: AppPrefs)
}
