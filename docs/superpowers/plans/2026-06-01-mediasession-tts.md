# MediaSession para el TTS — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Controlar la narración TTS desde auriculares bluetooth, coche y pantalla de bloqueo mediante `MediaSessionCompat`, mostrando portada + libro + capítulo y gestionando el foco de audio.

**Architecture:** Se amplía el `TtsPlaybackService` ya existente con una `MediaSessionCompat` (token, callbacks, PlaybackState, metadata y notificación `MediaStyle`). La metadata viaja por un nuevo `StateFlow<NowPlayingMetadata>` del `TtsController` (Singleton compartido entre ViewModel y Service). El foco de audio se aísla en un `AudioFocusController` testeable, siguiendo el mismo patrón que `PlaybackWakeLockController`. Los botones anterior/siguiente externos saltan de capítulo.

**Tech Stack:** Kotlin, Hilt, Coroutines/StateFlow, `androidx.media:media` (MediaSessionCompat), JUnit + MockK.

---

## Estructura de archivos

- **Crear** `core/tts/.../model/NowPlayingMetadata.kt` — modelo de metadata + función pura `buildNowPlaying`.
- **Crear** `core/tts/.../service/AudioFocusController.kt` — gestión testeable del foco de audio.
- **Crear** tests: `NowPlayingMetadataTest.kt`, `AudioFocusControllerTest.kt`.
- **Modificar** `core/tts/.../controller/TtsController.kt` — interfaz: `nowPlaying`, `setBookInfo`.
- **Modificar** `core/tts/.../controller/TtsControllerImpl.kt` — guardar títulos, emitir `nowPlaying`.
- **Modificar** `core/tts/.../service/TtsPlaybackService.kt` — MediaSession + MediaStyle + foco.
- **Modificar** `feature/audioplayer/.../AudioPlayerViewModel.kt` — llamar `setBookInfo`.
- **Modificar** `gradle/libs.versions.toml` y `core/tts/build.gradle.kts` — dependencia `androidx.media:media`.
- **Modificar** `app/src/main/AndroidManifest.xml` — `MediaButtonReceiver`.

> Nota de testing: `PlaybackStateCompat` / `MediaMetadataCompat` usan framework Android (Bundle), así que su construcción NO se testea en unit tests (se verifica manualmente en dispositivo). Lo que SÍ se testea es la lógica pura: `buildNowPlaying` y `AudioFocusController`. Es coherente con el patrón sin Robolectric que ya usa el proyecto.

---

### Task 1: Añadir la dependencia `androidx.media:media`

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `core/tts/build.gradle.kts:33-35`

- [ ] **Step 1: Añadir la versión y la librería al version catalog**

En `gradle/libs.versions.toml`, en `[versions]` añade tras la línea `media3 = "1.5.1"`:

```toml
androidxMedia = "1.7.0"
```

En `[libraries]`, tras el bloque `# Media3`, añade:

```toml
# MediaSessionCompat (controles multimedia externos)
androidx-media = { group = "androidx.media", name = "media", version.ref = "androidxMedia" }
```

- [ ] **Step 2: Declarar la dependencia en core:tts**

En `core/tts/build.gradle.kts`, sustituye el bloque `// Media3` (líneas 33-35) por:

```kotlin
    // Media3 (no usado actualmente; candidato a eliminación)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)

    // MediaSessionCompat (controles multimedia externos: bluetooth, coche, lockscreen)
    implementation(libs.androidx.media)
```

- [ ] **Step 3: Verificar que sincroniza/compila**

Run: `./gradlew :core:tts:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (resuelve `androidx.media:media:1.7.0`).

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml core/tts/build.gradle.kts
git commit -m "chore: add androidx.media para MediaSessionCompat"
```

---

### Task 2: Modelo `NowPlayingMetadata` + función pura `buildNowPlaying`

**Files:**
- Create: `core/tts/src/main/java/com/ebookreader/core/tts/model/NowPlayingMetadata.kt`
- Test: `core/tts/src/test/java/com/ebookreader/core/tts/model/NowPlayingMetadataTest.kt`

- [ ] **Step 1: Escribir el test que falla**

Crea `core/tts/src/test/java/com/ebookreader/core/tts/model/NowPlayingMetadataTest.kt`:

```kotlin
package com.ebookreader.core.tts.model

import org.junit.Assert.assertEquals
import org.junit.Test

class NowPlayingMetadataTest {

    @Test
    fun usaElTituloDelCapituloActual() {
        val result = buildNowPlaying(
            bookTitle = "El Quijote",
            author = "Cervantes",
            coverPath = "/covers/quijote.png",
            chapterTitles = listOf("Prólogo", "Capítulo 1", "Capítulo 2"),
            chapterIndex = 1
        )

        assertEquals("El Quijote", result.bookTitle)
        assertEquals("Cervantes", result.author)
        assertEquals("Capítulo 1", result.chapterTitle)
        assertEquals("/covers/quijote.png", result.coverPath)
    }

    @Test
    fun devuelveChapterTitleVacioSiElIndiceEstaFueraDeRango() {
        val result = buildNowPlaying(
            bookTitle = "Libro",
            author = "Autor",
            coverPath = null,
            chapterTitles = listOf("Único"),
            chapterIndex = 5
        )

        assertEquals("", result.chapterTitle)
        assertEquals(null, result.coverPath)
    }
}
```

- [ ] **Step 2: Ejecutar el test para verificar que falla**

Run: `./gradlew :core:tts:testDebugUnitTest --tests "*NowPlayingMetadataTest*"`
Expected: FAIL con error de compilación ("unresolved reference: buildNowPlaying" / "NowPlayingMetadata").

- [ ] **Step 3: Implementar el modelo y la función pura**

Crea `core/tts/src/main/java/com/ebookreader/core/tts/model/NowPlayingMetadata.kt`:

```kotlin
package com.ebookreader.core.tts.model

data class NowPlayingMetadata(
    val bookTitle: String = "",
    val author: String = "",
    val chapterTitle: String = "",
    val coverPath: String? = null
)

/**
 * Construye la metadata de "ahora sonando" combinando los datos del libro con
 * el título del capítulo actual. Función pura para poder testearla sin el
 * framework de Android.
 */
fun buildNowPlaying(
    bookTitle: String,
    author: String,
    coverPath: String?,
    chapterTitles: List<String>,
    chapterIndex: Int
): NowPlayingMetadata = NowPlayingMetadata(
    bookTitle = bookTitle,
    author = author,
    chapterTitle = chapterTitles.getOrNull(chapterIndex).orEmpty(),
    coverPath = coverPath
)
```

- [ ] **Step 4: Ejecutar el test para verificar que pasa**

Run: `./gradlew :core:tts:testDebugUnitTest --tests "*NowPlayingMetadataTest*"`
Expected: PASS (2 tests).

- [ ] **Step 5: Commit**

```bash
git add core/tts/src/main/java/com/ebookreader/core/tts/model/NowPlayingMetadata.kt core/tts/src/test/java/com/ebookreader/core/tts/model/NowPlayingMetadataTest.kt
git commit -m "feat: add NowPlayingMetadata y buildNowPlaying"
```

---

### Task 3: Ampliar `TtsController` con `nowPlaying` y `setBookInfo`

**Files:**
- Modify: `core/tts/src/main/java/com/ebookreader/core/tts/controller/TtsController.kt`
- Modify: `core/tts/src/main/java/com/ebookreader/core/tts/controller/TtsControllerImpl.kt`

> Esta tarea se apoya en `buildNowPlaying` (ya testeada). El cableado del flow es integración y se verifica en la Task 7; no se añade test unitario del Impl (su `init` depende de `Dispatchers.Main` y `UserPreferences`, costoso de mockear y fuera de alcance).

- [ ] **Step 1: Añadir a la interfaz `TtsController`**

En `core/tts/.../controller/TtsController.kt`, añade el import y los dos miembros:

```kotlin
import com.ebookreader.core.tts.model.NowPlayingMetadata
```

Dentro de la interfaz, tras `val currentSegment: StateFlow<TextSegment?>`:

```kotlin
    /** Metadata de "ahora sonando" para los controles multimedia externos. */
    val nowPlaying: StateFlow<NowPlayingMetadata>

    /** Datos del libro en reproducción (para lockscreen / coche). */
    fun setBookInfo(title: String, author: String, coverPath: String?)
```

- [ ] **Step 2: Implementar el estado en `TtsControllerImpl`**

En `core/tts/.../controller/TtsControllerImpl.kt`:

a) Añade el import:

```kotlin
import com.ebookreader.core.tts.model.NowPlayingMetadata
import com.ebookreader.core.tts.model.buildNowPlaying
```

b) Tras la declaración de `_currentSegment` (líneas ~36-37) añade los campos:

```kotlin
    private val _nowPlaying = MutableStateFlow(NowPlayingMetadata())
    override val nowPlaying: StateFlow<NowPlayingMetadata> = _nowPlaying.asStateFlow()

    private var chapterTitles: List<String> = emptyList()
    private var bookTitle: String = ""
    private var bookAuthor: String = ""
    private var bookCoverPath: String? = null
```

c) Añade el método `setBookInfo` y un helper privado (por ejemplo, antes de `private suspend fun speakCurrentSegment()`):

```kotlin
    override fun setBookInfo(title: String, author: String, coverPath: String?) {
        bookTitle = title
        bookAuthor = author
        bookCoverPath = coverPath
        refreshNowPlaying()
    }

    private fun refreshNowPlaying() {
        _nowPlaying.value = buildNowPlaying(
            bookTitle = bookTitle,
            author = bookAuthor,
            coverPath = bookCoverPath,
            chapterTitles = chapterTitles,
            chapterIndex = _state.value.currentChapterIndex
        )
    }
```

d) En `loadText`, al principio del cuerpo, guarda los títulos (hoy se descartan):

Sustituye la primera línea del método (`val built = mutableListOf<TextSegment>()`) por:

```kotlin
        chapterTitles = chapters.map { it.first }
        val built = mutableListOf<TextSegment>()
```

e) Suscribe la actualización de `nowPlaying` a los cambios de capítulo. En el bloque `init`, al final (tras el `.launchIn(scope)` existente), añade:

```kotlin
        // Recalcula la metadata de "ahora sonando" cada vez que cambia el capítulo.
        state
            .onEach { refreshNowPlaying() }
            .launchIn(scope)
```

- [ ] **Step 3: Verificar que compila**

Run: `./gradlew :core:tts:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add core/tts/src/main/java/com/ebookreader/core/tts/controller/TtsController.kt core/tts/src/main/java/com/ebookreader/core/tts/controller/TtsControllerImpl.kt
git commit -m "feat: exponer nowPlaying y setBookInfo en TtsController"
```

---

### Task 4: `AudioFocusController` testeable

**Files:**
- Create: `core/tts/src/main/java/com/ebookreader/core/tts/service/AudioFocusController.kt`
- Test: `core/tts/src/test/java/com/ebookreader/core/tts/service/AudioFocusControllerTest.kt`

- [ ] **Step 1: Escribir el test que falla**

Crea `core/tts/src/test/java/com/ebookreader/core/tts/service/AudioFocusControllerTest.kt`:

```kotlin
package com.ebookreader.core.tts.service

import android.media.AudioManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioFocusControllerTest {

    private fun controller(focus: FakeAudioFocusManager, sink: CallSink) =
        AudioFocusController(
            audioFocus = focus,
            onPause = { sink.pauseCalls++ },
            onResume = { sink.resumeCalls++ }
        )

    @Test
    fun pideFocoAlEmpezarReproduccion() {
        val focus = FakeAudioFocusManager(granted = true)
        val sink = CallSink()
        val c = controller(focus, sink)

        val granted = c.onPlaybackStarting()

        assertTrue(granted)
        assertEquals(1, focus.requestCalls)
    }

    @Test
    fun pausaYReanudaTrasPerdidaTransitoria() {
        val focus = FakeAudioFocusManager(granted = true)
        val sink = CallSink()
        val c = controller(focus, sink)
        c.onPlaybackStarting()

        c.onFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
        c.onFocusChange(AudioManager.AUDIOFOCUS_GAIN)

        assertEquals(1, sink.pauseCalls)
        assertEquals(1, sink.resumeCalls)
    }

    @Test
    fun noReanudaTrasPerdidaPermanente() {
        val focus = FakeAudioFocusManager(granted = true)
        val sink = CallSink()
        val c = controller(focus, sink)
        c.onPlaybackStarting()

        c.onFocusChange(AudioManager.AUDIOFOCUS_LOSS)
        c.onFocusChange(AudioManager.AUDIOFOCUS_GAIN)

        assertEquals(1, sink.pauseCalls)
        assertEquals(0, sink.resumeCalls)
    }

    @Test
    fun abandonaFocoAlParar() {
        val focus = FakeAudioFocusManager(granted = true)
        val sink = CallSink()
        val c = controller(focus, sink)
        c.onPlaybackStarting()

        c.onPlaybackStopping()

        assertEquals(1, focus.abandonCalls)
    }

    private class CallSink {
        var pauseCalls = 0
        var resumeCalls = 0
    }

    private class FakeAudioFocusManager(private val granted: Boolean) : AudioFocusManager {
        var requestCalls = 0
        var abandonCalls = 0
        override fun requestFocus(): Boolean { requestCalls++; return granted }
        override fun abandonFocus() { abandonCalls++ }
    }
}
```

- [ ] **Step 2: Ejecutar el test para verificar que falla**

Run: `./gradlew :core:tts:testDebugUnitTest --tests "*AudioFocusControllerTest*"`
Expected: FAIL (unresolved reference: `AudioFocusController` / `AudioFocusManager`).

- [ ] **Step 3: Implementar `AudioFocusController` + interfaz + impl real de Android**

Crea `core/tts/src/main/java/com/ebookreader/core/tts/service/AudioFocusController.kt`:

```kotlin
package com.ebookreader.core.tts.service

import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager

/** Abstracción del foco de audio para poder testear la lógica sin el framework. */
internal interface AudioFocusManager {
    fun requestFocus(): Boolean
    fun abandonFocus()
}

/** Implementación real basada en AudioManager (minSdk 26, API de AudioFocusRequest). */
internal class AndroidAudioFocusManager(
    private val audioManager: AudioManager,
    private val listener: AudioManager.OnAudioFocusChangeListener
) : AudioFocusManager {

    private val request: AudioFocusRequest =
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setOnAudioFocusChangeListener(listener)
            .build()

    override fun requestFocus(): Boolean =
        audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED

    override fun abandonFocus() {
        audioManager.abandonAudioFocusRequest(request)
    }
}

/**
 * Decide pausar/reanudar la narración según el foco de audio.
 * - Pérdida permanente (LOSS): pausa definitiva, no se marca para reanudar.
 * - Pérdida transitoria (LOSS_TRANSIENT / _CAN_DUCK): pausa y marca para reanudar.
 *   No se hace ducking: bajar el volumen de una narración no tiene sentido.
 * - GAIN: reanuda solo si la pausa fue transitoria.
 */
internal class AudioFocusController(
    private val audioFocus: AudioFocusManager,
    private val onPause: () -> Unit,
    private val onResume: () -> Unit
) {
    private var pausedByFocusLoss = false

    fun onPlaybackStarting(): Boolean = audioFocus.requestFocus()

    fun onPlaybackStopping() {
        pausedByFocusLoss = false
        audioFocus.abandonFocus()
    }

    fun onFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                pausedByFocusLoss = false
                onPause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                pausedByFocusLoss = true
                onPause()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (pausedByFocusLoss) {
                    pausedByFocusLoss = false
                    onResume()
                }
            }
        }
    }
}
```

- [ ] **Step 4: Ejecutar el test para verificar que pasa**

Run: `./gradlew :core:tts:testDebugUnitTest --tests "*AudioFocusControllerTest*"`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add core/tts/src/main/java/com/ebookreader/core/tts/service/AudioFocusController.kt core/tts/src/test/java/com/ebookreader/core/tts/service/AudioFocusControllerTest.kt
git commit -m "feat: add AudioFocusController para pausar/reanudar con el foco"
```

---

### Task 5: Integrar `MediaSessionCompat` en `TtsPlaybackService`

**Files:**
- Modify: `core/tts/src/main/java/com/ebookreader/core/tts/service/TtsPlaybackService.kt` (reescritura sustancial)

> Tarea de integración Android: sin test unitario (se verifica en la Task 7 en el dispositivo). Es la tarea más grande; aplícala completa antes de compilar.

- [ ] **Step 1: Reescribir el servicio con MediaSession**

Sustituye el contenido completo de `core/tts/.../service/TtsPlaybackService.kt` por:

```kotlin
package com.ebookreader.core.tts.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.session.MediaButtonReceiver
import com.ebookreader.core.tts.controller.TtsController
import com.ebookreader.core.tts.model.NowPlayingMetadata
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class TtsPlaybackService : Service() {

    @Inject
    lateinit var ttsController: TtsController

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var wakeLockController: PlaybackWakeLockController? = null
    private var audioFocusController: AudioFocusController? = null
    private lateinit var mediaSession: MediaSessionCompat

    // Caché del bitmap de portada para no decodificarlo en cada cambio de frase.
    private var cachedCoverPath: String? = null
    private var cachedCoverBitmap: Bitmap? = null

    companion object {
        const val CHANNEL_ID = "tts_playback"
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        wakeLockController = createWakeLockController()
        audioFocusController = createAudioFocusController()
        createNotificationChannel()
        initMediaSession()
        observeTtsState()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Botones de hardware (cascos bluetooth) llegan como ACTION_MEDIA_BUTTON.
        MediaButtonReceiver.handleIntent(mediaSession, intent)

        val initialState = ttsController.state.value
        startForeground(NOTIFICATION_ID, buildNotification(initialState.isPlaying))
        return START_STICKY
    }

    override fun onDestroy() {
        wakeLockController?.onServiceStopping()
        audioFocusController?.onPlaybackStopping()
        mediaSession.isActive = false
        mediaSession.release()
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun initMediaSession() {
        mediaSession = MediaSessionCompat(this, "TtsPlaybackService").apply {
            setCallback(mediaSessionCallback)
            isActive = true
        }
    }

    private val mediaSessionCallback = object : MediaSessionCompat.Callback() {
        override fun onPlay() {
            serviceScope.launch {
                if (audioFocusController?.onPlaybackStarting() != false) {
                    ttsController.play()
                }
            }
        }

        override fun onPause() {
            serviceScope.launch { ttsController.pause() }
        }

        override fun onStop() {
            serviceScope.launch {
                ttsController.stop()
                audioFocusController?.onPlaybackStopping()
                wakeLockController?.onServiceStopping()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        override fun onSkipToNext() {
            serviceScope.launch { ttsController.nextChapter() }
        }

        override fun onSkipToPrevious() {
            serviceScope.launch { ttsController.previousChapter() }
        }
    }

    private fun observeTtsState() {
        serviceScope.launch {
            ttsController.state.collectLatest { state ->
                wakeLockController?.onPlaybackStateChanged(state.isPlaying)
                updatePlaybackState(state.isPlaying)
                val manager = getSystemService(NotificationManager::class.java)
                manager.notify(NOTIFICATION_ID, buildNotification(state.isPlaying))
            }
        }
        serviceScope.launch {
            ttsController.nowPlaying.collectLatest { meta ->
                updateMetadata(meta)
                val manager = getSystemService(NotificationManager::class.java)
                manager.notify(NOTIFICATION_ID, buildNotification(ttsController.state.value.isPlaying))
            }
        }
    }

    private fun updatePlaybackState(isPlaying: Boolean) {
        val playState = if (isPlaying) {
            PlaybackStateCompat.STATE_PLAYING
        } else {
            PlaybackStateCompat.STATE_PAUSED
        }
        val state = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_STOP
            )
            .setState(playState, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1.0f)
            .build()
        mediaSession.setPlaybackState(state)
    }

    private suspend fun updateMetadata(meta: NowPlayingMetadata) {
        val cover = loadCover(meta.coverPath)
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, meta.chapterTitle)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, meta.author)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, meta.bookTitle)
            .apply { if (cover != null) putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, cover) }
            .build()
        mediaSession.setMetadata(metadata)
    }

    private suspend fun loadCover(path: String?): Bitmap? {
        if (path.isNullOrBlank()) return null
        if (path == cachedCoverPath) return cachedCoverBitmap
        val bitmap = withContext(Dispatchers.IO) {
            runCatching {
                val file = File(path)
                if (file.exists()) BitmapFactory.decodeFile(path) else null
            }.getOrNull()
        }
        cachedCoverPath = path
        cachedCoverBitmap = bitmap
        return bitmap
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "TTS Playback",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Controls for text-to-speech playback" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun createWakeLockController(): PlaybackWakeLockController? {
        val powerManager = getSystemService(PowerManager::class.java) ?: return null
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "$packageName:TtsPlaybackCpu"
        ).apply { setReferenceCounted(false) }
        return PlaybackWakeLockController(AndroidCpuWakeLock(wakeLock))
    }

    private fun createAudioFocusController(): AudioFocusController? {
        val audioManager = getSystemService(AudioManager::class.java) ?: return null
        lateinit var controller: AudioFocusController
        val focusManager = AndroidAudioFocusManager(audioManager) { focusChange ->
            controller.onFocusChange(focusChange)
        }
        controller = AudioFocusController(
            audioFocus = focusManager,
            onPause = { serviceScope.launch { ttsController.pause() } },
            onResume = { serviceScope.launch { ttsController.play() } }
        )
        return controller
    }

    private fun buildNotification(isPlaying: Boolean): Notification {
        val meta = ttsController.nowPlaying.value

        val playPauseAction = mediaAction(
            if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
            if (isPlaying) "Pause" else "Play",
            if (isPlaying) PlaybackStateCompat.ACTION_PAUSE else PlaybackStateCompat.ACTION_PLAY
        )
        val prevAction = mediaAction(
            android.R.drawable.ic_media_previous, "Anterior",
            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
        )
        val nextAction = mediaAction(
            android.R.drawable.ic_media_next, "Siguiente",
            PlaybackStateCompat.ACTION_SKIP_TO_NEXT
        )
        val stopIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(
            this, PlaybackStateCompat.ACTION_STOP
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(meta.bookTitle.ifBlank { "Lectura Audible" })
            .setContentText(meta.chapterTitle.ifBlank { if (isPlaying) "Reproduciendo" else "En pausa" })
            .setLargeIcon(cachedCoverBitmap)
            .setOnlyAlertOnce(true)
            .setOngoing(isPlaying)
            .setDeleteIntent(stopIntent)
            .addAction(prevAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
                    .setShowCancelButton(true)
                    .setCancelButtonIntent(stopIntent)
            )
            .build()
    }

    private fun mediaAction(icon: Int, title: String, action: Long): NotificationCompat.Action {
        val intent = MediaButtonReceiver.buildMediaButtonPendingIntent(this, action)
        return NotificationCompat.Action(icon, title, intent)
    }
}
```

- [ ] **Step 2: Compilar el módulo**

Run: `./gradlew :core:tts:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. (Si falla por `android.support.v4.media.*`, confirma que la dependencia `androidx.media` de la Task 1 está presente: esas clases vienen de ahí.)

- [ ] **Step 3: Commit**

```bash
git add core/tts/src/main/java/com/ebookreader/core/tts/service/TtsPlaybackService.kt
git commit -m "feat: integrar MediaSessionCompat (metadata, foco, botones de capítulo)"
```

---

### Task 6: `AudioPlayerViewModel` informa del libro

**Files:**
- Modify: `feature/audioplayer/src/main/java/com/ebookreader/feature/audioplayer/AudioPlayerViewModel.kt:66-81`

- [ ] **Step 1: Llamar a `setBookInfo` antes de cargar el texto**

En `AudioPlayerViewModel.loadBook()`, justo después de `_uiState.update { it.copy(book = book) }` (línea ~67) y antes de construir el `parser`, añade:

```kotlin
            ttsController.setBookInfo(book.title, book.author, book.coverPath)
```

- [ ] **Step 2: Verificar que compila**

Run: `./gradlew :feature:audioplayer:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add feature/audioplayer/src/main/java/com/ebookreader/feature/audioplayer/AudioPlayerViewModel.kt
git commit -m "feat: pasar datos del libro al TtsController para la metadata"
```

---

### Task 7: Manifest (MediaButtonReceiver) + build + verificación en dispositivo

**Files:**
- Modify: `app/src/main/AndroidManifest.xml:50-54`

- [ ] **Step 1: Declarar el `MediaButtonReceiver` y el intent-filter en el servicio**

En `app/src/main/AndroidManifest.xml`, sustituye el elemento `<service .../>` actual (autocerrado) por la versión con `intent-filter` y añade el receiver justo debajo:

```xml
        <service
            android:name="com.ebookreader.core.tts.service.TtsPlaybackService"
            android:exported="false"
            android:foregroundServiceType="mediaPlayback">
            <intent-filter>
                <action android:name="android.intent.action.MEDIA_BUTTON" />
            </intent-filter>
        </service>

        <receiver
            android:name="androidx.media.session.MediaButtonReceiver"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MEDIA_BUTTON" />
            </intent-filter>
        </receiver>
```

- [ ] **Step 2: Compilar el APK debug completo**

Run (con `JAVA_HOME` y `ANDROID_HOME` configurados, ver handoff):
`./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Ejecutar toda la batería de tests del módulo tts**

Run: `./gradlew :core:tts:testDebugUnitTest`
Expected: PASS (incluye `NowPlayingMetadataTest`, `AudioFocusControllerTest`, `PlaybackWakeLockControllerTest`).

- [ ] **Step 4: Instalar y verificar manualmente en el dispositivo**

Run: `adb install -r app/build/outputs/apk/debug/app-debug.apk`

Verificación manual (checklist):
- [ ] Abrir un libro, pulsar play: aparece notificación con portada + título del libro + capítulo.
- [ ] Pantalla de bloqueo: muestra portada y metadata; botones play/pause/anterior/siguiente visibles.
- [ ] Botón siguiente/anterior (notificación y lockscreen): salta de **capítulo**.
- [ ] Auriculares bluetooth: el botón play/pause del manos libres pausa y reanuda.
- [ ] Entra una llamada o suena otra app (p. ej. YouTube): la narración se pausa; al terminar, reanuda.
- [ ] Botón stop / deslizar la notificación: detiene la narración y quita el foreground.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/AndroidManifest.xml
git commit -m "feat: declarar MediaButtonReceiver para controles de hardware"
```

---

## Self-review (cobertura del spec)

- MediaSessionCompat (no media3) → Task 1 (dep) + Task 5 (sesión). ✓
- Botones externos = capítulo → Task 5 (`onSkipToNext/Previous` → `nextChapter/previousChapter`). ✓
- Metadata portada + libro + capítulo → Task 2 (modelo) + Task 3 (flow) + Task 5 (`updateMetadata`, `loadCover`) + Task 6 (datos). ✓
- Foco de audio pausar/reanudar (sin ducking) → Task 4 (`AudioFocusController`) + Task 5 (cableado). ✓
- Botones de hardware bluetooth → Task 5 (`MediaButtonReceiver.handleIntent`) + Task 7 (manifest). ✓
- Notificación MediaStyle → Task 5 (`buildNotification` con `MediaStyle`). ✓
- Error handling (cover nula, servicios nulos) → Task 5 (`loadCover` con `runCatching`, `createAudioFocusController`/`createWakeLockController` devuelven null con gracia). ✓
- Testing patrón wakelock → Task 2 y Task 4 (tests puros, sin Robolectric). ✓
- Degradación si `AudioManager`/`PowerManager` nulos → Task 5. ✓
