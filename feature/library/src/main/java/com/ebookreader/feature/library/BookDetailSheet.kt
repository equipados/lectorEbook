package com.ebookreader.feature.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ebookreader.core.data.db.entity.BookEntity
import com.ebookreader.core.ui.components.BookCoverImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailSheet(
    book: BookEntity,
    onDismiss: () -> Unit,
    onRead: () -> Unit,
    onListen: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                BookCoverImage(
                    coverPath = book.coverPath,
                    contentDescription = book.title,
                    modifier = Modifier
                        .width(100.dp)
                        .height(150.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(book.title, style = MaterialTheme.typography.titleLarge)
                    Text(book.author, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "${(book.progress * 100).toInt()}% completado",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onRead,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.MenuBook, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Leer")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onListen,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Headphones, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Escuchar")
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Eliminar de la biblioteca")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
