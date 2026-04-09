package com.ebookreader.core.book.scanner

import java.io.File

interface BookScanner {
    suspend fun scanForBooks(directories: List<File> = emptyList()): List<File>
}
