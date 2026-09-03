package com.waheed.artificerx.core.storage

import java.io.File

/** Central safety gate for all model/plugin/workspace paths. */
class StorageAccessPolicy {
    fun isManagedPath(file: File, roots: WorkspaceFileSystem.Roots): Boolean {
        val target = file.canonicalFile
        return roots.root.canonicalFile.let { target.path == it.path || target.path.startsWith(it.path + File.separator) }
    }

    fun safeChild(parent: File, requested: String): File? {
        val safe = requested.replace('\\', '/').split('/').filter { it.isNotBlank() && it != "." && it != ".." }
        if (safe.isEmpty()) return null
        val candidate = safe.fold(parent.canonicalFile) { acc, part -> acc.resolve(part) }.canonicalFile
        val root = parent.canonicalFile
        return if (candidate.path == root.path || candidate.path.startsWith(root.path + File.separator)) candidate else null
    }
}
