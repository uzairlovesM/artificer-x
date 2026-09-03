package com.waheed.artificerx.core.insights

import java.io.File

object ArtifactValidator {
    data class Result(val exists: Boolean, val readable: Boolean, val sizeBytes: Long, val reason: String)
    fun validate(path: String): Result {
        val file = File(path)
        return when {
            !file.exists() -> Result(false, false, 0L, "Artifact file no longer exists")
            !file.isFile -> Result(false, false, 0L, "Artifact path is not a file")
            !file.canRead() -> Result(true, false, file.length(), "Artifact exists but is not readable")
            else -> Result(true, true, file.length(), "OK")
        }
    }
}
