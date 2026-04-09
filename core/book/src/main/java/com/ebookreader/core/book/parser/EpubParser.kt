package com.ebookreader.core.book.parser

import android.content.Context
import com.ebookreader.core.book.model.Book
import com.ebookreader.core.book.model.BookContent
import com.ebookreader.core.book.model.Chapter
import com.ebookreader.core.book.model.TableOfContents
import com.ebookreader.core.book.model.TocEntry
import com.ebookreader.core.data.db.entity.BookFormat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.FileExtension
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.asset.FileAsset
import org.readium.r2.streamer.Streamer
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class EpubParser @Inject constructor(
    @ApplicationContext private val context: Context,
    private val streamer: Streamer
) : BookParser {

    override val supportedFormat: BookFormat = BookFormat.EPUB

    override suspend fun parseMetadata(file: File): Book = withContext(Dispatchers.IO) {
        val publication = openPublication(file)
            ?: return@withContext Book(
                title = file.nameWithoutExtension,
                author = "Unknown",
                filePath = file.absolutePath,
                format = BookFormat.EPUB
            )

        val metadata = publication.metadata
        Book(
            title = metadata.title,
            author = metadata.authors.joinToString(", ") { it.name }.ifBlank { "Unknown" },
            filePath = file.absolutePath,
            format = BookFormat.EPUB
        )
    }

    override suspend fun extractTextContent(file: File): BookContent = withContext(Dispatchers.IO) {
        val publication = openPublication(file)
            ?: return@withContext BookContent(emptyList())

        val chapters = publication.readingOrder.mapIndexed { index, link ->
            val rawHtml = try {
                val resource = publication.get(link)
                when (val result = resource.readAsString()) {
                    is Try.Success -> result.value
                    is Try.Failure -> ""
                }
            } catch (e: Exception) {
                ""
            }
            val text = stripHtmlTags(rawHtml)
            Chapter(
                index = index,
                title = link.title ?: "Chapter ${index + 1}",
                textContent = text
            )
        }

        BookContent(chapters)
    }

    override suspend fun getTableOfContents(file: File): TableOfContents = withContext(Dispatchers.IO) {
        val publication = openPublication(file)
            ?: return@withContext TableOfContents(emptyList())

        val entries = publication.tableOfContents.map { link ->
            mapLinkToTocEntry(link)
        }
        TableOfContents(entries)
    }

    override suspend fun extractCover(file: File, outputDir: File): String? = withContext(Dispatchers.IO) {
        val publication = openPublication(file) ?: return@withContext null

        return@withContext try {
            val coverBitmap = publication.cover() ?: return@withContext null
            outputDir.mkdirs()
            val coverFile = File(outputDir, "${file.nameWithoutExtension}_cover.jpg")
            FileOutputStream(coverFile).use { out ->
                coverBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
            }
            coverBitmap.recycle()
            coverFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun openPublication(file: File): Publication? {
        return try {
            val asset = FileAsset(
                file = file.toPath(),
                mediaType = null
            )
            when (val result = streamer.open(asset, allowUserInteraction = false)) {
                is Try.Success -> result.value
                is Try.Failure -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun mapLinkToTocEntry(link: Link): TocEntry {
        return TocEntry(
            title = link.title ?: link.href.toString(),
            href = link.href.toString(),
            children = link.children.map { mapLinkToTocEntry(it) }
        )
    }

    private fun stripHtmlTags(html: String): String {
        return html
            .replace(Regex("<style[^>]*>[\\s\\S]*?</style>"), " ")
            .replace(Regex("<script[^>]*>[\\s\\S]*?</script>"), " ")
            .replace(Regex("<[^>]+>"), " ")
            .replace(Regex("&nbsp;"), " ")
            .replace(Regex("&amp;"), "&")
            .replace(Regex("&lt;"), "<")
            .replace(Regex("&gt;"), ">")
            .replace(Regex("&quot;"), "\"")
            .replace(Regex("&#39;"), "'")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
