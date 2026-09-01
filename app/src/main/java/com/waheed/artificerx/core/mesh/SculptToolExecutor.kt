package com.waheed.artificerx.core.mesh

import com.waheed.artificerx.core.agent.ToolExecutionResult
import com.waheed.artificerx.domain.model.PrimitiveType
import com.waheed.artificerx.domain.model.SculptBrushType
import com.waheed.artificerx.domain.model.Vec3
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Executes 3D sculpting tool_calls (Section 3D extension of
 * Section 179/180's tool-calling architecture). Structurally
 * identical role to core.agent.ToolExecutor but operates on
 * SculptSceneStore/SculptMesh instead of LayerBitmapStore/Bitmap —
 * kept as a separate executor rather than merged into the 2D one
 * because 2D and 3D tool namespaces never overlap in a single turn
 * (the agent operates in either Studio 2D mode or Sculpt 3D mode,
 * never both simultaneously), so keeping them separate avoids one
 * enormous when-branch mixing two unrelated domains.
 */
@Singleton
class SculptToolExecutor
    @Inject
    constructor(
        private val primitiveMeshGenerator: PrimitiveMeshGenerator,
        private val sculptBrushEngine: SculptBrushEngine,
        private val sceneStore: SculptSceneStore,
    ) {
        fun createPrimitive(
            primitiveType: String,
            name: String,
        ): ToolExecutionResult {
            val type =
                runCatching { PrimitiveType.valueOf(primitiveType.uppercase()) }.getOrNull()
                    ?: return ToolExecutionResult.Failure(
                        "Unknown primitive type '$primitiveType'. Valid types: ${PrimitiveType.entries.map { it.name.lowercase() }}",
                    )

            val mesh = primitiveMeshGenerator.generate(type, name)
            sceneStore.addMesh(mesh)
            return ToolExecutionResult.Success(
                "Created ${type.name.lowercase()} '$name' with ${mesh.vertexCount} vertices, ${mesh.triangleCount} triangles",
                requiresSnapshot = true,
            )
        }

        fun sculptStroke(
            meshId: String,
            brushType: String,
            hitX: Float,
            hitY: Float,
            hitZ: Float,
            radius: Float?,
            strength: Float?,
        ): ToolExecutionResult {
            val mesh =
                sceneStore.getMesh(meshId)
                    ?: return ToolExecutionResult.Failure(
                        "No mesh with id '$meshId' exists. Valid mesh IDs: ${sceneStore.meshes.value.keys}",
                    )

            val brush =
                runCatching { SculptBrushType.valueOf(brushType.uppercase()) }.getOrNull()
                    ?: return ToolExecutionResult.Failure(
                        "Unknown brush type '$brushType'. Valid brushes: ${SculptBrushType.entries.map { it.name.lowercase() }}",
                    )

            val hitPoint = Vec3(hitX, hitY, hitZ)
            val effectiveRadius = (radius ?: 0.3f).coerceIn(0.02f, 5f)
            val effectiveStrength = (strength ?: 0.5f).coerceIn(0.05f, 2f)

            val distanceToClosestVertex = mesh.vertices.minOfOrNull { (it - hitPoint).length() } ?: Float.MAX_VALUE
            if (distanceToClosestVertex > effectiveRadius * 3f) {
                return ToolExecutionResult.Failure(
                    "Hit point ($hitX, $hitY, $hitZ) is too far from the mesh surface (closest vertex is ${"%.2f".format(
                        distanceToClosestVertex,
                    )} units away) — call inspect_scene first to see current mesh bounds.",
                )
            }

            val sculpted = sculptBrushEngine.applyStroke(mesh, brush, hitPoint, effectiveRadius, effectiveStrength)
            sceneStore.updateMesh(sculpted)

            return ToolExecutionResult.Success(
                "Applied ${brush.name.lowercase()} brush at ($hitX, $hitY, $hitZ) with radius $effectiveRadius, strength $effectiveStrength",
                requiresSnapshot = true,
            )
        }

        fun deleteMesh(meshId: String): ToolExecutionResult {
            if (sceneStore.getMesh(meshId) == null) {
                return ToolExecutionResult.Failure("No mesh with id '$meshId' exists.")
            }
            sceneStore.removeMesh(meshId)
            return ToolExecutionResult.Success("Deleted mesh '$meshId'")
        }

        fun setMeshColor(
            meshId: String,
            colorHex: String,
        ): ToolExecutionResult {
            val mesh = sceneStore.getMesh(meshId) ?: return ToolExecutionResult.Failure("No mesh with id '$meshId' exists.")
            sceneStore.updateMesh(mesh.copy(colorHex = colorHex))
            return ToolExecutionResult.Success("Set mesh '$meshId' color to $colorHex", requiresSnapshot = true)
        }

        fun transformMesh(
            meshId: String,
            positionX: Float?,
            positionY: Float?,
            positionZ: Float?,
            rotationXDegrees: Float?,
            rotationYDegrees: Float?,
            rotationZDegrees: Float?,
            scaleX: Float?,
            scaleY: Float?,
            scaleZ: Float?,
        ): ToolExecutionResult {
            val mesh = sceneStore.getMesh(meshId) ?: return ToolExecutionResult.Failure("No mesh with id '$meshId' exists.")

            val newPosition =
                Vec3(
                    positionX ?: mesh.position.x,
                    positionY ?: mesh.position.y,
                    positionZ ?: mesh.position.z,
                )
            val newRotation =
                Vec3(
                    rotationXDegrees ?: mesh.rotationEulerDegrees.x,
                    rotationYDegrees ?: mesh.rotationEulerDegrees.y,
                    rotationZDegrees ?: mesh.rotationEulerDegrees.z,
                )
            val newScale =
                Vec3(
                    scaleX ?: mesh.scale.x,
                    scaleY ?: mesh.scale.y,
                    scaleZ ?: mesh.scale.z,
                )

            sceneStore.updateMesh(mesh.copy(position = newPosition, rotationEulerDegrees = newRotation, scale = newScale))
            return ToolExecutionResult.Success("Transformed mesh '$meshId'", requiresSnapshot = true)
        }

        fun inspectScene(): ToolExecutionResult {
            val meshes = sceneStore.meshes.value.values
            val summary =
                if (meshes.isEmpty()) {
                    "Scene is empty — no meshes exist yet."
                } else {
                    meshes.joinToString("; ") { m ->
                        "'${m.name}' (id=${m.id}, ${m.vertexCount}v/${m.triangleCount}t, pos=${m.position})"
                    }
                }
            return ToolExecutionResult.Success(summary, requiresSnapshot = true)
        }
    }
