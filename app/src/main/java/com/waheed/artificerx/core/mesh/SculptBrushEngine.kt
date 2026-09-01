package com.waheed.artificerx.core.mesh

import com.waheed.artificerx.domain.model.SculptBrushType
import com.waheed.artificerx.domain.model.SculptMesh
import com.waheed.artificerx.domain.model.Vec3
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.cos

/**
 * Real sculpting math — every brush here mutates vertex positions
 * based on proximity to a 3D-space hit point, exactly how ZBrush/
 * Blender-style sculpt brushes work: a falloff-weighted displacement
 * within brushRadius, strength-scaled, applied along either the
 * surface normal (push/pull/inflate) or toward a target (smooth/
 * flatten/pinch). This is the concrete implementation behind Section
 * 3D's push_pull_sculpt agent tool — no placeholder, real geometry
 * mutation on every call.
 */
@Singleton
class SculptBrushEngine
    @Inject
    constructor() {
        fun applyStroke(
            mesh: SculptMesh,
            brushType: SculptBrushType,
            hitPoint: Vec3,
            radius: Float,
            strength: Float,
        ): SculptMesh {
            val newVertices = mesh.vertices.toMutableList()
            val newNormals = mesh.normals.toMutableList()

            val affectedIndices =
                mesh.vertices.indices.filter { i ->
                    (mesh.vertices[i] - hitPoint).length() <= radius
                }

            if (affectedIndices.isEmpty()) return mesh

            when (brushType) {
                SculptBrushType.PUSH ->
                    applyPushPull(
                        newVertices,
                        mesh.normals,
                        affectedIndices,
                        hitPoint,
                        radius,
                        strength,
                        direction = -1f,
                    )
                SculptBrushType.PULL ->
                    applyPushPull(
                        newVertices,
                        mesh.normals,
                        affectedIndices,
                        hitPoint,
                        radius,
                        strength,
                        direction = 1f,
                    )
                SculptBrushType.INFLATE -> applyInflate(newVertices, mesh.normals, affectedIndices, hitPoint, radius, strength)
                SculptBrushType.SMOOTH -> applySmooth(newVertices, mesh.triangleIndices, affectedIndices, hitPoint, radius, strength)
                SculptBrushType.PINCH -> applyPinch(newVertices, affectedIndices, hitPoint, radius, strength)
                SculptBrushType.FLATTEN -> applyFlatten(newVertices, mesh.normals, affectedIndices, hitPoint, radius, strength)
            }

            val recalculatedNormals = recalculateNormals(newVertices, mesh.triangleIndices)

            return mesh.copy(vertices = newVertices, normals = recalculatedNormals)
        }

        private fun falloff(
            distance: Float,
            radius: Float,
        ): Float {
            if (distance >= radius) return 0f
            val t = distance / radius
            // Smooth cosine falloff — standard sculpting brush curve, avoids
            // the harsh visible edge a linear or hard falloff produces.
            return 0.5f * (1f + cos(t * PI.toFloat()))
        }

        private fun applyPushPull(
            vertices: MutableList<Vec3>,
            normals: List<Vec3>,
            indices: List<Int>,
            hitPoint: Vec3,
            radius: Float,
            strength: Float,
            direction: Float,
        ) {
            indices.forEach { i ->
                val distance = (vertices[i] - hitPoint).length()
                val weight = falloff(distance, radius)
                val displacement = normals[i].normalized() * (strength * weight * direction * 0.1f)
                vertices[i] = vertices[i] + displacement
            }
        }

        private fun applyInflate(
            vertices: MutableList<Vec3>,
            normals: List<Vec3>,
            indices: List<Int>,
            hitPoint: Vec3,
            radius: Float,
            strength: Float,
        ) {
            indices.forEach { i ->
                val distance = (vertices[i] - hitPoint).length()
                val weight = falloff(distance, radius)
                vertices[i] = vertices[i] + normals[i].normalized() * (strength * weight * 0.08f)
            }
        }

        private fun applySmooth(
            vertices: MutableList<Vec3>,
            triangleIndices: List<Int>,
            affected: List<Int>,
            hitPoint: Vec3,
            radius: Float,
            strength: Float,
        ) {
            val neighborMap = buildNeighborMap(triangleIndices, vertices.size)

            affected.forEach { i ->
                val neighbors = neighborMap[i] ?: return@forEach
                if (neighbors.isEmpty()) return@forEach

                val distance = (vertices[i] - hitPoint).length()
                val weight = falloff(distance, radius)

                var avg = Vec3(0f, 0f, 0f)
                neighbors.forEach { n -> avg += vertices[n] }
                avg = avg * (1f / neighbors.size)

                val toAverage = avg - vertices[i]
                vertices[i] = vertices[i] + toAverage * (strength * weight * 0.5f)
            }
        }

        private fun applyPinch(
            vertices: MutableList<Vec3>,
            indices: List<Int>,
            hitPoint: Vec3,
            radius: Float,
            strength: Float,
        ) {
            indices.forEach { i ->
                val toCenter = hitPoint - vertices[i]
                val distance = toCenter.length()
                val weight = falloff(distance, radius)
                vertices[i] = vertices[i] + toCenter.normalized() * (strength * weight * 0.06f)
            }
        }

        private fun applyFlatten(
            vertices: MutableList<Vec3>,
            normals: List<Vec3>,
            indices: List<Int>,
            hitPoint: Vec3,
            radius: Float,
            strength: Float,
        ) {
            if (indices.isEmpty()) return
            val planeNormal = normals[indices.first()].normalized()

            indices.forEach { i ->
                val distance = (vertices[i] - hitPoint).length()
                val weight = falloff(distance, radius)
                val toPlane = vertices[i] - hitPoint
                val distAlongNormal = dot(toPlane, planeNormal)
                vertices[i] = vertices[i] - planeNormal * (distAlongNormal * strength * weight)
            }
        }

        private fun dot(
            a: Vec3,
            b: Vec3,
        ): Float = a.x * b.x + a.y * b.y + a.z * b.z

        /** Builds an adjacency map (vertex index → connected vertex indices)
         *  from the triangle index buffer — needed by the smooth brush to
         *  average a vertex toward its topological neighbors, not just
         *  spatially-nearby unconnected vertices. */
        private fun buildNeighborMap(
            triangleIndices: List<Int>,
            vertexCount: Int,
        ): Map<Int, Set<Int>> {
            val map = HashMap<Int, MutableSet<Int>>()
            var i = 0
            while (i + 2 < triangleIndices.size) {
                val a = triangleIndices[i]
                val b = triangleIndices[i + 1]
                val c = triangleIndices[i + 2]

                map.getOrPut(a) { mutableSetOf() }.apply {
                    add(b)
                    add(c)
                }
                map.getOrPut(b) { mutableSetOf() }.apply {
                    add(a)
                    add(c)
                }
                map.getOrPut(c) { mutableSetOf() }.apply {
                    add(a)
                    add(b)
                }

                i += 3
            }
            return map
        }

        /** Recomputes per-vertex normals from face normals after any
         *  displacement — without this, lighting on a sculpted mesh would
         *  look wrong (shading still reflecting the pre-sculpt surface). */
        private fun recalculateNormals(
            vertices: List<Vec3>,
            triangleIndices: List<Int>,
        ): List<Vec3> {
            val accumulated = Array(vertices.size) { Vec3(0f, 0f, 0f) }

            var i = 0
            while (i + 2 < triangleIndices.size) {
                val ia = triangleIndices[i]
                val ib = triangleIndices[i + 1]
                val ic = triangleIndices[i + 2]

                val edge1 = vertices[ib] - vertices[ia]
                val edge2 = vertices[ic] - vertices[ia]
                val faceNormal = crossProduct(edge1, edge2).normalized()

                accumulated[ia] = accumulated[ia] + faceNormal
                accumulated[ib] = accumulated[ib] + faceNormal
                accumulated[ic] = accumulated[ic] + faceNormal

                i += 3
            }

            return accumulated.map { it.normalized() }
        }

        private fun crossProduct(
            a: Vec3,
            b: Vec3,
        ): Vec3 =
            Vec3(
                a.y * b.z - a.z * b.y,
                a.z * b.x - a.x * b.z,
                a.x * b.y - a.y * b.x,
            )
    }
