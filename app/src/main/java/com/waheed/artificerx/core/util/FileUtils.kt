package com.waheed.artificerx.core.util

import java.io.File
import java.io.IOException
import java.security.MessageDigest

class FileUtils {
    fun copyFile(sourcePath: String, destPath: String): Boolean {
        return try {
            val sourceFile = File(sourcePath)
            val destFile = File(destPath)
            sourceFile.copyTo(destFile, overwrite = true)
            true
        } catch (e: IOException) {
            DebugLogger.e("FileUtils", "Copy failed from $sourcePath to $destPath", e)
            false
        }
    }

    fun readFileContent(filePath: String): String? {
        val file = File(filePath)
        return if (file.exists()) {
            file.readText(Charsets.UTF_8)
        } else {
            DebugLogger.e("FileUtils", "File not found: $filePath")
            null
        }
    }

    fun writeFileContent(filePath: String, content: String, append: Boolean = false): Boolean {
        return try {
            val file = File(filePath)
            if (!file.parentFile.exists()) {
                file.parentFile.mkdirs()
            }
            file.writeText(content, Charsets.UTF_8, append)
            true
        } catch (e: IOException) {
            DebugLogger.e("FileUtils", "Write failed: $filePath", e)
            false
        }
    }

    fun checkFileExists(filePath: String): Boolean {
        val file = File(filePath)
        if (!file.exists()) {
            DebugLogger.w("FileUtils", "File does not exist: $filePath")
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
            DebugLogger.e("FileUtils", "MD5 failed: $filePath", e)
            null
        }
    }

    fun mergeFiles(sourcePaths: List<String>, destPath: String): Boolean {
        val destFile = File(destPath)
        if (!destFile.parentFile.exists()) destFile.parentFile.mkdirs()
        destFile.writeText("", Charsets.UTF_8)

        for (source in sourcePaths) {
            val content = readFileContent(source)
            if (content != null) {
                writeFileContent(destPath, content, append = true)
            }
        }
        return destFile.exists() && destFile.length() > 0
    }
}