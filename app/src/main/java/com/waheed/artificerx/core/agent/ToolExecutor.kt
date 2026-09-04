package com.waheed.artificerx.core.agent

import com.waheed.artificerx.core.runtime.RuntimeToolCatalog
import com.waheed.artificerx.core.builtin.BuiltinRecipeCatalog
import com.waheed.artificerx.core.builtin.BuiltinRecipeExecutor
import com.waheed.artificerx.core.builtin.BuiltinRecipeTools
import com.waheed.artificerx.core.runtime.RuntimeToolExecutor
import com.waheed.artificerx.core.render.CanvasCompositor
import com.waheed.artificerx.core.render.LayerBitmapStore
import com.waheed.artificerx.ui.screens.canvas.StudioViewModel
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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
        private val artifactStore: com.waheed.artificerx.core.artifact.ArtifactStore,
        private val terminalSandbox: com.waheed.artificerx.core.terminal.TerminalSandbox,
        private val imageGenerationService: com.waheed.artificerx.core.image.ImageGenerationService,
        private val memoryRepository: com.waheed.artificerx.data.workspace.MemoryRepository,
        private val workspaceRepository: com.waheed.artificerx.data.repository.ChatWorkspaceRepository,
        private val workspaceSearch: com.waheed.artificerx.core.search.WorkspaceSearch,
        private val workspaceBundleService: com.waheed.artificerx.core.importexport.WorkspaceBundleService,
        private val workspaceFileTools: com.waheed.artificerx.core.storage.WorkspaceFileTools,
        private val runtimeToolExecutor: RuntimeToolExecutor,
        private val builtinRecipeCatalog: BuiltinRecipeCatalog,
        private val builtinRecipeExecutor: BuiltinRecipeExecutor,
        private val nativeRasterCore: com.waheed.artificerx.core.nativeops.NativeRasterCore,
        private val sceneCompositionEngine: com.waheed.artificerx.core.creative.SceneCompositionEngine,
        private val androidToolchainManager: com.waheed.artificerx.core.terminal.AndroidToolchainManager,
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
                is ParsedToolCall.ReadWorkspaceFile,
                is ParsedToolCall.WriteWorkspaceFile,
                is ParsedToolCall.ListWorkspaceDirectory,
                is ParsedToolCall.ReplaceWorkspaceText,
                is ParsedToolCall.GenerateImage,
                is ParsedToolCall.ComposeScene,
                is ParsedToolCall.InspectAndroidToolchain,
                is ParsedToolCall.Remember,
                is ParsedToolCall.Recall,
                is ParsedToolCall.CreateFile,
                is ParsedToolCall.CreateZip,
                is ParsedToolCall.RunTerminalCommand,
                is ParsedToolCall.RunTerminalBatch,
                is ParsedToolCall.ListArtifacts,
                is ParsedToolCall.SearchWorkspace,
                is ParsedToolCall.ArtifactInfo,
                is ParsedToolCall.ChecksumArtifact,
                is ParsedToolCall.WorkspaceStatus,
                is ParsedToolCall.Dynamic -> ToolExecutionResult.Failure("This tool requires the 2D workspace execution context.")
                is ParsedToolCall.Invalid -> ToolExecutionResult.Failure(parsedCall.reasons.joinToString(" "))
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

        /** Tool calls that mutate canvas pixels — every one of these must
         *  be preceded by an undo snapshot so a human can Ctrl+Z an
         *  agent-driven stroke exactly like a manual one. Layer-metadata-
         *  only calls (SetActiveLayer, SetLayerProperty, InspectCanvas,
         *  PickColor, etc.) are deliberately excluded — undo is scoped to
         *  pixels, matching StudioViewModel.undo()'s own doc comment. */
        private fun mutatesPixels(parsedCall: ParsedToolCall): Boolean =
            when (parsedCall) {
                is ParsedToolCall.DrawPath,
                is ParsedToolCall.DrawShape,
                is ParsedToolCall.ApplyGradient,
                is ParsedToolCall.FillRegion,
                is ParsedToolCall.DuplicateLayer,
                is ParsedToolCall.FlipLayer,
                is ParsedToolCall.CropCanvas,
                is ParsedToolCall.ApplyFilter,
                is ParsedToolCall.AddText,
                is ParsedToolCall.CreateMask,
                is ParsedToolCall.ApplyPattern,
                is ParsedToolCall.DrawCurve,
                is ParsedToolCall.ImportImageLayer,
                is ParsedToolCall.ComposeScene,
                is ParsedToolCall.DeleteLayer,
                is ParsedToolCall.SetCanvasBackground,
                -> true
                else -> false
            }

        suspend fun execute(
            parsedCall: ParsedToolCall,
            viewModel: StudioViewModel,
        ): ToolExecutionResult {
            val currentState = viewModel.state.value
            if (mutatesPixels(parsedCall)) {
                bitmapStore.pushUndoSnapshot()
            }

            val result = when (parsedCall) {
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
                            val drawn = if (currentState.layers.firstOrNull { it.id == activeLayerId }?.alphaLock == true) {
                                compositor.drawPathAlphaLocked(
                                    activeLayerId,
                                    variant,
                                    parsedCall.colorHex ?: currentState.toolState.brushColorHex,
                                    parsedCall.strokeWidthPx ?: currentState.toolState.brushSizePx,
                                    parsedCall.opacity ?: currentState.toolState.brushOpacity,
                                    brushType = parsedCall.brushType ?: currentState.toolState.brushType,
                                )
                            } else {
                                compositor.drawPath(
                                    activeLayerId,
                                    variant,
                                    parsedCall.colorHex ?: currentState.toolState.brushColorHex,
                                    parsedCall.strokeWidthPx ?: currentState.toolState.brushSizePx,
                                    parsedCall.opacity ?: currentState.toolState.brushOpacity,
                                    brushType = parsedCall.brushType ?: currentState.toolState.brushType,
                                )
                            }
                            if (drawn) {
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

                is ParsedToolCall.Remember -> {
                    if (parsedCall.value.isBlank()) ToolExecutionResult.Failure("remember requires a non-empty value")
                    else {
                        memoryRepository.remember(parsedCall.namespace, parsedCall.key, parsedCall.value, currentState.projectId)
                        ToolExecutionResult.Success("Remembered ${parsedCall.key} in ${parsedCall.namespace}.")
                    }
                }

                is ParsedToolCall.Recall -> {
                    val found = memoryRepository.recall(parsedCall.namespace, parsedCall.query)
                    if (found.isEmpty()) ToolExecutionResult.Success("No matching memories found in ${parsedCall.namespace}.")
                    else ToolExecutionResult.Success(found.joinToString("\n") { "${it.key}: ${it.value}" })
                }

                is ParsedToolCall.ReadWorkspaceFile -> {
                    workspaceFileTools.read(parsedCall.path, parsedCall.maxChars).fold(
                        onSuccess = { ToolExecutionResult.Success("FILE_BEGIN ${parsedCall.path}\n$it\nFILE_END") },
                        onFailure = { ToolExecutionResult.Failure(it.message ?: "Unable to read workspace file") },
                    )
                }
                is ParsedToolCall.WriteWorkspaceFile -> {
                    workspaceFileTools.write(parsedCall.path, parsedCall.content).fold(
                        onSuccess = { ToolExecutionResult.Success("Wrote workspace file at ${it.absolutePath}") },
                        onFailure = { ToolExecutionResult.Failure(it.message ?: "Unable to write workspace file") },
                    )
                }
                is ParsedToolCall.ListWorkspaceDirectory -> {
                    workspaceFileTools.list(parsedCall.path).fold(
                        onSuccess = { ToolExecutionResult.Success(it.joinToString("\n")) },
                        onFailure = { ToolExecutionResult.Failure(it.message ?: "Unable to list directory") },
                    )
                }
                is ParsedToolCall.ReplaceWorkspaceText -> {
                    workspaceFileTools.replace(parsedCall.path, parsedCall.old, parsedCall.new, parsedCall.all).fold(
                        onSuccess = { ToolExecutionResult.Success("Patched ${it.absolutePath}") },
                        onFailure = { ToolExecutionResult.Failure(it.message ?: "Unable to patch file") },
                    )
                }

                is ParsedToolCall.GenerateImage -> {
                    val result = imageGenerationService.generate(currentState.projectId ?: "default", parsedCall.prompt, parsedCall.size, parsedCall.model)
                    result.fold(
                        onSuccess = { image -> ToolExecutionResult.Success("MEDIA_URI=${image.uri}\nARTIFACT_NAME=${image.fileName}\nPATH=${image.path}\nSIZE=${image.sizeBytes}") },
                        onFailure = { ToolExecutionResult.Failure(it.message ?: "Image generation failed") },
                    )
                }

                is ParsedToolCall.CreateFile -> {
                    val threadId = currentState.projectId ?: "default"
                    val ref = artifactStore.writeText(threadId, parsedCall.fileName, parsedCall.content, parsedCall.mimeType, "create_file")
                    ToolExecutionResult.Success("Created artifact ${ref.name} (${ref.sizeBytes} bytes) at ${ref.path}")
                }

                is ParsedToolCall.CreateZip -> {
                    val inputs = runCatching {
                        kotlinx.serialization.json.Json.parseToJsonElement(parsedCall.filesJson).jsonArray.mapNotNull { element ->
                            val obj = element.jsonObject
                            val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                            com.waheed.artificerx.core.artifact.ArtifactInput(
                                name = name,
                                bytes = (obj["content"]?.jsonPrimitive?.contentOrNull ?: "").toByteArray(Charsets.UTF_8),
                                mimeType = obj["mime_type"]?.jsonPrimitive?.contentOrNull ?: "text/plain",
                            )
                        }
                    }.getOrElse { emptyList() }
                    if (inputs.isEmpty()) {
                        ToolExecutionResult.Failure("create_zip requires a non-empty JSON files array")
                    } else {
                        val ref = artifactStore.writeZip(currentState.projectId ?: "default", parsedCall.fileName, inputs, "create_zip")
                        ToolExecutionResult.Success("Created ZIP ${ref.name} (${ref.sizeBytes} bytes) at ${ref.path}")
                    }
                }

                is ParsedToolCall.RunTerminalCommand -> {
                    val result = terminalSandbox.run(parsedCall.command, parsedCall.timeoutSeconds.toLong())
                    ToolExecutionResult.Success("exit=${result.exitCode}\nstdout=${result.stdout}\nstderr=${result.stderr}")
                }

                is ParsedToolCall.RunTerminalBatch -> {
                    val results = terminalSandbox.runBatch(parsedCall.commands, parsedCall.timeoutSeconds.toLong())
                    ToolExecutionResult.Success(results.joinToString("\n\n") { "${it.command} -> exit=${it.exitCode}\nstdout=${it.stdout}\nstderr=${it.stderr}" })
                }

                is ParsedToolCall.ListArtifacts -> {
                    val artifacts = workspaceRepository.observeArtifacts(currentState.projectId ?: "default").first()
                    val filtered = parsedCall.query?.takeIf { it.isNotBlank() }?.let { q -> artifacts.filter { it.name.contains(q, true) || it.mimeType.contains(q, true) } } ?: artifacts
                    if (filtered.isEmpty()) ToolExecutionResult.Success("No matching artifacts.")
                    else ToolExecutionResult.Success(filtered.joinToString("\n") { "${it.id} | ${it.name} | ${it.mimeType} | ${it.sizeBytes} bytes" })
                }

                is ParsedToolCall.SearchWorkspace -> {
                    if (parsedCall.query.isBlank()) ToolExecutionResult.Failure("search_workspace requires a non-empty query")
                    else {
                        val results = workspaceSearch.search(parsedCall.query)
                        if (results.isEmpty()) ToolExecutionResult.Success("No workspace matches for '${parsedCall.query}'.")
                        else ToolExecutionResult.Success(results.joinToString("\n") { "${it.kind} | ${it.id} | ${it.title} | ${it.subtitle}" })
                    }
                }

                is ParsedToolCall.ArtifactInfo -> {
                    val artifact = workspaceRepository.getArtifact(parsedCall.artifactId)
                    if (artifact == null) ToolExecutionResult.Failure("Artifact '${parsedCall.artifactId}' was not found.")
                    else {
                        val validation = com.waheed.artificerx.core.insights.ArtifactValidator.validate(artifact.path)
                        ToolExecutionResult.Success("id=${artifact.id}\nname=${artifact.name}\nmime=${artifact.mimeType}\npath=${artifact.path}\nrecordedSize=${artifact.sizeBytes}\nactualSize=${validation.sizeBytes}\nvalidation=${validation.reason}")
                    }
                }

                is ParsedToolCall.ChecksumArtifact -> {
                    val artifact = workspaceRepository.getArtifact(parsedCall.artifactId)
                    if (artifact == null) ToolExecutionResult.Failure("Artifact '${parsedCall.artifactId}' was not found.")
                    else {
                        val file = java.io.File(artifact.path)
                        if (!file.isFile) ToolExecutionResult.Failure("Artifact file is unavailable at ${artifact.path}")
                        else {
                            val digest = java.security.MessageDigest.getInstance("SHA-256")
                            file.inputStream().use { input ->
                                val buffer = ByteArray(DEFAULT_CHECKSUM_BUFFER)
                                while (true) {
                                    val count = input.read(buffer)
                                    if (count <= 0) break
                                    digest.update(buffer, 0, count)
                                }
                            }
                            ToolExecutionResult.Success("SHA-256=${digest.digest().joinToString("") { "%02x".format(it) }}")
                        }
                    }
                }

                is ParsedToolCall.WorkspaceStatus -> {
                    val snapshot = com.waheed.artificerx.core.insights.WorkspaceInsights.snapshot()
                    ToolExecutionResult.Success("wiringScore=${snapshot.wiringScore}%\nplugins=${snapshot.pluginCount}\ntools=${snapshot.toolCount}\nhealthyFeatures=${snapshot.healthyFeatures}/${snapshot.totalFeatures}")
                }

                is ParsedToolCall.ExportWorkspaceBundle -> {
                    val ref = workspaceBundleService.exportThread(currentState.projectId ?: "default")
                    ToolExecutionResult.Success("Workspace bundle created: ${ref.name} (${ref.sizeBytes} bytes), artifactId=${ref.id}")
                }

                is ParsedToolCall.InvokeBuiltinRecipe -> {
                    val args = runCatching {
                        kotlinx.serialization.json.Json.parseToJsonElement(parsedCall.argsJson).jsonObject
                            .mapValues { it.value.jsonPrimitive.content }
                    }.getOrElse {
                        return ToolExecutionResult.Failure("Invalid built-in recipe arguments: ${it.message ?: "invalid JSON"}")
                    }
                    builtinRecipeExecutor.execute(parsedCall.recipeId, args)
                }

                is ParsedToolCall.SearchBuiltinRecipes -> {
                    BuiltinRecipeTools.summarize(builtinRecipeCatalog.search(parsedCall.query, parsedCall.limit))
                }

                is ParsedToolCall.InstallRuntimeTool -> {
                    val config = runCatching {
                        kotlinx.serialization.json.Json.parseToJsonElement(parsedCall.configJson).jsonObject
                            .mapValues { it.value.jsonPrimitive.content }
                    }.getOrElse { emptyMap() }
                    if (parsedCall.configJson.trim() != "{}" && config.isEmpty()) {
                        ToolExecutionResult.Failure("config_json must be a JSON object of string template values.")
                    } else {
                        RuntimeToolCatalog.install(
                            com.waheed.artificerx.core.runtime.RuntimeToolSpec(
                                name = parsedCall.name,
                                description = parsedCall.description,
                                operation = parsedCall.operation,
                                inputSchemaJson = parsedCall.inputSchemaJson,
                                config = config,
                            ),
                        ).fold(
                            onSuccess = { ToolExecutionResult.Success("Installed runtime tool '${it.name}'. It is available to the next agent iteration.") },
                            onFailure = { ToolExecutionResult.Failure(it.message ?: "Runtime tool installation failed") },
                        )
                    }
                }

                is ParsedToolCall.Dynamic -> {
                    val args = runCatching {
                        kotlinx.serialization.json.Json.parseToJsonElement(parsedCall.argsJson).jsonObject
                            .mapValues { it.value.jsonPrimitive.content }
                    }.getOrElse {
                        return ToolExecutionResult.Failure("Invalid runtime tool arguments: ${it.message ?: "invalid JSON"}")
                    }
                    runtimeToolExecutor.execute(parsedCall.name, args)
                }

                is ParsedToolCall.InspectCanvas -> {
                    val snapshot = viewModel.captureSnapshotNow()
                    val nativeStats = runCatching { nativeRasterCore.analyze(snapshot) }
                        .getOrElse { "native_analysis_error=${it.message ?: "unknown"}" }
                    ToolExecutionResult.Success(
                        "Snapshot requested\nNATIVE_RASTER=${nativeStats}",
                        requiresSnapshot = true,
                    )
                }

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
                            "radial_12" -> com.waheed.artificerx.domain.model.SymmetryMode.RADIAL_12
                            "radial_16" -> com.waheed.artificerx.domain.model.SymmetryMode.RADIAL_16
                            "kaleidoscope_6" -> com.waheed.artificerx.domain.model.SymmetryMode.KALEIDOSCOPE_6
                            "kaleidoscope_12" -> com.waheed.artificerx.domain.model.SymmetryMode.KALEIDOSCOPE_12
                            "mandala_24" -> com.waheed.artificerx.domain.model.SymmetryMode.MANDALA_24
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

                is ParsedToolCall.InspectAndroidToolchain -> {
                    val snapshot=androidToolchainManager.inspect()
                    ToolExecutionResult.Success(
                        "Android=${snapshot.androidRelease}; SDK=${snapshot.sdkRoot ?: "unknown"}; " +
                            "platforms=${snapshot.platforms}; buildTools=${snapshot.buildTools}; ndks=${snapshot.ndks}; cmake=${snapshot.cmake}; " +
                            "java=${snapshot.javaVersion}; git=${snapshot.gitAvailable}; adb=${snapshot.adbAvailable}"
                    )
                }

                is ParsedToolCall.ComposeScene -> {
                    runCatching { sceneCompositionEngine.compose(parsedCall.request, viewModel) }
                        .fold(
                            onSuccess = { ToolExecutionResult.Success(it, requiresSnapshot = true) },
                            onFailure = { ToolExecutionResult.Failure("Scene composition failed: ${it.message ?: "unknown"}") },
                        )
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

                is ParsedToolCall.WebSearch ->
                    ToolExecutionResult.Failure(
                        "Internal error: web_search reached ToolExecutor directly instead of being handled by AgentOrchestrator.",
                    )

                // v0.4.30: full project/canvas customization access.
                is ParsedToolCall.ResizeCanvas -> {
                    if (parsedCall.widthPx <= 0 || parsedCall.heightPx <= 0) {
                        ToolExecutionResult.Failure("resize_canvas requires positive width_px and height_px.")
                    } else if (parsedCall.widthPx > 8000 || parsedCall.heightPx > 8000) {
                        ToolExecutionResult.Failure(
                            "resize_canvas rejected ${parsedCall.widthPx}x${parsedCall.heightPx} — 8000px per side is the " +
                                "safe ceiling for this device's memory; pick a smaller size.",
                        )
                    } else {
                        viewModel.resizeCanvas(parsedCall.widthPx, parsedCall.heightPx)
                        ToolExecutionResult.Success(
                            "Canvas resized to ${parsedCall.widthPx}x${parsedCall.heightPx}px.",
                            requiresSnapshot = true,
                        )
                    }
                }

                is ParsedToolCall.SetCanvasBackground -> {
                    viewModel.setCanvasBackground(parsedCall.colorHex)
                    ToolExecutionResult.Success("Canvas background set to ${parsedCall.colorHex}.", requiresSnapshot = true)
                }

                is ParsedToolCall.SetBrushDefaults -> {
                    viewModel.setBrushDefaults(
                        brushType = parsedCall.brushType,
                        sizePx = parsedCall.sizePx,
                        colorHex = parsedCall.colorHex,
                        opacity = parsedCall.opacity,
                        hardness = parsedCall.hardness,
                    )
                    ToolExecutionResult.Success("Brush defaults updated.", requiresSnapshot = false)
                }

                is ParsedToolCall.SetSelection -> {
                    viewModel.setSelection(
                        com.waheed.artificerx.domain.model.SelectionRect(
                            parsedCall.left,
                            parsedCall.top,
                            parsedCall.right,
                            parsedCall.bottom,
                        ),
                    )
                    ToolExecutionResult.Success(
                        "Selection set to (${parsedCall.left},${parsedCall.top})-(${parsedCall.right},${parsedCall.bottom}).",
                        requiresSnapshot = false,
                    )
                }

                is ParsedToolCall.ClearSelection -> {
                    viewModel.setSelection(null)
                    ToolExecutionResult.Success("Selection cleared.", requiresSnapshot = false)
                }

                is ParsedToolCall.DeleteSelectionContent -> {
                    if (currentState.selection == null) {
                        ToolExecutionResult.Failure("delete_selection_content requires set_selection to be called first.")
                    } else {
                        viewModel.clearSelectionContent()
                        ToolExecutionResult.Success("Deleted pixel content inside the selection.", requiresSnapshot = true)
                    }
                }

                is ParsedToolCall.TransformLayer -> {
                    if (currentState.activeLayerId == null) {
                        ToolExecutionResult.Failure("No active layer to transform. Call create_layer or set_active_layer first.")
                    } else {
                        viewModel.beginTransformGesture()
                        viewModel.transformActiveLayer(
                            dx = parsedCall.dx,
                            dy = parsedCall.dy,
                            scaleFactor = parsedCall.scaleFactor,
                            rotationDegrees = parsedCall.rotationDegrees,
                            pivotX = parsedCall.pivotX ?: (currentState.canvasWidthPx / 2f),
                            pivotY = parsedCall.pivotY ?: (currentState.canvasHeightPx / 2f),
                        )
                        ToolExecutionResult.Success(
                            "Transformed active layer (dx=${parsedCall.dx}, dy=${parsedCall.dy}, " +
                                "scale=${parsedCall.scaleFactor}, rotation=${parsedCall.rotationDegrees}°).",
                            requiresSnapshot = true,
                        )
                    }
                }

                is ParsedToolCall.Unknown ->
                    ToolExecutionResult.Failure(
                        "Unknown tool '${parsedCall.toolName}'. Available tools: ${ToolRegistry.ALL_TOOLS.map { it.function.name }}",
                    )

                is ParsedToolCall.Invalid ->
                    ToolExecutionResult.Failure(parsedCall.reasons.joinToString(" "))
            }

            // CRITICAL FIX (v0.4.30): every pixel-mutating branch above writes
            // straight into bitmapStore/compositor and never told
            // StudioViewModel's StateFlow to refresh — the AI's drawing
            // succeeded on the backing bitmap but Compose never recomposed,
            // so nothing ever appeared on screen. This is the one call that
            // makes AI-generated output actually visible.
            if (mutatesPixels(parsedCall) && result is ToolExecutionResult.Success) {
                viewModel.recomposite()
            }

            return result
        }

        private companion object {
            const val DEFAULT_CHECKSUM_BUFFER = 16 * 1024
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
                // v0.4.30: denser radial fans, requested for mandala/pattern work.
                com.waheed.artificerx.domain.model.SymmetryMode.RADIAL_12 -> (0 until 12).map { rotateAround(points, it * 30.0) }
                com.waheed.artificerx.domain.model.SymmetryMode.RADIAL_16 -> (0 until 16).map { rotateAround(points, it * 22.5) }
                // v0.4.30: true kaleidoscope = rotation *and* a mirror at each
                // step, so each wedge is a reflected copy of its neighbor
                // (what makes a kaleidoscope look like one, vs. a plain
                // radial fan which just repeats the same orientation).
                com.waheed.artificerx.domain.model.SymmetryMode.KALEIDOSCOPE_6 ->
                    (0 until 6).flatMap { step ->
                        val rotated = rotateAround(points, step * 60.0)
                        listOf(rotated, mirrorVertical(rotated))
                    }
                com.waheed.artificerx.domain.model.SymmetryMode.KALEIDOSCOPE_12 ->
                    (0 until 12).flatMap { step ->
                        val rotated = rotateAround(points, step * 30.0)
                        listOf(rotated, mirrorVertical(rotated))
                    }
                // v0.4.30: MANDALA_24 = 24-way radial fan + mirror per step
                // (48 total copies) for the dense, flower-like repetition
                // mandala work actually wants, denser than a plain radial.
                com.waheed.artificerx.domain.model.SymmetryMode.MANDALA_24 ->
                    (0 until 24).flatMap { step ->
                        val rotated = rotateAround(points, step * 15.0)
                        listOf(rotated, mirrorVertical(rotated))
                    }
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
