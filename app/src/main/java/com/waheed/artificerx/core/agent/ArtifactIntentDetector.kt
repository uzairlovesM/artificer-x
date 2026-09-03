package com.waheed.artificerx.core.agent

object ArtifactIntentDetector {
    enum class OutputKind { TEXT, IMAGE, FILE, ARCHIVE, DOCUMENT }
    data class Intent(val requested: Set<OutputKind>, val explicit: Boolean)

    fun detect(prompt: String): Intent {
        val p = prompt.lowercase()
        val kinds = buildSet {
            if (listOf("image", "picture", "png", "jpg", "illustration").any(p::contains)) add(OutputKind.IMAGE)
            if (listOf("file", "code", "source", "json", "markdown", "txt").any(p::contains)) add(OutputKind.FILE)
            if (listOf("zip", "archive", "bundle", "project pack").any(p::contains)) add(OutputKind.ARCHIVE)
            if (listOf("pdf", "docx", "document").any(p::contains)) add(OutputKind.DOCUMENT)
        }
        return Intent(kinds, kinds.isNotEmpty())
    }
}
