package com.ebookreader.feature.library.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ebookreader.core.data.db.entity.BookEntity

@Composable
fun BookGrid(
    books: List<BookEntity>,
    onBookClick: (BookEntity) -> Unit,
    onBookLongClick: (BookEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 140.dp),
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(books, key = { it.id }) { book ->
            BookCard(
                book = book,
                onClick = { onBookClick(book) },
                onLongClick = { onBookLongClick(book) }
            )
        }
    }
}
