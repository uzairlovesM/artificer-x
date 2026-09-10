package com.waheed.artificerx.util

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.WorkerThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FileManager(private val context: Context) {
    @WorkerThread
    suspend fun createImageFile(): Uri =
        withContext(Dispatchers.IO) {
            val contentResolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "artificerx_${System.currentTimeMillis()}.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ArtificerX")
            }

            contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            ) ?: throw IOException("Failed to create image file")
        }

    fun getImageUriFromFilePath(filePath: String): Uri {
        val file = File(filePath)
        return if (file.exists()) {
            FileProvider.getUriForFile(
                context,
                context.packageName + ".provider",
                file
            )
        } else {
            throw FileNotFoundException("File not found: $filePath")
        }
    }

    fun getImagePathFromUri(uri: Uri): String =
        context.contentResolver.query(uri, null, null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndexOrThrow(
                        MediaStore.Images.Media.DATA
                    )
                    cursor.getString(columnIndex)
                } else {
                    throw IOException("No data found for URI: $uri")
                }
            } ?: throw SecurityException("Unauthorized URI access")

    fun getExternalFilesDir(): File = context.getExternalFilesDir(null) ?: File(context.filesDir, "external")

    fun deleteFile(uri: Uri) {
        context.contentResolver.delete(uri, null, null)
    }

    fun copyFile(source: Uri, destinationPath: String) {
        context.contentResolver.openInputStream(source).use { input ->
            File(destinationPath).outputStream().use { output ->
                input?.copyTo(output)
            }
        }
    }
}
