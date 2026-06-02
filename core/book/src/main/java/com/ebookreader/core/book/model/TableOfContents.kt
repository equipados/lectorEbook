package com.ebookreader.core.book.model

data class TocEntry(
    val title: String,
    val href: String,
    val preview: String = "",
    val chapterIndex: Int = -1,
    val children: List<TocEntry> = emptyList()
)

data class TableOfContents(
    val entries: List<TocEntry>
)
