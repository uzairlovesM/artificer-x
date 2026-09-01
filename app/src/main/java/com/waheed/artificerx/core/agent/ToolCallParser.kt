package com.waheed.artificerx.core.agent

import com.waheed.artificerx.core.network.ToolCallDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Decodes the raw JSON string in ToolCallDto.function.arguments into a
 * type-safe ParsedToolCall. Every field access is defensive (missing
 * or malformed args from the LLM must never crash the app — Section
 * 191 Reliability Engineering) and falls back to sensible defaults or
 * Unknown, letting ToolExecutor report a structured tool-result error
 * back to the LLM instead of the whole turn dying.
 */
object ToolCallParser {
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    fun parse(toolCall: ToolCallDto): ParsedToolCall {
        val args =
            runCatching {
                json.parseToJsonElement(toolCall.function.arguments).jsonObject
            }.getOrNull() ?: JsonObject(emptyMap())

        return when (toolCall.function.name) {
            "create_layer" ->
                ParsedToolCall.CreateLayer(
                    name = args["name"]?.jsonPrimitive?.contentOrNull ?: "New Layer",
                )
            "delete_layer" ->
                ParsedToolCall.DeleteLayer(
                    layerId = args["layer_id"]?.jsonPrimitive?.contentOrNull ?: "",
                )
            "set_active_layer" ->
                ParsedToolCall.SetActiveLayer(
                    layerId = args["layer_id"]?.jsonPrimitive?.contentOrNull ?: "",
                )
            "draw_path" ->
                ParsedToolCall.DrawPath(
                    points = (args["points"] as? JsonArray)?.mapNotNull { it.jsonPrimitive.floatOrNull } ?: emptyList(),
                    colorHex = args["color_hex"]?.jsonPrimitive?.contentOrNull,
                    strokeWidthPx = args["stroke_width_px"]?.jsonPrimitive?.floatOrNull,
                    opacity = args["opacity"]?.jsonPrimitive?.floatOrNull,
                )
            "draw_shape" ->
                ParsedToolCall.DrawShape(
                    shapeType = args["shape_type"]?.jsonPrimitive?.contentOrNull ?: "rectangle",
                    x = args["x"]?.jsonPrimitive?.floatOrNull ?: 0f,
                    y = args["y"]?.jsonPrimitive?.floatOrNull ?: 0f,
                    width = args["width"]?.jsonPrimitive?.floatOrNull,
                    height = args["height"]?.jsonPrimitive?.floatOrNull,
                    fillColorHex = args["fill_color_hex"]?.jsonPrimitive?.contentOrNull,
                    strokeColorHex = args["stroke_color_hex"]?.jsonPrimitive?.contentOrNull,
                    strokeWidthPx = args["stroke_width_px"]?.jsonPrimitive?.floatOrNull,
                    rotationDegrees = args["rotation_degrees"]?.jsonPrimitive?.floatOrNull,
                    sides = args["sides"]?.jsonPrimitive?.floatOrNull?.toInt(),
                )
            "apply_gradient" ->
                ParsedToolCall.ApplyGradient(
                    gradientType = args["gradient_type"]?.jsonPrimitive?.contentOrNull ?: "linear",
                    startColorHex = args["start_color_hex"]?.jsonPrimitive?.contentOrNull ?: "#000000",
                    endColorHex = args["end_color_hex"]?.jsonPrimitive?.contentOrNull ?: "#FFFFFF",
                    x = args["x"]?.jsonPrimitive?.floatOrNull,
                    y = args["y"]?.jsonPrimitive?.floatOrNull,
                    width = args["width"]?.jsonPrimitive?.floatOrNull,
                    height = args["height"]?.jsonPrimitive?.floatOrNull,
                    angleDegrees = args["angle_degrees"]?.jsonPrimitive?.floatOrNull,
                    additionalColorStopsHex =
                        (args["additional_color_stops_hex"] as? JsonArray)
                            ?.mapNotNull { it.jsonPrimitive.contentOrNull },
                )
            "fill_region" ->
                ParsedToolCall.FillRegion(
                    x = args["x"]?.jsonPrimitive?.floatOrNull ?: 0f,
                    y = args["y"]?.jsonPrimitive?.floatOrNull ?: 0f,
                    colorHex = args["color_hex"]?.jsonPrimitive?.contentOrNull ?: "#000000",
                    tolerance = args["tolerance"]?.jsonPrimitive?.floatOrNull,
                )
            "set_layer_property" ->
                ParsedToolCall.SetLayerProperty(
                    layerId = args["layer_id"]?.jsonPrimitive?.contentOrNull ?: "",
                    opacity = args["opacity"]?.jsonPrimitive?.floatOrNull,
                    blendMode = args["blend_mode"]?.jsonPrimitive?.contentOrNull,
                    isVisible = args["is_visible"]?.jsonPrimitive?.booleanOrNull,
                )
            "duplicate_layer" ->
                ParsedToolCall.DuplicateLayer(
                    sourceLayerId = args["layer_id"]?.jsonPrimitive?.contentOrNull ?: "",
                    newName = args["new_name"]?.jsonPrimitive?.contentOrNull,
                )
            "flip_layer" ->
                ParsedToolCall.FlipLayer(
                    layerId = args["layer_id"]?.jsonPrimitive?.contentOrNull ?: "",
                    horizontal = args["horizontal"]?.jsonPrimitive?.booleanOrNull ?: false,
                    vertical = args["vertical"]?.jsonPrimitive?.booleanOrNull ?: false,
                )
            "crop_canvas" ->
                ParsedToolCall.CropCanvas(
                    x = (args["x"]?.jsonPrimitive?.floatOrNull ?: 0f).toInt(),
                    y = (args["y"]?.jsonPrimitive?.floatOrNull ?: 0f).toInt(),
                    width = (args["width"]?.jsonPrimitive?.floatOrNull ?: 0f).toInt(),
                    height = (args["height"]?.jsonPrimitive?.floatOrNull ?: 0f).toInt(),
                )
            "inspect_canvas" -> ParsedToolCall.InspectCanvas
            "pick_color" ->
                ParsedToolCall.PickColor(
                    x = args["x"]?.jsonPrimitive?.floatOrNull ?: 0f,
                    y = args["y"]?.jsonPrimitive?.floatOrNull ?: 0f,
                )
            "apply_filter" ->
                ParsedToolCall.ApplyFilter(
                    layerId = args["layer_id"]?.jsonPrimitive?.contentOrNull ?: "",
                    filterType = args["filter_type"]?.jsonPrimitive?.contentOrNull ?: "blur",
                    intensity = args["intensity"]?.jsonPrimitive?.floatOrNull,
                )
            "add_text" ->
                ParsedToolCall.AddText(
                    text = args["text"]?.jsonPrimitive?.contentOrNull ?: "",
                    x = args["x"]?.jsonPrimitive?.floatOrNull ?: 0f,
                    y = args["y"]?.jsonPrimitive?.floatOrNull ?: 0f,
                    fontSizePx = args["font_size_px"]?.jsonPrimitive?.floatOrNull,
                    colorHex = args["color_hex"]?.jsonPrimitive?.contentOrNull,
                    bold = args["bold"]?.jsonPrimitive?.booleanOrNull,
                )
            "create_mask" ->
                ParsedToolCall.CreateMask(
                    layerId = args["layer_id"]?.jsonPrimitive?.contentOrNull ?: "",
                    maskShape = args["mask_shape"]?.jsonPrimitive?.contentOrNull ?: "rectangle",
                    x = args["x"]?.jsonPrimitive?.floatOrNull ?: 0f,
                    y = args["y"]?.jsonPrimitive?.floatOrNull ?: 0f,
                    width = args["width"]?.jsonPrimitive?.floatOrNull ?: 0f,
                    height = args["height"]?.jsonPrimitive?.floatOrNull ?: 0f,
                    invert = args["invert"]?.jsonPrimitive?.booleanOrNull,
                )
            "enable_symmetry" ->
                ParsedToolCall.EnableSymmetry(
                    mode = args["mode"]?.jsonPrimitive?.contentOrNull ?: "off",
                )
            "apply_pattern" ->
                ParsedToolCall.ApplyPattern(
                    patternType = args["pattern_type"]?.jsonPrimitive?.contentOrNull ?: "dots",
                    x = args["x"]?.jsonPrimitive?.floatOrNull ?: 0f,
                    y = args["y"]?.jsonPrimitive?.floatOrNull ?: 0f,
                    width = args["width"]?.jsonPrimitive?.floatOrNull ?: 0f,
                    height = args["height"]?.jsonPrimitive?.floatOrNull ?: 0f,
                    colorHex = args["color_hex"]?.jsonPrimitive?.contentOrNull ?: "#000000",
                    scalePx = args["scale_px"]?.jsonPrimitive?.floatOrNull,
                )
            "draw_curve" ->
                ParsedToolCall.DrawCurve(
                    startX = args["start_x"]?.jsonPrimitive?.floatOrNull ?: 0f,
                    startY = args["start_y"]?.jsonPrimitive?.floatOrNull ?: 0f,
                    controlX = args["control_x"]?.jsonPrimitive?.floatOrNull ?: 0f,
                    controlY = args["control_y"]?.jsonPrimitive?.floatOrNull ?: 0f,
                    endX = args["end_x"]?.jsonPrimitive?.floatOrNull ?: 0f,
                    endY = args["end_y"]?.jsonPrimitive?.floatOrNull ?: 0f,
                    colorHex = args["color_hex"]?.jsonPrimitive?.contentOrNull,
                    strokeWidthPx = args["stroke_width_px"]?.jsonPrimitive?.floatOrNull,
                )
            "import_image_layer" ->
                ParsedToolCall.ImportImageLayer(
                    layerName = args["layer_name"]?.jsonPrimitive?.contentOrNull ?: "Imported Image",
                    opacity = args["opacity"]?.jsonPrimitive?.floatOrNull,
                )
            "web_fetch" ->
                ParsedToolCall.WebFetch(
                    url = args["url"]?.jsonPrimitive?.contentOrNull ?: "",
                )
            "create_primitive" ->
                ParsedToolCall.CreatePrimitive(
                    primitiveType = args["primitive_type"]?.jsonPrimitive?.contentOrNull ?: "sphere",
                    name = args["name"]?.jsonPrimitive?.contentOrNull ?: "New Mesh",
                )
            "sculpt_stroke" ->
                ParsedToolCall.SculptStroke(
                    meshId = args["mesh_id"]?.jsonPrimitive?.contentOrNull ?: "",
                    brushType = args["brush_type"]?.jsonPrimitive?.contentOrNull ?: "pull",
                    hitX = args["hit_x"]?.jsonPrimitive?.floatOrNull ?: 0f,
                    hitY = args["hit_y"]?.jsonPrimitive?.floatOrNull ?: 0f,
                    hitZ = args["hit_z"]?.jsonPrimitive?.floatOrNull ?: 0f,
                    radius = args["radius"]?.jsonPrimitive?.floatOrNull,
                    strength = args["strength"]?.jsonPrimitive?.floatOrNull,
                )
            "delete_mesh" ->
                ParsedToolCall.DeleteMesh(
                    meshId = args["mesh_id"]?.jsonPrimitive?.contentOrNull ?: "",
                )
            "set_mesh_color" ->
                ParsedToolCall.SetMeshColor(
                    meshId = args["mesh_id"]?.jsonPrimitive?.contentOrNull ?: "",
                    colorHex = args["color_hex"]?.jsonPrimitive?.contentOrNull ?: "#CCCCCC",
                )
            "transform_mesh" ->
                ParsedToolCall.TransformMesh(
                    meshId = args["mesh_id"]?.jsonPrimitive?.contentOrNull ?: "",
                    positionX = args["position_x"]?.jsonPrimitive?.floatOrNull,
                    positionY = args["position_y"]?.jsonPrimitive?.floatOrNull,
                    positionZ = args["position_z"]?.jsonPrimitive?.floatOrNull,
                    rotationXDegrees = args["rotation_x_degrees"]?.jsonPrimitive?.floatOrNull,
                    rotationYDegrees = args["rotation_y_degrees"]?.jsonPrimitive?.floatOrNull,
                    rotationZDegrees = args["rotation_z_degrees"]?.jsonPrimitive?.floatOrNull,
                    scaleX = args["scale_x"]?.jsonPrimitive?.floatOrNull,
                    scaleY = args["scale_y"]?.jsonPrimitive?.floatOrNull,
                    scaleZ = args["scale_z"]?.jsonPrimitive?.floatOrNull,
                )
            "inspect_scene" -> ParsedToolCall.InspectScene
            "finish_turn" ->
                ParsedToolCall.FinishTurn(
                    summary = args["summary"]?.jsonPrimitive?.contentOrNull ?: "Done.",
                )
            else -> ParsedToolCall.Unknown(toolCall.function.name)
        }
    }
}
