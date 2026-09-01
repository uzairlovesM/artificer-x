package com.waheed.artificerx.architecture

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards a small number of layering conventions that are easy to
 * accidentally break as the codebase grows (Section 72's clean
 * architecture split between domain / data / core / ui). This is a
 * source-scan check rather than a bytecode/reflection check — cheap to
 * run, easy to read, and matched by CI's `--tests "*ArchitectureTest*"`
 * step so that step verifies something real instead of silently
 * matching zero tests.
 *
 * Kept deliberately minimal: a handful of rules that catch real
 * layering drift, not an exhaustive architecture-fitness suite.
 */
class ArchitectureTest {
    private val projectRoot: File by lazy {
        // Walk up from the test's working directory to find app/src/main/java.
        var dir = File(System.getProperty("user.dir") ?: ".")
        repeat(5) {
            val candidate = File(dir, "app/src/main/java/com/waheed/artificerx")
            if (candidate.exists()) return@lazy candidate
            dir = dir.parentFile ?: dir
        }
        File(dir, "app/src/main/java/com/waheed/artificerx")
    }

    private fun kotlinFilesUnder(subpackage: String): List<File> {
        val dir = File(projectRoot, subpackage)
        if (!dir.exists()) return emptyList()
        return dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
    }

    @Test
    fun `domain model package does not import android framework classes`() {
        // domain.model is meant to be plain Kotlin data classes/enums —
        // no android.* imports — so it stays trivially unit-testable and
        // reusable outside an Android runtime (Section 72).
        val offendingFiles =
            kotlinFilesUnder("domain/model").filter { file ->
                file.readLines().any { line ->
                    line.trimStart().startsWith("import android.") ||
                        line.trimStart().startsWith("import androidx.")
                }
            }
        assertTrue(
            "domain/model files must not import android/androidx classes, found violations in: " +
                offendingFiles.joinToString { it.name },
            offendingFiles.isEmpty(),
        )
    }

    @Test
    fun `every ViewModel file lives under a ui screens subpackage or core equivalent`() {
        // Sanity check that ViewModel classes aren't scattered into
        // random packages, which tends to happen after a lot of
        // copy-paste iteration.
        val allKotlinFiles = projectRoot.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        val viewModelFilesOutsideConvention =
            allKotlinFiles.filter { file ->
                val looksLikeViewModel = file.name.endsWith("ViewModel.kt")
                val inExpectedLocation = file.path.contains("${File.separator}ui${File.separator}screens${File.separator}")
                looksLikeViewModel && !inExpectedLocation
            }
        assertTrue(
            "ViewModel files found outside ui/screens/*: " +
                viewModelFilesOutsideConvention.joinToString { it.name },
            viewModelFilesOutsideConvention.isEmpty(),
        )
    }
}
