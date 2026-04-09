package com.ebookreader.core.book.model

data class TocEntry(
    val title: String,
    val href: String,
    val children: List<TocEntry> = emptyList()
)

data class TableOfContents(
    val entries: List<TocEntry>
)
