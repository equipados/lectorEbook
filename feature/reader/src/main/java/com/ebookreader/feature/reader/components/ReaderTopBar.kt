package com.ebookreader.feature.reader.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderTopBar(
    title: String,
    isBookmarked: Boolean,
    onBack: () -> Unit,
    onOpenToc: () -> Unit,
    onToggleBookmark: () -> Unit,
    onSwitchToAudio: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Atrás"
                )
            }
        },
        actions = {
            IconButton(onClick = onOpenToc) {
                Icon(
                    imageVector = Icons.Filled.List,
                    contentDescription = "Tabla de contenidos"
                )
            }
            IconButton(onClick = onToggleBookmark) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                    contentDescription = if (isBookmarked) "Quitar marcador" else "Añadir marcador"
                )
            }
            IconButton(onClick = onSwitchToAudio) {
                Icon(
                    imageVector = Icons.Filled.Headphones,
                    contentDescription = "Cambiar a modo audio"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        )
    )
}
