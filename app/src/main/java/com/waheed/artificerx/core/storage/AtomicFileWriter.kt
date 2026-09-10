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

    fun read(target: File): ByteArray = FileInputStream(target).use { it.readBytes() }
}
