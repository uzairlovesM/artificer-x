package com.waheed.artificerx.core.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
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
        val uri: android.net.Uri,
    ) : ExportResult()

    data class Failure(
        val message: String,
    ) : ExportResult()
}

/**
 * Section export flow: writes the composited canvas Bitmap as a real
 * PNG into the device's public Pictures/ARTIFICER-X gallery folder via
 * MediaStore, so exported artwork shows up in the user's normal
 * gallery app — not buried in app-private storage where they'd never
 * find it.
 *
 * v0.4.30: minSdk is now 33 (personal-device-only app, see
 * build.gradle.kts), so the pre-Q legacy Environment-path branch that
 * used to exist here is unreachable dead code and has been removed —
 * MediaStore is the only path Android 33+ ever takes. Also now returns
 * the written Uri (not just the display name) so callers like
 * AgentChatViewModel's auto-save can offer a direct "View"/"Share"
 * action on the AI's output instead of just naming the file.
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
                runCatching { exportViaMediaStore(bitmap, displayName) }
                    .fold(
                        onSuccess = { uri -> ExportResult.Success(displayName, uri) },
                        onFailure = { ExportResult.Failure(it.message ?: "Unknown export error") },
                    )
            }

        private fun exportViaMediaStore(
            bitmap: Bitmap,
            displayName: String,
        ): android.net.Uri {
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
            return uri
        }
    }
