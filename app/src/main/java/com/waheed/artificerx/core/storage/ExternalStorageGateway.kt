package com.waheed.artificerx.core.storage

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/** SAF + MediaStore bridge. Android 13+ never needs legacy WRITE_EXTERNAL_STORAGE. */
@Singleton
class ExternalStorageGateway @Inject constructor(@ApplicationContext private val context: Context) {
    fun persistTreePermission(uri: Uri): Boolean = runCatching {
        val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(uri, flags)
        true
    }.getOrDefault(false)

    fun tree(uri: Uri): DocumentFile? = DocumentFile.fromTreeUri(context, uri)

    fun copyIntoTree(source: File, treeUri: Uri, displayName: String = source.name): Uri? {
        val tree = tree(treeUri) ?: return null
        val target = tree.createFile(mimeFor(source.extension), displayName) ?: return null
        context.contentResolver.openOutputStream(target.uri)?.use { out -> source.inputStream().use { it.copyTo(out) } }
        return target.uri
    }

    fun importFromTree(sourceUri: Uri, target: File): File? {
        target.parentFile?.mkdirs()
        val input = context.contentResolver.openInputStream(sourceUri) ?: return null
        input.use { source -> target.outputStream().use { source.copyTo(it) } }
        return target
    }

    fun publishImage(bytes: ByteArray, name: String, mime: String = "image/png"): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, mime)
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ARTIFICER-X")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        return runCatching {
            context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("Cannot open MediaStore output")
            values.clear(); values.put(MediaStore.Images.Media.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
            uri
        }.getOrElse { context.contentResolver.delete(uri, null, null); null }
    }

    fun publishDocument(bytes: ByteArray, name: String, mime: String): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Files.FileColumns.DISPLAY_NAME, name)
            put(MediaStore.Files.FileColumns.MIME_TYPE, mime)
            put(MediaStore.Files.FileColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/ARTIFICER-X")
            put(MediaStore.Files.FileColumns.IS_PENDING, 1)
        }
        val uri = context.contentResolver.insert(MediaStore.Files.getContentUri("external"), values) ?: return null
        return runCatching {
            context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("Cannot open MediaStore document output")
            values.clear(); values.put(MediaStore.Files.FileColumns.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
            uri
        }.getOrElse { context.contentResolver.delete(uri, null, null); null }
    }

    private fun mimeFor(extension: String): String = when (extension.lowercase()) {
        "png" -> "image/png"; "jpg", "jpeg" -> "image/jpeg"; "webp" -> "image/webp"
        "zip" -> "application/zip"; "pdf" -> "application/pdf"; "json" -> "application/json"
        "md" -> "text/markdown"; "txt", "log" -> "text/plain"; else -> "application/octet-stream"
    }
}
