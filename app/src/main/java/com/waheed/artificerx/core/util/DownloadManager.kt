package com.waheed.artificerx.core.util

import android.content.Context
import android.net.Uri
import com.waheed.artificerx.core.runtime.NetworkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DownloadManager {
    private val networkManager: NetworkManager
    private val context: Context

    constructor(@ApplicationContext val context: Context, networkManager: NetworkManager) {
        this.context = context
        this.networkManager = networkManager
    }

    suspend fun downloadFile(url: String, destination: File): File? = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(url)
            networkManager.download(uri, destination)
            destination.takeIf { it.exists() }
        } catch (e: Exception) {
            DebugLogger.e("Download failed", e)
            null
        }
    }

    fun ensureExternalStoragePermission() {
        val permissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        NetworkManager.requestPermissions(permissions)
    }
}
