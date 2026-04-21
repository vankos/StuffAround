package com.example.googleAttractionsGpx.presentation

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.googleAttractionsGpx.data.repository.NeedPhotoSettings
import com.example.googleAttractionsGpx.data.repository.SettingsRepositoryImpl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToNeedPhotoExclusions: () -> Unit,
) {
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

            SectionHeader("Need a photo")

            SettingRow(
                title = "Exclusion categories",
                value = settingsRepository.needPhotoExclusions.size.toString(),
                desc = "Object types to skip when building the photo waypoints list",
                chevron = true,
                onClick = onNavigateToNeedPhotoExclusions
            )

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeedPhotoExclusionsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepositoryImpl(context) }
    var exclusions by rememberSaveable {
        mutableStateOf(settingsRepository.needPhotoExclusions)
    }
    var adding by rememberSaveable { mutableStateOf(false) }
    var newExclusion by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }

    fun saveExclusions(updated: List<String>) {
        exclusions = updated
        settingsRepository.needPhotoExclusions = updated
    }

    fun addExclusion() {
        val normalized = NeedPhotoSettings.normalizeExclusion(newExclusion.text)
        if (normalized.isNotEmpty() && normalized !in exclusions) {
            saveExclusions(exclusions + normalized)
        }
        newExclusion = TextFieldValue("")
        adding = false
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Exclusion categories") },
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
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Text(
                text = "Wikidata objects matching these categories will be skipped when building the photo waypoints list.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(exclusions, key = { it }) { exclusion ->
                    ExclusionRow(
                        value = exclusion,
                        onRemove = { saveExclusions(exclusions.filterNot { it == exclusion }) }
                    )
                }
            }

            Box(modifier = Modifier.padding(16.dp)) {
                if (adding) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newExclusion,
                                onValueChange = { newExclusion = it },
                                label = { Text("New exclusion") },
                                placeholder = { Text("e.g. bridge") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { addExclusion() }),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                newExclusion = TextFieldValue("")
                                adding = false
                            }) {
                                Icon(Icons.Filled.Close, contentDescription = "Cancel")
                            }
                            IconButton(onClick = { addExclusion() }) {
                                Icon(Icons.Filled.Done, contentDescription = "Add")
                            }
                        }
                    }
                } else {
                    TextButton(
                        onClick = { adding = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text("Add exclusion", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ExclusionRow(value: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = value.replaceFirstChar { it.titlecase() },
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 14.dp)
        )
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Close, contentDescription = "Remove")
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
    desc: String? = null,
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
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (desc != null) {
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
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
