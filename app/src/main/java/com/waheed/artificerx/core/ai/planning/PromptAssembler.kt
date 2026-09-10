package com.waheed.artificerx.core.ai.planning

data class PromptSection(val name: String, val text: String, val priority: Int)

class PromptAssembler(private val characterLimit: Int = 120_000) {
    init { require(characterLimit > 1000) }

    fun assemble(sections: List<PromptSection>): String {
        val ordered = sections.filter { it.text.isNotBlank() }.sortedByDescending { it.priority }
        val builder = StringBuilder()
        for (section in ordered) {
            val block = "\n[${section.name}]\n${section.text.trim()}\n"
            if (builder.length + block.length > characterLimit) break
            builder.append(block)
        }
        return builder.toString().trim()
    }
}
