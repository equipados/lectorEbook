package com.ebookreader.core.tts.engine

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.util.Base64
import android.util.Log
import com.ebookreader.core.data.preferences.UserPreferences
import com.ebookreader.core.tts.cache.TtsCacheManager
import com.ebookreader.core.tts.model.EngineType
import com.ebookreader.core.tts.model.TtsVoice
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google Cloud Text-to-Speech via REST API con API key.
 *
 * Usa el endpoint público `https://texttospeech.googleapis.com/v1/...`
 * autenticado con un parámetro `key=API_KEY`, en lugar del SDK oficial
 * que requiere un service account JSON (credenciales completas).
 */
@Singleton
class CloudTtsEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cacheManager: TtsCacheManager,
    private val userPreferences: UserPreferences
) : TtsEngine {

    private var mediaPlayer: MediaPlayer? = null
    private var initialized = false
    private var speed: Float = 1.0f
    private var selectedVoice: TtsVoice? = null

    override suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        val prefs = userPreferences.ttsPrefs.first()
        speed = prefs.speed
        if (selectedVoice == null && prefs.cloudVoiceName.isNotBlank()) {
            selectedVoice = TtsVoice(
                id = prefs.cloudVoiceName,
                name = prefs.cloudVoiceName,
                language = extractLanguageCode(prefs.cloudVoiceName),
                engineType = EngineType.CLOUD
            )
        }
        initialized = prefs.cloudApiKey.isNotBlank()
        initialized
    }

    override suspend fun speak(text: String, onDone: () -> Unit) {
        if (!initialized && !initialize()) {
            Log.w(TAG, "Cloud TTS not initialized (missing API key?)")
            onDone()
            return
        }

        val voiceId = selectedVoice?.id ?: getPreferredVoiceId()
        val cachedAudioPath = cacheManager.getCachedAudio(text, voiceId)
        if (cachedAudioPath != null && File(cachedAudioPath).exists()) {
            playAudioFile(cachedAudioPath, onDone)
            return
        }

        val audioPath = synthesizeToFile(text, voiceId)
        if (audioPath == null) {
            Log.w(TAG, "Synthesis failed, skipping segment")
            onDone()
            return
        }
        cacheManager.cacheAudio(text, voiceId, audioPath)
        playAudioFile(audioPath, onDone)
    }

    override suspend fun stop() {
        withContext(Dispatchers.Main) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    override suspend fun pause() {
        withContext(Dispatchers.Main) {
            mediaPlayer?.takeIf { it.isPlaying }?.pause()
        }
    }

    override suspend fun resume() {
        withContext(Dispatchers.Main) {
            mediaPlayer?.takeIf { !it.isPlaying }?.start()
        }
    }

    override fun setSpeed(speed: Float) {
        this.speed = speed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mediaPlayer?.let { player ->
                runCatching {
                    player.playbackParams = player.playbackParams.setSpeed(speed)
                }
            }
        }
    }

    override fun setVoice(voice: TtsVoice) {
        selectedVoice = voice
    }

    override suspend fun getAvailableVoices(): List<TtsVoice> = withContext(Dispatchers.IO) {
        val apiKey = userPreferences.ttsPrefs.first().cloudApiKey
        if (apiKey.isBlank()) return@withContext emptyList()

        runCatching {
            val url = URL("https://texttospeech.googleapis.com/v1/voices?key=${urlEncode(apiKey)}")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 15000
            }
            try {
                if (conn.responseCode != 200) {
                    Log.e(TAG, "listVoices HTTP ${conn.responseCode}")
                    return@runCatching emptyList<TtsVoice>()
                }
                val body = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(body)
                val voicesArray = json.optJSONArray("voices") ?: return@runCatching emptyList<TtsVoice>()
                val out = mutableListOf<TtsVoice>()
                for (i in 0 until voicesArray.length()) {
                    val v = voicesArray.getJSONObject(i)
                    val name = v.optString("name")
                    val langs = v.optJSONArray("languageCodes")
                    val firstLang = if (langs != null && langs.length() > 0) langs.getString(0) else "und"
                    out.add(
                        TtsVoice(
                            id = name,
                            name = name,
                            language = firstLang,
                            engineType = EngineType.CLOUD
                        )
                    )
                }
                out
            } finally {
                conn.disconnect()
            }
        }.getOrElse {
            Log.e(TAG, "listVoices error", it)
            emptyList()
        }
    }

    override fun detectLanguage(text: String): String {
        return Locale.getDefault().language
    }

    override fun isInitialized(): Boolean = initialized

    override fun shutdown() {
        runCatching {
            mediaPlayer?.release()
            mediaPlayer = null
            initialized = false
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private suspend fun synthesizeToFile(text: String, voiceId: String): String? =
        withContext(Dispatchers.IO) {
            val apiKey = userPreferences.ttsPrefs.first().cloudApiKey
            if (apiKey.isBlank()) {
                Log.w(TAG, "API key is blank")
                return@withContext null
            }

            val languageCode = selectedVoice?.language ?: extractLanguageCode(voiceId)

            val jsonBody = JSONObject().apply {
                put("input", JSONObject().put("text", text))
                put(
                    "voice",
                    JSONObject().apply {
                        put("languageCode", languageCode)
                        put("name", voiceId)
                    }
                )
                put(
                    "audioConfig",
                    JSONObject().apply {
                        put("audioEncoding", "MP3")
                        put("speakingRate", speed.toDouble())
                    }
                )
            }.toString()

            runCatching {
                val url = URL(
                    "https://texttospeech.googleapis.com/v1/text:synthesize?key=${urlEncode(apiKey)}"
                )
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    connectTimeout = 15000
                    readTimeout = 30000
                }

                try {
                    conn.outputStream.use { it.write(jsonBody.toByteArray(Charsets.UTF_8)) }

                    if (conn.responseCode != 200) {
                        val err = runCatching {
                            conn.errorStream?.bufferedReader()?.readText()
                        }.getOrNull()
                        Log.e(TAG, "synthesize HTTP ${conn.responseCode}: $err")
                        return@runCatching null
                    }

                    val responseText = conn.inputStream.bufferedReader().readText()
                    val audioBase64 = JSONObject(responseText).getString("audioContent")
                    val audioBytes = Base64.decode(audioBase64, Base64.DEFAULT)

                    val safeVoiceId = voiceId.replace(Regex("[^A-Za-z0-9._-]"), "_")
                    val outputDir = File(context.cacheDir, "tts_cloud").apply { mkdirs() }
                    val outputFile = File(
                        outputDir,
                        "${System.currentTimeMillis()}_$safeVoiceId.mp3"
                    )
                    outputFile.writeBytes(audioBytes)
                    outputFile.absolutePath
                } finally {
                    conn.disconnect()
                }
            }.getOrElse {
                Log.e(TAG, "synthesize error", it)
                null
            }
        }

    private suspend fun playAudioFile(audioPath: String, onDone: () -> Unit) {
        withContext(Dispatchers.Main) {
            mediaPlayer?.release()
            mediaPlayer = null

            val player = MediaPlayer().apply {
                setDataSource(audioPath)
                setOnCompletionListener {
                    onDone()
                }
                setOnErrorListener { mp, _, _ ->
                    mp.release()
                    mediaPlayer = null
                    onDone()
                    true
                }
                prepare()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    runCatching {
                        playbackParams = playbackParams.setSpeed(speed)
                    }
                }
                start()
            }

            mediaPlayer = player
        }
    }

    private suspend fun getPreferredVoiceId(): String {
        val prefs = userPreferences.ttsPrefs.first()
        return prefs.cloudVoiceName.ifBlank { "es-ES-Standard-A" }
    }

    private fun extractLanguageCode(voiceName: String): String {
        val parts = voiceName.split("-")
        return if (parts.size >= 2) "${parts[0]}-${parts[1]}" else Locale.getDefault().toLanguageTag()
    }

    private fun urlEncode(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name())

    companion object {
        private const val TAG = "CloudTtsEngine"
    }
}
