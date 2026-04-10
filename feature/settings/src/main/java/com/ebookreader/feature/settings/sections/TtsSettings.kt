package com.ebookreader.feature.settings.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ebookreader.core.data.preferences.TtsEngineType
import com.ebookreader.core.data.preferences.TtsPrefs

private data class SpanishVoice(val id: String, val label: String)

private val spanishVoices = listOf(
    SpanishVoice("es-ES-Standard-A", "Español (España) — Estándar A (mujer)"),
    SpanishVoice("es-ES-Standard-B", "Español (España) — Estándar B (hombre)"),
    SpanishVoice("es-ES-Standard-C", "Español (España) — Estándar C (mujer)"),
    SpanishVoice("es-ES-Standard-D", "Español (España) — Estándar D (mujer)"),
    SpanishVoice("es-ES-Wavenet-B", "Español (España) — WaveNet B (hombre)"),
    SpanishVoice("es-ES-Wavenet-C", "Español (España) — WaveNet C (mujer)"),
    SpanishVoice("es-ES-Wavenet-D", "Español (España) — WaveNet D (mujer)"),
    SpanishVoice("es-ES-Neural2-A", "Español (España) — Neural2 A (mujer)"),
    SpanishVoice("es-ES-Neural2-B", "Español (España) — Neural2 B (hombre)"),
    SpanishVoice("es-ES-Neural2-C", "Español (España) — Neural2 C (mujer)"),
    SpanishVoice("es-ES-Neural2-D", "Español (España) — Neural2 D (mujer)"),
    SpanishVoice("es-ES-Neural2-E", "Español (España) — Neural2 E (mujer)"),
    SpanishVoice("es-ES-Neural2-F", "Español (España) — Neural2 F (hombre)"),
    SpanishVoice("es-US-Standard-A", "Español (Latinoamérica) — Estándar A (mujer)"),
    SpanishVoice("es-US-Standard-B", "Español (Latinoamérica) — Estándar B (hombre)"),
    SpanishVoice("es-US-Standard-C", "Español (Latinoamérica) — Estándar C (hombre)"),
    SpanishVoice("es-US-Wavenet-A", "Español (Latinoamérica) — WaveNet A (mujer)"),
    SpanishVoice("es-US-Wavenet-B", "Español (Latinoamérica) — WaveNet B (hombre)"),
    SpanishVoice("es-US-Wavenet-C", "Español (Latinoamérica) — WaveNet C (hombre)"),
    SpanishVoice("es-US-Neural2-A", "Español (Latinoamérica) — Neural2 A (mujer)"),
    SpanishVoice("es-US-Neural2-B", "Español (Latinoamérica) — Neural2 B (hombre)"),
    SpanishVoice("es-US-Neural2-C", "Español (Latinoamérica) — Neural2 C (hombre)")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TtsSettings(
    ttsPrefs: TtsPrefs,
    onSpeedChanged: (Float) -> Unit,
    onCloudApiKeyChanged: (String) -> Unit,
    onEngineChanged: (TtsEngineType) -> Unit = {},
    onCloudVoiceChanged: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Texto a voz",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Engine selector
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Motor de voz",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "La voz local no requiere internet; la nube ofrece voces más naturales pero consume datos y tu cuota de API.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = ttsPrefs.preferredEngine == TtsEngineType.LOCAL,
                    onClick = { onEngineChanged(TtsEngineType.LOCAL) },
                    label = { Text("Local") }
                )
                FilterChip(
                    selected = ttsPrefs.preferredEngine == TtsEngineType.CLOUD,
                    onClick = { onEngineChanged(TtsEngineType.CLOUD) },
                    label = { Text("Google Cloud") }
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        // Speed slider
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Velocidad predeterminada",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = String.format("%.1fx", ttsPrefs.speed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Slider(
                value = ttsPrefs.speed,
                onValueChange = { onSpeedChanged(it) },
                valueRange = 0.5f..3.0f,
                steps = 24,
                modifier = Modifier.fillMaxWidth()
            )
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        // Cloud API key
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Clave API de Google Cloud TTS",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Necesaria para el texto a voz en la nube",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = ttsPrefs.cloudApiKey,
                onValueChange = { onCloudApiKeyChanged(it) },
                label = { Text("Clave API") },
                placeholder = { Text("Introduce tu clave API") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Cloud voice selector
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Voz de Google Cloud",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Las voces WaveNet y Neural2 suenan más naturales pero cuestan más por carácter.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            var expanded by remember { mutableStateOf(false) }
            val currentLabel = spanishVoices
                .firstOrNull { it.id == ttsPrefs.cloudVoiceName }
                ?.label
                ?: ttsPrefs.cloudVoiceName

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = currentLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Voz") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    spanishVoices.forEach { voice ->
                        DropdownMenuItem(
                            text = { Text(voice.label) },
                            onClick = {
                                onCloudVoiceChanged(voice.id)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
