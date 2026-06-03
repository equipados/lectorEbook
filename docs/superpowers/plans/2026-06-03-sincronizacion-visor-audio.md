# Sincronización visor ↔ audio — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Que el visor acompañe al narrador: resaltar la frase que suena, avanzar de página y de capítulo siguiendo al TTS, y que al pulsar play tras navegar a mano la lectura arranque en la página visible.

**Architecture:** Al cargar un capítulo se envuelve cada frase en un `<span class="__seg">` (inline, no rompe la paginación). La página de una frase = `floor((span.left + __currentPage*innerWidth) / innerWidth)` con el `getBoundingClientRect` del elemento (validado en spike). El emparejado TTS↔span es por texto normalizado. El `TtsController` aporta la frase actual y gana `jumpToSentenceByText`.

**Tech Stack:** Kotlin, Jetpack Compose, Android WebView + JavaScript, `org.json`.

---

## File Structure

- `feature/reader/.../epub/EpubReaderView.kt` — envoltura JS + helpers, resaltado/auto-avance, notificación de frase visible.
- `feature/reader/.../ReaderScreen.kt` — pasar params nuevos + `LaunchedEffect` de sync de capítulo.
- `feature/reader/.../ReaderViewModel.kt` — `syncVisibleChapter`, frase visible pendiente, `playPauseTts` desde página.
- `core/tts/.../controller/TtsController.kt` + `TtsControllerImpl.kt` — `jumpToSentenceByText`.

Sin tests unitarios nuevos salvo el matching: `feature/reader` no tiene infra de test y el grueso es JS/WebView. Verificación manual por hitos (Tareas 1-3 incluyen validación en dispositivo).

---

## Task 1: Envolver frases en spans + helpers JS

**Files:**
- Modify: `feature/reader/src/main/java/com/ebookreader/feature/reader/epub/EpubReaderView.kt`

- [ ] **Step 1: Añadir el CSS de resaltado al `styleScript`**

En la cadena `styleEl.textContent`, cambiar la última regla (la de `table, pre, code, svg`) para que termine en `+` y añadir la regla del span activo:

```javascript
                'table, pre, code, svg { max-width: ' + colW + 'px !important; overflow: hidden !important; break-inside: avoid !important; word-wrap:break-word !important; }' +
                'span.__seg.__active { background-color: rgba(255, 213, 79, 0.45); }';
```

- [ ] **Step 2: Definir el script de envoltura + helpers**

Tras el bloque `val resetScript = remember { ... }`, añadir:

```kotlin
    // Envuelve cada frase del capítulo en un <span class="__seg"> (inline, no
    // rompe la paginación) y define helpers para mapear frase<->página y resaltar.
    val segScript = remember {
        """
        javascript:(function(){
            if (!document.body) return;
            if (!window.__segWrapped) {
                var walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, null);
                var nodes = [], n;
                while (n = walker.nextNode()) { if (n.nodeValue && n.nodeValue.replace(/\s/g,'').length > 0) nodes.push(n); }
                var seg = 0;
                nodes.forEach(function(node){
                    var parts = node.nodeValue.match(/[^.!?]+[.!?]+\s*|[^.!?]+${'$'}/g);
                    if (!parts) return;
                    var frag = document.createDocumentFragment();
                    parts.forEach(function(p){
                        if (p.replace(/\s/g,'').length === 0) { frag.appendChild(document.createTextNode(p)); return; }
                        var sp = document.createElement('span');
                        sp.className = '__seg';
                        sp.setAttribute('data-seg', seg++);
                        sp.textContent = p;
                        frag.appendChild(sp);
                    });
                    if (node.parentNode) node.parentNode.replaceChild(frag, node);
                });
                window.__segWrapped = true;
            }
            window.__normSeg = function(s){
                return s.replace(/[‘’]/g,"'").replace(/[“”]/g,'"').replace(/[–—]/g,'-').replace(/\s+/g,' ').trim();
            };
            window.__pageBase = function(){
                var cp = (typeof window.__currentPage === 'number') ? window.__currentPage : 0;
                return cp * window.innerWidth;
            };
            window.__findSpan = function(text){
                var t = window.__normSeg(text);
                if (!t) return null;
                var spans = document.querySelectorAll('span.__seg');
                for (var i=0;i<spans.length;i++){
                    var st = window.__normSeg(spans[i].textContent);
                    if (st === t || st.indexOf(t) === 0 || t.indexOf(st) === 0) return spans[i];
                }
                return null;
            };
            window.__pageOfSentence = function(text){
                var sp = window.__findSpan(text);
                if (!sp) return -1;
                var r = sp.getBoundingClientRect();
                return Math.floor((r.left + window.__pageBase()) / window.innerWidth);
            };
            window.__highlightSentence = function(text){
                var prev = document.querySelector('span.__seg.__active');
                if (prev) prev.classList.remove('__active');
                var sp = window.__findSpan(text);
                if (sp) sp.classList.add('__active');
            };
            window.__clearHighlight = function(){
                var prev = document.querySelector('span.__seg.__active');
                if (prev) prev.classList.remove('__active');
            };
            window.__firstSentenceOfPage = function(page){
                var spans = document.querySelectorAll('span.__seg');
                for (var i=0;i<spans.length;i++){
                    var r = spans[i].getBoundingClientRect();
                    var pg = Math.floor((r.left + window.__pageBase()) / window.innerWidth);
                    if (pg === page) return spans[i].textContent;
                }
                return '';
            };
        })();
        """.trimIndent()
    }
```

- [ ] **Step 3: Ejecutar la envoltura al cargar el capítulo**

En `onPageFinished`, dentro del callback de `resetScript`, ANTES del bloque
`if (goToLastOnLoad.value)`, ejecutar la envoltura (los helpers deben existir antes
de cualquier resaltado posterior):

```kotlin
                                view.evaluateJavascript(resetScript) {
                                    view.evaluateJavascript(segScript, null)
                                    // 3. Si venimos de "capítulo anterior", ir a la última página
                                    if (goToLastOnLoad.value) {
```

- [ ] **Step 4: Compilar e instalar**

Run: `.\gradlew.bat :app:assembleDebug`
Then: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
Expected: BUILD SUCCESSFUL, Success.

- [ ] **Step 5: Verificar en dispositivo (paginación intacta)**

Abrir un libro y leer un capítulo pasando páginas a mano: la paginación funciona
igual que antes (las frases envueltas no cambian el número de páginas ni el flujo).
No hay cambios visibles aún.

- [ ] **Step 6: Commit**

```bash
git add feature/reader/src/main/java/com/ebookreader/feature/reader/epub/EpubReaderView.kt
git commit -m "feat: envolver frases en spans y helpers JS de mapeo frase-pagina"
```

---

## Task 2: Resaltado + auto-avance + seguir al narrador entre capítulos

**Files:**
- Modify: `feature/reader/src/main/java/com/ebookreader/feature/reader/epub/EpubReaderView.kt`
- Modify: `feature/reader/src/main/java/com/ebookreader/feature/reader/ReaderViewModel.kt`
- Modify: `feature/reader/src/main/java/com/ebookreader/feature/reader/ReaderScreen.kt`

- [ ] **Step 1: Añadir `visibleChapterIndex` a `EpubReaderView`**

En la firma, tras `currentTtsSegment: TextSegment?,`:

```kotlin
    currentTtsSegment: TextSegment?,
    visibleChapterIndex: Int,
    initialPage: Int,
```

- [ ] **Step 2: Holders para el segmento y el capítulo visible**

Tras `initialPageApplied` (≈L194), añadir:

```kotlin
    val segmentHolder = remember { mutableStateOf<TextSegment?>(null) }
    segmentHolder.value = currentTtsSegment
    val visibleChapterHolder = remember { mutableStateOf(visibleChapterIndex) }
    visibleChapterHolder.value = visibleChapterIndex
```

- [ ] **Step 3: `LaunchedEffect` de resaltado + auto-avance**

Tras el `LaunchedEffect(styleScript) { ... }` (≈L235-237), añadir:

```kotlin
    // Resalta la frase del TTS y lleva el visor a su página (si es del capítulo
    // visible). Si no aplica, limpia el resaltado.
    LaunchedEffect(currentTtsSegment, visibleChapterIndex) {
        val wv = webViewRef.value ?: return@LaunchedEffect
        val seg = currentTtsSegment
        if (seg != null && seg.chapterIndex == visibleChapterIndex) {
            val json = org.json.JSONObject.quote(seg.text)
            wv.evaluateJavascript(
                "javascript:(function(){ if(!window.__highlightSentence) return; window.__highlightSentence($json); var pg = window.__pageOfSentence($json); if (pg >= 0 && pg !== window.__currentPage) { window.__currentPage = pg; window.scrollTo(pg * window.innerWidth, 0); } })();",
                null
            )
        } else {
            wv.evaluateJavascript(
                "javascript:(function(){ if(window.__clearHighlight) window.__clearHighlight(); })();",
                null
            )
        }
    }
```

- [ ] **Step 4: Re-resaltar y reposicionar tras cargar el capítulo**

En `onPageFinished`, dentro del callback de `resetScript`, DESPUÉS del bloque de
`initialPage` (`if (startPage > 0 && !initialPageApplied.value) { ... }`), añadir:

```kotlin
                                    val activeSeg = segmentHolder.value
                                    if (activeSeg != null && activeSeg.chapterIndex == visibleChapterHolder.value) {
                                        val sJson = org.json.JSONObject.quote(activeSeg.text)
                                        view.evaluateJavascript(
                                            "javascript:(function(){ if(!window.__highlightSentence) return; window.__highlightSentence($sJson); var pg = window.__pageOfSentence($sJson); if (pg >= 0) { window.__currentPage = pg; window.scrollTo(pg * window.innerWidth, 0); } })();",
                                            null
                                        )
                                    }
```

- [ ] **Step 5: `syncVisibleChapter` en `ReaderViewModel`**

Junto a `jumpToChapter` (≈L215), añadir:

```kotlin
    /**
     * Sincroniza el capítulo visible con el que lee el TTS, sin reordenar el TTS
     * (unidireccional TTS→visor; evita bucles). Para seguir al narrador al cruzar
     * de capítulo.
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

- [ ] **Step 6: Sincronizar capítulo y pasar `visibleChapterIndex` en `ReaderScreen`**

En `ReaderScreen.kt`, tras las declaraciones de estado (después de
`val bookmarks by viewModel.bookmarks.collectAsState()`), añadir:

```kotlin
    LaunchedEffect(currentSegment?.chapterIndex, ttsState.isPlaying) {
        val seg = currentSegment
        if (ttsState.isPlaying && seg != null &&
            seg.chapterIndex != uiState.currentChapterIndex
        ) {
            viewModel.syncVisibleChapter(seg.chapterIndex)
        }
    }
```

Y en la llamada a `EpubReaderView(...)`, añadir el argumento tras
`currentTtsSegment = currentSegment,`:

```kotlin
                                currentTtsSegment = currentSegment,
                                visibleChapterIndex = uiState.currentChapterIndex,
                                initialPage = uiState.initialPage,
```

- [ ] **Step 7: Compilar, instalar y verificar**

Run: `.\gradlew.bat :app:assembleDebug` → BUILD SUCCESSFUL.
Then: `adb install -r app/build/outputs/apk/debug/app-debug.apk`.
Verificar: reproducir TTS desde el visor → la frase suena **resaltada**; al pasar de
las frases visibles, el visor **avanza de página solo**; al cruzar de capítulo, el
visor **cambia de capítulo** y sigue resaltando. Pausar → desaparece el resaltado.

- [ ] **Step 8: Commit**

```bash
git add feature/reader/src/main/java/com/ebookreader/feature/reader/epub/EpubReaderView.kt feature/reader/src/main/java/com/ebookreader/feature/reader/ReaderViewModel.kt feature/reader/src/main/java/com/ebookreader/feature/reader/ReaderScreen.kt
git commit -m "feat: resaltado, auto-avance de pagina y seguir al narrador entre capitulos"
```

---

## Task 3: Play desde la página visible (visor → audio)

**Files:**
- Modify: `core/tts/src/main/java/com/ebookreader/core/tts/controller/TtsController.kt`
- Modify: `core/tts/src/main/java/com/ebookreader/core/tts/controller/TtsControllerImpl.kt`
- Modify: `feature/reader/src/main/java/com/ebookreader/feature/reader/epub/EpubReaderView.kt`
- Modify: `feature/reader/src/main/java/com/ebookreader/feature/reader/ReaderScreen.kt`
- Modify: `feature/reader/src/main/java/com/ebookreader/feature/reader/ReaderViewModel.kt`

- [ ] **Step 1: `jumpToSentenceByText` en la interfaz `TtsController`**

Tras `jumpToSegment`:

```kotlin
    /** Salta al primer segmento del capítulo cuyo texto coincide con [text]. */
    suspend fun jumpToSentenceByText(chapter: Int, text: String)
```

- [ ] **Step 2: Implementar en `TtsControllerImpl`**

Añadir (junto a `jumpToSegment`):

```kotlin
    override suspend fun jumpToSentenceByText(chapter: Int, text: String) {
        if (segments.isEmpty()) return
        val norm = normalizeForMatch(text)
        if (norm.isBlank()) return
        val candidates = segments.withIndex().filter { it.value.chapterIndex == chapter }
        if (candidates.isEmpty()) return
        val match = candidates.firstOrNull {
            val st = normalizeForMatch(it.value.text)
            st == norm || st.startsWith(norm) || norm.startsWith(st)
        } ?: candidates.first()
        jumpToSegment(match.index)
    }

    private fun normalizeForMatch(s: String): String =
        s.replace('‘', '\'').replace('’', '\'')
            .replace('“', '"').replace('”', '"')
            .replace('–', '-').replace('—', '-')
            .replace(Regex("\\s+"), " ").trim()
```

- [ ] **Step 3: `onVisibleSentenceChanged` en `EpubReaderView`**

Añadir el parámetro a la firma, tras `onPageChanged: (Int) -> Unit,`:

```kotlin
    onPageChanged: (Int) -> Unit,
    onVisibleSentenceChanged: (String) -> Unit,
    onPreviousChapter: () -> Unit,
```

Y su `rememberUpdatedState` junto a los otros (≈L197):

```kotlin
    val latestOnVisibleSentenceChanged by rememberUpdatedState(onVisibleSentenceChanged)
```

- [ ] **Step 4: Notificar la frase visible al pasar página a mano**

En los handlers de tap, en el `else` (navegación intra-capítulo) de AMBAS zonas
(prev y next), donde hoy se lee `window.__currentPage` y se llama
`latestOnPageChanged`, añadir la lectura de la primera frase de la nueva página.
Reemplazar el bloque `else { wv.evaluateJavascript("...__currentPage...") { p -> ... } }`
por (en la zona PREV y, idéntico, en la NEXT):

```kotlin
                                        } else {
                                            wv.evaluateJavascript("(function(){ return window.__currentPage; })();") { p ->
                                                p?.toIntOrNull()?.let { latestOnPageChanged(it) }
                                            }
                                            wv.evaluateJavascript("(function(){ return window.__firstSentenceOfPage(window.__currentPage); })();") { s ->
                                                val decoded = runCatching { org.json.JSONTokener(s ?: "").nextValue() as? String }.getOrNull() ?: ""
                                                if (decoded.isNotBlank()) latestOnVisibleSentenceChanged(decoded)
                                            }
                                        }
```

- [ ] **Step 5: Guardar la frase visible y bandera en `ReaderViewModel`**

Añadir campos privados (junto a `bookId`):

```kotlin
    private var pendingStartSentence: String = ""
    private var userNavigatedSincePlay: Boolean = false
```

Añadir el método:

```kotlin
    fun onVisibleSentenceChanged(text: String) {
        if (text.isNotBlank()) {
            pendingStartSentence = text
            userNavigatedSincePlay = true
        }
    }
```

- [ ] **Step 6: `playPauseTts` arranca en la página visible si procede**

Reemplazar el cuerpo del `else` (cuando NO está reproduciendo) de `playPauseTts`
por dos caminos:

```kotlin
            } else {
                val state = _uiState.value
                if (userNavigatedSincePlay && pendingStartSentence.isNotBlank()) {
                    // El usuario navegó a mano: arrancar en la frase de la página visible.
                    ttsController.jumpToSentenceByText(state.currentChapterIndex, pendingStartSentence)
                    userNavigatedSincePlay = false
                    ttsController.play()
                } else {
                    val readerChapter = state.currentChapterIndex
                    val currentLen = state.chapterTextLengths.getOrNull(readerChapter) ?: 0
                    val targetChapter = if (currentLen < NARRATIVE_CHAPTER_MIN_CHARS) {
                        state.firstContentChapterIndex.also { idx ->
                            if (idx != readerChapter) {
                                _uiState.update { it.copy(currentChapterIndex = idx) }
                                persistChapterPosition(idx, state.chapterFiles.size)
                            }
                        }
                    } else {
                        readerChapter
                    }
                    if (ttsController.state.value.currentChapterIndex != targetChapter) {
                        ttsController.jumpToChapter(targetChapter)
                    }
                    ttsController.play()
                }
            }
```

- [ ] **Step 7: Pasar `onVisibleSentenceChanged` en `ReaderScreen`**

En la llamada a `EpubReaderView(...)`, tras `onPageChanged = viewModel::onVisualPageChanged,`:

```kotlin
                                onPageChanged = viewModel::onVisualPageChanged,
                                onVisibleSentenceChanged = viewModel::onVisibleSentenceChanged,
                                onPreviousChapter = viewModel::previousChapter,
```

- [ ] **Step 8: Compilar, instalar y verificar**

Run: `.\gradlew.bat :app:assembleDebug` → BUILD SUCCESSFUL.
Then: `adb install -r app/build/outputs/apk/debug/app-debug.apk`.
Verificar: con el TTS parado, pasar varias páginas a mano y pulsar play → el TTS
arranca en la primera frase de la página visible. Pausar y reanudar sin navegar →
continúa la misma frase (no retrocede).

- [ ] **Step 9: Commit**

```bash
git add core/tts/src/main/java/com/ebookreader/core/tts/controller/TtsController.kt core/tts/src/main/java/com/ebookreader/core/tts/controller/TtsControllerImpl.kt feature/reader/src/main/java/com/ebookreader/feature/reader/epub/EpubReaderView.kt feature/reader/src/main/java/com/ebookreader/feature/reader/ReaderScreen.kt feature/reader/src/main/java/com/ebookreader/feature/reader/ReaderViewModel.kt
git commit -m "feat: al pulsar play tras navegar, arrancar en la frase de la pagina visible"
```

---

## Task 4: Verificación integral en dispositivo

- [ ] **Step 1: Recorrido completo**

1. Abrir libro, play → frase resaltada, el visor avanza de página siguiendo al audio.
2. Dejar que cruce de capítulo → el visor cambia de capítulo y sigue.
3. Pausar → resaltado desaparece; reanudar → continúa la misma frase.
4. Parar, pasar páginas a mano, play → arranca en la página visible.
5. Cerrar la app y reabrir (persistencia fase 1) → reanuda donde iba; al darle play,
   el visor muestra y resalta la frase.

- [ ] **Step 2: Si una frase no se resalta o no avanza**

Es degradación suave esperada (frase con formato atípico que no casa). Solo
investigar si NINGUNA frase casa (indicaría fallo de la envoltura): capturar logcat
`EpubReaderJS`.

---

## Self-Review (completado durante la escritura)

- **Cobertura del spec:** envoltura en spans + helpers (Task 1); resaltado +
  auto-avance + seguir al narrador entre capítulos (Task 2); play desde página +
  `jumpToSentenceByText` + distinción reanudar/navegar (Task 3); página por
  `span.left` con `__pageBase` (Task 1, evita depender de `scrollX`); emparejado por
  texto normalizado (Task 1 JS y Task 3 Kotlin, misma normalización). ✔
- **Sin placeholders:** todo el JS y Kotlin está completo. ✔
- **Consistencia:** `visibleChapterIndex`, `onVisibleSentenceChanged`,
  `syncVisibleChapter`, `jumpToSentenceByText(chapter, text)`,
  helpers `__highlightSentence`/`__pageOfSentence`/`__firstSentenceOfPage`/`__clearHighlight`
  referenciados igual en todos los sitios. ✔
- **Anti-bucle:** `syncVisibleChapter` no toca el `TtsController`; el auto-scroll no
  llama `onPageChanged`; `onVisibleSentenceChanged` solo se dispara desde taps
  manuales (no desde el auto-scroll). ✔
- **Robustez de página:** se usa `__currentPage*innerWidth` (controlado) en vez de
  `window.scrollX` (poco fiable en este WebView). ✔
