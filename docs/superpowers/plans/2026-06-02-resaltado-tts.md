# Resaltado sincronizado durante TTS — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Resaltar en el visor la frase que el TTS está leyendo, avanzando de página y de capítulo automáticamente para "seguir al narrador".

**Architecture:** El `TtsController` (singleton) expone `currentSegment`. `ReaderScreen` ya lo observa y lo pasa a `EpubReaderView`. Se añade (1) sincronización unidireccional TTS→visor del capítulo visible en el `ReaderViewModel`/`ReaderScreen`, y (2) resaltado por JavaScript en el WebView usando la CSS Custom Highlight API (`Range` + `::highlight`), que no altera el DOM ni rompe la paginación por columnas. El auto-avance de página se calcula desde el `getBoundingClientRect()` del `Range`.

**Tech Stack:** Kotlin, Jetpack Compose, Android WebView, JavaScript (CSS Custom Highlight API), `org.json.JSONObject` para escapar texto.

---

## File Structure

- `feature/reader/.../ReaderViewModel.kt` — método `syncVisibleChapter(index)` (TTS→visor, sin reordenar el TTS).
- `feature/reader/.../ReaderScreen.kt` — `LaunchedEffect` de sincronización de capítulo; pasar `visibleChapterIndex` a `EpubReaderView`.
- `feature/reader/.../epub/EpubReaderView.kt` — funciones JS de resaltado + CSS en el `styleScript`, parámetro `visibleChapterIndex`, `LaunchedEffect(currentTtsSegment)`, re-resaltado en `onPageFinished`.

Sin tests unitarios: el módulo `feature:reader` no tiene infraestructura de test y la lógica es UI/WebView/JS. Verificación manual en dispositivo (Task 3).

---

## Task 1: Sincronización del capítulo visible (TTS → visor)

Cuando el TTS cruza al siguiente capítulo, el visor debe cambiar de capítulo solo,
SIN reordenar el TTS (la sincronización es unidireccional). El salto manual del
usuario sigue usando `jumpToChapter`/`nextChapter`/`previousChapter` (que sí
reordenan el TTS) sin cambios.

**Files:**
- Modify: `feature/reader/src/main/java/com/ebookreader/feature/reader/ReaderViewModel.kt`
- Modify: `feature/reader/src/main/java/com/ebookreader/feature/reader/ReaderScreen.kt`

- [ ] **Step 1: Añadir `syncVisibleChapter` al ViewModel**

En `ReaderViewModel.kt`, junto a `jumpToChapter` (≈L214) y `jumpToTocEntry`, añadir:

```kotlin
    /**
     * Sincroniza el capítulo VISIBLE con el que está leyendo el TTS, sin tocar
     * el propio TTS (evita bucles). Se usa para "seguir al narrador" cuando la
     * reproducción cruza al siguiente capítulo.
     */
    fun syncVisibleChapter(index: Int) {
        val state = _uiState.value
        val total = state.chapterFiles.size
        if (total == 0) return
        val safe = index.coerceIn(0, total - 1)
        if (safe == state.currentChapterIndex) return
        _uiState.update { it.copy(currentChapterIndex = safe) }
        persistChapterPosition(safe, total)
    }
```

- [ ] **Step 2: Añadir el `LaunchedEffect` de sincronización en `ReaderScreen`**

En `ReaderScreen.kt`, tras las declaraciones de estado (después de la línea
`val bookmarks by viewModel.bookmarks.collectAsState()` ≈L50) y antes de
`val drawerState`, añadir:

```kotlin
    // Seguir al narrador: cuando el TTS reproduce y su capítulo difiere del
    // visible, cambiar el capítulo visible (sin reordenar el TTS).
    LaunchedEffect(currentSegment?.chapterIndex, ttsState.isPlaying) {
        val seg = currentSegment
        if (ttsState.isPlaying && seg != null &&
            seg.chapterIndex != uiState.currentChapterIndex
        ) {
            viewModel.syncVisibleChapter(seg.chapterIndex)
        }
    }
```

Verificar que `androidx.compose.runtime.LaunchedEffect` ya está importado (lo está,
se usa en ≈L56). No añadir imports nuevos.

- [ ] **Step 3: Compilar**

Run: `.\gradlew.bat :feature:reader:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add feature/reader/src/main/java/com/ebookreader/feature/reader/ReaderViewModel.kt feature/reader/src/main/java/com/ebookreader/feature/reader/ReaderScreen.kt
git commit -m "feat: sincronizar el capitulo visible con el TTS (seguir al narrador)"
```

---

## Task 2: Resaltado en el WebView (CSS Custom Highlight API)

**Files:**
- Modify: `feature/reader/src/main/java/com/ebookreader/feature/reader/epub/EpubReaderView.kt`
- Modify: `feature/reader/src/main/java/com/ebookreader/feature/reader/ReaderScreen.kt` (call-site)

- [ ] **Step 1: Añadir el parámetro `visibleChapterIndex` a `EpubReaderView`**

En la firma de `EpubReaderView` (≈L35-45), añadir el parámetro tras
`currentTtsSegment`:

```kotlin
fun EpubReaderView(
    chapterFilePath: String,
    readingPrefs: ReadingPrefs,
    currentTtsSegment: TextSegment?,
    visibleChapterIndex: Int,
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit,
    onTap: () -> Unit,
    onFontLarger: () -> Unit = {},
    onFontSmaller: () -> Unit = {},
    modifier: Modifier = Modifier
) {
```

- [ ] **Step 2: Añadir el CSS de resaltado al `styleEl.textContent`**

Dentro del `styleScript`, en la cadena `styleEl.textContent` (≈L80-116), añadir una
regla más al final (justo antes del `;` de cierre de la última línea
`'table, pre, code, svg { ... }'`). Cambiar esa última línea para que termine en
`+` y añadir la regla de highlight:

```javascript
                'table, pre, code, svg { max-width: ' + colW + 'px !important; overflow: hidden !important; break-inside: avoid !important; word-wrap:break-word !important; }' +
                '::highlight(tts) { background-color: rgba(255, 213, 79, 0.45); }';
```

- [ ] **Step 3: Definir las funciones JS de resaltado dentro del `styleScript`**

En el `styleScript`, justo ANTES de la línea `window.__recalc();` del final
(≈L166, la que recalcula tras recomposición), añadir las definiciones de las
funciones de resaltado (son idempotentes; se redefinen sin problema):

```javascript
            // --- Resaltado de la frase leída por el TTS (CSS Custom Highlight API).
            // No modifica el DOM (no rompe la paginación por columnas).
            window.__clearHighlight = function() {
                if (window.CSS && CSS.highlights) CSS.highlights.delete('tts');
            };
            window.__highlightSentence = function(text) {
                if (!window.CSS || !CSS.highlights || typeof Highlight === 'undefined') return;
                CSS.highlights.delete('tts');
                if (!text) return;
                // Normaliza 1:1 por carácter (comillas/guiones) y colapsa espacios,
                // registrando para cada carácter emitido su nodo y offset original.
                var collapseChar = function(c) {
                    if (c === '‘' || c === '’') return "'";
                    if (c === '“' || c === '”') return '"';
                    if (c === '–' || c === '—') return '-';
                    if (/\s/.test(c)) return ' ';
                    return c;
                };
                var target = '';
                for (var i = 0; i < text.length; i++) {
                    var tc = collapseChar(text.charAt(i));
                    if (tc === ' ' && target.charAt(target.length - 1) === ' ') continue;
                    target += tc;
                }
                target = target.trim();
                if (!target) return;

                var walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, null);
                var full = '';
                var map = []; // map[k] = {node, offset} del k-ésimo carácter de 'full'
                var node;
                while (node = walker.nextNode()) {
                    var val = node.nodeValue;
                    for (var j = 0; j < val.length; j++) {
                        var nc = collapseChar(val.charAt(j));
                        if (nc === ' ' && full.charAt(full.length - 1) === ' ') continue;
                        full += nc;
                        map.push({ node: node, offset: j });
                    }
                }
                var idx = full.indexOf(target);
                if (idx < 0) return;
                var endIdx = idx + target.length;
                if (endIdx > map.length) return;

                var range = document.createRange();
                range.setStart(map[idx].node, map[idx].offset);
                var lastEntry = map[endIdx - 1];
                range.setEnd(lastEntry.node, lastEntry.offset + 1);

                try {
                    CSS.highlights.set('tts', new Highlight(range));
                } catch (e) { return; }

                // Auto-avance: lleva la página de la frase a la vista.
                var rect = range.getBoundingClientRect();
                var pageW = window.innerWidth;
                if (pageW > 0 && rect.width >= 0) {
                    var absX = rect.left + (window.scrollX || 0);
                    var targetPage = Math.floor(absX / pageW);
                    if (typeof window.__totalPages === 'number') {
                        targetPage = Math.max(0, Math.min(targetPage, window.__totalPages - 1));
                    } else {
                        targetPage = Math.max(0, targetPage);
                    }
                    if (targetPage !== window.__currentPage) {
                        window.__currentPage = targetPage;
                        window.scrollTo(targetPage * pageW, 0);
                    }
                }
            };
```

- [ ] **Step 4: Holders para el segmento y el capítulo visible**

Tras `styleHolder.value = styleScript` (≈L188), añadir holders que el
`WebViewClient` (capturado en la factory) pueda leer en `onPageFinished`:

```kotlin
    val segmentHolder = remember { mutableStateOf<TextSegment?>(null) }
    segmentHolder.value = currentTtsSegment
    val visibleChapterHolder = remember { mutableStateOf(visibleChapterIndex) }
    visibleChapterHolder.value = visibleChapterIndex
```

- [ ] **Step 5: `LaunchedEffect` que resalta al cambiar de segmento**

Tras el `LaunchedEffect(styleScript) { ... }` existente (≈L228-230), añadir:

```kotlin
    // Resalta la frase del TTS cuando cambia el segmento (solo si pertenece al
    // capítulo visible). Si no hay segmento aplicable, limpia el resaltado.
    LaunchedEffect(currentTtsSegment, visibleChapterIndex) {
        val wv = webViewRef.value ?: return@LaunchedEffect
        val seg = currentTtsSegment
        if (seg != null && seg.chapterIndex == visibleChapterIndex) {
            val json = org.json.JSONObject.quote(seg.text)
            wv.evaluateJavascript(
                "javascript:(function(){ if (window.__highlightSentence) window.__highlightSentence($json); })();",
                null
            )
        } else {
            wv.evaluateJavascript(
                "javascript:(function(){ if (window.__clearHighlight) window.__clearHighlight(); })();",
                null
            )
        }
    }
```

- [ ] **Step 6: Re-resaltar tras cargar el capítulo (`onPageFinished`)**

En `onPageFinished`, dentro del callback de `resetScript` (≈L262-271), después del
bloque `if (goToLastOnLoad.value) { ... }`, añadir el re-resaltado del segmento
actual si pertenece al capítulo recién cargado:

```kotlin
                                    val seg = segmentHolder.value
                                    if (seg != null && seg.chapterIndex == visibleChapterHolder.value) {
                                        val json = org.json.JSONObject.quote(seg.text)
                                        view.evaluateJavascript(
                                            "javascript:(function(){ if (window.__highlightSentence) window.__highlightSentence($json); })();",
                                            null
                                        )
                                    }
```

- [ ] **Step 7: Pasar `visibleChapterIndex` desde `ReaderScreen`**

En `ReaderScreen.kt`, en la llamada a `EpubReaderView` (≈L154-164), añadir el
argumento tras `currentTtsSegment`:

```kotlin
                            EpubReaderView(
                                chapterFilePath = chapterPath,
                                readingPrefs = readingPrefs,
                                currentTtsSegment = currentSegment,
                                visibleChapterIndex = uiState.currentChapterIndex,
                                onPreviousChapter = viewModel::previousChapter,
                                onNextChapter = viewModel::nextChapter,
                                onTap = viewModel::toggleControls,
                                onFontLarger = viewModel::increaseFontSize,
                                onFontSmaller = viewModel::decreaseFontSize,
                                modifier = Modifier.fillMaxSize()
                            )
```

- [ ] **Step 8: Compilar la app entera**

Run: `.\gradlew.bat :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 9: Commit**

```bash
git add feature/reader/src/main/java/com/ebookreader/feature/reader/epub/EpubReaderView.kt feature/reader/src/main/java/com/ebookreader/feature/reader/ReaderScreen.kt
git commit -m "feat: resaltar la frase leida por el TTS con auto-avance de pagina"
```

---

## Task 3: Verificación en dispositivo

- [ ] **Step 1: Instalar**

Run: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
Expected: Success

- [ ] **Step 2: Validar manualmente**

- Abrir un libro, pulsar play (TTS) desde el visor → la frase que suena aparece
  resaltada en amarillo translúcido.
- Al terminar las frases visibles de una página, el visor **avanza solo** a la
  página de la frase siguiente.
- Al cruzar al siguiente capítulo, el visor **cambia de capítulo solo** y sigue
  resaltando.
- Pausar el TTS → el resaltado desaparece. Reanudar → vuelve a resaltar.
- Pasar de página/capítulo manualmente (taps laterales) sigue funcionando.
- Cambiar tamaño de fuente/tema durante la reproducción no rompe el resaltado
  (se reajusta en el siguiente segmento).

- [ ] **Step 3: Si una frase concreta no se resalta**

Capturar logcat filtrando por `EpubReaderJS` (errores de consola del WebView) y por
`EpubReader`. La degradación esperada es: si la frase no casa, no se resalta pero
el TTS sigue. Solo investigar si NINGUNA frase se resalta (indicaría que
`CSS.highlights` o la inyección JS fallan).

---

## Self-Review (completado durante la escritura)

- **Cobertura del spec:** resaltado por búsqueda en DOM con CSS Highlight API
  (Task 2, Step 3), CSS `::highlight` (Task 2, Step 2), auto-avance de página
  (Task 2, Step 3, bloque "Auto-avance"), seguir al narrador entre capítulos
  (Task 1), limpieza al pausar/cambiar (Task 2, Step 5 rama `else`), re-resaltado
  al cargar capítulo (Task 2, Step 6), normalización tolerante (Task 2, Step 3
  `collapseChar`), degradación silenciosa si no casa (`if (idx < 0) return`). ✔
- **Sin placeholders:** todo el JS y Kotlin está completo. ✔
- **Consistencia de tipos:** `visibleChapterIndex: Int` añadido a la firma y pasado
  desde `ReaderScreen`; `syncVisibleChapter(Int)`; holders `segmentHolder`
  (`TextSegment?`) y `visibleChapterHolder` (`Int`); funciones JS
  `__highlightSentence(text)` / `__clearHighlight()` referenciadas igual en los
  3 sitios (LaunchedEffect, onPageFinished). ✔
- **Anti-bucle:** `syncVisibleChapter` NO llama al `ttsController` (a diferencia de
  `jumpToChapter`), evitando realimentación TTS↔visor. ✔
