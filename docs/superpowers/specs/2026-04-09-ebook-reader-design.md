# EbookReader - Lector de Ebooks con TTS para Android

## Resumen

App nativa Android para lectura de ebooks (EPUB y PDF) con síntesis de voz integrada (TTS). Ofrece dos modos de uso: lectura visual con TTS opcional y modo audio tipo reproductor. Soporta TTS local (offline) y voces IA en la nube (online). Orientada a publicación en Google Play Store.

## Stack tecnológico

- **Lenguaje:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Arquitectura:** MVVM + Clean Architecture
- **DI:** Hilt
- **Base de datos:** Room
- **Lectura EPUB/PDF:** Readium
- **TTS local:** Android TTS API
- **TTS nube:** Google Cloud TTS (WaveNet/Neural2)
- **Audio/Media:** Media3 (ExoPlayer)
- **Build:** Gradle con Kotlin DSL
- **Min SDK:** 26 (Android 8.0)

## Arquitectura

### Capas

```
Presentación (Compose + ViewModels)
    ↓
Dominio (UseCases / Interfaces)
    ↓
Datos (Room, FileSystem, TTS Engines, Cloud API)
```

### Módulos

- **:app** — Punto de entrada, navegación, DI
- **:feature:library** — Biblioteca de ebooks
- **:feature:reader** — Lectura visual (EPUB y PDF)
- **:feature:audioplayer** — Modo reproductor de audio
- **:feature:settings** — Configuración
- **:core:tts** — Motores TTS (local + nube), cache, controles
- **:core:book** — Parseo y modelo de datos de EPUB/PDF (Readium)
- **:core:data** — Room DB, repositorios, preferencias
- **:core:ui** — Componentes de UI compartidos, temas

## Funcionalidades

### 1. Gestión de biblioteca

- Escaneo automático del almacenamiento buscando `.epub` y `.pdf` al abrir la app
- Re-escaneo manual bajo demanda
- Importación manual mediante botón "Añadir" o intent filter (abrir archivo desde explorador)
- Base de datos local (Room) con:
  - Metadatos: título, autor, portada, formato, ruta del archivo
  - Progreso: posición actual (capítulo/página, porcentaje)
  - Último acceso
  - Marcadores del usuario
- Vista en grid con portadas
- Ordenar por: recientes, título, autor
- Búsqueda por título/autor
- Eliminar de biblioteca (sin borrar el archivo del dispositivo)

### 2. Lectura visual

#### EPUB
- Renderizado mediante Readium con soporte de estilos CSS
- Navegación por capítulos (índice lateral desplegable)
- Paso de página con swipe horizontal o tap en bordes de pantalla
- Personalización: tamaño de fuente, tipografía, interlineado
- Temas: claro, oscuro, sepia

#### PDF
- Renderizado mediante Readium (PdfRenderer nativo como fallback)
- Zoom con pinch, scroll vertical continuo
- Navegación por número de página y miniaturas

#### Común
- Botón flotante de "play" para activar TTS desde la posición actual
- Barra inferior con progreso general del libro
- Modo pantalla completa (ocultar barras del sistema)
- Lectura nocturna automática según horario del dispositivo
- Marcadores y progreso guardado automáticamente

### 3. Sistema TTS

#### Motor local (offline)
- API TTS nativa de Android
- Voces según las instaladas en el dispositivo
- Sin coste, funciona sin conexión

#### Motor en la nube (online)
- Google Cloud TTS con voces neuronales (WaveNet/Neural2)
- Requiere API key configurada por el usuario en ajustes
- Cache local de fragmentos ya generados para evitar peticiones repetidas

#### Controles
- Play / Pausa / Stop
- Avanzar/retroceder por frase o por párrafo
- Velocidad regulable (0.5x a 3.0x)
- Selector de voz e idioma
- Detección automática del idioma del texto

#### Resaltado sincronizado
- En modo lectura visual: resaltado frase a frase mientras se lee
- Auto-scroll para seguir la lectura

### 4. Modo audio (reproductor)

- Pantalla independiente tipo reproductor de música
- Muestra: portada del libro, título, autor, capítulo actual
- Controles grandes: play/pausa, avance/retroceso por frase y por capítulo
- Barra de progreso del capítulo actual
- Notificación persistente con controles media para uso con pantalla apagada
- Soporte de controles de auriculares bluetooth (MediaSession)
- Timer de apagado: 15, 30, 60 minutos, o fin de capítulo

### 5. Configuración

#### General
- Tema de la app (claro/oscuro/seguir sistema)
- Directorios a escanear para búsqueda de ebooks
- Idioma de la interfaz

#### TTS
- Motor por defecto (local/nube)
- Voz preferida para cada motor
- Velocidad por defecto
- Campo para API key de Google Cloud TTS

#### Lectura
- Fuente, tamaño, interlineado por defecto
- Tema de lectura por defecto
- Mantener pantalla encendida durante lectura

### 6. Publicación (Play Store)

- Onboarding: pantallas de bienvenida explicando funcionalidades clave
- Solicitud de permisos de almacenamiento guiada
- Icono y branding de la app
- Cumplimiento de políticas de Play Store

## Modelo de datos (Room)

### BookEntity
| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | Long (PK) | Identificador |
| title | String | Título del libro |
| author | String | Autor |
| coverPath | String? | Ruta a imagen de portada |
| filePath | String | Ruta al archivo EPUB/PDF |
| format | Enum (EPUB, PDF) | Formato del archivo |
| progress | Float | Progreso (0.0 a 1.0) |
| lastPosition | String | Posición serializada (locator de Readium) |
| lastAccess | Long | Timestamp del último acceso |
| addedAt | Long | Timestamp de cuando se añadió |

### BookmarkEntity
| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | Long (PK) | Identificador |
| bookId | Long (FK) | Referencia al libro |
| position | String | Posición serializada |
| label | String? | Etiqueta opcional del usuario |
| createdAt | Long | Timestamp de creación |

### TtsCacheEntity
| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | Long (PK) | Identificador |
| textHash | String | Hash del texto fuente |
| voiceId | String | ID de la voz usada |
| audioPath | String | Ruta al archivo de audio cacheado |
| createdAt | Long | Timestamp |

## Navegación

```
SplashScreen → Biblioteca (home)
    ├── Detalle de libro → Lector visual ↔ Modo audio
    ├── Configuración
    └── Búsqueda
```

## Permisos Android

- `READ_EXTERNAL_STORAGE` / `READ_MEDIA_DOCUMENTS` (según API level)
- `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK` (TTS en background)
- `INTERNET` (TTS en la nube)
- `WAKE_LOCK` (mantener CPU durante reproducción)
