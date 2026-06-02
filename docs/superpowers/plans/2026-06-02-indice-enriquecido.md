# Índice enriquecido — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Hacer el índice del visor navegable y útil: cada entrada muestra un preview del contenido, las entradas numéricas se prefijan con "Capítulo N", el encabezado se traduce a español y al pulsar una entrada se abre el capítulo correcto.

**Architecture:** Una función pura `TocEnricher` en `core/book` combina el `TableOfContents` (estructura del NCX/Nav) con el `BookContent` (texto por capítulo) mapeando por `href`, y produce un `TableOfContents` enriquecido (preview, título de visualización, índice de capítulo resuelto). El `ReaderViewModel` la invoca tras cargar el libro; el `TableOfContentsDrawer` solo renderiza. La lógica vive en el módulo con infraestructura de test (`core/book`), no en `feature/reader` (sin tests).

**Tech Stack:** Kotlin, JUnit, Jetpack Compose (Material3).

---

## File Structure

- `core/book/.../model/BookContent.kt` — añadir `href` a `Chapter`.
- `core/book/.../model/TableOfContents.kt` — añadir `preview` y `chapterIndex` a `TocEntry`.
- `core/book/.../parser/EpubParser.kt` — poblar `href` en `extractTextContent`.
- `core/book/.../parser/PdfParser.kt` — poblar `href` (vacío) para que compile.
- `core/book/.../toc/TocEnricher.kt` — NUEVO. Función pura de enriquecimiento (testeable).
- `core/book/src/test/.../toc/TocEnricherTest.kt` — NUEVO. Tests.
- `feature/reader/.../ReaderViewModel.kt` — invocar `TocEnricher`; navegar por índice resuelto.
- `feature/reader/.../components/TableOfContentsDrawer.kt` — render preview, header ES, navegación por `entry.chapterIndex`.

---

## Task 1: Añadir `href` a `Chapter` y poblarlo en los parsers

**Files:**
- Modify: `core/book/src/main/java/com/ebookreader/core/book/model/BookContent.kt`
- Modify: `core/book/src/main/java/com/ebookreader/core/book/parser/EpubParser.kt:61-65`
- Modify: `core/book/src/main/java/com/ebookreader/core/book/parser/PdfParser.kt:48-53`

- [ ] **Step 1: Añadir el campo `href` a `Chapter`**

En `BookContent.kt`, reemplazar la data class `Chapter`:

```kotlin
data class Chapter(
    val index: Int,
    val href: String,
    val title: String,
    val textContent: String
)
```

- [ ] **Step 2: Poblar `href` en `EpubParser.extractTextContent`**

En `EpubParser.kt`, el bloque que construye cada `Chapter` (≈L61) pasa de:

```kotlin
        Chapter(
            index = index,
            title = item.title.ifBlank { "Chapter ${index + 1}" },
            textContent = text
        )
```

a:

```kotlin
        Chapter(
            index = index,
            href = item.href,
            title = item.title.ifBlank { "Chapter ${index + 1}" },
            textContent = text
        )
```

- [ ] **Step 3: Poblar `href` en `PdfParser`**

En `PdfParser.kt` (≈L48), el PDF no tiene href; usar cadena vacía:

```kotlin
                        chapters.add(
                            Chapter(
                                index = i,
                                href = "",
                                title = "Page ${i + 1}",
                                textContent = "[Page ${i + 1} of $pageCount — visual content only]"
                            )
                        )
```

- [ ] **Step 4: Compilar el módulo core:book**

Run: `./gradlew.bat :core:book:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add core/book/src/main/java/com/ebookreader/core/book/model/BookContent.kt core/book/src/main/java/com/ebookreader/core/book/parser/EpubParser.kt core/book/src/main/java/com/ebookreader/core/book/parser/PdfParser.kt
git commit -m "feat: exponer href en Chapter para mapear ToC con contenido"
```

---

## Task 2: Extender `TocEntry` con `preview` y `chapterIndex`

**Files:**
- Modify: `core/book/src/main/java/com/ebookreader/core/book/model/TableOfContents.kt`

- [ ] **Step 1: Añadir campos a `TocEntry`**

Reemplazar la data class `TocEntry`:

```kotlin
data class TocEntry(
    val title: String,
    val href: String,
    val preview: String = "",
    val chapterIndex: Int = -1,
    val children: List<TocEntry> = emptyList()
)
```

`chapterIndex = -1` significa "no resuelto / no navegable". Los campos tienen
default para no romper los call-sites del parser (que no los pasan).

- [ ] **Step 2: Compilar core:book**

Run: `./gradlew.bat :core:book:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add core/book/src/main/java/com/ebookreader/core/book/model/TableOfContents.kt
git commit -m "feat: añadir preview y chapterIndex a TocEntry"
```

---

## Task 3: `TocEnricher` — función pura (TDD)

Combina `TableOfContents` + `BookContent`: resuelve cada entrada a su capítulo por
`href` (ignorando fragmento `#`), genera el preview, prefija "Capítulo N" a títulos
numéricos y asigna el índice de capítulo. Recursivo sobre `children`.

**Files:**
- Create: `core/book/src/main/java/com/ebookreader/core/book/toc/TocEnricher.kt`
- Test: `core/book/src/test/java/com/ebookreader/core/book/toc/TocEnricherTest.kt`

- [ ] **Step 1: Escribir los tests que fallan**

Crear `TocEnricherTest.kt`:

```kotlin
package com.ebookreader.core.book.toc

import com.ebookreader.core.book.model.BookContent
import com.ebookreader.core.book.model.Chapter
import com.ebookreader.core.book.model.TableOfContents
import com.ebookreader.core.book.model.TocEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class TocEnricherTest {

    private fun content(vararg ch: Chapter) = BookContent(ch.toList())

    @Test
    fun `resuelve chapterIndex por href ignorando fragmento`() {
        val toc = TableOfContents(listOf(TocEntry(title = "1", href = "OPS/c1.xhtml#top")))
        val book = content(Chapter(index = 5, href = "OPS/c1.xhtml", title = "1", textContent = "Hola mundo"))

        val result = TocEnricher.enrich(toc, book)

        assertEquals(5, result.entries[0].chapterIndex)
    }

    @Test
    fun `genera preview con las primeras palabras saltando el numero inicial`() {
        val toc = TableOfContents(listOf(TocEntry(title = "1", href = "c1.xhtml")))
        val book = content(Chapter(index = 0, href = "c1.xhtml", title = "1", textContent = "1 1 REG. 18/10/2018 transcripción de audio del detective"))

        val result = TocEnricher.enrich(toc, book)

        assertEquals("REG. 18/10/2018 transcripción de audio del detective", result.entries[0].preview)
    }

    @Test
    fun `corta el preview en limite de palabra sin pasar de 90 caracteres`() {
        val long = (1..40).joinToString(" ") { "palabra$it" }
        val toc = TableOfContents(listOf(TocEntry(title = "1", href = "c1.xhtml")))
        val book = content(Chapter(index = 0, href = "c1.xhtml", title = "1", textContent = long))

        val result = TocEnricher.enrich(toc, book)
        val preview = result.entries[0].preview

        assert(preview.length <= 91) { "preview demasiado largo: ${preview.length}" }
        assert(preview.endsWith("…")) { "debería terminar en elipsis: $preview" }
        assert(!preview.contains("palabr ")) { "no debe cortar a media palabra" }
    }

    @Test
    fun `prefija Capitulo a titulos puramente numericos`() {
        val toc = TableOfContents(listOf(TocEntry(title = "  3 ", href = "c.xhtml")))
        val book = content(Chapter(index = 0, href = "c.xhtml", title = "3", textContent = "texto"))

        val result = TocEnricher.enrich(toc, book)

        assertEquals("Capítulo 3", result.entries[0].title)
    }

    @Test
    fun `no prefija titulos no numericos`() {
        val toc = TableOfContents(listOf(TocEntry(title = "ENTONCES", href = "c.xhtml")))
        val book = content(Chapter(index = 0, href = "c.xhtml", title = "", textContent = "texto"))

        val result = TocEnricher.enrich(toc, book)

        assertEquals("ENTONCES", result.entries[0].title)
    }

    @Test
    fun `entrada sin capitulo correspondiente queda con chapterIndex -1 y sin preview`() {
        val toc = TableOfContents(listOf(TocEntry(title = "Huérfana", href = "noexiste.xhtml")))
        val book = content(Chapter(index = 0, href = "c.xhtml", title = "x", textContent = "texto"))

        val result = TocEnricher.enrich(toc, book)

        assertEquals(-1, result.entries[0].chapterIndex)
        assertEquals("", result.entries[0].preview)
    }

    @Test
    fun `enriquece recursivamente los children`() {
        val toc = TableOfContents(listOf(
            TocEntry(title = "ENTONCES", href = "part.xhtml", children = listOf(
                TocEntry(title = "1", href = "c1.xhtml")
            ))
        ))
        val book = content(
            Chapter(index = 0, href = "part.xhtml", title = "ENTONCES", textContent = "ENTONCES"),
            Chapter(index = 1, href = "c1.xhtml", title = "1", textContent = "1 Texto del capítulo uno")
        )

        val result = TocEnricher.enrich(toc, book)
        val child = result.entries[0].children[0]

        assertEquals(1, child.chapterIndex)
        assertEquals("Capítulo 1", child.title)
        assertEquals("Texto del capítulo uno", child.preview)
    }
}
```

- [ ] **Step 2: Ejecutar los tests para verlos fallar**

Run: `./gradlew.bat :core:book:testDebugUnitTest --tests "*TocEnricherTest*"`
Expected: FAIL — `TocEnricher` no existe (error de compilación / unresolved reference).

- [ ] **Step 3: Implementar `TocEnricher`**

Crear `TocEnricher.kt`:

```kotlin
package com.ebookreader.core.book.toc

import com.ebookreader.core.book.model.BookContent
import com.ebookreader.core.book.model.Chapter
import com.ebookreader.core.book.model.TableOfContents
import com.ebookreader.core.book.model.TocEntry

/**
 * Enriquece un [TableOfContents] crudo (estructura del NCX/Nav) con datos
 * derivados del [BookContent]: índice de capítulo resuelto por href, preview
 * del contenido y título de visualización ("Capítulo N" para entradas numéricas).
 * Función pura, sin dependencias de Android — testeable en JVM.
 */
object TocEnricher {

    private const val PREVIEW_MAX = 90

    fun enrich(toc: TableOfContents, content: BookContent): TableOfContents {
        val byHref: Map<String, Chapter> = content.chapters.associateBy { it.href }
        return TableOfContents(toc.entries.map { enrichEntry(it, byHref) })
    }

    private fun enrichEntry(entry: TocEntry, byHref: Map<String, Chapter>): TocEntry {
        val key = entry.href.substringBefore('#')
        val chapter = byHref[key]
        return entry.copy(
            title = displayTitle(entry.title),
            preview = chapter?.let { buildPreview(it.textContent) } ?: "",
            chapterIndex = chapter?.index ?: -1,
            children = entry.children.map { enrichEntry(it, byHref) }
        )
    }

    /** Prefija "Capítulo N" cuando el título es solo un número. */
    private fun displayTitle(raw: String): String {
        val t = raw.trim()
        return if (t.isNotEmpty() && t.all { it.isDigit() }) "Capítulo $t" else t
    }

    /**
     * Primeras palabras del capítulo, saltando los números/espacios iniciales
     * (que suelen ser el propio número de capítulo repetido), cortando en límite
     * de palabra y añadiendo elipsis si se trunca.
     */
    private fun buildPreview(textContent: String): String {
        // Colapsa espacios y elimina el prefijo numérico inicial ("1 1 REG..." -> "REG...").
        val collapsed = textContent.trim().replace(Regex("\\s+"), " ")
        val body = collapsed.replace(Regex("^(\\d+\\s+)+"), "")
        if (body.length <= PREVIEW_MAX) return body
        val cut = body.substring(0, PREVIEW_MAX)
        val lastSpace = cut.lastIndexOf(' ')
        val trimmed = if (lastSpace > 0) cut.substring(0, lastSpace) else cut
        return "$trimmed…"
    }
}
```

- [ ] **Step 4: Ejecutar los tests para verlos pasar**

Run: `./gradlew.bat :core:book:testDebugUnitTest --tests "*TocEnricherTest*"`
Expected: PASS (7 tests).

- [ ] **Step 5: Commit**

```bash
git add core/book/src/main/java/com/ebookreader/core/book/toc/TocEnricher.kt core/book/src/test/java/com/ebookreader/core/book/toc/TocEnricherTest.kt
git commit -m "feat: add TocEnricher (preview, prefijo Capitulo, indice resuelto)"
```

---

## Task 4: Invocar `TocEnricher` en `ReaderViewModel` y navegar por índice resuelto

**Files:**
- Modify: `feature/reader/src/main/java/com/ebookreader/feature/reader/ReaderViewModel.kt`

- [ ] **Step 1: Enriquecer el ToC tras cargar texto e índice**

En `loadBook()`, hoy hay (≈L88-94):

```kotlin
                val toc = parser.getTableOfContents(file)
                val content = parser.extractTextContent(file)

                val chapters = content.chapters.map { chapter ->
                    chapter.title to chapter.textContent
                }
                ttsController.loadText(chapters)
```

Reemplazar la primera línea (`val toc = ...`) por una versión enriquecida, usando
el `content` ya cargado. Mover el cálculo de `toc` después de `content` y aplicar
el enricher:

```kotlin
                val rawToc = parser.getTableOfContents(file)
                val content = parser.extractTextContent(file)
                val toc = com.ebookreader.core.book.toc.TocEnricher.enrich(rawToc, content)

                val chapters = content.chapters.map { chapter ->
                    chapter.title to chapter.textContent
                }
                ttsController.loadText(chapters)
```

(El resto de `loadBook`, incluida la asignación `toc = toc` en el `_uiState.update`
de ≈L130, no cambia.)

- [ ] **Step 2: Añadir navegación por índice de capítulo resuelto**

Añadir un método nuevo junto a `jumpToChapter` (≈L214) que ignore índices no
resueltos (`-1`):

```kotlin
    /** Navega a una entrada del índice usando su índice de capítulo ya resuelto. */
    fun jumpToTocEntry(chapterIndex: Int) {
        if (chapterIndex < 0) return
        jumpToChapter(chapterIndex)
    }
```

- [ ] **Step 3: Compilar feature:reader**

Run: `./gradlew.bat :feature:reader:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add feature/reader/src/main/java/com/ebookreader/feature/reader/ReaderViewModel.kt
git commit -m "feat: enriquecer el ToC y navegar por indice de capitulo resuelto"
```

---

## Task 5: Drawer — preview, encabezado en español y navegación correcta

**Files:**
- Modify: `feature/reader/src/main/java/com/ebookreader/feature/reader/components/TableOfContentsDrawer.kt`
- Modify: `feature/reader/src/main/java/com/ebookreader/feature/reader/ReaderScreen.kt:68-74`

- [ ] **Step 1: Traducir encabezados y cambiar la firma de click a índice de capítulo**

En `TableOfContentsDrawer.kt`, cambiar el callback para entregar el índice de
capítulo (no la posición de lista) y traducir los textos. Reemplazar la firma y
los textos:

```kotlin
@Composable
fun TableOfContentsDrawer(
    toc: TableOfContents,
    onEntryClick: (TocEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Column {
            Text(
                text = "Índice",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
            Divider()
            if (toc.entries.isEmpty()) {
                Text(
                    text = "Sin índice disponible",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn {
                    itemsIndexed(toc.entries) { _, entry ->
                        TocEntryItem(
                            entry = entry,
                            depth = 0,
                            onEntryClick = onEntryClick
                        )
                        Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 2: Renderizar título + preview y propagar el click**

Reemplazar `TocEntryItem` completo:

```kotlin
@Composable
private fun TocEntryItem(
    entry: TocEntry,
    depth: Int,
    onEntryClick: (TocEntry) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEntryClick(entry) }
            .padding(
                start = (16 + depth * 16).dp,
                end = 16.dp,
                top = 12.dp,
                bottom = 12.dp
            )
    ) {
        Text(
            text = entry.title,
            style = if (depth == 0) {
                MaterialTheme.typography.bodyLarge
            } else {
                MaterialTheme.typography.bodyMedium
            },
            color = if (depth == 0) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        if (entry.preview.isNotBlank()) {
            Text(
                text = entry.preview,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
    entry.children.forEach { child ->
        TocEntryItem(
            entry = child,
            depth = depth + 1,
            onEntryClick = onEntryClick
        )
    }
}
```

- [ ] **Step 3: Ajustar imports del drawer**

Asegurar que `TableOfContentsDrawer.kt` importa lo necesario y quita lo que ya no
usa. Imports a tener:

```kotlin
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ebookreader.core.book.model.TableOfContents
import com.ebookreader.core.book.model.TocEntry
```

(Se eliminan los imports ya no usados: `Row`, `Spacer`, `width`, `Alignment`.)

- [ ] **Step 4: Actualizar el call-site en `ReaderScreen`**

En `ReaderScreen.kt` (≈L68-74), cambiar el lambda para usar el índice de capítulo
resuelto de la entrada:

```kotlin
            TableOfContentsDrawer(
                toc = uiState.toc,
                onEntryClick = { entry ->
                    viewModel.jumpToTocEntry(entry.chapterIndex)
                    scope.launch { drawerState.close() }
                }
            )
```

- [ ] **Step 5: Compilar la app entera**

Run: `./gradlew.bat :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add feature/reader/src/main/java/com/ebookreader/feature/reader/components/TableOfContentsDrawer.kt feature/reader/src/main/java/com/ebookreader/feature/reader/ReaderScreen.kt
git commit -m "feat: indice con preview, encabezado en espanol y navegacion correcta"
```

---

## Task 6: Verificación en dispositivo

- [ ] **Step 1: Instalar**

Run: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
Expected: Success

- [ ] **Step 2: Validar manualmente**

Abrir un libro con capítulos numerados y abrir el índice (icono del menú lateral):
- Cada entrada muestra "Capítulo N" + un fragmento del inicio del capítulo en gris.
- El encabezado dice "Índice".
- Las partes nombradas ("ENTONCES", "AHORA") aparecen sin prefijo "Capítulo", con
  sus capítulos anidados debajo.
- Pulsar una entrada abre **ese** capítulo (no uno desplazado).

---

## Self-Review (completado durante la escritura)

- **Cobertura del spec:** preview (Task 3/5), prefijo "Capítulo N" (Task 3),
  encabezado ES (Task 5), fix de navegación por href→índice (Task 1/2/3/4/5),
  jerarquía preservada (Task 5). `href` en Chapter (Task 1). ✔
- **Sin placeholders:** todo el código está completo y explícito. ✔
- **Consistencia de tipos:** `Chapter(index, href, title, textContent)`,
  `TocEntry(title, href, preview, chapterIndex, children)`,
  `TocEnricher.enrich(TableOfContents, BookContent): TableOfContents`,
  `onEntryClick: (TocEntry) -> Unit`, `jumpToTocEntry(Int)` — coherentes entre tareas. ✔
- **Desvío respecto al spec:** el spec ubicaba el preview en el ViewModel; se mueve
  a `TocEnricher` (core/book) por testabilidad. Mismo resultado, mejor diseño.
