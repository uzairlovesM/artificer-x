package com.waheed.artificerx.util

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedComponentScope
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutinesCoroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionManager @Inject constructor(
    @ApplicationContext val app: Application
) {
    private val requestPermissionLauncher = app.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Handle permission result
        isPermissionGranted(isGranted)
    }

    fun requestStoragePermission() {
        requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    fun requestLocationPermission() {
        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    fun isPermissionGranted(isGranted: Boolean) {
        if (isGranted) {
            DebugLogger.d("PermissionManager", "Required permission granted")
        } else {
            DebugLogger.e("PermissionManager", "Required permission denied: user might need to update app settings")
        }
    }

    fun isStoragePermissionGranted(): Boolean = app.checkSelfPermission(
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED

    fun isLocationPermissionGranted(): Boolean = app.checkSelfPermission(
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}