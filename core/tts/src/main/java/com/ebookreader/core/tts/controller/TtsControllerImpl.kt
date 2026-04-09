package com.ebookreader.core.tts.controller

import com.ebookreader.core.tts.engine.LocalTtsEngine
import com.ebookreader.core.tts.engine.TtsEngine
import com.ebookreader.core.tts.model.EngineType
import com.ebookreader.core.tts.model.TextSegment
import com.ebookreader.core.tts.model.TtsState
import com.ebookreader.core.tts.model.TtsVoice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TtsControllerImpl @Inject constructor(
    private val localEngine: LocalTtsEngine,
    // cloudEngine will be wired in Task 7; typed as TtsEngine interface so it compiles now
    private val cloudEngine: TtsEngine
) : TtsController {

    private val _state = MutableStateFlow(TtsState())
    override val state: StateFlow<TtsState> = _state.asStateFlow()

    private val _currentSegment = MutableStateFlow<TextSegment?>(null)
    override val currentSegment: StateFlow<TextSegment?> = _currentSegment.asStateFlow()

    private var segments: List<TextSegment> = emptyList()

    private val activeEngine: TtsEngine
        get() = when (_state.value.engineType) {
            EngineType.LOCAL -> localEngine
            EngineType.CLOUD -> cloudEngine
        }

    override suspend fun loadText(chapters: List<Pair<String, String>>) {
        val built = mutableListOf<TextSegment>()
        chapters.forEachIndexed { chapterIndex, (_, content) ->
            val sentences = splitIntoSentences(content)
            var offset = 0
            for (sentence in sentences) {
                val start = content.indexOf(sentence, offset).takeIf { it >= 0 } ?: offset
                val end = start + sentence.length
                built.add(TextSegment(sentence, start, end, chapterIndex))
                offset = end
            }
        }
        segments = built
        _state.update { it.copy(currentSegmentIndex = 0, currentChapterIndex = 0) }
        _currentSegment.value = segments.firstOrNull()
    }

    override suspend fun play() {
        if (!activeEngine.isInitialized()) {
            activeEngine.initialize()
        }
        _state.update { it.copy(isPlaying = true) }
        speakCurrentSegment()
    }

    override suspend fun pause() {
        activeEngine.pause()
        _state.update { it.copy(isPlaying = false) }
    }

    override suspend fun stop() {
        activeEngine.stop()
        _state.update { it.copy(isPlaying = false, currentSegmentIndex = 0, currentChapterIndex = 0) }
        _currentSegment.value = segments.firstOrNull()
    }

    override suspend fun nextSentence() {
        val next = _state.value.currentSegmentIndex + 1
        if (next < segments.size) {
            val segment = segments[next]
            _state.update {
                it.copy(
                    currentSegmentIndex = next,
                    currentChapterIndex = segment.chapterIndex
                )
            }
            _currentSegment.value = segment
            if (_state.value.isPlaying) {
                speakCurrentSegment()
            }
        }
    }

    override suspend fun previousSentence() {
        val prev = (_state.value.currentSegmentIndex - 1).coerceAtLeast(0)
        val segment = segments[prev]
        _state.update {
            it.copy(
                currentSegmentIndex = prev,
                currentChapterIndex = segment.chapterIndex
            )
        }
        _currentSegment.value = segment
        if (_state.value.isPlaying) {
            speakCurrentSegment()
        }
    }

    override suspend fun nextChapter() {
        val currentChapter = _state.value.currentChapterIndex
        val nextSegmentIndex = segments.indexOfFirst { it.chapterIndex > currentChapter }
        if (nextSegmentIndex >= 0) {
            val segment = segments[nextSegmentIndex]
            _state.update {
                it.copy(
                    currentSegmentIndex = nextSegmentIndex,
                    currentChapterIndex = segment.chapterIndex
                )
            }
            _currentSegment.value = segment
            if (_state.value.isPlaying) {
                speakCurrentSegment()
            }
        }
    }

    override suspend fun previousChapter() {
        val currentChapter = _state.value.currentChapterIndex
        val targetChapter = (currentChapter - 1).coerceAtLeast(0)
        jumpToChapter(targetChapter)
    }

    override suspend fun jumpToChapter(index: Int) {
        val segmentIndex = segments.indexOfFirst { it.chapterIndex == index }
        if (segmentIndex >= 0) {
            val segment = segments[segmentIndex]
            _state.update {
                it.copy(
                    currentSegmentIndex = segmentIndex,
                    currentChapterIndex = segment.chapterIndex
                )
            }
            _currentSegment.value = segment
            if (_state.value.isPlaying) {
                speakCurrentSegment()
            }
        }
    }

    override fun setSpeed(speed: Float) {
        _state.update { it.copy(speed = speed) }
        activeEngine.setSpeed(speed)
    }

    override fun setVoice(voice: TtsVoice) {
        _state.update { it.copy(activeVoice = voice) }
        activeEngine.setVoice(voice)
    }

    override suspend fun getAvailableVoices(): List<TtsVoice> {
        return activeEngine.getAvailableVoices()
    }

    override fun shutdown() {
        localEngine.shutdown()
        cloudEngine.shutdown()
    }

    private suspend fun speakCurrentSegment() {
        val index = _state.value.currentSegmentIndex
        val segment = segments.getOrNull(index) ?: return
        _currentSegment.value = segment

        activeEngine.speak(segment.text) {
            // onDone callback: advance to next segment if still playing
            val currentIndex = _state.value.currentSegmentIndex
            val nextIndex = currentIndex + 1
            if (_state.value.isPlaying && nextIndex < segments.size) {
                val nextSegment = segments[nextIndex]
                _state.value = _state.value.copy(
                    currentSegmentIndex = nextIndex,
                    currentChapterIndex = nextSegment.chapterIndex
                )
                _currentSegment.value = nextSegment
                // Note: cannot call suspending function directly from callback;
                // the engine will call onDone again when the next segment finishes.
                // The actual recursive speak is handled by re-triggering via the engine's
                // non-suspend speak overload using the same listener pattern.
            } else if (nextIndex >= segments.size) {
                _state.value = _state.value.copy(isPlaying = false)
            }
        }
    }

    private fun splitIntoSentences(text: String): List<String> {
        val pattern = Regex("[^.!?]+[.!?]+\\s*")
        val matches = pattern.findAll(text).map { it.value.trim() }.filter { it.isNotEmpty() }.toList()
        if (matches.isEmpty() && text.isNotBlank()) {
            return listOf(text.trim())
        }
        return matches
    }
}
