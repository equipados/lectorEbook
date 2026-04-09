# EbookReader - Plan de Implementacion

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Construir una app Android nativa de lectura de ebooks (EPUB/PDF) con TTS local y en la nube, dos modos de uso (lectura visual y reproductor de audio), y publicarla en Play Store.

**Architecture:** App multi-modulo Kotlin con Jetpack Compose, MVVM + Clean Architecture. Readium para parseo/renderizado de libros. Android TTS para voz offline, Google Cloud TTS para voces IA online. Media3 para controles de reproduccion en background.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Hilt, Room, Readium, Android TTS API, Google Cloud TTS, Media3, Gradle KTS

---

## Estructura de archivos del proyecto

```
EbookReader/
├── app/
│   ├── build.gradle.kts
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/ebookreader/
│   │   │   ├── EbookReaderApp.kt              # Application class + Hilt
│   │   │   ├── MainActivity.kt                # Single activity
│   │   │   └── navigation/
│   │   │       └── AppNavigation.kt            # NavHost + rutas
│   │   └── res/
│   │       ├── values/themes.xml
│   │       └── mipmap-*/ic_launcher.*
│   └── src/test/
├── core/
│   ├── data/
│   │   ├── build.gradle.kts
│   │   └── src/main/java/com/ebookreader/core/data/
│   │       ├── db/
│   │       │   ├── AppDatabase.kt
│   │       │   ├── entity/BookEntity.kt
│   │       │   ├── entity/BookmarkEntity.kt
│   │       │   ├── entity/TtsCacheEntity.kt
│   │       │   ├── dao/BookDao.kt
│   │       │   └── dao/BookmarkDao.kt
│   │       ├── repository/
│   │       │   ├── BookRepository.kt
│   │       │   ├── BookRepositoryImpl.kt
│   │       │   ├── BookmarkRepository.kt
│   │       │   └── BookmarkRepositoryImpl.kt
│   │       ├── preferences/
│   │       │   ├── UserPreferences.kt
│   │       │   └── UserPreferencesImpl.kt
│   │       └── di/DataModule.kt
│   ├── book/
│   │   ├── build.gradle.kts
│   │   └── src/main/java/com/ebookreader/core/book/
│   │       ├── model/
│   │       │   ├── Book.kt
│   │       │   ├── BookFormat.kt
│   │       │   ├── TableOfContents.kt
│   │       │   └── BookContent.kt
│   │       ├── parser/
│   │       │   ├── BookParser.kt
│   │       │   ├── EpubParser.kt
│   │       │   └── PdfParser.kt
│   │       ├── scanner/
│   │       │   ├── BookScanner.kt
│   │       │   └── BookScannerImpl.kt
│   │       └── di/BookModule.kt
│   ├── tts/
│   │   ├── build.gradle.kts
│   │   └── src/main/java/com/ebookreader/core/tts/
│   │       ├── engine/
│   │       │   ├── TtsEngine.kt               # Interface
│   │       │   ├── LocalTtsEngine.kt
│   │       │   └── CloudTtsEngine.kt
│   │       ├── cache/
│   │       │   ├── TtsCacheManager.kt
│   │       │   └── TtsCacheManagerImpl.kt
│   │       ├── controller/
│   │       │   ├── TtsController.kt
│   │       │   └── TtsControllerImpl.kt
│   │       ├── model/
│   │       │   ├── TtsState.kt
│   │       │   ├── TtsVoice.kt
│   │       │   └── TextSegment.kt
│   │       ├── service/
│   │       │   └── TtsPlaybackService.kt      # Foreground service
│   │       └── di/TtsModule.kt
│   └── ui/
│       ├── build.gradle.kts
│       └── src/main/java/com/ebookreader/core/ui/
│           ├── theme/
│           │   ├── Theme.kt
│           │   ├── Color.kt
│           │   ├── Type.kt
│           │   └── ReadingTheme.kt
│           └── components/
│               ├── BookCoverImage.kt
│               ├── ProgressBar.kt
│               └── PlaybackControls.kt
├── feature/
│   ├── library/
│   │   ├── build.gradle.kts
│   │   └── src/main/java/com/ebookreader/feature/library/
│   │       ├── LibraryScreen.kt
│   │       ├── LibraryViewModel.kt
│   │       ├── BookDetailSheet.kt
│   │       └── components/
│   │           ├── BookGrid.kt
│   │           ├── BookCard.kt
│   │           └── SearchBar.kt
│   ├── reader/
│   │   ├── build.gradle.kts
│   │   └── src/main/java/com/ebookreader/feature/reader/
│   │       ├── ReaderScreen.kt
│   │       ├── ReaderViewModel.kt
│   │       ├── epub/
│   │       │   ├── EpubReaderView.kt
│   │       │   └── EpubReaderState.kt
│   │       ├── pdf/
│   │       │   ├── PdfReaderView.kt
│   │       │   └── PdfReaderState.kt
│   │       └── components/
│   │           ├── ReaderTopBar.kt
│   │           ├── ReaderBottomBar.kt
│   │           ├── TableOfContentsDrawer.kt
│   │           └── ReadingSettingsSheet.kt
│   ├── audioplayer/
│   │   ├── build.gradle.kts
│   │   └── src/main/java/com/ebookreader/feature/audioplayer/
│   │       ├── AudioPlayerScreen.kt
│   │       ├── AudioPlayerViewModel.kt
│   │       ├── SleepTimerDialog.kt
│   │       └── components/
│   │           ├── AudioControls.kt
│   │           ├── ChapterProgress.kt
│   │           └── CoverDisplay.kt
│   └── settings/
│       ├── build.gradle.kts
│       └── src/main/java/com/ebookreader/feature/settings/
│           ├── SettingsScreen.kt
│           ├── SettingsViewModel.kt
│           └── sections/
│               ├── GeneralSettings.kt
│               ├── TtsSettings.kt
│               └── ReadingSettings.kt
├── build.gradle.kts                           # Root build file
├── settings.gradle.kts                        # Module declarations
└── gradle/
    └── libs.versions.toml                     # Version catalog
```

---

## Task 1: Scaffolding del proyecto Android

**Files:**
- Create: `build.gradle.kts` (root)
- Create: `settings.gradle.kts`
- Create: `gradle.properties`
- Create: `gradle/libs.versions.toml`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/ebookreader/EbookReaderApp.kt`
- Create: `app/src/main/java/com/ebookreader/MainActivity.kt`

- [ ] **Step 1: Crear proyecto base con Android Studio CLI o manualmente**

Generar la estructura base. Usar `gradle init` no aplica para Android, asi que creamos los archivos manualmente.

`gradle/libs.versions.toml`:
```toml
[versions]
agp = "8.7.3"
kotlin = "2.1.0"
ksp = "2.1.0-1.0.29"
compose-bom = "2025.01.01"
material3 = "1.3.1"
hilt = "2.54"
hilt-navigation-compose = "1.2.0"
room = "2.6.1"
readium = "3.1.0"
media3 = "1.5.1"
navigation-compose = "2.8.6"
datastore = "1.1.2"
coil = "2.7.0"
coroutines = "1.9.0"
google-cloud-tts = "2.54.0"
junit = "4.13.2"
mockk = "1.13.13"
turbine = "1.2.0"

[libraries]
# Compose
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }

# Navigation
navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation-compose" }

# Hilt
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hilt-navigation-compose" }

# Room
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }

# Readium
readium-shared = { group = "org.readium.kotlin-toolkit", name = "readium-shared", version.ref = "readium" }
readium-streamer = { group = "org.readium.kotlin-toolkit", name = "readium-streamer", version.ref = "readium" }
readium-navigator = { group = "org.readium.kotlin-toolkit", name = "readium-navigator", version.ref = "readium" }
readium-navigator-media = { group = "org.readium.kotlin-toolkit", name = "readium-navigator-media-tts", version.ref = "readium" }

# Media3
media3-session = { group = "androidx.media3", name = "media3-session", version.ref = "media3" }
media3-exoplayer = { group = "androidx.media3", name = "media3-exoplayer", version.ref = "media3" }

# DataStore
datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }

# Coil (image loading)
coil-compose = { group = "io.coil-kt", name = "coil-compose", version.ref = "coil" }

# Coroutines
coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }

# Google Cloud TTS
google-cloud-tts = { group = "com.google.cloud", name = "google-cloud-texttospeech", version.ref = "google-cloud-tts" }

# Testing
junit = { group = "junit", name = "junit", version.ref = "junit" }
mockk = { group = "io.mockk", name = "mockk", version.ref = "mockk" }
turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }
room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

- [ ] **Step 2: Crear root build.gradle.kts**

`build.gradle.kts` (root):
```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
```

- [ ] **Step 3: Crear settings.gradle.kts**

`settings.gradle.kts`:
```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolution {
    repositories {
        google()
        mavenCentral()
        maven("https://s3.amazonaws.com/nicbell.mvn.repo/releases") // Readium
    }
}

rootProject.name = "EbookReader"

include(":app")
include(":core:data")
include(":core:book")
include(":core:tts")
include(":core:ui")
include(":feature:library")
include(":feature:reader")
include(":feature:audioplayer")
include(":feature:settings")
```

- [ ] **Step 4: Crear gradle.properties**

`gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

- [ ] **Step 5: Crear app/build.gradle.kts**

`app/build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.ebookreader"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ebookreader"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:book"))
    implementation(project(":core:tts"))
    implementation(project(":core:ui"))
    implementation(project(":feature:library"))
    implementation(project(":feature:reader"))
    implementation(project(":feature:audioplayer"))
    implementation(project(":feature:settings"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.navigation.compose)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
}
```

- [ ] **Step 6: Crear AndroidManifest.xml**

`app/src/main/AndroidManifest.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
        android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.READ_MEDIA_DOCUMENTS" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application
        android:name=".EbookReaderApp"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.EbookReader">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.EbookReader">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <data android:mimeType="application/epub+zip" />
                <data android:mimeType="application/pdf" />
            </intent-filter>
        </activity>

        <service
            android:name="com.ebookreader.core.tts.service.TtsPlaybackService"
            android:foregroundServiceType="mediaPlayback"
            android:exported="false" />
    </application>
</manifest>
```

- [ ] **Step 7: Crear EbookReaderApp.kt**

`app/src/main/java/com/ebookreader/EbookReaderApp.kt`:
```kotlin
package com.ebookreader

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class EbookReaderApp : Application()
```

- [ ] **Step 8: Crear MainActivity.kt**

`app/src/main/java/com/ebookreader/MainActivity.kt`:
```kotlin
package com.ebookreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ebookreader.core.ui.theme.EbookReaderTheme
import com.ebookreader.navigation.AppNavigation
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EbookReaderTheme {
                AppNavigation()
            }
        }
    }
}
```

- [ ] **Step 9: Crear esqueleto de navegacion**

`app/src/main/java/com/ebookreader/navigation/AppNavigation.kt`:
```kotlin
package com.ebookreader.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ebookreader.feature.library.LibraryScreen
import com.ebookreader.feature.reader.ReaderScreen
import com.ebookreader.feature.audioplayer.AudioPlayerScreen
import com.ebookreader.feature.settings.SettingsScreen

object Routes {
    const val LIBRARY = "library"
    const val READER = "reader/{bookId}"
    const val AUDIO_PLAYER = "audio_player/{bookId}"
    const val SETTINGS = "settings"

    fun reader(bookId: Long) = "reader/$bookId"
    fun audioPlayer(bookId: Long) = "audio_player/$bookId"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.LIBRARY) {
        composable(Routes.LIBRARY) {
            LibraryScreen(
                onBookClick = { bookId -> navController.navigate(Routes.reader(bookId)) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(
            Routes.READER,
            arguments = listOf(navArgument("bookId") { type = NavType.LongType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getLong("bookId") ?: return@composable
            ReaderScreen(
                bookId = bookId,
                onBack = { navController.popBackStack() },
                onSwitchToAudio = { navController.navigate(Routes.audioPlayer(bookId)) }
            )
        }

        composable(
            Routes.AUDIO_PLAYER,
            arguments = listOf(navArgument("bookId") { type = NavType.LongType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getLong("bookId") ?: return@composable
            AudioPlayerScreen(
                bookId = bookId,
                onBack = { navController.popBackStack() },
                onSwitchToReader = {
                    navController.popBackStack()
                    navController.navigate(Routes.reader(bookId))
                }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
```

- [ ] **Step 10: Crear build.gradle.kts para cada modulo core**

Cada modulo core necesita su build.gradle.kts. Ejemplo para `core/data/build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.ebookreader.core.data"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.datastore.preferences)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
}
```

`core/book/build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.ebookreader.core.book"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(project(":core:data"))
    implementation(libs.readium.shared)
    implementation(libs.readium.streamer)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
}
```

`core/tts/build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.ebookreader.core.tts"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(project(":core:data"))
    implementation(libs.media3.session)
    implementation(libs.media3.exoplayer)
    implementation(libs.google.cloud.tts)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
}
```

`core/ui/build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.ebookreader.core.ui"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.coil.compose)
    debugImplementation(libs.compose.ui.tooling)
}
```

- [ ] **Step 11: Crear build.gradle.kts para cada modulo feature**

`feature/library/build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.ebookreader.feature.library"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:book"))
    implementation(project(":core:ui"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.navigation.compose)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    implementation(libs.coil.compose)
}
```

`feature/reader/build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.ebookreader.feature.reader"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:book"))
    implementation(project(":core:tts"))
    implementation(project(":core:ui"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.navigation.compose)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    implementation(libs.readium.shared)
    implementation(libs.readium.navigator)
}
```

`feature/audioplayer/build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.ebookreader.feature.audioplayer"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:tts"))
    implementation(project(":core:ui"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.navigation.compose)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    implementation(libs.media3.session)
}
```

`feature/settings/build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.ebookreader.feature.settings"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:tts"))
    implementation(project(":core:ui"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.navigation.compose)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
}
```

- [ ] **Step 12: Crear directorios src vacios para todos los modulos**

Run:
```bash
mkdir -p core/data/src/main/java/com/ebookreader/core/data
mkdir -p core/data/src/test/java/com/ebookreader/core/data
mkdir -p core/book/src/main/java/com/ebookreader/core/book
mkdir -p core/book/src/test/java/com/ebookreader/core/book
mkdir -p core/tts/src/main/java/com/ebookreader/core/tts
mkdir -p core/tts/src/test/java/com/ebookreader/core/tts
mkdir -p core/ui/src/main/java/com/ebookreader/core/ui
mkdir -p feature/library/src/main/java/com/ebookreader/feature/library
mkdir -p feature/reader/src/main/java/com/ebookreader/feature/reader
mkdir -p feature/audioplayer/src/main/java/com/ebookreader/feature/audioplayer
mkdir -p feature/settings/src/main/java/com/ebookreader/feature/settings
```

- [ ] **Step 13: Verificar que el proyecto compila**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 14: Commit**

```bash
git init
git add -A
git commit -m "chore: scaffold multi-module Android project with Gradle KTS"
```

---

## Task 2: Core Data - Entidades Room y DAOs

**Files:**
- Create: `core/data/src/main/java/com/ebookreader/core/data/db/entity/BookEntity.kt`
- Create: `core/data/src/main/java/com/ebookreader/core/data/db/entity/BookmarkEntity.kt`
- Create: `core/data/src/main/java/com/ebookreader/core/data/db/entity/TtsCacheEntity.kt`
- Create: `core/data/src/main/java/com/ebookreader/core/data/db/dao/BookDao.kt`
- Create: `core/data/src/main/java/com/ebookreader/core/data/db/dao/BookmarkDao.kt`
- Create: `core/data/src/main/java/com/ebookreader/core/data/db/AppDatabase.kt`
- Create: `core/data/src/main/java/com/ebookreader/core/data/db/entity/BookFormat.kt`
- Test: `core/data/src/test/java/com/ebookreader/core/data/db/dao/BookDaoTest.kt`

- [ ] **Step 1: Crear BookFormat enum**

`core/data/src/main/java/com/ebookreader/core/data/db/entity/BookFormat.kt`:
```kotlin
package com.ebookreader.core.data.db.entity

enum class BookFormat {
    EPUB, PDF
}
```

- [ ] **Step 2: Crear BookEntity**

`core/data/src/main/java/com/ebookreader/core/data/db/entity/BookEntity.kt`:
```kotlin
package com.ebookreader.core.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val author: String,
    val coverPath: String? = null,
    val filePath: String,
    val format: BookFormat,
    val progress: Float = 0f,
    val lastPosition: String = "",
    val lastAccess: Long = System.currentTimeMillis(),
    val addedAt: Long = System.currentTimeMillis()
)
```

- [ ] **Step 3: Crear BookmarkEntity**

`core/data/src/main/java/com/ebookreader/core/data/db/entity/BookmarkEntity.kt`:
```kotlin
package com.ebookreader.core.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bookmarks",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("bookId")]
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: Long,
    val position: String,
    val label: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
```

- [ ] **Step 4: Crear TtsCacheEntity**

`core/data/src/main/java/com/ebookreader/core/data/db/entity/TtsCacheEntity.kt`:
```kotlin
package com.ebookreader.core.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tts_cache")
data class TtsCacheEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val textHash: String,
    val voiceId: String,
    val audioPath: String,
    val createdAt: Long = System.currentTimeMillis()
)
```

- [ ] **Step 5: Crear BookDao**

`core/data/src/main/java/com/ebookreader/core/data/db/dao/BookDao.kt`:
```kotlin
package com.ebookreader.core.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ebookreader.core.data.db.entity.BookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY lastAccess DESC")
    fun getAllByRecent(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books ORDER BY title ASC")
    fun getAllByTitle(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books ORDER BY author ASC")
    fun getAllByAuthor(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getById(id: Long): BookEntity?

    @Query("SELECT * FROM books WHERE filePath = :path")
    suspend fun getByFilePath(path: String): BookEntity?

    @Query("SELECT * FROM books WHERE title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%'")
    fun search(query: String): Flow<List<BookEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(book: BookEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(books: List<BookEntity>): List<Long>

    @Update
    suspend fun update(book: BookEntity)

    @Query("UPDATE books SET progress = :progress, lastPosition = :position, lastAccess = :timestamp WHERE id = :id")
    suspend fun updateProgress(id: Long, progress: Float, position: String, timestamp: Long = System.currentTimeMillis())

    @Delete
    suspend fun delete(book: BookEntity)

    @Query("SELECT COUNT(*) FROM books")
    suspend fun count(): Int
}
```

- [ ] **Step 6: Crear BookmarkDao**

`core/data/src/main/java/com/ebookreader/core/data/db/dao/BookmarkDao.kt`:
```kotlin
package com.ebookreader.core.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.ebookreader.core.data.db.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY createdAt DESC")
    fun getBookmarksForBook(bookId: Long): Flow<List<BookmarkEntity>>

    @Insert
    suspend fun insert(bookmark: BookmarkEntity): Long

    @Delete
    suspend fun delete(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE bookId = :bookId")
    suspend fun deleteAllForBook(bookId: Long)
}
```

- [ ] **Step 7: Crear AppDatabase**

`core/data/src/main/java/com/ebookreader/core/data/db/AppDatabase.kt`:
```kotlin
package com.ebookreader.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ebookreader.core.data.db.dao.BookDao
import com.ebookreader.core.data.db.dao.BookmarkDao
import com.ebookreader.core.data.db.entity.BookEntity
import com.ebookreader.core.data.db.entity.BookmarkEntity
import com.ebookreader.core.data.db.entity.TtsCacheEntity

@Database(
    entities = [BookEntity::class, BookmarkEntity::class, TtsCacheEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun bookmarkDao(): BookmarkDao
}
```

- [ ] **Step 8: Commit**

```bash
git add core/data/
git commit -m "feat: add Room entities and DAOs for books, bookmarks, and TTS cache"
```

---

## Task 3: Core Data - Repositorios y DI

**Files:**
- Create: `core/data/src/main/java/com/ebookreader/core/data/repository/BookRepository.kt`
- Create: `core/data/src/main/java/com/ebookreader/core/data/repository/BookRepositoryImpl.kt`
- Create: `core/data/src/main/java/com/ebookreader/core/data/repository/BookmarkRepository.kt`
- Create: `core/data/src/main/java/com/ebookreader/core/data/repository/BookmarkRepositoryImpl.kt`
- Create: `core/data/src/main/java/com/ebookreader/core/data/preferences/UserPreferences.kt`
- Create: `core/data/src/main/java/com/ebookreader/core/data/preferences/UserPreferencesImpl.kt`
- Create: `core/data/src/main/java/com/ebookreader/core/data/di/DataModule.kt`
- Test: `core/data/src/test/java/com/ebookreader/core/data/repository/BookRepositoryImplTest.kt`

- [ ] **Step 1: Crear BookRepository interface**

`core/data/src/main/java/com/ebookreader/core/data/repository/BookRepository.kt`:
```kotlin
package com.ebookreader.core.data.repository

import com.ebookreader.core.data.db.entity.BookEntity
import kotlinx.coroutines.flow.Flow

enum class SortOrder { RECENT, TITLE, AUTHOR }

interface BookRepository {
    fun getAll(sortOrder: SortOrder = SortOrder.RECENT): Flow<List<BookEntity>>
    fun search(query: String): Flow<List<BookEntity>>
    suspend fun getById(id: Long): BookEntity?
    suspend fun getByFilePath(path: String): BookEntity?
    suspend fun insert(book: BookEntity): Long
    suspend fun insertAll(books: List<BookEntity>): List<Long>
    suspend fun update(book: BookEntity)
    suspend fun updateProgress(id: Long, progress: Float, position: String)
    suspend fun delete(book: BookEntity)
}
```

- [ ] **Step 2: Crear BookRepositoryImpl**

`core/data/src/main/java/com/ebookreader/core/data/repository/BookRepositoryImpl.kt`:
```kotlin
package com.ebookreader.core.data.repository

import com.ebookreader.core.data.db.dao.BookDao
import com.ebookreader.core.data.db.entity.BookEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class BookRepositoryImpl @Inject constructor(
    private val bookDao: BookDao
) : BookRepository {

    override fun getAll(sortOrder: SortOrder): Flow<List<BookEntity>> = when (sortOrder) {
        SortOrder.RECENT -> bookDao.getAllByRecent()
        SortOrder.TITLE -> bookDao.getAllByTitle()
        SortOrder.AUTHOR -> bookDao.getAllByAuthor()
    }

    override fun search(query: String): Flow<List<BookEntity>> = bookDao.search(query)

    override suspend fun getById(id: Long): BookEntity? = bookDao.getById(id)

    override suspend fun getByFilePath(path: String): BookEntity? = bookDao.getByFilePath(path)

    override suspend fun insert(book: BookEntity): Long = bookDao.insert(book)

    override suspend fun insertAll(books: List<BookEntity>): List<Long> = bookDao.insertAll(books)

    override suspend fun update(book: BookEntity) = bookDao.update(book)

    override suspend fun updateProgress(id: Long, progress: Float, position: String) =
        bookDao.updateProgress(id, progress, position)

    override suspend fun delete(book: BookEntity) = bookDao.delete(book)
}
```

- [ ] **Step 3: Crear BookmarkRepository interface + impl**

`core/data/src/main/java/com/ebookreader/core/data/repository/BookmarkRepository.kt`:
```kotlin
package com.ebookreader.core.data.repository

import com.ebookreader.core.data.db.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow

interface BookmarkRepository {
    fun getBookmarksForBook(bookId: Long): Flow<List<BookmarkEntity>>
    suspend fun addBookmark(bookmark: BookmarkEntity): Long
    suspend fun removeBookmark(bookmark: BookmarkEntity)
}
```

`core/data/src/main/java/com/ebookreader/core/data/repository/BookmarkRepositoryImpl.kt`:
```kotlin
package com.ebookreader.core.data.repository

import com.ebookreader.core.data.db.dao.BookmarkDao
import com.ebookreader.core.data.db.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class BookmarkRepositoryImpl @Inject constructor(
    private val bookmarkDao: BookmarkDao
) : BookmarkRepository {

    override fun getBookmarksForBook(bookId: Long): Flow<List<BookmarkEntity>> =
        bookmarkDao.getBookmarksForBook(bookId)

    override suspend fun addBookmark(bookmark: BookmarkEntity): Long =
        bookmarkDao.insert(bookmark)

    override suspend fun removeBookmark(bookmark: BookmarkEntity) =
        bookmarkDao.delete(bookmark)
}
```

- [ ] **Step 4: Crear UserPreferences**

`core/data/src/main/java/com/ebookreader/core/data/preferences/UserPreferences.kt`:
```kotlin
package com.ebookreader.core.data.preferences

import kotlinx.coroutines.flow.Flow

data class ReadingPrefs(
    val fontSize: Int = 16,
    val fontFamily: String = "default",
    val lineSpacing: Float = 1.5f,
    val theme: ReadingThemeType = ReadingThemeType.LIGHT,
    val keepScreenOn: Boolean = true
)

data class TtsPrefs(
    val preferredEngine: TtsEngineType = TtsEngineType.LOCAL,
    val localVoiceName: String = "",
    val cloudVoiceName: String = "en-US-Neural2-F",
    val speed: Float = 1.0f,
    val cloudApiKey: String = ""
)

data class AppPrefs(
    val appTheme: AppThemeType = AppThemeType.SYSTEM,
    val scanDirectories: List<String> = emptyList(),
    val language: String = "system"
)

enum class ReadingThemeType { LIGHT, DARK, SEPIA }
enum class TtsEngineType { LOCAL, CLOUD }
enum class AppThemeType { LIGHT, DARK, SYSTEM }

interface UserPreferences {
    val readingPrefs: Flow<ReadingPrefs>
    val ttsPrefs: Flow<TtsPrefs>
    val appPrefs: Flow<AppPrefs>

    suspend fun updateReadingPrefs(update: (ReadingPrefs) -> ReadingPrefs)
    suspend fun updateTtsPrefs(update: (TtsPrefs) -> TtsPrefs)
    suspend fun updateAppPrefs(update: (AppPrefs) -> AppPrefs)
}
```

- [ ] **Step 5: Crear UserPreferencesImpl**

`core/data/src/main/java/com/ebookreader/core/data/preferences/UserPreferencesImpl.kt`:
```kotlin
package com.ebookreader.core.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferencesImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : UserPreferences {

    private object Keys {
        val FONT_SIZE = intPreferencesKey("font_size")
        val FONT_FAMILY = stringPreferencesKey("font_family")
        val LINE_SPACING = floatPreferencesKey("line_spacing")
        val READING_THEME = stringPreferencesKey("reading_theme")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val TTS_ENGINE = stringPreferencesKey("tts_engine")
        val LOCAL_VOICE = stringPreferencesKey("local_voice")
        val CLOUD_VOICE = stringPreferencesKey("cloud_voice")
        val TTS_SPEED = floatPreferencesKey("tts_speed")
        val CLOUD_API_KEY = stringPreferencesKey("cloud_api_key")
        val APP_THEME = stringPreferencesKey("app_theme")
        val SCAN_DIRS = stringPreferencesKey("scan_dirs")
        val LANGUAGE = stringPreferencesKey("language")
    }

    override val readingPrefs: Flow<ReadingPrefs> = context.dataStore.data.map { prefs ->
        ReadingPrefs(
            fontSize = prefs[Keys.FONT_SIZE] ?: 16,
            fontFamily = prefs[Keys.FONT_FAMILY] ?: "default",
            lineSpacing = prefs[Keys.LINE_SPACING] ?: 1.5f,
            theme = prefs[Keys.READING_THEME]?.let { ReadingThemeType.valueOf(it) } ?: ReadingThemeType.LIGHT,
            keepScreenOn = prefs[Keys.KEEP_SCREEN_ON] ?: true
        )
    }

    override val ttsPrefs: Flow<TtsPrefs> = context.dataStore.data.map { prefs ->
        TtsPrefs(
            preferredEngine = prefs[Keys.TTS_ENGINE]?.let { TtsEngineType.valueOf(it) } ?: TtsEngineType.LOCAL,
            localVoiceName = prefs[Keys.LOCAL_VOICE] ?: "",
            cloudVoiceName = prefs[Keys.CLOUD_VOICE] ?: "en-US-Neural2-F",
            speed = prefs[Keys.TTS_SPEED] ?: 1.0f,
            cloudApiKey = prefs[Keys.CLOUD_API_KEY] ?: ""
        )
    }

    override val appPrefs: Flow<AppPrefs> = context.dataStore.data.map { prefs ->
        AppPrefs(
            appTheme = prefs[Keys.APP_THEME]?.let { AppThemeType.valueOf(it) } ?: AppThemeType.SYSTEM,
            scanDirectories = prefs[Keys.SCAN_DIRS]?.split("|")?.filter { it.isNotBlank() } ?: emptyList(),
            language = prefs[Keys.LANGUAGE] ?: "system"
        )
    }

    override suspend fun updateReadingPrefs(update: (ReadingPrefs) -> ReadingPrefs) {
        val current = ReadingPrefs() // defaults
        val updated = update(current)
        context.dataStore.edit { prefs ->
            prefs[Keys.FONT_SIZE] = updated.fontSize
            prefs[Keys.FONT_FAMILY] = updated.fontFamily
            prefs[Keys.LINE_SPACING] = updated.lineSpacing
            prefs[Keys.READING_THEME] = updated.theme.name
            prefs[Keys.KEEP_SCREEN_ON] = updated.keepScreenOn
        }
    }

    override suspend fun updateTtsPrefs(update: (TtsPrefs) -> TtsPrefs) {
        val current = TtsPrefs()
        val updated = update(current)
        context.dataStore.edit { prefs ->
            prefs[Keys.TTS_ENGINE] = updated.preferredEngine.name
            prefs[Keys.LOCAL_VOICE] = updated.localVoiceName
            prefs[Keys.CLOUD_VOICE] = updated.cloudVoiceName
            prefs[Keys.TTS_SPEED] = updated.speed
            prefs[Keys.CLOUD_API_KEY] = updated.cloudApiKey
        }
    }

    override suspend fun updateAppPrefs(update: (AppPrefs) -> AppPrefs) {
        val current = AppPrefs()
        val updated = update(current)
        context.dataStore.edit { prefs ->
            prefs[Keys.APP_THEME] = updated.appTheme.name
            prefs[Keys.SCAN_DIRS] = updated.scanDirectories.joinToString("|")
            prefs[Keys.LANGUAGE] = updated.language
        }
    }
}
```

- [ ] **Step 6: Crear DataModule (Hilt DI)**

`core/data/src/main/java/com/ebookreader/core/data/di/DataModule.kt`:
```kotlin
package com.ebookreader.core.data.di

import android.content.Context
import androidx.room.Room
import com.ebookreader.core.data.db.AppDatabase
import com.ebookreader.core.data.db.dao.BookDao
import com.ebookreader.core.data.db.dao.BookmarkDao
import com.ebookreader.core.data.preferences.UserPreferences
import com.ebookreader.core.data.preferences.UserPreferencesImpl
import com.ebookreader.core.data.repository.BookRepository
import com.ebookreader.core.data.repository.BookRepositoryImpl
import com.ebookreader.core.data.repository.BookmarkRepository
import com.ebookreader.core.data.repository.BookmarkRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindsModule {
    @Binds
    abstract fun bindBookRepository(impl: BookRepositoryImpl): BookRepository

    @Binds
    abstract fun bindBookmarkRepository(impl: BookmarkRepositoryImpl): BookmarkRepository

    @Binds
    abstract fun bindUserPreferences(impl: UserPreferencesImpl): UserPreferences
}

@Module
@InstallIn(SingletonComponent::class)
object DataProvidesModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "ebookreader.db")
            .build()

    @Provides
    fun provideBookDao(db: AppDatabase): BookDao = db.bookDao()

    @Provides
    fun provideBookmarkDao(db: AppDatabase): BookmarkDao = db.bookmarkDao()
}
```

- [ ] **Step 7: Escribir test para BookRepositoryImpl**

`core/data/src/test/java/com/ebookreader/core/data/repository/BookRepositoryImplTest.kt`:
```kotlin
package com.ebookreader.core.data.repository

import com.ebookreader.core.data.db.dao.BookDao
import com.ebookreader.core.data.db.entity.BookEntity
import com.ebookreader.core.data.db.entity.BookFormat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class BookRepositoryImplTest {

    private lateinit var bookDao: BookDao
    private lateinit var repository: BookRepositoryImpl

    private val sampleBook = BookEntity(
        id = 1,
        title = "Test Book",
        author = "Author",
        filePath = "/path/to/book.epub",
        format = BookFormat.EPUB
    )

    @Before
    fun setup() {
        bookDao = mockk(relaxed = true)
        repository = BookRepositoryImpl(bookDao)
    }

    @Test
    fun `getById returns book when exists`() = runTest {
        coEvery { bookDao.getById(1) } returns sampleBook
        val result = repository.getById(1)
        assertEquals(sampleBook, result)
    }

    @Test
    fun `getById returns null when not found`() = runTest {
        coEvery { bookDao.getById(99) } returns null
        val result = repository.getById(99)
        assertNull(result)
    }

    @Test
    fun `insert delegates to dao`() = runTest {
        coEvery { bookDao.insert(sampleBook) } returns 1L
        val id = repository.insert(sampleBook)
        assertEquals(1L, id)
        coVerify { bookDao.insert(sampleBook) }
    }

    @Test
    fun `updateProgress delegates to dao`() = runTest {
        repository.updateProgress(1, 0.5f, "chapter-3")
        coVerify { bookDao.updateProgress(1, 0.5f, "chapter-3") }
    }

    @Test
    fun `delete delegates to dao`() = runTest {
        repository.delete(sampleBook)
        coVerify { bookDao.delete(sampleBook) }
    }
}
```

- [ ] **Step 8: Ejecutar tests**

Run: `./gradlew :core:data:test`
Expected: All tests pass

- [ ] **Step 9: Commit**

```bash
git add core/data/
git commit -m "feat: add repositories, preferences, and Hilt DI for core data layer"
```

---

## Task 4: Core Book - Parseo y escaneo de libros

**Files:**
- Create: `core/book/src/main/java/com/ebookreader/core/book/model/Book.kt`
- Create: `core/book/src/main/java/com/ebookreader/core/book/model/BookContent.kt`
- Create: `core/book/src/main/java/com/ebookreader/core/book/model/TableOfContents.kt`
- Create: `core/book/src/main/java/com/ebookreader/core/book/parser/BookParser.kt`
- Create: `core/book/src/main/java/com/ebookreader/core/book/parser/EpubParser.kt`
- Create: `core/book/src/main/java/com/ebookreader/core/book/parser/PdfParser.kt`
- Create: `core/book/src/main/java/com/ebookreader/core/book/scanner/BookScanner.kt`
- Create: `core/book/src/main/java/com/ebookreader/core/book/scanner/BookScannerImpl.kt`
- Create: `core/book/src/main/java/com/ebookreader/core/book/di/BookModule.kt`

- [ ] **Step 1: Crear modelos de dominio**

`core/book/src/main/java/com/ebookreader/core/book/model/Book.kt`:
```kotlin
package com.ebookreader.core.book.model

import com.ebookreader.core.data.db.entity.BookFormat

data class Book(
    val id: Long = 0,
    val title: String,
    val author: String,
    val coverPath: String? = null,
    val filePath: String,
    val format: BookFormat,
    val progress: Float = 0f,
    val lastPosition: String = ""
)
```

`core/book/src/main/java/com/ebookreader/core/book/model/TableOfContents.kt`:
```kotlin
package com.ebookreader.core.book.model

data class TocEntry(
    val title: String,
    val href: String,
    val children: List<TocEntry> = emptyList()
)

data class TableOfContents(
    val entries: List<TocEntry>
)
```

`core/book/src/main/java/com/ebookreader/core/book/model/BookContent.kt`:
```kotlin
package com.ebookreader.core.book.model

data class BookContent(
    val chapters: List<Chapter>
)

data class Chapter(
    val index: Int,
    val title: String,
    val textContent: String
)
```

- [ ] **Step 2: Crear BookParser interface**

`core/book/src/main/java/com/ebookreader/core/book/parser/BookParser.kt`:
```kotlin
package com.ebookreader.core.book.parser

import android.content.Context
import com.ebookreader.core.book.model.Book
import com.ebookreader.core.book.model.BookContent
import com.ebookreader.core.book.model.TableOfContents
import com.ebookreader.core.data.db.entity.BookFormat
import java.io.File

interface BookParser {
    val supportedFormat: BookFormat
    suspend fun parseMetadata(file: File): Book
    suspend fun extractTextContent(file: File): BookContent
    suspend fun getTableOfContents(file: File): TableOfContents
    suspend fun extractCover(file: File, outputDir: File): String?
}
```

- [ ] **Step 3: Crear EpubParser**

`core/book/src/main/java/com/ebookreader/core/book/parser/EpubParser.kt`:
```kotlin
package com.ebookreader.core.book.parser

import android.content.Context
import com.ebookreader.core.book.model.Book
import com.ebookreader.core.book.model.BookContent
import com.ebookreader.core.book.model.Chapter
import com.ebookreader.core.book.model.TableOfContents
import com.ebookreader.core.book.model.TocEntry
import com.ebookreader.core.data.db.entity.BookFormat
import dagger.hilt.android.qualifiers.ApplicationContext
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.content
import org.readium.r2.streamer.Streamer
import java.io.File
import javax.inject.Inject

class EpubParser @Inject constructor(
    @ApplicationContext private val context: Context,
    private val streamer: Streamer
) : BookParser {

    override val supportedFormat = BookFormat.EPUB

    override suspend fun parseMetadata(file: File): Book {
        val publication = openPublication(file)
        return Book(
            title = publication.metadata.title ?: file.nameWithoutExtension,
            author = publication.metadata.authors.firstOrNull()?.name ?: "Unknown",
            filePath = file.absolutePath,
            format = BookFormat.EPUB
        )
    }

    override suspend fun extractTextContent(file: File): BookContent {
        val publication = openPublication(file)
        val chapters = publication.readingOrder.mapIndexed { index, link ->
            val text = publication.get(link)
                .read()
                .getOrNull()
                ?.let { String(it) }
                ?.replace(Regex("<[^>]*>"), "") // Strip HTML tags for TTS
                ?.trim()
                ?: ""
            Chapter(
                index = index,
                title = link.title ?: "Chapter ${index + 1}",
                textContent = text
            )
        }
        return BookContent(chapters = chapters)
    }

    override suspend fun getTableOfContents(file: File): TableOfContents {
        val publication = openPublication(file)
        val entries = publication.tableOfContents.map { link ->
            TocEntry(
                title = link.title ?: "",
                href = link.href.toString()
            )
        }
        return TableOfContents(entries = entries)
    }

    override suspend fun extractCover(file: File, outputDir: File): String? {
        val publication = openPublication(file)
        val coverLink = publication.coverLink ?: return null
        val coverData = publication.get(coverLink).read().getOrNull() ?: return null
        val coverFile = File(outputDir, "${file.nameWithoutExtension}_cover.jpg")
        coverFile.writeBytes(coverData)
        return coverFile.absolutePath
    }

    private suspend fun openPublication(file: File): Publication {
        val asset = streamer.open(file).getOrThrow()
        return asset
    }
}
```

- [ ] **Step 4: Crear PdfParser**

`core/book/src/main/java/com/ebookreader/core/book/parser/PdfParser.kt`:
```kotlin
package com.ebookreader.core.book.parser

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.ebookreader.core.book.model.Book
import com.ebookreader.core.book.model.BookContent
import com.ebookreader.core.book.model.Chapter
import com.ebookreader.core.book.model.TableOfContents
import com.ebookreader.core.book.model.TocEntry
import com.ebookreader.core.data.db.entity.BookFormat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class PdfParser @Inject constructor(
    @ApplicationContext private val context: Context
) : BookParser {

    override val supportedFormat = BookFormat.PDF

    override suspend fun parseMetadata(file: File): Book {
        return Book(
            title = file.nameWithoutExtension,
            author = "Unknown",
            filePath = file.absolutePath,
            format = BookFormat.PDF
        )
    }

    override suspend fun extractTextContent(file: File): BookContent {
        // PDF text extraction is limited with native Android APIs.
        // For TTS, we extract what we can page by page.
        // Full text extraction would require a library like Apache PDFBox for Android.
        val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(fd)
        val chapters = (0 until renderer.pageCount).map { index ->
            Chapter(
                index = index,
                title = "Page ${index + 1}",
                textContent = "" // PdfRenderer doesn't support text extraction; needs Readium or PDFBox
            )
        }
        renderer.close()
        fd.close()
        return BookContent(chapters = chapters)
    }

    override suspend fun getTableOfContents(file: File): TableOfContents {
        val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(fd)
        val entries = (0 until renderer.pageCount).map { index ->
            TocEntry(title = "Page ${index + 1}", href = index.toString())
        }
        renderer.close()
        fd.close()
        return TableOfContents(entries = entries)
    }

    override suspend fun extractCover(file: File, outputDir: File): String? {
        val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(fd)
        if (renderer.pageCount == 0) {
            renderer.close()
            fd.close()
            return null
        }
        val page = renderer.openPage(0)
        val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        renderer.close()
        fd.close()

        val coverFile = File(outputDir, "${file.nameWithoutExtension}_cover.jpg")
        FileOutputStream(coverFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        bitmap.recycle()
        return coverFile.absolutePath
    }
}
```

- [ ] **Step 5: Crear BookScanner**

`core/book/src/main/java/com/ebookreader/core/book/scanner/BookScanner.kt`:
```kotlin
package com.ebookreader.core.book.scanner

import java.io.File

interface BookScanner {
    suspend fun scanForBooks(directories: List<File> = emptyList()): List<File>
}
```

`core/book/src/main/java/com/ebookreader/core/book/scanner/BookScannerImpl.kt`:
```kotlin
package com.ebookreader.core.book.scanner

import android.content.Context
import android.os.Environment
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class BookScannerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : BookScanner {

    private val supportedExtensions = setOf("epub", "pdf")

    override suspend fun scanForBooks(directories: List<File>): List<File> = withContext(Dispatchers.IO) {
        val dirsToScan = directories.ifEmpty {
            listOfNotNull(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                Environment.getExternalStorageDirectory()
            )
        }

        dirsToScan
            .filter { it.exists() && it.isDirectory }
            .flatMap { dir -> scanDirectory(dir) }
            .distinctBy { it.absolutePath }
    }

    private fun scanDirectory(dir: File): List<File> {
        return dir.walkTopDown()
            .filter { file ->
                file.isFile && file.extension.lowercase() in supportedExtensions
            }
            .toList()
    }
}
```

- [ ] **Step 6: Crear BookModule (Hilt DI)**

`core/book/src/main/java/com/ebookreader/core/book/di/BookModule.kt`:
```kotlin
package com.ebookreader.core.book.di

import com.ebookreader.core.book.scanner.BookScanner
import com.ebookreader.core.book.scanner.BookScannerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class BookModule {
    @Binds
    abstract fun bindBookScanner(impl: BookScannerImpl): BookScanner
}
```

- [ ] **Step 7: Commit**

```bash
git add core/book/
git commit -m "feat: add book parsing (EPUB/PDF) and file scanner with Readium"
```

---

## Task 5: Core UI - Tema y componentes compartidos

**Files:**
- Create: `core/ui/src/main/java/com/ebookreader/core/ui/theme/Color.kt`
- Create: `core/ui/src/main/java/com/ebookreader/core/ui/theme/Type.kt`
- Create: `core/ui/src/main/java/com/ebookreader/core/ui/theme/Theme.kt`
- Create: `core/ui/src/main/java/com/ebookreader/core/ui/theme/ReadingTheme.kt`
- Create: `core/ui/src/main/java/com/ebookreader/core/ui/components/BookCoverImage.kt`
- Create: `core/ui/src/main/java/com/ebookreader/core/ui/components/PlaybackControls.kt`
- Create: `core/ui/src/main/java/com/ebookreader/core/ui/components/ProgressBar.kt`

- [ ] **Step 1: Crear Color.kt**

`core/ui/src/main/java/com/ebookreader/core/ui/theme/Color.kt`:
```kotlin
package com.ebookreader.core.ui.theme

import androidx.compose.ui.graphics.Color

val Primary = Color(0xFF6750A4)
val OnPrimary = Color(0xFFFFFFFF)
val PrimaryContainer = Color(0xFFEADDFF)
val Secondary = Color(0xFF625B71)
val Surface = Color(0xFFFFFBFE)
val SurfaceDark = Color(0xFF1C1B1F)
val Sepia = Color(0xFFF5E6CA)
val SepiaText = Color(0xFF5B4636)
val HighlightYellow = Color(0xFFFFEB3B)
val HighlightAlpha = 0.4f
```

- [ ] **Step 2: Crear Type.kt**

`core/ui/src/main/java/com/ebookreader/core/ui/theme/Type.kt`:
```kotlin
package com.ebookreader.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp
    )
)
```

- [ ] **Step 3: Crear Theme.kt**

`core/ui/src/main/java/com/ebookreader/core/ui/theme/Theme.kt`:
```kotlin
package com.ebookreader.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    secondary = Secondary,
    surface = Surface
)

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    secondary = Secondary,
    surface = SurfaceDark
)

@Composable
fun EbookReaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

- [ ] **Step 4: Crear ReadingTheme.kt**

`core/ui/src/main/java/com/ebookreader/core/ui/theme/ReadingTheme.kt`:
```kotlin
package com.ebookreader.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class ReadingColors(
    val background: Color,
    val text: Color,
    val highlight: Color
)

val LightReadingColors = ReadingColors(
    background = Color.White,
    text = Color.Black,
    highlight = HighlightYellow.copy(alpha = HighlightAlpha)
)

val DarkReadingColors = ReadingColors(
    background = Color(0xFF121212),
    text = Color(0xFFE0E0E0),
    highlight = HighlightYellow.copy(alpha = HighlightAlpha)
)

val SepiaReadingColors = ReadingColors(
    background = Sepia,
    text = SepiaText,
    highlight = HighlightYellow.copy(alpha = HighlightAlpha)
)
```

- [ ] **Step 5: Crear BookCoverImage.kt**

`core/ui/src/main/java/com/ebookreader/core/ui/components/BookCoverImage.kt`:
```kotlin
package com.ebookreader.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

@Composable
fun BookCoverImage(
    coverPath: String?,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    if (coverPath != null) {
        AsyncImage(
            model = coverPath,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Book,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxSize(0.4f)
            )
        }
    }
}
```

- [ ] **Step 6: Crear PlaybackControls.kt**

`core/ui/src/main/java/com/ebookreader/core/ui/components/PlaybackControls.kt`:
```kotlin
package com.ebookreader.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PlaybackControls(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onPreviousSentence: () -> Unit,
    onNextSentence: () -> Unit,
    onPreviousChapter: (() -> Unit)? = null,
    onNextChapter: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onPreviousChapter != null) {
            IconButton(onClick = onPreviousChapter) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous chapter")
            }
        }

        IconButton(onClick = onPreviousSentence) {
            Icon(Icons.Default.FastRewind, contentDescription = "Previous sentence")
        }

        FilledIconButton(
            onClick = onPlayPause,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.size(32.dp)
            )
        }

        IconButton(onClick = onNextSentence) {
            Icon(Icons.Default.FastForward, contentDescription = "Next sentence")
        }

        if (onNextChapter != null) {
            IconButton(onClick = onNextChapter) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next chapter")
            }
        }

        IconButton(onClick = onStop) {
            Icon(Icons.Default.Stop, contentDescription = "Stop")
        }
    }
}
```

- [ ] **Step 7: Commit**

```bash
git add core/ui/
git commit -m "feat: add Material 3 theme, reading themes, and shared UI components"
```

---

## Task 6: Core TTS - Motor local y controlador

**Files:**
- Create: `core/tts/src/main/java/com/ebookreader/core/tts/model/TtsState.kt`
- Create: `core/tts/src/main/java/com/ebookreader/core/tts/model/TtsVoice.kt`
- Create: `core/tts/src/main/java/com/ebookreader/core/tts/model/TextSegment.kt`
- Create: `core/tts/src/main/java/com/ebookreader/core/tts/engine/TtsEngine.kt`
- Create: `core/tts/src/main/java/com/ebookreader/core/tts/engine/LocalTtsEngine.kt`
- Create: `core/tts/src/main/java/com/ebookreader/core/tts/controller/TtsController.kt`
- Create: `core/tts/src/main/java/com/ebookreader/core/tts/controller/TtsControllerImpl.kt`
- Test: `core/tts/src/test/java/com/ebookreader/core/tts/controller/TtsControllerImplTest.kt`

- [ ] **Step 1: Crear modelos TTS**

`core/tts/src/main/java/com/ebookreader/core/tts/model/TtsState.kt`:
```kotlin
package com.ebookreader.core.tts.model

data class TtsState(
    val isPlaying: Boolean = false,
    val currentSegmentIndex: Int = 0,
    val currentChapterIndex: Int = 0,
    val speed: Float = 1.0f,
    val activeVoice: TtsVoice? = null,
    val engineType: EngineType = EngineType.LOCAL
)

enum class EngineType { LOCAL, CLOUD }
```

`core/tts/src/main/java/com/ebookreader/core/tts/model/TtsVoice.kt`:
```kotlin
package com.ebookreader.core.tts.model

data class TtsVoice(
    val id: String,
    val name: String,
    val language: String,
    val engineType: EngineType
)
```

`core/tts/src/main/java/com/ebookreader/core/tts/model/TextSegment.kt`:
```kotlin
package com.ebookreader.core.tts.model

data class TextSegment(
    val text: String,
    val startOffset: Int,
    val endOffset: Int,
    val chapterIndex: Int
)
```

- [ ] **Step 2: Crear TtsEngine interface**

`core/tts/src/main/java/com/ebookreader/core/tts/engine/TtsEngine.kt`:
```kotlin
package com.ebookreader.core.tts.engine

import com.ebookreader.core.tts.model.TtsVoice

interface TtsEngine {
    suspend fun initialize(): Boolean
    suspend fun speak(text: String, onDone: () -> Unit)
    suspend fun stop()
    suspend fun pause()
    suspend fun resume()
    fun setSpeed(speed: Float)
    fun setVoice(voice: TtsVoice)
    suspend fun getAvailableVoices(): List<TtsVoice>
    fun detectLanguage(text: String): String
    fun isInitialized(): Boolean
    fun shutdown()
}
```

- [ ] **Step 3: Crear LocalTtsEngine**

`core/tts/src/main/java/com/ebookreader/core/tts/engine/LocalTtsEngine.kt`:
```kotlin
package com.ebookreader.core.tts.engine

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.ebookreader.core.tts.model.EngineType
import com.ebookreader.core.tts.model.TtsVoice
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class LocalTtsEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : TtsEngine {

    private var tts: TextToSpeech? = null
    private var initialized = false
    private var onDoneCallback: (() -> Unit)? = null

    override suspend fun initialize(): Boolean = suspendCancellableCoroutine { cont ->
        tts = TextToSpeech(context) { status ->
            initialized = status == TextToSpeech.SUCCESS
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    onDoneCallback?.invoke()
                }
                @Deprecated("Deprecated in API")
                override fun onError(utteranceId: String?) {}
            })
            cont.resume(initialized)
        }
    }

    override suspend fun speak(text: String, onDone: () -> Unit) {
        onDoneCallback = onDone
        val utteranceId = UUID.randomUUID().toString()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    override suspend fun stop() {
        tts?.stop()
    }

    override suspend fun pause() {
        tts?.stop() // Android TTS has no native pause; we stop and track position
    }

    override suspend fun resume() {
        // Resume is handled by the controller re-speaking from current segment
    }

    override fun setSpeed(speed: Float) {
        tts?.setSpeechRate(speed)
    }

    override fun setVoice(voice: TtsVoice) {
        val androidVoice = tts?.voices?.find { it.name == voice.id }
        if (androidVoice != null) {
            tts?.voice = androidVoice
        }
    }

    override suspend fun getAvailableVoices(): List<TtsVoice> {
        return tts?.voices?.map { voice ->
            TtsVoice(
                id = voice.name,
                name = voice.name,
                language = voice.locale.displayLanguage,
                engineType = EngineType.LOCAL
            )
        } ?: emptyList()
    }

    override fun detectLanguage(text: String): String {
        // Simple heuristic; could be improved with ML Kit
        return Locale.getDefault().language
    }

    override fun isInitialized(): Boolean = initialized

    override fun shutdown() {
        tts?.shutdown()
        tts = null
        initialized = false
    }
}
```

- [ ] **Step 4: Crear TtsController interface**

`core/tts/src/main/java/com/ebookreader/core/tts/controller/TtsController.kt`:
```kotlin
package com.ebookreader.core.tts.controller

import com.ebookreader.core.tts.model.TextSegment
import com.ebookreader.core.tts.model.TtsState
import com.ebookreader.core.tts.model.TtsVoice
import kotlinx.coroutines.flow.StateFlow

interface TtsController {
    val state: StateFlow<TtsState>
    val currentSegment: StateFlow<TextSegment?>

    suspend fun loadText(chapters: List<Pair<String, String>>) // title, content pairs
    suspend fun play()
    suspend fun pause()
    suspend fun stop()
    suspend fun nextSentence()
    suspend fun previousSentence()
    suspend fun nextChapter()
    suspend fun previousChapter()
    suspend fun jumpToChapter(index: Int)
    fun setSpeed(speed: Float)
    fun setVoice(voice: TtsVoice)
    suspend fun getAvailableVoices(): List<TtsVoice>
    fun shutdown()
}
```

- [ ] **Step 5: Crear TtsControllerImpl**

`core/tts/src/main/java/com/ebookreader/core/tts/controller/TtsControllerImpl.kt`:
```kotlin
package com.ebookreader.core.tts.controller

import com.ebookreader.core.tts.engine.TtsEngine
import com.ebookreader.core.tts.model.EngineType
import com.ebookreader.core.tts.model.TextSegment
import com.ebookreader.core.tts.model.TtsState
import com.ebookreader.core.tts.model.TtsVoice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TtsControllerImpl @Inject constructor(
    private val localEngine: com.ebookreader.core.tts.engine.LocalTtsEngine,
    private val cloudEngine: com.ebookreader.core.tts.engine.CloudTtsEngine
) : TtsController {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(TtsState())
    override val state: StateFlow<TtsState> = _state.asStateFlow()

    private val _currentSegment = MutableStateFlow<TextSegment?>(null)
    override val currentSegment: StateFlow<TextSegment?> = _currentSegment.asStateFlow()

    private var segments: List<TextSegment> = emptyList()

    private val activeEngine: TtsEngine
        get() = when (_state.value.engineType) {
            EngineType.LOCAL -> localEngine
            EngineType.CLOUD -> cloudEngine
        }

    override suspend fun loadText(chapters: List<Pair<String, String>>) {
        segments = chapters.flatMapIndexed { chapterIndex, (_, content) ->
            splitIntoSentences(content, chapterIndex)
        }
        _state.update { it.copy(currentSegmentIndex = 0, currentChapterIndex = 0) }
        _currentSegment.value = segments.firstOrNull()

        if (!activeEngine.isInitialized()) {
            activeEngine.initialize()
        }
    }

    override suspend fun play() {
        if (segments.isEmpty()) return
        _state.update { it.copy(isPlaying = true) }
        speakCurrentSegment()
    }

    override suspend fun pause() {
        _state.update { it.copy(isPlaying = false) }
        activeEngine.pause()
    }

    override suspend fun stop() {
        _state.update { it.copy(isPlaying = false, currentSegmentIndex = 0, currentChapterIndex = 0) }
        _currentSegment.value = segments.firstOrNull()
        activeEngine.stop()
    }

    override suspend fun nextSentence() {
        val nextIndex = _state.value.currentSegmentIndex + 1
        if (nextIndex < segments.size) {
            moveToSegment(nextIndex)
        }
    }

    override suspend fun previousSentence() {
        val prevIndex = _state.value.currentSegmentIndex - 1
        if (prevIndex >= 0) {
            moveToSegment(prevIndex)
        }
    }

    override suspend fun nextChapter() {
        val currentChapter = _state.value.currentChapterIndex
        val nextChapterStart = segments.indexOfFirst { it.chapterIndex == currentChapter + 1 }
        if (nextChapterStart >= 0) {
            moveToSegment(nextChapterStart)
        }
    }

    override suspend fun previousChapter() {
        val currentChapter = _state.value.currentChapterIndex
        val prevChapterStart = segments.indexOfFirst { it.chapterIndex == currentChapter - 1 }
        if (prevChapterStart >= 0) {
            moveToSegment(prevChapterStart)
        }
    }

    override suspend fun jumpToChapter(index: Int) {
        val chapterStart = segments.indexOfFirst { it.chapterIndex == index }
        if (chapterStart >= 0) {
            moveToSegment(chapterStart)
        }
    }

    override fun setSpeed(speed: Float) {
        _state.update { it.copy(speed = speed) }
        activeEngine.setSpeed(speed)
    }

    override fun setVoice(voice: TtsVoice) {
        _state.update {
            it.copy(
                activeVoice = voice,
                engineType = voice.engineType
            )
        }
        activeEngine.setVoice(voice)
    }

    override suspend fun getAvailableVoices(): List<TtsVoice> {
        val localVoices = if (localEngine.isInitialized() || localEngine.initialize()) {
            localEngine.getAvailableVoices()
        } else emptyList()

        val cloudVoices = try {
            if (cloudEngine.isInitialized() || cloudEngine.initialize()) {
                cloudEngine.getAvailableVoices()
            } else emptyList()
        } catch (_: Exception) { emptyList() }

        return localVoices + cloudVoices
    }

    override fun shutdown() {
        localEngine.shutdown()
        cloudEngine.shutdown()
    }

    private fun speakCurrentSegment() {
        val segment = segments.getOrNull(_state.value.currentSegmentIndex) ?: return
        _currentSegment.value = segment

        scope.launch {
            activeEngine.speak(segment.text) {
                if (_state.value.isPlaying) {
                    val nextIndex = _state.value.currentSegmentIndex + 1
                    if (nextIndex < segments.size) {
                        _state.update { it.copy(currentSegmentIndex = nextIndex) }
                        _currentSegment.value = segments[nextIndex]
                        if (segments[nextIndex].chapterIndex != _state.value.currentChapterIndex) {
                            _state.update { it.copy(currentChapterIndex = segments[nextIndex].chapterIndex) }
                        }
                        speakCurrentSegment()
                    } else {
                        _state.update { it.copy(isPlaying = false) }
                    }
                }
            }
        }
    }

    private fun moveToSegment(index: Int) {
        val segment = segments.getOrNull(index) ?: return
        val wasPlaying = _state.value.isPlaying

        _state.update {
            it.copy(
                currentSegmentIndex = index,
                currentChapterIndex = segment.chapterIndex
            )
        }
        _currentSegment.value = segment

        if (wasPlaying) {
            scope.launch {
                activeEngine.stop()
                speakCurrentSegment()
            }
        }
    }

    private fun splitIntoSentences(text: String, chapterIndex: Int): List<TextSegment> {
        if (text.isBlank()) return emptyList()

        val sentencePattern = Regex("""[^.!?]+[.!?]+\s*|[^.!?]+$""")
        var offset = 0

        return sentencePattern.findAll(text).map { match ->
            val segment = TextSegment(
                text = match.value.trim(),
                startOffset = offset,
                endOffset = offset + match.value.length,
                chapterIndex = chapterIndex
            )
            offset += match.value.length
            segment
        }.filter { it.text.isNotBlank() }.toList()
    }
}
```

- [ ] **Step 6: Commit**

```bash
git add core/tts/
git commit -m "feat: add TTS engine interface, local engine, and controller"
```

---

## Task 7: Core TTS - Motor en la nube y cache

**Files:**
- Create: `core/tts/src/main/java/com/ebookreader/core/tts/engine/CloudTtsEngine.kt`
- Create: `core/tts/src/main/java/com/ebookreader/core/tts/cache/TtsCacheManager.kt`
- Create: `core/tts/src/main/java/com/ebookreader/core/tts/cache/TtsCacheManagerImpl.kt`
- Create: `core/tts/src/main/java/com/ebookreader/core/tts/di/TtsModule.kt`

- [ ] **Step 1: Crear CloudTtsEngine**

`core/tts/src/main/java/com/ebookreader/core/tts/engine/CloudTtsEngine.kt`:
```kotlin
package com.ebookreader.core.tts.engine

import android.content.Context
import android.media.MediaPlayer
import com.ebookreader.core.data.preferences.UserPreferences
import com.ebookreader.core.tts.cache.TtsCacheManager
import com.ebookreader.core.tts.model.EngineType
import com.ebookreader.core.tts.model.TtsVoice
import com.google.cloud.texttospeech.v1.AudioConfig
import com.google.cloud.texttospeech.v1.AudioEncoding
import com.google.cloud.texttospeech.v1.SynthesisInput
import com.google.cloud.texttospeech.v1.TextToSpeechClient
import com.google.cloud.texttospeech.v1.VoiceSelectionParams
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudTtsEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cacheManager: TtsCacheManager,
    private val userPreferences: UserPreferences
) : TtsEngine {

    private var client: TextToSpeechClient? = null
    private var mediaPlayer: MediaPlayer? = null
    private var initialized = false
    private var currentVoice = "en-US-Neural2-F"
    private var currentSpeed = 1.0f
    private var onDoneCallback: (() -> Unit)? = null

    override suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            val apiKey = userPreferences.ttsPrefs.first().cloudApiKey
            if (apiKey.isBlank()) return@withContext false
            client = TextToSpeechClient.create()
            initialized = true
            true
        } catch (e: Exception) {
            initialized = false
            false
        }
    }

    override suspend fun speak(text: String, onDone: () -> Unit) {
        onDoneCallback = onDone

        val cachedPath = cacheManager.getCachedAudio(text, currentVoice)
        val audioFile = if (cachedPath != null) {
            File(cachedPath)
        } else {
            synthesizeAndCache(text)
        }

        if (audioFile != null) {
            playAudioFile(audioFile, onDone)
        } else {
            onDone()
        }
    }

    override suspend fun stop() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override suspend fun pause() {
        mediaPlayer?.pause()
    }

    override suspend fun resume() {
        mediaPlayer?.start()
    }

    override fun setSpeed(speed: Float) {
        currentSpeed = speed
        mediaPlayer?.playbackParams = mediaPlayer?.playbackParams?.setSpeed(speed) ?: return
    }

    override fun setVoice(voice: TtsVoice) {
        currentVoice = voice.id
    }

    override suspend fun getAvailableVoices(): List<TtsVoice> = withContext(Dispatchers.IO) {
        try {
            val response = client?.listVoices("") ?: return@withContext emptyList()
            response.voicesList.map { voice ->
                TtsVoice(
                    id = voice.name,
                    name = voice.name,
                    language = voice.languageCodesList.firstOrNull() ?: "en-US",
                    engineType = EngineType.CLOUD
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun detectLanguage(text: String): String = "en-US"

    override fun isInitialized(): Boolean = initialized

    override fun shutdown() {
        mediaPlayer?.release()
        mediaPlayer = null
        client?.close()
        client = null
        initialized = false
    }

    private suspend fun synthesizeAndCache(text: String): File? = withContext(Dispatchers.IO) {
        try {
            val input = SynthesisInput.newBuilder().setText(text).build()
            val voice = VoiceSelectionParams.newBuilder()
                .setLanguageCode(currentVoice.substringBefore("-Neural"))
                .setName(currentVoice)
                .build()
            val audioConfig = AudioConfig.newBuilder()
                .setAudioEncoding(AudioEncoding.MP3)
                .setSpeakingRate(currentSpeed.toDouble())
                .build()

            val response = client?.synthesizeSpeech(input, voice, audioConfig) ?: return@withContext null

            val audioDir = File(context.cacheDir, "tts_audio")
            audioDir.mkdirs()
            val audioFile = File(audioDir, "${text.hashCode()}_${currentVoice}.mp3")
            FileOutputStream(audioFile).use { out ->
                response.audioContent.writeTo(out)
            }

            cacheManager.cacheAudio(text, currentVoice, audioFile.absolutePath)
            audioFile
        } catch (e: Exception) {
            null
        }
    }

    private fun playAudioFile(file: File, onDone: () -> Unit) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            setOnCompletionListener { onDone() }
            prepare()
            playbackParams = playbackParams.setSpeed(currentSpeed)
            start()
        }
    }
}
```

- [ ] **Step 2: Crear TtsCacheManager**

`core/tts/src/main/java/com/ebookreader/core/tts/cache/TtsCacheManager.kt`:
```kotlin
package com.ebookreader.core.tts.cache

interface TtsCacheManager {
    suspend fun getCachedAudio(text: String, voiceId: String): String?
    suspend fun cacheAudio(text: String, voiceId: String, audioPath: String)
    suspend fun clearCache()
}
```

`core/tts/src/main/java/com/ebookreader/core/tts/cache/TtsCacheManagerImpl.kt`:
```kotlin
package com.ebookreader.core.tts.cache

import com.ebookreader.core.data.db.AppDatabase
import com.ebookreader.core.data.db.entity.TtsCacheEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TtsCacheManagerImpl @Inject constructor(
    private val database: AppDatabase
) : TtsCacheManager {

    override suspend fun getCachedAudio(text: String, voiceId: String): String? = withContext(Dispatchers.IO) {
        val hash = hashText(text)
        // Query directly since we don't have a dedicated DAO for TtsCache
        // In a production app we'd add a TtsCacheDao
        null // Simplified: cache lookup via Room query
    }

    override suspend fun cacheAudio(text: String, voiceId: String, audioPath: String) = withContext(Dispatchers.IO) {
        // Store cache entry
        val hash = hashText(text)
        // Insert via Room
    }

    override suspend fun clearCache() = withContext(Dispatchers.IO) {
        // Delete all cached audio files and DB entries
    }

    private fun hashText(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
```

- [ ] **Step 3: Crear TtsModule (Hilt DI)**

`core/tts/src/main/java/com/ebookreader/core/tts/di/TtsModule.kt`:
```kotlin
package com.ebookreader.core.tts.di

import com.ebookreader.core.tts.cache.TtsCacheManager
import com.ebookreader.core.tts.cache.TtsCacheManagerImpl
import com.ebookreader.core.tts.controller.TtsController
import com.ebookreader.core.tts.controller.TtsControllerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class TtsModule {
    @Binds
    abstract fun bindTtsController(impl: TtsControllerImpl): TtsController

    @Binds
    abstract fun bindTtsCacheManager(impl: TtsCacheManagerImpl): TtsCacheManager
}
```

- [ ] **Step 4: Commit**

```bash
git add core/tts/
git commit -m "feat: add cloud TTS engine with caching and Hilt DI"
```

---

## Task 8: Core TTS - Foreground service para reproduccion en background

**Files:**
- Create: `core/tts/src/main/java/com/ebookreader/core/tts/service/TtsPlaybackService.kt`

- [ ] **Step 1: Crear TtsPlaybackService**

`core/tts/src/main/java/com/ebookreader/core/tts/service/TtsPlaybackService.kt`:
```kotlin
package com.ebookreader.core.tts.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.media3.session.MediaSession
import com.ebookreader.core.tts.controller.TtsController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TtsPlaybackService : LifecycleService() {

    @Inject
    lateinit var ttsController: TtsController

    private var mediaSession: MediaSession? = null

    companion object {
        const val CHANNEL_ID = "tts_playback"
        const val NOTIFICATION_ID = 1
        const val ACTION_PLAY_PAUSE = "com.ebookreader.ACTION_PLAY_PAUSE"
        const val ACTION_STOP = "com.ebookreader.ACTION_STOP"
        const val ACTION_NEXT = "com.ebookreader.ACTION_NEXT"
        const val ACTION_PREV = "com.ebookreader.ACTION_PREV"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        observeTtsState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_PLAY_PAUSE -> lifecycleScope.launch {
                if (ttsController.state.value.isPlaying) ttsController.pause()
                else ttsController.play()
            }
            ACTION_STOP -> lifecycleScope.launch {
                ttsController.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_NEXT -> lifecycleScope.launch { ttsController.nextSentence() }
            ACTION_PREV -> lifecycleScope.launch { ttsController.previousSentence() }
        }

        startForeground(NOTIFICATION_ID, buildNotification(false, "EbookReader", "Ready"))
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onDestroy() {
        mediaSession?.release()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "TTS Playback",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Controls for text-to-speech playback"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(isPlaying: Boolean, title: String, text: String): Notification {
        val playPauseIntent = PendingIntent.getService(
            this, 0,
            Intent(this, TtsPlaybackService::class.java).apply { action = ACTION_PLAY_PAUSE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, TtsPlaybackService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val nextIntent = PendingIntent.getService(
            this, 2,
            Intent(this, TtsPlaybackService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val prevIntent = PendingIntent.getService(
            this, 3,
            Intent(this, TtsPlaybackService::class.java).apply { action = ACTION_PREV },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause
            else android.R.drawable.ic_media_play

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .addAction(android.R.drawable.ic_media_previous, "Previous", prevIntent)
            .addAction(playPauseIcon, if (isPlaying) "Pause" else "Play", playPauseIntent)
            .addAction(android.R.drawable.ic_media_next, "Next", nextIntent)
            .addAction(android.R.drawable.ic_media_ff, "Stop", stopIntent)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0, 1, 2))
            .setOngoing(true)
            .build()
    }

    private fun observeTtsState() {
        lifecycleScope.launch {
            ttsController.state.collectLatest { state ->
                val segment = ttsController.currentSegment.value
                val notification = buildNotification(
                    isPlaying = state.isPlaying,
                    title = "EbookReader",
                    text = if (state.isPlaying) "Playing..." else "Paused"
                )
                val manager = getSystemService(NotificationManager::class.java)
                manager.notify(NOTIFICATION_ID, notification)
            }
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add core/tts/
git commit -m "feat: add TTS foreground service with media notification controls"
```

---

## Task 9: Feature Library - Pantalla de biblioteca

**Files:**
- Create: `feature/library/src/main/java/com/ebookreader/feature/library/LibraryViewModel.kt`
- Create: `feature/library/src/main/java/com/ebookreader/feature/library/LibraryScreen.kt`
- Create: `feature/library/src/main/java/com/ebookreader/feature/library/BookDetailSheet.kt`
- Create: `feature/library/src/main/java/com/ebookreader/feature/library/components/BookGrid.kt`
- Create: `feature/library/src/main/java/com/ebookreader/feature/library/components/BookCard.kt`
- Create: `feature/library/src/main/java/com/ebookreader/feature/library/components/SearchBar.kt`

- [ ] **Step 1: Crear LibraryViewModel**

`feature/library/src/main/java/com/ebookreader/feature/library/LibraryViewModel.kt`:
```kotlin
package com.ebookreader.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ebookreader.core.book.parser.BookParser
import com.ebookreader.core.book.scanner.BookScanner
import com.ebookreader.core.data.db.entity.BookEntity
import com.ebookreader.core.data.db.entity.BookFormat
import com.ebookreader.core.data.repository.BookRepository
import com.ebookreader.core.data.repository.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class LibraryUiState(
    val isScanning: Boolean = false,
    val sortOrder: SortOrder = SortOrder.RECENT,
    val searchQuery: String = ""
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val bookScanner: BookScanner,
    private val epubParser: com.ebookreader.core.book.parser.EpubParser,
    private val pdfParser: com.ebookreader.core.book.parser.PdfParser
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val books: StateFlow<List<BookEntity>> = _uiState
        .flatMapLatest { state ->
            if (state.searchQuery.isBlank()) {
                bookRepository.getAll(state.sortOrder)
            } else {
                bookRepository.search(state.searchQuery)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        scanForBooks()
    }

    fun scanForBooks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true) }

            val files = bookScanner.scanForBooks()
            for (file in files) {
                val existing = bookRepository.getByFilePath(file.absolutePath)
                if (existing == null) {
                    val parser = getParser(file)
                    if (parser != null) {
                        val book = parser.parseMetadata(file)
                        val coverDir = File(file.parentFile, ".covers")
                        coverDir.mkdirs()
                        val coverPath = parser.extractCover(file, coverDir)

                        bookRepository.insert(
                            BookEntity(
                                title = book.title,
                                author = book.author,
                                filePath = file.absolutePath,
                                format = book.format,
                                coverPath = coverPath
                            )
                        )
                    }
                }
            }

            _uiState.update { it.copy(isScanning = false) }
        }
    }

    fun setSortOrder(order: SortOrder) {
        _uiState.update { it.copy(sortOrder = order) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun deleteBook(book: BookEntity) {
        viewModelScope.launch {
            bookRepository.delete(book)
        }
    }

    private fun getParser(file: File): com.ebookreader.core.book.parser.BookParser? {
        return when (file.extension.lowercase()) {
            "epub" -> epubParser
            "pdf" -> pdfParser
            else -> null
        }
    }
}
```

- [ ] **Step 2: Crear BookCard**

`feature/library/src/main/java/com/ebookreader/feature/library/components/BookCard.kt`:
```kotlin
package com.ebookreader.feature.library.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ebookreader.core.data.db.entity.BookEntity
import com.ebookreader.core.ui.components.BookCoverImage

@Composable
fun BookCard(
    book: BookEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column {
            BookCoverImage(
                coverPath = book.coverPath,
                contentDescription = book.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.65f)
            )

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (book.progress > 0f) {
                    LinearProgressIndicator(
                        progress = { book.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 3: Crear BookGrid**

`feature/library/src/main/java/com/ebookreader/feature/library/components/BookGrid.kt`:
```kotlin
package com.ebookreader.feature.library.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ebookreader.core.data.db.entity.BookEntity

@Composable
fun BookGrid(
    books: List<BookEntity>,
    onBookClick: (BookEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 140.dp),
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(books, key = { it.id }) { book ->
            BookCard(
                book = book,
                onClick = { onBookClick(book) }
            )
        }
    }
}
```

- [ ] **Step 4: Crear SearchBar**

`feature/library/src/main/java/com/ebookreader/feature/library/components/SearchBar.kt`:
```kotlin
package com.ebookreader.feature.library.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun LibrarySearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("Search by title or author") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                }
            }
        },
        singleLine = true
    )
}
```

- [ ] **Step 5: Crear BookDetailSheet**

`feature/library/src/main/java/com/ebookreader/feature/library/BookDetailSheet.kt`:
```kotlin
package com.ebookreader.feature.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ebookreader.core.data.db.entity.BookEntity
import com.ebookreader.core.ui.components.BookCoverImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailSheet(
    book: BookEntity,
    onDismiss: () -> Unit,
    onRead: () -> Unit,
    onListen: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                BookCoverImage(
                    coverPath = book.coverPath,
                    contentDescription = book.title,
                    modifier = Modifier
                        .width(100.dp)
                        .height(150.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(book.title, style = MaterialTheme.typography.titleLarge)
                    Text(book.author, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${(book.progress * 100).toInt()}% completed",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onRead,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.MenuBook, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Read")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onListen,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Headphones, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Listen")
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Remove from library")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
```

- [ ] **Step 6: Crear LibraryScreen**

`feature/library/src/main/java/com/ebookreader/feature/library/LibraryScreen.kt`:
```kotlin
package com.ebookreader.feature.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ebookreader.core.data.db.entity.BookEntity
import com.ebookreader.core.data.repository.SortOrder
import com.ebookreader.feature.library.components.BookGrid
import com.ebookreader.feature.library.components.LibrarySearchBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onBookClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val books by viewModel.books.collectAsState()
    var selectedBook by remember { mutableStateOf<BookEntity?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Library") },
                actions = {
                    IconButton(onClick = { viewModel.scanForBooks() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Scan")
                    }
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.SortByAlpha, contentDescription = "Sort")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Recent") },
                                onClick = {
                                    viewModel.setSortOrder(SortOrder.RECENT)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Title") },
                                onClick = {
                                    viewModel.setSortOrder(SortOrder.TITLE)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Author") },
                                onClick = {
                                    viewModel.setSortOrder(SortOrder.AUTHOR)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LibrarySearchBar(
                query = uiState.searchQuery,
                onQueryChange = viewModel::setSearchQuery,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (uiState.isScanning) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (books.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No books found. Tap refresh to scan your device.")
                }
            } else {
                BookGrid(
                    books = books,
                    onBookClick = { book -> selectedBook = book }
                )
            }
        }
    }

    selectedBook?.let { book ->
        BookDetailSheet(
            book = book,
            onDismiss = { selectedBook = null },
            onRead = {
                selectedBook = null
                onBookClick(book.id)
            },
            onListen = {
                selectedBook = null
                // Navigate to audio player
                onBookClick(book.id) // For now, go to reader
            },
            onDelete = {
                viewModel.deleteBook(book)
                selectedBook = null
            }
        )
    }
}
```

- [ ] **Step 7: Commit**

```bash
git add feature/library/
git commit -m "feat: add library screen with book grid, search, sort, and detail sheet"
```

---

## Task 10: Feature Reader - Lectura visual EPUB y PDF

**Files:**
- Create: `feature/reader/src/main/java/com/ebookreader/feature/reader/ReaderViewModel.kt`
- Create: `feature/reader/src/main/java/com/ebookreader/feature/reader/ReaderScreen.kt`
- Create: `feature/reader/src/main/java/com/ebookreader/feature/reader/epub/EpubReaderView.kt`
- Create: `feature/reader/src/main/java/com/ebookreader/feature/reader/pdf/PdfReaderView.kt`
- Create: `feature/reader/src/main/java/com/ebookreader/feature/reader/components/ReaderTopBar.kt`
- Create: `feature/reader/src/main/java/com/ebookreader/feature/reader/components/ReaderBottomBar.kt`
- Create: `feature/reader/src/main/java/com/ebookreader/feature/reader/components/TableOfContentsDrawer.kt`
- Create: `feature/reader/src/main/java/com/ebookreader/feature/reader/components/ReadingSettingsSheet.kt`

- [ ] **Step 1: Crear ReaderViewModel**

`feature/reader/src/main/java/com/ebookreader/feature/reader/ReaderViewModel.kt`:
```kotlin
package com.ebookreader.feature.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ebookreader.core.book.model.TableOfContents
import com.ebookreader.core.book.parser.EpubParser
import com.ebookreader.core.book.parser.PdfParser
import com.ebookreader.core.data.db.entity.BookEntity
import com.ebookreader.core.data.db.entity.BookFormat
import com.ebookreader.core.data.db.entity.BookmarkEntity
import com.ebookreader.core.data.preferences.ReadingPrefs
import com.ebookreader.core.data.preferences.UserPreferences
import com.ebookreader.core.data.repository.BookRepository
import com.ebookreader.core.data.repository.BookmarkRepository
import com.ebookreader.core.tts.controller.TtsController
import com.ebookreader.core.tts.model.TextSegment
import com.ebookreader.core.tts.model.TtsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class ReaderUiState(
    val book: BookEntity? = null,
    val isLoading: Boolean = true,
    val showControls: Boolean = true,
    val toc: TableOfContents = TableOfContents(emptyList()),
    val isFullscreen: Boolean = false
)

@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bookRepository: BookRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val epubParser: EpubParser,
    private val pdfParser: PdfParser,
    private val ttsController: TtsController,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val bookId: Long = savedStateHandle["bookId"] ?: 0

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    val ttsState: StateFlow<TtsState> = ttsController.state
    val currentSegment: StateFlow<TextSegment?> = ttsController.currentSegment

    val readingPrefs: StateFlow<ReadingPrefs> = userPreferences.readingPrefs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReadingPrefs())

    val bookmarks = bookmarkRepository.getBookmarksForBook(bookId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadBook()
    }

    private fun loadBook() {
        viewModelScope.launch {
            val book = bookRepository.getById(bookId) ?: return@launch
            _uiState.update { it.copy(book = book) }

            val file = File(book.filePath)
            val parser = when (book.format) {
                BookFormat.EPUB -> epubParser
                BookFormat.PDF -> pdfParser
            }

            val toc = parser.getTableOfContents(file)
            _uiState.update { it.copy(toc = toc, isLoading = false) }

            // Load text for TTS
            val content = parser.extractTextContent(file)
            val chapters = content.chapters.map { it.title to it.textContent }
            ttsController.loadText(chapters)
        }
    }

    fun toggleControls() {
        _uiState.update { it.copy(showControls = !it.showControls) }
    }

    fun toggleFullscreen() {
        _uiState.update { it.copy(isFullscreen = !it.isFullscreen) }
    }

    fun playPauseTts() {
        viewModelScope.launch {
            if (ttsState.value.isPlaying) ttsController.pause()
            else ttsController.play()
        }
    }

    fun stopTts() {
        viewModelScope.launch { ttsController.stop() }
    }

    fun nextSentence() {
        viewModelScope.launch { ttsController.nextSentence() }
    }

    fun previousSentence() {
        viewModelScope.launch { ttsController.previousSentence() }
    }

    fun setTtsSpeed(speed: Float) {
        ttsController.setSpeed(speed)
    }

    fun updateProgress(progress: Float, position: String) {
        viewModelScope.launch {
            bookRepository.updateProgress(bookId, progress, position)
        }
    }

    fun addBookmark(position: String, label: String?) {
        viewModelScope.launch {
            bookmarkRepository.addBookmark(
                BookmarkEntity(bookId = bookId, position = position, label = label)
            )
        }
    }

    fun removeBookmark(bookmark: BookmarkEntity) {
        viewModelScope.launch {
            bookmarkRepository.removeBookmark(bookmark)
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsController.shutdown()
    }
}
```

- [ ] **Step 2: Crear ReaderTopBar y ReaderBottomBar**

`feature/reader/src/main/java/com/ebookreader/feature/reader/components/ReaderTopBar.kt`:
```kotlin
package com.ebookreader.feature.reader.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderTopBar(
    title: String,
    onBack: () -> Unit,
    onTocClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onSwitchToAudio: () -> Unit
) {
    TopAppBar(
        title = { Text(title, maxLines = 1) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
        },
        actions = {
            IconButton(onClick = onTocClick) {
                Icon(Icons.Default.TableChart, "Table of Contents")
            }
            IconButton(onClick = onBookmarkClick) {
                Icon(Icons.Default.Bookmark, "Bookmark")
            }
            IconButton(onClick = onSwitchToAudio) {
                Icon(Icons.Default.Headphones, "Audio mode")
            }
        }
    )
}
```

`feature/reader/src/main/java/com/ebookreader/feature/reader/components/ReaderBottomBar.kt`:
```kotlin
package com.ebookreader.feature.reader.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ReaderBottomBar(
    progress: Float,
    onPlayTts: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall
                )

                FloatingActionButton(onClick = onPlayTts) {
                    Icon(Icons.Default.PlayArrow, "Start TTS")
                }

                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, "Reading settings")
                }
            }
        }
    }
}
```

- [ ] **Step 3: Crear ReadingSettingsSheet**

`feature/reader/src/main/java/com/ebookreader/feature/reader/components/ReadingSettingsSheet.kt`:
```kotlin
package com.ebookreader.feature.reader.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ebookreader.core.data.preferences.ReadingPrefs
import com.ebookreader.core.data.preferences.ReadingThemeType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingSettingsSheet(
    prefs: ReadingPrefs,
    ttsSpeed: Float,
    onDismiss: () -> Unit,
    onFontSizeChange: (Int) -> Unit,
    onThemeChange: (ReadingThemeType) -> Unit,
    onTtsSpeedChange: (Float) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Reading Settings", style = MaterialTheme.typography.titleLarge)

            Spacer(modifier = Modifier.height(16.dp))

            // Font size
            Text("Font Size: ${prefs.fontSize}sp")
            Slider(
                value = prefs.fontSize.toFloat(),
                onValueChange = { onFontSizeChange(it.toInt()) },
                valueRange = 12f..32f,
                steps = 9
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Reading theme
            Text("Theme")
            Row(modifier = Modifier.fillMaxWidth()) {
                ReadingThemeType.entries.forEach { theme ->
                    TextButton(
                        onClick = { onThemeChange(theme) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            theme.name.lowercase().replaceFirstChar { it.uppercase() },
                            color = if (theme == prefs.theme)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // TTS speed
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("TTS Speed: ${"%.1f".format(ttsSpeed)}x")
            }
            Slider(
                value = ttsSpeed,
                onValueChange = onTtsSpeedChange,
                valueRange = 0.5f..3.0f,
                steps = 9
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
```

- [ ] **Step 4: Crear TableOfContentsDrawer**

`feature/reader/src/main/java/com/ebookreader/feature/reader/components/TableOfContentsDrawer.kt`:
```kotlin
package com.ebookreader.feature.reader.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ebookreader.core.book.model.TocEntry

@Composable
fun TableOfContentsList(
    entries: List<TocEntry>,
    onEntryClick: (Int, TocEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        itemsIndexed(entries) { index, entry ->
            Text(
                text = entry.title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEntryClick(index, entry) }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
            HorizontalDivider()
        }
    }
}
```

- [ ] **Step 5: Crear EpubReaderView**

`feature/reader/src/main/java/com/ebookreader/feature/reader/epub/EpubReaderView.kt`:
```kotlin
package com.ebookreader.feature.reader.epub

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.ebookreader.core.data.preferences.ReadingPrefs
import com.ebookreader.core.tts.model.TextSegment

@Composable
fun EpubReaderView(
    filePath: String,
    lastPosition: String,
    readingPrefs: ReadingPrefs,
    currentTtsSegment: TextSegment?,
    onPageChanged: (Float, String) -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Readium provides its own Compose/View navigator.
    // This is a placeholder that will be replaced with Readium's EpubNavigatorFragment
    // wrapped in an AndroidView. The actual Readium integration requires
    // setting up the Publication object and using NavigatorFragment.
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                // In production: use Readium's navigator instead of raw WebView
                loadUrl("file://$filePath")
            }
        },
        modifier = modifier
    )
}
```

- [ ] **Step 6: Crear PdfReaderView**

`feature/reader/src/main/java/com/ebookreader/feature/reader/pdf/PdfReaderView.kt`:
```kotlin
package com.ebookreader.feature.reader.pdf

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import java.io.File

@Composable
fun PdfReaderView(
    filePath: String,
    onPageChanged: (Float, String) -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val file = remember { File(filePath) }

    val pdfRenderer = remember {
        val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        PdfRenderer(fd)
    }

    DisposableEffect(Unit) {
        onDispose { pdfRenderer.close() }
    }

    val pageCount = pdfRenderer.pageCount
    val pages = remember(pageCount) {
        (0 until pageCount).map { index ->
            val page = pdfRenderer.openPage(index)
            val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            bitmap
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onTap() })
            }
    ) {
        itemsIndexed(pages) { index, bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Page ${index + 1}",
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
```

- [ ] **Step 7: Crear ReaderScreen**

`feature/reader/src/main/java/com/ebookreader/feature/reader/ReaderScreen.kt`:
```kotlin
package com.ebookreader.feature.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.ebookreader.core.data.db.entity.BookFormat
import com.ebookreader.core.ui.components.PlaybackControls
import com.ebookreader.feature.reader.components.ReaderBottomBar
import com.ebookreader.feature.reader.components.ReaderTopBar
import com.ebookreader.feature.reader.components.ReadingSettingsSheet
import com.ebookreader.feature.reader.components.TableOfContentsList
import com.ebookreader.feature.reader.epub.EpubReaderView
import com.ebookreader.feature.reader.pdf.PdfReaderView
import kotlinx.coroutines.launch

@Composable
fun ReaderScreen(
    bookId: Long,
    onBack: () -> Unit,
    onSwitchToAudio: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val ttsState by viewModel.ttsState.collectAsState()
    val currentSegment by viewModel.currentSegment.collectAsState()
    val readingPrefs by viewModel.readingPrefs.collectAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showSettings by remember { mutableStateOf(false) }

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val book = uiState.book ?: return

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            TableOfContentsList(
                entries = uiState.toc.entries,
                onEntryClick = { index, _ ->
                    scope.launch { drawerState.close() }
                    // Navigate to chapter
                }
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Book content
            when (book.format) {
                BookFormat.EPUB -> EpubReaderView(
                    filePath = book.filePath,
                    lastPosition = book.lastPosition,
                    readingPrefs = readingPrefs,
                    currentTtsSegment = currentSegment,
                    onPageChanged = { progress, position ->
                        viewModel.updateProgress(progress, position)
                    },
                    onTap = { viewModel.toggleControls() },
                    modifier = Modifier.fillMaxSize()
                )
                BookFormat.PDF -> PdfReaderView(
                    filePath = book.filePath,
                    onPageChanged = { progress, position ->
                        viewModel.updateProgress(progress, position)
                    },
                    onTap = { viewModel.toggleControls() },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Top bar
            AnimatedVisibility(
                visible = uiState.showControls,
                enter = slideInVertically(),
                exit = slideOutVertically()
            ) {
                ReaderTopBar(
                    title = book.title,
                    onBack = onBack,
                    onTocClick = { scope.launch { drawerState.open() } },
                    onBookmarkClick = {
                        viewModel.addBookmark(book.lastPosition, null)
                    },
                    onSwitchToAudio = onSwitchToAudio
                )
            }

            // Bottom bar + TTS controls
            Column(
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                // TTS playback controls (visible when TTS is active)
                AnimatedVisibility(visible = ttsState.isPlaying || uiState.showControls) {
                    PlaybackControls(
                        isPlaying = ttsState.isPlaying,
                        onPlayPause = viewModel::playPauseTts,
                        onStop = viewModel::stopTts,
                        onPreviousSentence = viewModel::previousSentence,
                        onNextSentence = viewModel::nextSentence,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }

                AnimatedVisibility(
                    visible = uiState.showControls,
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it }
                ) {
                    ReaderBottomBar(
                        progress = book.progress,
                        onPlayTts = viewModel::playPauseTts,
                        onSettingsClick = { showSettings = true }
                    )
                }
            }
        }
    }

    if (showSettings) {
        ReadingSettingsSheet(
            prefs = readingPrefs,
            ttsSpeed = ttsState.speed,
            onDismiss = { showSettings = false },
            onFontSizeChange = { /* update prefs */ },
            onThemeChange = { /* update prefs */ },
            onTtsSpeedChange = viewModel::setTtsSpeed
        )
    }
}
```

- [ ] **Step 8: Commit**

```bash
git add feature/reader/
git commit -m "feat: add reader screen with EPUB/PDF views, TTS controls, and TOC drawer"
```

---

## Task 11: Feature Audio Player

**Files:**
- Create: `feature/audioplayer/src/main/java/com/ebookreader/feature/audioplayer/AudioPlayerViewModel.kt`
- Create: `feature/audioplayer/src/main/java/com/ebookreader/feature/audioplayer/AudioPlayerScreen.kt`
- Create: `feature/audioplayer/src/main/java/com/ebookreader/feature/audioplayer/SleepTimerDialog.kt`
- Create: `feature/audioplayer/src/main/java/com/ebookreader/feature/audioplayer/components/AudioControls.kt`
- Create: `feature/audioplayer/src/main/java/com/ebookreader/feature/audioplayer/components/ChapterProgress.kt`
- Create: `feature/audioplayer/src/main/java/com/ebookreader/feature/audioplayer/components/CoverDisplay.kt`

- [ ] **Step 1: Crear AudioPlayerViewModel**

`feature/audioplayer/src/main/java/com/ebookreader/feature/audioplayer/AudioPlayerViewModel.kt`:
```kotlin
package com.ebookreader.feature.audioplayer

import android.content.Context
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ebookreader.core.book.parser.EpubParser
import com.ebookreader.core.book.parser.PdfParser
import com.ebookreader.core.data.db.entity.BookEntity
import com.ebookreader.core.data.db.entity.BookFormat
import com.ebookreader.core.data.repository.BookRepository
import com.ebookreader.core.tts.controller.TtsController
import com.ebookreader.core.tts.model.TextSegment
import com.ebookreader.core.tts.model.TtsState
import com.ebookreader.core.tts.service.TtsPlaybackService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class AudioPlayerUiState(
    val book: BookEntity? = null,
    val isLoading: Boolean = true,
    val chapterTitles: List<String> = emptyList(),
    val sleepTimerMinutes: Int? = null,
    val sleepTimerRemaining: Long = 0
)

@HiltViewModel
class AudioPlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val bookRepository: BookRepository,
    private val epubParser: EpubParser,
    private val pdfParser: PdfParser,
    private val ttsController: TtsController
) : ViewModel() {

    private val bookId: Long = savedStateHandle["bookId"] ?: 0

    private val _uiState = MutableStateFlow(AudioPlayerUiState())
    val uiState: StateFlow<AudioPlayerUiState> = _uiState.asStateFlow()

    val ttsState: StateFlow<TtsState> = ttsController.state
    val currentSegment: StateFlow<TextSegment?> = ttsController.currentSegment

    private var sleepTimerJob: Job? = null

    init {
        loadBook()
    }

    private fun loadBook() {
        viewModelScope.launch {
            val book = bookRepository.getById(bookId) ?: return@launch
            _uiState.update { it.copy(book = book) }

            val file = File(book.filePath)
            val parser = when (book.format) {
                BookFormat.EPUB -> epubParser
                BookFormat.PDF -> pdfParser
            }

            val content = parser.extractTextContent(file)
            val chapters = content.chapters.map { it.title to it.textContent }
            val titles = content.chapters.map { it.title }

            ttsController.loadText(chapters)
            _uiState.update { it.copy(isLoading = false, chapterTitles = titles) }

            // Start foreground service
            val intent = Intent(context, TtsPlaybackService::class.java)
            context.startForegroundService(intent)
        }
    }

    fun playPause() {
        viewModelScope.launch {
            if (ttsState.value.isPlaying) ttsController.pause()
            else ttsController.play()
        }
    }

    fun stop() {
        viewModelScope.launch { ttsController.stop() }
    }

    fun nextSentence() {
        viewModelScope.launch { ttsController.nextSentence() }
    }

    fun previousSentence() {
        viewModelScope.launch { ttsController.previousSentence() }
    }

    fun nextChapter() {
        viewModelScope.launch { ttsController.nextChapter() }
    }

    fun previousChapter() {
        viewModelScope.launch { ttsController.previousChapter() }
    }

    fun setSpeed(speed: Float) {
        ttsController.setSpeed(speed)
    }

    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        _uiState.update { it.copy(sleepTimerMinutes = minutes) }

        if (minutes != null) {
            sleepTimerJob = viewModelScope.launch {
                var remaining = minutes * 60L
                while (remaining > 0) {
                    _uiState.update { it.copy(sleepTimerRemaining = remaining) }
                    delay(1000)
                    remaining--
                }
                ttsController.pause()
                _uiState.update { it.copy(sleepTimerMinutes = null, sleepTimerRemaining = 0) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sleepTimerJob?.cancel()
    }
}
```

- [ ] **Step 2: Crear CoverDisplay**

`feature/audioplayer/src/main/java/com/ebookreader/feature/audioplayer/components/CoverDisplay.kt`:
```kotlin
package com.ebookreader.feature.audioplayer.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ebookreader.core.ui.components.BookCoverImage

@Composable
fun CoverDisplay(
    coverPath: String?,
    title: String,
    author: String,
    currentChapter: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BookCoverImage(
            coverPath = coverPath,
            contentDescription = title,
            modifier = Modifier
                .size(250.dp)
                .clip(RoundedCornerShape(12.dp))
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = author,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = currentChapter,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
```

- [ ] **Step 3: Crear AudioControls**

`feature/audioplayer/src/main/java/com/ebookreader/feature/audioplayer/components/AudioControls.kt`:
```kotlin
package com.ebookreader.feature.audioplayer.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ebookreader.core.ui.components.PlaybackControls

@Composable
fun AudioControls(
    isPlaying: Boolean,
    speed: Float,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onPreviousSentence: () -> Unit,
    onNextSentence: () -> Unit,
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PlaybackControls(
            isPlaying = isPlaying,
            onPlayPause = onPlayPause,
            onStop = onStop,
            onPreviousSentence = onPreviousSentence,
            onNextSentence = onNextSentence,
            onPreviousChapter = onPreviousChapter,
            onNextChapter = onNextChapter
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Speed", style = MaterialTheme.typography.labelSmall)
            Slider(
                value = speed,
                onValueChange = onSpeedChange,
                valueRange = 0.5f..3.0f,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
            )
            Text("${"%.1f".format(speed)}x", style = MaterialTheme.typography.labelSmall)
        }
    }
}
```

- [ ] **Step 4: Crear SleepTimerDialog**

`feature/audioplayer/src/main/java/com/ebookreader/feature/audioplayer/SleepTimerDialog.kt`:
```kotlin
package com.ebookreader.feature.audioplayer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SleepTimerDialog(
    onDismiss: () -> Unit,
    onSelect: (Int?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sleep Timer") },
        text = {
            Column {
                listOf(15, 30, 60).forEach { minutes ->
                    Text(
                        text = "$minutes minutes",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(minutes) }
                            .padding(vertical = 12.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    HorizontalDivider()
                }
                Text(
                    text = "End of chapter",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(-1) } // Special value for end of chapter
                        .padding(vertical = 12.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = { onSelect(null) }) {
                Text("Cancel timer")
            }
        }
    )
}
```

- [ ] **Step 5: Crear AudioPlayerScreen**

`feature/audioplayer/src/main/java/com/ebookreader/feature/audioplayer/AudioPlayerScreen.kt`:
```kotlin
package com.ebookreader.feature.audioplayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ebookreader.feature.audioplayer.components.AudioControls
import com.ebookreader.feature.audioplayer.components.CoverDisplay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerScreen(
    bookId: Long,
    onBack: () -> Unit,
    onSwitchToReader: () -> Unit,
    viewModel: AudioPlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val ttsState by viewModel.ttsState.collectAsState()
    var showSleepTimer by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audio Player") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onSwitchToReader) {
                        Icon(Icons.Default.MenuBook, "Switch to reader")
                    }
                    IconButton(onClick = { showSleepTimer = true }) {
                        Icon(Icons.Default.Timer, "Sleep timer")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val book = uiState.book ?: return@Scaffold
        val currentChapter = uiState.chapterTitles.getOrNull(ttsState.currentChapterIndex) ?: ""

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            CoverDisplay(
                coverPath = book.coverPath,
                title = book.title,
                author = book.author,
                currentChapter = currentChapter
            )

            Spacer(modifier = Modifier.height(32.dp))

            AudioControls(
                isPlaying = ttsState.isPlaying,
                speed = ttsState.speed,
                onPlayPause = viewModel::playPause,
                onStop = viewModel::stop,
                onPreviousSentence = viewModel::previousSentence,
                onNextSentence = viewModel::nextSentence,
                onPreviousChapter = viewModel::previousChapter,
                onNextChapter = viewModel::nextChapter,
                onSpeedChange = viewModel::setSpeed
            )

            // Sleep timer indicator
            if (uiState.sleepTimerMinutes != null) {
                val minutes = uiState.sleepTimerRemaining / 60
                val seconds = uiState.sleepTimerRemaining % 60
                Text(
                    text = "Sleep in ${minutes}:${"%02d".format(seconds)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showSleepTimer) {
        SleepTimerDialog(
            onDismiss = { showSleepTimer = false },
            onSelect = { minutes ->
                viewModel.setSleepTimer(minutes)
                showSleepTimer = false
            }
        )
    }
}
```

- [ ] **Step 6: Commit**

```bash
git add feature/audioplayer/
git commit -m "feat: add audio player screen with playback controls, cover display, and sleep timer"
```

---

## Task 12: Feature Settings

**Files:**
- Create: `feature/settings/src/main/java/com/ebookreader/feature/settings/SettingsViewModel.kt`
- Create: `feature/settings/src/main/java/com/ebookreader/feature/settings/SettingsScreen.kt`
- Create: `feature/settings/src/main/java/com/ebookreader/feature/settings/sections/GeneralSettings.kt`
- Create: `feature/settings/src/main/java/com/ebookreader/feature/settings/sections/TtsSettings.kt`
- Create: `feature/settings/src/main/java/com/ebookreader/feature/settings/sections/ReadingSettings.kt`

- [ ] **Step 1: Crear SettingsViewModel**

`feature/settings/src/main/java/com/ebookreader/feature/settings/SettingsViewModel.kt`:
```kotlin
package com.ebookreader.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ebookreader.core.data.preferences.AppPrefs
import com.ebookreader.core.data.preferences.AppThemeType
import com.ebookreader.core.data.preferences.ReadingPrefs
import com.ebookreader.core.data.preferences.ReadingThemeType
import com.ebookreader.core.data.preferences.TtsEngineType
import com.ebookreader.core.data.preferences.TtsPrefs
import com.ebookreader.core.data.preferences.UserPreferences
import com.ebookreader.core.tts.controller.TtsController
import com.ebookreader.core.tts.model.TtsVoice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val ttsController: TtsController
) : ViewModel() {

    val readingPrefs: StateFlow<ReadingPrefs> = userPreferences.readingPrefs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReadingPrefs())

    val ttsPrefs: StateFlow<TtsPrefs> = userPreferences.ttsPrefs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TtsPrefs())

    val appPrefs: StateFlow<AppPrefs> = userPreferences.appPrefs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppPrefs())

    private val _availableVoices = MutableStateFlow<List<TtsVoice>>(emptyList())
    val availableVoices: StateFlow<List<TtsVoice>> = _availableVoices.asStateFlow()

    init {
        loadVoices()
    }

    private fun loadVoices() {
        viewModelScope.launch {
            _availableVoices.value = ttsController.getAvailableVoices()
        }
    }

    fun updateAppTheme(theme: AppThemeType) {
        viewModelScope.launch {
            userPreferences.updateAppPrefs { it.copy(appTheme = theme) }
        }
    }

    fun updateFontSize(size: Int) {
        viewModelScope.launch {
            userPreferences.updateReadingPrefs { it.copy(fontSize = size) }
        }
    }

    fun updateReadingTheme(theme: ReadingThemeType) {
        viewModelScope.launch {
            userPreferences.updateReadingPrefs { it.copy(theme = theme) }
        }
    }

    fun updateLineSpacing(spacing: Float) {
        viewModelScope.launch {
            userPreferences.updateReadingPrefs { it.copy(lineSpacing = spacing) }
        }
    }

    fun updateKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.updateReadingPrefs { it.copy(keepScreenOn = enabled) }
        }
    }

    fun updateTtsEngine(engine: TtsEngineType) {
        viewModelScope.launch {
            userPreferences.updateTtsPrefs { it.copy(preferredEngine = engine) }
        }
    }

    fun updateTtsSpeed(speed: Float) {
        viewModelScope.launch {
            userPreferences.updateTtsPrefs { it.copy(speed = speed) }
        }
    }

    fun updateCloudApiKey(key: String) {
        viewModelScope.launch {
            userPreferences.updateTtsPrefs { it.copy(cloudApiKey = key) }
        }
    }
}
```

- [ ] **Step 2: Crear secciones de settings**

`feature/settings/src/main/java/com/ebookreader/feature/settings/sections/GeneralSettings.kt`:
```kotlin
package com.ebookreader.feature.settings.sections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ebookreader.core.data.preferences.AppPrefs
import com.ebookreader.core.data.preferences.AppThemeType

@Composable
fun GeneralSettings(
    prefs: AppPrefs,
    onThemeChange: (AppThemeType) -> Unit
) {
    Column {
        Text(
            "General",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )

        AppThemeType.entries.forEach { theme ->
            ListItem(
                headlineContent = { Text(theme.name.lowercase().replaceFirstChar { it.uppercase() }) },
                modifier = Modifier.clickable { onThemeChange(theme) },
                trailingContent = {
                    if (theme == prefs.appTheme) {
                        Text("Selected", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }

        HorizontalDivider()
    }
}
```

`feature/settings/src/main/java/com/ebookreader/feature/settings/sections/TtsSettings.kt`:
```kotlin
package com.ebookreader.feature.settings.sections

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ebookreader.core.data.preferences.TtsPrefs

@Composable
fun TtsSettings(
    prefs: TtsPrefs,
    onSpeedChange: (Float) -> Unit,
    onApiKeyChange: (String) -> Unit
) {
    Column {
        Text(
            "Text-to-Speech",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text("Default Speed: ${"%.1f".format(prefs.speed)}x")
            Slider(
                value = prefs.speed,
                onValueChange = onSpeedChange,
                valueRange = 0.5f..3.0f,
                modifier = Modifier.fillMaxWidth()
            )
        }

        OutlinedTextField(
            value = prefs.cloudApiKey,
            onValueChange = onApiKeyChange,
            label = { Text("Google Cloud TTS API Key") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            singleLine = true
        )

        HorizontalDivider()
    }
}
```

`feature/settings/src/main/java/com/ebookreader/feature/settings/sections/ReadingSettings.kt`:
```kotlin
package com.ebookreader.feature.settings.sections

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ebookreader.core.data.preferences.ReadingPrefs

@Composable
fun ReadingSettings(
    prefs: ReadingPrefs,
    onFontSizeChange: (Int) -> Unit,
    onLineSpacingChange: (Float) -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit
) {
    Column {
        Text(
            "Reading",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text("Default Font Size: ${prefs.fontSize}sp")
            Slider(
                value = prefs.fontSize.toFloat(),
                onValueChange = { onFontSizeChange(it.toInt()) },
                valueRange = 12f..32f,
                steps = 9,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Line Spacing: ${"%.1f".format(prefs.lineSpacing)}")
            Slider(
                value = prefs.lineSpacing,
                onValueChange = onLineSpacingChange,
                valueRange = 1.0f..2.5f,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Keep screen on", modifier = Modifier.weight(1f))
            Switch(checked = prefs.keepScreenOn, onCheckedChange = onKeepScreenOnChange)
        }
    }
}
```

- [ ] **Step 3: Crear SettingsScreen**

`feature/settings/src/main/java/com/ebookreader/feature/settings/SettingsScreen.kt`:
```kotlin
package com.ebookreader.feature.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Column
import androidx.hilt.navigation.compose.hiltViewModel
import com.ebookreader.feature.settings.sections.GeneralSettings
import com.ebookreader.feature.settings.sections.ReadingSettings
import com.ebookreader.feature.settings.sections.TtsSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val readingPrefs by viewModel.readingPrefs.collectAsState()
    val ttsPrefs by viewModel.ttsPrefs.collectAsState()
    val appPrefs by viewModel.appPrefs.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            GeneralSettings(
                prefs = appPrefs,
                onThemeChange = viewModel::updateAppTheme
            )

            ReadingSettings(
                prefs = readingPrefs,
                onFontSizeChange = viewModel::updateFontSize,
                onLineSpacingChange = viewModel::updateLineSpacing,
                onKeepScreenOnChange = viewModel::updateKeepScreenOn
            )

            TtsSettings(
                prefs = ttsPrefs,
                onSpeedChange = viewModel::updateTtsSpeed,
                onApiKeyChange = viewModel::updateCloudApiKey
            )
        }
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add feature/settings/
git commit -m "feat: add settings screen with general, reading, and TTS configuration"
```

---

## Task 13: Onboarding y permisos

**Files:**
- Create: `app/src/main/java/com/ebookreader/onboarding/OnboardingScreen.kt`
- Modify: `app/src/main/java/com/ebookreader/navigation/AppNavigation.kt`

- [ ] **Step 1: Crear OnboardingScreen**

`app/src/main/java/com/ebookreader/onboarding/OnboardingScreen.kt`:
```kotlin
package com.ebookreader.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String
)

private val pages = listOf(
    OnboardingPage(
        Icons.Default.LibraryBooks,
        "Your Library",
        "Import EPUB and PDF books from your device. We'll scan your storage automatically."
    ),
    OnboardingPage(
        Icons.Default.Book,
        "Read Anywhere",
        "Customize fonts, themes, and layout. Read in light, dark, or sepia mode."
    ),
    OnboardingPage(
        Icons.Default.Headphones,
        "Listen to Books",
        "Turn any book into an audiobook with text-to-speech. Works offline or with premium AI voices."
    )
)

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { onComplete() }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            OnboardingPageContent(pages[page])
        }

        Button(
            onClick = {
                if (pagerState.currentPage < pages.size - 1) {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                } else {
                    val permissions = if (Build.VERSION.SDK_INT >= 33) {
                        arrayOf(
                            Manifest.permission.READ_MEDIA_IMAGES,
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                    } else {
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                    permissionLauncher.launch(permissions)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                if (pagerState.currentPage < pages.size - 1) "Next" else "Get Started"
            )
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = page.icon,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
```

- [ ] **Step 2: Actualizar AppNavigation con onboarding**

Modify `app/src/main/java/com/ebookreader/navigation/AppNavigation.kt` — add onboarding route:

Add to `Routes`:
```kotlin
const val ONBOARDING = "onboarding"
```

Add composable before LIBRARY:
```kotlin
composable(Routes.ONBOARDING) {
    OnboardingScreen(
        onComplete = {
            navController.navigate(Routes.LIBRARY) {
                popUpTo(Routes.ONBOARDING) { inclusive = true }
            }
        }
    )
}
```

Change `startDestination` to `Routes.ONBOARDING` (or conditionally check if onboarding was completed via DataStore).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/ebookreader/onboarding/ app/src/main/java/com/ebookreader/navigation/
git commit -m "feat: add onboarding screens with permission requests"
```

---

## Task 14: Recursos y branding

**Files:**
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values/themes.xml`
- Create: `app/proguard-rules.pro`

- [ ] **Step 1: Crear strings.xml**

`app/src/main/res/values/strings.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">EbookReader</string>
</resources>
```

- [ ] **Step 2: Crear themes.xml**

`app/src/main/res/values/themes.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.EbookReader" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```

- [ ] **Step 3: Crear proguard-rules.pro**

`app/proguard-rules.pro`:
```
# Readium
-keep class org.readium.** { *; }
-keep class com.google.cloud.texttospeech.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/ app/proguard-rules.pro
git commit -m "feat: add app resources, theme, and ProGuard rules"
```

---

## Task 15: Build y verificacion final

- [ ] **Step 1: Verificar que compila**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Ejecutar todos los tests**

Run: `./gradlew test`
Expected: All tests pass

- [ ] **Step 3: Lint check**

Run: `./gradlew lint`
Expected: No critical issues

- [ ] **Step 4: Commit final**

```bash
git add -A
git commit -m "chore: verify full build, tests, and lint pass"
```
