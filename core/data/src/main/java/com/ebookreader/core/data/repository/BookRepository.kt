package com.ebookreader.core.data.repository

import com.ebookreader.core.data.db.entity.BookEntity
import kotlinx.coroutines.flow.Flow

enum class SortOrder {
    RECENT,
    TITLE,
    AUTHOR
}

interface BookRepository {

    fun getAll(sortOrder: SortOrder = SortOrder.RECENT): Flow<List<BookEntity>>

    fun search(query: String): Flow<List<BookEntity>>

    suspend fun getById(id: Long): BookEntity?

    suspend fun getByFilePath(path: String): BookEntity?

    suspend fun insert(book: BookEntity): Long

    suspend fun insertAll(books: List<BookEntity>): List<Long>

    suspend fun update(book: BookEntity)

    suspend fun updateProgress(id: Long, progress: Float, position: String)

    suspend fun delete(book: BookEntity)
}
