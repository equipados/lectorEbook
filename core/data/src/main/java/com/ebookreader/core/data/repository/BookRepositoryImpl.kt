package com.ebookreader.core.data.repository

import com.ebookreader.core.data.db.dao.BookDao
import com.ebookreader.core.data.db.entity.BookEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class BookRepositoryImpl @Inject constructor(
    private val bookDao: BookDao
) : BookRepository {

    override fun getAll(sortOrder: SortOrder): Flow<List<BookEntity>> = when (sortOrder) {
        SortOrder.RECENT -> bookDao.getAllByRecent()
        SortOrder.TITLE  -> bookDao.getAllByTitle()
        SortOrder.AUTHOR -> bookDao.getAllByAuthor()
    }

    override fun search(query: String): Flow<List<BookEntity>> =
        bookDao.search(query)

    override suspend fun getById(id: Long): BookEntity? =
        bookDao.getById(id)

    override suspend fun getByFilePath(path: String): BookEntity? =
        bookDao.getByFilePath(path)

    override suspend fun insert(book: BookEntity): Long =
        bookDao.insert(book)

    override suspend fun insertAll(books: List<BookEntity>): List<Long> =
        bookDao.insertAll(books)

    override suspend fun update(book: BookEntity) =
        bookDao.update(book)

    override suspend fun updateProgress(id: Long, progress: Float, position: String) =
        bookDao.updateProgress(id, progress, position, System.currentTimeMillis())

    override suspend fun delete(book: BookEntity) =
        bookDao.delete(book)
}
