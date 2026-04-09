package com.ebookreader.feature.reader.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private data class PdfPage(val index: Int, val bitmap: Bitmap)

@Composable
fun PdfReaderView(
    filePath: String,
    onPageChanged: (Float) -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pages by remember { mutableStateOf<List<PdfPage>>(emptyList()) }
    var pageCount by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()

    DisposableEffect(filePath) {
        val job = scope.launch {
            isLoading = true
            val renderedPages = withContext(Dispatchers.IO) {
                val file = File(filePath)
                if (!file.exists()) return@withContext emptyList()

                val result = mutableListOf<PdfPage>()
                try {
                    val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    pfd.use { descriptor ->
                        PdfRenderer(descriptor).use { renderer ->
                            val count = renderer.pageCount
                            for (i in 0 until count) {
                                renderer.openPage(i).use { page ->
                                    val width = page.width.coerceAtLeast(1)
                                    val height = page.height.coerceAtLeast(1)
                                    val bitmap = Bitmap.createBitmap(
                                        width, height, Bitmap.Config.ARGB_8888
                                    )
                                    val canvas = Canvas(bitmap)
                                    canvas.drawColor(Color.WHITE)
                                    page.render(
                                        bitmap, null, null,
                                        PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                                    )
                                    result.add(PdfPage(i, bitmap))
                                }
                            }
                        }
                    }
                } catch (_: Exception) {
                }
                result
            }
            pages = renderedPages
            pageCount = renderedPages.size
            isLoading = false
        }
        onDispose {
            job.cancel()
            pages.forEach { it.bitmap.recycle() }
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }.collect { index ->
            if (pageCount > 0) {
                onPageChanged(index.toFloat() / pageCount.toFloat())
            }
        }
    }

    Box(modifier = modifier) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTap() }
            ) {
                items(pages.size) { index ->
                    Image(
                        bitmap = pages[index].bitmap.asImageBitmap(),
                        contentDescription = "Page ${index + 1}",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}
