package com.ebookreader.core.tts.model

data class TtsState(
    val isPlaying: Boolean = false,
    val currentSegmentIndex: Int = 0,
    val currentChapterIndex: Int = 0,
    val speed: Float = 1.0f,
    val activeVoice: TtsVoice? = null,
    val engineType: EngineType = EngineType.LOCAL
)

enum class EngineType {
    LOCAL,
    CLOUD
}
