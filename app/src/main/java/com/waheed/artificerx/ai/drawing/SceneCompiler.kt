package com.waheed.artificerx.ai.drawing

import java.util.UUID

data class SceneNode(val id: String = UUID.randomUUID().toString(), val kind: String, val label: String, val z: Int, val properties: Map<String, String>)
data class SceneSpec(val width: Int, val height: Int, val camera: Map<String, Float>, val nodes: List<SceneNode>, val palette: List<String>)

class SceneCompiler {
    fun compile(intent: DrawingIntent): SceneSpec {
        val names = intent.requiredObjects.ifEmpty { listOf("floor", "back_wall", "side_wall", "window", "door", "bed", "desk", "chair", "lamp", "rug") }
        val nodes = names.mapIndexed { i, name ->
            SceneNode(kind = inferKind(name), label = name, z = i, properties = mapOf("style" to intent.style, "environment" to intent.environment))
        }
        return SceneSpec(2048, 1536, mapOf("fov" to 42f, "yaw" to 0f, "pitch" to -4f, "cameraDistance" to 7f), nodes, listOf("#F2E6D8", "#9A7655", "#334155", "#D4A72C", "#F8FAFC"))
    }

    private fun inferKind(name: String): String = when {
        name.contains("wall", true) || name.contains("floor", true) -> "architecture"
        name.contains("window", true) || name.contains("door", true) -> "opening"
        name.contains("light", true) || name.contains("lamp", true) -> "lighting"
        else -> "furniture"
    }
}
