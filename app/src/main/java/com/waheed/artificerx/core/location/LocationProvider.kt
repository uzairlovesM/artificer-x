package com.waheed.artificerx.core.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/** Section: Maps/location services. Outcome of a location-fix
 *  attempt — a sealed result rather than a nullable Location so
 *  callers (MapViewModel) can show a specific, actionable message
 *  ("permission not granted" vs "GPS timed out" vs "location
 *  services disabled system-wide") instead of a generic failure. */
sealed class LocationFixResult {
    data class Success(
        val latitude: Double,
        val longitude: Double,
        val accuracyMeters: Float,
    ) : LocationFixResult()

    object PermissionDenied : LocationFixResult()

    object TimedOut : LocationFixResult()

    data class Failure(
        val message: String,
    ) : LocationFixResult()
}

/**
 * Thin coroutine wrapper over Play Services' FusedLocationProviderClient
 * (Section: Maps/location services) — used to center MapScreen's
 * osmdroid MapView on the device's current position. Deliberately not
 * a continuous location stream: this app has no navigation/tracking
 * feature that needs live updates, only a one-shot "where am I right
 * now" fix when the user opens the map or taps a recenter button, so
 * getCurrentLocation() (which Play Services itself optimizes for a
 * single fresh fix rather than leaving GPS hardware active) is the
 * right API rather than requestLocationUpdates().
 */
@Singleton
class LocationProvider
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val fusedClient: FusedLocationProviderClient by lazy {
            LocationServices.getFusedLocationProviderClient(context)
        }

        fun hasLocationPermission(): Boolean =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        /** Requests one fresh location fix. Suspends until Play Services
         *  returns a fix, times out, or errors. Checks permission itself
         *  rather than trusting the caller, since @RequiresPermission is a
         *  lint-time hint, not a runtime guarantee — a caller could still
         *  invoke this after a permission was revoked mid-session. */
        @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
        suspend fun getCurrentLocation(): LocationFixResult {
            if (!hasLocationPermission()) {
                return LocationFixResult.PermissionDenied
            }

            val request =
                CurrentLocationRequest
                    .Builder()
                    .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                    .setMaxUpdateAgeMillis(MAX_LOCATION_AGE_MILLIS)
                    .setDurationMillis(LOCATION_TIMEOUT_MILLIS)
                    .build()

            return suspendCancellableCoroutine { continuation ->
                val cancellationTokenSource = CancellationTokenSource()
                continuation.invokeOnCancellation { cancellationTokenSource.cancel() }

                val task =
                    runCatching {
                        fusedClient.getCurrentLocation(request, cancellationTokenSource.token)
                    }.getOrElse { error ->
                        if (continuation.isActive) {
                            continuation.resume(LocationFixResult.Failure(error.message ?: "Location request failed to start."))
                        }
                        return@suspendCancellableCoroutine
                    }

                task.addOnSuccessListener { location ->
                    if (!continuation.isActive) return@addOnSuccessListener
                    if (location != null) {
                        continuation.resume(
                            LocationFixResult.Success(location.latitude, location.longitude, location.accuracy),
                        )
                    } else {
                        continuation.resume(LocationFixResult.TimedOut)
                    }
                }
                task.addOnFailureListener { error ->
                    if (continuation.isActive) {
                        continuation.resume(LocationFixResult.Failure(error.message ?: "Location request failed."))
                    }
                }
                task.addOnCanceledListener {
                    if (continuation.isActive) {
                        continuation.resume(LocationFixResult.Failure("Location request was canceled."))
                    }
                }
            }
        }

        private companion object {
            const val MAX_LOCATION_AGE_MILLIS = 60_000L
            const val LOCATION_TIMEOUT_MILLIS = 10_000L
        }
    }
