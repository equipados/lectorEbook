package com.ebookreader.feature.settings.sections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
    appPrefs: AppPrefs,
    onThemeSelected: (AppThemeType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "General",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        AppThemeType.entries.forEach { theme ->
            val isSelected = appPrefs.appTheme == theme
            ListItem(
                headlineContent = {
                    Text(
                        text = when (theme) {
                            AppThemeType.LIGHT -> "Light"
                            AppThemeType.DARK -> "Dark"
                            AppThemeType.SYSTEM -> "System Default"
                        }
                    )
                },
                trailingContent = if (isSelected) {
                    {
                        Text(
                            text = "Selected",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onThemeSelected(theme) }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
