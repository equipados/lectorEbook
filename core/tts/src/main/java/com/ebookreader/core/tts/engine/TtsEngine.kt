package com.ebookreader.core.tts.engine

import com.ebookreader.core.tts.model.TtsVoice

interface TtsEngine {

    suspend fun initialize(): Boolean

    suspend fun speak(text: String, onDone: () -> Unit)

    suspend fun stop()

    suspend fun pause()

    suspend fun resume()

    fun setSpeed(speed: Float)

    fun setVoice(voice: TtsVoice)

    suspend fun getAvailableVoices(): List<TtsVoice>

    fun detectLanguage(text: String): String

    fun isInitialized(): Boolean

    fun shutdown()
}
