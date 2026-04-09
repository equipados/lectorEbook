package com.ebookreader.core.book.model

data class Chapter(
    val index: Int,
    val title: String,
    val textContent: String
)

data class BookContent(
    val chapters: List<Chapter>
)
