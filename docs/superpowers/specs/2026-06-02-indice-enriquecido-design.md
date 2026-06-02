# Diseño: Índice enriquecido (preview + cosmético + fix de navegación)

> Fecha: 2026-06-02
> Feature independiente. Implementar antes que el resaltado TTS.

## Problema

El drawer del índice (`TableOfContentsDrawer`) muestra los títulos que trae el
EPUB en su NCX/Nav. En muchos libros esos títulos son pobres: capítulos
numerados ("1", "2", "3") agrupados bajo partes ("ENTONCES", "AHORA"). No es un
bug del parser —funciona y muestra fielmente el ToC del libro— sino una
limitación del origen: el libro no tiene títulos descriptivos.

Diagnóstico confirmado con dos EPUB reales del dispositivo (EPUB2 con `toc.ncx`):
- Los `navLabel` de los capítulos contienen solo números; el `<h3>` del capítulo
  también es solo el número, seguido directamente de la narración.
- Los `href` del NCX coinciden con los del spine (sin fragmentos `#`).

Además se detectaron dos defectos colaterales:
1. **Navegación incorrecta:** `onEntryClick(entry, index)` (`TableOfContentsDrawer.kt:76`)
   usa la posición de la entrada en la lista del nivel actual como índice de
   capítulo. El spine suele tener más ítems que el ToC (portada, portadilla,
   página de índice…), así que el índice de lista ≠ índice de capítulo del spine
   → al pulsar una entrada se puede saltar al capítulo equivocado.
2. **Idioma:** el encabezado del drawer está en inglés ("Table of Contents",
   "No table of contents available") en una app en español.

## Objetivo

Hacer el índice navegable y correcto:
- Mostrar un **preview** del contenido de cada capítulo junto a su título/número.
- Pulido cosmético: prefijar "Capítulo N" a entradas puramente numéricas y
  traducir el encabezado del drawer.
- Corregir la navegación para que cada entrada salte a su capítulo real.

## Arquitectura

### 1. Exponer `href` en el modelo `Chapter`

`core/book/.../model/BookContent.kt` — añadir `href: String` a `Chapter`:

```kotlin
data class Chapter(
    val index: Int,
    val href: String,      // nuevo: href del spine item (clave de mapeo con el ToC)
    val title: String,
    val textContent: String
)
```

`core/book/.../parser/EpubParser.kt` — `extractTextContent()` (≈L49-69) ya itera
`info.spineItems` con su `href`; poblar el nuevo campo:

```kotlin
Chapter(index = index, href = item.href, title = ..., textContent = text)
```

`TocEntry` (`core/book/.../model/TableOfContents.kt`) ya tiene `href`. Para el
preview se añade un campo opcional:

```kotlin
data class TocEntry(
    val title: String,
    val href: String,
    val preview: String = "",        // nuevo
    val children: List<TocEntry> = emptyList()
)
```

### 2. Generar preview y resolver navegación en el ViewModel

`feature/reader/.../ReaderViewModel.kt` — `loadBook()` ya tiene a mano `toc`
(de `getTableOfContents`) y `content` (de `extractTextContent`). Sin releer el
EPUB:

- Construir un mapa `href (sin fragmento) -> Chapter` a partir de `content.chapters`.
- Para cada `TocEntry` (recursivo, incluyendo `children`), buscar su capítulo por
  `href.substringBefore('#')` y generar `preview` = primeras ~90 caracteres
  "limpias" del `textContent`, saltando el número/encabezado inicial y cortando en
  límite de palabra. Si no hay capítulo o texto, `preview = ""`.
- Construir un mapa `href -> chapter.index` para resolver la navegación.

El `uiState.toc` pasa a contener entradas con `preview`. La navegación del drawer
pasará el `href` (o el índice de capítulo ya resuelto) en vez del índice de lista.

### 3. Drawer

`feature/reader/.../components/TableOfContentsDrawer.kt`:
- `onEntryClick` cambia para entregar el **índice de capítulo del spine** resuelto
  desde `entry.href` (vía el mapa del ViewModel), no el índice de la lista.
- Render de cada entrada: título (con prefijo "Capítulo N" si el título es
  puramente numérico) + `preview` en una segunda línea con
  `MaterialTheme.colorScheme.onSurfaceVariant`, `maxLines = 2`, elipsis.
- Encabezado traducido: "Índice" y "Sin índice disponible".
- La jerarquía parte→capítulo (children con `depth`) ya existe; se mantiene.

## Casos borde

- Entrada del ToC sin capítulo correspondiente (href no casa) → sin preview, el
  título se muestra igual; al pulsarla no se navega (no-op, no crash).
- Capítulo con texto vacío (portada) → preview vacío.
- Título no numérico (p. ej. "ENTONCES", "Dedicatoria") → sin prefijo "Capítulo".
- `href` con fragmento `#` → se compara sin el fragmento.

## Qué NO incluye (YAGNI)

- No se reordena ni se filtra el ToC.
- No se inventa título cuando el libro no lo trae (se usa número + preview).
- No se cachea el preview en DB (se calcula al abrir el libro).

## Verificación

- Abrir un libro con capítulos numerados → el índice muestra "Capítulo 1" +
  fragmento del inicio; pulsar una entrada abre el capítulo correcto.
- Encabezado del drawer en español.
- Libro con partes nombradas → jerarquía visible, partes sin prefijo "Capítulo".
