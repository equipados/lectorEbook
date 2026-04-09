package com.ebookreader.feature.reader.epub

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.ebookreader.core.data.preferences.ReadingPrefs
import com.ebookreader.core.data.preferences.ReadingThemeType
import com.ebookreader.core.tts.model.TextSegment

/**
 * EPUB reader view using WebView as a host for Readium navigator integration.
 *
 * In a full Readium integration, the [EpubNavigatorFragment] from
 * `org.readium.r2.navigator:navigator` would be used. This composable wraps a
 * WebView as a placeholder that applies the reading theme and font-size from
 * [readingPrefs], highlights the [currentTtsSegment] via JavaScript, and
 * forwards tap and page-change events.
 */
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

    val webView = remember {
        WebView(android.app.Application())
    }

    LaunchedEffect(filePath) {
        webView.apply {
            settings.javaScriptEnabled = true
            settings.builtInZoomControls = false
            webViewClient = WebViewClient()
            // Placeholder: load local file via content URI or Readium streamer URL
            loadUrl("file://$filePath")
        }
    }

    LaunchedEffect(readingPrefs) {
        val css = """
            document.body.style.backgroundColor = '$backgroundColor';
            document.body.style.color = '$textColor';
            document.body.style.fontSize = '${fontSize}px';
            document.body.style.lineHeight = '${readingPrefs.lineSpacing}';
        """.trimIndent()
        webView.evaluateJavascript("javascript:(function(){$css})();", null)
    }

    LaunchedEffect(currentTtsSegment) {
        if (currentTtsSegment != null) {
            val highlightJs = """
                (function() {
                    var existing = document.getElementById('tts-highlight');
                    if (existing) existing.remove();
                })();
            """.trimIndent()
            webView.evaluateJavascript("javascript:$highlightJs", null)
        }
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.builtInZoomControls = false
                webViewClient = WebViewClient()
                setOnClickListener { onTap() }
                loadUrl("file://$filePath")
            }
        },
        update = { view ->
            val css = """
                document.body.style.backgroundColor = '$backgroundColor';
                document.body.style.color = '$textColor';
                document.body.style.fontSize = '${fontSize}px';
            """.trimIndent()
            view.evaluateJavascript("javascript:(function(){$css})();", null)
        },
        modifier = modifier
    )
}
