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
data class CustomBrushPreset(val id: String, val name: String, val baseType: String, val size: Float, val opacity: Float, val hardness: Float)

@Singleton
class CustomBrushStore @Inject constructor(private val fs: WorkspaceFileSystem) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val file get() = fs.roots.system.resolve("custom-brushes.json")
    suspend fun list(): List<CustomBrushPreset> = withContext(Dispatchers.IO) { if (!file.exists()) emptyList() else runCatching { json.decodeFromString(ListSerializer(CustomBrushPreset.serializer()), file.readText()) }.getOrElse { emptyList() } }
    suspend fun save(preset: CustomBrushPreset) = withContext(Dispatchers.IO) { val all = list().filterNot { it.id == preset.id } + preset; fs.writeTextAtomic(file, json.encodeToString(ListSerializer(CustomBrushPreset.serializer()), all)) }
}
