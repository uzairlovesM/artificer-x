package com.waheed.artificerx.core.terminal

import android.content.Context
import com.waheed.artificerx.core.security.SecurityPolicy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class TerminalResult(
    val command: String,
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val durationMs: Long,
    val timedOut: Boolean = false,
    val outputTruncated: Boolean = false,
)

@Singleton
class TerminalSandbox @Inject constructor(@ApplicationContext context: Context) {
    val root = File(context.filesDir, "sandbox/terminal").apply { mkdirs() }

    suspend fun run(command: String, timeoutSeconds: Long = 20): TerminalResult = withContext(Dispatchers.IO) {
        val started = System.currentTimeMillis()
        if (!SecurityPolicy.isShellAllowed(command)) {
            return@withContext TerminalResult(command, 126, "", "Command rejected by Artificer-X sandbox policy.", 0L)
        }

        val safeTimeout = timeoutSeconds.coerceIn(1, 120)
        val process = runCatching {
            ProcessBuilder("/system/bin/sh", "-c", command)
                .directory(root)
                .redirectErrorStream(false)
                .apply {
                    environment().clear()
                    environment()["HOME"] = root.absolutePath
                    environment()["PWD"] = root.absolutePath
                    environment()["TMPDIR"] = File(root, "tmp").apply { mkdirs() }.absolutePath
                    environment()["PATH"] = "/system/bin:/system/xbin:/apex/com.android.runtime/bin"
                    environment()["LANG"] = "C.UTF-8"
                }
                .start()
        }.getOrElse { error ->
            return@withContext TerminalResult(command, 127, "", error.message ?: "Could not start sandbox process.", System.currentTimeMillis() - started)
        }

        try {
            coroutineScope {
                val stdout = async(Dispatchers.IO) { readBounded(process.inputStream, MAX_OUTPUT_BYTES) }
                val stderr = async(Dispatchers.IO) { readBounded(process.errorStream, MAX_OUTPUT_BYTES) }
                val finished = process.waitFor(safeTimeout, TimeUnit.SECONDS)
                if (!finished) process.destroyForcibly()
                val out = stdout.await()
                val err = stderr.await()
                TerminalResult(
                    command = command,
                    exitCode = if (finished) process.exitValue() else 124,
                    stdout = out.text,
                    stderr = err.text,
                    durationMs = System.currentTimeMillis() - started,
                    timedOut = !finished,
                    outputTruncated = out.truncated || err.truncated,
                )
            }
        } finally {
            process.inputStream.close()
            process.errorStream.close()
            process.outputStream.close()
        }
    }

    suspend fun runBatch(commands: List<String>, timeoutSecondsEach: Long = 20): List<TerminalResult> =
        commands.asSequence().filter(String::isNotBlank).take(MAX_BATCH_COMMANDS).map { run(it, timeoutSecondsEach) }.toList()

    private fun readBounded(input: InputStream, maxBytes: Int): ReadResult {
        input.use { stream ->
            val buffer = ByteArray(16 * 1024)
            val output = java.io.ByteArrayOutputStream(minOf(maxBytes, 64 * 1024))
            var count = 0
            var truncated = false
            while (true) {
                val n = stream.read(buffer)
                if (n < 0) break
                if (count < maxBytes) {
                    val keep = minOf(n, maxBytes - count)
                    output.write(buffer, 0, keep)
                    count += keep
                    if (keep < n) truncated = true
                } else {
                    truncated = true
                }
            }
            return ReadResult(output.toString(Charsets.UTF_8.name()), truncated)
        }
    }

    private data class ReadResult(val text: String, val truncated: Boolean)

    companion object {
        private const val MAX_OUTPUT_BYTES = 100_000
        private const val MAX_BATCH_COMMANDS = 32
    }
}
