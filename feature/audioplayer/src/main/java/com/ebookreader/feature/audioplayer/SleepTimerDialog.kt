package com.ebookreader.feature.audioplayer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SleepTimerDialog(
    onDismiss: () -> Unit,
    onSelect: (Int?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep Timer") },
        text = {
            Column {
                listOf(15, 30, 60).forEach { minutes ->
                    Text(
                        text = "$minutes minutes",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(minutes) }
                            .padding(vertical = 12.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    HorizontalDivider()
                }

                Text(
                    text = "End of chapter",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(-1) }
                        .padding(vertical = 12.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = { onSelect(null) }) {
                Text("Cancel timer")
            }
        }
    )
}