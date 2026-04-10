package com.ebookreader.feature.library.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ebookreader.core.data.db.entity.BookEntity
import com.ebookreader.core.ui.components.BookCoverImage

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookCard(
    book: BookEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        )
    ) {
        Column {
            BookCoverImage(
                coverPath = book.coverPath,
                contentDescription = book.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.65f)
            )

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (book.progress > 0f) {
                    LinearProgressIndicator(
                        progress = { book.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
