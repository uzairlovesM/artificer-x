package com.waheed.artificerx.core.runtime

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.TimeUnit

@Singleton
class NetworkManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun isConnected(): Boolean {
        val network = connectivity.activeNetwork ?: return false
        val caps = connectivity.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun isMetered(): Boolean = !runCatching {
        val network = connectivity.activeNetwork ?: return true
        val caps = connectivity.getNetworkCapabilities(network) ?: return true
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }.getOrDefault(false)

    suspend fun download(uri: Uri, destination: File): File? = withContext(Dispatchers.IO) {
        if (!isConnected()) return@withContext null
        destination.parentFile?.mkdirs()
        val temp = File(destination.parentFile ?: context.cacheDir, destination.name + ".part")
        val request = Request.Builder().url(uri.toString()).get().build()
        runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use false
                val body = response.body ?: return@use false
                body.byteStream().use { input -> temp.outputStream().use { output -> input.copyTo(output) } }
                if (!temp.renameTo(destination)) {
                    temp.copyTo(destination, overwrite = true)
                    temp.delete()
                }
                true
            }
        }.getOrNull()?.let { if (it) destination else null }
    }
}
