package com.ebookreader.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ebookreader.feature.audioplayer.AudioPlayerScreen
import com.ebookreader.feature.library.LibraryScreen
import com.ebookreader.feature.reader.ReaderScreen
import com.ebookreader.feature.settings.SettingsScreen
import com.ebookreader.onboarding.OnboardingScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val LIBRARY = "library"
    const val READER = "reader/{bookId}"
    const val AUDIO_PLAYER = "audio_player/{bookId}"
    const val SETTINGS = "settings"

    fun reader(bookId: Long): String = "reader/$bookId"
    fun audioPlayer(bookId: Long): String = "audio_player/$bookId"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.ONBOARDING
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Routes.LIBRARY) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.LIBRARY) {
            LibraryScreen(
                onBookClick = { bookId ->
                    navController.navigate(Routes.reader(bookId))
                },
                onSettingsClick = {
                    navController.navigate(Routes.SETTINGS)
                }
            )
        }

        composable(
            route = Routes.READER,
            arguments = listOf(navArgument("bookId") { type = NavType.LongType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getLong("bookId") ?: 0L
            ReaderScreen(
                bookId = bookId,
                onBack = { navController.popBackStack() },
                onSwitchToAudio = {
                    navController.navigate(Routes.audioPlayer(bookId))
                }
            )
        }

        composable(
            route = Routes.AUDIO_PLAYER,
            arguments = listOf(navArgument("bookId") { type = NavType.LongType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getLong("bookId") ?: 0L
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
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
