# Persistencia de posición de lectura — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Recordar por dónde iba la lectura (frase del TTS y página del visor) guardándola conforme avanza —incluso en segundo plano—, y restaurarla al reabrir dejando el TTS posicionado en esa frase sin reproducir.

**Architecture:** Se añaden campos a `BookEntity` (`lastChapter`, `lastSegment`, `lastPage`) con migración Room aditiva. El `TtsControllerImpl` (singleton, vivo en segundo plano vía el foreground service) persiste su posición de audio en cada cambio de frase usando `BookRepository` directamente. El visor persiste la página al pasarla a mano. Visor y reproductor restauran la posición al abrir con un nuevo `TtsController.jumpToSegment`.

**Tech Stack:** Kotlin, Room, Hilt, Jetpack Compose, Android WebView.

---

## File Structure

- `core/data/.../entity/BookEntity.kt` — campos nuevos.
- `core/data/.../db/AppDatabase.kt` — version 2.
- `core/data/.../db/Migrations.kt` — NUEVO: `MIGRATION_1_2`.
- `core/data/.../di/DataModule.kt` — `addMigrations`.
- `core/data/.../dao/BookDao.kt` — queries de posición.
- `core/data/.../repository/BookRepository.kt` + `BookRepositoryImpl.kt` — `saveAudioPosition` / `saveVisualPosition`.
- `core/data/src/test/.../BookRepositoryImplTest.kt` — tests de los métodos nuevos.
- `core/tts/.../controller/TtsController.kt` + `TtsControllerImpl.kt` — `jumpToSegment`, `setBookInfo(bookId,...)`, persistencia.
- `feature/reader/.../ReaderViewModel.kt` — restaurar posición; persistir página.
- `feature/reader/.../epub/EpubReaderView.kt` — `onPageChanged`, `initialPage`.
- `feature/reader/.../ReaderScreen.kt` — pasar los nuevos params.
- `feature/audioplayer/.../AudioPlayerViewModel.kt` — restaurar posición.

---

## Task 1: Campos nuevos en `BookEntity` + migración Room

**Files:**
- Modify: `core/data/src/main/java/com/ebookreader/core/data/db/entity/BookEntity.kt`
- Modify: `core/data/src/main/java/com/ebookreader/core/data/db/AppDatabase.kt`
- Create: `core/data/src/main/java/com/ebookreader/core/data/db/Migrations.kt`
- Modify: `core/data/src/main/java/com/ebookreader/core/data/di/DataModule.kt`

- [ ] **Step 1: Añadir campos a `BookEntity`**

Reemplazar la data class por:

```kotlin
@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val author: String,
    val coverPath: String? = null,
    val filePath: String,
    val format: BookFormat,
    val progress: Float = 0f,
    val lastPosition: String = "",
    val lastChapter: Int = 0,
    val lastSegment: Int = 0,
    val lastPage: Int = 0,
    val lastAccess: Long = System.currentTimeMillis(),
    val addedAt: Long = System.currentTimeMillis()
)
```

- [ ] **Step 2: Crear la migración**

Crear `core/data/src/main/java/com/ebookreader/core/data/db/Migrations.kt`:

```kotlin
package com.ebookreader.core.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 -> v2: añade lastChapter, lastSegment, lastPage a books.
 * Inicializa lastChapter desde el lastPosition existente cuando es numérico.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE books ADD COLUMN lastChapter INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE books ADD COLUMN lastSegment INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE books ADD COLUMN lastPage INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE books SET lastChapter = CAST(lastPosition AS INTEGER) WHERE lastPosition GLOB '[0-9]*'")
    }
}
```

- [ ] **Step 3: Subir la versión de la base de datos**

En `AppDatabase.kt`, cambiar `version = 1` por `version = 2`:

```kotlin
@Database(
    entities = [BookEntity::class, BookmarkEntity::class, TtsCacheEntity::class],
    version = 2,
    exportSchema = false
)
```

- [ ] **Step 4: Registrar la migración en el builder**

En `DataModule.kt`, en `provideAppDatabase`, importar la migración y añadir `.addMigrations`:

```kotlin
import com.ebookreader.core.data.db.MIGRATION_1_2
```

```kotlin
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "ebook_reader.db"
    ).addMigrations(MIGRATION_1_2).build()
```

- [ ] **Step 5: Compilar**

Run: `.\gradlew.bat :core:data:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add core/data/src/main/java/com/ebookreader/core/data/db/entity/BookEntity.kt core/data/src/main/java/com/ebookreader/core/data/db/AppDatabase.kt core/data/src/main/java/com/ebookreader/core/data/db/Migrations.kt core/data/src/main/java/com/ebookreader/core/data/di/DataModule.kt
git commit -m "feat: campos de posicion (lastChapter/lastSegment/lastPage) y migracion 1-2"
```

---

## Task 2: DAO + Repository — guardar posición de audio y visual (TDD)

**Files:**
- Modify: `core/data/src/main/java/com/ebookreader/core/data/db/dao/BookDao.kt`
- Modify: `core/data/src/main/java/com/ebookreader/core/data/repository/BookRepository.kt`
- Modify: `core/data/src/main/java/com/ebookreader/core/data/repository/BookRepositoryImpl.kt`
- Test: `core/data/src/test/java/com/ebookreader/core/data/repository/BookRepositoryImplTest.kt`

- [ ] **Step 1: Añadir queries al DAO**

En `BookDao.kt`, tras `updateProgress`, añadir:

```kotlin
    @Query("UPDATE books SET lastChapter = :chapter, lastSegment = :segment, lastPosition = :chapterStr, progress = :progress, lastAccess = :timestamp WHERE id = :id")
    suspend fun updateAudioPosition(id: Long, chapter: Int, segment: Int, chapterStr: String, progress: Float, timestamp: Long)

    @Query("UPDATE books SET lastChapter = :chapter, lastPage = :page, lastPosition = :chapterStr, lastAccess = :timestamp WHERE id = :id")
    suspend fun updateVisualPosition(id: Long, chapter: Int, page: Int, chapterStr: String, timestamp: Long)
```

- [ ] **Step 2: Añadir métodos a la interfaz `BookRepository`**

Tras `updateProgress`:

```kotlin
    suspend fun saveAudioPosition(id: Long, chapter: Int, segment: Int, progress: Float)

    suspend fun saveVisualPosition(id: Long, chapter: Int, page: Int)
```

- [ ] **Step 3: Escribir el test que falla**

En `BookRepositoryImplTest.kt`, añadir (dentro de la clase de test existente; usa el
mismo estilo/mocks que los tests ya presentes, con MockK):

```kotlin
    @Test
    fun `saveAudioPosition delega en updateAudioPosition con el capitulo como string`() = runTest {
        val dao = mockk<BookDao>(relaxed = true)
        val repo = BookRepositoryImpl(dao)

        repo.saveAudioPosition(id = 7, chapter = 3, segment = 42, progress = 0.5f)

        coVerify { dao.updateAudioPosition(7, 3, 42, "3", 0.5f, any()) }
    }

    @Test
    fun `saveVisualPosition delega en updateVisualPosition con el capitulo como string`() = runTest {
        val dao = mockk<BookDao>(relaxed = true)
        val repo = BookRepositoryImpl(dao)

        repo.saveVisualPosition(id = 7, chapter = 2, page = 5)

        coVerify { dao.updateVisualPosition(7, 2, 5, "2", any()) }
    }
```

Asegúrate de que el archivo importa lo necesario: `io.mockk.mockk`,
`io.mockk.coVerify`, `kotlinx.coroutines.test.runTest`, `org.junit.Test`,
`com.ebookreader.core.data.db.dao.BookDao`. (Reutiliza los imports ya presentes y
añade solo los que falten.)

- [ ] **Step 4: Ejecutar el test para verlo fallar**

Run: `.\gradlew.bat :core:data:testDebugUnitTest --tests "*BookRepositoryImplTest*"`
Expected: FAIL — `saveAudioPosition` / `saveVisualPosition` no existen.

- [ ] **Step 5: Implementar en `BookRepositoryImpl`**

Tras `updateProgress`:

```kotlin
    override suspend fun saveAudioPosition(id: Long, chapter: Int, segment: Int, progress: Float) =
        bookDao.updateAudioPosition(id, chapter, segment, chapter.toString(), progress, System.currentTimeMillis())

    override suspend fun saveVisualPosition(id: Long, chapter: Int, page: Int) =
        bookDao.updateVisualPosition(id, chapter, page, chapter.toString(), System.currentTimeMillis())
```

- [ ] **Step 6: Ejecutar el test para verlo pasar**

Run: `.\gradlew.bat :core:data:testDebugUnitTest --tests "*BookRepositoryImplTest*"`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add core/data/src/main/java/com/ebookreader/core/data/db/dao/BookDao.kt core/data/src/main/java/com/ebookreader/core/data/repository/BookRepository.kt core/data/src/main/java/com/ebookreader/core/data/repository/BookRepositoryImpl.kt core/data/src/test/java/com/ebookreader/core/data/repository/BookRepositoryImplTest.kt
git commit -m "feat: saveAudioPosition y saveVisualPosition en el repositorio"
```

---

## Task 3: `TtsController` — `jumpToSegment`, `setBookInfo(bookId)` y persistencia

**Files:**
- Modify: `core/tts/src/main/java/com/ebookreader/core/tts/controller/TtsController.kt`
- Modify: `core/tts/src/main/java/com/ebookreader/core/tts/controller/TtsControllerImpl.kt`

- [ ] **Step 1: Actualizar la interfaz `TtsController`**

Cambiar la firma de `setBookInfo` y añadir `jumpToSegment`:

```kotlin
    /** Datos del libro en reproducción (para lockscreen / coche y persistencia). */
    fun setBookInfo(bookId: Long, title: String, author: String, coverPath: String?)

    /** Posiciona la lectura en el segmento global indicado SIN reproducir. */
    suspend fun jumpToSegment(index: Int)
```

- [ ] **Step 2: Inyectar `BookRepository` y guardar el `bookId`**

En `TtsControllerImpl.kt`, añadir el import y el parámetro de constructor:

```kotlin
import com.ebookreader.core.data.repository.BookRepository
```

```kotlin
class TtsControllerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val localEngine: LocalTtsEngine,
    private val cloudEngine: CloudTtsEngine,
    private val userPreferences: UserPreferences,
    private val bookRepository: BookRepository
) : TtsController {
```

Añadir un campo para el bookId, junto a `bookTitle`/`bookAuthor` (≈L45-47):

```kotlin
    private var bookId: Long? = null
```

- [ ] **Step 3: Persistir la posición en cada cambio de frase**

En el bloque `init`, junto al observador `state.onEach { refreshNowPlaying() }`,
añadir un observador que persista la posición de audio cuando cambie el segmento
y haya libro activo. Necesita los imports `kotlinx.coroutines.flow.distinctUntilChanged`
y `kotlinx.coroutines.flow.map` (añádelos arriba):

```kotlin
        // Persiste la posición de audio (capítulo + frase) cada vez que avanza
        // el segmento, también en segundo plano (el controller es singleton).
        state
            .map { it.currentSegmentIndex to it.currentChapterIndex }
            .distinctUntilChanged()
            .onEach { (segment, chapter) ->
                val id = bookId
                if (id != null && segments.isNotEmpty()) {
                    val progress = if (segments.isNotEmpty()) {
                        segment.toFloat() / segments.size
                    } else 0f
                    runCatching { bookRepository.saveAudioPosition(id, chapter, segment, progress) }
                }
            }
            .launchIn(scope)
```

- [ ] **Step 4: Actualizar `setBookInfo` para recibir el bookId**

Reemplazar la implementación de `setBookInfo`:

```kotlin
    override fun setBookInfo(bookId: Long, title: String, author: String, coverPath: String?) {
        this.bookId = bookId
        bookTitle = title
        bookAuthor = author
        bookCoverPath = coverPath
        refreshNowPlaying()
    }
```

- [ ] **Step 5: Implementar `jumpToSegment`**

Añadir (junto a `jumpToChapter`):

```kotlin
    override suspend fun jumpToSegment(index: Int) {
        if (segments.isEmpty()) return
        val safe = index.coerceIn(0, segments.lastIndex)
        val segment = segments[safe]
        _state.update {
            it.copy(
                currentSegmentIndex = safe,
                currentChapterIndex = segment.chapterIndex
            )
        }
        _currentSegment.value = segment
    }
```

- [ ] **Step 6: Compilar**

Run: `.\gradlew.bat :core:tts:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (habrá errores luego en los módulos que llaman a
`setBookInfo` con la firma vieja — se arreglan en las tareas 4 y 6).

- [ ] **Step 7: Commit**

```bash
git add core/tts/src/main/java/com/ebookreader/core/tts/controller/TtsController.kt core/tts/src/main/java/com/ebookreader/core/tts/controller/TtsControllerImpl.kt
git commit -m "feat: TtsController persiste posicion de audio, jumpToSegment y bookId"
```

---

## Task 4: Restaurar posición y persistir página en `ReaderViewModel`

**Files:**
- Modify: `feature/reader/src/main/java/com/ebookreader/feature/reader/ReaderViewModel.kt`

- [ ] **Step 1: Restaurar la posición en `loadBook` (orden correcto)**

En `loadBook()`, el bloque actual (≈L95-101) es:

```kotlin
                ttsController.loadText(chapters)

                // Informa al controller de los datos del libro para que la
                // notificación de controles multimedia (lockscreen, bluetooth)
                // muestre título, autor y carátula al reproducir desde el visor,
                // igual que ya hace el reproductor de audio.
                ttsController.setBookInfo(book.title, book.author, book.coverPath)
```

Reemplazarlo por (posicionar ANTES de setBookInfo, para que el `bookId` no se
active hasta que la posición esté restaurada):

```kotlin
                ttsController.loadText(chapters)

                // Restaura la frase donde se quedó la lectura (sin reproducir).
                ttsController.jumpToSegment(book.lastSegment)

                // Informa al controller de los datos del libro (metadata de
                // controles multimedia) y activa la persistencia con el bookId.
                ttsController.setBookInfo(book.id, book.title, book.author, book.coverPath)
```

- [ ] **Step 2: Restaurar el capítulo visible desde `lastChapter`**

En el mismo `loadBook`, el cálculo de `initialChapter` (≈L116) usa
`book.lastPosition.toIntOrNull()`. Cambiarlo para usar el campo dedicado:

```kotlin
                    val saved = book.lastChapter
                    initialChapter = if (chapterFiles.isEmpty()) {
                        0
                    } else {
                        saved.coerceIn(0, chapterFiles.lastIndex)
                    }
```

- [ ] **Step 3: Exponer la página inicial en el uiState y persistir página**

Añadir un campo `initialPage` al `ReaderUiState` (tras `currentChapterIndex`):

```kotlin
    val currentChapterIndex: Int = 0,
    val initialPage: Int = 0,
```

En el `_uiState.update` final de `loadBook` (el que fija `currentChapterIndex = initialChapter`),
añadir `initialPage = book.lastPage`:

```kotlin
                        currentChapterIndex = initialChapter,
                        initialPage = book.lastPage,
```

Añadir un método para persistir la página visual (junto a `persistChapterPosition`):

```kotlin
    fun onVisualPageChanged(page: Int) {
        val state = _uiState.value
        viewModelScope.launch {
            bookRepository.saveVisualPosition(bookId, state.currentChapterIndex, page)
        }
    }
```

- [ ] **Step 4: Compilar**

Run: `.\gradlew.bat :feature:reader:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add feature/reader/src/main/java/com/ebookreader/feature/reader/ReaderViewModel.kt
git commit -m "feat: restaurar frase/capitulo/pagina al abrir y persistir pagina visual"
```

---

## Task 5: `EpubReaderView` — exponer cambio de página y página inicial

**Files:**
- Modify: `feature/reader/src/main/java/com/ebookreader/feature/reader/epub/EpubReaderView.kt`
- Modify: `feature/reader/src/main/java/com/ebookreader/feature/reader/ReaderScreen.kt`

- [ ] **Step 1: Añadir parámetros `initialPage` y `onPageChanged`**

En la firma de `EpubReaderView` (tras `currentTtsSegment`):

```kotlin
    currentTtsSegment: TextSegment?,
    initialPage: Int,
    onPageChanged: (Int) -> Unit,
    onPreviousChapter: () -> Unit,
```

- [ ] **Step 2: Ir a la página inicial al cargar el capítulo**

En `onPageFinished`, dentro del callback de `resetScript` y DESPUÉS del bloque
`if (goToLastOnLoad.value) { ... }`, añadir el salto a la página guardada (solo si
es > 0; se lee desde un holder para evitar stale closures). Primero, junto a los
otros holders (tras `styleHolder.value = styleScript`), añadir:

```kotlin
    val initialPageHolder = remember { mutableStateOf(0) }
    initialPageHolder.value = initialPage
```

Y en el callback de `resetScript`, tras el bloque de `goToLastOnLoad`:

```kotlin
                                    val startPage = initialPageHolder.value
                                    if (startPage > 0) {
                                        view.evaluateJavascript(
                                            "javascript:(function(){ window.__recalc(); window.__currentPage = Math.min($startPage, window.__totalPages - 1); window.scrollTo(window.__currentPage * window.innerWidth, 0); })();",
                                            null
                                        )
                                    }
```

- [ ] **Step 3: Notificar el cambio de página en los taps laterales**

En los handlers de tap de página (zona izquierda y derecha), tras el
`evaluateJavascript` que ejecuta `__prevPage` / `__nextPage`, leer la página actual
y notificarla. En la zona PREV, dentro del callback `{ result -> ... }` del
`evaluateJavascript`, añadir (al final del bloque, tras la lógica de
`goToLastOnLoad`):

```kotlin
                                        wv.evaluateJavascript("(function(){ return window.__currentPage; })();") { p ->
                                            p?.toIntOrNull()?.let { latestOnPageChanged(it) }
                                        }
```

En la zona NEXT, igual, dentro de su callback `{ result -> ... }`:

```kotlin
                                        wv.evaluateJavascript("(function(){ return window.__currentPage; })();") { p ->
                                            p?.toIntOrNull()?.let { latestOnPageChanged(it) }
                                        }
```

Y añadir el `rememberUpdatedState` para `onPageChanged` junto a los otros
(`latestOnPreviousChapter`, etc., ≈L191):

```kotlin
    val latestOnPageChanged by rememberUpdatedState(onPageChanged)
```

- [ ] **Step 4: Pasar los nuevos parámetros desde `ReaderScreen`**

En `ReaderScreen.kt`, en la llamada a `EpubReaderView` (≈L154), añadir tras
`currentTtsSegment = currentSegment,`:

```kotlin
                                currentTtsSegment = currentSegment,
                                initialPage = uiState.initialPage,
                                onPageChanged = viewModel::onVisualPageChanged,
                                onPreviousChapter = viewModel::previousChapter,
```

- [ ] **Step 5: Compilar la app entera**

Run: `.\gradlew.bat :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add feature/reader/src/main/java/com/ebookreader/feature/reader/epub/EpubReaderView.kt feature/reader/src/main/java/com/ebookreader/feature/reader/ReaderScreen.kt
git commit -m "feat: visor restaura pagina inicial y notifica cambios de pagina"
```

---

## Task 6: Restaurar posición en `AudioPlayerViewModel`

**Files:**
- Modify: `feature/audioplayer/src/main/java/com/ebookreader/feature/audioplayer/AudioPlayerViewModel.kt`

- [ ] **Step 1: Restaurar la frase y pasar el bookId**

En `loadBook()`, el bloque actual (≈L78-91) es:

```kotlin
            ttsController.setBookInfo(book.title, book.author, book.coverPath)

            // Sincroniza el TTS con la última posición guardada del libro,
            // para que el reproductor empiece en el capítulo donde el usuario
            // estaba leyendo en el visor, no desde el principio.
            val savedChapter = book.lastPosition.toIntOrNull() ?: 0
            if (savedChapter > 0) {
                ttsController.jumpToChapter(savedChapter)
            }
```

Reemplazarlo por (posicionar en la frase exacta antes de activar la persistencia):

```kotlin
            // Restaura la frase exacta donde se quedó la lectura (sin reproducir).
            ttsController.jumpToSegment(book.lastSegment)

            // Informa al controller de los datos del libro y activa la
            // persistencia con el bookId.
            ttsController.setBookInfo(book.id, book.title, book.author, book.coverPath)
```

- [ ] **Step 2: Compilar la app entera**

Run: `.\gradlew.bat :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add feature/audioplayer/src/main/java/com/ebookreader/feature/audioplayer/AudioPlayerViewModel.kt
git commit -m "feat: el reproductor restaura la frase exacta al abrir"
```

---

## Task 7: Verificación en dispositivo

- [ ] **Step 1: Instalar (verifica la migración con datos existentes)**

Run: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
Expected: Success. La app abre sin crash (la migración 1→2 conserva la biblioteca).

- [ ] **Step 2: Validar persistencia de audio**

- Abrir un libro, pulsar play, dejar leer varias frases.
- Cerrar la app por completo (deslizar de recientes); probar también con la pantalla
  apagada un rato antes de cerrar.
- Reabrir el libro → pulsar play: debe reanudar en (o muy cerca de) la frase donde
  se quedó, NO desde el principio del capítulo.

- [ ] **Step 3: Validar persistencia visual**

- Sin audio, pasar varias páginas a mano (taps laterales) dentro de un capítulo.
- Cerrar y reabrir → el visor abre en esa página (con la misma fuente).

- [ ] **Step 4: Validar capítulo distinto del primero**

- Saltar a un capítulo avanzado por el índice, leer, cerrar y reabrir → reabre en
  ese capítulo, no en el primero.

---

## Self-Review (completado durante la escritura)

- **Cobertura del spec:** campos `lastChapter/lastSegment/lastPage` + migración
  (Task 1); `saveAudioPosition`/`saveVisualPosition` (Task 2); persistencia de audio
  en background desde el `TtsController` + `jumpToSegment` + `setBookInfo(bookId)`
  (Task 3); restaurar y persistir página en el visor (Task 4/5); restaurar en el
  reproductor (Task 6); `lastPosition` mantenido como espejo del capítulo (queries
  de Task 2). Posicionar-sin-reproducir: `jumpToSegment` no toca `isPlaying`. ✔
- **Sin placeholders:** todo el código está completo. ✔
- **Consistencia de tipos:** `saveAudioPosition(id, chapter, segment, progress)`,
  `saveVisualPosition(id, chapter, page)`, `setBookInfo(bookId, title, author, coverPath)`,
  `jumpToSegment(index)`, `onVisualPageChanged(page)`, `onPageChanged: (Int) -> Unit`,
  `initialPage: Int` — coherentes entre tareas. ✔
- **Desvío respecto al spec (intencionado):** se inyecta `BookRepository` directo en
  el `TtsController` en vez de una interfaz `ReadingProgressStore`, para evitar el
  ciclo de dependencias `core/tts ↔ core/data` (core/tts ya depende de core/data).
- **Orden anti-sobrescritura:** en la restauración, `jumpToSegment` va ANTES de
  `setBookInfo` (que activa el `bookId`), de modo que el reposicionamiento inicial no
  persiste un 0 transitorio. ✔
