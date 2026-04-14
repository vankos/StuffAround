package com.example.googleAttractionsGpx.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
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
            TopAppBar(
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = googleApiKeyText,
                onValueChange = { newValue ->
                    googleApiKeyText = newValue
                    settingsRepository.googleApiKey = newValue.text
                },
                label = { Text("Places API Key") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
            )

            OutlinedTextField(
                value = tripAdvisorApiKeyText,
                onValueChange = { newValue ->
                    tripAdvisorApiKeyText = newValue
                    settingsRepository.tripAdvisorApiKey = newValue.text
                },
                label = { Text("TripAdvisor API Key") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
            )

            OutlinedTextField(
                value = iNaturalistUsernameText,
                onValueChange = { newValue ->
                    iNaturalistUsernameText = newValue
                    settingsRepository.iNaturalistUsername = newValue.text
                },
                label = { Text("iNaturalist Username") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
