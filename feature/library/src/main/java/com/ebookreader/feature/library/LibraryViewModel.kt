package com.ebookreader.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ebookreader.core.book.parser.BookParser
import com.ebookreader.core.book.parser.EpubParser
import com.ebookreader.core.book.parser.PdfParser
import com.ebookreader.core.book.scanner.BookScanner
import com.ebookreader.core.data.db.entity.BookEntity
import com.ebookreader.core.data.repository.BookRepository
import com.ebookreader.core.data.repository.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class LibraryUiState(
    val isScanning: Boolean = false,
    val sortOrder: SortOrder = SortOrder.RECENT,
    val searchQuery: String = ""
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val bookScanner: BookScanner,
    private val epubParser: EpubParser,
    private val pdfParser: PdfParser
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val books: StateFlow<List<BookEntity>> = _uiState
        .flatMapLatest { state ->
            if (state.searchQuery.isBlank()) {
                bookRepository.getAll(state.sortOrder)
            } else {
                bookRepository.search(state.searchQuery)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        scanForBooks()
    }

    fun scanForBooks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true) }
            try {
                val files = bookScanner.scanForBooks()
                for (file in files) {
                    val existing = bookRepository.getByFilePath(file.absolutePath)
                    if (existing == null) {
                        val parser = getParser(file) ?: continue
                        val metadata = parser.parseMetadata(file)
                        val coverDir = File(file.parentFile, ".covers")
                        val coverPath = parser.extractCover(file, coverDir)
                        bookRepository.insert(
                            BookEntity(
                                title = metadata.title,
                                author = metadata.author,
                                coverPath = coverPath,
                                filePath = file.absolutePath,
                                format = metadata.format
                            )
                        )
                    }
                }
            } finally {
                _uiState.update { it.copy(isScanning = false) }
            }
        }
    }

    fun setSortOrder(order: SortOrder) {
        _uiState.update { it.copy(sortOrder = order) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun deleteBook(book: BookEntity) {
        viewModelScope.launch {
            bookRepository.delete(book)
        }
    }

    private fun getParser(file: File): BookParser? {
        return when (file.extension.lowercase()) {
            "epub" -> epubParser
            "pdf" -> pdfParser
            else -> null
        }
    }
}
