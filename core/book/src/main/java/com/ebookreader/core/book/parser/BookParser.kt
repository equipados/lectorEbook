package com.ebookreader.core.book.parser

import com.ebookreader.core.book.model.Book
import com.ebookreader.core.book.model.BookContent
import com.ebookreader.core.book.model.TableOfContents
import com.ebookreader.core.data.db.entity.BookFormat
import java.io.File

interface BookParser {

    val supportedFormat: BookFormat

    suspend fun parseMetadata(file: File): Book

    suspend fun extractTextContent(file: File): BookContent

    suspend fun getTableOfContents(file: File): TableOfContents

    suspend fun extractCover(file: File, outputDir: File): String?
}
