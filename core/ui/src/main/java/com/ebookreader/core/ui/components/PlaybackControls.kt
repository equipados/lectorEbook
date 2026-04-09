package com.ebookreader.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PlaybackControls(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onPreviousSentence: () -> Unit,
    onNextSentence: () -> Unit,
    onPreviousChapter: (() -> Unit)? = null,
    onNextChapter: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onPreviousChapter != null) {
            IconButton(onClick = onPreviousChapter) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "Previous chapter"
                )
            }
        }

        IconButton(onClick = onPreviousSentence) {
            Icon(
                imageVector = Icons.Filled.FastRewind,
                contentDescription = "Previous sentence"
            )
        }

        FilledIconButton(
            onClick = onPlayPause,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.size(32.dp)
            )
        }

        IconButton(onClick = onNextSentence) {
            Icon(
                imageVector = Icons.Filled.FastForward,
                contentDescription = "Next sentence"
            )
        }

        if (onNextChapter != null) {
            IconButton(onClick = onNextChapter) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Next chapter"
                )
            }
        }

        IconButton(onClick = onStop) {
            Icon(
                imageVector = Icons.Filled.Stop,
                contentDescription = "Stop"
            )
        }
    }
}
