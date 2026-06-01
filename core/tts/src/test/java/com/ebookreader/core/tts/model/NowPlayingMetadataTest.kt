package com.ebookreader.core.tts.model

import org.junit.Assert.assertEquals
import org.junit.Test

class NowPlayingMetadataTest {

    @Test
    fun usaElTituloDelCapituloActual() {
        val result = buildNowPlaying(
            bookTitle = "El Quijote",
            author = "Cervantes",
            coverPath = "/covers/quijote.png",
            chapterTitles = listOf("Prólogo", "Capítulo 1", "Capítulo 2"),
            chapterIndex = 1
        )

        assertEquals("El Quijote", result.bookTitle)
        assertEquals("Cervantes", result.author)
        assertEquals("Capítulo 1", result.chapterTitle)
        assertEquals("/covers/quijote.png", result.coverPath)
    }

    @Test
    fun devuelveChapterTitleVacioSiElIndiceEstaFueraDeRango() {
        val result = buildNowPlaying(
            bookTitle = "Libro",
            author = "Autor",
            coverPath = null,
            chapterTitles = listOf("Único"),
            chapterIndex = 5
        )

        assertEquals("", result.chapterTitle)
        assertEquals(null, result.coverPath)
    }
}
