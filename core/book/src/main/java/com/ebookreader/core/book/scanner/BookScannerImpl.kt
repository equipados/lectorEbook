package com.ebookreader.core.book.scanner

import android.content.Context
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class BookScannerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : BookScanner {

    private val supportedExtensions = setOf("epub", "pdf")

    override suspend fun scanForBooks(directories: List<File>): List<File> = withContext(Dispatchers.IO) {
        val dirsToScan = directories.ifEmpty { defaultDirectories() }

        dirsToScan
            .filter { it.exists() && it.isDirectory }
            .flatMap { dir ->
                dir.walkTopDown()
                    .filter { file ->
                        file.isFile && file.extension.lowercase() in supportedExtensions
                    }
                    .toList()
            }
            .distinctBy { it.absolutePath }
    }

    private fun defaultDirectories(): List<File> {
        val dirs = mutableListOf<File>()

        // Downloads folder
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (downloads != null) dirs.add(downloads)

        // Documents folder
        val documents = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        if (documents != null) dirs.add(documents)

        // External storage root
        val externalStorage = Environment.getExternalStorageDirectory()
        if (externalStorage != null) dirs.add(externalStorage)

        // App-specific external files dirs (no permission needed)
        context.getExternalFilesDirs(null).filterNotNull().forEach { dirs.add(it) }

        return dirs
    }
}
