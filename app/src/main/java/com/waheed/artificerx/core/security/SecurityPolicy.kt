package com.waheed.artificerx.core.security

import java.io.File

object SecurityPolicy {
    private val forbidden = listOf(
        "rm -rf /", "rm -fr /", "mkfs", "dd if=", ":(){", "shutdown -h now", "reboot",
        "mount ", "umount ", "insmod ", "rmmod ", "setenforce ", "iptables ", "nft ",
        "su -c", "busybox rm -rf /", "toybox rm -rf /", "chmod -r 777 /",
    )
    fun isShellAllowed(command: String): Boolean {
        val normalized = command.lowercase().replace("\n", " ").trim()
        return normalized.isNotBlank() && forbidden.none(normalized::contains)
    }

    fun constrainPath(root: File, relative: String): File? {
        val base = root.canonicalFile
        val candidate = File(base, relative).canonicalFile
        return if (candidate.path == base.path || candidate.path.startsWith(base.path + File.separator)) candidate else null
    }
}
