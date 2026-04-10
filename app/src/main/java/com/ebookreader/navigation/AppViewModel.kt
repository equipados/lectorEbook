package com.ebookreader.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ebookreader.core.data.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    /**
     * Destino inicial resuelto a partir de las preferencias.
     * `null` mientras se cargan los datos.
     */
    val startDestination: StateFlow<String?> = userPreferences.appPrefs
        .map { prefs ->
            if (prefs.onboardingCompleted) Routes.LIBRARY else Routes.ONBOARDING
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun markOnboardingCompleted() {
        viewModelScope.launch {
            val current = userPreferences.appPrefs.first()
            userPreferences.updateAppPrefs(current.copy(onboardingCompleted = true))
        }
    }
}
