package com.waheed.artificerx.core.importer

enum class ImportKind { IMAGE, VIDEO, AUDIO, PROJECT, ARCHIVE, DOCUMENT, UNKNOWN }

data class ImportDecision(val kind: ImportKind, val accepted: Boolean, val reason: String)

class ImportPolicy {
    private val extensions = mapOf(
        "png" to ImportKind.IMAGE, "jpg" to ImportKind.IMAGE, "jpeg" to ImportKind.IMAGE,
        "webp" to ImportKind.IMAGE, "gif" to ImportKind.IMAGE, "bmp" to ImportKind.IMAGE,
        "mp4" to ImportKind.VIDEO, "webm" to ImportKind.VIDEO, "mkv" to ImportKind.VIDEO,
        "mp3" to ImportKind.AUDIO, "wav" to ImportKind.AUDIO, "m4a" to ImportKind.AUDIO,
        "artx" to ImportKind.PROJECT, "psd" to ImportKind.PROJECT, "kra" to ImportKind.PROJECT,
        "zip" to ImportKind.ARCHIVE, "pdf" to ImportKind.DOCUMENT, "svg" to ImportKind.IMAGE
    )

    fun decide(name: String, sizeBytes: Long, maxBytes: Long = 2L * 1024 * 1024 * 1024): ImportDecision {
        if (sizeBytes <= 0) return ImportDecision(ImportKind.UNKNOWN, false, "File is empty")
        if (sizeBytes > maxBytes) return ImportDecision(ImportKind.UNKNOWN, false, "File exceeds import size limit")
        val ext = name.substringAfterLast('.', "").lowercase()
        val kind = extensions[ext] ?: ImportKind.UNKNOWN
        return ImportDecision(kind, kind != ImportKind.UNKNOWN, if (kind == ImportKind.UNKNOWN) "Unsupported extension" else "Accepted")
    }
}
