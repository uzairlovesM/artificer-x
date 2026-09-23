package com.waheed.artificerx.core.agent

import kotlin.math.min

/**
 * Deterministic numeric safety boundary between parsed AI tool calls and the
 * pixel/bitmap execution layer.
 *
 * The parser validates structure and required fields. This gate owns the
 * second boundary: finite numbers, bounded geometry, bounded collection
 * sizes, normalized blend modes, and device-safe canvas dimensions.
 *
 * The goal is not to make creative decisions for the model. It is to prevent
 * malformed or adversarial numeric input from turning into NaN/Infinity,
 * pathological memory requests, or unexpectedly gigantic draw operations.
 */
object CanvasToolSafetyGate {
    private const val MAX_DRAW_PATH_VALUES = 40_000
    private const val MAX_GRADIENT_STOPS = 32
    private const val MIN_CANVAS_SIDE = 64
    private const val MAX_CANVAS_SIDE = 8_000
    private const val MIN_STROKE = 0.25f
    private const val MAX_STROKE = 4_096f
    private const val MIN_FONT_SIZE = 4f
    private const val MAX_FONT_SIZE = 2_048f
    private const val MIN_PATTERN_SCALE = 1f
    private const val MAX_PATTERN_SCALE = 4_096f
    private const val MAX_TRANSFORM_TRANSLATION_MULTIPLIER = 4f

    private val allowedBlendModes = setOf(
        "normal",
        "multiply",
        "screen",
        "overlay",
        "darken",
        "lighten",
        "color_dodge",
        "color_burn",
        "add",
        "subtract",
    )

    sealed interface Outcome {
        data class Accepted(val call: ParsedToolCall) : Outcome
        data class Rejected(val reason: String) : Outcome
    }

    fun sanitize(
        call: ParsedToolCall,
        canvasWidth: Int,
        canvasHeight: Int,
    ): Outcome {
        if (canvasWidth <= 0 || canvasHeight <= 0) {
            return Outcome.Rejected("Canvas dimensions must be positive before executing a canvas tool.")
        }

        val maxX = (canvasWidth - 1).coerceAtLeast(0).toFloat()
        val maxY = (canvasHeight - 1).coerceAtLeast(0).toFloat()
        val maxWidth = canvasWidth.toFloat()
        val maxHeight = canvasHeight.toFloat()

        fun finite(value: Float, field: String): String? =
            if (value.isFinite()) null else "$field must be finite; got $value."

        fun finiteAll(values: List<Float>, field: String): String? {
            for ((index, value) in values.withIndex()) {
                if (!value.isFinite()) return "$field[$index] must be finite; got $value."
            }
            return null
        }

        fun bounded(value: Float?, minValue: Float, maxValue: Float): Float? =
            value?.coerceIn(minValue, maxValue)

        fun rejectIf(value: String?): Outcome.Rejected? = value?.let(Outcome::Rejected)

        return when (call) {
            is ParsedToolCall.DrawPath -> {
                if (call.points.size > MAX_DRAW_PATH_VALUES) {
                    Outcome.Rejected(
                        "draw_path received ${call.points.size} numeric values; maximum is $MAX_DRAW_PATH_VALUES to keep a single tool call bounded.",
                    )
                } else if (call.points.size % 2 != 0 || call.points.size < 4) {
                    Outcome.Rejected("draw_path requires an even number of coordinate values with at least two points.")
                } else {
                    rejectIf(finiteAll(call.points, "points"))
                        ?: rejectIf(finite(call.strokeWidthPx ?: MIN_STROKE, "stroke_width_px"))
                        ?: rejectIf(finite(call.opacity ?: 1f, "opacity"))
                        ?: Outcome.Accepted(
                            call.copy(
                                points = call.points.mapIndexed { index, value ->
                                    if (index % 2 == 0) value.coerceIn(0f, maxX) else value.coerceIn(0f, maxY)
                                },
                                strokeWidthPx = call.strokeWidthPx?.coerceIn(MIN_STROKE, MAX_STROKE),
                                opacity = call.opacity?.coerceIn(0f, 1f),
                            ),
                        )
                }
            }

            is ParsedToolCall.DrawShape -> {
                rejectIf(finite(call.x, "x"))
                    ?: rejectIf(finite(call.y, "y"))
                    ?: rejectIf(call.width?.let { finite(it, "width") })
                    ?: rejectIf(call.height?.let { finite(it, "height") })
                    ?: rejectIf(call.strokeWidthPx?.let { finite(it, "stroke_width_px") })
                    ?: rejectIf(call.rotationDegrees?.let { finite(it, "rotation_degrees") })
                    ?: Outcome.Accepted(
                        call.copy(
                            x = call.x.coerceIn(0f, maxX),
                            y = call.y.coerceIn(0f, maxY),
                            width = call.width?.coerceIn(1f, maxWidth),
                            height = call.height?.coerceIn(1f, maxHeight),
                            strokeWidthPx = call.strokeWidthPx?.coerceIn(MIN_STROKE, MAX_STROKE),
                            rotationDegrees = call.rotationDegrees?.coerceIn(-360f, 360f),
                            sides = call.sides?.coerceIn(3, 128),
                        ),
                    )
            }

            is ParsedToolCall.ApplyGradient -> {
                if ((call.additionalColorStopsHex?.size ?: 0) > MAX_GRADIENT_STOPS) {
                    Outcome.Rejected("apply_gradient allows at most $MAX_GRADIENT_STOPS additional color stops.")
                } else {
                    rejectIf(call.x?.let { finite(it, "x") })
                        ?: rejectIf(call.y?.let { finite(it, "y") })
                        ?: rejectIf(call.width?.let { finite(it, "width") })
                        ?: rejectIf(call.height?.let { finite(it, "height") })
                        ?: rejectIf(call.angleDegrees?.let { finite(it, "angle_degrees") })
                        ?: Outcome.Accepted(
                            call.copy(
                                x = call.x?.coerceIn(0f, maxX),
                                y = call.y?.coerceIn(0f, maxY),
                                width = call.width?.coerceIn(1f, maxWidth),
                                height = call.height?.coerceIn(1f, maxHeight),
                                angleDegrees = call.angleDegrees?.coerceIn(-360f, 360f),
                                additionalColorStopsHex = call.additionalColorStopsHex?.take(MAX_GRADIENT_STOPS),
                            ),
                        )
                }
            }

            is ParsedToolCall.FillRegion -> {
                rejectIf(finite(call.x, "x"))
                    ?: rejectIf(finite(call.y, "y"))
                    ?: rejectIf(call.tolerance?.let { finite(it, "tolerance") })
                    ?: Outcome.Accepted(
                        call.copy(
                            x = call.x.coerceIn(0f, maxX),
                            y = call.y.coerceIn(0f, maxY),
                            tolerance = call.tolerance?.coerceIn(0f, 1f),
                        ),
                    )
            }

            is ParsedToolCall.SetLayerProperty -> {
                val normalizedBlend = call.blendMode?.trim()?.lowercase()
                when {
                    normalizedBlend != null && normalizedBlend !in allowedBlendModes ->
                        Outcome.Rejected(
                            "Unsupported blend_mode '${call.blendMode}'. Allowed values: ${allowedBlendModes.sorted().joinToString(", ")}.",
                        )
                    call.opacity?.let { !it.isFinite() } == true ->
                        Outcome.Rejected("opacity must be finite; got ${call.opacity}.")
                    else ->
                        Outcome.Accepted(call.copy(opacity = call.opacity?.coerceIn(0f, 1f), blendMode = normalizedBlend))
                }
            }

            is ParsedToolCall.CropCanvas -> {
                val left = call.x.coerceAtLeast(0)
                val top = call.y.coerceAtLeast(0)
                val right = min(canvasWidth, left + call.width.coerceAtLeast(0))
                val bottom = min(canvasHeight, top + call.height.coerceAtLeast(0))
                if (right <= left || bottom <= top) {
                    Outcome.Rejected("crop_canvas must overlap the current canvas with a positive width and height.")
                } else {
                    Outcome.Accepted(call.copy(x = left, y = top, width = right - left, height = bottom - top))
                }
            }

            is ParsedToolCall.PickColor -> {
                rejectIf(finite(call.x, "x"))
                    ?: rejectIf(finite(call.y, "y"))
                    ?: Outcome.Accepted(call.copy(x = call.x.coerceIn(0f, maxX), y = call.y.coerceIn(0f, maxY)))
            }

            is ParsedToolCall.ApplyFilter -> {
                rejectIf(call.intensity?.let { finite(it, "intensity") })
                    ?: Outcome.Accepted(call.copy(intensity = call.intensity?.coerceIn(0f, 1f)))
            }

            is ParsedToolCall.AddText -> {
                rejectIf(finite(call.x, "x"))
                    ?: rejectIf(finite(call.y, "y"))
                    ?: rejectIf(call.fontSizePx?.let { finite(it, "font_size_px") })
                    ?: Outcome.Accepted(
                        call.copy(
                            x = call.x.coerceIn(0f, maxX),
                            y = call.y.coerceIn(0f, maxY),
                            fontSizePx = call.fontSizePx?.coerceIn(MIN_FONT_SIZE, MAX_FONT_SIZE),
                        ),
                    )
            }

            is ParsedToolCall.CreateMask -> {
                rejectIf(finite(call.x, "x"))
                    ?: rejectIf(finite(call.y, "y"))
                    ?: rejectIf(finite(call.width, "width"))
                    ?: rejectIf(finite(call.height, "height"))
                    ?: Outcome.Accepted(
                        call.copy(
                            x = call.x.coerceIn(0f, maxX),
                            y = call.y.coerceIn(0f, maxY),
                            width = call.width.coerceIn(1f, maxWidth),
                            height = call.height.coerceIn(1f, maxHeight),
                        ),
                    )
            }

            is ParsedToolCall.ApplyPattern -> {
                rejectIf(finite(call.x, "x"))
                    ?: rejectIf(finite(call.y, "y"))
                    ?: rejectIf(finite(call.width, "width"))
                    ?: rejectIf(finite(call.height, "height"))
                    ?: rejectIf(call.scalePx?.let { finite(it, "scale_px") })
                    ?: Outcome.Accepted(
                        call.copy(
                            x = call.x.coerceIn(0f, maxX),
                            y = call.y.coerceIn(0f, maxY),
                            width = call.width.coerceIn(1f, maxWidth),
                            height = call.height.coerceIn(1f, maxHeight),
                            scalePx = call.scalePx?.coerceIn(MIN_PATTERN_SCALE, MAX_PATTERN_SCALE),
                        ),
                    )
            }

            is ParsedToolCall.DrawCurve -> {
                val values = listOf(call.startX, call.startY, call.controlX, call.controlY, call.endX, call.endY)
                rejectIf(finiteAll(values, "curve"))
                    ?: rejectIf(call.strokeWidthPx?.let { finite(it, "stroke_width_px") })
                    ?: Outcome.Accepted(
                        call.copy(
                            startX = call.startX.coerceIn(0f, maxX),
                            startY = call.startY.coerceIn(0f, maxY),
                            controlX = call.controlX.coerceIn(0f, maxX),
                            controlY = call.controlY.coerceIn(0f, maxY),
                            endX = call.endX.coerceIn(0f, maxX),
                            endY = call.endY.coerceIn(0f, maxY),
                            strokeWidthPx = call.strokeWidthPx?.coerceIn(MIN_STROKE, MAX_STROKE),
                        ),
                    )
            }

            is ParsedToolCall.RelightScene -> {
                rejectIf(finite(call.directionDegrees, "direction_degrees"))
                    ?: rejectIf(call.intensity?.let { finite(it, "intensity") })
                    ?: Outcome.Accepted(
                        call.copy(
                            directionDegrees = call.directionDegrees.coerceIn(-360f, 360f),
                            intensity = call.intensity?.coerceIn(0f, 1f),
                        ),
                    )
            }

            is ParsedToolCall.ResizeCanvas -> {
                if (call.widthPx !in MIN_CANVAS_SIDE..MAX_CANVAS_SIDE || call.heightPx !in MIN_CANVAS_SIDE..MAX_CANVAS_SIDE) {
                    Outcome.Rejected(
                        "resize_canvas supports ${MIN_CANVAS_SIDE}..${MAX_CANVAS_SIDE}px per side to stay inside the app's bitmap memory ceiling.",
                    )
                } else {
                    Outcome.Accepted(call)
                }
            }

            is ParsedToolCall.SetBrushDefaults -> {
                rejectIf(call.sizePx?.let { finite(it, "size_px") })
                    ?: rejectIf(call.opacity?.let { finite(it, "opacity") })
                    ?: rejectIf(call.hardness?.let { finite(it, "hardness") })
                    ?: Outcome.Accepted(
                        call.copy(
                            sizePx = call.sizePx?.coerceIn(MIN_STROKE, MAX_STROKE),
                            opacity = call.opacity?.coerceIn(0f, 1f),
                            hardness = call.hardness?.coerceIn(0f, 1f),
                        ),
                    )
            }

            is ParsedToolCall.SetSelection -> {
                val values = listOf(call.left, call.top, call.right, call.bottom)
                rejectIf(finiteAll(values, "selection"))
                    ?: Outcome.Accepted(
                        call.copy(
                            left = call.left.coerceIn(0f, maxX),
                            top = call.top.coerceIn(0f, maxY),
                            right = call.right.coerceIn(0f, maxX),
                            bottom = call.bottom.coerceIn(0f, maxY),
                        ),
                    )
            }

            is ParsedToolCall.TransformLayer -> {
                val values = listOf(call.dx, call.dy, call.scaleFactor, call.rotationDegrees)
                rejectIf(finiteAll(values, "transform"))
                    ?: rejectIf(call.pivotX?.let { finite(it, "pivot_x") })
                    ?: rejectIf(call.pivotY?.let { finite(it, "pivot_y") })
                    ?: Outcome.Accepted(
                        call.copy(
                            dx = call.dx.coerceIn(-maxWidth * MAX_TRANSFORM_TRANSLATION_MULTIPLIER, maxWidth * MAX_TRANSFORM_TRANSLATION_MULTIPLIER),
                            dy = call.dy.coerceIn(-maxHeight * MAX_TRANSFORM_TRANSLATION_MULTIPLIER, maxHeight * MAX_TRANSFORM_TRANSLATION_MULTIPLIER),
                            scaleFactor = call.scaleFactor.coerceIn(0.05f, 20f),
                            rotationDegrees = call.rotationDegrees.coerceIn(-360f, 360f),
                            pivotX = call.pivotX?.coerceIn(0f, maxX),
                            pivotY = call.pivotY?.coerceIn(0f, maxY),
                        ),
                    )
            }

            else -> Outcome.Accepted(call)
        }
    }
}
