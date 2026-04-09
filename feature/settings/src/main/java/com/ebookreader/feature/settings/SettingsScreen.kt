package com.ebookreader.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ebookreader.feature.settings.sections.GeneralSettings
import com.ebookreader.feature.settings.sections.ReadingSettings
import com.ebookreader.feature.settings.sections.TtsSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val appPrefs by viewModel.appPrefs.collectAsStateWithLifecycle()
    val readingPrefs by viewModel.readingPrefs.collectAsStateWithLifecycle()
    val ttsPrefs by viewModel.ttsPrefs.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Ajustes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            GeneralSettings(
                appPrefs = appPrefs,
                onThemeSelected = viewModel::updateAppTheme
            )

            HorizontalDivider()

            ReadingSettings(
                readingPrefs = readingPrefs,
                onFontSizeChanged = viewModel::updateFontSize,
                onLineSpacingChanged = viewModel::updateLineSpacing,
                onKeepScreenOnChanged = viewModel::updateKeepScreenOn
            )

            HorizontalDivider()

            TtsSettings(
                ttsPrefs = ttsPrefs,
                onSpeedChanged = viewModel::updateTtsSpeed,
                onCloudApiKeyChanged = viewModel::updateCloudApiKey
            )
        }
    }
}
