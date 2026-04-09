package com.ebookreader.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
            // TODO: Replace with LibraryScreen from feature:library
            PlaceholderScreen("Library")
        }

        composable(
            route = Routes.READER,
            arguments = listOf(navArgument("bookId") { type = NavType.LongType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getLong("bookId") ?: 0L
            // TODO: Replace with ReaderScreen from feature:reader
            PlaceholderScreen("Reader (Book ID: $bookId)")
        }

        composable(
            route = Routes.AUDIO_PLAYER,
            arguments = listOf(navArgument("bookId") { type = NavType.LongType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getLong("bookId") ?: 0L
            // TODO: Replace with AudioPlayerScreen from feature:audioplayer
            PlaceholderScreen("Audio Player (Book ID: $bookId)")
        }

        composable(Routes.SETTINGS) {
            // TODO: Replace with SettingsScreen from feature:settings
            PlaceholderScreen("Settings")
        }
    }
}

@Composable
private fun PlaceholderScreen(name: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = name)
    }
}
