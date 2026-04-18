package com.example.googleAttractionsGpx.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPickerSheet(
    open: Boolean,
    initialLat: Double,
    initialLng: Double,
    onClose: () -> Unit,
    onConfirm: (lat: Double, lng: Double) -> Unit
) {
    val context = LocalContext.current

    // Center coordinates that track the map's center as user drags
    var centerLat by remember { mutableStateOf(initialLat) }
    var centerLng by remember { mutableStateOf(initialLng) }

    // Keep a stable reference to the MapView across recompositions
    val mapViewState = remember { mutableStateOf<MapView?>(null) }

    if (!open) return

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .navigationBarsPadding()
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(32.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    .align(Alignment.CenterHorizontally)
            )

            // Title row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
                Text(
                    text = "Pick on map",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
            }

            // Map + overlaid controls
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
            ) {
                AndroidView(
                    factory = { ctx ->
                        Configuration.getInstance().userAgentValue = "GoogleAttractionsGpx/1.0"
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            // Hide built-in osmdroid zoom buttons (we have our own overlay)
                            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                            controller.setZoom(13.0)
                            controller.setCenter(GeoPoint(initialLat, initialLng))
                            mapViewState.value = this
                            // Allow single-finger panning by preventing BottomSheet from
                            // intercepting touch events when the map is being touched
                            setOnTouchListener { v, event ->
                                v.parent?.requestDisallowInterceptTouchEvent(true)
                                false
                            }
                            addMapListener(object : MapListener {
                                override fun onScroll(event: ScrollEvent?): Boolean {
                                    val center = mapCenter
                                    centerLat = center.latitude
                                    centerLng = center.longitude
                                    return false
                                }
                                override fun onZoom(event: ZoomEvent?): Boolean {
                                    val center = mapCenter
                                    centerLat = center.latitude
                                    centerLng = center.longitude
                                    return false
                                }
                            })
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Fixed center pin
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(36.dp)
                        .align(Alignment.Center)
                        .offset(y = (-18).dp)
                )

                // Zoom controls (top-right)
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .width(40.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IconButton(
                        onClick = { mapViewState.value?.controller?.zoomIn() },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Text("+", fontSize = 20.sp, fontWeight = FontWeight.Medium)
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 6.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    IconButton(
                        onClick = { mapViewState.value?.controller?.zoomOut() },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Text("−", fontSize = 20.sp, fontWeight = FontWeight.Medium)
                    }
                }

                // Current location mini FAB (bottom-right)
                SmallFloatingActionButton(
                    onClick = {
                        if (ActivityCompat.checkSelfPermission(
                                context, Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            val fusedLocationClient =
                                LocationServices.getFusedLocationProviderClient(context)
                            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                                location?.let {
                                    centerLat = it.latitude
                                    centerLng = it.longitude
                                    val map = mapViewState.value
                                    map?.controller?.animateTo(GeoPoint(it.latitude, it.longitude))
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp),
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                ) {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = "My location",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Coordinates display + confirm button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SELECTED",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "%.4f, %.4f".format(centerLat, centerLng),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Button(
                    onClick = { onConfirm(centerLat, centerLng) },
                    shape = RoundedCornerShape(100.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Set")
                }
            }
        }
    }
}
