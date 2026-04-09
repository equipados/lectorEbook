pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
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
