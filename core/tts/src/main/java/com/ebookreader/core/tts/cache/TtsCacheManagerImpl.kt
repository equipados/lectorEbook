package com.ebookreader.core.tts.cache

import androidx.sqlite.db.SimpleSQLiteQuery
import com.ebookreader.core.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TtsCacheManagerImpl @Inject constructor(
    private val database: AppDatabase
) : TtsCacheManager {

    override suspend fun getCachedAudio(text: String, voiceId: String): String? = withContext(Dispatchers.IO) {
        val query = SimpleSQLiteQuery(
            "SELECT audioPath FROM tts_cache WHERE textHash = ? AND voiceId = ? ORDER BY createdAt DESC LIMIT 1",
            arrayOf(hashText(text), voiceId)
        )

        database.openHelper.readableDatabase.query(query).use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }

    override suspend fun cacheAudio(text: String, voiceId: String, audioPath: String) = withContext(Dispatchers.IO) {
        val hash = hashText(text)
        val writableDb = database.openHelper.writableDatabase

        writableDb.execSQL(
            "DELETE FROM tts_cache WHERE textHash = ? AND voiceId = ?",
            arrayOf(hash, voiceId)
        )
        writableDb.execSQL(
            "INSERT INTO tts_cache(textHash, voiceId, audioPath, createdAt) VALUES (?, ?, ?, ?)",
            arrayOf(hash, voiceId, audioPath, System.currentTimeMillis())
        )
    }

    override suspend fun clearCache() = withContext(Dispatchers.IO) {
        database.openHelper.writableDatabase.execSQL("DELETE FROM tts_cache")
    }

    private fun hashText(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
        return digest.joinToString(separator = "") { "%02x".format(it) }
    }
}
