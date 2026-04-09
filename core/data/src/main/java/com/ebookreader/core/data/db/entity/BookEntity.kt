package com.ebookreader.core.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val author: String,
    val coverPath: String? = null,
    val filePath: String,
    val format: BookFormat,
    val progress: Float = 0f,
    val lastPosition: String = "",
    val lastAccess: Long = System.currentTimeMillis(),
    val addedAt: Long = System.currentTimeMillis()
)
