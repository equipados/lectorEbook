# Diseño: Resaltado sincronizado durante TTS

> Fecha: 2026-06-02
> Feature independiente. Implementar después del índice enriquecido.

## Problema

Al reproducir un libro con TTS no hay indicación visual de qué frase se está
leyendo. Se quiere resaltar en el visor la frase que suena y, además, que el
visor "siga al narrador" (avance de página y de capítulo solo).

## Contexto técnico (confirmado en la investigación)

- El visor es un **WebView** (`feature/reader/.../epub/EpubReaderView.kt`) que
  carga el XHTML original del capítulo: `view.loadUrl("file://$chapterFilePath")`
  (≈L282). La paginación es por **columnas CSS** con helpers JS ya inyectados
  (`window.__currentPage`, `__nextPage`, `__prevPage`, `__recalc`) en
  `onPageFinished` (≈L258-273).
- El TTS usa una **extracción independiente**: `extractTextContent()` →
  `stripHtmlTags()` produce texto plano, dividido en frases
  (`TtsControllerImpl.splitIntoSentences`). Cada `TextSegment` tiene
  `text`, `startOffset`, `endOffset` (relativos al texto plano del capítulo) y
  `chapterIndex`. `currentSegment: StateFlow<TextSegment?>` expone el actual.
- `EpubReaderView` **ya recibe** `currentTtsSegment: TextSegment?`
  (`ReaderScreen.kt:157`) pero **no lo usa**.
- Los offsets del texto plano **no** mapean 1:1 al DOM del WebView → el resaltado
  se hace localizando el **texto de la frase** dentro del DOM.

## Enfoque elegido

Búsqueda de la frase en el DOM vía JavaScript + **CSS Custom Highlight API**
(`Range` + `::highlight`). Motivo: no inserta nodos en el DOM, por lo que **no
altera el layout ni rompe la paginación por columnas**. Soportado por el WebView
de Android 15.

### Resaltado (JS inyectado)

Función `window.__highlightSentence(text)`:
1. Normalizar `text` y el contenido del DOM: colapsar espacios, unificar comillas
   tipográficas/rectas y guiones. (La frase viene de `stripHtmlTags`, el DOM trae
   formato/entidades; la normalización maximiza coincidencias.)
2. Recorrer los nodos de texto del `body`, concatenando su texto normalizado y
   registrando, por nodo, su rango de offsets en la cadena concatenada.
3. Buscar la frase normalizada en la cadena. Si no aparece → no resaltar (sin
   error). Si aparece → construir un `Range` DOM que cubra esos offsets (mapeando
   inicio/fin de la frase a nodo+offset).
4. Registrar el `Range` en un `Highlight` y publicarlo con
   `CSS.highlights.set('tts', highlight)`. Limpiar el anterior antes.
5. **Auto-avance de página:** calcular la página/columna del `Range` con
   `range.getBoundingClientRect().x` y el ancho de página; si difiere de
   `__currentPage`, hacer `scrollTo` a esa página y actualizar `__currentPage`.

CSS inyectado junto al `styleScript` existente:
```css
::highlight(tts) { background-color: <acento translúcido del tema>; }
```

### Integración en `EpubReaderView`

- `LaunchedEffect(currentTtsSegment)`: si el segmento es no nulo y su
  `chapterIndex` == capítulo visible, `evaluateJavascript("__highlightSentence(<json>)")`.
  Si es nulo (pausa/stop) → `__clearHighlight()` (limpia `CSS.highlights`).
- Re-resaltar tras `onPageFinished` cuando ya hay un segmento activo (al cambiar
  de capítulo el WebView recarga y hay que reaplicar).

### Seguir al narrador entre capítulos (alcance añadido aprobado)

Hoy el visor no cambia de capítulo cuando el TTS cruza al siguiente. Para
"seguir al narrador":
- Observar `currentSegment.chapterIndex`. Si el TTS está reproduciendo y ese
  índice difiere del `currentChapterIndex` visible, actualizar
  `currentChapterIndex` (el WebView carga el nuevo capítulo y reaplica resaltado).
- Guardas anti-bucle: el cambio de capítulo visible **no** debe reordenar el TTS
  (la sincronización es unidireccional TTS → visor durante la reproducción). El
  salto manual de capítulo del usuario sigue funcionando como hoy.

### Limpieza

- Pausa/stop del TTS → `__clearHighlight()`.
- Cambio manual de capítulo → el resaltado se recalcula para el nuevo capítulo
  (o se limpia si la frase no está).

## Casos borde

- Frase que no casa por formato atípico → no se resalta (degradación silenciosa);
  el TTS sigue sonando.
- Frase repetida en el capítulo → se resalta la primera coincidencia desde el
  inicio; aceptable para el MVP.
- `CSS.highlights` no disponible → fallback: no resaltar (no romper); registrar
  el caso. (No esperado en Android 15.)
- Cambio de tamaño de fuente/tema → recalcular página del `Range` en el siguiente
  resaltado.

## Qué NO incluye (YAGNI)

- No se resalta palabra por palabra (solo frase).
- No se reescribe el render del capítulo ni se pre-envuelven spans.
- No se sincroniza la posición del visor hacia el TTS (solo TTS → visor).

## Verificación

- Reproducir TTS desde el visor → la frase sonando aparece resaltada.
- Al terminar una página, el visor avanza solo a la página de la frase siguiente.
- Al cruzar al siguiente capítulo, el visor cambia de capítulo solo y sigue
  resaltando.
- Pausar → desaparece el resaltado. Reanudar → vuelve a resaltar.
- Saltar de página/capítulo manualmente sigue funcionando.
