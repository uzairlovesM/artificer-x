package com.waheed.artificerx.core.terminal

import com.waheed.artificerx.core.security.SecurityPolicy
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class TerminalResult(val command: String, val exitCode: Int, val stdout: String, val stderr: String, val durationMs: Long)

@Singleton
class TerminalSandbox @Inject constructor(@ApplicationContext context: Context) {
    val root = File(context.filesDir, "sandbox/terminal").apply { mkdirs() }

    suspend fun run(command: String, timeoutSeconds: Long = 20): TerminalResult = withContext(Dispatchers.IO) {
        val started = System.currentTimeMillis()
        if (!SecurityPolicy.isShellAllowed(command)) {
            return@withContext TerminalResult(
                command = command,
                exitCode = 126,
                stdout = "",
                stderr = "Command rejected by Artificer-X sandbox policy.",
                durationMs = 0L,
            )
        }
        val stdoutFile = File(root, ".stdout-${java.util.UUID.randomUUID()}.tmp")
        val stderrFile = File(root, ".stderr-${java.util.UUID.randomUUID()}.tmp")
        try {
            val process = ProcessBuilder("/system/bin/sh", "-c", command)
                .directory(root)
                .redirectOutput(stdoutFile)
                .redirectError(stderrFile)
                .start()
            val finished = process.waitFor(timeoutSeconds.coerceIn(1, 60), TimeUnit.SECONDS)
            if (!finished) process.destroyForcibly()
            TerminalResult(
                command = command,
                exitCode = if (finished) process.exitValue() else 124,
                stdout = stdoutFile.takeIf { it.exists() }?.readText().orEmpty().take(20000),
                stderr = stderrFile.takeIf { it.exists() }?.readText().orEmpty().take(20000),
                durationMs = System.currentTimeMillis() - started,
            )
        } finally {
            stdoutFile.delete()
            stderrFile.delete()
        }
    }

    suspend fun runBatch(commands: List<String>, timeoutSecondsEach: Long = 20): List<TerminalResult> = commands.filter { it.isNotBlank() }.map { run(it, timeoutSecondsEach) }
}
