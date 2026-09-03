package com.waheed.artificerx.core.art

import com.waheed.artificerx.core.storage.WorkspaceFileSystem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class MangaPanel(val id: String, val x: Float, val y: Float, val width: Float, val height: Float, val rotation: Float = 0f, val caption: String = "")
@Serializable
data class MangaPage(val width: Int, val height: Int, val panels: List<MangaPanel>)

@Singleton
class MangaLayoutStore @Inject constructor(private val fs: WorkspaceFileSystem) {
    private val json = Json { prettyPrint = true }
    suspend fun save(projectId: String, page: MangaPage) = withContext(Dispatchers.IO) { fs.writeTextAtomic(fs.projectDir(projectId).resolve("manga/page.json"), json.encodeToString(MangaPage.serializer(), page)) }
    suspend fun load(projectId: String): MangaPage? = withContext(Dispatchers.IO) { val f = fs.projectDir(projectId).resolve("manga/page.json"); if (!f.exists()) null else runCatching { json.decodeFromString(MangaPage.serializer(), f.readText()) }.getOrNull() }
}
