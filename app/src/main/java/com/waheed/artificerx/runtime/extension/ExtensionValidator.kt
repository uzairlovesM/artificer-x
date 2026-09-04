package com.waheed.artificerx.runtime.extension

class ExtensionValidator {
    fun validate(extension: RuntimeExtension): List<String> = buildList {
        if (!extension.id.matches(Regex("[a-z0-9_.-]+"))) add("invalid id")
        if (extension.version.isBlank()) add("missing version")
        if (extension.entrypoint.isBlank()) add("missing entrypoint")
        if (extension.checksum.length < 16) add("weak checksum")
        if (extension.capabilities.isEmpty()) add("no capabilities")
    }
}
