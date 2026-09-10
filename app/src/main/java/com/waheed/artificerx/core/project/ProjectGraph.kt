package com.waheed.artificerx.core.project

import java.security.MessageDigest

/** Canonical, deterministic scene/layer graph used by every higher-level subsystem. */
class ProjectGraph(snapshot: ProjectSnapshot) {
    private var value = snapshot
    private var structuralVersion = 0L

    init { require(ProjectValidator().isSafe(snapshot)) }

    @Synchronized fun snapshot(): ProjectSnapshot = value
    @Synchronized fun version(): Long = structuralVersion

    @Synchronized fun rename(name: String) {
        require(name.isNotBlank())
        value = value.copy(
            metadata = value.metadata.copy(title = name.trim(), modifiedAt = System.currentTimeMillis()),
            saved = false,
        )
        structuralVersion++
    }

    @Synchronized fun addLayer(name: String, id: String, opacity: Float = 1f, blendMode: String = "normal"): LayerId {
        val layerId = LayerId(id)
        require(value.layers.none { it.id == layerId }) { "Duplicate layer id: $id" }
        val nextOrder = (value.layers.maxOfOrNull { it.order } ?: -1) + 1
        val layer = LayerDescriptor(layerId, name.trim(), true, opacity, blendMode, false, nextOrder)
        require(layer.name.isNotBlank())
        value = value.copy(layers = (value.layers + layer).sortedBy { it.order }, activeLayer = layerId, saved = false)
        structuralVersion++
        return layerId
    }

    @Synchronized fun removeLayer(id: LayerId) {
        require(value.layers.size > 1) { "A project must retain at least one layer" }
        require(value.layers.any { it.id == id }) { "Layer does not exist: ${id.value}" }
        val remaining = value.layers.filterNot { it.id == id }
        val active = if (value.activeLayer == id) remaining.lastOrNull()?.id else value.activeLayer
        value = value.copy(layers = remaining, activeLayer = active, saved = false)
        structuralVersion++
    }

    @Synchronized fun setActiveLayer(id: LayerId) {
        require(value.layers.any { it.id == id }) { "Layer does not exist: ${id.value}" }
        value = value.copy(activeLayer = id, saved = false)
        structuralVersion++
    }

    @Synchronized fun updateLayer(id: LayerId, block: (LayerDescriptor) -> LayerDescriptor) {
        val index = value.layers.indexOfFirst { it.id == id }
        require(index >= 0) { "Layer does not exist: ${id.value}" }
        val next = block(value.layers[index])
        require(next.id == id) { "Layer id is immutable" }
        require(next.name.isNotBlank())
        require(next.opacity in 0f..1f)
        val updated = value.layers.toMutableList().apply { set(index, next) }.sortedBy { it.order }
        value = value.copy(layers = updated, saved = false)
        structuralVersion++
    }

    @Synchronized fun markSaved() { value = value.copy(saved = true) }

    @Synchronized fun fingerprint(): String {
        val canonical = buildString {
            append(value.metadata.id.value).append('\n')
            append(value.metadata.width).append('x').append(value.metadata.height).append('\n')
            append(value.metadata.colorProfile).append('\n')
            append(value.activeLayer?.value ?: "").append('\n')
            value.layers.sortedBy { it.order }.forEach { layer ->
                append(layer.id.value).append('|')
                    .append(layer.name).append('|')
                    .append(layer.visible).append('|')
                    .append("%.5f".format(layer.opacity)).append('|')
                    .append(layer.blendMode).append('|')
                    .append(layer.locked).append('|')
                    .append(layer.order).append('\n')
            }
        }
        return sha256(canonical)
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
