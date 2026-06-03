# Diseño: Persistencia de posición de lectura

> Fecha: 2026-06-03
> Fase 1 de 2. La fase 2 (resaltado + auto-avance de página) queda fuera de alcance.

## Problema

Al cerrar la app y volver a abrir un libro, la lectura "empieza siempre donde
mismo". Causas confirmadas en el código actual:

1. Solo se guarda el **índice de capítulo** (`BookEntity.lastPosition`, vía
   `ReaderViewModel.persistChapterPosition`). No se guarda la frase ni la página.
2. La posición solo se persiste en **navegación manual** (cambiar capítulo, pulsar
   una entrada del índice). Mientras el **TTS lee solo y avanza**, no se guarda
   nada: el `ReaderViewModel` no observa el avance del TTS, y aunque lo hiciera, el
   ViewModel se destruye al cerrar la pantalla mientras el TTS sigue en segundo
   plano (foreground service).
3. Al reabrir, el TTS arranca desde el primer segmento del capítulo.

## Objetivo

Recordar por dónde iba la lectura y reanudar ahí:
- Guardar la **frase exacta** del TTS conforme avanza, también en segundo plano.
- Guardar la **página** del visor al leer a mano.
- Al reabrir, dejar el TTS **posicionado en la frase** (sin reproducir; el usuario
  pulsa play) y el visor en el capítulo + página guardados.

Decisiones de producto tomadas:
- Al reabrir: **posicionar y esperar a Play** (no reanudar automáticamente).
- Recordar posición **escuchando (frase) y leyendo (página)**.

## Modelo de posición

Una posición de lectura se compone de tres datos:
- `chapter: Int` — índice de capítulo (en el spine / `chapterFiles`).
- `segment: Int` — índice **global** del segmento (frase) del TTS. Posición de
  audio robusta (no depende del tamaño de letra). El capítulo es derivable de él.
- `page: Int` — página visible en el visor dentro del capítulo. Posición de lectura
  visual.

### Almacenamiento (campos separados, no un string)

Se añaden a `BookEntity` campos dedicados en vez de codificar todo en
`lastPosition`. Motivo: el avance del audio y el avance visual se guardan desde
sitios distintos y no deben pisarse al escribir a la vez.

```kotlin
@Entity(tableName = "books")
data class BookEntity(
    ...
    val progress: Float = 0f,
    val lastPosition: String = "",   // se mantiene por compatibilidad (capítulo)
    val lastChapter: Int = 0,        // nuevo
    val lastSegment: Int = 0,        // nuevo: frase global del TTS
    val lastPage: Int = 0,           // nuevo: página del visor
    ...
)
```

Migración Room **aditiva** (ADD COLUMN, sin borrar datos), inicializando
`lastChapter` desde el `lastPosition` existente cuando sea un entero.

### Operaciones de guardado (repositorio)

Dos operaciones que tocan campos distintos para no pisarse:
- `saveAudioPosition(bookId, chapter, segment)` — escribe `lastChapter` +
  `lastSegment` (+ `progress`). La invoca el `TtsController` al avanzar de frase.
- `saveVisualPosition(bookId, chapter, page)` — escribe `lastChapter` + `lastPage`.
  La invoca el visor al pasar de página a mano.

Ambas actualizan además `lastPosition = chapter.toString()` para no romper el
código que aún lo lee (bookmarks, restauración de capítulo del reproductor). La
restauración nueva usa los campos dedicados; `lastPosition` queda como espejo del
capítulo por compatibilidad.

`lastChapter` lo comparten ambas (refleja la última actividad). En el uso normal
audio y lectura están en el mismo capítulo; si divergieran, el visor restaura
dónde leías y el TTS dónde escuchabas.

## Arquitectura

### 1. Persistir el avance del audio (clave: ocurre en segundo plano)

El `TtsControllerImpl` (singleton, vive mientras el proceso, mantenido vivo por el
foreground service durante la reproducción) persiste su posición **en cada cambio
de `currentSegmentIndex`** (avance automático por `onDone`, salto de frase/capítulo,
etc.).

Para no acoplar `core/tts` a la base de datos, se define una interfaz en `core/tts`:

```kotlin
interface ReadingProgressStore {
    fun saveAudioPosition(bookId: Long, chapter: Int, segment: Int)
}
```

Implementada en `core/data` (o `app`) con el `BookRepository`. El `TtsController`
la recibe inyectada. La escritura va a Room en un scope de IO; persistir en cada
frase (~cada varios segundos) es barato.

`setBookInfo` pasa a recibir también `bookId` para saber qué libro persistir:
`setBookInfo(bookId, title, author, coverPath)`.

Guardas: no persistir si no hay `bookId` activo o si la lista de segmentos está
vacía (p. ej. durante la carga).

### 2. Persistir la página (lectura visual)

Cuando el usuario pasa de página **a mano** en el visor (taps laterales que mueven
`__currentPage` sin llegar a cambiar de capítulo), el `ReaderViewModel` guarda
`saveVisualPosition(bookId, currentChapter, page)`. El número de página actual se
obtiene del WebView (`window.__currentPage`) y se eleva a Compose mediante un
callback `onPageChanged(page)` desde `EpubReaderView`.

*Limitación asumida:* la página depende del tamaño de letra; si cambia entre
sesiones, la página restaurada puede desplazarse. La posición de audio (frase) no
se ve afectada.

### 3. Restaurar al abrir

`ReaderViewModel.loadBook` y `AudioPlayerViewModel` (ambos cargan el libro):
1. Leer la posición guardada del `BookEntity` (`lastChapter`, `lastSegment`,
   `lastPage`).
2. `ttsController.loadText(...)` y luego `ttsController.jumpToSegment(lastSegment)`
   para **posicionar sin reproducir**.
3. El visor muestra `lastChapter` y, tras cargar el capítulo, scrollea a `lastPage`
   (reutilizando los helpers de paginación del WebView: fijar `__currentPage` y
   `scrollTo`).
4. No se llama a `play()`: el usuario reanuda pulsando play.

### 4. Cambios en `TtsController`

- `jumpToSegment(globalIndex: Int)`: posiciona `currentSegmentIndex` y
  `currentChapterIndex` (derivado) en el segmento dado, **sin reproducir**
  (`isPlaying` no cambia). Acota el índice al rango válido.
- `setBookInfo(bookId, title, author, coverPath)`: añade `bookId`.
- En cada cambio de `currentSegmentIndex`, llamar a
  `progressStore.saveAudioPosition(bookId, chapter, segment)`.

## Componentes y responsabilidades

- `core/tts`: `ReadingProgressStore` (interfaz), `TtsController` (persiste audio,
  `jumpToSegment`, `setBookInfo` con bookId).
- `core/data`: campos nuevos en `BookEntity` + migración; `BookRepository`
  (`saveAudioPosition`, `saveVisualPosition`); implementación de
  `ReadingProgressStore`.
- `feature/reader`: restaurar posición en `loadBook`; persistir página al pasar
  página; `EpubReaderView` expone `onPageChanged` y acepta `initialPage`.
- `feature/audioplayer`: restaurar posición (posicionar el TTS en la frase).

## Casos borde

- Libro nunca abierto → posición (0,0,0): primer segmento, página 0.
- `lastSegment` fuera de rango (texto cambió) → acotar a [0, último]; si no hay
  segmentos, no posicionar.
- Migración: libros existentes → `lastChapter` desde `lastPosition`, `lastSegment`
  y `lastPage` a 0.
- Cambiar de motor TTS (local/nube) no cambia la segmentación → el `segment` sigue
  siendo válido.

## Qué NO incluye (YAGNI / fase 2)

- Resaltado de la frase y auto-avance de página siguiendo al narrador.
- Mapear página↔frase (no se convierte una en otra; cada modo guarda lo suyo).
- Sincronizar posición entre dispositivos.

## Verificación

- Escuchar con TTS varias frases, cerrar la app (incluso con pantalla apagada),
  reabrir → el libro queda posicionado en esa frase; al pulsar play reanuda ahí.
- Leer a mano pasando páginas, cerrar, reabrir → el visor abre en esa página.
- Cerrar a mitad de un capítulo distinto del primero → reabre en el capítulo
  correcto, no en el principio.
