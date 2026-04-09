package com.ebookreader.feature.audioplayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ebookreader.feature.audioplayer.components.AudioControls
import com.ebookreader.feature.audioplayer.components.CoverDisplay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerScreen(
    bookId: Long,
    onBack: () -> Unit,
    onSwitchToReader: () -> Unit,
    viewModel: AudioPlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val ttsState by viewModel.ttsState.collectAsState()
    var showSleepTimer by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audio Player") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSwitchToReader) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = "Switch to reader"
                        )
                    }
                    IconButton(onClick = { showSleepTimer = true }) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Sleep timer"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val book = uiState.book ?: return@Scaffold
        val currentChapter = uiState.chapterTitles.getOrNull(ttsState.currentChapterIndex) ?: ""

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            CoverDisplay(
                coverPath = book.coverPath,
                title = book.title,
                author = book.author,
                currentChapter = currentChapter
            )

            Spacer(modifier = Modifier.height(32.dp))

            AudioControls(
                isPlaying = ttsState.isPlaying,
                speed = ttsState.speed,
                onPlayPause = viewModel::playPause,
                onStop = viewModel::stop,
                onPreviousSentence = viewModel::previousSentence,
                onNextSentence = viewModel::nextSentence,
                onPreviousChapter = viewModel::previousChapter,
                onNextChapter = viewModel::nextChapter,
                onSpeedChange = viewModel::setSpeed
            )

            if (uiState.sleepTimerMinutes != null) {
                val minutes = uiState.sleepTimerRemaining / 60
                val seconds = uiState.sleepTimerRemaining % 60
                Text(
                    text = "Sleep in $minutes:${"%02d".format(seconds)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showSleepTimer) {
        SleepTimerDialog(
            onDismiss = { showSleepTimer = false },
            onSelect = { minutes ->
                viewModel.setSleepTimer(minutes)
                showSleepTimer = false
            }
        )
    }
}