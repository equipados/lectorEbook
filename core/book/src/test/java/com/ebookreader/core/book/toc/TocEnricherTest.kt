package com.ebookreader.core.book.toc

import com.ebookreader.core.book.model.BookContent
import com.ebookreader.core.book.model.Chapter
import com.ebookreader.core.book.model.TableOfContents
import com.ebookreader.core.book.model.TocEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class TocEnricherTest {

    private fun content(vararg ch: Chapter) = BookContent(ch.toList())

    @Test
    fun `resuelve chapterIndex por href ignorando fragmento`() {
        val toc = TableOfContents(listOf(TocEntry(title = "1", href = "OPS/c1.xhtml#top")))
        val book = content(Chapter(index = 5, href = "OPS/c1.xhtml", title = "1", textContent = "Hola mundo"))

        val result = TocEnricher.enrich(toc, book)

        assertEquals(5, result.entries[0].chapterIndex)
    }

    @Test
    fun `genera preview con las primeras palabras saltando el numero inicial`() {
        val toc = TableOfContents(listOf(TocEntry(title = "1", href = "c1.xhtml")))
        val book = content(Chapter(index = 0, href = "c1.xhtml", title = "1", textContent = "1 1 REG. 18/10/2018 transcripción de audio del detective"))

        val result = TocEnricher.enrich(toc, book)

        assertEquals("REG. 18/10/2018 transcripción de audio del detective", result.entries[0].preview)
    }

    @Test
    fun `corta el preview en limite de palabra sin pasar de 90 caracteres`() {
        val long = (1..40).joinToString(" ") { "palabra$it" }
        val toc = TableOfContents(listOf(TocEntry(title = "1", href = "c1.xhtml")))
        val book = content(Chapter(index = 0, href = "c1.xhtml", title = "1", textContent = long))

        val result = TocEnricher.enrich(toc, book)
        val preview = result.entries[0].preview

        assert(preview.length <= 91) { "preview demasiado largo: ${preview.length}" }
        assert(preview.endsWith("…")) { "debería terminar en elipsis: $preview" }
        assert(!preview.contains("palabr ")) { "no debe cortar a media palabra" }
    }

    @Test
    fun `prefija Capitulo a titulos puramente numericos`() {
        val toc = TableOfContents(listOf(TocEntry(title = "  3 ", href = "c.xhtml")))
        val book = content(Chapter(index = 0, href = "c.xhtml", title = "3", textContent = "texto"))

        val result = TocEnricher.enrich(toc, book)

        assertEquals("Capítulo 3", result.entries[0].title)
    }

    @Test
    fun `no prefija titulos no numericos`() {
        val toc = TableOfContents(listOf(TocEntry(title = "ENTONCES", href = "c.xhtml")))
        val book = content(Chapter(index = 0, href = "c.xhtml", title = "", textContent = "texto"))

        val result = TocEnricher.enrich(toc, book)

        assertEquals("ENTONCES", result.entries[0].title)
    }

    @Test
    fun `entrada sin capitulo correspondiente queda con chapterIndex -1 y sin preview`() {
        val toc = TableOfContents(listOf(TocEntry(title = "Huérfana", href = "noexiste.xhtml")))
        val book = content(Chapter(index = 0, href = "c.xhtml", title = "x", textContent = "texto"))

        val result = TocEnricher.enrich(toc, book)

        assertEquals(-1, result.entries[0].chapterIndex)
        assertEquals("", result.entries[0].preview)
    }

    @Test
    fun `enriquece recursivamente los children`() {
        val toc = TableOfContents(listOf(
            TocEntry(title = "ENTONCES", href = "part.xhtml", children = listOf(
                TocEntry(title = "1", href = "c1.xhtml")
            ))
        ))
        val book = content(
            Chapter(index = 0, href = "part.xhtml", title = "ENTONCES", textContent = "ENTONCES"),
            Chapter(index = 1, href = "c1.xhtml", title = "1", textContent = "1 Texto del capítulo uno")
        )

        val result = TocEnricher.enrich(toc, book)
        val child = result.entries[0].children[0]

        assertEquals(1, child.chapterIndex)
        assertEquals("Capítulo 1", child.title)
        assertEquals("Texto del capítulo uno", child.preview)
    }
}
