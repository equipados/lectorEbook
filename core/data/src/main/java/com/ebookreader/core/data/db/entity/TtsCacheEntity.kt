package com.ebookreader.core.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tts_cache")
data class TtsCacheEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val textHash: String,
    val voiceId: String,
    val audioPath: String,
    val createdAt: Long = System.currentTimeMillis()
)
