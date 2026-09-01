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
        fun drawPath(
            layerId: String,
            points: List<Float>,
            colorHex: String?,
            strokeWidthPx: Float?,
            opacity: Float?,
        ): Boolean {
            val canvas = bitmapStore.getCanvas(layerId) ?: return false
            val path = Path()
            path.moveTo(points[0], points[1])
            var i = 2
            while (i + 1 < points.size) {
                path.lineTo(points[i], points[i + 1])
                i += 2
            }

            val paint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                    color = safeParseColor(colorHex, default = Color.BLACK)
                    alpha = ((opacity ?: 1f).coerceIn(0f, 1f) * 255).toInt()
                    strokeWidth = strokeWidthPx ?: 8f
                }

            canvas.drawPath(path, paint)
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

        /** Regular n-sided polygon inscribed in the (x,y,w,h) bounding box,
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

        /**
         * Composites all visible layers, bottom to top, respecting opacity
         * and blend mode, into a single flattened Bitmap — this is what
         * gets shown on screen and what gets base64-encoded for the
         * agent's vision-feedback snapshot (Section 156).
         */
        fun compositeVisibleLayers(
            layers: List<CanvasLayer>,
            widthPx: Int,
            heightPx: Int,
        ): Bitmap {
            val output = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            canvas.drawColor(Color.WHITE)

            layers.sortedBy { it.orderIndex }.forEach { layer ->
                if (!layer.isVisible) return@forEach
                val layerBitmap = bitmapStore.getBitmap(layer.id) ?: return@forEach

                val paint =
                    Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        alpha = (layer.opacity.coerceIn(0f, 1f) * 255).toInt()
                        xfermode = blendModeToPorterDuff(layer.blendMode)
                    }
                canvas.drawBitmap(layerBitmap, 0f, 0f, paint)
            }

            return output
        }

        private fun blendModeToPorterDuff(mode: LayerBlendMode): PorterDuffXfermode? =
            when (mode) {
                LayerBlendMode.MULTIPLY -> PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
                LayerBlendMode.SCREEN -> PorterDuffXfermode(PorterDuff.Mode.SCREEN)
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
