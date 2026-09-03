package com.waheed.artificerx.core.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

object PermissionManager {
    enum class Capability(val permissions: List<String>) {
        CAMERA(listOf(Manifest.permission.CAMERA)),
        VOICE(listOf(Manifest.permission.RECORD_AUDIO)),
        IMAGES(if (Build.VERSION.SDK_INT >= 33) listOf(Manifest.permission.READ_MEDIA_IMAGES) else listOf(Manifest.permission.READ_EXTERNAL_STORAGE)),
        VIDEO(if (Build.VERSION.SDK_INT >= 33) listOf(Manifest.permission.READ_MEDIA_VIDEO) else listOf(Manifest.permission.READ_EXTERNAL_STORAGE)),
        AUDIO(if (Build.VERSION.SDK_INT >= 33) listOf(Manifest.permission.READ_MEDIA_AUDIO) else listOf(Manifest.permission.READ_EXTERNAL_STORAGE)),
        NOTIFICATIONS(if (Build.VERSION.SDK_INT >= 33) listOf(Manifest.permission.POST_NOTIFICATIONS) else emptyList()),
    }

    fun isGranted(context: Context, capability: Capability): Boolean = capability.permissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
    fun missing(context: Context, capability: Capability): List<String> = capability.permissions.filter { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }
    fun canUseExternalMedia(context: Context): Boolean = Build.VERSION.SDK_INT >= 33 || isGranted(context, Capability.IMAGES)

    fun appDetailsIntent(context: Context): Intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = android.net.Uri.parse("package:${context.packageName}") }
    fun manageAllFilesIntent(context: Context): Intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply { data = android.net.Uri.parse("package:${context.packageName}") }
}
