package com.ebookreader.core.tts.cache

interface TtsCacheManager {

    suspend fun getCachedAudio(text: String, voiceId: String): String?

    suspend fun cacheAudio(text: String, voiceId: String, audioPath: String)

    suspend fun clearCache()
}
