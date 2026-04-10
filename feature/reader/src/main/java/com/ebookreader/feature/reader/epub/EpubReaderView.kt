package com.ebookreader.feature.reader.epub

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.ebookreader.core.data.preferences.ReadingPrefs
import com.ebookreader.core.data.preferences.ReadingThemeType
import com.ebookreader.core.tts.model.TextSegment

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EpubReaderView(
    chapterFilePath: String,
    readingPrefs: ReadingPrefs,
    currentTtsSegment: TextSegment?,
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit,
    onTap: () -> Unit,
    onFontLarger: () -> Unit = {},
    onFontSmaller: () -> Unit = {},
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
    val lineSpacing = readingPrefs.lineSpacing

    // --- Script de ESTILO + definición idempotente de helpers de paginación.
    //     NO resetea __currentPage ni hace scrollTo(0). Se puede reejecutar
    //     con seguridad en cada recomposición del AndroidView.
    val styleScript = remember(backgroundColor, textColor, fontSize, lineSpacing) {
        """
        javascript:(function(){
            if (!document.body) return;
            var padX = 20;
            var padY = 24;
            var colW = window.innerWidth - 2 * padX;
            var colGap = 2 * padX;

            // Inyecta un <style> con !important para sobreescribir cualquier CSS
            // del propio EPUB (muchos libros tienen font-size explícito en px).
            var styleId = '__ebook_override_style';
            var styleEl = document.getElementById(styleId);
            if (!styleEl) {
                styleEl = document.createElement('style');
                styleEl.id = styleId;
                (document.head || document.documentElement).appendChild(styleEl);
            }
            styleEl.textContent =
                'html { margin:0 !important; padding:0 !important; height:100% !important; overflow-x:auto !important; overflow-y:hidden !important; -webkit-overflow-scrolling:touch !important; }' +
                'html::-webkit-scrollbar, body::-webkit-scrollbar { display:none !important; width:0 !important; height:0 !important; }' +
                'html, body { background-color: $backgroundColor !important; color: $textColor !important; font-family: serif !important; scrollbar-width: none !important; }' +
                // FUERZA las columnas CSS — algunos EPUBs traen CSS propio que
                // sobrescribe las propiedades column-* asignadas inline.
                'body {' +
                '  margin:0 !important;' +
                '  padding:' + padY + 'px ' + padX + 'px !important;' +
                '  box-sizing:border-box !important;' +
                '  height:' + window.innerHeight + 'px !important;' +
                '  max-height:' + window.innerHeight + 'px !important;' +
                '  min-height:' + window.innerHeight + 'px !important;' +
                '  width:auto !important;' +
                '  max-width:none !important;' +
                '  column-width:' + colW + 'px !important;' +
                '  -webkit-column-width:' + colW + 'px !important;' +
                '  column-gap:' + colGap + 'px !important;' +
                '  -webkit-column-gap:' + colGap + 'px !important;' +
                '  column-fill:auto !important;' +
                '  -webkit-column-fill:auto !important;' +
                '  column-count:auto !important;' +
                '  -webkit-column-count:auto !important;' +
                '  word-wrap:break-word !important;' +
                '  overflow-wrap:break-word !important;' +
                '}' +
                'body, body * { font-size: ${fontSize}px !important; line-height: $lineSpacing !important; }' +
                'h1, h1 * { font-size: ' + (${fontSize} + 8) + 'px !important; }' +
                'h2, h2 * { font-size: ' + (${fontSize} + 6) + 'px !important; }' +
                'h3, h3 * { font-size: ' + (${fontSize} + 4) + 'px !important; }' +
                'h4, h4 * { font-size: ' + (${fontSize} + 2) + 'px !important; }' +
                // Elementos hijos: evitar que algo fuerce ancho fijo mayor
                // que la columna (columnas no pueden partir esos nodos).
                'body > * { max-width:' + colW + 'px !important; }' +
                'p, div, span, li, blockquote { max-width:' + colW + 'px !important; box-sizing:border-box !important; }' +
                'img { max-width: ' + colW + 'px !important; max-height: ' + (window.innerHeight - 2 * padY) + 'px !important; height: auto !important; object-fit: contain !important; page-break-inside: avoid !important; break-inside: avoid !important; }' +
                'table, pre, code, svg { max-width: ' + colW + 'px !important; overflow: hidden !important; break-inside: avoid !important; word-wrap:break-word !important; }';

            var b = document.body;
            b.style.margin = '0';
            b.style.padding = padY + 'px ' + padX + 'px';
            b.style.boxSizing = 'border-box';
            b.style.height = window.innerHeight + 'px';
            b.style.maxHeight = window.innerHeight + 'px';
            b.style.width = 'auto';
            b.style.columnWidth = colW + 'px';
            b.style.webkitColumnWidth = colW + 'px';
            b.style.columnGap = colGap + 'px';
            b.style.webkitColumnGap = colGap + 'px';
            b.style.columnFill = 'auto';
            b.style.webkitColumnFill = 'auto';
            b.style.wordWrap = 'break-word';
            b.style.overflowWrap = 'break-word';

            window.__pageWidth = window.innerWidth;

            // Definir helpers sólo una vez (o refrescar si faltan).
            window.__recalc = function() {
                var sw = document.body.scrollWidth;
                window.__totalPages = Math.max(1, Math.round(sw / window.innerWidth));
            };
            window.__nextPage = function() {
                window.__recalc();
                if (typeof window.__currentPage !== 'number') window.__currentPage = 0;
                if (window.__currentPage + 1 >= window.__totalPages) return false;
                window.__currentPage += 1;
                window.scrollTo(window.__currentPage * window.innerWidth, 0);
                return true;
            };
            window.__prevPage = function() {
                window.__recalc();
                if (typeof window.__currentPage !== 'number') window.__currentPage = 0;
                if (window.__currentPage <= 0) return false;
                window.__currentPage -= 1;
                window.scrollTo(window.__currentPage * window.innerWidth, 0);
                return true;
            };
            window.__goToLastPage = function() {
                window.__recalc();
                window.__currentPage = window.__totalPages - 1;
                window.scrollTo(window.__currentPage * window.innerWidth, 0);
            };

            // Si venimos de una recomposición y ya teníamos página,
            // reajustamos el scroll al nuevo layout. Si no había página,
            // no tocamos (onPageFinished se encarga del reset inicial).
            window.__recalc();
            if (typeof window.__currentPage === 'number') {
                var target = Math.min(window.__currentPage, window.__totalPages - 1) * window.innerWidth;
                window.scrollTo(target, 0);
            }
        })();
        """.trimIndent()
    }

    // --- Script de RESET — sólo se ejecuta al cargar un nuevo capítulo.
    val resetScript = remember {
        """
        javascript:(function(){
            window.__currentPage = 0;
            window.scrollTo(0, 0);
        })();
        """.trimIndent()
    }

    // Holder mutable para que el WebViewClient (capturado en factory) siempre
    // lea la versión más reciente del script al cambiar tema / fuente.
    val styleHolder = remember { mutableStateOf("") }
    styleHolder.value = styleScript

    // Lambdas actualizadas — evita stale closures en pointerInput.
    val latestOnPreviousChapter by rememberUpdatedState(onPreviousChapter)
    val latestOnNextChapter by rememberUpdatedState(onNextChapter)
    val latestOnTap by rememberUpdatedState(onTap)
    val latestOnFontLarger by rememberUpdatedState(onFontLarger)
    val latestOnFontSmaller by rememberUpdatedState(onFontSmaller)

    val context = LocalContext.current
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
    fun buzz() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(30)
            }
        } catch (_: Exception) {}
    }

    // Bandera: si el capítulo se cargó por "ir al capítulo anterior", mostramos
    // la última página automáticamente al terminar de cargar.
    val goToLastOnLoad = remember { mutableStateOf(false) }
    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    // Acumulador de zoom para el pinch (umbral evita cambios por ruido).
    val zoomAccum = remember { mutableFloatStateOf(1f) }

    // Aplica los estilos inmediatamente cuando cambia cualquier preferencia
    // visible (tamaño, tema, interlineado). No depende del update del
    // AndroidView, que puede no dispararse al instante.
    LaunchedEffect(styleScript) {
        webViewRef.value?.evaluateJavascript(styleScript, null)
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                WebView.setWebContentsDebuggingEnabled(true)
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.builtInZoomControls = false
                    settings.displayZoomControls = false
                    settings.loadWithOverviewMode = false
                    settings.useWideViewPort = false
                    settings.allowFileAccess = true
                    @Suppress("DEPRECATION")
                    settings.allowFileAccessFromFileURLs = true
                    @Suppress("DEPRECATION")
                    settings.allowUniversalAccessFromFileURLs = true
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                    overScrollMode = WebView.OVER_SCROLL_NEVER
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(cm: ConsoleMessage?): Boolean {
                            Log.d("EpubReaderJS", "${cm?.messageLevel()} ${cm?.message()} (${cm?.sourceId()}:${cm?.lineNumber()})")
                            return true
                        }
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            // 1. Aplica estilos + define helpers
                            view?.evaluateJavascript(styleHolder.value) {
                                // 2. Reset: nuevo capítulo → página 0, scroll 0
                                view.evaluateJavascript(resetScript) {
                                    // 3. Si venimos de "capítulo anterior", ir a la última página
                                    if (goToLastOnLoad.value) {
                                        goToLastOnLoad.value = false
                                        view.evaluateJavascript(
                                            "javascript:(function(){ if (window.__goToLastPage) window.__goToLastPage(); })();",
                                            null
                                        )
                                    }
                                }
                            }
                        }
                    }
                    webViewRef.value = this
                }
            },
            update = { view ->
                val currentTag = view.tag as? String
                if (currentTag != chapterFilePath) {
                    view.tag = chapterFilePath
                    view.loadUrl("file://$chapterFilePath")
                } else {
                    // Mismo capítulo → sólo reaplica estilos (NO resetea paginación).
                    view.evaluateJavascript(styleHolder.value, null)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay transparente sobre el WebView para capturar los taps.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    // Pinch-to-zoom → ajusta el tamaño de fuente.
                    // Acumulamos el zoom y sólo disparamos cuando supera
                    // un umbral (evita cambios por micro-movimientos).
                    detectTransformGestures(panZoomLock = true) { _, _, zoom, _ ->
                        zoomAccum.floatValue *= zoom
                        when {
                            zoomAccum.floatValue >= 1.15f -> {
                                latestOnFontLarger()
                                zoomAccum.floatValue = 1f
                            }
                            zoomAccum.floatValue <= 0.87f -> {
                                latestOnFontSmaller()
                                zoomAccum.floatValue = 1f
                            }
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        Log.d("EpubReader", "tap x=${offset.x} width=${size.width}")
                        val width = size.width
                        val wv = webViewRef.value
                        when {
                            offset.x < width / 3f -> {
                                Log.d("EpubReader", "PREV zone")
                                buzz()
                                if (wv == null) {
                                    latestOnPreviousChapter()
                                } else {
                                    wv.evaluateJavascript(
                                        "(function(){ var r = window.__prevPage ? window.__prevPage() : 'nohelper'; console.log('prevPage result=' + r + ' cur=' + window.__currentPage + ' tot=' + window.__totalPages + ' sw=' + document.body.scrollWidth + ' iw=' + window.innerWidth); return r; })();"
                                    ) { result ->
                                        Log.d("EpubReader", "prevPage js result=$result")
                                        if (result == "false" || result == "null" || result == "\"nohelper\"") {
                                            goToLastOnLoad.value = true
                                            latestOnPreviousChapter()
                                        }
                                    }
                                }
                            }
                            offset.x > width * 2f / 3f -> {
                                Log.d("EpubReader", "NEXT zone")
                                buzz()
                                if (wv == null) {
                                    latestOnNextChapter()
                                } else {
                                    wv.evaluateJavascript(
                                        "(function(){ var r = window.__nextPage ? window.__nextPage() : 'nohelper'; console.log('nextPage result=' + r + ' cur=' + window.__currentPage + ' tot=' + window.__totalPages + ' sw=' + document.body.scrollWidth + ' iw=' + window.innerWidth); return r; })();"
                                    ) { result ->
                                        Log.d("EpubReader", "nextPage js result=$result")
                                        if (result == "false" || result == "null" || result == "\"nohelper\"") {
                                            latestOnNextChapter()
                                        }
                                    }
                                }
                            }
                            else -> latestOnTap()
                        }
                    }
                }
        )
    }
}
