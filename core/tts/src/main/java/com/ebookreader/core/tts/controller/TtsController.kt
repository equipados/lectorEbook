package com.ebookreader.core.tts.controller

import com.ebookreader.core.tts.model.TextSegment
import com.ebookreader.core.tts.model.TtsState
import com.ebookreader.core.tts.model.TtsVoice
import kotlinx.coroutines.flow.StateFlow

interface TtsController {

    val state: StateFlow<TtsState>

    val currentSegment: StateFlow<TextSegment?>

    /**
     * Load chapters as title-content pairs and split into speakable segments.
     */
    suspend fun loadText(chapters: List<Pair<String, String>>)

    suspend fun play()

    suspend fun pause()

    suspend fun stop()

    suspend fun nextSentence()

    suspend fun previousSentence()

    suspend fun nextChapter()

    suspend fun previousChapter()

    suspend fun jumpToChapter(index: Int)

    fun setSpeed(speed: Float)

    fun setVoice(voice: TtsVoice)

    suspend fun getAvailableVoices(): List<TtsVoice>

    fun shutdown()
}
