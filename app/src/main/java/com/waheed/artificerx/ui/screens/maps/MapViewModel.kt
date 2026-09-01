package com.waheed.artificerx.ui.screens.maps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waheed.artificerx.core.location.LocationFixResult
import com.waheed.artificerx.core.location.LocationProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Section: Maps/location services. */
data class MapUiState(
    val centerLatitude: Double = DEFAULT_LATITUDE,
    val centerLongitude: Double = DEFAULT_LONGITUDE,
    val isLocating: Boolean = false,
    val locationErrorMessage: String? = null,
    val hasCenteredOnUserOnce: Boolean = false,
) {
    companion object {
        // Default center: 0,0 (Gulf of Guinea) is a poor first-launch
        // default — nothing to orient against. Centering on a
        // recognizable, populated reference point until the user's
        // real location loads (or they deny the permission) reads as
        // "a map that works," not "a map that's broken."
        const val DEFAULT_LATITUDE = 28.6139 // New Delhi — a neutral, populated default
        const val DEFAULT_LONGITUDE = 77.2090
        const val DEFAULT_ZOOM = 5.0
        const val LOCATED_ZOOM = 15.0
    }
}

@HiltViewModel
class MapViewModel
    @Inject
    constructor(
        private val locationProvider: LocationProvider,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(MapUiState())
        val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

        fun hasLocationPermission(): Boolean = locationProvider.hasLocationPermission()

        fun centerOnCurrentLocation() {
            if (!locationProvider.hasLocationPermission()) {
                _uiState.value =
                    _uiState.value.copy(
                        locationErrorMessage = "Location permission not granted.",
                    )
                return
            }
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLocating = true, locationErrorMessage = null)
                when (val result = locationProvider.getCurrentLocation()) {
                    is LocationFixResult.Success -> {
                        _uiState.value =
                            _uiState.value.copy(
                                centerLatitude = result.latitude,
                                centerLongitude = result.longitude,
                                isLocating = false,
                                hasCenteredOnUserOnce = true,
                                locationErrorMessage = null,
                            )
                    }
                    is LocationFixResult.PermissionDenied -> {
                        _uiState.value =
                            _uiState.value.copy(
                                isLocating = false,
                                locationErrorMessage = "Location permission not granted.",
                            )
                    }
                    is LocationFixResult.TimedOut -> {
                        _uiState.value =
                            _uiState.value.copy(
                                isLocating = false,
                                locationErrorMessage = "Couldn't get a location fix — try again outdoors or with a clearer GPS view.",
                            )
                    }
                    is LocationFixResult.Failure -> {
                        _uiState.value =
                            _uiState.value.copy(
                                isLocating = false,
                                locationErrorMessage = result.message,
                            )
                    }
                }
            }
        }

        fun dismissLocationError() {
            _uiState.value = _uiState.value.copy(locationErrorMessage = null)
        }
    }
