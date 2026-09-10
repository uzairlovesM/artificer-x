package com.waheed.artificerx.core.util

import android.content.Context
import android.net.Uri
import com.waheed.artificerx.core.runtime.NetworkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val networkManager: NetworkManager,
) {
    suspend fun downloadFile(url: String, destination: File): File? =
        networkManager.download(Uri.parse(url), destination)

    suspend fun downloadToCache(url: String, fileName: String): File? = withContext(Dispatchers.IO) {
        downloadFile(url, File(context.cacheDir, fileName))
    }
}
