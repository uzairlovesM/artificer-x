package com.waheed.artificerx.core.export

enum class ExportFormat { PNG, JPEG, WEBP, TIFF, PSD, SVG, PDF, ZIP }

data class ExportOptions(
    val format: ExportFormat,
    val quality: Int = 95,
    val includeHiddenLayers: Boolean = false,
    val includeMetadata: Boolean = true
) {
    init { require(quality in 1..100) }
}

class ExportPlanner {
    fun estimateBytes(width: Int, height: Int, options: ExportOptions): Long {
        require(width > 0 && height > 0)
        val raw = width.toLong() * height.toLong() * 4L
        val factor = when (options.format) {
            ExportFormat.PNG, ExportFormat.TIFF, ExportFormat.PSD -> 0.65
            ExportFormat.JPEG -> 0.12 + options.quality / 1000.0
            ExportFormat.WEBP -> 0.10 + options.quality / 1200.0
            ExportFormat.SVG -> 0.02
            ExportFormat.PDF, ExportFormat.ZIP -> 0.75
        }
        return (raw * factor).toLong().coerceAtLeast(1024L)
    }
}
