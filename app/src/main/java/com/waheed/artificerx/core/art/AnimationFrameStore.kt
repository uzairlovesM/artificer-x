package com.waheed.artificerx.core.art

import android.graphics.Bitmap
import com.waheed.artificerx.core.storage.WorkspaceFileSystem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnimationFrameStore @Inject constructor(private val fs: WorkspaceFileSystem) {
    suspend fun saveFrame(projectId: String, frameIndex: Int, bitmap: Bitmap): File = withContext(Dispatchers.IO) {
        val dir = fs.projectDir(projectId).resolve("animation/frames").also { it.mkdirs() }
        val out = dir.resolve("frame-${frameIndex.toString().padStart(5, '0')}.png")
        out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        out
    }
    fun listFrames(projectId: String): List<File> = fs.projectDir(projectId).resolve("animation/frames").let { d -> if (d.exists()) d.listFiles()?.filter { it.isFile }?.sortedBy { it.name }.orEmpty() else emptyList() }
}
