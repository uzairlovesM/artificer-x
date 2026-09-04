package com.waheed.artificerx.ui.screens.canvas

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import com.waheed.artificerx.domain.model.DrawToolState
import com.waheed.artificerx.domain.model.DrawToolType

/**
 * Real finger-drawing surface — the manual counterpart to the agent's
 * draw_path/draw_shape tool calls. Every stroke a finger makes here
 * flows through exactly the same CanvasCompositor the agent uses, so
 * a human stroke and an agent stroke are pixel-identical in how
 * they're rasterized — no parallel/duplicate drawing code path.
 *
 * Handles input per active tool:
 *  - BRUSH/ERASER: continuous freehand path. Live points are reported
 *    via onStrokeInProgress on every move (so the caller can render an
 *    immediate on-screen preview overlay while the finger is still
 *    down — without this, a stroke only appears after lift-off, which
 *    reads as a broken/unresponsive canvas even though the underlying
 *    draw call is correct), then the final point list is flushed once
 *    via onStrokeComplete on release. This keeps the *committed* draw
 *    call a single compositor write per stroke (cheap on a budget
 *    device — Section 137) while still giving live visual feedback.
 *  - SHAPE_RECT/SHAPE_ELLIPSE/SHAPE_LINE: drag defines a bounding box
 *    from first-touch to release; live bounds reported the same way
 *    for an in-progress outline preview, single shape call on release.
 *  - FILL: single tap triggers one flood-fill call.
 *  - EYEDROPPER: single tap samples color.
 *
 * All coordinates emitted here are in the CALLER'S coordinate space
 * (i.e. whatever space the Modifier is attached in). The caller is
 * responsible for mapping into canvas-bitmap pixel space before
 * calling into StudioViewModel/CanvasCompositor if the two differ —
 * see StudioScreen's screenToCanvasPx for the Fit-scaled render
 * surface's version of that mapping.
 */
fun Modifier.canvasTouchInput(
    toolState: DrawToolState,
    canvasSizePx: IntSize,
    onStrokeInProgress: (points: List<Float>) -> Unit = {},
    onStrokeComplete: (points: List<Float>, pressureWeights: List<Float>?) -> Unit,
    onShapeInProgress: (startX: Float, startY: Float, endX: Float, endY: Float) -> Unit = { _, _, _, _ -> },
    onShapeComplete: (startX: Float, startY: Float, endX: Float, endY: Float) -> Unit,
    onFillTap: (x: Float, y: Float) -> Unit,
    onColorPickTap: (x: Float, y: Float) -> Unit,
    // v0.4.30 selection tool: same in-progress/complete shape as
    // SHAPE_RECT above (drag defines a bounding box), kept as separate
    // callbacks rather than reusing onShapeInProgress/onShapeComplete
    // so the caller can distinguish "draw a rectangle" from "select a
    // region" without inspecting toolState itself.
    onSelectionInProgress: (left: Float, top: Float, right: Float, bottom: Float) -> Unit = { _, _, _, _ -> },
    onSelectionComplete: (left: Float, top: Float, right: Float, bottom: Float) -> Unit = { _, _, _, _ -> },
    // v0.4.30 transform tool: fires once when the TRANSFORM tool becomes
    // active (see doc below on why gesture-start granularity is one
    // undo step per tool-activation rather than per individual drag —
    // a deliberate simplicity tradeoff), then once per gesture-update
    // frame with that frame's pan/zoom/rotation delta.
    onTransformGestureStart: () -> Unit = {},
    onTransformGesture: (dx: Float, dy: Float, scaleFactor: Float, rotationDegrees: Float, pivotX: Float, pivotY: Float) -> Unit =
        { _, _, _, _, _, _ -> },
): Modifier =
    this.pointerInput(toolState.activeTool) {
        when (toolState.activeTool) {
            DrawToolType.BRUSH, DrawToolType.ERASER -> {
                awaitEachGesture {
                    val down = awaitFirstDown(pass = PointerEventPass.Main)
                    val points = mutableListOf(down.position.x, down.position.y)
                    val pressureWeights = mutableListOf<Float>()
                    var lastPressure = down.pressure.coerceIn(0.15f, 1.6f)
                    down.consumeAllChanges()
                    onStrokeInProgress(points.toList())

                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (change.pressed) {
                            pressureWeights += ((lastPressure + change.pressure) * 0.5f).coerceIn(0.15f, 1.6f)
                            points.add(change.position.x)
                            points.add(change.position.y)
                            lastPressure = change.pressure.coerceIn(0.15f, 1.6f)
                            onStrokeInProgress(points.toList())
                        }
                        change.consumeAllChanges()
                    } while (event.changes.any { it.pressed })

                    if (points.size >= 4) onStrokeComplete(points, pressureWeights.take((points.size / 2 - 1).coerceAtLeast(0)))
                }
            }

            DrawToolType.SHAPE_RECT, DrawToolType.SHAPE_ELLIPSE, DrawToolType.SHAPE_LINE -> {
                awaitEachGesture {
                    val down = awaitFirstDown(pass = PointerEventPass.Main)
                    down.consumeAllChanges()
                    var lastPosition = down.position
                    onShapeInProgress(down.position.x, down.position.y, lastPosition.x, lastPosition.y)

                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        lastPosition = change.position
                        onShapeInProgress(down.position.x, down.position.y, lastPosition.x, lastPosition.y)
                        change.consumeAllChanges()
                    } while (event.changes.any { it.pressed })

                    onShapeComplete(down.position.x, down.position.y, lastPosition.x, lastPosition.y)
                }
            }

            DrawToolType.FILL -> {
                awaitEachGesture {
                    val down = awaitFirstDown(pass = PointerEventPass.Main)
                    down.consumeAllChanges()
                    onFillTap(down.position.x, down.position.y)
                }
            }

            DrawToolType.EYEDROPPER -> {
                awaitEachGesture {
                    val down = awaitFirstDown(pass = PointerEventPass.Main)
                    down.consumeAllChanges()
                    onColorPickTap(down.position.x, down.position.y)
                }
            }

            // v0.4.30 selection tool: identical drag-to-rect shape as
            // SHAPE_RECT (down defines one corner, drag/release the
            // opposite), routed to the selection callbacks instead of
            // drawing a shape.
            DrawToolType.SELECTION -> {
                awaitEachGesture {
                    val down = awaitFirstDown(pass = PointerEventPass.Main)
                    down.consumeAllChanges()
                    var lastPosition = down.position
                    onSelectionInProgress(down.position.x, down.position.y, lastPosition.x, lastPosition.y)

                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        lastPosition = change.position
                        onSelectionInProgress(down.position.x, down.position.y, lastPosition.x, lastPosition.y)
                        change.consumeAllChanges()
                    } while (event.changes.any { it.pressed })

                    onSelectionComplete(down.position.x, down.position.y, lastPosition.x, lastPosition.y)
                }
            }

            // v0.4.30 transform tool: Compose's own multi-touch
            // pan/zoom/rotate recognizer, which is considerably more
            // robust than hand-rolling centroid/angle math here. Fires
            // onTransformGestureStart once when this branch is entered
            // (i.e. once per TRANSFORM-tool activation — the pointerInput
            // key above is toolState.activeTool, so switching tools away
            // and back re-enters this branch and starts a fresh undo
            // step), then onTransformGesture continuously for as long as
            // the tool stays selected.
            DrawToolType.TRANSFORM -> {
                onTransformGestureStart()
                detectTransformGestures { centroid, pan, zoom, rotation ->
                    onTransformGesture(pan.x, pan.y, zoom, rotation, centroid.x, centroid.y)
                }
            }

            else -> Unit
        }
    }
