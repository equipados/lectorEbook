package com.ebookreader.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class ReadingColors(
    val background: Color,
    val text: Color,
    val highlight: Color
)

val LightReadingColors = ReadingColors(
    background = Color.White,
    text = Color.Black,
    highlight = HighlightYellow
)

val DarkReadingColors = ReadingColors(
    background = Color(0xFF121212),
    text = Color(0xFFE0E0E0),
    highlight = HighlightYellow
)

val SepiaReadingColors = ReadingColors(
    background = Sepia,
    text = SepiaText,
    highlight = HighlightYellow
)
