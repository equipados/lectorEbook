package com.ebookreader.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ebookreader.about.AboutScreen
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
    const val ABOUT = "about"

    fun reader(bookId: Long): String = "reader/$bookId"
    fun audioPlayer(bookId: Long): String = "audio_player/$bookId"
}

@Composable
fun AppNavigation(appViewModel: AppViewModel = hiltViewModel()) {
    val startDestination by appViewModel.startDestination.collectAsState()

    if (startDestination == null) {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        return
    }

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = startDestination!!
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onComplete = {
                    appViewModel.markOnboardingCompleted()
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
                onBack = { navController.popBackStack() },
                onAboutClick = { navController.navigate(Routes.ABOUT) }
            )
        }

        composable(Routes.ABOUT) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}
