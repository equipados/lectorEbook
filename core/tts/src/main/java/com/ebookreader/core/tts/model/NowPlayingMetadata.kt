package com.ebookreader.core.tts.model

data class NowPlayingMetadata(
    val bookTitle: String = "",
    val author: String = "",
    val chapterTitle: String = "",
    val coverPath: String? = null
)

/**
 * Construye la metadata de "ahora sonando" combinando los datos del libro con
 * el título del capítulo actual. Función pura para poder testearla sin el
 * framework de Android.
 */
fun buildNowPlaying(
    bookTitle: String,
    author: String,
    coverPath: String?,
    chapterTitles: List<String>,
    chapterIndex: Int
): NowPlayingMetadata = NowPlayingMetadata(
    bookTitle = bookTitle,
    author = author,
    chapterTitle = chapterTitles.getOrNull(chapterIndex).orEmpty(),
    coverPath = coverPath
)
