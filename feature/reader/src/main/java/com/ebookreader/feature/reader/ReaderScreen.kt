package com.ebookreader.feature.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.ebookreader.core.data.db.entity.BookFormat
import com.ebookreader.core.data.preferences.ReadingPrefs
import com.ebookreader.feature.reader.components.ReaderBottomBar
import com.ebookreader.feature.reader.components.ReaderTopBar
import com.ebookreader.feature.reader.components.ReadingSettingsSheet
import com.ebookreader.feature.reader.components.TableOfContentsDrawer
import com.ebookreader.feature.reader.epub.EpubReaderView
import com.ebookreader.feature.reader.pdf.PdfReaderView
import kotlinx.coroutines.launch

@Composable
fun ReaderScreen(
    bookId: Long,
    onBack: () -> Unit,
    onSwitchToAudio: (Long) -> Unit,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val ttsState by viewModel.ttsState.collectAsState()
    val currentSegment by viewModel.currentSegment.collectAsState()
    val readingPrefs by viewModel.readingPrefs.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    val book = uiState.book
    val isBookmarked = bookmarks.any { it.position == (book?.lastPosition ?: "") }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            TableOfContentsDrawer(
                toc = uiState.toc,
                onEntryClick = { entry, index ->
                    viewModel.jumpToChapter(index)
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                AnimatedVisibility(
                    visible = uiState.showControls,
                    enter = fadeIn() + slideInVertically { -it },
                    exit = fadeOut() + slideOutVertically { -it }
                ) {
                    ReaderTopBar(
                        title = book?.title ?: "",
                        isBookmarked = isBookmarked,
                        onBack = onBack,
                        onOpenToc = { scope.launch { drawerState.open() } },
                        onToggleBookmark = {
                            val pos = book?.lastPosition ?: ""
                            val existing = bookmarks.find { it.position == pos }
                            if (existing != null) {
                                viewModel.removeBookmark(existing)
                            } else {
                                viewModel.addBookmark(pos, label = book?.title)
                            }
                        },
                        onSwitchToAudio = { book?.let { onSwitchToAudio(it.id) } }
                    )
                }
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = uiState.showControls,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it }
                ) {
                    ReaderBottomBar(
                        progress = book?.progress ?: 0f,
                        isPlaying = ttsState.isPlaying,
                        onPlayPauseTts = viewModel::playPauseTts,
                        onOpenSettings = viewModel::showSettingsSheet,
                        onFontSmaller = {
                            viewModel.decreaseFontSize()
                            val newSize = (readingPrefs.fontSize - 2).coerceAtLeast(10)
                            scope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                snackbarHostState.showSnackbar("Tamaño: ${newSize}px")
                            }
                        },
                        onFontLarger = {
                            viewModel.increaseFontSize()
                            val newSize = (readingPrefs.fontSize + 2).coerceAtMost(48)
                            scope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                snackbarHostState.showSnackbar("Tamaño: ${newSize}px")
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }

                    book == null -> {
                        Text(
                            text = uiState.errorMessage ?: "Book not found",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    book.format == BookFormat.EPUB -> {
                        val chapterPath = uiState.chapterFiles.getOrNull(uiState.currentChapterIndex)
                        if (chapterPath != null) {
                            EpubReaderView(
                                chapterFilePath = chapterPath,
                                readingPrefs = readingPrefs,
                                currentTtsSegment = currentSegment,
                                onPreviousChapter = viewModel::previousChapter,
                                onNextChapter = viewModel::nextChapter,
                                onTap = viewModel::toggleControls,
                                onFontLarger = viewModel::increaseFontSize,
                                onFontSmaller = viewModel::decreaseFontSize,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = "No se pudo extraer el contenido del EPUB",
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }

                    book.format == BookFormat.PDF -> {
                        PdfReaderView(
                            filePath = book.filePath,
                            onPageChanged = { progress ->
                                viewModel.updateProgress(progress, progress.toString())
                            },
                            onTap = viewModel::toggleControls,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

            }
        }

        // Settings sheet
        if (uiState.showSettingsSheet) {
            ReadingSettingsSheet(
                readingPrefs = readingPrefs,
                ttsSpeed = ttsState.speed,
                onDismiss = viewModel::hideSettingsSheet,
                onFontSizeChange = { size ->
                    viewModel.updateReadingPrefs(readingPrefs.copy(fontSize = size))
                },
                onThemeChange = { theme ->
                    viewModel.updateReadingPrefs(readingPrefs.copy(theme = theme))
                },
                onTtsSpeedChange = viewModel::setTtsSpeed
            )
        }
    }
}
