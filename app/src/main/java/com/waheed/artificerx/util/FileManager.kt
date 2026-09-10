package com.waheed.artificerx.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.annotation.WorkerThread
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    @WorkerThread
    suspend fun createImageFile(displayName: String = "artificerx_${System.currentTimeMillis()}.png"): Uri = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ArtificerX")
        }
        context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Failed to create image file")
    }

    fun getImageUriFromFilePath(filePath: String): Uri {
        val file = File(filePath)
        if (!file.exists()) throw FileNotFoundException(filePath)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun getExternalFilesDir(): File = context.getExternalFilesDir(null) ?: File(context.filesDir, "external").apply { mkdirs() }

    fun deleteFile(uri: Uri): Boolean = context.contentResolver.delete(uri, null, null) > 0
}
