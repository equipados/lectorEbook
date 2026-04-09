package com.ebookreader.feature.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ebookreader.core.book.model.TableOfContents
import com.ebookreader.core.book.parser.EpubParser
import com.ebookreader.core.book.parser.PdfParser
import com.ebookreader.core.data.db.entity.BookEntity
import com.ebookreader.core.data.db.entity.BookFormat
import com.ebookreader.core.data.db.entity.BookmarkEntity
import com.ebookreader.core.data.preferences.ReadingPrefs
import com.ebookreader.core.data.preferences.UserPreferences
import com.ebookreader.core.data.repository.BookmarkRepository
import com.ebookreader.core.data.repository.BookRepository
import com.ebookreader.core.tts.controller.TtsController
import com.ebookreader.core.tts.model.TextSegment
import com.ebookreader.core.tts.model.TtsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class ReaderUiState(
    val book: BookEntity? = null,
    val isLoading: Boolean = true,
    val showControls: Boolean = true,
    val toc: TableOfContents = TableOfContents(emptyList()),
    val isFullscreen: Boolean = false,
    val showSettingsSheet: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bookRepository: BookRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val epubParser: EpubParser,
    private val pdfParser: PdfParser,
    private val ttsController: TtsController,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val bookId: Long = checkNotNull(savedStateHandle["bookId"])

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    val ttsState: StateFlow<TtsState> = ttsController.state

    val currentSegment: StateFlow<TextSegment?> = ttsController.currentSegment

    val readingPrefs: StateFlow<ReadingPrefs> = userPreferences.readingPrefs
        .stateIn(viewModelScope, SharingStarted.Eagerly, ReadingPrefs())

    val bookmarks: StateFlow<List<BookmarkEntity>> = bookmarkRepository
        .getBookmarksForBook(bookId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        loadBook()
    }

    fun loadBook() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val book = bookRepository.getById(bookId)
                if (book == null) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Book not found") }
                    return@launch
                }

                val file = File(book.filePath)
                val parser = if (book.format == BookFormat.EPUB) epubParser else pdfParser

                val toc = parser.getTableOfContents(file)
                val content = parser.extractTextContent(file)

                val chapters = content.chapters.map { chapter ->
                    chapter.title to chapter.textContent
                }
                ttsController.loadText(chapters)

                _uiState.update {
                    it.copy(
                        book = book,
                        isLoading = false,
                        toc = toc
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Unknown error")
                }
            }
        }
    }

    fun toggleControls() {
        _uiState.update { it.copy(showControls = !it.showControls) }
    }

    fun toggleFullscreen() {
        _uiState.update { it.copy(isFullscreen = !it.isFullscreen) }
    }

    fun showSettingsSheet() {
        _uiState.update { it.copy(showSettingsSheet = true) }
    }

    fun hideSettingsSheet() {
        _uiState.update { it.copy(showSettingsSheet = false) }
    }

    fun playPauseTts() {
        viewModelScope.launch {
            if (ttsController.state.value.isPlaying) {
                ttsController.pause()
            } else {
                ttsController.play()
            }
        }
    }

    fun stopTts() {
        viewModelScope.launch {
            ttsController.stop()
        }
    }

    fun nextSentence() {
        viewModelScope.launch {
            ttsController.nextSentence()
        }
    }

    fun previousSentence() {
        viewModelScope.launch {
            ttsController.previousSentence()
        }
    }

    fun setTtsSpeed(speed: Float) {
        ttsController.setSpeed(speed)
    }

    fun jumpToChapter(index: Int) {
        viewModelScope.launch {
            ttsController.jumpToChapter(index)
        }
    }

    fun updateProgress(progress: Float, position: String) {
        viewModelScope.launch {
            bookRepository.updateProgress(bookId, progress, position)
        }
    }

    fun addBookmark(position: String, label: String? = null) {
        viewModelScope.launch {
            bookmarkRepository.addBookmark(
                BookmarkEntity(
                    bookId = bookId,
                    position = position,
                    label = label
                )
            )
        }
    }

    fun removeBookmark(bookmark: BookmarkEntity) {
        viewModelScope.launch {
            bookmarkRepository.removeBookmark(bookmark)
        }
    }

    fun updateReadingPrefs(prefs: ReadingPrefs) {
        viewModelScope.launch {
            userPreferences.updateReadingPrefs(prefs)
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsController.shutdown()
    }
}
