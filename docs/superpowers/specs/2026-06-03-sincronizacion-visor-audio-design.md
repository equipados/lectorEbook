# Diseño: Sincronización visor ↔ audio (fase 2)

> Fecha: 2026-06-03
> Fase 2 de 2. La fase 1 (persistencia de posición) ya está en master.

## Problema

Al escuchar con TTS, el visor no acompaña al narrador: la pantalla no muestra la
página de la frase que suena, y al pulsar play tras pasar páginas a mano la lectura
empieza por el principio del capítulo, no por la página visible.

## Viabilidad (validada con un spike en dispositivo)

- Envolver cada frase del capítulo en un `<span>` **no rompe** la paginación por
  columnas (scrollWidth idéntico antes/después).
- El `getBoundingClientRect().left` de un **elemento** `<span>` sí refleja la
  columna visual (`distByRectLeft={0:36,1:36,2:31,3:14}` para 117 frases). En
  cambio, el de un `Range` y `offsetTop` dan el flujo lineal → inútiles. **Esto
  explica por qué falló el auto-avance anterior** (medía un Range).

Conclusión: la página de una frase = `floor((span.left + scrollX) / innerWidth)`.

## Objetivo

- **Resaltar** la frase que el TTS está leyendo.
- **audio → visor:** el visor avanza de página solo para mostrar la frase que suena.
- **visor → audio:** al pulsar play tras navegar a mano, el TTS arranca en la
  primera frase de la página visible (no al inicio del capítulo).

## Arquitectura

Todo el mecanismo nuevo vive en el visor. El `TtsController` solo aporta la frase
actual (ya lo hace) y gana un salto por texto. No se toca la paginación existente.

### 1. Envolver las frases (JS, al cargar el capítulo)

En `onPageFinished`, tras aplicar estilos, inyectar un script que recorre los nodos
de texto del `body`, los divide en frases con la **misma regla que el TTS**
(`[^.!?]+[.!?]+\s*|[^.!?]+$`) y envuelve cada frase en
`<span class="__seg" data-seg="i">`. Se hace una vez por carga de capítulo. Los
spans son `inline`, así que no alteran el layout.

CSS de resaltado inyectado con el resto: `span.__seg.__active { background-color: rgba(255,213,79,0.45); }`.

Helpers JS expuestos:
- `window.__pageOfSentence(text)` → localiza el span cuyo texto normalizado coincide
  con `text` (normalización: espacios colapsados, comillas/guiones tipográficos) y
  devuelve `floor((rect.left + scrollX) / innerWidth)`, o `-1` si no casa.
- `window.__highlightSentence(text)` → quita `.__active` del anterior y lo pone al
  span que coincide. `window.__clearHighlight()` lo quita.
- `window.__firstSentenceOfPage(page)` → recorre los spans en orden de `data-seg`,
  devuelve el `textContent` del primero cuya página calculada == `page`, o `""`.

La normalización y la búsqueda por texto reutilizan la lógica del resaltado previo.
Búsqueda O(nº spans) por evento (cientos de frases, cada varios segundos) → barato.

### 2. Resaltado + auto-avance (audio → visor)

`EpubReaderView` recibe `currentTtsSegment` (ya lo recibe) y `visibleChapterIndex`.
Un `LaunchedEffect(currentTtsSegment)`:
- Si la frase pertenece al capítulo visible: `__highlightSentence(text)`, calcular su
  página con `__pageOfSentence(text)` y, si difiere de `__currentPage`, hacer
  `scrollTo(page * innerWidth)` y actualizar `__currentPage`.
- Si no aplica (pausa con frase de otro capítulo, etc.): `__clearHighlight()`.

El auto-scroll **no** llama a `onPageChanged` (no es navegación manual del usuario).
Tras cargar un capítulo (`onPageFinished`), si hay frase activa de ese capítulo, se
re-resalta y reposiciona (con holders para evitar stale closures).

**Seguir al narrador entre capítulos.** Para que el visor acompañe al TTS cuando
cruza de capítulo, el `ReaderViewModel` observa `currentSegment.chapterIndex`: si el
TTS reproduce y su capítulo difiere del visible, cambia el capítulo visible **sin
reordenar el TTS** (método `syncVisibleChapter`, sincronización unidireccional
TTS→visor para evitar bucles). Al recargar el WebView con el nuevo capítulo, el
`LaunchedEffect` reaplica resaltado y posición.

### 3. Play desde la página visible (visor → audio)

Distinción clave entre **reanudar** (continuar la frase actual) y **arrancar en la
página visible** (tras navegar a mano):

- `EpubReaderView` marca cuando el usuario navega de página **a mano** (en los
  handlers de tap, que ya llaman `onPageChanged`). Esa navegación notifica también
  el texto de la primera frase de la página nueva vía un callback
  `onVisibleSentenceChanged(text)`.
- `ReaderViewModel` guarda `pendingStartSentence` (el texto) y una bandera
  `userNavigatedSincePlay`. La pone a `true` con cada navegación manual; el
  auto-scroll no la toca.
- En `playPauseTts`, al **arrancar** (no al pausar): si `userNavigatedSincePlay`,
  llamar `ttsController.jumpToSentenceByText(currentChapter, pendingStartSentence)`
  antes de `play()`, y resetear la bandera. Si no, comportamiento actual (continuar).

### 4. `TtsController.jumpToSentenceByText`

Nuevo método: `suspend fun jumpToSentenceByText(chapter: Int, text: String)`.
Busca entre los segmentos del capítulo `chapter` el primero cuyo texto normalizado
coincida (igualdad o "empieza por") con `text` y hace `jumpToSegment(globalIndex)`.
Si no casa ninguno, salta al primer segmento del capítulo (degradación suave).

## Componentes y responsabilidades

- `EpubReaderView`: envoltura JS de frases, helpers `__pageOfSentence` /
  `__highlightSentence` / `__firstSentenceOfPage`, `LaunchedEffect` de resaltado +
  auto-scroll, callback `onVisibleSentenceChanged`, parámetro `visibleChapterIndex`.
- `ReaderScreen`: pasar `visibleChapterIndex` y `onVisibleSentenceChanged`.
- `ReaderViewModel`: bandera de navegación manual + `pendingStartSentence`; en
  `playPauseTts`, saltar a la frase visible si procede.
- `TtsController`: `jumpToSentenceByText`.

## Casos borde

- Frase del TTS que no casa con ningún span (formato atípico) → no se resalta ni se
  avanza esa vez; el TTS sigue. Es degradación suave, no error.
- Cambio de tamaño de letra/tema → las páginas de los spans se recalculan al vuelo
  (no se cachean), así que el mapeo sigue válido tras el reflow.
- Frase repetida en el capítulo → se usa la primera coincidencia (aceptable).
- Capítulo sin spans (carga incompleta) → helpers devuelven -1/"" y no hacen nada.
- Reanudar tras pausa sin navegar → continúa la frase actual (no salta).

## Qué NO incluye (YAGNI)

- Resaltado palabra por palabra (solo frase).
- Sincronizar la posición de scroll vertical (la paginación es horizontal).
- Cachear el mapeo frase↔página (se calcula al vuelo; evita invalidación por reflow).

## Verificación (en dispositivo, por hitos)

1. Cargar capítulo → las frases quedan envueltas sin cambiar la paginación (mismas
   páginas que antes).
2. Reproducir → la frase suena resaltada; al pasar de las frases visibles, el visor
   avanza de página solo siguiendo al narrador.
3. Cruzar de capítulo escuchando → el visor cambia de capítulo y sigue resaltando.
4. Navegar a mano varias páginas y pulsar play → el TTS arranca en la primera frase
   de la página visible.
5. Pausar y reanudar sin navegar → continúa la misma frase (no retrocede).
