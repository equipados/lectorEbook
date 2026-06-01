# Diseño: MediaSession para el TTS (controles multimedia externos)

> Fecha: 2026-06-01
> Estado: aprobado, pendiente de plan de implementación
> Módulos afectados: `core:tts`, `feature:audioplayer`, `app` (manifest)

## Objetivo

Permitir controlar la narración TTS desde controles multimedia externos: auriculares
bluetooth, sistemas de coche (Android Auto básico vía MediaSession) y la pantalla de
bloqueo. Hoy existe un foreground service (`TtsPlaybackService`) con una notificación de
4 botones, pero **no hay `MediaSession`**, por lo que los controles externos y los botones
de hardware no funcionan, y la pantalla de bloqueo no muestra metadata del libro.

## Decisiones de comportamiento (acordadas con el usuario)

1. **Botones anterior/siguiente en controles externos = saltar de capítulo** (como pistas
   de un audiolibro). Los botones de frase a frase, si se conservan, quedan solo dentro de
   la pantalla del reproductor, no en los controles externos.
2. **Metadata en lockscreen/coche = portada + título del libro + capítulo actual.**
3. **Foco de audio = pausar y reanudar.** Pausa al perder el foco; reanuda al recuperarlo
   si la pérdida fue transitoria (llamada, otra app de audio puntual).

## Decisión técnica — librería

Se usa **`MediaSessionCompat`** (`androidx.media:media`), no media3.

Motivo: el TTS es un reproductor de segmentos discretos (frase/capítulo) **sin timeline de
milisegundos, sin duración real y sin seek**. `media3.MediaSession` exige un
`androidx.media3.common.Player`, lo que obligaría a escribir un adaptador `SimpleBasePlayer`
que invente posición/duración para un motor que no las tiene: más código y conceptualmente
forzado. `MediaSessionCompat` modela este caso sin fricción y encaja con el `Service` actual.

- **Dependencia nueva:** `androidx.media:media` (añadir al version catalog y a `core:tts`).
- **Nota (fuera de alcance):** `core:tts` declara hoy `media3-session` y `media3-exoplayer`
  sin usarlas. Quedan como candidatas a eliminación en una limpieza posterior; este trabajo
  no las toca.

## Arquitectura de componentes

```
TtsPlaybackService  (existe — se amplía)
 ├─ MediaSessionCompat            ← nuevo: token, callbacks, PlaybackState, metadata
 ├─ PlaybackWakeLockController    ← existe, sin cambios
 ├─ AudioFocusController          ← nuevo, testeable (patrón del wakelock controller)
 └─ Notificación MediaStyle       ← buildNotification reescrito con setMediaSession(token)

TtsController / TtsControllerImpl  (se amplía)
 ├─ loadText(...)                 ← ahora guarda los títulos de capítulo (hoy los descarta)
 ├─ setBookInfo(title, author, coverPath)   ← nuevo
 └─ nowPlaying: StateFlow<NowPlayingMetadata>  ← nuevo (libro + capítulo actual)
```

### `NowPlayingMetadata` (modelo nuevo en `core:tts`)

```kotlin
data class NowPlayingMetadata(
    val bookTitle: String = "",
    val author: String = "",
    val chapterTitle: String = "",
    val coverPath: String? = null
)
```

## Flujo de la metadata

El `TtsController` es `@Singleton` e inyectado tanto en `AudioPlayerViewModel` como en
`TtsPlaybackService`, por lo que es la fuente de verdad natural de la metadata.

1. `AudioPlayerViewModel.loadBook()` ya dispone del `BookEntity`; llama a
   `ttsController.setBookInfo(book.title, book.author, book.coverPath)`.
2. `loadText` deja de descartar los títulos: guarda `chapterTitles: List<String>`.
3. El controller expone `nowPlaying: StateFlow<NowPlayingMetadata>`, recomputado cuando
   cambia `currentChapterIndex` (toma el título del capítulo actual + datos del libro).
4. El Service observa `nowPlaying`:
   - Carga el bitmap de `coverPath` en `Dispatchers.IO`, cacheado (no recargar en cada cambio
     de frase; solo si cambia el path).
   - Construye `MediaMetadataCompat`: `METADATA_KEY_TITLE` = título de capítulo,
     `METADATA_KEY_ARTIST` y `METADATA_KEY_ALBUM` = libro/autor,
     `METADATA_KEY_ALBUM_ART` = bitmap de portada.
   - Usa el bitmap como large icon de la notificación.

## PlaybackState y callbacks de la sesión

- **PlaybackState:** `STATE_PLAYING` / `STATE_PAUSED` según `TtsState.isPlaying`.
  Acciones soportadas: `ACTION_PLAY | ACTION_PAUSE | ACTION_PLAY_PAUSE |
  ACTION_SKIP_TO_NEXT | ACTION_SKIP_TO_PREVIOUS | ACTION_STOP`.
- **Callbacks (`MediaSessionCompat.Callback`):**
  - `onPlay` → `ttsController.play()`
  - `onPause` → `ttsController.pause()`
  - `onStop` → `ttsController.stop()` + parar foreground + liberar wakelock/foco
  - `onSkipToNext` → `ttsController.nextChapter()`
  - `onSkipToPrevious` → `ttsController.previousChapter()`
- **Unificación:** los `ACTION_NEXT` / `ACTION_PREV` del Service (hoy llaman a
  `nextSentence` / `previousSentence`) pasan a `nextChapter` / `previousChapter`.
- **Botones de hardware (cascos bluetooth):** `MediaButtonReceiver.handleIntent(session, intent)`
  en `onStartCommand`. Requiere registrar el `MediaButtonReceiver` de `androidx.media`.

## Foco de audio — `AudioFocusController`

Componente nuevo y testeable (mismo patrón que `PlaybackWakeLockController`: la API de
`AudioManager` se abstrae tras una interfaz mockeable).

- Al iniciar reproducción: solicita foco (`AudioFocusRequest`, `AUDIOFOCUS_GAIN`, minSdk 26 ✓).
- `AUDIOFOCUS_LOSS` → pausa definitiva (no se marca para reanudar).
- `AUDIOFOCUS_LOSS_TRANSIENT` (y `..._CAN_DUCK`) → pausa y marca para reanudar; **no se hace
  ducking** (bajar el volumen de una narración no tiene sentido).
- `AUDIOFOCUS_GAIN` → reanuda solo si la pausa fue transitoria.

## Error handling

- Si `coverPath` es nulo o el bitmap no decodifica → metadata sin `ALBUM_ART` y notificación
  con el icono por defecto de la app (no fallar).
- Si `getSystemService(AudioManager)` o el `PowerManager` es nulo → degradar con gracia
  (reproducir sin foco / sin wakelock), igual que ya hace `createWakeLockController()`.
- Cargar el bitmap fuera del hilo principal; si falla, log y continuar.

## Testing

Sigue el patrón ya inaugurado con `PlaybackWakeLockControllerTest`:

- `AudioFocusController`: interfaz de `AudioManager` abstraída y mockeada con MockK.
  Casos: gana foco al reproducir; pausa+reanuda en pérdida transitoria; pausa definitiva en
  pérdida permanente; no reanuda tras pérdida permanente.
- Funciones puras de mapeo `TtsState → PlaybackStateCompat` y
  `NowPlayingMetadata → MediaMetadataCompat` (extraídas para ser testeables sin Android
  framework, o con Robolectric si hace falta el builder).
- `TtsControllerImpl`: `loadText` guarda títulos; `nowPlaying` emite el título del capítulo
  correcto al cambiar de capítulo.

## Alcance explícito

**Incluye:** MediaSession, metadata en lockscreen/coche con portada, botones de hardware,
foco de audio pausar/reanudar, notificación MediaStyle, unificación de prev/next a capítulo,
tests de los componentes nuevos.

**No incluye:** Android Auto certificado (solo el soporte básico que da MediaSession),
ducking de volumen, eliminación de las dependencias media3 sin usar, cambios en la UI de la
pantalla del reproductor más allá de lo necesario.
