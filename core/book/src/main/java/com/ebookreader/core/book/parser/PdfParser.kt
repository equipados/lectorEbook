package com.ebookreader.core.book.parser

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.ebookreader.core.book.model.Book
import com.ebookreader.core.book.model.BookContent
import com.ebookreader.core.book.model.Chapter
import com.ebookreader.core.book.model.TableOfContents
import com.ebookreader.core.book.model.TocEntry
import com.ebookreader.core.data.db.entity.BookFormat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class PdfParser @Inject constructor(
    @ApplicationContext private val context: Context
) : BookParser {

    override val supportedFormat: BookFormat = BookFormat.PDF

    override suspend fun parseMetadata(file: File): Book = withContext(Dispatchers.IO) {
        Book(
            title = file.nameWithoutExtension,
            author = "Unknown",
            filePath = file.absolutePath,
            format = BookFormat.PDF
        )
    }

    override suspend fun extractTextContent(file: File): BookContent = withContext(Dispatchers.IO) {
        val chapters = mutableListOf<Chapter>()
        try {
            val parcelFd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            parcelFd.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val pageCount = renderer.pageCount
                    for (i in 0 until pageCount) {
                        // Native PdfRenderer does not support text extraction;
                        // we create a placeholder chapter per page.
                        chapters.add(
                            Chapter(
                                index = i,
                                title = "Page ${i + 1}",
                                textContent = "[Page ${i + 1} of $pageCount — visual content only]"
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // Return empty content on any failure
        }
        BookContent(chapters)
    }

    override suspend fun getTableOfContents(file: File): TableOfContents = withContext(Dispatchers.IO) {
        val entries = mutableListOf<TocEntry>()
        try {
            val parcelFd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            parcelFd.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    for (i in 0 until renderer.pageCount) {
                        entries.add(
                            TocEntry(
                                title = "Page ${i + 1}",
                                href = "page://${i + 1}"
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            // Return empty TOC on failure
        }
        TableOfContents(entries)
    }

    override suspend fun extractCover(file: File, outputDir: File): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val parcelFd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            parcelFd.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (renderer.pageCount == 0) return@withContext null

                    renderer.openPage(0).use { page ->
                        val width = page.width.coerceAtLeast(1)
                        val height = page.height.coerceAtLeast(1)
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

                        // Fill white background before rendering
                        val canvas = Canvas(bitmap)
                        canvas.drawColor(Color.WHITE)

                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                        outputDir.mkdirs()
                        val coverFile = File(outputDir, "${file.nameWithoutExtension}_cover.jpg")
                        FileOutputStream(coverFile).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                        }
                        bitmap.recycle()
                        coverFile.absolutePath
                    }
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}
