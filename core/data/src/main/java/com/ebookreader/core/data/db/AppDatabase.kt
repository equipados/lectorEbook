package com.ebookreader.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ebookreader.core.data.db.dao.BookDao
import com.ebookreader.core.data.db.dao.BookmarkDao
import com.ebookreader.core.data.db.entity.BookEntity
import com.ebookreader.core.data.db.entity.BookmarkEntity
import com.ebookreader.core.data.db.entity.TtsCacheEntity

@Database(
    entities = [BookEntity::class, BookmarkEntity::class, TtsCacheEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun bookmarkDao(): BookmarkDao
}
