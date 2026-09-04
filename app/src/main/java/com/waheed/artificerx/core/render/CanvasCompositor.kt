package com.waheed.artificerx.core.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.core.graphics.get
import com.waheed.artificerx.domain.model.CanvasLayer
import com.waheed.artificerx.domain.model.LayerBlendMode
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/**
 * Real pixel-level drawing operations, called by ToolExecutor once a
 * tool_call is validated. This is the concrete layer beneath Section
 * 26's layer system and what makes Section 179/180's "the brain draws
 * via tool calls" claim literally true rather than a structural stub —
 * every method here mutates a real Bitmap via android.graphics.Canvas.
 */
@Singleton
class CanvasCompositor
    @Inject
    constructor(
        private val bitmapStore: LayerBitmapStore,
    ) {
        /** v0.4.30 real brush engine: [brushType] genuinely changes how the
         *  stroke renders (not just a label) — see the private per-type
         *  functions below. [pointWeights], when supplied, is a parallel
         *  per-point 0..1 multiplier (from simulated touch pressure or a
         *  future real stylus reading) applied on top of [strokeWidthPx];
         *  null means "flat width," preserving old behavior for every
         *  existing caller (including the AI's draw_path tool call, which
         *  doesn't send weights) so nothing else breaks by adding this. */
        fun drawPath(
            layerId: String,
            points: List<Float>,
            colorHex: String?,
            strokeWidthPx: Float?,
            opacity: Float?,
            brushType: com.waheed.artificerx.domain.model.BrushType =
                com.waheed.artificerx.domain.model.BrushType.INK_PEN,
            pointWeights: List<Float>? = null,
        ): Boolean {
            val canvas = bitmapStore.getCanvas(layerId) ?: return false
            if (points.size < 4) return false
            val color = safeParseColor(colorHex, default = Color.BLACK)
            val baseWidth = strokeWidthPx ?: 8f
            val baseAlpha = ((opacity ?: 1f).coerceIn(0f, 1f) * 255).toInt()

            when (brushType) {
                com.waheed.artificerx.domain.model.BrushType.INK_PEN ->
                    drawSmoothStroke(canvas, points, color, baseWidth, baseAlpha, pointWeights)
                com.waheed.artificerx.domain.model.BrushType.PENCIL ->
                    drawPencilStroke(canvas, points, color, baseWidth, baseAlpha, pointWeights)
                com.waheed.artificerx.domain.model.BrushType.MARKER ->
                    drawMarkerStroke(canvas, points, color, baseWidth, baseAlpha)
                com.waheed.artificerx.domain.model.BrushType.CALLIGRAPHY ->
                    drawCalligraphyStroke(canvas, points, color, baseWidth, baseAlpha)
                com.waheed.artificerx.domain.model.BrushType.AIRBRUSH ->
                    drawSoftDabStroke(canvas, points, color, baseWidth, baseAlpha, blurRadius = baseWidth * 0.5f, dabAlpha = 26)
                com.waheed.artificerx.domain.model.BrushType.WATERCOLOR ->
                    drawSoftDabStroke(canvas, points, color, baseWidth * 1.6f, baseAlpha, blurRadius = baseWidth * 0.9f, dabAlpha = 14)
                com.waheed.artificerx.domain.model.BrushType.CHARCOAL ->
                    drawCharcoalStroke(canvas, points, color, baseWidth, baseAlpha)
                com.waheed.artificerx.domain.model.BrushType.ERASER_SOFT ->
                    drawSoftDabStroke(canvas, points, Color.TRANSPARENT, baseWidth, 255, blurRadius = baseWidth * 0.6f, dabAlpha = 40, isClear = true)
            }
            return true
        }

        /** Alpha-lock drawing: the new pixels are constrained to the pre-existing alpha mask. */
        fun drawPathAlphaLocked(
            layerId: String,
            points: List<Float>,
            colorHex: String?,
            strokeWidthPx: Float?,
            opacity: Float?,
            brushType: com.waheed.artificerx.domain.model.BrushType,
        ): Boolean {
            val bitmap = bitmapStore.getBitmap(layerId) ?: return false
            val canvas = bitmapStore.getCanvas(layerId) ?: return false
            if (points.size < 4) return false
            val alphaMask = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            val drawn = drawPath(layerId, points, colorHex, strokeWidthPx, opacity, brushType)
            if (!drawn) { alphaMask.recycle(); return false }
            val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) }
            canvas.drawBitmap(alphaMask, 0f, 0f, maskPaint)
            maskPaint.xfermode = null
            alphaMask.recycle()
            return true
        }

        /** True transparency erase — Paint.Style.STROKE with an XOR/CLEAR
         *  Xfermode along the path, so it actually punches a hole down to
         *  alpha = 0 regardless of what color is underneath. v0.4.30 fix:
         *  the eraser previously worked by drawing opaque white over the
         *  stroke (see old StudioViewModel.drawManualStroke), which looked
         *  right only by accident on a plain white background and quietly
         *  corrupted transparency for layer blending / PNG export
         *  everywhere else — hard eraser and soft eraser (ERASER_SOFT
         *  brush type above) now both genuinely clear pixels. */
        fun erasePath(
            layerId: String,
            points: List<Float>,
            strokeWidthPx: Float?,
        ): Boolean {
            val canvas = bitmapStore.getCanvas(layerId) ?: return false
            if (points.size < 4) return false
            val path = Path().apply {
                moveTo(points[0], points[1])
                var i = 2
                while (i + 1 < points.size) {
                    lineTo(points[i], points[i + 1])
                    i += 2
                }
            }
            val paint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                    strokeWidth = strokeWidthPx ?: 8f
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                }
            canvas.drawPath(path, paint)
            return true
        }

        private fun buildPath(points: List<Float>): Path =
            Path().apply {
                moveTo(points[0], points[1])
                var i = 2
                while (i + 1 < points.size) {
                    lineTo(points[i], points[i + 1])
                    i += 2
                }
            }

        /** INK_PEN: the original clean round-cap stroke, now width-
         *  modulated per segment when [pointWeights] is present so a slow
         *  "heavy" part of a finger stroke is visibly thicker than a fast
         *  "light" flick — the touch-simulated-pressure feel. */
        private fun drawSmoothStroke(
            canvas: Canvas,
            points: List<Float>,
            color: Int,
            baseWidth: Float,
            baseAlpha: Int,
            pointWeights: List<Float>?,
        ) {
            // pointWeights carries one entry PER SEGMENT (points.size/2 - 1
            // segments for a polyline of points.size/2 points), not one
            // per point — comparing against the segment count here, not
            // the point count, so the weighted per-segment path below
            // actually gets used instead of always falling through to the
            // flat-width branch.
            val segmentCount = (points.size / 2 - 1).coerceAtLeast(0)
            if (pointWeights == null || pointWeights.size < segmentCount) {
                val paint =
                    Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND
                        this.color = color
                        alpha = baseAlpha
                        strokeWidth = baseWidth
                    }
                canvas.drawPath(buildPath(points), paint)
                return
            }
            // Per-segment width: draw short round-capped segments so the
            // stroke can taper smoothly along its length instead of being
            // one flat-width Path.
            val paint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                    this.color = color
                    alpha = baseAlpha
                }
            var i = 0
            var wi = 0
            while (i + 3 < points.size) {
                val w = (pointWeights.getOrNull(wi) ?: 1f).coerceIn(0.15f, 1.6f)
                paint.strokeWidth = baseWidth * w
                canvas.drawLine(points[i], points[i + 1], points[i + 2], points[i + 3], paint)
                i += 2
                wi += 1
            }
        }

        /** PENCIL: several thin, low-alpha overlapping passes with a tiny
         *  random per-pass offset — graphite doesn't lay down one perfectly
         *  flat line, it's a scatter of grains, and this is the cheapest
         *  real approximation of that on a Canvas API with no texture
         *  brushes. Deterministic-enough seed (path hash) so the same
         *  stroke redrawn (e.g. on undo/redo restore) looks the same. */
        private fun drawPencilStroke(
            canvas: Canvas,
            points: List<Float>,
            color: Int,
            baseWidth: Float,
            baseAlpha: Int,
            pointWeights: List<Float>?,
        ) {
            val random = java.util.Random(points.hashCode().toLong())
            val passes = 3
            val paint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                    this.color = color
                    strokeWidth = (baseWidth * 0.55f).coerceAtLeast(1f)
                }
            repeat(passes) { pass ->
                paint.alpha = (baseAlpha / (passes + 1)).coerceIn(10, 255)
                val jitter = baseWidth * 0.12f
                val jittered =
                    points.mapIndexed { idx, v ->
                        val isX = idx % 2 == 0
                        v + (random.nextFloat() - 0.5f) * jitter * (if (isX) 1f else 1f) + pass * 0f
                    }
                canvas.drawPath(buildPath(jittered), paint)
            }
        }

        /** MARKER: flat/square cap, wide, semi-transparent so overlapping
         *  passes visibly darken where they cross — exactly how a real
         *  alcohol/felt marker behaves, unlike a fully-opaque single pass. */
        private fun drawMarkerStroke(
            canvas: Canvas,
            points: List<Float>,
            color: Int,
            baseWidth: Float,
            baseAlpha: Int,
        ) {
            val paint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.SQUARE
                    strokeJoin = Paint.Join.BEVEL
                    this.color = color
                    alpha = (baseAlpha * 0.72f).toInt().coerceIn(0, 255)
                    strokeWidth = baseWidth * 1.4f
                }
            canvas.drawPath(buildPath(points), paint)
        }

        /** CALLIGRAPHY: simulates a flat chisel nib held at a fixed 45°
         *  angle — real calligraphy pens are thick when the stroke
         *  direction is perpendicular to the nib angle and thin when
         *  travelling parallel to it. Drawn as short per-segment lines
         *  whose width is baseWidth * |cos(segmentAngle - penAngle)|. */
        private fun drawCalligraphyStroke(
            canvas: Canvas,
            points: List<Float>,
            color: Int,
            baseWidth: Float,
            baseAlpha: Int,
        ) {
            val penAngleRad = Math.toRadians(45.0)
            val paint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.SQUARE
                    this.color = color
                    alpha = baseAlpha
                }
            var i = 0
            while (i + 3 < points.size) {
                val dx = points[i + 2] - points[i]
                val dy = points[i + 3] - points[i + 1]
                val segAngle = Math.atan2(dy.toDouble(), dx.toDouble())
                val widthFactor = Math.abs(Math.cos(segAngle - penAngleRad)).coerceIn(0.18, 1.0).toFloat()
                paint.strokeWidth = (baseWidth * (0.3f + widthFactor)).coerceAtLeast(1.5f)
                canvas.drawLine(points[i], points[i + 1], points[i + 2], points[i + 3], paint)
                i += 2
            }
        }

        /** AIRBRUSH / WATERCOLOR / soft eraser share this: a chain of
         *  soft-edged, low-alpha circular dabs (BlurMaskFilter) stamped
         *  along the path and overlapped so density builds up naturally
         *  the slower/more the user goes over the same spot — real
         *  airbrush/wash behavior, not a hard stroke outline. [isClear]
         *  routes the same dab logic through a CLEAR Xfermode for the
         *  soft-eraser brush instead of laying down paint. */
        private fun drawSoftDabStroke(
            canvas: Canvas,
            points: List<Float>,
            color: Int,
            baseWidth: Float,
            baseAlpha: Int,
            blurRadius: Float,
            dabAlpha: Int,
            isClear: Boolean = false,
        ) {
            val radius = (baseWidth / 2f).coerceAtLeast(2f)
            val paint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    this.color = color
                    alpha = ((dabAlpha.toFloat() / 255f) * (baseAlpha.toFloat() / 255f) * 255f).toInt().coerceIn(1, 255)
                    if (blurRadius > 0.5f) {
                        maskFilter = android.graphics.BlurMaskFilter(blurRadius, android.graphics.BlurMaskFilter.Blur.NORMAL)
                    }
                    if (isClear) xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                }
            var i = 0
            var prevX = points[0]
            var prevY = points[1]
            while (i + 1 < points.size) {
                val x = points[i]
                val y = points[i + 1]
                val dist = Math.hypot((x - prevX).toDouble(), (y - prevY).toDouble()).toFloat()
                val steps = (dist / (radius * 0.5f)).toInt().coerceAtLeast(1)
                for (s in 0 until steps) {
                    val t = s.toFloat() / steps
                    canvas.drawCircle(prevX + (x - prevX) * t, prevY + (y - prevY) * t, radius, paint)
                }
                prevX = x
                prevY = y
                i += 2
            }
            canvas.drawCircle(points[points.size - 2], points[points.size - 1], radius, paint)
        }

        /** CHARCOAL: like pencil but coarser — fewer, thicker, more
         *  randomly-offset passes with heavier alpha variance, giving the
         *  broken/grainy edge real charcoal has versus pencil's finer
         *  grain. */
        private fun drawCharcoalStroke(
            canvas: Canvas,
            points: List<Float>,
            color: Int,
            baseWidth: Float,
            baseAlpha: Int,
        ) {
            val random = java.util.Random(points.hashCode().toLong() xor 0x5EED)
            val passes = 4
            val paint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                    this.color = color
                }
            repeat(passes) {
                paint.strokeWidth = baseWidth * (0.6f + random.nextFloat() * 0.7f)
                paint.alpha = (baseAlpha * (0.25f + random.nextFloat() * 0.35f)).toInt().coerceIn(8, 255)
                val jitter = baseWidth * 0.25f
                val jittered = points.map { v -> v + (random.nextFloat() - 0.5f) * jitter }
                canvas.drawPath(buildPath(jittered), paint)
            }
        }

        /** v0.4.30 selection tool: clears every pixel inside the given
         *  rect on one layer to transparent (CLEAR Xfermode, same real-
         *  erase approach as erasePath above) — the "delete selection"
         *  action. */
        fun clearSelectionRegion(
            layerId: String,
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
        ): Boolean {
            val canvas = bitmapStore.getCanvas(layerId) ?: return false
            val paint =
                Paint().apply {
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                }
            canvas.drawRect(left, top, right, bottom, paint)
            return true
        }

        /** v0.4.30 selection tool: moves the pixel content inside the
         *  given rect by (dx, dy) on one layer — extracts the sub-bitmap,
         *  clears the original area, redraws it at the offset position.
         *  Content that would land outside the layer bounds after the
         *  move is naturally clipped by Canvas, matching how Photoshop/
         *  Procreate's move-selection behaves. */
        fun moveSelectionRegion(
            layerId: String,
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
            dx: Float,
            dy: Float,
        ): Boolean {
            val source = bitmapStore.getBitmap(layerId) ?: return false
            val canvas = bitmapStore.getCanvas(layerId) ?: return false
            val safeLeft = left.coerceIn(0f, source.width.toFloat())
            val safeTop = top.coerceIn(0f, source.height.toFloat())
            val safeRight = right.coerceIn(safeLeft, source.width.toFloat())
            val safeBottom = bottom.coerceIn(safeTop, source.height.toFloat())
            val width = (safeRight - safeLeft).toInt()
            val height = (safeBottom - safeTop).toInt()
            if (width <= 0 || height <= 0) return false

            val extracted = Bitmap.createBitmap(source, safeLeft.toInt(), safeTop.toInt(), width, height)
            val clearPaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }
            canvas.drawRect(safeLeft, safeTop, safeRight, safeBottom, clearPaint)
            canvas.drawBitmap(extracted, safeLeft + dx, safeTop + dy, null)
            extracted.recycle()
            return true
        }

        fun drawShape(
            layerId: String,
            shapeType: String,
            x: Float,
            y: Float,
            width: Float?,
            height: Float?,
            fillColorHex: String?,
            strokeColorHex: String?,
            strokeWidthPx: Float?,
            rotationDegrees: Float? = null,
            sides: Int? = null,
        ): Boolean {
            val canvas = bitmapStore.getCanvas(layerId) ?: return false
            val w = width ?: 100f
            val h = height ?: 100f
            val rotation = rotationDegrees ?: 0f
            val pivotX = x + w / 2f
            val pivotY = y + h / 2f

            val saveCount = if (rotation != 0f) canvas.save() else -1
            if (rotation != 0f) canvas.rotate(rotation, pivotX, pivotY)

            if (fillColorHex != null) {
                val fillPaint =
                    Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.FILL
                        color = safeParseColor(fillColorHex, default = Color.TRANSPARENT)
                    }
                drawShapePath(canvas, shapeType, x, y, w, h, fillPaint, sides)
            }

            if (strokeColorHex != null) {
                val strokePaint =
                    Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        color = safeParseColor(strokeColorHex, default = Color.BLACK)
                        strokeWidth = strokeWidthPx ?: 4f
                    }
                drawShapePath(canvas, shapeType, x, y, w, h, strokePaint, sides)
            }

            if (saveCount >= 0) canvas.restoreToCount(saveCount)

            return true
        }

        private fun drawShapePath(
            canvas: Canvas,
            shapeType: String,
            x: Float,
            y: Float,
            w: Float,
            h: Float,
            paint: Paint,
            sides: Int? = null,
        ) {
            when (shapeType.lowercase()) {
                "ellipse" -> canvas.drawOval(x, y, x + w, y + h, paint)
                "line" -> canvas.drawLine(x, y, x + w, y + h, paint)
                "polygon" -> canvas.drawPath(regularPolygonPath(x, y, w, h, (sides ?: 6).coerceIn(3, 20)), paint)
                "star" -> canvas.drawPath(starPath(x, y, w, h, (sides ?: 5).coerceIn(3, 20)), paint)
                else -> canvas.drawRect(x, y, x + w, y + h, paint)
            }
        }

        /** Regular n-sided polygon inscribed in the (x, y, w, h) bounding box,
         *  point-up (first vertex at the top) so a 3-sided polygon reads as
         *  an upward-pointing triangle by default. */
        private fun regularPolygonPath(
            x: Float,
            y: Float,
            w: Float,
            h: Float,
            sides: Int,
        ): Path {
            val cx = x + w / 2f
            val cy = y + h / 2f
            val rx = w / 2f
            val ry = h / 2f
            val path = Path()
            for (i in 0 until sides) {
                val angle = (-Math.PI / 2) + (2 * Math.PI * i / sides)
                val px = cx + rx * Math.cos(angle).toFloat()
                val py = cy + ry * Math.sin(angle).toFloat()
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()
            return path
        }

        /** [points]-pointed star inscribed in the bounding box, alternating
         *  outer tip vertices with inner valley vertices at 50% of the
         *  outer radius — the standard "star polygon" construction. */
        private fun starPath(
            x: Float,
            y: Float,
            w: Float,
            h: Float,
            points: Int,
        ): Path {
            val cx = x + w / 2f
            val cy = y + h / 2f
            val outerRx = w / 2f
            val outerRy = h / 2f
            val innerRx = outerRx * 0.5f
            val innerRy = outerRy * 0.5f
            val path = Path()
            val totalVertices = points * 2
            for (i in 0 until totalVertices) {
                val angle = (-Math.PI / 2) + (Math.PI * i / points)
                val isOuter = i % 2 == 0
                val px = cx + (if (isOuter) outerRx else innerRx) * Math.cos(angle).toFloat()
                val py = cy + (if (isOuter) outerRy else innerRy) * Math.sin(angle).toFloat()
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()
            return path
        }

        fun applyGradient(
            layerId: String,
            gradientType: String,
            startColorHex: String,
            endColorHex: String,
            x: Float?,
            y: Float?,
            width: Float?,
            height: Float?,
            angleDegrees: Float? = null,
            additionalColorStopsHex: List<String>? = null,
        ): Boolean {
            val canvas = bitmapStore.getCanvas(layerId) ?: return false
            val bitmap = bitmapStore.getBitmap(layerId) ?: return false

            val left = x ?: 0f
            val top = y ?: 0f
            val w = width ?: bitmap.width.toFloat()
            val h = height ?: bitmap.height.toFloat()

            val colors =
                buildList {
                    add(safeParseColor(startColorHex, default = Color.BLACK))
                    additionalColorStopsHex?.forEach { add(safeParseColor(it, default = Color.GRAY)) }
                    add(safeParseColor(endColorHex, default = Color.WHITE))
                }.toIntArray()

            val shader =
                if (gradientType.lowercase() == "radial") {
                    RadialGradient(
                        left + w / 2f,
                        top + h / 2f,
                        max(w, h) / 2f,
                        colors,
                        null,
                        Shader.TileMode.CLAMP,
                    )
                } else {
                    // angleDegrees rotates the gradient axis around the region's
                    // center: 0° is left-to-right, 90° is top-to-bottom, matching
                    // the conventional CSS/design-tool gradient-angle mental
                    // model rather than the previous fixed corner-to-corner line.
                    val angle = angleDegrees ?: 90f
                    val cx = left + w / 2f
                    val cy = top + h / 2f
                    val radius = max(w, h) / 2f
                    val radians = Math.toRadians(angle.toDouble())
                    val dx = (Math.cos(radians) * radius).toFloat()
                    val dy = (Math.sin(radians) * radius).toFloat()
                    LinearGradient(cx - dx, cy - dy, cx + dx, cy + dy, colors, null, Shader.TileMode.CLAMP)
                }

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader }
            canvas.drawRect(left, top, left + w, top + h, paint)
            return true
        }

        fun fillRegion(
            layerId: String,
            x: Float,
            y: Float,
            colorHex: String,
            tolerance: Float?,
        ): Boolean {
            val bitmap = bitmapStore.getBitmap(layerId) ?: return false
            val canvas = bitmapStore.getCanvas(layerId) ?: return false
            val startX = x.toInt().coerceIn(0, bitmap.width - 1)
            val startY = y.toInt().coerceIn(0, bitmap.height - 1)

            val targetColor = bitmap[startX, startY]
            val fillColor = safeParseColor(colorHex, default = Color.BLACK)
            val tol = (tolerance ?: 32f).coerceIn(0f, 255f)

            if (targetColor == fillColor) return true

            floodFillScanline(bitmap, canvas, startX, startY, targetColor, fillColor, tol)
            return true
        }

        /**
         * Scanline flood fill — far cheaper than naive 4-directional
         * recursive fill on a mobile CPU/heap (Section 171's budget-device
         * target), avoids StackOverflow risk entirely since it's iterative.
         */
        private fun floodFillScanline(
            bitmap: Bitmap,
            canvas: Canvas,
            startX: Int,
            startY: Int,
            targetColor: Int,
            fillColor: Int,
            tolerance: Float,
        ) {
            val width = bitmap.width
            val height = bitmap.height
            val stack = ArrayDeque<Pair<Int, Int>>()
            stack.addLast(startX to startY)
            val visited = HashSet<Long>()

            fun key(
                px: Int,
                py: Int,
            ) = px.toLong() * height + py

            fun colorsMatch(
                c1: Int,
                c2: Int,
            ): Boolean {
                val dr = ((c1 shr 16 and 0xFF) - (c2 shr 16 and 0xFF))
                val dg = ((c1 shr 8 and 0xFF) - (c2 shr 8 and 0xFF))
                val db = ((c1 and 0xFF) - (c2 and 0xFF))
                return (dr * dr + dg * dg + db * db) <= tolerance * tolerance
            }

            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            while (stack.isNotEmpty()) {
                val (px, py) = stack.removeLast()
                if (px < 0 || px >= width || py < 0 || py >= height) continue
                val k = key(px, py)
                if (k in visited) continue
                if (!colorsMatch(pixels[py * width + px], targetColor)) continue

                visited.add(k)
                pixels[py * width + px] = fillColor

                stack.addLast((px + 1) to py)
                stack.addLast((px - 1) to py)
                stack.addLast(px to (py + 1))
                stack.addLast(px to (py - 1))

                if (visited.size > MAX_FILL_PIXELS) break
            }

            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        }

        fun clearLayer(layerId: String) {
            bitmapStore.clearLayer(layerId)
        }

        /** v0.4.30: solid full-canvas fill for one layer — set_canvas_
         *  background's backing. Deliberately separate from fillRegion
         *  (bucket/flood fill from a tap point, contiguous-color region
         *  only) since a background fill needs to cover the WHOLE layer
         *  unconditionally regardless of what's currently on it. */
        fun fillWholeLayer(
            layerId: String,
            colorHex: String,
            widthPx: Int,
            heightPx: Int,
        ): Boolean {
            val canvas = bitmapStore.getCanvas(layerId) ?: return false
            canvas.drawColor(safeParseColor(colorHex, default = Color.WHITE))
            return true
        }

        /** Section pick_color tool: real pixel sampling on the flattened
         *  composite (what the model actually "sees"), not a single layer,
         *  since that's what its vision-feedback snapshot showed it. */
        fun pickColor(
            compositedBitmap: Bitmap,
            x: Float,
            y: Float,
        ): String {
            val px = x.toInt().coerceIn(0, compositedBitmap.width - 1)
            val py = y.toInt().coerceIn(0, compositedBitmap.height - 1)
            val color = compositedBitmap[px, py]
            return String.format("#%06X", 0xFFFFFF and color)
        }

        fun applyFilter(
            layerId: String,
            filterType: String,
            intensity: Float?,
        ): Boolean {
            val bitmap = bitmapStore.getBitmap(layerId) ?: return false
            val canvas = bitmapStore.getCanvas(layerId) ?: return false
            val amount = intensity ?: 1f

            val colorMatrix =
                when (filterType.lowercase()) {
                    "grayscale" -> android.graphics.ColorMatrix().apply { setSaturation(0f) }
                    "invert" ->
                        android.graphics.ColorMatrix(
                            floatArrayOf(
                                -1f,
                                0f,
                                0f,
                                0f,
                                255f,
                                0f,
                                -1f,
                                0f,
                                0f,
                                255f,
                                0f,
                                0f,
                                -1f,
                                0f,
                                255f,
                                0f,
                                0f,
                                0f,
                                1f,
                                0f,
                            ),
                        )
                    "saturation" -> android.graphics.ColorMatrix().apply { setSaturation(amount) }
                    "brightness" ->
                        android.graphics.ColorMatrix().apply {
                            val b = (amount - 1f) * 255f
                            set(
                                floatArrayOf(
                                    1f,
                                    0f,
                                    0f,
                                    0f,
                                    b,
                                    0f,
                                    1f,
                                    0f,
                                    0f,
                                    b,
                                    0f,
                                    0f,
                                    1f,
                                    0f,
                                    b,
                                    0f,
                                    0f,
                                    0f,
                                    1f,
                                    0f,
                                ),
                            )
                        }
                    "contrast" ->
                        android.graphics.ColorMatrix().apply {
                            val scale = amount
                            val translate = (1f - scale) * 128f
                            set(
                                floatArrayOf(
                                    scale,
                                    0f,
                                    0f,
                                    0f,
                                    translate,
                                    0f,
                                    scale,
                                    0f,
                                    0f,
                                    translate,
                                    0f,
                                    0f,
                                    scale,
                                    0f,
                                    translate,
                                    0f,
                                    0f,
                                    0f,
                                    1f,
                                    0f,
                                ),
                            )
                        }
                    else -> null
                }

            return when (filterType.lowercase()) {
                "blur" -> applyStackBlur(bitmap, canvas, radius = (amount * 8f).toInt().coerceIn(1, 25))
                "sharpen" -> applySharpen(bitmap, canvas)
                else -> {
                    if (colorMatrix == null) return false
                    val paint = Paint().apply { colorFilter = android.graphics.ColorMatrixColorFilter(colorMatrix) }
                    val copy = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                    canvas.drawBitmap(copy, 0f, 0f, paint)
                    copy.recycle()
                    true
                }
            }
        }

        /** Lightweight box-blur approximation — a true stack/gaussian blur
         *  is heavier than a budget device (Section 171) should pay for on
         *  every filter call, so this uses a simple multi-pass box blur
         *  which looks close enough at typical canvas viewing sizes. */
        private fun applyStackBlur(
            bitmap: Bitmap,
            canvas: Canvas,
            radius: Int,
        ): Boolean {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            repeat(2) {
                boxBlurPass(pixels, width, height, radius)
            }

            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            return true
        }

        private fun boxBlurPass(
            pixels: IntArray,
            width: Int,
            height: Int,
            radius: Int,
        ) {
            val output = pixels.copyOf()
            for (y in 0 until height) {
                for (x in 0 until width) {
                    var r = 0
                    var g = 0
                    var b = 0
                    var a = 0
                    var count = 0
                    for (dx in -radius..radius step max(1, radius / 4)) {
                        val nx = (x + dx).coerceIn(0, width - 1)
                        val pixel = pixels[y * width + nx]
                        a += (pixel shr 24) and 0xFF
                        r += (pixel shr 16) and 0xFF
                        g += (pixel shr 8) and 0xFF
                        b += pixel and 0xFF
                        count++
                    }
                    output[y * width + x] = ((a / count) shl 24) or ((r / count) shl 16) or ((g / count) shl 8) or (b / count)
                }
            }
            System.arraycopy(output, 0, pixels, 0, pixels.size)
        }

        private fun applySharpen(
            bitmap: Bitmap,
            canvas: Canvas,
        ): Boolean {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            val output = pixels.copyOf()

            for (y in 1 until height - 1) {
                for (x in 1 until width - 1) {
                    fun channel(shift: Int): Int {
                        val center = (pixels[y * width + x] shr shift) and 0xFF
                        val up = (pixels[(y - 1) * width + x] shr shift) and 0xFF
                        val down = (pixels[(y + 1) * width + x] shr shift) and 0xFF
                        val left = (pixels[y * width + x - 1] shr shift) and 0xFF
                        val right = (pixels[y * width + x + 1] shr shift) and 0xFF
                        return (center * 5 - up - down - left - right).coerceIn(0, 255)
                    }
                    val a = (pixels[y * width + x] shr 24) and 0xFF
                    output[y * width + x] = (a shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
                }
            }
            bitmap.setPixels(output, 0, width, 0, 0, width, height)
            return true
        }

        fun addText(
            layerId: String,
            text: String,
            x: Float,
            y: Float,
            fontSizePx: Float?,
            colorHex: String?,
            bold: Boolean?,
        ): Boolean {
            val canvas = bitmapStore.getCanvas(layerId) ?: return false
            val paint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = safeParseColor(colorHex, default = Color.BLACK)
                    textSize = fontSizePx ?: 32f
                    isFakeBoldText = bold ?: false
                    typeface = android.graphics.Typeface.DEFAULT
                }
            canvas.drawText(text, x, y, paint)
            return true
        }

        fun createMask(
            layerId: String,
            maskShape: String,
            x: Float,
            y: Float,
            width: Float,
            height: Float,
            invert: Boolean?,
        ): Boolean {
            val bitmap = bitmapStore.getBitmap(layerId) ?: return false
            val canvas = bitmapStore.getCanvas(layerId) ?: return false
            val shouldInvert = invert ?: false

            val maskBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ALPHA_8)
            val maskCanvas = Canvas(maskBitmap)
            val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }

            if (shouldInvert) {
                maskCanvas.drawRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(), maskPaint)
                maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            }

            if (maskShape.lowercase() == "ellipse") {
                maskCanvas.drawOval(x, y, x + width, y + height, maskPaint)
            } else {
                maskCanvas.drawRect(x, y, x + width, y + height, maskPaint)
            }

            val original = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            val srcInPaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) }
            canvas.drawBitmap(original, 0f, 0f, null)
            canvas.drawBitmap(maskBitmap, 0f, 0f, srcInPaint)

            original.recycle()
            maskBitmap.recycle()
            return true
        }

        fun applyPattern(
            layerId: String,
            patternType: String,
            x: Float,
            y: Float,
            width: Float,
            height: Float,
            colorHex: String,
            scalePx: Float?,
        ): Boolean {
            val canvas = bitmapStore.getCanvas(layerId) ?: return false
            val scale = (scalePx ?: 16f).coerceAtLeast(4f)
            val paint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = safeParseColor(colorHex, default = Color.BLACK)
                    style = Paint.Style.FILL
                }
            val strokePaint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = safeParseColor(colorHex, default = Color.BLACK)
                    style = Paint.Style.STROKE
                    strokeWidth = scale / 8f
                }

            canvas.save()
            canvas.clipRect(x, y, x + width, y + height)

            when (patternType.lowercase()) {
                "dots" -> {
                    var py = y
                    while (py < y + height) {
                        var px = x
                        while (px < x + width) {
                            canvas.drawCircle(px, py, scale / 4f, paint)
                            px += scale
                        }
                        py += scale
                    }
                }
                "stripes" -> {
                    var px = x
                    while (px < x + width) {
                        canvas.drawRect(px, y, px + scale / 2f, y + height, paint)
                        px += scale
                    }
                }
                "checkerboard" -> {
                    var py = y
                    var row = 0
                    while (py < y + height) {
                        var px = x
                        var col = 0
                        while (px < x + width) {
                            if ((row + col) % 2 == 0) canvas.drawRect(px, py, px + scale, py + scale, paint)
                            px += scale
                            col++
                        }
                        py += scale
                        row++
                    }
                }
                "crosshatch" -> {
                    var px = x
                    while (px < x + width) {
                        canvas.drawLine(px, y, px, y + height, strokePaint)
                        px += scale
                    }
                    var py = y
                    while (py < y + height) {
                        canvas.drawLine(x, py, x + width, py, strokePaint)
                        py += scale
                    }
                }
            }

            canvas.restore()
            return true
        }

        fun drawCurve(
            layerId: String,
            startX: Float,
            startY: Float,
            controlX: Float,
            controlY: Float,
            endX: Float,
            endY: Float,
            colorHex: String?,
            strokeWidthPx: Float?,
        ): Boolean {
            val canvas = bitmapStore.getCanvas(layerId) ?: return false
            val path =
                Path().apply {
                    moveTo(startX, startY)
                    quadTo(controlX, controlY, endX, endY)
                }
            val paint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                    color = safeParseColor(colorHex, default = Color.BLACK)
                    strokeWidth = strokeWidthPx ?: 6f
                }
            canvas.drawPath(path, paint)
            return true
        }

        fun importImageLayer(
            layerId: String,
            sourceBitmap: Bitmap,
            targetWidth: Int,
            targetHeight: Int,
            opacity: Float?,
        ): Boolean {
            val canvas = bitmapStore.getCanvas(layerId) ?: return false
            val scaled = Bitmap.createScaledBitmap(sourceBitmap, targetWidth, targetHeight, true)
            val paint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    alpha = ((opacity ?: 1f).coerceIn(0f, 1f) * 255).toInt()
                }
            canvas.drawBitmap(scaled, 0f, 0f, paint)
            if (scaled !== sourceBitmap) scaled.recycle()
            return true
        }

        /** Composites all visible layers, bottom to top, respecting opacity
         *  and blend mode, into a single flattened Bitmap — this is what
         *  gets shown on screen and what gets base64-encoded for the
         *  agent's vision-feedback snapshot (Section 156).
         *
         *  Blend-mode note: Android's android.graphics.PorterDuff.Mode is
         *  a much smaller set than Photoshop-style layer blend modes —
         *  it has no COLOR_DODGE, COLOR_BURN, or SUBTRACT equivalent.
         *  Previously those three (plus OVERLAY, which DOES exist in
         *  PorterDuff.Mode but wasn't wired) silently fell through to
         *  `null` in blendModeToPorterDuff and rendered as plain Normal —
         *  a real, user-visible bug: selecting "Color Burn" from the
         *  layer panel produced no visible difference at all, which
         *  reads as "the app is broken" even though most of the blend
         *  system works. OVERLAY is now wired to its real PorterDuff
         *  equivalent; COLOR_DODGE/COLOR_BURN/SUBTRACT are computed with
         *  manual per-pixel math in blendManually since Android has no
         *  native Xfermode for them. */
        fun compositeVisibleLayers(
            layers: List<CanvasLayer>,
            widthPx: Int,
            heightPx: Int,
        ): Bitmap {
            val output = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            canvas.drawColor(Color.WHITE)
            val ordered = layers.sortedBy { it.orderIndex }

            ordered.forEachIndexed { index, layer ->
                if (!layer.isVisible) return@forEachIndexed
                val rawBitmap = bitmapStore.getBitmap(layer.id) ?: return@forEachIndexed
                val below = ordered.take(index).asReversed().firstOrNull { it.isVisible && bitmapStore.getBitmap(it.id) != null }
                val layerBitmap = if (layer.clipToBelow && below != null) {
                    createAlphaClippedBitmap(rawBitmap, bitmapStore.getBitmap(below.id)!!)
                } else rawBitmap

                if (requiresManualBlend(layer.blendMode)) {
                    blendManually(output, layerBitmap, layer.blendMode, layer.opacity.coerceIn(0f, 1f))
                } else {
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        alpha = (layer.opacity.coerceIn(0f, 1f) * 255).toInt()
                        xfermode = blendModeToPorterDuff(layer.blendMode)
                    }
                    canvas.drawBitmap(layerBitmap, 0f, 0f, paint)
                }
                if (layerBitmap !== rawBitmap) layerBitmap.recycle()
            }
            return output
        }

        private fun createAlphaClippedBitmap(source: Bitmap, mask: Bitmap): Bitmap {
            val clipped = source.copy(Bitmap.Config.ARGB_8888, true)
            val maskCopy = if (mask.config == Bitmap.Config.ARGB_8888) mask else mask.copy(Bitmap.Config.ARGB_8888, false)
            val c = Canvas(clipped)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) }
            c.drawBitmap(maskCopy, 0f, 0f, paint)
            paint.xfermode = null
            if (maskCopy !== mask) maskCopy.recycle()
            return clipped
        }

        private fun requiresManualBlend(mode: LayerBlendMode): Boolean =
            mode == LayerBlendMode.COLOR_DODGE || mode == LayerBlendMode.COLOR_BURN || mode == LayerBlendMode.SUBTRACT

        /** Per-pixel blend for the three modes Android's PorterDuff has no
         *  native equivalent for. Reads both bitmaps' pixel arrays,
         *  applies the standard Photoshop-style blend formula for the
         *  given mode at the layer's opacity, and writes the result
         *  directly into [base]'s pixel array in place.
         *
         *  Formulas (per channel, base/blend in 0..1 range):
         *   - Color Dodge: base / (1 - blend), clamped to 1 when blend = 1
         *   - Color Burn:  1 - (1 - base) / blend, clamped to 0 when blend = 0
         *   - Subtract:    base - blend, clamped to 0
         *  These match the standard digital-compositing definitions used
         *  by Photoshop/GIMP/Krita, so a layer authored with this blend
         *  mode composites the same way ArtificerX users would expect
         *  from any other paint tool. */
        private fun blendManually(
            base: Bitmap,
            overlay: Bitmap,
            mode: LayerBlendMode,
            opacity: Float,
        ) {
            val width = base.width
            val height = base.height
            val basePixels = IntArray(width * height)
            val overlayPixels = IntArray(width * height)
            base.getPixels(basePixels, 0, width, 0, 0, width, height)
            overlay.getPixels(overlayPixels, 0, width, 0, 0, width, height)

            fun blendChannel(
                baseValue: Int,
                overlayValue: Int,
            ): Int {
                val b = baseValue / 255f
                val o = overlayValue / 255f
                val blended =
                    when (mode) {
                        LayerBlendMode.COLOR_DODGE -> if (o >= 1f) 1f else (b / (1f - o)).coerceAtMost(1f)
                        LayerBlendMode.COLOR_BURN -> if (o <= 0f) 0f else (1f - (1f - b) / o).coerceAtLeast(0f)
                        LayerBlendMode.SUBTRACT -> (b - o).coerceAtLeast(0f)
                        else -> b
                    }
                return (blended.coerceIn(0f, 1f) * 255).toInt()
            }

            for (i in basePixels.indices) {
                val basePixel = basePixels[i]
                val overlayPixel = overlayPixels[i]
                val overlayAlpha = ((overlayPixel shr 24) and 0xFF) / 255f * opacity
                if (overlayAlpha <= 0f) continue

                val baseA = (basePixel shr 24) and 0xFF
                val baseR = (basePixel shr 16) and 0xFF
                val baseG = (basePixel shr 8) and 0xFF
                val baseB = basePixel and 0xFF

                val overlayR = (overlayPixel shr 16) and 0xFF
                val overlayG = (overlayPixel shr 8) and 0xFF
                val overlayB = overlayPixel and 0xFF

                val blendedR = blendChannel(baseR, overlayR)
                val blendedG = blendChannel(baseG, overlayG)
                val blendedB = blendChannel(baseB, overlayB)

                // Standard alpha-weighted mix between the base pixel and
                // the fully-blended result, using the overlay's own alpha
                // (times layer opacity) as the mix factor — so a
                // semi-transparent stroke on the blended layer partially
                // blends rather than being all-or-nothing.
                val outR = (baseR * (1 - overlayAlpha) + blendedR * overlayAlpha).toInt().coerceIn(0, 255)
                val outG = (baseG * (1 - overlayAlpha) + blendedG * overlayAlpha).toInt().coerceIn(0, 255)
                val outB = (baseB * (1 - overlayAlpha) + blendedB * overlayAlpha).toInt().coerceIn(0, 255)
                val outA = max(baseA, (overlayAlpha * 255).toInt())

                basePixels[i] = (outA shl 24) or (outR shl 16) or (outG shl 8) or outB
            }

            base.setPixels(basePixels, 0, width, 0, 0, width, height)
        }

        private fun blendModeToPorterDuff(mode: LayerBlendMode): PorterDuffXfermode? =
            when (mode) {
                LayerBlendMode.MULTIPLY -> PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
                LayerBlendMode.SCREEN -> PorterDuffXfermode(PorterDuff.Mode.SCREEN)
                LayerBlendMode.OVERLAY -> PorterDuffXfermode(PorterDuff.Mode.OVERLAY)
                LayerBlendMode.DARKEN -> PorterDuffXfermode(PorterDuff.Mode.DARKEN)
                LayerBlendMode.LIGHTEN -> PorterDuffXfermode(PorterDuff.Mode.LIGHTEN)
                LayerBlendMode.ADD -> PorterDuffXfermode(PorterDuff.Mode.ADD)
                else -> null
            }

        private fun safeParseColor(
            hex: String?,
            default: Int,
        ): Int {
            if (hex.isNullOrBlank()) return default
            return runCatching { Color.parseColor(hex) }.getOrDefault(default)
        }

        companion object {
            private const val MAX_FILL_PIXELS = 2_000_000
        }
    }
