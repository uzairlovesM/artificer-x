package com.waheed.artificerx.core.project

data class ProjectId(val value: String)
data class LayerId(val value: String)

data class ProjectMetadata(
    val id: ProjectId,
    val title: String,
    val width: Int,
    val height: Int,
    val createdAt: Long,
    val modifiedAt: Long,
    val colorProfile: String = "sRGB",
    val revision: Long = 0L
) {
    init {
        require(title.isNotBlank())
        require(width in 1..32_768 && height in 1..32_768)
        require(revision >= 0)
    }
}

data class LayerDescriptor(
    val id: LayerId,
    val name: String,
    val visible: Boolean = true,
    val opacity: Float = 1f,
    val blendMode: String = "normal",
    val locked: Boolean = false,
    val order: Int = 0
) {
    init { require(name.isNotBlank()); require(opacity in 0f..1f) }
}

data class ProjectSnapshot(
    val metadata: ProjectMetadata,
    val layers: List<LayerDescriptor>,
    val activeLayer: LayerId?,
    val saved: Boolean
)
