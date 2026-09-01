package com.waheed.artificerx.core.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

sealed class ExportResult {
    data class Success(
        val displayName: String,
    ) : ExportResult()

    data class Failure(
        val message: String,
    ) : ExportResult()
}

/**
 * Section export flow: writes the composited canvas Bitmap as a real
 * PNG into the device's public Pictures/ARTIFICER-X gallery folder via
 * MediaStore (API 29+) or the legacy Environment path (API 26-28),
 * so exported artwork shows up in the user's normal gallery app —
 * not buried in app-private storage where they'd never find it.
 */
@Singleton
class ImageExporter
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        suspend fun exportPng(
            bitmap: Bitmap,
            fileNameWithoutExtension: String,
        ): ExportResult =
            withContext(Dispatchers.IO) {
                val displayName = "$fileNameWithoutExtension.png"
                runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        exportViaMediaStore(bitmap, displayName)
                    } else {
                        exportViaLegacyPath(bitmap, displayName)
                    }
                }.fold(
                    onSuccess = { ExportResult.Success(displayName) },
                    onFailure = { ExportResult.Failure(it.message ?: "Unknown export error") },
                )
            }

        private fun exportViaMediaStore(
            bitmap: Bitmap,
            displayName: String,
        ) {
            val resolver = context.contentResolver
            val contentValues =
                ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/ARTIFICER-X")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }

            val uri =
                resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: error("MediaStore insert failed")

            resolver.openOutputStream(uri)?.use { stream: OutputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            } ?: error("Could not open output stream")

            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        }

        private fun exportViaLegacyPath(
            bitmap: Bitmap,
            displayName: String,
        ) {
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val appDir = java.io.File(picturesDir, "ARTIFICER-X")
            if (!appDir.exists()) appDir.mkdirs()
            val file = java.io.File(appDir, displayName)
            java.io.FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }

            // Trigger a media scan so it appears immediately in gallery apps
            android.media.MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                arrayOf("image/png"),
                null,
            )
        }
    }
