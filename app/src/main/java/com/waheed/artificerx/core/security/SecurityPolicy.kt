package com.waheed.artificerx.core.security

import java.io.File

object SecurityPolicy {
    private val forbiddenFragments = listOf(
        "rm -rf /", "rm -fr /", "mkfs", "dd if=", ":(){", "shutdown -h now", "reboot",
        "mount ", "umount ", "insmod ", "rmmod ", "setenforce ", "iptables ", "nft ",
        "su -c", "busybox rm -rf /", "toybox rm -rf /", "chmod -r 777 /",
    )

    /**
     * Shell commands are executed by a child shell whose working directory is
     * the app-private terminal root. This policy therefore rejects the shell
     * constructs most likely to turn a bounded workspace command into an
     * arbitrary host traversal or an unbounded background process.
     */
    fun isShellAllowed(command: String): Boolean {
        val normalized = command.lowercase().replace("\u0000", "").trim()
        if (normalized.isBlank() || normalized.length > 4_096) return false
        if (forbiddenFragments.any(normalized::contains)) return false

        // Keep the agent sandbox single-process and workspace-oriented.
        val structuralFragments = listOf(
            "\n", "\r", "`", "$(", "&&", "||", ";", "\u007c",
            ">", "<", "&", "cd ", "cd\t", "pushd ", "popd ", "exec ", "source ", "nohup ", "setsid ", "disown", "watch ",
        )
        if (structuralFragments.any(normalized::contains)) return false

        // Reject explicit absolute paths and traversal segments. Binaries are
        // resolved through PATH, while files are expected to live below the
        // sandbox working directory.
        if (Regex("""(^|[\s(=\"'])/(?:[^/]|$)""").containsMatchIn(normalized)) return false
        if (Regex("""(^|[\s=/\"'])\.\.(?:[/\s]|$)""").containsMatchIn(normalized)) return false
        if (Regex("""(^|[\s=\"'])(?:[a-z]:[/\\])""").containsMatchIn(normalized)) return false

        return true
    }

    fun constrainPath(root: File, relative: String): File? {
        if (relative.indexOf('\u0000') >= 0) return null
        val base = root.canonicalFile
        val candidate = File(base, relative).canonicalFile
        return if (candidate.path == base.path || candidate.path.startsWith(base.path + File.separator)) candidate else null
    }
}
