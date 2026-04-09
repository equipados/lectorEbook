package com.ebookreader.core.data.repository

import com.ebookreader.core.data.db.dao.BookmarkDao
import com.ebookreader.core.data.db.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class BookmarkRepositoryImpl @Inject constructor(
    private val bookmarkDao: BookmarkDao
) : BookmarkRepository {

    override fun getBookmarksForBook(bookId: Long): Flow<List<BookmarkEntity>> =
        bookmarkDao.getBookmarksForBook(bookId)

    override suspend fun addBookmark(bookmark: BookmarkEntity): Long =
        bookmarkDao.insert(bookmark)

    override suspend fun removeBookmark(bookmark: BookmarkEntity) =
        bookmarkDao.delete(bookmark)
}
