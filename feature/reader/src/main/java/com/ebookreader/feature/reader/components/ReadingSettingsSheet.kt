package com.ebookreader.feature.reader.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ebookreader.core.data.preferences.ReadingPrefs
import com.ebookreader.core.data.preferences.ReadingThemeType
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingSettingsSheet(
    readingPrefs: ReadingPrefs,
    ttsSpeed: Float,
    onDismiss: () -> Unit,
    onFontSizeChange: (Int) -> Unit,
    onThemeChange: (ReadingThemeType) -> Unit,
    onTtsSpeedChange: (Float) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Ajustes de lectura",
                style = MaterialTheme.typography.titleLarge
            )

            // Font size
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tamaño de fuente",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "${readingPrefs.fontSize}sp",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Slider(
                    value = readingPrefs.fontSize.toFloat(),
                    onValueChange = { onFontSizeChange(it.roundToInt()) },
                    valueRange = 12f..32f,
                    steps = 19,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Reading theme
            Column {
                Text(
                    text = "Tema de lectura",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReadingThemeType.entries.forEach { theme ->
                        FilterChip(
                            selected = readingPrefs.theme == theme,
                            onClick = { onThemeChange(theme) },
                            label = {
                                Text(
                                    text = theme.name.lowercase()
                                        .replaceFirstChar { it.uppercase() }
                                )
                            }
                        )
                    }
                }
            }

            // TTS speed
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Velocidad de voz",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "%.1fx".format(ttsSpeed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Slider(
                    value = ttsSpeed,
                    onValueChange = onTtsSpeedChange,
                    valueRange = 0.5f..3.0f,
                    steps = 24,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
