package com.ebookreader.core.book.parser

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.ebookreader.core.book.model.Book
import com.ebookreader.core.book.model.BookContent
import com.ebookreader.core.book.model.Chapter
import com.ebookreader.core.book.model.TableOfContents
import com.ebookreader.core.book.model.TocEntry
import com.ebookreader.core.data.db.entity.BookFormat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.StringReader
import java.util.zip.ZipFile
import javax.inject.Inject

class EpubParser @Inject constructor(
    @ApplicationContext private val context: Context
) : BookParser {

    override val supportedFormat: BookFormat = BookFormat.EPUB

    // -------------------------------------------------------------------------
    // Public interface
    // -------------------------------------------------------------------------

    override suspend fun parseMetadata(file: File): Book = withContext(Dispatchers.IO) {
        val info = runCatching { extractEpubInfo(file) }.getOrNull()
        Book(
            title = info?.title?.takeIf { it.isNotBlank() } ?: file.nameWithoutExtension,
            author = info?.author?.takeIf { it.isNotBlank() } ?: "Unknown",
            filePath = file.absolutePath,
            format = BookFormat.EPUB
        )
    }

    override suspend fun extractTextContent(file: File): BookContent = withContext(Dispatchers.IO) {
        val info = runCatching { extractEpubInfo(file) }.getOrNull()
            ?: return@withContext BookContent(emptyList())

        val chapters = info.spineItems.mapIndexed { index, item ->
            val rawHtml = runCatching {
                ZipFile(file).use { zip ->
                    val entry = zip.getEntry(item.href) ?: return@runCatching ""
                    zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).readText()
                }
            }.getOrElse { "" }
            val text = stripHtmlTags(rawHtml)
            Chapter(
                index = index,
                title = item.title.ifBlank { "Chapter ${index + 1}" },
                textContent = text
            )
        }

        BookContent(chapters)
    }

    override suspend fun getTableOfContents(file: File): TableOfContents = withContext(Dispatchers.IO) {
        val info = runCatching { extractEpubInfo(file) }.getOrNull()
            ?: return@withContext TableOfContents(emptyList())

        val entries = info.tocEntries.ifEmpty {
            // Fall back to spine order when no NCX/Nav ToC is available
            info.spineItems.mapIndexed { i, item ->
                TocEntry(
                    title = item.title.ifBlank { "Chapter ${i + 1}" },
                    href = item.href
                )
            }
        }
        TableOfContents(entries)
    }

    override suspend fun extractCover(file: File, outputDir: File): String? = withContext(Dispatchers.IO) {
        val info = runCatching { extractEpubInfo(file) }.getOrNull() ?: return@withContext null
        val coverHref = info.coverHref ?: return@withContext null

        runCatching {
            ZipFile(file).use { zip ->
                val entry = zip.getEntry(coverHref) ?: return@withContext null
                val bitmap: Bitmap = zip.getInputStream(entry).use { stream ->
                    BitmapFactory.decodeStream(stream)
                } ?: return@withContext null

                outputDir.mkdirs()
                val coverFile = File(outputDir, "${file.nameWithoutExtension}_cover.jpg")
                FileOutputStream(coverFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                bitmap.recycle()
                coverFile.absolutePath
            }
        }.getOrNull()
    }

    // -------------------------------------------------------------------------
    // Internal data classes
    // -------------------------------------------------------------------------

    private data class EpubInfo(
        val title: String,
        val author: String,
        val spineItems: List<SpineItem>,
        val tocEntries: List<TocEntry>,
        val coverHref: String?
    )

    private data class SpineItem(
        val href: String,   // path relative to EPUB root (already resolved)
        val title: String
    )

    // -------------------------------------------------------------------------
    // Core parsing logic
    // -------------------------------------------------------------------------

    /**
     * Open the EPUB ZIP, locate the OPF manifest, parse spine + metadata,
     * then attempt to parse the NCX or Navigation Document for the ToC.
     */
    private fun extractEpubInfo(file: File): EpubInfo {
        ZipFile(file).use { zip ->
            // 1. Find OPF path via META-INF/container.xml
            val opfPath = findOpfPath(zip)

            // 2. Parse OPF
            val opfXml = readZipEntry(zip, opfPath)
            val opfDir = opfPath.substringBeforeLast('/', "")

            val metadata = parseOpfMetadata(opfXml)
            val manifest = parseOpfManifest(opfXml, opfDir)
            val spineIdrefs = parseOpfSpine(opfXml)

            // 3. Build ordered spine list
            val spineItems = spineIdrefs.mapNotNull { idref ->
                manifest[idref]?.let { href -> SpineItem(href = href, title = "") }
            }

            // 4. Find cover image
            val coverHref = findCoverHref(opfXml, manifest, zip)

            // 5. Parse ToC (NCX or EPUB3 nav)
            val tocEntries = parseToc(zip, opfXml, manifest, opfDir)

            // 6. Enrich spine items with titles from ToC
            val hrefToTitle = tocEntries.flattenToc().associate { it.href to it.title }
            val enrichedSpine = spineItems.map { item ->
                item.copy(title = hrefToTitle[item.href] ?: "")
            }

            return EpubInfo(
                title = metadata.title,
                author = metadata.author,
                spineItems = enrichedSpine,
                tocEntries = tocEntries,
                coverHref = coverHref
            )
        }
    }

    // -------------------------------------------------------------------------
    // container.xml -> OPF path
    // -------------------------------------------------------------------------

    private fun findOpfPath(zip: ZipFile): String {
        val containerXml = readZipEntry(zip, "META-INF/container.xml")
        // Quick regex approach – avoids a full XML parse for this tiny file
        val match = Regex("""full-path\s*=\s*["']([^"']+)["']""").find(containerXml)
        return match?.groupValues?.get(1)
            ?: throw IllegalStateException("Cannot locate OPF in container.xml")
    }

    // -------------------------------------------------------------------------
    // OPF parsing
    // -------------------------------------------------------------------------

    private data class OpfMetadata(val title: String, val author: String)

    private fun parseOpfMetadata(opfXml: String): OpfMetadata {
        var title = ""
        var author = ""
        try {
            val parser = newParser(opfXml)
            var eventType = parser.eventType
            var inTitle = false
            var inCreator = false
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val local = parser.name.substringAfterLast(':')
                        inTitle = local.equals("title", ignoreCase = true)
                        inCreator = local.equals("creator", ignoreCase = true)
                    }
                    XmlPullParser.TEXT -> {
                        if (inTitle && title.isBlank()) title = parser.text.trim()
                        if (inCreator && author.isBlank()) author = parser.text.trim()
                    }
                    XmlPullParser.END_TAG -> {
                        inTitle = false
                        inCreator = false
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {}
        return OpfMetadata(title, author)
    }

    /** Returns id -> resolved-href map for all manifest items. */
    private fun parseOpfManifest(opfXml: String, opfDir: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        try {
            val parser = newParser(opfXml)
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    val local = parser.name.substringAfterLast(':')
                    if (local.equals("item", ignoreCase = true)) {
                        val id = parser.getAttributeValue(null, "id") ?: ""
                        val href = parser.getAttributeValue(null, "href") ?: ""
                        if (id.isNotBlank() && href.isNotBlank()) {
                            map[id] = resolveHref(opfDir, href)
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {}
        return map
    }

    /** Returns ordered list of spine idrefs. */
    private fun parseOpfSpine(opfXml: String): List<String> {
        val list = mutableListOf<String>()
        try {
            val parser = newParser(opfXml)
            var inSpine = false
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val local = parser.name.substringAfterLast(':')
                        when {
                            local.equals("spine", ignoreCase = true) -> inSpine = true
                            inSpine && local.equals("itemref", ignoreCase = true) -> {
                                val idref = parser.getAttributeValue(null, "idref") ?: ""
                                if (idref.isNotBlank()) list.add(idref)
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name.substringAfterLast(':').equals("spine", ignoreCase = true)) {
                            inSpine = false
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {}
        return list
    }

    // -------------------------------------------------------------------------
    // Cover detection
    // -------------------------------------------------------------------------

    private val imageMediaTypes = setOf(
        "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    )

    private fun findCoverHref(opfXml: String, manifest: Map<String, String>, zip: ZipFile): String? {
        // Strategy 1: manifest item with id="cover-image" or properties="cover-image"
        try {
            val parser = newParser(opfXml)
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    val local = parser.name.substringAfterLast(':')
                    if (local.equals("item", ignoreCase = true)) {
                        val id = parser.getAttributeValue(null, "id") ?: ""
                        val props = parser.getAttributeValue(null, "properties") ?: ""
                        val mediaType = parser.getAttributeValue(null, "media-type") ?: ""
                        val href = parser.getAttributeValue(null, "href") ?: ""
                        if ((id.equals("cover-image", ignoreCase = true) ||
                                    props.contains("cover-image", ignoreCase = true)) &&
                            mediaType in imageMediaTypes && href.isNotBlank()
                        ) {
                            return manifest[id] ?: href
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {}

        // Strategy 2: meta name="cover" content="<id>"
        try {
            val match = Regex("""<meta[^>]+name\s*=\s*["']cover["'][^>]+content\s*=\s*["']([^"']+)["']""",
                RegexOption.IGNORE_CASE).find(opfXml)
                ?: Regex("""<meta[^>]+content\s*=\s*["']([^"']+)["'][^>]+name\s*=\s*["']cover["']""",
                    RegexOption.IGNORE_CASE).find(opfXml)
            if (match != null) {
                val coverId = match.groupValues[1]
                val href = manifest[coverId]
                if (href != null && zip.getEntry(href) != null) return href
            }
        } catch (_: Exception) {}

        return null
    }

    // -------------------------------------------------------------------------
    // ToC parsing (NCX + EPUB3 nav)
    // -------------------------------------------------------------------------

    private fun parseToc(
        zip: ZipFile,
        opfXml: String,
        manifest: Map<String, String>,
        opfDir: String
    ): List<TocEntry> {
        // Try EPUB3 navigation document first
        val navHref = findNavHref(opfXml, manifest)
        if (navHref != null) {
            val navXml = runCatching { readZipEntry(zip, navHref) }.getOrNull()
            if (navXml != null) {
                val navDir = navHref.substringBeforeLast('/', "")
                val entries = parseNavDocument(navXml, navDir)
                if (entries.isNotEmpty()) return entries
            }
        }

        // Fall back to NCX
        val ncxHref = findNcxHref(opfXml, manifest)
        if (ncxHref != null) {
            val ncxXml = runCatching { readZipEntry(zip, ncxHref) }.getOrNull()
            if (ncxXml != null) {
                val ncxDir = ncxHref.substringBeforeLast('/', "")
                val entries = parseNcx(ncxXml, ncxDir)
                if (entries.isNotEmpty()) return entries
            }
        }

        return emptyList()
    }

    private fun findNavHref(opfXml: String, manifest: Map<String, String>): String? {
        try {
            val parser = newParser(opfXml)
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    val local = parser.name.substringAfterLast(':')
                    if (local.equals("item", ignoreCase = true)) {
                        val props = parser.getAttributeValue(null, "properties") ?: ""
                        val id = parser.getAttributeValue(null, "id") ?: ""
                        if (props.contains("nav", ignoreCase = true)) {
                            return manifest[id]
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {}
        return null
    }

    private fun findNcxHref(opfXml: String, manifest: Map<String, String>): String? {
        try {
            val parser = newParser(opfXml)
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    val local = parser.name.substringAfterLast(':')
                    if (local.equals("item", ignoreCase = true)) {
                        val mediaType = parser.getAttributeValue(null, "media-type") ?: ""
                        val id = parser.getAttributeValue(null, "id") ?: ""
                        if (mediaType.equals("application/x-dtbncx+xml", ignoreCase = true)) {
                            return manifest[id]
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {}
        return null
    }

    /** Parse EPUB3 nav document – look for <nav epub:type="toc"> / <ol> / <li> / <a>. */
    private fun parseNavDocument(navXml: String, navDir: String): List<TocEntry> {
        val entries = mutableListOf<TocEntry>()
        try {
            val parser = newParser(navXml)
            var inTocNav = false
            var depth = 0
            val stack = ArrayDeque<MutableList<TocEntry>>()
            stack.addLast(entries)
            var pendingHref = ""
            var pendingTitle = ""
            var inAnchor = false

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                val localName = parser.name?.substringAfterLast(':') ?: ""
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when {
                            localName.equals("nav", ignoreCase = true) -> {
                                val epubType = parser.getAttributeValue(null, "epub:type")
                                    ?: parser.getAttributeValue(
                                        "http://www.idpf.org/2007/ops", "type"
                                    ) ?: ""
                                if (epubType.contains("toc", ignoreCase = true)) {
                                    inTocNav = true
                                    depth = 0
                                }
                            }
                            inTocNav && localName.equals("ol", ignoreCase = true) -> {
                                depth++
                                if (depth > 1) stack.addLast(mutableListOf())
                            }
                            inTocNav && localName.equals("a", ignoreCase = true) -> {
                                pendingHref = resolveHref(navDir,
                                    parser.getAttributeValue(null, "href") ?: "")
                                pendingTitle = ""
                                inAnchor = true
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inAnchor) pendingTitle += parser.text
                    }
                    XmlPullParser.END_TAG -> {
                        when {
                            localName.equals("nav", ignoreCase = true) && inTocNav -> {
                                inTocNav = false
                            }
                            inTocNav && localName.equals("a", ignoreCase = true) -> {
                                inAnchor = false
                            }
                            inTocNav && localName.equals("li", ignoreCase = true) -> {
                                if (pendingHref.isNotBlank() || pendingTitle.isNotBlank()) {
                                    val children: List<TocEntry> =
                                        if (stack.size > 1) stack.last().toList() else emptyList()
                                    val entry = TocEntry(
                                        title = pendingTitle.trim().ifBlank { pendingHref },
                                        href = pendingHref,
                                        children = children
                                    )
                                    if (stack.size > 1) stack.last().clear()
                                    stack[stack.size - 2].add(entry)
                                    pendingHref = ""
                                    pendingTitle = ""
                                }
                            }
                            inTocNav && localName.equals("ol", ignoreCase = true) -> {
                                depth--
                                if (stack.size > 1) stack.removeLast()
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {}
        return entries
    }

    /** Parse EPUB2 NCX file. */
    private fun parseNcx(ncxXml: String, ncxDir: String): List<TocEntry> {
        val root = mutableListOf<TocEntry>()
        try {
            val parser = newParser(ncxXml)
            val stack = ArrayDeque<MutableList<TocEntry>>()
            stack.addLast(root)
            var pendingHref = ""
            var pendingTitle = ""
            var inLabel = false

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                val local = parser.name?.substringAfterLast(':') ?: ""
                when (eventType) {
                    XmlPullParser.START_TAG -> when {
                        local.equals("navPoint", ignoreCase = true) -> {
                            stack.addLast(mutableListOf())
                            pendingHref = ""
                            pendingTitle = ""
                        }
                        local.equals("navLabel", ignoreCase = true) -> inLabel = true
                        local.equals("content", ignoreCase = true) -> {
                            pendingHref = resolveHref(
                                ncxDir,
                                parser.getAttributeValue(null, "src") ?: ""
                            )
                        }
                        local.equals("text", ignoreCase = true) && inLabel -> { /* text follows */ }
                    }
                    XmlPullParser.TEXT -> {
                        if (inLabel && pendingTitle.isBlank()) pendingTitle = parser.text.trim()
                    }
                    XmlPullParser.END_TAG -> when {
                        local.equals("navLabel", ignoreCase = true) -> inLabel = false
                        local.equals("navPoint", ignoreCase = true) -> {
                            val children = stack.removeLast().toList()
                            val entry = TocEntry(
                                title = pendingTitle.ifBlank { pendingHref },
                                href = pendingHref,
                                children = children
                            )
                            stack.last().add(entry)
                            pendingHref = ""
                            pendingTitle = ""
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {}
        return root
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun readZipEntry(zip: ZipFile, path: String): String {
        val entry = zip.getEntry(path)
            ?: throw IllegalArgumentException("Entry not found in ZIP: $path")
        return zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).readText()
    }

    private fun resolveHref(baseDir: String, href: String): String {
        if (href.startsWith("http://") || href.startsWith("https://")) return href
        return if (baseDir.isBlank()) href else "$baseDir/$href"
    }

    private fun newParser(xml: String): XmlPullParser {
        val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }
        return factory.newPullParser().apply {
            setInput(StringReader(xml))
        }
    }

    private fun List<TocEntry>.flattenToc(): List<TocEntry> {
        val result = mutableListOf<TocEntry>()
        fun visit(entry: TocEntry) {
            result.add(entry)
            entry.children.forEach { visit(it) }
        }
        forEach { visit(it) }
        return result
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
