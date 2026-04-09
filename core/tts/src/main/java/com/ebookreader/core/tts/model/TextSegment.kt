package com.ebookreader.core.tts.model

data class TextSegment(
    val text: String,
    val startOffset: Int,
    val endOffset: Int,
    val chapterIndex: Int
)
