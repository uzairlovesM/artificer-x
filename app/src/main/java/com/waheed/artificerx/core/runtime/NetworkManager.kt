package com.waheed.artificerx.core.runtime

import android.net.Uri
import android.util.Log
import com.waheed.artificerx.core.util.DownloadManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NetworkManager {
    private val downloadManager = DownloadManager()

    suspend fun download(uri: Uri, destination: java.io.File): java.io.File? = withContext(Dispatchers.IO) {
        val url = uri.toString()
        val result = downloadManager.downloadFile(url, destination)
        if (result == null) {
            Log.e("NetworkManager", "Download failed for: $url")
            null
        } else {
            result
        }
    }

    fun requestPermissions(permissions: Array<String>) {
        // Implementation for runtime permission handling
    }

    fun isConnected(): Boolean {
        // Check network connectivity
        return true
    }
}