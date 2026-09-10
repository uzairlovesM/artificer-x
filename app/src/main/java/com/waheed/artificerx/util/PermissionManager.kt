package com.waheed.artificerx.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionManager @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {
    data class PermissionStatus(val permission: String, val granted: Boolean)

    fun isPermissionGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED

    fun isStoragePermissionGranted(): Boolean =
        if (Build.VERSION.SDK_INT >= 33) isPermissionGranted(Manifest.permission.READ_MEDIA_IMAGES)
        else isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
            isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE)

    fun status(permission: String): PermissionStatus = PermissionStatus(permission, isPermissionGranted(permission))

    fun statusFor(vararg permissions: String): List<PermissionStatus> = permissions.map(::status)

    fun appDetailsIntent(): Intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:${appContext.packageName}")
    }

    fun manageAllFilesIntent(): Intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
        data = Uri.parse("package:${appContext.packageName}")
    }
    companion object {
        fun manageAllFilesIntent(context: Context): Intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }
}
