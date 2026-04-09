package com.ebookreader.core.data.repository

import com.ebookreader.core.data.db.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow

interface BookmarkRepository {

    fun getBookmarksForBook(bookId: Long): Flow<List<BookmarkEntity>>

    suspend fun addBookmark(bookmark: BookmarkEntity): Long

    suspend fun removeBookmark(bookmark: BookmarkEntity)
}
