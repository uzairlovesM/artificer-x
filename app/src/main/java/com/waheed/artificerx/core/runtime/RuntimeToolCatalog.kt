package com.waheed.artificerx.core.runtime

import android.content.Context
import com.waheed.artificerx.core.network.FunctionDefinitionDto
import com.waheed.artificerx.core.network.ToolDefinitionDto
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class RuntimeToolSpec(
    val name: String,
    val description: String,
    val operation: String,
    val inputSchemaJson: String,
    val config: Map<String, String> = emptyMap(),
    val version: Int = 1,
)

/**
 * Persistent, declarative runtime extension catalog. An installed tool is
 * executable data, not generated Kotlin: it survives process death and can be
 * added/updated by the agent without rebuilding the APK. Only operations in
 * RuntimeToolExecutor's audited allow-list can execute.
 */
object RuntimeToolCatalog {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val tools = ConcurrentHashMap<String, RuntimeToolSpec>()
    private var root: File? = null

    fun init(context: Context) {
        if (root != null) return
        root = File(context.filesDir, "runtime-tools").apply { mkdirs() }
        loadAll()
    }

    fun definitions(): List<ToolDefinitionDto> = tools.values.sortedBy { it.name }.map { spec ->
        ToolDefinitionDto(
            function = FunctionDefinitionDto(
                name = spec.name,
                description = spec.description,
                parameters = runCatching { kotlinx.serialization.json.Json.parseToJsonElement(spec.inputSchemaJson) }.getOrElse { emptySchema() },
            ),
        )
    }

    fun get(name: String): RuntimeToolSpec? = tools[name]
    fun contains(name: String): Boolean = tools.containsKey(name)

    @Synchronized
    fun install(spec: RuntimeToolSpec): Result<RuntimeToolSpec> {
        val error = validate(spec)
        if (error != null) return Result.failure(IllegalArgumentException(error))
        val base = requireNotNull(root) { "RuntimeToolCatalog.init(context) must be called first" }
        val normalized = spec.copy(name = spec.name.lowercase())
        val target = File(base, "${normalized.name}.json")
        return runCatching {
            target.writeText(json.encodeToString(RuntimeToolSpec.serializer(), normalized))
            tools[normalized.name] = normalized
        }.fold(onSuccess = { Result.success(normalized) }, onFailure = { Result.failure(it) })
    }

    @Synchronized
    fun uninstall(name: String): Boolean {
        val removed = tools.remove(name) != null
        root?.let { File(it, "$name.json").delete() }
        return removed
    }

    private fun validate(spec: RuntimeToolSpec): String? {
        if (!Regex("^runtime_[a-z][a-z0-9_]{2,63}$").matches(spec.name.lowercase())) return "Runtime tool names must match runtime_[a-z][a-z0-9_]{2,63}."
        if (spec.description.isBlank() || spec.description.length > 600) return "Runtime tool description must be 1..600 characters."
        if (spec.operation !in SUPPORTED_OPERATIONS) return "Unsupported runtime operation '${spec.operation}'. Supported: ${SUPPORTED_OPERATIONS.joinToString()}."
        val schema = runCatching { Json.parseToJsonElement(spec.inputSchemaJson) }.getOrNull()
            ?: return "input_schema_json must be valid JSON."
        if (schema !is JsonObject || schema["type"]?.jsonPrimitive?.content != "object") return "input_schema_json must be a JSON object schema."
        return null
    }

    private fun loadAll() {
        tools.clear()
        val dir = root ?: return
        dir.listFiles { f -> f.isFile && f.extension == "json" }.orEmpty().forEach { file ->
            runCatching { json.decodeFromString(RuntimeToolSpec.serializer(), file.readText()) }
                .getOrNull()
                ?.let { spec -> if (validate(spec) == null) tools[spec.name] = spec }
        }
    }

    private fun emptySchema(): JsonObject = buildJsonObject {
        put("type", "object")
        put("properties", buildJsonObject { })
    }

    const val INSTALL_TOOL = "install_runtime_tool"
    val SUPPORTED_OPERATIONS = setOf(
        "WRITE_FILE", "READ_FILE", "LIST_DIRECTORY", "REPLACE_TEXT",
        "COPY_FILE", "MOVE_FILE", "DELETE_FILE", "HASH_FILE", "HTTP_GET", "RUN_COMMAND",
    )
}
