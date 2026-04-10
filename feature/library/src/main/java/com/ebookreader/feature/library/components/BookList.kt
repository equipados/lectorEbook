package com.ebookreader.feature.library.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ebookreader.core.data.db.entity.BookEntity
import com.ebookreader.core.ui.components.BookCoverImage

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookList(
    books: List<BookEntity>,
    onBookClick: (BookEntity) -> Unit,
    onBookLongClick: (BookEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(books, key = { it.id }) { book ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { onBookClick(book) },
                        onLongClick = { onBookLongClick(book) }
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BookCoverImage(
                    coverPath = book.coverPath,
                    contentDescription = book.title,
                    modifier = Modifier
                        .width(48.dp)
                        .height(72.dp)
                )
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = book.author,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (book.progress > 0f) {
                        Box(modifier = Modifier.padding(top = 4.dp)) {
                            LinearProgressIndicator(
                                progress = { book.progress },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}
