package com.waheed.artificerx.ui.screens.canvas

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
    onStrokeComplete: (points: List<Float>) -> Unit,
    onShapeInProgress: (startX: Float, startY: Float, endX: Float, endY: Float) -> Unit = { _, _, _, _ -> },
    onShapeComplete: (startX: Float, startY: Float, endX: Float, endY: Float) -> Unit,
    onFillTap: (x: Float, y: Float) -> Unit,
    onColorPickTap: (x: Float, y: Float) -> Unit,
): Modifier =
    this.pointerInput(toolState.activeTool) {
        when (toolState.activeTool) {
            DrawToolType.BRUSH, DrawToolType.ERASER -> {
                awaitEachGesture {
                    val down = awaitFirstDown(pass = PointerEventPass.Main)
                    val points = mutableListOf(down.position.x, down.position.y)
                    down.consumeAllChanges()
                    onStrokeInProgress(points.toList())

                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (change.pressed) {
                            points.add(change.position.x)
                            points.add(change.position.y)
                            onStrokeInProgress(points.toList())
                        }
                        change.consumeAllChanges()
                    } while (event.changes.any { it.pressed })

                    if (points.size >= 4) onStrokeComplete(points)
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

            else -> Unit
        }
    }
