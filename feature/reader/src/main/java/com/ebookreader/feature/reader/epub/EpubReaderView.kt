package com.ebookreader.feature.reader.epub

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.ebookreader.core.data.preferences.ReadingPrefs
import com.ebookreader.core.data.preferences.ReadingThemeType
import com.ebookreader.core.tts.model.TextSegment

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EpubReaderView(
    filePath: String,
    lastPosition: String,
    readingPrefs: ReadingPrefs,
    currentTtsSegment: TextSegment?,
    onPageChanged: (Float) -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (readingPrefs.theme) {
        ReadingThemeType.DARK -> "#121212"
        ReadingThemeType.SEPIA -> "#F5E6CA"
        ReadingThemeType.LIGHT -> "#FFFFFF"
    }
    val textColor = when (readingPrefs.theme) {
        ReadingThemeType.DARK -> "#E0E0E0"
        ReadingThemeType.SEPIA -> "#5B4636"
        ReadingThemeType.LIGHT -> "#000000"
    }
    val fontSize = readingPrefs.fontSize

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.allowFileAccess = true
                webViewClient = WebViewClient()
                loadUrl("file://$filePath")
            }
        },
        update = { view ->
            val css = """
                javascript:(function(){
                    document.body.style.backgroundColor = '$backgroundColor';
                    document.body.style.color = '$textColor';
                    document.body.style.fontSize = '${fontSize}px';
                    document.body.style.lineHeight = '${readingPrefs.lineSpacing}';
                    document.body.style.padding = '16px';
                    document.body.style.wordWrap = 'break-word';
                })();
            """.trimIndent()
            view.evaluateJavascript(css, null)
        },
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) { onTap() }
    )
}
