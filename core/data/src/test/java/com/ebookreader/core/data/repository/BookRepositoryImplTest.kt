package com.ebookreader.core.data.repository

import com.ebookreader.core.data.db.dao.BookDao
import com.ebookreader.core.data.db.entity.BookEntity
import com.ebookreader.core.data.db.entity.BookFormat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class BookRepositoryImplTest {

    private lateinit var bookDao: BookDao
    private lateinit var repository: BookRepositoryImpl

    private val sampleBook = BookEntity(
        id = 1L,
        title = "Test Book",
        author = "Test Author",
        filePath = "/storage/books/test.epub",
        format = BookFormat.EPUB
    )

    @Before
    fun setUp() {
        bookDao = mockk()
        repository = BookRepositoryImpl(bookDao)
    }

    // -----------------------------------------------------------------------
    // getById
    // -----------------------------------------------------------------------

    @Test
    fun `getById returns book when it exists`() = runTest {
        coEvery { bookDao.getById(1L) } returns sampleBook

        val result = repository.getById(1L)

        assertEquals(sampleBook, result)
        coVerify(exactly = 1) { bookDao.getById(1L) }
    }

    @Test
    fun `getById returns null when book does not exist`() = runTest {
        coEvery { bookDao.getById(99L) } returns null

        val result = repository.getById(99L)

        assertNull(result)
        coVerify(exactly = 1) { bookDao.getById(99L) }
    }

    // -----------------------------------------------------------------------
    // insert
    // -----------------------------------------------------------------------

    @Test
    fun `insert delegates to dao and returns generated id`() = runTest {
        coEvery { bookDao.insert(sampleBook) } returns 42L

        val result = repository.insert(sampleBook)

        assertEquals(42L, result)
        coVerify(exactly = 1) { bookDao.insert(sampleBook) }
    }

    // -----------------------------------------------------------------------
    // updateProgress
    // -----------------------------------------------------------------------

    @Test
    fun `updateProgress delegates to dao with current timestamp`() = runTest {
        coEvery { bookDao.updateProgress(any(), any(), any(), any()) } returns Unit

        repository.updateProgress(id = 1L, progress = 0.5f, position = "chapter_3")

        coVerify(exactly = 1) {
            bookDao.updateProgress(
                id = 1L,
                progress = 0.5f,
                position = "chapter_3",
                timestamp = any()
            )
        }
    }

    // -----------------------------------------------------------------------
    // delete
    // -----------------------------------------------------------------------

    @Test
    fun `delete delegates to dao`() = runTest {
        coEvery { bookDao.delete(sampleBook) } returns Unit

        repository.delete(sampleBook)

        coVerify(exactly = 1) { bookDao.delete(sampleBook) }
    }
}
