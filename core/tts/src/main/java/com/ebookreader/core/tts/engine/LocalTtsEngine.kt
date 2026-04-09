package com.ebookreader.core.tts.engine

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.ebookreader.core.tts.model.EngineType
import com.ebookreader.core.tts.model.TtsVoice
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class LocalTtsEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : TtsEngine {

    private var tts: TextToSpeech? = null
    private var initialized = false

    override suspend fun initialize(): Boolean = suspendCancellableCoroutine { continuation ->
        val instance = TextToSpeech(context) { status ->
            initialized = status == TextToSpeech.SUCCESS
            continuation.resume(initialized)
        }
        tts = instance

        continuation.invokeOnCancellation {
            instance.shutdown()
            tts = null
            initialized = false
        }
    }

    override suspend fun speak(text: String, onDone: () -> Unit) {
        val engine = tts ?: return
        val utteranceId = "utterance_${System.currentTimeMillis()}"

        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                onDone()
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {}

            override fun onError(utteranceId: String?, errorCode: Int) {}
        })

        val params = Bundle()
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    override suspend fun stop() {
        tts?.stop()
    }

    override suspend fun pause() {
        // Android TTS has no native pause; stop is the closest approximation
        tts?.stop()
    }

    override suspend fun resume() {
        // Resume is not natively supported; the controller handles re-speaking the segment
    }

    override fun setSpeed(speed: Float) {
        tts?.setSpeechRate(speed)
    }

    override fun setVoice(voice: TtsVoice) {
        val engine = tts ?: return
        val androidVoice = engine.voices?.find { it.name == voice.name }
        if (androidVoice != null) {
            engine.voice = androidVoice
        }
    }

    override suspend fun getAvailableVoices(): List<TtsVoice> {
        val engine = tts ?: return emptyList()
        return engine.voices?.map { androidVoice ->
            TtsVoice(
                id = androidVoice.name,
                name = androidVoice.name,
                language = androidVoice.locale.language,
                engineType = EngineType.LOCAL
            )
        } ?: emptyList()
    }

    override fun detectLanguage(text: String): String {
        return Locale.getDefault().language
    }

    override fun isInitialized(): Boolean = initialized

    override fun shutdown() {
        tts?.shutdown()
        tts = null
        initialized = false
    }
}
