package com.ebookreader.core.book.model

import com.ebookreader.core.data.db.entity.BookFormat

data class Book(
    val id: Long = 0,
    val title: String,
    val author: String,
    val coverPath: String? = null,
    val filePath: String,
    val format: BookFormat,
    val progress: Float = 0f,
    val lastPosition: String = ""
)
