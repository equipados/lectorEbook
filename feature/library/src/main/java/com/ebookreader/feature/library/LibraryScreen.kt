package com.ebookreader.feature.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ebookreader.core.data.db.entity.BookEntity
import com.ebookreader.core.data.repository.SortOrder
import com.ebookreader.feature.library.components.BookGrid
import com.ebookreader.feature.library.components.BookList
import com.ebookreader.feature.library.components.LibrarySearchBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onBookClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val books by viewModel.books.collectAsState()
    var bookToDelete by remember { mutableStateOf<BookEntity?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importBook(it) }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    filePickerLauncher.launch(
                        arrayOf(
                            "application/epub+zip",
                            "application/pdf",
                            "application/octet-stream"
                        )
                    )
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir libro")
            }
        },
        topBar = {
            TopAppBar(
                title = { Text("Mi Biblioteca") },
                actions = {
                    IconButton(onClick = { viewModel.toggleViewMode() }) {
                        Icon(
                            imageVector = if (uiState.viewMode == LibraryViewMode.GRID) {
                                Icons.Default.ViewList
                            } else {
                                Icons.Default.GridView
                            },
                            contentDescription = "Cambiar vista"
                        )
                    }
                    IconButton(onClick = { viewModel.scanForBooks() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Escanear")
                    }
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.SortByAlpha, contentDescription = "Ordenar")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Reciente") },
                                onClick = {
                                    viewModel.setSortOrder(SortOrder.RECENT)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Título") },
                                onClick = {
                                    viewModel.setSortOrder(SortOrder.TITLE)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Autor") },
                                onClick = {
                                    viewModel.setSortOrder(SortOrder.AUTHOR)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Ajustes")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LibrarySearchBar(
                query = uiState.searchQuery,
                onQueryChange = viewModel::setSearchQuery,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (uiState.isScanning) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }

            if (books.isEmpty() && !uiState.isScanning) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(96.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Tu biblioteca está vacía",
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Añade libros EPUB o PDF para empezar a leer",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                filePickerLauncher.launch(
                                    arrayOf(
                                        "application/epub+zip",
                                        "application/pdf",
                                        "application/octet-stream"
                                    )
                                )
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Añadir libro")
                        }
                    }
                }
            } else {
                when (uiState.viewMode) {
                    LibraryViewMode.GRID -> BookGrid(
                        books = books,
                        onBookClick = { book -> onBookClick(book.id) },
                        onBookLongClick = { book -> bookToDelete = book }
                    )
                    LibraryViewMode.LIST -> BookList(
                        books = books,
                        onBookClick = { book -> onBookClick(book.id) },
                        onBookLongClick = { book -> bookToDelete = book }
                    )
                }
            }
        }
    }

    // Diálogo de confirmación de borrado (aparece con long-press)
    bookToDelete?.let { book ->
        AlertDialog(
            onDismissRequest = { bookToDelete = null },
            title = { Text("Eliminar libro") },
            text = {
                Text("¿Seguro que quieres eliminar \"${book.title}\" de tu biblioteca?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteBook(book)
                        bookToDelete = null
                    }
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { bookToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
