package com.waheed.artificerx.core.storage
import java.io.File
class PathPolicy(private val root:File) {
    fun resolve(relative:String):File {
        val candidate=File(root,relative).canonicalFile
        val base=root.canonicalFile
        require(candidate.path==base.path || candidate.path.startsWith(base.path+File.separator)){"Path escapes workspace"}
        return candidate
    }
}
