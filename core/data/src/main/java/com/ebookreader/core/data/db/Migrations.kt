package com.ebookreader.core.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 -> v2: añade lastChapter, lastSegment, lastPage a books.
 * Inicializa lastChapter desde el lastPosition existente cuando es numérico.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE books ADD COLUMN lastChapter INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE books ADD COLUMN lastSegment INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE books ADD COLUMN lastPage INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE books SET lastChapter = CAST(lastPosition AS INTEGER) WHERE lastPosition GLOB '[0-9]*'")
    }
}
