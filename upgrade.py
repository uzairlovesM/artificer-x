from pathlib import Path
root=Path('/mnt/data/artificer_upgrade_work')

def write(rel, text):
    p=root/rel; p.parent.mkdir(parents=True, exist_ok=True); p.write_text(text)

write('app/src/main/java/com/waheed/artificerx/core/storage/WorkspaceFileSystem.kt', r'''package com.waheed.artificerx.core.storage

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/** Owns every app-managed data path. All folders are created lazily and safely. */
@Singleton
class WorkspaceFileSystem @Inject constructor(@ApplicationContext private val context: Context) {
    data class Roots(
        val root: File,
        val works: File,
        val cache: File,
        val system: File,
        val plugins: File,
        val models: File,
        val exports: File,
        val imports: File,
        val logs: File,
        val temp: File,
        val thumbnails: File,
        val backups: File,
        val autosave: File,
        val projects: File,
        val recipes: File,
    )

    val roots: Roots by lazy { createRoots() }

    fun ensureReady() { roots }

    fun projectDir(projectId: String): File = roots.projects.resolve(safeSegment(projectId)).also { it.mkdirs() }
    fun pluginDir(pluginId: String): File = roots.plugins.resolve(safeSegment(pluginId)).also { it.mkdirs() }
    fun cacheFile(name: String): File = roots.cache.resolve(safeSegment(name))
    fun tempFile(prefix: String, suffix: String = ".tmp"): File = File.createTempFile(safeSegment(prefix), suffix, roots.temp)

    fun writeTextAtomic(target: File, value: String): File {
        target.parentFile?.mkdirs()
        val tmp = File(target.parentFile, ".${target.name}.writing")
        tmp.writeText(value)
        if (!tmp.renameTo(target)) {
            target.writeText(value)
            tmp.delete()
        }
        return target
    }

    fun writeBytesAtomic(target: File, value: ByteArray): File {
        target.parentFile?.mkdirs()
        val tmp = File(target.parentFile, ".${target.name}.writing")
        tmp.writeBytes(value)
        if (!tmp.renameTo(target)) {
            target.writeBytes(value)
            tmp.delete()
        }
        return target
    }

    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun usageBytes(): Long = roots.root.walkTopDown().filter { it.isFile }.sumOf { it.length() }

    fun clearCache(): Long {
        val bytes = roots.cache.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        roots.cache.deleteRecursively(); roots.cache.mkdirs()
        return bytes
    }

    fun listFiles(directory: File, recursive: Boolean = true): List<File> {
        if (!directory.exists()) return emptyList()
        return if (recursive) directory.walkTopDown().filter { it.isFile }.toList() else directory.listFiles()?.filter { it.isFile }.orEmpty()
    }

    private fun createRoots(): Roots {
        val root = context.filesDir.resolve("ARTIFICER-X")
        val result = Roots(
            root = root,
            works = root.resolve("works"),
            cache = root.resolve("cache"),
            system = root.resolve("system"),
            plugins = root.resolve("plugins"),
            models = root.resolve("models"),
            exports = root.resolve("exports"),
            imports = root.resolve("imports"),
            logs = root.resolve("logs"),
            temp = root.resolve("temp"),
            thumbnails = root.resolve("thumbnails"),
            backups = root.resolve("backups"),
            autosave = root.resolve("autosave"),
            projects = root.resolve("projects"),
            recipes = root.resolve("recipes"),
        )
        listOf(result.root, result.works, result.cache, result.system, result.plugins, result.models,
            result.exports, result.imports, result.logs, result.temp, result.thumbnails, result.backups,
            result.autosave, result.projects, result.recipes).forEach { it.mkdirs() }
        writeTextAtomic(result.system.resolve("workspace.json"), "{\"schema\":3,\"initialized\":true}")
        return result
    }

    private fun safeSegment(value: String): String = value.replace(Regex("[^A-Za-z0-9._-]"), "_").take(120).ifBlank { "item" }

    companion object { private const val DEFAULT_BUFFER = 32 * 1024 }
}
''')

write('app/src/main/java/com/waheed/artificerx/core/storage/ExternalStorageGateway.kt', r'''package com.waheed.artificerx.core.storage

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
''')

write('app/src/main/java/com/waheed/artificerx/core/permissions/PermissionManager.kt', r'''package com.waheed.artificerx.core.permissions

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
''')

write('app/src/main/java/com/waheed/artificerx/core/background/WorkspaceMaintenanceWorker.kt', r'''package com.waheed.artificerx.core.background

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.waheed.artificerx.core.storage.WorkspaceFileSystem
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class WorkspaceMaintenanceWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val fileSystem: WorkspaceFileSystem,
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result = runCatching {
        fileSystem.ensureReady()
        cleanupTemp()
        Result.success()
    }.getOrElse { Result.retry() }

    private fun cleanupTemp() {
        fileSystem.listFiles(fileSystem.roots.temp).filter { System.currentTimeMillis() - it.lastModified() > MAX_TEMP_AGE_MS }.forEach { it.delete() }
    }

    companion object { private const val MAX_TEMP_AGE_MS = 24L * 60 * 60 * 1000 }
}
''')

write('app/src/main/java/com/waheed/artificerx/core/background/WorkspaceMaintenanceScheduler.kt', r'''package com.waheed.artificerx.core.background

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkspaceMaintenanceScheduler @Inject constructor(private val context: Context) {
    fun schedule() {
        val request = PeriodicWorkRequestBuilder<WorkspaceMaintenanceWorker>(12, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("artificerx-workspace-maintenance", ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
''')
