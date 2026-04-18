package com.example.googleAttractionsGpx.presentation

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.googleAttractionsGpx.data.repository.*
import com.example.googleAttractionsGpx.domain.models.Coordinates
import com.example.googleAttractionsGpx.domain.models.PointData
import com.example.googleAttractionsGpx.domain.repository.IGpxGenerator
import com.example.googleAttractionsGpx.domain.repository.SettingsRepository
import com.example.googleAttractionsGpx.presentation.theme.GoogleAttractionsGpxTheme
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.Charset

// ── Source definitions ──────────────────────────────────────────────────────

data class SourceDef(
    val id: String,
    val label: String,
    val desc: String,
    val requiresApiKey: Boolean = false,
)

val ALL_SOURCES = listOf(
    SourceDef("google",   "Google Places",  "Rich POI data, photos",         requiresApiKey = true),
    SourceDef("osm",      "OpenStreetMap",  "Free & community-mapped"),
    SourceDef("trip",     "TripAdvisor",    "Reviews & ratings",             requiresApiKey = true),
    SourceDef("wikidata", "Wikidata",       "Encyclopedic attractions"),
    SourceDef("inat",     "iNaturalist",    "Nature observations"),
    SourceDef("wiki",     "Wikipedia",      "Articles about places"),
)

class MainActivity : ComponentActivity() {

    // Request location permission
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (!isGranted){
            // Show an alert dialog if permission is not granted
            AlertDialog.Builder(this)
                .setTitle("Permission Denied")
                .setMessage("Location permission is required to use this app. Please grant the permission in the app settings.")
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                    finishAffinity()
                }
                .show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if permission is granted; if not, request it
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        setContent {
            GoogleAttractionsGpxTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "main") {
                    composable("main") {
                        GpxGeneratorScreen(
                            onNavigateToSettings = { navController.navigate("settings") }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(onNavigateBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}

// ── Home screen ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpxGeneratorScreen(onNavigateToSettings: () -> Unit = {}) {
    val context = LocalContext.current
    val settingsRepository: SettingsRepository = remember { SettingsRepositoryImpl(context) }
    val scope = rememberCoroutineScope()

    var coordinatesText by remember { mutableStateOf("") }
    var selectedSources by remember { mutableStateOf(settingsRepository.selectedSources) }
    var statusText by remember { mutableStateOf("") }
    var showMapPicker by remember { mutableStateOf(false) }
    var isGenerating by remember { mutableStateOf(false) }

    LaunchedEffect(selectedSources) {
        settingsRepository.selectedSources = selectedSources
    }

    // ── helpers ──

    fun fetchCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let { coordinatesText = "${it.latitude},${it.longitude}" }
            }
        }
    }

    fun buildGenerator(id: String, settings: SettingsRepository): IGpxGenerator? = when (id) {
        "google"   -> GooglePlaceGpxGenerator(settings.googleApiKey)
        "osm"      -> OsmPlaceGpxGenerator()
        "trip"     -> TripAdvisorGpxGenerator(settings.tripAdvisorApiKey)
        "wikidata" -> WikidataAttractionsGpxGenerator()
        "inat"     -> INaturalistGpxGenerator(settings.iNaturalistUsername)
        "wiki"     -> WikipediaArticlesGpxGenerator()
        else       -> null
    }

    fun generate() {
        val coords = coordinatesText.trim()
        if (coords.isEmpty()) { statusText = "Please enter coordinates first."; return }
        val sources = selectedSources.toList()
        if (sources.isEmpty()) { statusText = "Select at least one source."; return }
        scope.launch {
            withContext(Dispatchers.Main) { isGenerating = true; statusText = "Generating GPX\u2026" }
            try {
                val centerCoordinates = Coordinates.fromString(coords)
                val allPoints = mutableListOf<PointData>()
                val errors = mutableListOf<String>()
                val deferreds = sources.map { id ->
                    async(Dispatchers.IO) {
                        val gen = buildGenerator(id, settingsRepository)
                        if (gen == null) Pair(id, emptyList<PointData>())
                        else try {
                            Pair(id, gen.getData(centerCoordinates))
                        } catch (e: Exception) {
                            synchronized(errors) { errors.add("$id: ${e.message}") }
                            Pair(id, emptyList<PointData>())
                        }
                    }
                }
                val results = deferreds.map { it.await() }
                results.forEach { (_, pts) -> allPoints.addAll(pts) }

                val gpxString = buildGpxContent(allPoints)
                val fileName = withContext(Dispatchers.IO) { getFileName(coords, sources.joinToString("-")) }
                val file = File(context.getExternalFilesDir(null), fileName)
                withContext(Dispatchers.IO) { file.writeText(gpxString, Charset.defaultCharset()) }

                val uri: Uri = FileProvider.getUriForFile(
                    context, "com.example.googleAttractionsGpx.fileProvider", file
                )
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/octet-stream")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                withContext(Dispatchers.Main) {
                    val stats = results.joinToString(", ") { (id, pts) -> "$id: ${pts.size}" }
                    val errStr = if (errors.isNotEmpty()) "\nErrors: ${errors.joinToString("; ")}" else ""
                    statusText = "Done! $stats (total: ${allPoints.size} points)$errStr"
                    isGenerating = false
                    context.startActivity(Intent.createChooser(intent, "Open GPX"))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { isGenerating = false; statusText = "Error: ${e.message}" }
            }
        }
    }

    // ── UI ──

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GPX Generator") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 100.dp)
            ) {
                LocationCard(
                    coords = coordinatesText,
                    onChange = { coordinatesText = it },
                    onCurrent = { fetchCurrentLocation() },
                    onPickMap = { showMapPicker = true }
                )
                SourcesSection(
                    selected = selectedSources,
                    onToggle = { id ->
                        selectedSources = if (selectedSources.contains(id))
                            selectedSources - id else selectedSources + id
                    }
                )
                if (statusText.isNotEmpty()) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
            }
            GenerateFab(
                count = selectedSources.size,
                isGenerating = isGenerating,
                onClick = { generate() },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            )
        }
    }

    // Map picker
    val latLng = remember(coordinatesText) {
        try { val c = Coordinates.fromString(coordinatesText); Pair(c.latitude, c.longitude) }
        catch (e: Exception) { Pair(44.4268, 26.1025) }
    }
    if (showMapPicker) {
        MapPickerSheet(
            open = true,
            initialLat = latLng.first,
            initialLng = latLng.second,
            onClose = { showMapPicker = false },
            onConfirm = { lat, lng -> coordinatesText = "$lat, $lng"; showMapPicker = false }
        )
    }
}

// ── Location card ─────────────────────────────────────────────────────────────

@Composable
private fun LocationCard(
    coords: String,
    onChange: (String) -> Unit,
    onCurrent: () -> Unit,
    onPickMap: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 14.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        Icons.Filled.LocationOn, null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text("Location", style = MaterialTheme.typography.titleMedium)
            }

            OutlinedTextField(
                value = coords,
                onValueChange = onChange,
                label = { Text("Coordinates (lat, lng)") },
                placeholder = { Text("44.4268, 26.1025") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCurrent,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Icon(Icons.Filled.LocationOn, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Current")
                }
                Button(
                    onClick = onPickMap,
                    modifier = Modifier.weight(1.2f).height(44.dp),
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(Icons.Filled.LocationOn, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Pick on map")
                }
            }
        }
    }
}

// ── Sources section ───────────────────────────────────────────────────────────

@Composable
private fun SourcesSection(selected: Set<String>, onToggle: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Sources", style = MaterialTheme.typography.titleMedium)
            Text(
                "${selected.size} selected",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ALL_SOURCES.forEach { src ->
                SourceRow(src, selected.contains(src.id)) { onToggle(src.id) }
            }
        }
    }
}

@Composable
private fun SourceRow(source: SourceDef, selected: Boolean, onToggle: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.secondaryContainer
             else MaterialTheme.colorScheme.surfaceContainerLow
    val fg = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
             else MaterialTheme.colorScheme.onSurface

    Surface(onClick = onToggle, shape = RoundedCornerShape(16.dp), color = bg,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (selected) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                        RoundedCornerShape(12.dp)
                    )
            ) {
                Text(sourceEmoji(source.id), fontSize = 20.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(source.label,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = fg)
                Text(source.desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = fg.copy(alpha = if (selected) 0.85f else 1f))
            }
            if (selected) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(24.dp).background(MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(Icons.Filled.Check, null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp))
                }
            } else {
                Surface(shape = CircleShape, color = Color.Transparent,
                    border = ButtonDefaults.outlinedButtonBorder(true),
                    modifier = Modifier.size(24.dp)
                ) {}
            }
        }
    }
}

private fun sourceEmoji(id: String) = when (id) {
    "google"   -> "\uD83C\uDF10"
    "osm"      -> "\uD83D\uDDFA"
    "trip"     -> "\u2708"
    "wikidata" -> "\u2B50"
    "inat"     -> "\uD83C\uDF3F"
    "wiki"     -> "\uD83D\uDCD6"
    else       -> "\uD83D\uDCCD"
}

// ── Generate FAB ──────────────────────────────────────────────────────────────

@Composable
private fun GenerateFab(count: Int, isGenerating: Boolean = false, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        enabled = count > 0 && !isGenerating,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.height(64.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = if (count > 0) 6.dp else 0.dp)
    ) {
        Text("Generate GPX",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold, fontSize = 17.sp))
        if (count > 1) {
            Spacer(Modifier.width(10.dp))
            Surface(shape = RoundedCornerShape(100.dp),
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f)
            ) {
                Text("$count",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp))
            }
        }
    }
}

// ── File name helper ──────────────────────────────────────────────────────────

private fun getFileName(coords: String, prefix: String): String {
    val locationName: String? = try {
        val coordinates = Coordinates.fromString(coords)
        val service = NominatimService()
        val language = java.util.Locale.getDefault().language
        service.getLocationName(coordinates, language)
    } catch (e: Exception) {
        null
    }
    val now = java.time.LocalDateTime.now().toString().replace(":", "-")
    return "${prefix}_${locationName}_$now.gpx"
}
