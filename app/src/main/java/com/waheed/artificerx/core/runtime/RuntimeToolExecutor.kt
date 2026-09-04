package com.waheed.artificerx.core.runtime

import com.waheed.artificerx.core.security.SecurityPolicy
import com.waheed.artificerx.core.storage.WorkspaceFileSystem
import com.waheed.artificerx.core.storage.WorkspaceFileTools
import com.waheed.artificerx.core.terminal.TerminalSandbox
import com.waheed.artificerx.core.agent.ToolExecutionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RuntimeToolExecutor @Inject constructor(
    private val workspaceFiles: WorkspaceFileTools,
    private val workspaceFs: WorkspaceFileSystem,
    private val terminalSandbox: TerminalSandbox,
) {
    private val http = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()

    suspend fun execute(name: String, args: Map<String, String>): ToolExecutionResult = withContext(Dispatchers.IO) {
        val spec = RuntimeToolCatalog.get(name) ?: return@withContext ToolExecutionResult.Failure("Runtime tool '$name' is not installed.")
        fun cfg(key: String): String = render(spec.config[key].orEmpty(), args)
        when (spec.operation) {
            "WRITE_FILE" -> workspaceFiles.write(cfg("path_template"), cfg("content_template")).fold(
                { ToolExecutionResult.Success("Runtime tool '$name' wrote ${it.absolutePath}") },
                { ToolExecutionResult.Failure(it.message ?: "write failed") },
            )
            "READ_FILE" -> workspaceFiles.read(cfg("path_template"), cfg("max_chars").toIntOrNull() ?: 100_000).fold(
                { ToolExecutionResult.Success(it) },
                { ToolExecutionResult.Failure(it.message ?: "read failed") },
            )
            "LIST_DIRECTORY" -> workspaceFiles.list(cfg("path_template")).fold(
                { ToolExecutionResult.Success(it.joinToString("\n")) },
                { ToolExecutionResult.Failure(it.message ?: "list failed") },
            )
            "REPLACE_TEXT" -> workspaceFiles.replace(cfg("path_template"), cfg("old_template"), cfg("new_template"), cfg("all").equals("true", true)).fold(
                { ToolExecutionResult.Success("Runtime tool '$name' patched ${it.absolutePath}") },
                { ToolExecutionResult.Failure(it.message ?: "replace failed") },
            )
            "COPY_FILE", "MOVE_FILE", "DELETE_FILE", "HASH_FILE" -> fileOperation(spec.operation, cfg)
            "HTTP_GET" -> httpGet(cfg("url_template"))
            "RUN_COMMAND" -> {
                val command = cfg("command_template")
                if (!SecurityPolicy.isShellAllowed(command)) ToolExecutionResult.Failure("Runtime command rejected by sandbox policy.")
                else {
                    val result = terminalSandbox.run(command, (cfg("timeout_seconds").toLongOrNull() ?: 30L).coerceIn(1L, 60L))
                    ToolExecutionResult.Success("exit=${result.exitCode}\nstdout=${result.stdout}\nstderr=${result.stderr}")
                }
            }
            else -> ToolExecutionResult.Failure("Unsupported runtime operation '${spec.operation}'.")
        }
    }

    private fun fileOperation(operation: String, cfg: (String) -> String): ToolExecutionResult {
        val source = safe(cfg("source_template")) ?: return ToolExecutionResult.Failure("Invalid source path.")
        return when (operation) {
            "COPY_FILE" -> {
                val dest = safe(cfg("destination_template")) ?: return ToolExecutionResult.Failure("Invalid destination path.")
                if (!source.isFile) return ToolExecutionResult.Failure("Source file not found: ${source.path}")
                dest.parentFile?.mkdirs(); source.copyTo(dest, overwrite = cfg("overwrite").equals("true", true))
                ToolExecutionResult.Success("Copied ${source.path} -> ${dest.path}")
            }
            "MOVE_FILE" -> {
                val dest = safe(cfg("destination_template")) ?: return ToolExecutionResult.Failure("Invalid destination path.")
                if (!source.exists()) return ToolExecutionResult.Failure("Source path not found: ${source.path}")
                dest.parentFile?.mkdirs(); if (source.isDirectory) source.copyRecursively(dest, overwrite = cfg("overwrite").equals("true", true)) else source.copyTo(dest, overwrite = cfg("overwrite").equals("true", true)); source.deleteRecursively()
                ToolExecutionResult.Success("Moved ${source.path} -> ${dest.path}")
            }
            "DELETE_FILE" -> {
                if (!source.exists()) return ToolExecutionResult.Failure("Path not found: ${source.path}")
                if (source.absolutePath == workspaceFs.roots.works.canonicalPath) return ToolExecutionResult.Failure("Refusing to delete workspace root.")
                source.deleteRecursively()
                ToolExecutionResult.Success("Deleted ${source.path}")
            }
            "HASH_FILE" -> {
                if (!source.isFile) return ToolExecutionResult.Failure("File not found: ${source.path}")
                val digest = MessageDigest.getInstance("SHA-256")
                source.inputStream().use { input -> val buf = ByteArray(32 * 1024); while (true) { val n = input.read(buf); if (n <= 0) break; digest.update(buf, 0, n) } }
                ToolExecutionResult.Success("SHA-256=${digest.digest().joinToString("") { "%02x".format(it) }}")
            }
            else -> ToolExecutionResult.Failure("Unsupported file operation '$operation'.")
        }
    }

    private fun httpGet(url: String): ToolExecutionResult = runCatching {
        val response = http.newCall(Request.Builder().url(url).get().build()).execute()
        response.use { ToolExecutionResult.Success("HTTP ${it.code}\n${it.body?.string().orEmpty().take(100_000)}") }
    }.getOrElse { ToolExecutionResult.Failure(it.message ?: "HTTP GET failed") }

    private fun safe(relative: String): File? = SecurityPolicy.constrainPath(workspaceFs.roots.works, relative)

    private fun render(template: String, args: Map<String, String>): String = Regex("\\$\\{([a-zA-Z0-9_]+)}").replace(template) { args[it.groupValues[1]].orEmpty() }
}
