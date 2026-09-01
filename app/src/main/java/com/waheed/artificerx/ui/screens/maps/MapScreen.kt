package com.waheed.artificerx.ui.screens.maps

import android.Manifest
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.waheed.artificerx.ui.theme.GoldPrimary
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView

/**
 * Section: Maps/location services. Embeds osmdroid's classic-Android
 * MapView inside Compose via AndroidView — osmdroid has no native
 * Compose API, so the interop bridge (with lifecycle-aware
 * onResume/onPause/onDetach forwarding, since MapView owns real tile-
 * download threads that must be paused/resumed with the screen) is
 * the standard, correct approach rather than waiting for a
 * Compose-native osmdroid wrapper that doesn't exist.
 *
 * No API key required (OpenStreetMap tiles, not Google Maps) — see
 * gradle/libs.versions.toml's comment on why osmdroid was chosen for
 * this personal build. Configuration (User-Agent, cache paths) is set
 * once process-wide in ArtificerXApp.configureOsmdroid() — this
 * screen only ever creates the MapView itself.
 */
@Composable
fun MapScreen(
    onBack: () -> Unit,
    viewModel: MapViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var permissionDenied by remember { mutableStateOf(false) }

    val permissionLauncher =
        androidx.activity.compose.rememberLauncherForActivityResult(
            contract =
                androidx.activity.result.contract.ActivityResultContracts
                    .RequestMultiplePermissions(),
        ) { grants ->
            val granted =
                grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (granted) {
                viewModel.centerOnCurrentLocation()
            } else {
                permissionDenied = true
            }
        }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (viewModel.hasLocationPermission()) {
                        viewModel.centerOnCurrentLocation()
                    } else {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                            ),
                        )
                    }
                },
                containerColor = GoldPrimary,
            ) {
                if (state.isLocating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(Icons.Filled.MyLocation, contentDescription = "Center on my location", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        },
    ) { scaffoldPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(scaffoldPadding)) {
            OsmMapView(
                centerLatitude = state.centerLatitude,
                centerLongitude = state.centerLongitude,
                zoom = if (state.hasCenteredOnUserOnce) MapUiState.LOCATED_ZOOM else MapUiState.DEFAULT_ZOOM,
            )

            Row(modifier = Modifier.padding(8.dp)) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }

            if (permissionDenied) {
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    action = { IconButton(onClick = { permissionDenied = false }) { Text("Dismiss") } },
                ) {
                    Text("Location permission denied — showing default map view.")
                }
            }

            state.locationErrorMessage?.let { message ->
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    action = { IconButton(onClick = viewModel::dismissLocationError) { Text("Dismiss") } },
                ) {
                    Text(message)
                }
            }
        }
    }
}

/** The actual AndroidView interop wrapper — kept separate from
 *  MapScreen so its lifecycle-forwarding logic is isolated and easy
 *  to review on its own. The MapView instance created in [factory] is
 *  captured into [mapViewHolder] specifically so the DisposableEffect
 *  below has a real reference to forward onResume/onPause/onDetach
 *  to — capturing it is the part that's easy to get subtly wrong
 *  (declaring the effect but never actually wiring it to the created
 *  view leaves onPause() silently never called, and the tile-download
 *  thread pool running forever in the background). */
@Composable
private fun OsmMapView(
    centerLatitude: Double,
    centerLongitude: Double,
    zoom: Double,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentCenter = rememberUpdatedState(GeoPoint(centerLatitude, centerLongitude))
    val currentZoom = rememberUpdatedState(zoom)
    val mapViewHolder = remember { arrayOfNulls<MapView>(1) }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            MapView(ctx).apply {
                setMultiTouchControls(true)
                zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
                controller.setZoom(currentZoom.value)
                controller.setCenter(currentCenter.value)
                mapViewHolder[0] = this
            }
        },
        update = { mapView ->
            mapView.controller.animateTo(currentCenter.value)
            mapView.controller.setZoom(currentZoom.value)
        },
    )

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> mapViewHolder[0]?.onResume()
                    Lifecycle.Event.ON_PAUSE -> mapViewHolder[0]?.onPause()
                    else -> Unit
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapViewHolder[0]?.onDetach()
        }
    }
}
