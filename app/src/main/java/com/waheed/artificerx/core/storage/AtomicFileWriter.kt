package com.waheed.artificerx.core.storage

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class AtomicFileWriter {
    fun write(target: File, bytes: ByteArray) {
        require(target.parentFile?.exists() == true) { "Parent directory does not exist" }
        val temp = File(target.parentFile, ".${target.name}.${System.nanoTime()}.tmp")
        try {
            FileOutputStream(temp).use { out ->
                out.write(bytes)
                out.fd.sync()
            }
            if (target.exists() && !target.delete()) error("Unable to replace existing file")
            if (!temp.renameTo(target)) error("Unable to atomically move temporary file")
        } finally {
            if (temp.exists()) temp.delete()
        }
    }

    fun read(target: File, maxBytes: Long = DEFAULT_MAX_READ_BYTES): ByteArray {
        require(target.isFile) { "File not found: ${target.path}" }
        require(target.length() <= maxBytes) { "File exceeds the safe read limit of $maxBytes bytes" }
        return FileInputStream(target).use { it.readBytes() }
    }

    private companion object {
        const val DEFAULT_MAX_READ_BYTES = 100L * 1024L * 1024L
    }
}
