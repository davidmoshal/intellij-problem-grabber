package com.github.davidmoshal.problemgrabber.services

import com.github.davidmoshal.problemgrabber.models.ProblemData
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File

@Service(Service.Level.PROJECT)
class ProblemGrabberService(private val project: Project) {
    private val LOG = Logger.getInstance(ProblemGrabberService::class.java)

    /**
     * Get problems for the current project
     */
    fun getProblems(): List<ProblemData> {
        LOG.info("Getting all problems for project: ${project.name}")

        val problems = mutableListOf<ProblemData>()

        // Get all open files
        val fileEditorManager = FileEditorManager.getInstance(project)
        val openFiles = fileEditorManager.openFiles

        for (file in openFiles) {
            problems.addAll(getProblemsForFile(file))
        }

        return problems
    }

    private fun isActualProblem(info: HighlightInfo): Boolean {
        // Only include errors, warnings, and weak warnings
        return when (info.severity) {
            HighlightSeverity.ERROR -> true
            HighlightSeverity.WARNING -> true
            HighlightSeverity.WEAK_WARNING -> true
            HighlightSeverity.INFORMATION -> true
            // Exclude SYMBOL_TYPE_SEVERITY and others
            else -> false
        }
    }

    /**
     * Get problems for a specific file
     */
    fun getProblemsForFile(file: VirtualFile): List<ProblemData> {
        LOG.info("Getting problems for file: ${file.path}")

        val problems = mutableListOf<ProblemData>()

        val psiManager = PsiManager.getInstance(project)
        val fileDocumentManager = FileDocumentManager.getInstance()

        val psiFile = psiManager.findFile(file) ?: return emptyList()
        val document = fileDocumentManager.getDocument(file) ?: return emptyList()

        // Get highlighting info for this file
        val highlights = DaemonCodeAnalyzerImpl.getHighlights(document, null, project)
        if (highlights == null || highlights.isEmpty()) return emptyList()

        LOG.info("Found ${highlights.size} highlights for file ${file.name}")

        for (info in highlights) {
            val problemData = convertToProblemData(info, file, document, psiFile)
            if (problemData != null) {
                problems.add(problemData)
            }
        }

        LOG.info("Filtered to ${problems.size} actual problems")

        return problems
    }

    /**
     * Convert HighlightInfo to ProblemData
     */
    /**
     * Convert HighlightInfo to ProblemData
     */
    private fun convertToProblemData(info: HighlightInfo, file: VirtualFile, document: Document, psiFile: PsiFile): ProblemData? {
        // Only include actual errors and warnings
        if (!isActualProblem(info)) {
            return null
        }

        // Rest of the method stays the same
        val startOffset = info.startOffset
        val lineNumber = document.getLineNumber(startOffset)
        val lineStartOffset = document.getLineStartOffset(lineNumber)
        val columnNumber = startOffset - lineStartOffset

        // Get code snippet
        val codeSnippet = extractCodeSnippet(document, lineNumber)
        val surroundingCode = extractSurroundingCode(document, lineNumber, 3)

        return ProblemData(
            message = info.description ?: "Unknown problem",
            description = info.toolTip ?: "",
            type = info.type.toString(),
            fileName = file.path,
            lineNumber = lineNumber + 1, // Convert to 1-based line number
            columnNumber = columnNumber + 1, // Convert to 1-based column number
            severity = info.severity.name,
            fix = getQuickFix(info, psiFile),
            code = codeSnippet,
            surroundingCode = surroundingCode
        )
    }

    /**
     * Get quick fix information if available
     */
    private fun getQuickFix(info: HighlightInfo, psiFile: PsiFile): String? {
        return info.quickFixActionRanges?.firstOrNull()?.first?.action?.text
    }

    /**
     * Extract code from a document at a specific line
     */
    private fun extractCodeSnippet(document: Document, line: Int): String {
        if (line < 0 || line >= document.lineCount) {
            return ""
        }

        val startOffset = document.getLineStartOffset(line)
        val endOffset = document.getLineEndOffset(line)
        return document.getText(TextRange(startOffset, endOffset)).trim()
    }

    /**
     * Extract surrounding code from a document
     */
    private fun extractSurroundingCode(document: Document, line: Int, contextLines: Int): String {
        if (line < 0 || line >= document.lineCount) {
            return ""
        }

        val startLine = maxOf(0, line - contextLines)
        val endLine = minOf(document.lineCount - 1, line + contextLines)

        val result = StringBuilder()
        for (i in startLine..endLine) {
            val linePrefix = if (i == line) ">>> " else "    "
            val lineNumber = "${i + 1}".padStart(4)
            val lineText = document.getText(TextRange(
                document.getLineStartOffset(i),
                document.getLineEndOffset(i)
            ))
            result.appendLine("$lineNumber: $linePrefix$lineText")
        }

        return result.toString()
    }

    /**
     * Copy problems to clipboard
     */
    fun copyToClipboard(problems: List<ProblemData>) {
        LOG.info("Copying ${problems.size} problems to clipboard")

        val sb = StringBuilder()
        sb.appendLine("# Project Problems: ${project.name}")
        sb.appendLine("Total problems: ${problems.size}")
        sb.appendLine()

        problems.forEachIndexed { index, problem ->
            sb.appendLine("## Problem ${index + 1}")
            sb.appendLine(problem.toMarkdown())
            sb.appendLine("---")
        }

        val selection = StringSelection(sb.toString())
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(selection, selection)
    }

    /**
     * Export problems to file
     */
    fun exportToFile(problems: List<ProblemData>, filePath: String) {
        LOG.info("Exporting ${problems.size} problems to file: $filePath")

        val sb = StringBuilder()
        sb.appendLine("# Project Problems: ${project.name}")
        sb.appendLine("Total problems: ${problems.size}")
        sb.appendLine()

        problems.forEachIndexed { index, problem ->
            sb.appendLine("## Problem ${index + 1}")
            sb.appendLine(problem.toMarkdown())
            sb.appendLine("---")
        }

        File(filePath).writeText(sb.toString())
    }

    companion object {
        fun getInstance(project: Project): ProblemGrabberService {
            return project.getService(ProblemGrabberService::class.java)
        }
    }
}
