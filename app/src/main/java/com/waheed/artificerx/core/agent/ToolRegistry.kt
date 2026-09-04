package com.waheed.artificerx.core.agent

import com.waheed.artificerx.core.network.FunctionDefinitionDto
import com.waheed.artificerx.core.runtime.RuntimeToolCatalog
import com.waheed.artificerx.core.builtin.BuiltinRecipeTools
import com.waheed.artificerx.core.network.ToolDefinitionDto
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Section 179/180's core architectural bet: no diffusion model. The
 * Reasoning Brain draws by emitting tool_calls against this exact
 * registry — draw_path, draw_shape, create_layer, apply_gradient,
 * fill_region, set_layer_property — and watches the resulting canvas
 * snapshot (vision feedback) to decide its next call. Every tool here
 * is a JSON-Schema function definition the LLM sees in its request;
 * ToolExecutor (separate file) is what actually performs the operation
 * against StudioViewModel's real canvas state once a tool_call comes
 * back.
 */
object ToolRegistry {
    private val BUILTIN_TOOLS: List<ToolDefinitionDto> =
        listOf(
            createLayerTool(),
            deleteLayerTool(),
            setActiveLayerTool(),
            drawPathTool(),
            resizeCanvasTool(),
            setCanvasBackgroundTool(),
            setBrushDefaultsTool(),
            setSelectionTool(),
            clearSelectionTool(),
            deleteSelectionContentTool(),
            transformLayerTool(),
            drawShapeTool(),
            applyGradientTool(),
            fillRegionTool(),
            setLayerPropertyTool(),
            duplicateLayerTool(),
            flipLayerTool(),
            cropCanvasTool(),
            inspectCanvasTool(),
            inspectAndroidToolchain(),
            pickColorTool(),
            applyFilterTool(),
            addTextTool(),
            createMaskTool(),
            enableSymmetryTool(),
            applyPatternTool(),
            drawCurveTool(),
            importImageLayerTool(),
            composeSceneTool(),
            webFetchTool(),
            webSearchTool(),
            createPrimitiveTool(),
            sculptStrokeTool(),
            deleteMeshTool(),
            setMeshColorTool(),
            transformMeshTool(),
            inspectSceneTool(),
            finishTurnTool(),
            rememberTool(),
            recallTool(),
            generateImageTool(),
            readWorkspaceFileTool(),
            writeWorkspaceFileTool(),
            listWorkspaceDirectoryTool(),
            replaceWorkspaceTextTool(),
            createFileTool(),
            createZipTool(),
            runTerminalTool(),
            runTerminalBatchTool(),
            listArtifactsTool(),
            searchWorkspaceTool(),
            artifactInfoTool(),
            checksumArtifactTool(),
            workspaceStatusTool(),
            exportWorkspaceBundleTool(),
            installRuntimeTool(),
        )

    val ALL_TOOLS: List<ToolDefinitionDto>
        get() = BUILTIN_TOOLS + BuiltinRecipeTools.definitions() + RuntimeToolCatalog.definitions()
    // NOTE (reliability audit): a Previous synthetic numbered tool schemas were removed. Runtime extensions now use persisted, audited `runtime_*` definitions whose operations map to real executors.

    private fun installRuntimeTool() =
        ToolDefinitionDto(
            function =
                FunctionDefinitionDto(
                    name = RuntimeToolCatalog.INSTALL_TOOL,
                    description = "Install or replace a persistent runtime tool without rebuilding the APK. Only audited declarative operations are allowed.",
                    parameters = buildJsonObject {
                        put("type", "object")
                        putJsonObject("properties") {
                            putJsonObject("name") { put("type", "string"); put("description", "Name beginning with runtime_") }
                            putJsonObject("description") { put("type", "string") }
                            putJsonObject("operation") {
                                put("type", "string")
                                putJsonArray("enum") { RuntimeToolCatalog.SUPPORTED_OPERATIONS.forEach { add(JsonPrimitive(it)) } }
                            }
                            putJsonObject("input_schema_json") { put("type", "string") }
                            putJsonObject("config_json") { put("type", "string") }
                        }
                        putJsonArray("required") {
                            add(JsonPrimitive("name")); add(JsonPrimitive("description")); add(JsonPrimitive("operation"));
                            add(JsonPrimitive("input_schema_json")); add(JsonPrimitive("config_json"))
                        }
                    },
                ),
        )

    private fun createLayerTool() =
        ToolDefinitionDto(
            function =
                FunctionDefinitionDto(
                    name = "create_layer",
                    description = "Creates a new empty layer above the current active layer and makes it active.",
                    parameters =
                        buildJsonObject {
                            put("type", "object")
                            putJsonObject("properties") {
                                putJsonObject("name") {
                                    put("type", "string")
                                    put("description", "Human-readable layer name, e.g. 'Character Outline'")
                                }
                            }
                            putJsonArray("required") { add(JsonPrimitive("name")) }
                        },
                ),
        )

    private fun deleteLayerTool() =
        ToolDefinitionDto(
            function =
                FunctionDefinitionDto(
                    name = "delete_layer",
                    description = "Deletes a layer by its ID. Cannot delete the last remaining layer.",
                    parameters =
                        buildJsonObject {
                            put("type", "object")
                            putJsonObject("properties") {
                                putJsonObject("layer_id") { put("type", "string") }
                            }
                            putJsonArray("required") { add(JsonPrimitive("layer_id")) }
                        },
                ),
        )

    private fun setActiveLayerTool() =
        ToolDefinitionDto(
            function =
                FunctionDefinitionDto(
                    name = "set_active_layer",
                    description = "Switches which layer subsequent draw operations apply to.",
                    parameters =
                        buildJsonObject {
                            put("type", "object")
                            putJsonObject("properties") {
                                putJsonObject("layer_id") { put("type", "string") }
                            }
                            putJsonArray("required") { add(JsonPrimitive("layer_id")) }
                        },
                ),
        )

    private fun drawPathTool() =
        ToolDefinitionDto(
            function =
                FunctionDefinitionDto(
                    name = "draw_path",
                    description =
                        "Draws a freehand stroke on the active layer as a sequence of " +
                            "points, using the current brush settings unless overridden.",
                    parameters =
                        buildJsonObject {
                            put("type", "object")
                            putJsonObject("properties") {
                                putJsonObject("points") {
                                    put("type", "array")
                                    put("description", "Flat array of [x1,y1,x2,y2,...] pixel coordinates")
                                    putJsonObject("items") { put("type", "number") }
                                }
                                putJsonObject("color_hex") { put("type", "string") }
                                putJsonObject("stroke_width_px") { put("type", "number") }
                                putJsonObject("opacity") { put("type", "number") }
                                putJsonObject("brush_type") {
                                    put("type", "string")
                                    put("description", "Which real brush engine to render with — each renders genuinely differently.")
                                    putJsonArray("enum") {
                                        add(JsonPrimitive("pencil"))
                                        add(JsonPrimitive("ink_pen"))
                                        add(JsonPrimitive("marker"))
                                        add(JsonPrimitive("calligraphy"))
                                        add(JsonPrimitive("airbrush"))
                                        add(JsonPrimitive("charcoal"))
                                        add(JsonPrimitive("watercolor"))
                                    }
                                }
                            }
                            putJsonArray("required") { add(JsonPrimitive("points")) }
                        },
                ),
        )

    private fun resizeCanvasTool() =
        ToolDefinitionDto(
            function =
                FunctionDefinitionDto(
                    name = "resize_canvas",
                    description =
                        "Resizes the active project's canvas to an exact pixel width/height — call this FIRST " +
                            "when a request implies a specific format (e.g. '2000x3000 poster', 'square Instagram " +
                            "post', '16:9 wallpaper') before creating any layers, since existing layer content is " +
                            "cropped/padded to the new bounds, not rescaled.",
                    parameters =
                        buildJsonObject {
                            put("type", "object")
                            putJsonObject("properties") {
                                putJsonObject("width_px") { put("type", "integer") }
                                putJsonObject("height_px") { put("type", "integer") }
                            }
                            putJsonArray("required") { add(JsonPrimitive("width_px")); add(JsonPrimitive("height_px")) }
                        },
                ),
        )

    private fun setCanvasBackgroundTool() =
        ToolDefinitionDto(
            function =
                FunctionDefinitionDto(
                    name = "set_canvas_background",
                    description = "Sets the project's base background color (the bottom-most layer's fill).",
                    parameters =
                        buildJsonObject {
                            put("type", "object")
                            putJsonObject("properties") {
                                putJsonObject("color_hex") { put("type", "string") }
                            }
                            putJsonArray("required") { add(JsonPrimitive("color_hex")) }
                        },
                ),
        )

    private fun setBrushDefaultsTool() =
        ToolDefinitionDto(
            function =
                FunctionDefinitionDto(
                    name = "set_brush_defaults",
                    description =
                        "Sets the standing brush configuration (type/size/color/opacity/hardness) used by any " +
                            "following draw_path call that doesn't explicitly override that field — convenient " +
                            "when drawing many strokes with the same brush instead of repeating every parameter " +
                            "on every draw_path call.",
                    parameters =
                        buildJsonObject {
                            put("type", "object")
                            putJsonObject("properties") {
                                putJsonObject("brush_type") {
                                    put("type", "string")
                                    putJsonArray("enum") {
                                        add(JsonPrimitive("pencil"))
                                        add(JsonPrimitive("ink_pen"))
                                        add(JsonPrimitive("marker"))
                                        add(JsonPrimitive("calligraphy"))
                                        add(JsonPrimitive("airbrush"))
                                        add(JsonPrimitive("charcoal"))
                                        add(JsonPrimitive("watercolor"))
                                    }
                                }
                                putJsonObject("size_px") { put("type", "number") }
                                putJsonObject("color_hex") { put("type", "string") }
                                putJsonObject("opacity") { put("type", "number") }
                                putJsonObject("hardness") { put("type", "number") }
                            }
                        },
                ),
        )

    private fun setSelectionTool() =
        ToolDefinitionDto(
            function =
                FunctionDefinitionDto(
                    name = "set_selection",
                    description =
                        "Sets a rectangular selection on the canvas in pixel coordinates. Any following " +
                            "delete_selection_content call operates only inside this rect.",
                    parameters =
                        buildJsonObject {
                            put("type", "object")
                            putJsonObject("properties") {
                                putJsonObject("left") { put("type", "number") }
                                putJsonObject("top") { put("type", "number") }
                                putJsonObject("right") { put("type", "number") }
                                putJsonObject("bottom") { put("type", "number") }
                            }
                            putJsonArray("required") {
                                add(JsonPrimitive("left")); add(JsonPrimitive("top"))
                                add(JsonPrimitive("right")); add(JsonPrimitive("bottom"))
                            }
                        },
                ),
        )

    private fun clearSelectionTool() =
        ToolDefinitionDto(
            function =
                FunctionDefinitionDto(
                    name = "clear_selection",
                    description = "Deselects — drops the active selection rectangle without touching any pixels.",
                    parameters = buildJsonObject { put("type", "object"); putJsonObject("properties") {} },
                ),
        )

    private fun deleteSelectionContentTool() =
        ToolDefinitionDto(
            function =
                FunctionDefinitionDto(
                    name = "delete_selection_content",
                    description =
                        "Permanently clears (to transparency) the pixels on the active layer inside the current " +
                            "selection rectangle. Requires set_selection to have been called first.",
                    parameters = buildJsonObject { put("type", "object"); putJsonObject("properties") {} },
                ),
        )

    private fun transformLayerTool() =
        ToolDefinitionDto(
            function =
                FunctionDefinitionDto(
                    name = "transform_layer",
                    description =
                        "Moves/scales/rotates the ENTIRE active layer's pixel content in place — use this to " +
                            "reposition, resize, or rotate something you already drew, instead of erasing and " +
                            "redrawing it from scratch.",
                    parameters =
                        buildJsonObject {
                            put("type", "object")
                            putJsonObject("properties") {
                                putJsonObject("dx") { put("type", "number"); put("description", "Pixels to move horizontally.") }
                                putJsonObject("dy") { put("type", "number"); put("description", "Pixels to move vertically.") }
                                putJsonObject("scale_factor") {
                                    put("type", "number")
                                    put("description", "1.0 = no change, 2.0 = double size, 0.5 = half size.")
                                }
                                putJsonObject("rotation_degrees") { put("type", "number") }
                                putJsonObject("pivot_x") {
                                    put("type", "number")
                                    put("description", "Pivot point for scale/rotate; defaults to canvas center if omitted.")
                                }
                                putJsonObject("pivot_y") { put("type", "number") }
                            }
                        },
                ),
        )

    private fun drawShapeTool() =
        ToolDefinitionDto(
            function =
                FunctionDefinitionDto(
                    name = "draw_shape",
                    description =
                        "Draws a primitive shape (rectangle, ellipse, line, polygon, " +
                            "star) at a specific position and size, with optional rotation.",
                    parameters =
                        buildJsonObject {
                            put("type", "object")
                            putJsonObject("properties") {
                                putJsonObject("shape_type") {
                                    put("type", "string")
                                    putJsonArray("enum") {
                                        add(JsonPrimitive("rectangle"))
                                        add(JsonPrimitive("ellipse"))
                                        add(JsonPrimitive("line"))
                                        add(JsonPrimitive("polygon"))
                                        add(JsonPrimitive("star"))
                                    }
                                }
                                putJsonObject("x") { put("type", "number") }
                                putJsonObject("y") { put("type", "number") }
                                putJsonObject("width") { put("type", "number") }
                                putJsonObject("height") { put("type", "number") }
                                putJsonObject("fill_color_hex") { put("type", "string") }
                                putJsonObject("stroke_color_hex") { put("type", "string") }
                                putJsonObject("stroke_width_px") { put("type", "number") }
                                putJsonObject("rotation_degrees") {
                                    put("type", "number")
                                    put("description", "Rotates the shape around its own center. 0 = no rotation.")
                                }
                                putJsonObject("sides") {
                                    put("type", "number")
                                    put(
                                        "description",
                                        "Number of sides for polygon (3-20, default 6) or points for star (3-20, default 5). Ignored for other shape types.",
                                    )
                                }
                            }
                            putJsonArray("required") {
                                add(JsonPrimitive("shape_type"))
                                add(JsonPrimitive("x"))
                                add(JsonPrimitive("y"))
                            }
                        },
                ),
        )

    private fun applyGradientTool() =
        ToolDefinitionDto(
            function =
                FunctionDefinitionDto(
                    name = "apply_gradient",
                    description = "Fills a rectangular region with a linear or radial gradient, optionally with more than two color stops.",
                    parameters =
                        buildJsonObject {
                            put("type", "object")
                            putJsonObject("properties") {
                                putJsonObject("gradient_type") {
                                    put("type", "string")
                                    putJsonArray("enum") {
                                        add(JsonPrimitive("linear"))
                                        add(JsonPrimitive("radial"))
                                    }
                                }
                                putJsonObject("start_color_hex") { put("type", "string") }
                                putJsonObject("end_color_hex") { put("type", "string") }
                                putJsonObject("x") { put("type", "number") }
                                putJsonObject("y") { put("type", "number") }
                                putJsonObject("width") { put("type", "number") }
                                putJsonObject("height") { put("type", "number") }
                                putJsonObject("angle_degrees") {
                                    put("type", "number")
                                    put(
                                        "description",
                                        "Linear gradient direction: 0=left-to-right, 90=top-to-bottom, 180=right-to-left, 270=bottom-to-top. Ignored for radial. Default 90.",
                                    )
                                }
                                putJsonObject("additional_color_stops_hex") {
                                    put("type", "array")
                                    put(
                                        "description",
                                        "Optional extra color stops placed evenly between start_color_hex and end_color_hex, for richer multi-color gradients (e.g. a sunset sky).",
                                    )
                                    putJsonObject("items") { put("type", "string") }
                                }
                            }
                            putJsonArray("required") {
                                add(JsonPrimitive("gradient_type"))
                                add(JsonPrimitive("start_color_hex"))
                                add(JsonPrimitive("end_color_hex"))
                            }
                        },
                ),
        )

    private fun fillRegionTool() =
        ToolDefinitionDto(
            function =
                FunctionDefinitionDto(
                    name = "fill_region",
                    description = "Flood-fills a contiguous region starting at (x,y) with a solid color, like a paint bucket tool.",
                    parameters =
                        buildJsonObject {
                            put("type", "object")
                            putJsonObject("properties") {
                                putJsonObject("x") { put("type", "number") }
                                putJsonObject("y") { put("type", "number") }
                                putJsonObject("color_hex") { put("type", "string") }
                                putJsonObject("tolerance") {
                                    put("type", "number")
                                    put("description", "Color-match tolerance 0-255")
                                }
                            }
                            putJsonArray("required") {
                                add(JsonPrimitive("x"))
                                add(JsonPrimitive("y"))
                                add(JsonPrimitive("color_hex"))
                            }
                        },
                ),
        )

    private fun setLayerPropertyTool() =
        ToolDefinitionDto(
            function =
                FunctionDefinitionDto(
                    name = "set_layer_property",
                    description = "Adjusts opacity, blend mode, or visibility of an existing layer.",
                    parameters =
                        buildJsonObject {
                            put("type", "object")
                            putJsonObject("properties") {
                                putJsonObject("layer_id") { put("type", "string") }
                                putJsonObject("opacity") { put("type", "number") }
                                putJsonObject("blend_mode") { put("type", "string") }
                                putJsonObject("is_visible") { put("type", "boolean") }
                            }
                            putJsonArray("required") { add(JsonPrimitive("layer_id")) }
                        },
                ),
        )

    private fun duplicateLayerTool() =
        ToolDefinitionDto(
            function =
                FunctionDefinitionDto(
                    name = "duplicate_layer",
                    description =
                        "Creates an exact pixel-for-pixel copy of an existing layer, inserted directly above the source and made active. " +
                            "Useful for non-destructive experimentation (e.g. duplicate before a risky filter) or for repeating an element.",
                    parameters =
                        buildJsonObject {
                            put("type", "object")
                            putJsonObject("properties") {
                                putJsonObject("layer_id") {
                                    put("type", "string")
                                    put("description", "The layer to duplicate.")
                                }
                                putJsonObject("new_name") {
                                    put("type", "string")
                                    put("description", "Optional name for the new layer. Defaults to '<source name> copy'.")
                                }
                            }
                            putJsonArray("required") { add(JsonPrimitive("layer_id")) }
                        },
                ),
        )

    private fun flipLayerTool() =
        ToolDefinitionDto(
            function =
                FunctionDefinitionDto(
                    name = "flip_layer",
                    description =
                        "Mirrors a layer's pixel content horizontally and/or vertically in place. Useful for " +
                            "symmetric elements, correcting orientation, or quickly creating a mirrored variant.",
                    parameters =
                        buildJsonObject {
                            put("type", "object")
                            putJsonObject("properties") {
                                putJsonObject("layer_id") { put("type", "string") }
                                putJsonObject("horizontal") {
                                    put("type", "boolean")
                                    put("description", "Mirror left-to-right.")
                                }
                                putJsonObject("vertical") {
                                    put("type", "boolean")
                                    put("description", "Mirror top-to-bottom.")
                                }
                            }
                            putJsonArray("required") { add(JsonPrimitive("layer_id")) }
                        },
                ),
        )

    private fun cropCanvasTool() =
        ToolDefinitionDto(
            function =
                FunctionDefinitionDto(
                    name = "crop_canvas",
                    description =
                        "Crops the entire canvas (every layer) to a new rectangular region, shrinking the canvas dimensions to " +
                            "match. This cannot be undone by any other tool — use inspect_canvas first if unsure about the exact region.",
                    parameters =
                        buildJsonObject {
                            put("type", "object")
                            putJsonObject("properties") {
                                putJsonObject("x") {
                                    put("type", "number")
                                    put("description", "Left edge of the crop region in pixels.")
                                }
                                putJsonObject("y") {
                                    put("type", "number")
                                    put("description", "Top edge of the crop region in pixels.")
                                }
                                putJsonObject("width") { put("type", "number") }
                                putJsonObject("height") { put("type", "number") }
                            }
                            putJsonArray("required") {
                                add(JsonPrimitive("x"))
                                add(JsonPrimitive("y"))
                                add(JsonPrimitive("width"))
                                add(JsonPrimitive("height"))
                            }
                        },
                ),
        )

    private fun inspectCanvasTool() =
        ToolDefinitionDto(
            function =
                FunctionDefinitionDto(
                    name = "inspect_canvas",
                    description =
                        "Requests a fresh snapshot of the current canvas render so you can visually verify your " +
                            "last operation before continuing. Use this after any draw operation you're unsure about.",
                    parameters =
                        buildJsonObject {
                            put("type", "object")
                            putJsonObject("properties") {}
                        },
                ),
        )

    private fun pickColorTool() =
        ToolDefinitionDto(
            function =
                FunctionDefinitionDto(
                    name = "pick_color",
                    description =
                        "Samples the exact pixel color at (x,y) on the current composited canvas and returns " +
                            "it as a hex string, so you can match or react to existing colors instead of guessing.",
                    parameters =
                        buildJsonObject {
                            put("type", "object")
                            putJsonObject("properties") {
                                putJsonObject("x") { put("type", "number") }
                                putJsonObject("y") { put("type", "number") }
                            }
                            putJsonArray("required") {
                                add(JsonPrimitive("x"))
                                add(JsonPrimitive("y"))
                            }
                        },
                ),
        )

    private fun applyFilterTool() =
        ToolDefinitionDto(
            function =
                FunctionDefinitionDto(
                    name = "apply_filter",
                    description =
                        "Applies a full-layer image filter: blur, sharpen, " +
                            "brightness, contrast, saturation, grayscale, or invert.",
                    parameters =
                        buildJsonObject {
                            put("type", "object")
                            putJsonObject("properties") {
                                putJsonObject("layer_id") { put("type", "string") }
                                putJsonObject("filter_type") {
                                    put("type", "string")
                                    putJsonArray("enum") {
                                        add(JsonPrimitive("blur"))
                                        add(JsonPrimitive("sharpen"))
                                        add(JsonPrimitive("brightness"))
                                        add(JsonPrimitive("contrast"))
                                        add(JsonPrimitive("saturation"))
                                        add(JsonPrimitive("grayscale"))
                                        add(JsonPrimitive("invert"))
                                    }
                                }
                                putJsonObject("intensity") {
                                    put("type", "number")
                                    put(
                                        "description",
                                        "0.0 to 2.0, where 1.0 is neutral for brightness/contrast/saturation. Ignored for grayscale/invert.",
                                    )
                                }
                            }
                            putJsonArray("required") {
                                add(JsonPrimitive("layer_id"))
                                add(JsonPrimitive("filter_type"))
                            }
                        },
                ),
        )

    private fun addTextTool() =
        ToolDefinitionDto(
            function =
                FunctionDefinitionDto(
                    name = "add_text",
                    description = "Renders text onto the active layer at a given position with a chosen size and color.",
                    parameters =
                        buildJsonObject {
                            put("type", "object")
                            putJsonObject("properties") {
                                putJsonObject("text") { put("type", "string") }
                                putJsonObject("x") { put("type", "number") }
                                putJsonObject("y") { put("type", "number") }
                                putJsonObject("font_size_px") { put("type", "number") }
                                putJsonObject("color_hex") { put("type", "string") }
                                putJsonObject("bold") { put("type", "boolean") }
                            }
                            putJsonArray("required") {
                                add(JsonPrimitive("text"))
                                add(JsonPrimitive("x"))
                                add(JsonPrimitive("y"))
                            }
                        },
                ),
        )

    private fun createMaskTool() =
        ToolDefinitionDto(
            function =
                FunctionDefinitionDto(
                    name = "create_mask",
                    description =
                        "Applies a rectangular or elliptical alpha mask to a layer, hiding everything " +
                            "outside the given region without deleting the pixel data underneath.",
                    parameters =
                        buildJsonObject {
                            put("type", "object")
                            putJsonObject("properties") {
                                putJsonObject("layer_id") { put("type", "string") }
                                putJsonObject("mask_shape") {
                                    put("type", "string")
                                    putJsonArray("enum") {
                                        add(JsonPrimitive("rectangle"))
                                        add(JsonPrimitive("ellipse"))
                                    }
                                }
                                putJsonObject("x") { put("type", "number") }
                                putJsonObject("y") { put("type", "number") }
                                putJsonObject("width") { put("type", "number") }
                                putJsonObject("height") { put("type", "number") }
                                putJsonObject("invert") {
                                    put("type", "boolean")
                                    put("description", "If true, hides inside the region instead of outside it.")
                                }
                            }
                            putJsonArray("required") {
                                add(JsonPrimitive("layer_id"))
                                add(JsonPrimitive("mask_shape"))
                                add(JsonPrimitive("x"))
                                add(JsonPrimitive("y"))
                                add(JsonPrimitive("width"))
                                add(JsonPrimitive("height"))
                            }
                        },
                ),
        )

    private fun enableSymmetryTool() =
        ToolDefinitionDto(
            function =
                FunctionDefinitionDto(
                    name = "enable_symmetry",
                    description =
                        "Turns on symmetry mode for subsequent draw_path calls on the active layer: every " +
                            "stroke is automatically mirrored across a vertical, horizontal, radial, or " +
                            "kaleidoscope axis, useful for mandalas, faces, symmetric character designs, " +
                            "and dense ornamental patterns.",
                    parameters =
                        buildJsonObject {
                            put("type", "object")
                            putJsonObject("properties") {
                                putJsonObject("mode") {
                                    put("type", "string")
                                    putJsonArray("enum") {
                                        add(JsonPrimitive("off"))
                                        add(JsonPrimitive("vertical"))
                                        add(JsonPrimitive("horizontal"))
                                        add(JsonPrimitive("radial_4"))
                                        add(JsonPrimitive("radial_8"))
                                        add(JsonPrimitive("radial_12"))
                                        add(JsonPrimitive("radial_16"))
                                        add(JsonPrimitive("kaleidoscope_6"))
                                        add(JsonPrimitive("kaleidoscope_12"))
                                        add(JsonPrimitive("mandala_24"))
                                    }
                                }
                            }
                            putJsonArray("required") { add(JsonPrimitive("mode")) }
                        },
                ),
        )

    private fun applyPatternTool() =
        ToolDefinitionDto(
            function =
                FunctionDefinitionDto(
                    name = "apply_pattern",
                    description =
                        "Fills a rectangular region with a repeating procedural pattern: dots, stripes, checkerboard, or " +
                            "crosshatch — useful for textures, backgrounds, and clothing fabric without hand-drawing every repetition.",
                    parameters =
                        buildJsonObject {
                            put("type", "object")
                            putJsonObject("properties") {
                                putJsonObject("pattern_type") {
                                    put("type", "string")
                                    putJsonArray("enum") {
                                        add(JsonPrimitive("dots"))
                                        add(JsonPrimitive("stripes"))
                                        add(JsonPrimitive("checkerboard"))
                                        add(JsonPrimitive("crosshatch"))
                                    }
                                }
                                putJsonObject("x") { put("type", "number") }
                                putJsonObject("y") { put("type", "number") }
                                putJsonObject("width") { put("type", "number") }
                                putJsonObject("height") { put("type", "number") }
                                putJsonObject("color_hex") { put("type", "string") }
                                putJsonObject("scale_px") {
                                    put("type", "number")
                                    put("description", "Size of one pattern repeat unit in pixels")
                                }
                            }
                            putJsonArray("required") {
                                add(JsonPrimitive("pattern_type"))
                                add(JsonPrimitive("x"))
                                add(JsonPrimitive("y"))
                                add(JsonPrimitive("width"))
                                add(JsonPrimitive("height"))
                                add(JsonPrimitive("color_hex"))
                            }
                        },
                ),
        )

    private fun drawCurveTool() =
        ToolDefinitionDto(
            function =
                FunctionDefinitionDto(
                    name = "draw_curve",
                    description =
                        "Draws a smooth quadratic Bezier curve from a start point through a control point to an end point " +
                            "— better for organic lines (limbs, hair, clothing folds) than the straight segments of draw_path.",
                    parameters =
                        buildJsonObject {
                            put("type", "object")
                            putJsonObject("properties") {
                                putJsonObject("start_x") { put("type", "number") }
                                putJsonObject("start_y") { put("type", "number") }
                                putJsonObject("control_x") { put("type", "number") }
                                putJsonObject("control_y") { put("type", "number") }
                                putJsonObject("end_x") { put("type", "number") }
                                putJsonObject("end_y") { put("type", "number") }
                                putJsonObject("color_hex") { put("type", "string") }
                                putJsonObject("stroke_width_px") { put("type", "number") }
                            }
                            putJsonArray("required") {
                                add(JsonPrimitive("start_x"))
                                add(JsonPrimitive("start_y"))
                                add(JsonPrimitive("control_x"))
                                add(JsonPrimitive("control_y"))
                                add(JsonPrimitive("end_x"))
                                add(JsonPrimitive("end_y"))
                            }
                        },
                ),
        )

    private fun importImageLayerTool() =
        ToolDefinitionDto(
            function =
                FunctionDefinitionDto(
                    name = "import_image_layer",
                    description =
                        "Imports a user-attached reference image as a new layer on the canvas, positioned and " +
                            "scaled to fit, so it can be traced over, color-matched, or blended with generated artwork.",
                    parameters =
                        buildJsonObject {
                            put("type", "object")
                            putJsonObject("properties") {
                                putJsonObject("layer_name") { put("type", "string") }
                                putJsonObject("opacity") { put("type", "number") }
                            }
                            putJsonArray("required") { add(JsonPrimitive("layer_name")) }
                        },
                ),
        )

    private fun composeSceneTool() =
        ToolDefinitionDto(
            function =
                FunctionDefinitionDto(
                    name = "compose_scene",
                    description = "Build a recognizable structured scene from a natural-language brief. Use for rooms, bedrooms, studios, classrooms, streets and other spatial compositions. It creates real multi-layer raster artwork with perspective, major objects, lighting and line art, then returns a composition report.",
                    parameters = buildJsonObject {
                        put("type", "object")
                        putJsonObject("properties") {
                            putJsonObject("request") { put("type", "string"); put("description", "Exact scene brief, including style, subject, camera, mood and important objects") }
                            putJsonObject("quality") { put("type", "integer"); put("minimum", 1); put("maximum", 5) }
                        }
                        putJsonArray("required") { add(JsonPrimitive("request")) }
                    },
                ),
        )

    private fun inspectAndroidToolchain() = ToolDefinitionDto(
        function = FunctionDefinitionDto(
            name = "inspect_android_toolchain",
            description = "Inspect the private Android build environment: SDK root, installed platforms, build-tools, NDKs, CMake, Java, Git and ADB availability. Use before coding/build tasks that depend on device capabilities.",
            parameters = buildJsonObject { put("type", "object"); putJsonObject("properties") {}; putJsonArray("required") {} },
        ),
    )

    private fun webFetchTool() =
        ToolDefinitionDto(
            function =
                FunctionDefinitionDto(
                    name = "web_fetch",
                    description =
                        "Fetches a web page by URL and returns its readable article text (ads, navigation, and " +
                            "boilerplate stripped out). Use this to look up current information, read documentation, " +
                            "or research a reference before drawing/sculpting something based on real-world facts.",
                    parameters =
                        buildJsonObject {
                            put("type", "object")
                            putJsonObject("properties") {
                                putJsonObject("url") {
                                    put("type", "string")
                                    put("description", "Full http:// or https:// URL to fetch.")
                                }
                            }
                            putJsonArray("required") { add(JsonPrimitive("url")) }
                        },
                ),
        )

    // v0.4.30: real research capability (DuckDuckGo HTML scrape, no
    // API key — see WebSearcher's doc for why). This is what Deep
    // Studio mode's mandatory "research before drawing" workflow
    // actually calls; without it that workflow would just be asking
    // the model to pretend.
    private fun webSearchTool() =
        ToolDefinitionDto(
            function =
                FunctionDefinitionDto(
                    name = "web_search",
                    description =
                        "Searches the web and returns a list of {title, url, snippet} results. Use this BEFORE " +
                            "drawing anything you're not fully certain how to construct correctly — anatomy, " +
                            "proportions, color palettes, art-style technique, real-world reference for an object " +
                            "or scene. Follow up with web_fetch on the most relevant result URL to read full detail.",
                    parameters =
                        buildJsonObject {
                            put("type", "object")
                            putJsonObject("properties") {
                                putJsonObject("query") {
                                    put("type", "string")
                                    put("description", "The search query, e.g. 'anime eye anatomy front view proportions'.")
                                }
                            }
                            putJsonArray("required") { add(JsonPrimitive("query")) }
                        },
                ),
        )

    private fun createPrimitiveTool() =
        ToolDefinitionDto(
            function =
                FunctionDefinitionDto(
                    name = "create_primitive",
                    description =
                        "Adds a new 3D primitive mesh (sphere, cube, cylinder, cone, plane, " +
                            "or torus) to the sculpt scene as a starting point for sculpting.",
                    parameters =
                        buildJsonObject {
                            put("type", "object")
                            putJsonObject("properties") {
                                putJsonObject("primitive_type") {
                                    put("type", "string")
                                    putJsonArray("enum") {
                                        add(JsonPrimitive("sphere"))
                                        add(JsonPrimitive("cube"))
                                        add(JsonPrimitive("cylinder"))
                                        add(JsonPrimitive("cone"))
                                        add(JsonPrimitive("plane"))
                                        add(JsonPrimitive("torus"))
                                    }
                                }
                                putJsonObject("name") { put("type", "string") }
                            }
                            putJsonArray("required") {
                                add(JsonPrimitive("primitive_type"))
                                add(JsonPrimitive("name"))
                            }
                        },
                ),
        )

    private fun sculptStrokeTool() =
        ToolDefinitionDto(
            function =
                FunctionDefinitionDto(
                    name = "sculpt_stroke",
                    description =
                        "Applies one sculpting brush stroke to a mesh at a 3D world-space point: push (pushes " +
                            "surface inward), pull (pulls surface outward), smooth (averages toward neighbors), " +
                            "pinch (draws surface toward the point), inflate (expands along normal), or flatten " +
                            "(levels surface to a local plane).",
                    parameters =
                        buildJsonObject {
                            put("type", "object")
                            putJsonObject("properties") {
                                putJsonObject("mesh_id") { put("type", "string") }
                                putJsonObject("brush_type") {
                                    put("type", "string")
                                    putJsonArray("enum") {
                                        add(JsonPrimitive("push"))
                                        add(JsonPrimitive("pull"))
                                        add(JsonPrimitive("smooth"))
                                        add(JsonPrimitive("pinch"))
                                        add(JsonPrimitive("inflate"))
                                        add(JsonPrimitive("flatten"))
                                    }
                                }
                                putJsonObject("hit_x") { put("type", "number") }
                                putJsonObject("hit_y") { put("type", "number") }
                                putJsonObject("hit_z") { put("type", "number") }
                                putJsonObject("radius") {
                                    put("type", "number")
                                    put("description", "Brush influence radius in world units, default 0.3")
                                }
                                putJsonObject("strength") {
                                    put("type", "number")
                                    put("description", "Displacement strength, default 0.5")
                                }
                            }
                            putJsonArray("required") {
                                add(JsonPrimitive("mesh_id"))
                                add(JsonPrimitive("brush_type"))
                                add(JsonPrimitive("hit_x"))
                                add(JsonPrimitive("hit_y"))
                                add(JsonPrimitive("hit_z"))
                            }
                        },
                ),
        )

    private fun deleteMeshTool() =
        ToolDefinitionDto(
            function =
                FunctionDefinitionDto(
                    name = "delete_mesh",
                    description = "Removes a mesh from the sculpt scene entirely.",
                    parameters =
                        buildJsonObject {
                            put("type", "object")
                            putJsonObject("properties") { putJsonObject("mesh_id") { put("type", "string") } }
                            putJsonArray("required") { add(JsonPrimitive("mesh_id")) }
                        },
                ),
        )

    private fun setMeshColorTool() =
        ToolDefinitionDto(
            function =
                FunctionDefinitionDto(
                    name = "set_mesh_color",
                    description = "Sets the base surface color of a mesh.",
                    parameters =
                        buildJsonObject {
                            put("type", "object")
                            putJsonObject("properties") {
                                putJsonObject("mesh_id") { put("type", "string") }
                                putJsonObject("color_hex") { put("type", "string") }
                            }
                            putJsonArray("required") {
                                add(JsonPrimitive("mesh_id"))
                                add(JsonPrimitive("color_hex"))
                            }
                        },
                ),
        )

    private fun transformMeshTool() =
        ToolDefinitionDto(
            function =
                FunctionDefinitionDto(
                    name = "transform_mesh",
                    description =
                        "Moves, rotates, or scales a mesh as a whole (not sculpting — repositioning the " +
                            "entire object in the scene). Any parameter omitted keeps its current value.",
                    parameters =
                        buildJsonObject {
                            put("type", "object")
                            putJsonObject("properties") {
                                putJsonObject("mesh_id") { put("type", "string") }
                                putJsonObject("position_x") { put("type", "number") }
                                putJsonObject("position_y") { put("type", "number") }
                                putJsonObject("position_z") { put("type", "number") }
                                putJsonObject("rotation_x_degrees") { put("type", "number") }
                                putJsonObject("rotation_y_degrees") { put("type", "number") }
                                putJsonObject("rotation_z_degrees") { put("type", "number") }
                                putJsonObject("scale_x") { put("type", "number") }
                                putJsonObject("scale_y") { put("type", "number") }
                                putJsonObject("scale_z") { put("type", "number") }
                            }
                            putJsonArray("required") { add(JsonPrimitive("mesh_id")) }
                        },
                ),
        )

    private fun inspectSceneTool() =
        ToolDefinitionDto(
            function =
                FunctionDefinitionDto(
                    name = "inspect_scene",
                    description =
                        "Requests a fresh render snapshot of the 3D sculpt scene from the current camera angle, plus a text summary " +
                            "of every mesh's name, id, vertex/triangle count, and position — use this to verify sculpting progress.",
                    parameters =
                        buildJsonObject {
                            put("type", "object")
                            putJsonObject("properties") {}
                        },
                ),
        )

    private fun finishTurnTool() =
        ToolDefinitionDto(
            function =
                FunctionDefinitionDto(
                    name = "finish_turn",
                    description = "Call this when the requested artwork/edit is complete and no further tool calls are needed this turn.",
                    parameters =
                        buildJsonObject {
                            put("type", "object")
                            putJsonObject("properties") {
                                putJsonObject("summary") {
                                    put("type", "string")
                                    put("description", "Brief description of what was created or changed, shown to the user.")
                                }
                            }
                            putJsonArray("required") { add(JsonPrimitive("summary")) }
                        },
                ),
        )
}

private fun kotlinx.serialization.json.JsonObjectBuilder.putJsonObject(
    key: String,
    builderAction: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit,
) {
    put(key, buildJsonObject(builderAction))
}

private fun kotlinx.serialization.json.JsonObjectBuilder.putJsonArray(
    key: String,
    builderAction: kotlinx.serialization.json.JsonArrayBuilder.() -> Unit,
) {
    put(key, buildJsonArray(builderAction))

    private fun listArtifactsTool() = ToolDefinitionDto(function = FunctionDefinitionDto(
        name = "list_artifacts",
        description = "Lists real artifacts stored in the active workspace, optionally filtered by name or MIME type.",
        parameters = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") { putJsonObject("query") { put("type", "string") } }
        },
    ))

    private fun searchWorkspaceTool() = ToolDefinitionDto(function = FunctionDefinitionDto(
        name = "search_workspace",
        description = "Searches persisted conversation threads, messages, and artifacts in the local workspace.",
        parameters = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") { putJsonObject("query") { put("type", "string") } }
            putJsonArray("required") { add(JsonPrimitive("query")) }
        },
    ))

    private fun artifactInfoTool() = ToolDefinitionDto(function = FunctionDefinitionDto(
        name = "artifact_info",
        description = "Returns verified metadata for an existing artifact ID, including path, MIME type and file size.",
        parameters = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") { putJsonObject("artifact_id") { put("type", "string") } }
            putJsonArray("required") { add(JsonPrimitive("artifact_id")) }
        },
    ))

    private fun checksumArtifactTool() = ToolDefinitionDto(function = FunctionDefinitionDto(
        name = "checksum_artifact",
        description = "Computes a SHA-256 checksum of an existing artifact so generated output can be integrity-verified.",
        parameters = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") { putJsonObject("artifact_id") { put("type", "string") } }
            putJsonArray("required") { add(JsonPrimitive("artifact_id")) }
        },
    ))

    private fun workspaceStatusTool() = ToolDefinitionDto(function = FunctionDefinitionDto(
        name = "get_workspace_status",
        description = "Returns a compact local runtime status including registered tools and plugin contracts.",
        parameters = buildJsonObject { put("type", "object"); putJsonObject("properties") {} },
    ))

}

private fun exportWorkspaceBundleTool() = ToolDefinitionDto(function = FunctionDefinitionDto(
    name = "export_workspace_bundle",
    description = "Exports the active conversation, redacted memory snapshot, metadata, and readable generated artifacts into a real portable ZIP workspace bundle.",
    parameters = buildJsonObject { put("type", "object"); putJsonObject("properties") {} },
))

private fun rememberTool() = ToolDefinitionDto(function = FunctionDefinitionDto(
    name = "remember",
    description = "Persist a useful non-secret user/project fact locally for future conversations and future app launches.",
    parameters = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("key") { put("type", "string") }
            putJsonObject("value") { put("type", "string") }
            putJsonObject("namespace") { put("type", "string") }
        }
        putJsonArray("required") { add(JsonPrimitive("key")); add(JsonPrimitive("value")) }
    }
))

private fun recallTool() = ToolDefinitionDto(function = FunctionDefinitionDto(
    name = "recall",
    description = "Search persistent local memory for a previous fact, preference, project detail, or instruction.",
    parameters = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("query") { put("type", "string") }
            putJsonObject("namespace") { put("type", "string") }
        }
        putJsonArray("required") { add(JsonPrimitive("query")) }
    }
))

private fun generateImageTool() = ToolDefinitionDto(function = FunctionDefinitionDto(
    name = "generate_image",
    description = "Generates an actual PNG image with the configured image-capable provider, saves it as a real artifact, and returns a shareable URI.",
    parameters = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("prompt") { put("type", "string") }
            putJsonObject("size") { put("type", "string") }
            putJsonObject("model") { put("type", "string") }
        }
        putJsonArray("required") { add(JsonPrimitive("prompt")) }
    }
))

private fun readWorkspaceFileTool() = ToolDefinitionDto(function = FunctionDefinitionDto(
    name = "read_workspace_file", description = "Reads a UTF-8 file from the managed ARTIFICER-X/works workspace. Safe path constrained.",
    parameters = buildJsonObject { put("type","object"); putJsonObject("properties") { putJsonObject("path"){put("type","string")}; putJsonObject("max_chars"){put("type","integer")} }; putJsonArray("required"){add(JsonPrimitive("path"))} }
))
private fun writeWorkspaceFileTool() = ToolDefinitionDto(function = FunctionDefinitionDto(
    name = "write_workspace_file", description = "Writes a UTF-8 file into the managed ARTIFICER-X/works workspace using an atomic replace.",
    parameters = buildJsonObject { put("type","object"); putJsonObject("properties") { putJsonObject("path"){put("type","string")}; putJsonObject("content"){put("type","string")} }; putJsonArray("required"){add(JsonPrimitive("path"));add(JsonPrimitive("content"))} }
))
private fun listWorkspaceDirectoryTool() = ToolDefinitionDto(function = FunctionDefinitionDto(
    name = "list_workspace_directory", description = "Lists files and directories inside the managed ARTIFICER-X/works workspace.",
    parameters = buildJsonObject { put("type","object"); putJsonObject("properties"){putJsonObject("path"){put("type","string")}} }
))
private fun replaceWorkspaceTextTool() = ToolDefinitionDto(function = FunctionDefinitionDto(
    name = "replace_workspace_text", description = "Replaces a precise text fragment in a managed workspace file. Prefer this for small code patches.",
    parameters = buildJsonObject { put("type","object"); putJsonObject("properties"){putJsonObject("path"){put("type","string")};putJsonObject("old"){put("type","string")};putJsonObject("new"){put("type","string")};putJsonObject("all"){put("type","boolean")}};putJsonArray("required"){add(JsonPrimitive("path"));add(JsonPrimitive("old"));add(JsonPrimitive("new"))} }
))

private fun createFileTool() = ToolDefinitionDto(function = FunctionDefinitionDto(
    name = "create_file",
    description = "Creates a real artifact file in the active chat workspace. Use for code, text, JSON, Markdown, SVG and project files.",
    parameters = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("file_name") { put("type", "string") }
            putJsonObject("content") { put("type", "string") }
            putJsonObject("mime_type") { put("type", "string") }
        }
        putJsonArray("required") { add(JsonPrimitive("file_name")); add(JsonPrimitive("content")) }
    }
))

private fun createZipTool() = ToolDefinitionDto(function = FunctionDefinitionDto(
    name = "create_zip",
    description = "Creates a real ZIP artifact from a JSON array of {name,content,mime_type} entries.",
    parameters = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("file_name") { put("type", "string") }
            putJsonObject("files_json") { put("type", "string") }
        }
        putJsonArray("required") { add(JsonPrimitive("file_name")); add(JsonPrimitive("files_json")) }
    }
))

private fun runTerminalTool() = ToolDefinitionDto(function = FunctionDefinitionDto(
    name = "run_terminal_command",
    description = "Runs one shell command inside Artificer-X's app-private terminal sandbox and returns stdout/stderr.",
    parameters = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("command") { put("type", "string") }
            putJsonObject("timeout_seconds") { put("type", "integer") }
        }
        putJsonArray("required") { add(JsonPrimitive("command")) }
    }
))

private fun runTerminalBatchTool() = ToolDefinitionDto(function = FunctionDefinitionDto(
    name = "run_terminal_batch",
    description = "Runs multiple shell commands sequentially inside the same private terminal sandbox.",
    parameters = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("commands") {
                put("type", "array")
                putJsonObject("items") { put("type", "string") }
            }
            putJsonObject("timeout_seconds") { put("type", "integer") }
        }
        putJsonArray("required") { add(JsonPrimitive("commands")) }
    }
))
