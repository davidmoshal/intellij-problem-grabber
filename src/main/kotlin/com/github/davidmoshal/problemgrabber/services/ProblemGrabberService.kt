package com.github.davidmoshal.problemgrabber.services

import com.github.davidmoshal.problemgrabber.models.ProblemData
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager

@Service(Service.Level.PROJECT)
class ProblemGrabberService(private val project: Project) {
    private val logger = Logger.getInstance(ProblemGrabberService::class.java)

    /**
     * Get problems for the current project
     */
    fun getProblemsForProject(severityFilter: Set<HighlightSeverity>? = null): List<ProblemData> {
        logger.info("Getting problems for project: ${project.name}")

        val moduleManager = ModuleManager.getInstance(project)
        return moduleManager.modules.flatMap { module ->
            getProblemsForModule(module, severityFilter)
        }
    }

    /**
     * Get problems for a specific module
     */
    fun getProblemsForModule(module: Module, severityFilter: Set<HighlightSeverity>? = null): List<ProblemData> {
        logger.info("Getting problems for module: ${module.name}")

        val rootManager = ModuleRootManager.getInstance(module)
        val psiManager = PsiManager.getInstance(project)

        return rootManager.sourceRoots.flatMap { root ->
            getProblemsForDirectory(root, psiManager, severityFilter)
        }
    }

    /**
     * Get problems for a directory recursively
     */
    private fun getProblemsForDirectory(
        dir: VirtualFile,
        psiManager: PsiManager,
        severityFilter: Set<HighlightSeverity>?
    ): List<ProblemData> {
        val result = mutableListOf<ProblemData>()

        // Process files in this directory
        for (file in dir.children) {
            if (file.isDirectory) {
                result.addAll(getProblemsForDirectory(file, psiManager, severityFilter))
            } else {
                val psiFile = psiManager.findFile(file)
                if (psiFile != null) {
                    result.addAll(getProblemsForFile(psiFile, severityFilter))
                }
            }
        }

        return result
    }

    /**
     * Get problems for the current project
     */
    fun getProblemsForFile(psiFile: PsiFile, severityFilter: Set<HighlightSeverity>? = null): List<ProblemData> {
        logger.info("Getting problems for file: ${psiFile.name}")

        val file = psiFile.virtualFile
        val fileDocumentManager = FileDocumentManager.getInstance()
        val document = fileDocumentManager.getDocument(file) ?: return emptyList()

        // Get highlighting info for this file
        val highlights = DaemonCodeAnalyzerImpl.getHighlights(document, null, project)
        if (highlights.isEmpty()) return emptyList()

        logger.info("Found ${highlights.size} highlights for file ${file.name}")

        // Filter by severity if requested
        val filteredHighlights = if (severityFilter != null) {
            highlights.filter { info -> severityFilter.contains(info.severity) }
        } else {
            highlights
        }

        return filteredHighlights.map { info ->
            val line = document.getLineNumber(info.startOffset)
            val column = info.startOffset - document.getLineStartOffset(line)
            val codeSnippet = extractCodeSnippet(document, line)
            val surroundingCode = extractSurroundingCode(document, line, 3)

            ProblemData(
                message = info.description ?: "",
                description = info.toolTip ?: "",
                type = info.type.toString(),
                filePath = file.path,
                line = line,
                column = column,
                severity = info.severity.name,
                fix = getQuickFix(info),
                code = codeSnippet,
                surroundingCode = surroundingCode,
            )
        }
    }

    /**
     * Get quick fix information if available
     */
    private fun getQuickFix(info: HighlightInfo): String? {
       // return info.quickFixActionRanges?.firstOrNull()?.first?.action?.text
        return info.findRegisteredQuickFix { descriptor, _ -> descriptor.action.text }
    }

    /**
     * Extract code from a document at a specific line
     */
    private fun extractCodeSnippet(document: Document, line: Int): String {
        if ((line < 0) || (line >= document.lineCount)) {
            return ""
        }

        val startOffset = document.getLineStartOffset(line)
        val endOffset = document.getLineEndOffset(line)
        return document.getText(TextRange(startOffset, endOffset))
    }

    /**
     * Extract surrounding code from a document
     */
    private fun extractSurroundingCode(document: Document, line: Int, contextLines: Int): String {
        if ((line < 0) || (line >= document.lineCount)) {
            return ""
        }

        val startLine = maxOf(0, line - contextLines)
        val endLine = minOf(document.lineCount - 1, line + contextLines)

        val result = StringBuilder()

        for (i in startLine..endLine) {
            val linePrefix = if (i == line) ">>> " else "    "
            val lineNumber = "${i + 1}".padStart(4)
            val lineText = document.getText(
                TextRange(
                    document.getLineStartOffset(i),
                    document.getLineEndOffset(i),
                ),
            )
            result.appendLine("$lineNumber: $linePrefix$lineText")
        }

        return result.toString()
    }

    /**
     * Export problems to file
     */
    internal fun exportToFile(problems: List<ProblemData>, filePath: String) {
        logger.info("Exporting ${problems.size} problems to file: $filePath")

        val sb = StringBuilder()
        sb.appendLine("# Project Problems: ${project.name}")
        sb.appendLine("Total problems: ${problems.size}")
        sb.appendLine()

        problems.forEachIndexed { index, problem ->
            sb.appendLine("## Problem ${index + 1}")
            sb.appendLine("## Problem Details")
            sb.appendLine("- **Message**: ${problem.message}")
            sb.appendLine("- **Description**: ${problem.description}")
            sb.appendLine("- **Type**: ${problem.type}")
            sb.appendLine("- **Severity**: ${problem.severity}")
            sb.appendLine("- **File**: ${problem.filePath}")
            sb.appendLine("- **Line**: ${problem.line}")
            sb.appendLine("- **Column**: ${problem.column}")
            sb.appendLine()

            sb.appendLine("### Problematic Code")
            sb.appendLine("```")
            sb.appendLine(problem.code)
            sb.appendLine("```")
            sb.appendLine()

            sb.appendLine("### Code Context")
            sb.appendLine("```")
            sb.appendLine(problem.surroundingCode)
            sb.appendLine("```")
            sb.appendLine()

            sb.appendLine("---")
        }

        // Write to file
        java.io.File(filePath).writeText(sb.toString())
    }
}
