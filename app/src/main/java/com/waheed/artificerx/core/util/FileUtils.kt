package com.waheed.artificerx.core.util

import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.io.FileOutputStream

class FileUtils {
    fun copyFile(sourcePath: String, destPath: String): Boolean {
        return try {
            val sourceFile = File(sourcePath)
            val destFile = File(destPath)
            sourceFile.copyTo(destFile, overwrite = true)
            true
        } catch (e: IOException) {
            DebugLogger.e("Copy failed from $sourcePath to $destPath", e)
            false
        }
    }

    fun readFileContent(filePath: String): String? {
        val file = File(filePath)
        return if (file.exists()) {
            if (file.length() > MAX_READ_FILE_BYTES) {
                DebugLogger.e("File too large to read safely: $filePath")
                null
            } else {
                file.readText(Charsets.UTF_8)
            }
        } else {
            DebugLogger.e("File not found: $filePath")
            null
        }
    }

    fun writeFileContent(filePath: String, content: String, append: Boolean = false): Boolean {
        return try {
            val file = File(filePath)
            if (!file.parentFile.exists()) {
                file.parentFile.mkdirs()
            }
            if (append) file.appendText(content, Charsets.UTF_8) else file.writeText(content, Charsets.UTF_8)
            true
        } catch (e: IOException) {
            DebugLogger.e("Write failed: $filePath", e)
            false
        }
    }

    fun checkFileExists(filePath: String): Boolean {
        val file = File(filePath)
        if (!file.exists()) {
            DebugLogger.e("File does not exist: $filePath")
        }
        return file.exists()
    }

    fun getFileSize(filePath: String): Long {
        val file = File(filePath)
        if (!checkFileExists(filePath)) return 0L
        return file.length()
    }

    fun calculateMD5(filePath: String): String? {
        val file = File(filePath)
        if (!checkFileExists(filePath)) return null
        return try {
            val digest = MessageDigest.getInstance("MD5")
            file.inputStream().use { input ->
                val buffer = ByteArray(16 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    digest.update(buffer, 0, read)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            DebugLogger.e("MD5 failed: $filePath", e)
            null
        }
    }

    fun mergeFiles(sourcePaths: List<String>, destPath: String): Boolean {
        if (sourcePaths.size > MAX_MERGE_SOURCES) return false
        val destFile = File(destPath)
        if (!destFile.parentFile.exists()) destFile.parentFile.mkdirs()
        destFile.writeText("", Charsets.UTF_8)

        var totalBytes = 0L
        for (source in sourcePaths) {
            val sourceFile = File(source)
            if (!sourceFile.isFile) continue
            if (totalBytes + sourceFile.length() > MAX_MERGE_OUTPUT_BYTES) return false
            val content = readFileContent(source) ?: continue
            if (!writeFileContent(destPath, content, append = true)) return false
            totalBytes += content.toByteArray(Charsets.UTF_8).size
        }
        return destFile.exists() && destFile.length() > 0 && destFile.length() <= MAX_MERGE_OUTPUT_BYTES
    }
}