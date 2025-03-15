package com.github.davidmoshal.problemgrabber.models
import com.intellij.openapi.util.text.StringUtil

import com.intellij.openapi.project.Project

/**
 * Data class representing a detected problem in the code
 */
data class ProblemData(
    val message: String,
    val description: String,
    val type: String,
    val filePath: String,
    val line: Int,
    val column: Int,
    val severity: String,
    val fix: String?,
    val code: String,
    val surroundingCode: String
) {
    companion object {

        /**
         * Cleans HTML from problem descriptions
         */
        fun cleanHtmlDescription(description: String): String {
            if (!description.contains("<") && !description.contains("&")) return description

            var cleaned = description

            // Convert tables to markdown
            cleaned = convertHtmlTableToMarkdown(cleaned)

            // Remove "more..." links
            cleaned = cleaned.replace(Regex("<a href=\"#inspection/[^\"]*\"[^>]*>more…</a>"), "")

            // Remove "Powered by..." text
            cleaned = cleaned.replace(Regex("<p[^>]*>\\s*Powered by [^<]*</p>"), "")

            // Remove control characters like (Ctrl+1)
            cleaned = cleaned.replace(Regex("\\(Ctrl\\+\\d+\\)"), "")

            // Convert bold tags to markdown
            cleaned = cleaned.replace(Regex("<(strong|b)>([^<]*)</\\1>"), "**$2**")

            // Convert italic tags to markdown
            cleaned = cleaned.replace(Regex("<(em|i)>([^<]*)</\\1>"), "*$2*")

            // Convert paragraph breaks to double newlines
            cleaned = cleaned.replace(Regex("</p>\\s*<p[^>]*>"), "\n\n")

            // Remove any remaining HTML tags
            cleaned = cleaned.replace(Regex("<[^>]*>"), "")

            // Decode HTML entities
            cleaned = StringUtil.unescapeXmlEntities(cleaned)

            // Clean up excessive whitespace
            cleaned = cleaned.replace(Regex("\\s+"), " ").trim()

            return cleaned
        }

        /**
         * Extracts the key from a HighlightInfoType string
         */
        fun extractTypeKey(typeString: String): String {
            val keyPattern = Regex("key=([^,\\]]+)")
            val keyMatch = keyPattern.find(typeString)
            return keyMatch?.groupValues?.get(1) ?: typeString
        }

        /**
         * Convert absolute file path to path relative to project root
         */
        fun getRelativePath(project: Project, absolutePath: String): String {
            val projectBasePath = project.basePath
            if (projectBasePath != null && absolutePath.startsWith(projectBasePath)) {
                return absolutePath.substring(projectBasePath.length).removePrefix("/")
            }
            return absolutePath
        }

        /**
         * Format a list of problems into a readable markdown document
         */
        fun formatProblems(project: Project, problems: List<ProblemData>): String {
            val sb = StringBuilder()
            sb.appendLine("# Project Problems: ${project.name}")
            sb.appendLine("Total problems: ${problems.size}")
            sb.appendLine()

            // Add summary by severity
            if (problems.isNotEmpty()) {
                sb.appendLine("## Problem Severity Summary")
                val severityCounts = problems.groupBy { it.severity }
                    .mapValues { it.value.size }
                    .toList()
                    .sortedByDescending { it.second }

                for ((severity, count) in severityCounts) {
                    sb.appendLine("- $severity: $count")
                }

                sb.appendLine()

                // Add summary by key instead of type
                sb.appendLine("## Problem Type Summary")
                val keyCounts = problems.groupBy { extractTypeKey(it.type) }
                    .mapValues { it.value.size }
                    .toList()
                    .sortedByDescending { it.second }

                for ((key, count) in keyCounts) {
                    sb.appendLine("- $key: $count")
                }

                sb.appendLine()
            }

            // Group problems by file
            val problemsByFile = problems.groupBy { it.filePath }

            // For each file
            problemsByFile.entries.forEachIndexed { fileIndex, (filePath, fileProblems) ->
                // Use relative path instead of absolute path
                val relativePath = getRelativePath(project, filePath)
                sb.appendLine("## File ${fileIndex + 1}: $relativePath")
                sb.appendLine()

                // For each problem in the file
                fileProblems.forEachIndexed { problemIndex, problem ->
                    sb.appendLine("### Problem ${fileIndex + 1}.${problemIndex + 1}")
                    sb.appendLine("- **Line**: ${problem.line}")
                    sb.appendLine("- **Column**: ${problem.column}")
                    sb.appendLine("- **Severity**: ${problem.severity}")
                    sb.appendLine("- **Key**: ${extractTypeKey(problem.type)}")

                    // Only include message if it's different from description
                    val cleanedDescription = cleanHtmlDescription(problem.description)
                    if (problem.message != cleanedDescription && problem.message.isNotEmpty()) {
                        sb.appendLine("- **Message**: ${problem.message}")
                    }

                    // Use code block with quadruple backticks for description
                    sb.appendLine("- **Description**:")
                    sb.appendLine("````")
                    sb.appendLine(cleanedDescription)
                    sb.appendLine("````")

                    if (!problem.fix.isNullOrEmpty()) {
                        sb.appendLine("- **Suggested Fix**: ${problem.fix}")
                    }
                    sb.appendLine()

                    sb.appendLine("#### Problematic Code")
                    sb.appendLine("```")
                    sb.appendLine(problem.code)
                    sb.appendLine("```")
                    sb.appendLine()

                    sb.appendLine("#### Code Context")
                    sb.appendLine("```")
                    sb.appendLine(problem.surroundingCode)
                    sb.appendLine("```")
                    sb.appendLine()

                    if (problemIndex < fileProblems.size - 1) {
                        sb.appendLine("---")
                        sb.appendLine()
                    }
                }

                if (fileIndex < problemsByFile.size - 1) {
                    sb.appendLine("\n---\n")
                }
            }

            return sb.toString()
        }

        /**
         * Converts HTML tables to markdown format
         */
        fun convertHtmlTableToMarkdown(html: String): String {
            // Extract table content
            val tablePattern = Regex("<table[^>]*>(.*?)</table>", RegexOption.DOT_MATCHES_ALL)
            val tableMatch = tablePattern.find(html) ?: return html

            val tableContent = tableMatch.groupValues[1]

            // Extract rows
            val rowPattern = Regex("<tr>(.*?)</tr>", RegexOption.DOT_MATCHES_ALL)
            val rows = rowPattern.findAll(tableContent).map { it.groupValues[1] }.toList()

            if (rows.isEmpty()) return html

            val markdown = StringBuilder("\n\n")

            // Process each row
            rows.forEachIndexed { index, row ->
                // Extract cells
                val cellPattern = Regex("<td[^>]*>(.*?)</td>", RegexOption.DOT_MATCHES_ALL)
                val cells = cellPattern.findAll(row).map {
                    // Clean cell content (remove tags, trim)
                    it.groupValues[1]
                        .replace(Regex("<(strong|b)>([^<]*)</\\1>"), "**$2**")
                        .replace(Regex("<[^>]*>"), "")
                        .trim()
                }.toList()

                if (cells.isEmpty()) return@forEachIndexed

                // Add cell content with pipe separators
                markdown.append("| ")
                markdown.append(cells.joinToString(" | "))
                markdown.append(" |")
                markdown.append("\n")

                // Add separator row after header (first row)
                if (index == 0) {
                    markdown.append("| ")
                    markdown.append(cells.joinToString(" | ") { "-".repeat(it.length.coerceAtLeast(3)) })
                    markdown.append(" |")
                    markdown.append("\n")
                }
            }

            return html.replace(tableMatch.value, markdown.toString())
        }
    }
}
