package com.waheed.artificerx.core.ai.planning

data class ResponseIssue(val code: String, val message: String)

class ResponseValidator {
    fun validate(text: String, maxCharacters: Int = 500_000): List<ResponseIssue> {
        val issues = mutableListOf<ResponseIssue>()
        if (text.isBlank()) issues += ResponseIssue("EMPTY", "Response is empty")
        if (text.length > maxCharacters) issues += ResponseIssue("OVERSIZE", "Response exceeds configured safety limit")
        if (text.contains("\u0000")) issues += ResponseIssue("CONTROL_CHAR", "Response contains NUL character")
        if (text.count { it == '`' } % 2 != 0) issues += ResponseIssue("UNCLOSED_CODE", "Unbalanced code fence")
        return issues
    }
}
