package com.github.davidmoshal.problemgrabber.models

/**
 * Data class representing an IntelliJ problem
 */
data class ProblemData(
    val message: String,
    val description: String,
    val type: String,
    val fileName: String,
    val lineNumber: Int,
    val columnNumber: Int,
    val severity: String,
    val fix: String?,
    val code: String?,
    val surroundingCode: String?
) {
    fun toMarkdown(): String {
        val sb = StringBuilder()

        sb.appendLine("## Problem Details")
        sb.appendLine("- **Message**: $message")
        if (description.isNotBlank()) {
            sb.appendLine("- **Description**: $description")
        }
        sb.appendLine("- **Type**: $type")
        sb.appendLine("- **Severity**: $severity")
        sb.appendLine("- **File**: $fileName")
        sb.appendLine("- **Line**: $lineNumber")
        sb.appendLine("- **Column**: $columnNumber")

        if (!fix.isNullOrBlank()) {
            sb.appendLine("\n### Quick Fix")
            sb.appendLine(fix)
        }

        if (!code.isNullOrBlank()) {
            sb.appendLine("\n### Problematic Code")
            sb.appendLine("```")
            sb.appendLine(code)
            sb.appendLine("```")
        }

        if (!surroundingCode.isNullOrBlank()) {
            sb.appendLine("\n### Code Context")
            sb.appendLine("```")
            sb.appendLine(surroundingCode)
            sb.appendLine("```")
        }

        return sb.toString()
    }
}
