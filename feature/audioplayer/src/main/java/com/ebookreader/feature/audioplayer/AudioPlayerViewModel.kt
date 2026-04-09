package com.ebookreader.feature.audioplayer

import android.content.Context
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ebookreader.core.book.parser.EpubParser
import com.ebookreader.core.book.parser.PdfParser
import com.ebookreader.core.data.db.entity.BookEntity
import com.ebookreader.core.data.db.entity.BookFormat
import com.ebookreader.core.data.repository.BookRepository
import com.ebookreader.core.tts.controller.TtsController
import com.ebookreader.core.tts.model.TextSegment
import com.ebookreader.core.tts.model.TtsState
import com.ebookreader.core.tts.service.TtsPlaybackService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class AudioPlayerUiState(
    val book: BookEntity? = null,
    val isLoading: Boolean = true,
    val chapterTitles: List<String> = emptyList(),
    val sleepTimerMinutes: Int? = null,
    val sleepTimerRemaining: Long = 0
)

@HiltViewModel
class AudioPlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val bookRepository: BookRepository,
    private val epubParser: EpubParser,
    private val pdfParser: PdfParser,
    private val ttsController: TtsController
) : ViewModel() {

    private val bookId: Long = savedStateHandle["bookId"] ?: 0L

    private val _uiState = MutableStateFlow(AudioPlayerUiState())
    val uiState: StateFlow<AudioPlayerUiState> = _uiState.asStateFlow()

    val ttsState: StateFlow<TtsState> = ttsController.state
    val currentSegment: StateFlow<TextSegment?> = ttsController.currentSegment

    private var sleepTimerJob: Job? = null

    init {
        loadBook()
    }

    private fun loadBook() {
        viewModelScope.launch {
            val book = bookRepository.getById(bookId) ?: run {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }
            _uiState.update { it.copy(book = book) }

            val file = File(book.filePath)
            val parser = when (book.format) {
                BookFormat.EPUB -> epubParser
                BookFormat.PDF -> pdfParser
            }

            val content = parser.extractTextContent(file)
            val chapters = content.chapters.map { chapter ->
                chapter.title to chapter.textContent
            }
            val titles = content.chapters.map { it.title }

            ttsController.loadText(chapters)
            _uiState.update { it.copy(isLoading = false, chapterTitles = titles) }

            val intent = Intent(context, TtsPlaybackService::class.java)
            context.startForegroundService(intent)
        }
    }

    fun playPause() {
        viewModelScope.launch {
            if (ttsState.value.isPlaying) ttsController.pause() else ttsController.play()
        }
    }

    fun stop() {
        viewModelScope.launch { ttsController.stop() }
    }

    fun nextSentence() {
        viewModelScope.launch { ttsController.nextSentence() }
    }

    fun previousSentence() {
        viewModelScope.launch { ttsController.previousSentence() }
    }

    fun nextChapter() {
        viewModelScope.launch { ttsController.nextChapter() }
    }

    fun previousChapter() {
        viewModelScope.launch { ttsController.previousChapter() }
    }

    fun setSpeed(speed: Float) {
        ttsController.setSpeed(speed)
    }

    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()

        if (minutes == null) {
            _uiState.update { it.copy(sleepTimerMinutes = null, sleepTimerRemaining = 0) }
            return
        }

        _uiState.update { it.copy(sleepTimerMinutes = minutes) }
        sleepTimerJob = viewModelScope.launch {
            var remaining = minutes * 60L
            while (remaining > 0) {
                _uiState.update { it.copy(sleepTimerRemaining = remaining) }
                delay(1000)
                remaining--
            }
            ttsController.pause()
            _uiState.update { it.copy(sleepTimerMinutes = null, sleepTimerRemaining = 0) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sleepTimerJob?.cancel()
    }
}