package com.ebookreader.feature.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@Composable
fun LibrarySearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = shape
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = shape
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Buscar",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(
            modifier = Modifier
                .weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                interactionSource = interactionSource,
                modifier = Modifier.fillMaxWidth()
            )
            if (query.isEmpty()) {
                Text(
                    text = "Buscar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (query.isNotEmpty()) {
            IconButton(
                onClick = { onQueryChange("") },
                modifier = Modifier.height(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Limpiar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
