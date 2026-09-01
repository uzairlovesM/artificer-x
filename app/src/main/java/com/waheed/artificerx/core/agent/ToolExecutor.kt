package com.waheed.artificerx.core.agent

import com.waheed.artificerx.core.render.CanvasCompositor
import com.waheed.artificerx.core.render.LayerBitmapStore
import com.waheed.artificerx.ui.screens.canvas.StudioViewModel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Executes a ParsedToolCall against the live StudioViewModel state
 * AND the real pixel-level CanvasCompositor (Section 179/180's
 * tool-calling-draws architecture, now with actual bitmaps instead of
 * structural stand-ins). Every draw_* tool call here produces real
 * pixels the agent can later inspect via inspect_canvas's snapshot.
 */
@Singleton
class ToolExecutor
    @Inject
    constructor(
        private val compositor: CanvasCompositor,
        private val bitmapStore: LayerBitmapStore,
        private val sculptToolExecutor: com.waheed.artificerx.core.mesh.SculptToolExecutor,
    ) {
        /** Used when the agent is operating purely in 3D Sculpt mode with
         *  no active 2D Studio session — routes only sculpt/mesh-family
         *  tool calls (and finish_turn), rejecting 2D-only tools with a
         *  clear structured error rather than crashing on a null
         *  StudioViewModel. */
        fun executeSculptOnly(parsedCall: ParsedToolCall): ToolExecutionResult =
            when (parsedCall) {
                is ParsedToolCall.CreatePrimitive -> sculptToolExecutor.createPrimitive(parsedCall.primitiveType, parsedCall.name)
                is ParsedToolCall.SculptStroke ->
                    sculptToolExecutor.sculptStroke(
                        parsedCall.meshId,
                        parsedCall.brushType,
                        parsedCall.hitX,
                        parsedCall.hitY,
                        parsedCall.hitZ,
                        parsedCall.radius,
                        parsedCall.strength,
                    )
                is ParsedToolCall.DeleteMesh -> sculptToolExecutor.deleteMesh(parsedCall.meshId)
                is ParsedToolCall.SetMeshColor -> sculptToolExecutor.setMeshColor(parsedCall.meshId, parsedCall.colorHex)
                is ParsedToolCall.TransformMesh ->
                    sculptToolExecutor.transformMesh(
                        parsedCall.meshId,
                        parsedCall.positionX,
                        parsedCall.positionY,
                        parsedCall.positionZ,
                        parsedCall.rotationXDegrees,
                        parsedCall.rotationYDegrees,
                        parsedCall.rotationZDegrees,
                        parsedCall.scaleX,
                        parsedCall.scaleY,
                        parsedCall.scaleZ,
                    )
                is ParsedToolCall.InspectScene -> sculptToolExecutor.inspectScene()
                is ParsedToolCall.FinishTurn -> ToolExecutionResult.TurnFinished(parsedCall.summary)
                // ParsedToolCall.WebFetch never actually reaches here —
                // AgentOrchestrator.handleUserMessage() intercepts and routes
                // it to executeWebFetch() (a real network call) before either
                // executeSculptOnly() or execute() below is ever invoked, since
                // this class's dispatch runs synchronously on
                // Dispatchers.Main.immediate for canvas-thread safety and was
                // never meant to block on I/O. This branch exists only so the
                // `when` stays exhaustive against ParsedToolCall's full sealed
                // hierarchy; if it's ever hit, that's an orchestrator wiring
                // bug, not a legitimate "wrong mode" situation, hence the
                // distinct message from the generic 2D-Studio-required one below.
                is ParsedToolCall.WebFetch ->
                    ToolExecutionResult.Failure(
                        "Internal error: web_fetch reached ToolExecutor directly instead of being handled by AgentOrchestrator.",
                    )
                else ->
                    ToolExecutionResult.Failure(
                        "Tool '${parsedCall::class.simpleName}' requires 2D Studio mode, which isn't active right now.",
                    )
            }

        fun execute(
            parsedCall: ParsedToolCall,
            viewModel: StudioViewModel,
        ): ToolExecutionResult {
            val currentState = viewModel.state.value

            return when (parsedCall) {
                is ParsedToolCall.CreateLayer -> {
                    viewModel.addLayer()
                    val newState = viewModel.state.value
                    val newLayerId = newState.activeLayerId
                    if (newLayerId != null) {
                        bitmapStore.ensureLayer(newLayerId, newState.canvasWidthPx, newState.canvasHeightPx)
                    }
                    ToolExecutionResult.Success("Created layer '${parsedCall.name}'")
                }

                is ParsedToolCall.DeleteLayer -> {
                    if (currentState.layers.none { it.id == parsedCall.layerId }) {
                        ToolExecutionResult.Failure(
                            "No layer with id '${parsedCall.layerId}' exists. Valid layer IDs: ${currentState.layers.map { it.id }}",
                        )
                    } else if (currentState.layers.size <= 1) {
                        ToolExecutionResult.Failure("Cannot delete the last remaining layer.")
                    } else {
                        viewModel.deleteLayer(parsedCall.layerId)
                        bitmapStore.removeLayer(parsedCall.layerId)
                        ToolExecutionResult.Success("Deleted layer '${parsedCall.layerId}'")
                    }
                }

                is ParsedToolCall.SetActiveLayer -> {
                    if (currentState.layers.none { it.id == parsedCall.layerId }) {
                        ToolExecutionResult.Failure(
                            "No layer with id '${parsedCall.layerId}' exists. Valid layer IDs: ${currentState.layers.map { it.id }}",
                        )
                    } else {
                        viewModel.setActiveLayer(parsedCall.layerId)
                        ToolExecutionResult.Success("Active layer set to '${parsedCall.layerId}'")
                    }
                }

                is ParsedToolCall.DrawPath -> {
                    val activeLayerId = currentState.activeLayerId
                    if (activeLayerId == null) {
                        ToolExecutionResult.Failure("No active layer to draw on. Call create_layer or set_active_layer first.")
                    } else if (parsedCall.points.size < 4 || parsedCall.points.size % 2 != 0) {
                        ToolExecutionResult.Failure(
                            "draw_path requires an even-length points array with at least 2 coordinate pairs (4 numbers).",
                        )
                    } else {
                        ensureBitmapExists(activeLayerId, currentState)
                        val allVariants =
                            mirrorPointsForSymmetry(
                                parsedCall.points,
                                currentState.toolState.symmetryMode,
                                currentState.canvasWidthPx,
                                currentState.canvasHeightPx,
                            )
                        var anySuccess = false
                        allVariants.forEach { variant ->
                            if (compositor.drawPath(
                                    activeLayerId,
                                    variant,
                                    parsedCall.colorHex,
                                    parsedCall.strokeWidthPx,
                                    parsedCall.opacity,
                                )
                            ) {
                                anySuccess = true
                            }
                        }
                        if (anySuccess) {
                            val mirrorNote =
                                if (currentState.toolState.symmetryMode != com.waheed.artificerx.domain.model.SymmetryMode.OFF) {
                                    " (mirrored ${allVariants.size}x via ${currentState.toolState.symmetryMode})"
                                } else {
                                    ""
                                }
                            ToolExecutionResult.Success(
                                "Drew a ${parsedCall.points.size / 2}-point stroke" + (parsedCall.colorHex?.let { " in $it" } ?: "") +
                                    mirrorNote,
                                requiresSnapshot = true,
                            )
                        } else {
                            ToolExecutionResult.Failure("Failed to draw path — layer bitmap unavailable.")
                        }
                    }
                }

                is ParsedToolCall.DrawShape -> {
                    val activeLayerId = currentState.activeLayerId
                    if (activeLayerId == null) {
                        ToolExecutionResult.Failure("No active layer to draw on.")
                    } else {
                        ensureBitmapExists(activeLayerId, currentState)
                        val success =
                            compositor.drawShape(
                                layerId = activeLayerId,
                                shapeType = parsedCall.shapeType,
                                x = parsedCall.x,
                                y = parsedCall.y,
                                width = parsedCall.width,
                                height = parsedCall.height,
                                fillColorHex = parsedCall.fillColorHex,
                                strokeColorHex = parsedCall.strokeColorHex,
                                strokeWidthPx = parsedCall.strokeWidthPx,
                                rotationDegrees = parsedCall.rotationDegrees,
                                sides = parsedCall.sides,
                            )
                        if (success) {
                            ToolExecutionResult.Success(
                                "Drew a ${parsedCall.shapeType} at (${parsedCall.x}, ${parsedCall.y})",
                                requiresSnapshot = true,
                            )
                        } else {
                            ToolExecutionResult.Failure("Failed to draw shape — layer bitmap unavailable.")
                        }
                    }
                }

                is ParsedToolCall.ApplyGradient -> {
                    val activeLayerId = currentState.activeLayerId
                    if (activeLayerId == null) {
                        ToolExecutionResult.Failure("No active layer to apply gradient on.")
                    } else {
                        ensureBitmapExists(activeLayerId, currentState)
                        val success =
                            compositor.applyGradient(
                                layerId = activeLayerId,
                                gradientType = parsedCall.gradientType,
                                startColorHex = parsedCall.startColorHex,
                                endColorHex = parsedCall.endColorHex,
                                x = parsedCall.x,
                                y = parsedCall.y,
                                width = parsedCall.width,
                                height = parsedCall.height,
                                angleDegrees = parsedCall.angleDegrees,
                                additionalColorStopsHex = parsedCall.additionalColorStopsHex,
                            )
                        if (success) {
                            ToolExecutionResult.Success(
                                "Applied ${parsedCall.gradientType} gradient from ${parsedCall.startColorHex} to ${parsedCall.endColorHex}",
                                requiresSnapshot = true,
                            )
                        } else {
                            ToolExecutionResult.Failure("Failed to apply gradient — layer bitmap unavailable.")
                        }
                    }
                }

                is ParsedToolCall.FillRegion -> {
                    val activeLayerId = currentState.activeLayerId
                    if (activeLayerId == null) {
                        ToolExecutionResult.Failure("No active layer to fill.")
                    } else {
                        ensureBitmapExists(activeLayerId, currentState)
                        val success =
                            compositor.fillRegion(
                                layerId = activeLayerId,
                                x = parsedCall.x,
                                y = parsedCall.y,
                                colorHex = parsedCall.colorHex,
                                tolerance = parsedCall.tolerance,
                            )
                        if (success) {
                            ToolExecutionResult.Success(
                                "Filled region at (${parsedCall.x}, ${parsedCall.y}) with ${parsedCall.colorHex}",
                                requiresSnapshot = true,
                            )
                        } else {
                            ToolExecutionResult.Failure("Failed to fill region — layer bitmap unavailable.")
                        }
                    }
                }

                is ParsedToolCall.SetLayerProperty -> {
                    if (currentState.layers.none { it.id == parsedCall.layerId }) {
                        ToolExecutionResult.Failure("No layer with id '${parsedCall.layerId}' exists.")
                    } else {
                        parsedCall.opacity?.let { viewModel.setLayerOpacity(parsedCall.layerId, it) }
                        parsedCall.isVisible?.let {
                            val layer = currentState.layers.first { l -> l.id == parsedCall.layerId }
                            if (layer.isVisible != it) viewModel.toggleLayerVisibility(parsedCall.layerId)
                        }
                        ToolExecutionResult.Success("Updated properties on layer '${parsedCall.layerId}'")
                    }
                }

                is ParsedToolCall.DuplicateLayer -> {
                    if (currentState.layers.none { it.id == parsedCall.sourceLayerId }) {
                        ToolExecutionResult.Failure(
                            "No layer with id '${parsedCall.sourceLayerId}' exists. Valid layer IDs: ${currentState.layers.map { it.id }}",
                        )
                    } else {
                        val success = viewModel.duplicateLayer(parsedCall.sourceLayerId, parsedCall.newName)
                        if (success) {
                            ToolExecutionResult.Success("Duplicated layer '${parsedCall.sourceLayerId}'", requiresSnapshot = true)
                        } else {
                            ToolExecutionResult.Failure("Failed to duplicate layer — source bitmap unavailable.")
                        }
                    }
                }

                is ParsedToolCall.FlipLayer -> {
                    if (currentState.layers.none { it.id == parsedCall.layerId }) {
                        ToolExecutionResult.Failure(
                            "No layer with id '${parsedCall.layerId}' exists. Valid layer IDs: ${currentState.layers.map { it.id }}",
                        )
                    } else if (!parsedCall.horizontal && !parsedCall.vertical) {
                        ToolExecutionResult.Failure("flip_layer requires at least one of horizontal or vertical to be true.")
                    } else {
                        val success = viewModel.flipLayer(parsedCall.layerId, parsedCall.horizontal, parsedCall.vertical)
                        if (success) {
                            val axis =
                                when {
                                    parsedCall.horizontal && parsedCall.vertical -> "both axes"
                                    parsedCall.horizontal -> "horizontally"
                                    else -> "vertically"
                                }
                            ToolExecutionResult.Success("Flipped layer '${parsedCall.layerId}' $axis", requiresSnapshot = true)
                        } else {
                            ToolExecutionResult.Failure("Failed to flip layer — bitmap unavailable.")
                        }
                    }
                }

                is ParsedToolCall.CropCanvas -> {
                    if (parsedCall.width <= 0 || parsedCall.height <= 0) {
                        ToolExecutionResult.Failure("crop_canvas requires positive width and height.")
                    } else {
                        val success = viewModel.cropCanvas(parsedCall.x, parsedCall.y, parsedCall.width, parsedCall.height)
                        if (success) {
                            ToolExecutionResult.Success(
                                "Cropped canvas to ${parsedCall.width}x${parsedCall.height} at (${parsedCall.x}, ${parsedCall.y})",
                                requiresSnapshot = true,
                            )
                        } else {
                            ToolExecutionResult.Failure("Failed to crop canvas.")
                        }
                    }
                }

                is ParsedToolCall.InspectCanvas ->
                    ToolExecutionResult.Success(
                        "Snapshot requested",
                        requiresSnapshot = true,
                    )

                is ParsedToolCall.PickColor -> {
                    val snapshot = viewModel.captureSnapshotNow()
                    val hex = compositor.pickColor(snapshot, parsedCall.x, parsedCall.y)
                    ToolExecutionResult.Success("Color at (${parsedCall.x}, ${parsedCall.y}) is $hex")
                }

                is ParsedToolCall.ApplyFilter -> {
                    if (currentState.layers.none { it.id == parsedCall.layerId }) {
                        ToolExecutionResult.Failure("No layer with id '${parsedCall.layerId}' exists.")
                    } else {
                        val success = compositor.applyFilter(parsedCall.layerId, parsedCall.filterType, parsedCall.intensity)
                        if (success) {
                            ToolExecutionResult.Success(
                                "Applied ${parsedCall.filterType} filter to layer '${parsedCall.layerId}'",
                                requiresSnapshot = true,
                            )
                        } else {
                            ToolExecutionResult.Failure("Unknown filter type '${parsedCall.filterType}' or layer bitmap unavailable.")
                        }
                    }
                }

                is ParsedToolCall.AddText -> {
                    val activeLayerId = currentState.activeLayerId
                    if (activeLayerId == null) {
                        ToolExecutionResult.Failure("No active layer to add text on.")
                    } else {
                        ensureBitmapExists(activeLayerId, currentState)
                        val success =
                            compositor.addText(
                                activeLayerId,
                                parsedCall.text,
                                parsedCall.x,
                                parsedCall.y,
                                parsedCall.fontSizePx,
                                parsedCall.colorHex,
                                parsedCall.bold,
                            )
                        if (success) {
                            ToolExecutionResult.Success(
                                "Added text '${parsedCall.text}' at (${parsedCall.x}, ${parsedCall.y})",
                                requiresSnapshot = true,
                            )
                        } else {
                            ToolExecutionResult.Failure("Failed to add text — layer bitmap unavailable.")
                        }
                    }
                }

                is ParsedToolCall.CreateMask -> {
                    if (currentState.layers.none { it.id == parsedCall.layerId }) {
                        ToolExecutionResult.Failure("No layer with id '${parsedCall.layerId}' exists.")
                    } else {
                        val success =
                            compositor.createMask(
                                parsedCall.layerId,
                                parsedCall.maskShape,
                                parsedCall.x,
                                parsedCall.y,
                                parsedCall.width,
                                parsedCall.height,
                                parsedCall.invert,
                            )
                        if (success) {
                            ToolExecutionResult.Success(
                                "Applied ${parsedCall.maskShape} mask to layer '${parsedCall.layerId}'",
                                requiresSnapshot = true,
                            )
                        } else {
                            ToolExecutionResult.Failure("Failed to create mask — layer bitmap unavailable.")
                        }
                    }
                }

                is ParsedToolCall.EnableSymmetry -> {
                    val mode =
                        when (parsedCall.mode.lowercase()) {
                            "vertical" -> com.waheed.artificerx.domain.model.SymmetryMode.VERTICAL
                            "horizontal" -> com.waheed.artificerx.domain.model.SymmetryMode.HORIZONTAL
                            "radial_4" -> com.waheed.artificerx.domain.model.SymmetryMode.RADIAL_4
                            "radial_8" -> com.waheed.artificerx.domain.model.SymmetryMode.RADIAL_8
                            else -> com.waheed.artificerx.domain.model.SymmetryMode.OFF
                        }
                    viewModel.setSymmetryMode(mode)
                    ToolExecutionResult.Success("Symmetry mode set to ${parsedCall.mode}")
                }

                is ParsedToolCall.ApplyPattern -> {
                    val activeLayerId = currentState.activeLayerId
                    if (activeLayerId == null) {
                        ToolExecutionResult.Failure("No active layer to apply pattern on.")
                    } else {
                        ensureBitmapExists(activeLayerId, currentState)
                        val success =
                            compositor.applyPattern(
                                activeLayerId,
                                parsedCall.patternType,
                                parsedCall.x,
                                parsedCall.y,
                                parsedCall.width,
                                parsedCall.height,
                                parsedCall.colorHex,
                                parsedCall.scalePx,
                            )
                        if (success) {
                            ToolExecutionResult.Success(
                                "Applied ${parsedCall.patternType} pattern at (${parsedCall.x}, ${parsedCall.y})",
                                requiresSnapshot = true,
                            )
                        } else {
                            ToolExecutionResult.Failure("Failed to apply pattern — layer bitmap unavailable.")
                        }
                    }
                }

                is ParsedToolCall.DrawCurve -> {
                    val activeLayerId = currentState.activeLayerId
                    if (activeLayerId == null) {
                        ToolExecutionResult.Failure("No active layer to draw on.")
                    } else {
                        ensureBitmapExists(activeLayerId, currentState)
                        val success =
                            compositor.drawCurve(
                                activeLayerId,
                                parsedCall.startX,
                                parsedCall.startY,
                                parsedCall.controlX,
                                parsedCall.controlY,
                                parsedCall.endX,
                                parsedCall.endY,
                                parsedCall.colorHex,
                                parsedCall.strokeWidthPx,
                            )
                        if (success) {
                            ToolExecutionResult.Success(
                                "Drew a curve from (${parsedCall.startX}, ${parsedCall.startY}) to (${parsedCall.endX}, ${parsedCall.endY})",
                                requiresSnapshot = true,
                            )
                        } else {
                            ToolExecutionResult.Failure("Failed to draw curve — layer bitmap unavailable.")
                        }
                    }
                }

                is ParsedToolCall.ImportImageLayer -> {
                    val attachedBitmap = viewModel.consumePendingAttachedImage()
                    if (attachedBitmap == null) {
                        ToolExecutionResult.Failure("No reference image is currently attached by the user to import.")
                    } else {
                        viewModel.addLayer()
                        val newLayerId = viewModel.state.value.activeLayerId
                        if (newLayerId == null) {
                            ToolExecutionResult.Failure("Failed to create layer for imported image.")
                        } else {
                            bitmapStore.ensureLayer(newLayerId, currentState.canvasWidthPx, currentState.canvasHeightPx)
                            val success =
                                compositor.importImageLayer(
                                    newLayerId,
                                    attachedBitmap,
                                    currentState.canvasWidthPx,
                                    currentState.canvasHeightPx,
                                    parsedCall.opacity,
                                )
                            if (success) {
                                ToolExecutionResult.Success(
                                    "Imported reference image as layer '${parsedCall.layerName}'",
                                    requiresSnapshot = true,
                                )
                            } else {
                                ToolExecutionResult.Failure("Failed to import image into new layer.")
                            }
                        }
                    }
                }

                is ParsedToolCall.CreatePrimitive ->
                    sculptToolExecutor.createPrimitive(parsedCall.primitiveType, parsedCall.name)

                is ParsedToolCall.SculptStroke ->
                    sculptToolExecutor.sculptStroke(
                        parsedCall.meshId,
                        parsedCall.brushType,
                        parsedCall.hitX,
                        parsedCall.hitY,
                        parsedCall.hitZ,
                        parsedCall.radius,
                        parsedCall.strength,
                    )

                is ParsedToolCall.DeleteMesh -> sculptToolExecutor.deleteMesh(parsedCall.meshId)

                is ParsedToolCall.SetMeshColor -> sculptToolExecutor.setMeshColor(parsedCall.meshId, parsedCall.colorHex)

                is ParsedToolCall.TransformMesh ->
                    sculptToolExecutor.transformMesh(
                        parsedCall.meshId,
                        parsedCall.positionX,
                        parsedCall.positionY,
                        parsedCall.positionZ,
                        parsedCall.rotationXDegrees,
                        parsedCall.rotationYDegrees,
                        parsedCall.rotationZDegrees,
                        parsedCall.scaleX,
                        parsedCall.scaleY,
                        parsedCall.scaleZ,
                    )

                is ParsedToolCall.InspectScene -> sculptToolExecutor.inspectScene()

                is ParsedToolCall.FinishTurn -> ToolExecutionResult.TurnFinished(parsedCall.summary)

                // See the matching comment on executeSculptOnly() above:
                // WebFetch is always intercepted and handled by
                // AgentOrchestrator before either of ToolExecutor's two
                // entry points is invoked. This branch exists purely to
                // keep this `when` exhaustive over ParsedToolCall's full
                // sealed hierarchy — without it, adding WebFetch to
                // ParsedToolCall would have been a real compile error here
                // (this `when` has no `else`, unlike executeSculptOnly()'s).
                is ParsedToolCall.WebFetch ->
                    ToolExecutionResult.Failure(
                        "Internal error: web_fetch reached ToolExecutor directly instead of being handled by AgentOrchestrator.",
                    )

                is ParsedToolCall.Unknown ->
                    ToolExecutionResult.Failure(
                        "Unknown tool '${parsedCall.toolName}'. Available tools: ${ToolRegistry.ALL_TOOLS.map { it.function.name }}",
                    )
            }
        }

        /** Section symmetry tool: generates mirrored point-array variants
         *  of a single stroke so drawPath can be called once per mirror
         *  axis, producing simultaneous symmetric strokes (mandala/face/
         *  character-design workflows) without the agent needing to
         *  compute mirror math itself in every draw_path call. */
        private fun mirrorPointsForSymmetry(
            points: List<Float>,
            mode: com.waheed.artificerx.domain.model.SymmetryMode,
            canvasWidth: Int,
            canvasHeight: Int,
        ): List<List<Float>> {
            if (mode == com.waheed.artificerx.domain.model.SymmetryMode.OFF) return listOf(points)

            val centerX = canvasWidth / 2f
            val centerY = canvasHeight / 2f

            fun mirrorVertical(pts: List<Float>): List<Float> = pts.mapIndexed { i, v -> if (i % 2 == 0) (2 * centerX - v) else v }

            fun mirrorHorizontal(pts: List<Float>): List<Float> = pts.mapIndexed { i, v -> if (i % 2 == 1) (2 * centerY - v) else v }

            fun rotateAround(
                pts: List<Float>,
                degrees: Double,
            ): List<Float> {
                val radians = Math.toRadians(degrees)
                val cos = Math.cos(radians).toFloat()
                val sin = Math.sin(radians).toFloat()
                val result = mutableListOf<Float>()
                var i = 0
                while (i + 1 < pts.size) {
                    val dx = pts[i] - centerX
                    val dy = pts[i + 1] - centerY
                    result.add(centerX + dx * cos - dy * sin)
                    result.add(centerY + dx * sin + dy * cos)
                    i += 2
                }
                return result
            }

            return when (mode) {
                com.waheed.artificerx.domain.model.SymmetryMode.VERTICAL -> listOf(points, mirrorVertical(points))
                com.waheed.artificerx.domain.model.SymmetryMode.HORIZONTAL -> listOf(points, mirrorHorizontal(points))
                com.waheed.artificerx.domain.model.SymmetryMode.RADIAL_4 -> listOf(0.0, 90.0, 180.0, 270.0).map { rotateAround(points, it) }
                com.waheed.artificerx.domain.model.SymmetryMode.RADIAL_8 -> (0 until 8).map { rotateAround(points, it * 45.0) }
                com.waheed.artificerx.domain.model.SymmetryMode.OFF -> listOf(points)
            }
        }

        private fun ensureBitmapExists(
            layerId: String,
            state: com.waheed.artificerx.domain.model.CanvasProjectState,
        ) {
            if (!bitmapStore.hasLayer(layerId)) {
                bitmapStore.ensureLayer(layerId, state.canvasWidthPx, state.canvasHeightPx)
            }
        }
    }
