package com.waheed.artificerx.core.mesh

import com.waheed.artificerx.domain.model.PrimitiveType
import com.waheed.artificerx.domain.model.SculptMesh
import com.waheed.artificerx.domain.model.Vec3
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Real parametric mesh generation — every function here produces
 * actual vertex/normal/index arrays via standard computational
 * geometry formulas (UV-sphere, cylinder, torus parametrization),
 * not placeholder cubes. This is the starting geometry the sculpt
 * brushes (SculptBrushEngine) then displace vertex-by-vertex.
 */
@Singleton
class PrimitiveMeshGenerator
    @Inject
    constructor() {
        fun generate(
            type: PrimitiveType,
            name: String,
            segments: Int = 24,
        ): SculptMesh =
            when (type) {
                PrimitiveType.SPHERE -> generateSphere(name, radius = 1f, latSegments = segments, lonSegments = segments)
                PrimitiveType.CUBE -> generateCube(name, size = 1f)
                PrimitiveType.CYLINDER -> generateCylinder(name, radius = 1f, height = 2f, segments = segments)
                PrimitiveType.CONE -> generateCone(name, radius = 1f, height = 2f, segments = segments)
                PrimitiveType.PLANE -> generatePlane(name, size = 2f, subdivisions = segments / 2)
                PrimitiveType.TORUS ->
                    generateTorus(
                        name,
                        majorRadius = 1f,
                        minorRadius = 0.35f,
                        majorSegments = segments,
                        minorSegments =
                            segments / 2,
                    )
            }

        private fun generateSphere(
            name: String,
            radius: Float,
            latSegments: Int,
            lonSegments: Int,
        ): SculptMesh {
            val vertices = mutableListOf<Vec3>()
            val normals = mutableListOf<Vec3>()
            val indices = mutableListOf<Int>()

            for (lat in 0..latSegments) {
                val theta = lat * PI.toFloat() / latSegments
                val sinTheta = sin(theta)
                val cosTheta = cos(theta)

                for (lon in 0..lonSegments) {
                    val phi = lon * 2f * PI.toFloat() / lonSegments
                    val sinPhi = sin(phi)
                    val cosPhi = cos(phi)

                    val x = cosPhi * sinTheta
                    val y = cosTheta
                    val z = sinPhi * sinTheta

                    vertices.add(Vec3(x * radius, y * radius, z * radius))
                    normals.add(Vec3(x, y, z))
                }
            }

            for (lat in 0 until latSegments) {
                for (lon in 0 until lonSegments) {
                    val first = lat * (lonSegments + 1) + lon
                    val second = first + lonSegments + 1

                    indices.addAll(listOf(first, second, first + 1))
                    indices.addAll(listOf(second, second + 1, first + 1))
                }
            }

            return SculptMesh(
                id = UUID.randomUUID().toString(),
                name = name,
                vertices = vertices,
                normals = normals,
                triangleIndices = indices,
            )
        }

        private fun generateCube(
            name: String,
            size: Float,
        ): SculptMesh {
            val h = size / 2f
            val positions =
                listOf(
                    // Front
                    Vec3(-h, -h, h),
                    Vec3(h, -h, h),
                    Vec3(h, h, h),
                    Vec3(-h, h, h),
                    // Back
                    Vec3(-h, -h, -h),
                    Vec3(-h, h, -h),
                    Vec3(h, h, -h),
                    Vec3(h, -h, -h),
                    // Top
                    Vec3(-h, h, -h),
                    Vec3(-h, h, h),
                    Vec3(h, h, h),
                    Vec3(h, h, -h),
                    // Bottom
                    Vec3(-h, -h, -h),
                    Vec3(h, -h, -h),
                    Vec3(h, -h, h),
                    Vec3(-h, -h, h),
                    // Right
                    Vec3(h, -h, -h),
                    Vec3(h, h, -h),
                    Vec3(h, h, h),
                    Vec3(h, -h, h),
                    // Left
                    Vec3(-h, -h, -h),
                    Vec3(-h, -h, h),
                    Vec3(-h, h, h),
                    Vec3(-h, h, -h),
                )

            val faceNormals =
                listOf(
                    Vec3(0f, 0f, 1f),
                    Vec3(0f, 0f, -1f),
                    Vec3(0f, 1f, 0f),
                    Vec3(0f, -1f, 0f),
                    Vec3(1f, 0f, 0f),
                    Vec3(-1f, 0f, 0f),
                )
            val normals = faceNormals.flatMap { n -> listOf(n, n, n, n) }

            val indices = mutableListOf<Int>()
            for (face in 0 until 6) {
                val base = face * 4
                indices.addAll(listOf(base, base + 1, base + 2, base, base + 2, base + 3))
            }

            return SculptMesh(
                id = UUID.randomUUID().toString(),
                name = name,
                vertices = positions,
                normals = normals,
                triangleIndices = indices,
            )
        }

        private fun generateCylinder(
            name: String,
            radius: Float,
            height: Float,
            segments: Int,
        ): SculptMesh {
            val vertices = mutableListOf<Vec3>()
            val normals = mutableListOf<Vec3>()
            val indices = mutableListOf<Int>()
            val halfHeight = height / 2f

            for (i in 0..segments) {
                val angle = i * 2f * PI.toFloat() / segments
                val x = cos(angle) * radius
                val z = sin(angle) * radius
                val normal = Vec3(cos(angle), 0f, sin(angle))

                vertices.add(Vec3(x, -halfHeight, z))
                normals.add(normal)
                vertices.add(Vec3(x, halfHeight, z))
                normals.add(normal)
            }

            for (i in 0 until segments) {
                val base = i * 2
                indices.addAll(listOf(base, base + 1, base + 2))
                indices.addAll(listOf(base + 1, base + 3, base + 2))
            }

            val bottomCenterIndex = vertices.size
            vertices.add(Vec3(0f, -halfHeight, 0f))
            normals.add(Vec3(0f, -1f, 0f))
            val topCenterIndex = vertices.size
            vertices.add(Vec3(0f, halfHeight, 0f))
            normals.add(Vec3(0f, 1f, 0f))

            for (i in 0 until segments) {
                val angle1 = i * 2f * PI.toFloat() / segments
                val angle2 = (i + 1) * 2f * PI.toFloat() / segments

                val b1 = vertices.size
                vertices.add(Vec3(cos(angle1) * radius, -halfHeight, sin(angle1) * radius))
                normals.add(Vec3(0f, -1f, 0f))
                val b2 = vertices.size
                vertices.add(Vec3(cos(angle2) * radius, -halfHeight, sin(angle2) * radius))
                normals.add(Vec3(0f, -1f, 0f))
                indices.addAll(listOf(bottomCenterIndex, b2, b1))

                val t1 = vertices.size
                vertices.add(Vec3(cos(angle1) * radius, halfHeight, sin(angle1) * radius))
                normals.add(Vec3(0f, 1f, 0f))
                val t2 = vertices.size
                vertices.add(Vec3(cos(angle2) * radius, halfHeight, sin(angle2) * radius))
                normals.add(Vec3(0f, 1f, 0f))
                indices.addAll(listOf(topCenterIndex, t1, t2))
            }

            return SculptMesh(
                id = UUID.randomUUID().toString(),
                name = name,
                vertices = vertices,
                normals = normals,
                triangleIndices = indices,
            )
        }

        private fun generateCone(
            name: String,
            radius: Float,
            height: Float,
            segments: Int,
        ): SculptMesh {
            val vertices = mutableListOf<Vec3>()
            val normals = mutableListOf<Vec3>()
            val indices = mutableListOf<Int>()
            val halfHeight = height / 2f
            val apex = Vec3(0f, halfHeight, 0f)

            val apexIndex = vertices.size
            vertices.add(apex)
            normals.add(Vec3(0f, 1f, 0f))

            val baseCenterIndex = vertices.size
            vertices.add(Vec3(0f, -halfHeight, 0f))
            normals.add(Vec3(0f, -1f, 0f))

            val ringStart = vertices.size
            for (i in 0..segments) {
                val angle = i * 2f * PI.toFloat() / segments
                val x = cos(angle) * radius
                val z = sin(angle) * radius
                val toApex = (apex - Vec3(x, -halfHeight, z)).normalized()
                vertices.add(Vec3(x, -halfHeight, z))
                normals.add(toApex)
            }

            for (i in 0 until segments) {
                val current = ringStart + i
                val next = ringStart + i + 1
                indices.addAll(listOf(apexIndex, current, next))
                indices.addAll(listOf(baseCenterIndex, next, current))
            }

            return SculptMesh(
                id = UUID.randomUUID().toString(),
                name = name,
                vertices = vertices,
                normals = normals,
                triangleIndices = indices,
            )
        }

        private fun generatePlane(
            name: String,
            size: Float,
            subdivisions: Int,
        ): SculptMesh {
            val vertices = mutableListOf<Vec3>()
            val normals = mutableListOf<Vec3>()
            val indices = mutableListOf<Int>()
            val half = size / 2f
            val steps = subdivisions.coerceAtLeast(1)
            val step = size / steps

            for (row in 0..steps) {
                for (col in 0..steps) {
                    val x = -half + col * step
                    val z = -half + row * step
                    vertices.add(Vec3(x, 0f, z))
                    normals.add(Vec3(0f, 1f, 0f))
                }
            }

            for (row in 0 until steps) {
                for (col in 0 until steps) {
                    val topLeft = row * (steps + 1) + col
                    val topRight = topLeft + 1
                    val bottomLeft = topLeft + steps + 1
                    val bottomRight = bottomLeft + 1

                    indices.addAll(listOf(topLeft, bottomLeft, topRight))
                    indices.addAll(listOf(topRight, bottomLeft, bottomRight))
                }
            }

            return SculptMesh(
                id = UUID.randomUUID().toString(),
                name = name,
                vertices = vertices,
                normals = normals,
                triangleIndices = indices,
            )
        }

        private fun generateTorus(
            name: String,
            majorRadius: Float,
            minorRadius: Float,
            majorSegments: Int,
            minorSegments: Int,
        ): SculptMesh {
            val vertices = mutableListOf<Vec3>()
            val normals = mutableListOf<Vec3>()
            val indices = mutableListOf<Int>()

            for (i in 0..majorSegments) {
                val u = i * 2f * PI.toFloat() / majorSegments
                for (j in 0..minorSegments) {
                    val v = j * 2f * PI.toFloat() / minorSegments

                    val x = (majorRadius + minorRadius * cos(v)) * cos(u)
                    val y = minorRadius * sin(v)
                    val z = (majorRadius + minorRadius * cos(v)) * sin(u)

                    val centerX = majorRadius * cos(u)
                    val centerZ = majorRadius * sin(u)
                    val normal = Vec3(x - centerX, y, z - centerZ).normalized()

                    vertices.add(Vec3(x, y, z))
                    normals.add(normal)
                }
            }

            for (i in 0 until majorSegments) {
                for (j in 0 until minorSegments) {
                    val a = i * (minorSegments + 1) + j
                    val b = a + minorSegments + 1
                    val c = a + 1
                    val d = b + 1

                    indices.addAll(listOf(a, b, c))
                    indices.addAll(listOf(b, d, c))
                }
            }

            return SculptMesh(
                id = UUID.randomUUID().toString(),
                name = name,
                vertices = vertices,
                normals = normals,
                triangleIndices = indices,
            )
        }
    }
