package com.ebookreader.feature.reader.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ebookreader.core.book.model.TableOfContents
import com.ebookreader.core.book.model.TocEntry

@Composable
fun TableOfContentsDrawer(
    toc: TableOfContents,
    onEntryClick: (TocEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Column {
            Text(
                text = "Índice",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
            Divider()
            if (toc.entries.isEmpty()) {
                Text(
                    text = "Sin índice disponible",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn {
                    itemsIndexed(toc.entries) { _, entry ->
                        TocEntryItem(
                            entry = entry,
                            depth = 0,
                            onEntryClick = onEntryClick
                        )
                        Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun TocEntryItem(
    entry: TocEntry,
    depth: Int,
    onEntryClick: (TocEntry) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEntryClick(entry) }
            .padding(
                start = (16 + depth * 16).dp,
                end = 16.dp,
                top = 12.dp,
                bottom = 12.dp
            )
    ) {
        Text(
            text = entry.title,
            style = if (depth == 0) {
                MaterialTheme.typography.bodyLarge
            } else {
                MaterialTheme.typography.bodyMedium
            },
            color = if (depth == 0) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        if (entry.preview.isNotBlank()) {
            Text(
                text = entry.preview,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
    entry.children.forEach { child ->
        TocEntryItem(
            entry = child,
            depth = depth + 1,
            onEntryClick = onEntryClick
        )
    }
}
