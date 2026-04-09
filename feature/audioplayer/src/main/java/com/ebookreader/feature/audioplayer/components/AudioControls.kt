package com.ebookreader.feature.audioplayer.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ebookreader.core.ui.components.PlaybackControls

@Composable
fun AudioControls(
    isPlaying: Boolean,
    speed: Float,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onPreviousSentence: () -> Unit,
    onNextSentence: () -> Unit,
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PlaybackControls(
            isPlaying = isPlaying,
            onPlayPause = onPlayPause,
            onStop = onStop,
            onPreviousSentence = onPreviousSentence,
            onNextSentence = onNextSentence,
            onPreviousChapter = onPreviousChapter,
            onNextChapter = onNextChapter
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Velocidad",
                style = MaterialTheme.typography.labelSmall
            )
            Slider(
                value = speed,
                onValueChange = onSpeedChange,
                valueRange = 0.5f..3.0f,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )
            Text(
                text = "${"%.1f".format(speed)}x",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}