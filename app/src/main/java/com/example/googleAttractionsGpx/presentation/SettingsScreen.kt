package com.example.googleAttractionsGpx.presentation

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.googleAttractionsGpx.data.repository.SettingsRepositoryImpl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepositoryImpl(context) }

    var googleApiKeyText by remember { mutableStateOf(TextFieldValue("")) }
    var tripAdvisorApiKeyText by remember { mutableStateOf(TextFieldValue("")) }
    var iNaturalistUsernameText by remember { mutableStateOf(TextFieldValue("")) }

    LaunchedEffect(Unit) {
        googleApiKeyText = TextFieldValue(settingsRepository.googleApiKey)
        tripAdvisorApiKeyText = TextFieldValue(settingsRepository.tripAdvisorApiKey)
        iNaturalistUsernameText = TextFieldValue(settingsRepository.iNaturalistUsername)
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // ── API Keys section ──
            SectionHeader("API Keys")

            MaskedField(
                value = googleApiKeyText,
                onValueChange = {
                    googleApiKeyText = it
                    settingsRepository.googleApiKey = it.text
                },
                label = "Google Places API key"
            )

            MaskedField(
                value = tripAdvisorApiKeyText,
                onValueChange = {
                    tripAdvisorApiKeyText = it
                    settingsRepository.tripAdvisorApiKey = it.text
                },
                label = "TripAdvisor API key"
            )

            OutlinedTextField(
                value = iNaturalistUsernameText,
                onValueChange = {
                    iNaturalistUsernameText = it
                    settingsRepository.iNaturalistUsername = it.text
                },
                label = { Text("iNaturalist username") },
                supportingText = { Text("Used to fetch your observations") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            // ── About section ──
            SectionHeader("About")

            val packageInfo = remember {
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            SettingRow(title = "Version", value = packageInfo.versionName ?: "unknown")

            SettingRow(
                title = "Source on GitHub",
                chevron = true,
                onClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/vankos/GoogleAttractionsGpx")
                    )
                    context.startActivity(intent)
                }
            )

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 12.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun MaskedField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: String,
) {
    var visible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Text(
                    if (visible) "\uD83D\uDE48" else "\uD83D\uDC41",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    )
}

@Composable
private fun SettingRow(
    title: String,
    value: String? = null,
    chevron: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (chevron) {
            Text(
                text = "›",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}
