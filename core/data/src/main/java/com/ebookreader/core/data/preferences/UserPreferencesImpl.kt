package com.ebookreader.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : UserPreferences {

    // -----------------------------------------------------------------------
    // Keys
    // -----------------------------------------------------------------------

    object Keys {
        // Reading
        val FONT_SIZE = intPreferencesKey("reading_font_size")
        val FONT_FAMILY = stringPreferencesKey("reading_font_family")
        val LINE_SPACING = floatPreferencesKey("reading_line_spacing")
        val READING_THEME = stringPreferencesKey("reading_theme")
        val KEEP_SCREEN_ON = booleanPreferencesKey("reading_keep_screen_on")

        // TTS
        val TTS_ENGINE = stringPreferencesKey("tts_preferred_engine")
        val TTS_LOCAL_VOICE = stringPreferencesKey("tts_local_voice_name")
        val TTS_CLOUD_VOICE = stringPreferencesKey("tts_cloud_voice_name")
        val TTS_SPEED = floatPreferencesKey("tts_speed")
        val TTS_CLOUD_API_KEY = stringPreferencesKey("tts_cloud_api_key")

        // App
        val APP_THEME = stringPreferencesKey("app_theme")
        val SCAN_DIRECTORIES = stringSetPreferencesKey("app_scan_directories")
        val LANGUAGE = stringPreferencesKey("app_language")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("app_onboarding_completed")
    }

    // -----------------------------------------------------------------------
    // Flows
    // -----------------------------------------------------------------------

    override val readingPrefs: Flow<ReadingPrefs> = dataStore.data.map { prefs ->
        ReadingPrefs(
            fontSize = prefs[Keys.FONT_SIZE] ?: 16,
            fontFamily = prefs[Keys.FONT_FAMILY] ?: "default",
            lineSpacing = prefs[Keys.LINE_SPACING] ?: 1.5f,
            theme = prefs[Keys.READING_THEME]
                ?.let { runCatching { ReadingThemeType.valueOf(it) }.getOrNull() }
                ?: ReadingThemeType.LIGHT,
            keepScreenOn = prefs[Keys.KEEP_SCREEN_ON] ?: true
        )
    }

    override val ttsPrefs: Flow<TtsPrefs> = dataStore.data.map { prefs ->
        TtsPrefs(
            preferredEngine = prefs[Keys.TTS_ENGINE]
                ?.let { runCatching { TtsEngineType.valueOf(it) }.getOrNull() }
                ?: TtsEngineType.LOCAL,
            localVoiceName = prefs[Keys.TTS_LOCAL_VOICE] ?: "",
            cloudVoiceName = prefs[Keys.TTS_CLOUD_VOICE] ?: "en-US-Neural2-F",
            speed = prefs[Keys.TTS_SPEED] ?: 1.0f,
            cloudApiKey = prefs[Keys.TTS_CLOUD_API_KEY] ?: ""
        )
    }

    override val appPrefs: Flow<AppPrefs> = dataStore.data.map { prefs ->
        AppPrefs(
            appTheme = prefs[Keys.APP_THEME]
                ?.let { runCatching { AppThemeType.valueOf(it) }.getOrNull() }
                ?: AppThemeType.SYSTEM,
            scanDirectories = prefs[Keys.SCAN_DIRECTORIES]?.toList() ?: emptyList(),
            language = prefs[Keys.LANGUAGE] ?: "system",
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false
        )
    }

    // -----------------------------------------------------------------------
    // Update methods
    // -----------------------------------------------------------------------

    override suspend fun updateReadingPrefs(prefs: ReadingPrefs) {
        dataStore.edit { store ->
            store[Keys.FONT_SIZE] = prefs.fontSize
            store[Keys.FONT_FAMILY] = prefs.fontFamily
            store[Keys.LINE_SPACING] = prefs.lineSpacing
            store[Keys.READING_THEME] = prefs.theme.name
            store[Keys.KEEP_SCREEN_ON] = prefs.keepScreenOn
        }
    }

    override suspend fun updateTtsPrefs(prefs: TtsPrefs) {
        dataStore.edit { store ->
            store[Keys.TTS_ENGINE] = prefs.preferredEngine.name
            store[Keys.TTS_LOCAL_VOICE] = prefs.localVoiceName
            store[Keys.TTS_CLOUD_VOICE] = prefs.cloudVoiceName
            store[Keys.TTS_SPEED] = prefs.speed
            store[Keys.TTS_CLOUD_API_KEY] = prefs.cloudApiKey
        }
    }

    override suspend fun updateAppPrefs(prefs: AppPrefs) {
        dataStore.edit { store ->
            store[Keys.APP_THEME] = prefs.appTheme.name
            store[Keys.SCAN_DIRECTORIES] = prefs.scanDirectories.toSet()
            store[Keys.LANGUAGE] = prefs.language
            store[Keys.ONBOARDING_COMPLETED] = prefs.onboardingCompleted
        }
    }
}
