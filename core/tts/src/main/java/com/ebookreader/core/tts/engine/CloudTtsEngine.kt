package com.ebookreader.core.tts.engine

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import com.ebookreader.core.data.preferences.UserPreferences
import com.ebookreader.core.tts.cache.TtsCacheManager
import com.ebookreader.core.tts.model.EngineType
import com.ebookreader.core.tts.model.TtsVoice
import com.google.cloud.texttospeech.v1.AudioConfig
import com.google.cloud.texttospeech.v1.AudioEncoding
import com.google.cloud.texttospeech.v1.ListVoicesRequest
import com.google.cloud.texttospeech.v1.SynthesisInput
import com.google.cloud.texttospeech.v1.SynthesizeSpeechRequest
import com.google.cloud.texttospeech.v1.TextToSpeechClient
import com.google.cloud.texttospeech.v1.VoiceSelectionParams
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudTtsEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cacheManager: TtsCacheManager,
    private val userPreferences: UserPreferences
) : TtsEngine {

    private var ttsClient: TextToSpeechClient? = null
    private var mediaPlayer: MediaPlayer? = null
    private var initialized = false
    private var speed: Float = 1.0f
    private var selectedVoice: TtsVoice? = null

    override suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        if (initialized && ttsClient != null) return@withContext true

        runCatching {
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
            ttsClient = TextToSpeechClient.create()
            initialized = true
            true
        }.getOrElse {
            initialized = false
            ttsClient = null
            false
        }
    }

    override suspend fun speak(text: String, onDone: () -> Unit) {
        if (!initialized && !initialize()) return

        val voiceId = selectedVoice?.id ?: getPreferredVoiceId()
        val cachedAudioPath = cacheManager.getCachedAudio(text, voiceId)
        if (cachedAudioPath != null && File(cachedAudioPath).exists()) {
            playAudioFile(cachedAudioPath, onDone)
            return
        }

        val audioPath = synthesizeToFile(text, voiceId) ?: return
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
        if (!initialized && !initialize()) {
            return@withContext emptyList()
        }

        val client = ttsClient ?: return@withContext emptyList()
        runCatching {
            client.listVoices(ListVoicesRequest.getDefaultInstance()).voicesList.map { voice ->
                TtsVoice(
                    id = voice.name,
                    name = voice.name,
                    language = voice.languageCodesList.firstOrNull() ?: "und",
                    engineType = EngineType.CLOUD
                )
            }
        }.getOrElse { emptyList() }
    }

    override fun detectLanguage(text: String): String {
        return Locale.getDefault().language
    }

    override fun isInitialized(): Boolean = initialized

    override fun shutdown() {
        runCatching {
            mediaPlayer?.release()
            mediaPlayer = null
            ttsClient?.close()
            ttsClient = null
            initialized = false
        }
    }

    private suspend fun synthesizeToFile(text: String, voiceId: String): String? = withContext(Dispatchers.IO) {
        val client = ttsClient ?: return@withContext null

        runCatching {
            val request = SynthesizeSpeechRequest.newBuilder()
                .setInput(SynthesisInput.newBuilder().setText(text).build())
                .setVoice(
                    VoiceSelectionParams.newBuilder()
                        .setLanguageCode(selectedVoice?.language ?: extractLanguageCode(voiceId))
                        .setName(voiceId)
                        .build()
                )
                .setAudioConfig(
                    AudioConfig.newBuilder()
                        .setAudioEncoding(AudioEncoding.MP3)
                        .setSpeakingRate(speed.toDouble())
                        .build()
                )
                .build()

            val response = client.synthesizeSpeech(request)
            val safeVoiceId = voiceId.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val outputFile = File(
                context.cacheDir,
                "tts_cloud/${System.currentTimeMillis()}_$safeVoiceId.mp3"
            )
            outputFile.parentFile?.mkdirs()

            FileOutputStream(outputFile).use { stream ->
                stream.write(response.audioContent.toByteArray())
            }

            outputFile.absolutePath
        }.getOrNull()
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
        return prefs.cloudVoiceName.ifBlank { "en-US-Neural2-F" }
    }

    private fun extractLanguageCode(voiceName: String): String {
        val parts = voiceName.split("-")
        return if (parts.size >= 2) "${parts[0]}-${parts[1]}" else Locale.getDefault().toLanguageTag()
    }
}
