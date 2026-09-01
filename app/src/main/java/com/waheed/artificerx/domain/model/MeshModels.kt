package com.waheed.artificerx.domain.model

import kotlinx.serialization.Serializable

/**
 * Section 3D-sculpting extension: an indexed triangle mesh, the same
 * representation every real-time 3D engine (Filament included) wants
 * to consume — flat float arrays for GPU upload, not a scene-graph of
 * objects. Kept serializable so a sculpted mesh can be saved/restored
 * through the same ProjectRepository / version-history machinery the
 * 2D layer system already uses (Section 27), rather than a parallel
 * persistence path.
 */
@Serializable
data class Vec3(
    val x: Float,
    val y: Float,
    val z: Float,
) {
    operator fun plus(other: Vec3) = Vec3(x + other.x, y + other.y, z + other.z)

    operator fun minus(other: Vec3) = Vec3(x - other.x, y - other.y, z - other.z)

    operator fun times(scalar: Float) = Vec3(x * scalar, y * scalar, z * scalar)

    fun length(): Float = kotlin.math.sqrt(x * x + y * y + z * z)

    fun normalized(): Vec3 {
        val len = length()
        return if (len < 0.00001f) this else Vec3(x / len, y / len, z / len)
    }
}

@Serializable
data class SculptMesh(
    val id: String,
    val name: String,
    val vertices: List<Vec3>,
    val normals: List<Vec3>,
    val triangleIndices: List<Int>,
    val colorHex: String = "#CCCCCC",
    val position: Vec3 = Vec3(0f, 0f, 0f),
    val rotationEulerDegrees: Vec3 = Vec3(0f, 0f, 0f),
    val scale: Vec3 = Vec3(1f, 1f, 1f),
) {
    val vertexCount: Int get() = vertices.size
    val triangleCount: Int get() = triangleIndices.size / 3
}

enum class PrimitiveType { SPHERE, CUBE, CYLINDER, CONE, PLANE, TORUS }

enum class SculptBrushType { PUSH, PULL, SMOOTH, PINCH, INFLATE, FLATTEN }

data class SculptSceneState(
    val meshes: List<SculptMesh> = emptyList(),
    val activeMeshId: String? = null,
    val activeBrush: SculptBrushType = SculptBrushType.PULL,
    val brushRadius: Float = 0.3f,
    val brushStrength: Float = 0.5f,
    val cameraOrbitYawDegrees: Float = 0f,
    val cameraOrbitPitchDegrees: Float = 20f,
    val cameraDistance: Float = 5f,
)
