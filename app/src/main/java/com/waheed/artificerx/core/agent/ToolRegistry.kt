package com.waheed.artificerx.core.agent

import com.waheed.artificerx.core.network.FunctionDefinitionDto
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
    val ALL_TOOLS: List<ToolDefinitionDto> =
        listOf(
            createLayerTool(),
            deleteLayerTool(),
            setActiveLayerTool(),
            drawPathTool(),
            drawShapeTool(),
            applyGradientTool(),
            fillRegionTool(),
            setLayerPropertyTool(),
            duplicateLayerTool(),
            flipLayerTool(),
            cropCanvasTool(),
            inspectCanvasTool(),
            pickColorTool(),
            applyFilterTool(),
            addTextTool(),
            createMaskTool(),
            enableSymmetryTool(),
            applyPatternTool(),
            drawCurveTool(),
            importImageLayerTool(),
            webFetchTool(),
            createPrimitiveTool(),
            sculptStrokeTool(),
            deleteMeshTool(),
            setMeshColorTool(),
            transformMeshTool(),
            inspectSceneTool(),
            finishTurnTool(),
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
                            }
                            putJsonArray("required") { add(JsonPrimitive("points")) }
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
                            "stroke is automatically mirrored across a vertical, horizontal, or radial axis, " +
                            "useful for mandalas, faces, and symmetric character designs.",
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
}
