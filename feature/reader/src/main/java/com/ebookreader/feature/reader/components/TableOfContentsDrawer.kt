package com.ebookreader.feature.reader.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ebookreader.core.book.model.TableOfContents
import com.ebookreader.core.book.model.TocEntry

@Composable
fun TableOfContentsDrawer(
    toc: TableOfContents,
    onEntryClick: (TocEntry, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Column {
            Text(
                text = "Table of Contents",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
            Divider()
            if (toc.entries.isEmpty()) {
                Text(
                    text = "No table of contents available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn {
                    itemsIndexed(toc.entries) { index, entry ->
                        TocEntryItem(
                            entry = entry,
                            index = index,
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
    index: Int,
    depth: Int,
    onEntryClick: (TocEntry, Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEntryClick(entry, index) }
            .padding(
                start = (16 + depth * 16).dp,
                end = 16.dp,
                top = 12.dp,
                bottom = 12.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (depth > 0) {
            Spacer(modifier = Modifier.width(8.dp))
        }
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
    }
    entry.children.forEachIndexed { childIndex, child ->
        TocEntryItem(
            entry = child,
            index = childIndex,
            depth = depth + 1,
            onEntryClick = onEntryClick
        )
    }
}
