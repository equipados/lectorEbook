package com.ebookreader.core.book.toc

import com.ebookreader.core.book.model.BookContent
import com.ebookreader.core.book.model.Chapter
import com.ebookreader.core.book.model.TableOfContents
import com.ebookreader.core.book.model.TocEntry

/**
 * Enriquece un [TableOfContents] crudo (estructura del NCX/Nav) con datos
 * derivados del [BookContent]: índice de capítulo resuelto por href, preview
 * del contenido y título de visualización ("Capítulo N" para entradas numéricas).
 * Función pura, sin dependencias de Android — testeable en JVM.
 */
object TocEnricher {

    private const val PREVIEW_MAX = 90

    fun enrich(toc: TableOfContents, content: BookContent): TableOfContents {
        val byHref: Map<String, Chapter> = content.chapters.associateBy { it.href }
        return TableOfContents(toc.entries.map { enrichEntry(it, byHref) })
    }

    private fun enrichEntry(entry: TocEntry, byHref: Map<String, Chapter>): TocEntry {
        val key = entry.href.substringBefore('#')
        val chapter = byHref[key]
        return entry.copy(
            title = displayTitle(entry.title),
            preview = chapter?.let { buildPreview(it.textContent) } ?: "",
            chapterIndex = chapter?.index ?: -1,
            children = entry.children.map { enrichEntry(it, byHref) }
        )
    }

    /** Prefija "Capítulo N" cuando el título es solo un número. */
    private fun displayTitle(raw: String): String {
        val t = raw.trim()
        return if (t.isNotEmpty() && t.all { it.isDigit() }) "Capítulo $t" else t
    }

    /**
     * Primeras palabras del capítulo, saltando los números/espacios iniciales
     * (que suelen ser el propio número de capítulo repetido), cortando en límite
     * de palabra y añadiendo elipsis si se trunca.
     */
    private fun buildPreview(textContent: String): String {
        // Colapsa espacios y elimina el prefijo numérico inicial ("1 1 REG..." -> "REG...").
        val collapsed = textContent.trim().replace(Regex("\\s+"), " ")
        val body = collapsed.replace(Regex("^(\\d+\\s+)+"), "")
        if (body.length <= PREVIEW_MAX) return body
        val cut = body.substring(0, PREVIEW_MAX)
        val lastSpace = cut.lastIndexOf(' ')
        val trimmed = if (lastSpace > 0) cut.substring(0, lastSpace) else cut
        return "$trimmed…"
    }
}
