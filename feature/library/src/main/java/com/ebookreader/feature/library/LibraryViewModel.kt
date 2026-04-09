package com.ebookreader.feature.library

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ebookreader.core.book.parser.BookParser
import com.ebookreader.core.book.parser.EpubParser
import com.ebookreader.core.book.parser.PdfParser
import com.ebookreader.core.book.scanner.BookScanner
import com.ebookreader.core.data.db.entity.BookEntity
import com.ebookreader.core.data.repository.BookRepository
import com.ebookreader.core.data.repository.SortOrder
import com.ebookreader.core.data.db.entity.BookFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context,
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

    fun importBook(uri: Uri) {
        viewModelScope.launch {
            try {
                // Get real filename from content resolver
                val fileName = getFileName(uri) ?: "book_${System.currentTimeMillis()}"
                val extension = fileName.substringAfterLast('.', "").lowercase()

                // Determine format from extension or MIME type
                val mimeType = context.contentResolver.getType(uri) ?: ""
                val format = when {
                    extension == "epub" || mimeType.contains("epub") -> BookFormat.EPUB
                    extension == "pdf" || mimeType.contains("pdf") -> BookFormat.PDF
                    else -> return@launch
                }

                val finalName = if (fileName.contains('.')) fileName
                    else "$fileName.${if (format == BookFormat.EPUB) "epub" else "pdf"}"

                // Copy file to app's internal storage
                val booksDir = File(context.filesDir, "books")
                booksDir.mkdirs()
                val destFile = File(booksDir, finalName)

                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                } ?: return@launch

                val existing = bookRepository.getByFilePath(destFile.absolutePath)
                if (existing != null) return@launch

                val parser = getParser(destFile) ?: return@launch
                val metadata = parser.parseMetadata(destFile)
                val coverDir = File(booksDir, ".covers")
                coverDir.mkdirs()
                val coverPath = try { parser.extractCover(destFile, coverDir) } catch (_: Exception) { null }

                bookRepository.insert(
                    BookEntity(
                        title = metadata.title.ifBlank { finalName.substringBeforeLast('.') },
                        author = metadata.author.ifBlank { "Desconocido" },
                        coverPath = coverPath,
                        filePath = destFile.absolutePath,
                        format = format
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getFileName(uri: Uri): String? {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) return cursor.getString(nameIndex)
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')
    }

    private fun getParser(file: File): BookParser? {
        return when (file.extension.lowercase()) {
            "epub" -> epubParser
            "pdf" -> pdfParser
            else -> null
        }
    }
}
