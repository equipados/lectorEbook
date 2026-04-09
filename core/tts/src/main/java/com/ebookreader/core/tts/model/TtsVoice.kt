package com.ebookreader.core.tts.model

data class TtsVoice(
    val id: String,
    val name: String,
    val language: String,
    val engineType: EngineType
)
