package com.waheed.artificerx.core.ai.apex.reasoning

data class PromptSection(val name: String, val value: String, val weight: Int)
class PromptCompiler {
    fun compile(system: String, sections: List<PromptSection>, user: String, maxChars: Int): String {
        require(system.isNotBlank() && user.isNotBlank() && maxChars > 0)
        val ordered = sections.sortedByDescending { it.weight }
        val out = StringBuilder(system.trim())
        for (section in ordered) {
            val piece = "\n[${section.name}]\n${section.value.trim()}"
            if (out.length + piece.length > maxChars) break
            out.append(piece)
        }
        return (out.toString() + "\n[USER]\n" + user.trim()).take(maxChars)
    }
}
