package com.waheed.artificerx.core.agent

import com.waheed.artificerx.core.artifact.ArtifactRef
import com.waheed.artificerx.core.artifact.ArtifactStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turns explicitly structured model output into real workspace artifacts.
 *
 * Supported form:
 *   file: src/Main.kt
 *   ```kotlin
 *   ...
 *   ```
 *
 * The model must opt in by naming the block. We deliberately do not turn every
 * fenced code sample into a file because explanations frequently contain code
 * that should remain prose rather than become side effects.
 */
@Singleton
class AIResponseArtifactMaterializer @Inject constructor(
    private val artifactStore: ArtifactStore,
) {
    private val blockRegex = Regex(
        "(?ms)^(?:artifact|file)\\s*:\\s*([^\\n]+)\\n```(?:[^\\n]*)\\n(.*?)\\n```"
    )

    suspend fun materialize(threadId: String, response: String): List<ArtifactRef> {
        if (threadId.isBlank() || response.isBlank()) return emptyList()
        val results = mutableListOf<ArtifactRef>()
        val seenNames = mutableSetOf<String>()
        for (match in blockRegex.findAll(response)) {
            val name = match.groupValues[1].trim()
            val content = match.groupValues[2]
            if (name.isBlank() || content.isBlank()) continue
            if (!seenNames.add(name)) continue
            val mime = mimeFor(name)
            val ref = runCatching { artifactStore.writeText(threadId, name, content, mime, "ai_response_materializer") }.getOrNull()
            if (ref != null) results += ref
            if (results.size >= 100) break
        }
        return results
    }

    private fun mimeFor(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "kt", "kts", "java", "swift", "ts", "tsx", "js", "jsx", "py", "rs", "cpp", "c", "h" -> "text/plain"
        "json" -> "application/json"
        "md", "markdown" -> "text/markdown"
        "html", "htm" -> "text/html"
        "css" -> "text/css"
        "xml" -> "application/xml"
        "csv" -> "text/csv"
        "yaml", "yml" -> "text/yaml"
        else -> "text/plain"
    }
}
