package com.github.davidmoshal.problemgrabber.actions

import com.github.davidmoshal.problemgrabber.models.ProblemData
import com.github.davidmoshal.problemgrabber.services.ProblemGrabberService
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import java.awt.datatransfer.StringSelection
import javax.swing.Icon

class CaptureProblemsAction : ActionGroup() {

    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
        return arrayOf(
            // Error actions
            CaptureErrorsFileAction(),
            CaptureErrorsProjectAction(),
            Separator.getInstance(),

            // All problems actions
            CaptureAllFileAction(),
            CaptureAllProjectAction(),
            Separator.getInstance(),

            // Category-specific actions (with TODOs)
            CaptureApiIssuesAction(),
            CaptureCodeStyleIssuesAction(),
            Separator.getInstance(),

            // Configuration action (with TODO)
            ConfigureFiltersAction()
        )
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    // Base class for all problem capture actions
    abstract class BaseProblemCaptureAction(
        text: String,
        description: String,
        icon: Icon? = null
    ) : AnAction(text, description, icon) {

        enum class Scope { FILE, PROJECT }

        abstract val scope: Scope
        abstract val severityFilter: Set<HighlightSeverity>?

        override fun update(e: AnActionEvent) {
            // Only check if project is available, avoid PSI checks here
            e.presentation.isEnabledAndVisible = e.project != null
        }

        override fun getActionUpdateThread(): ActionUpdateThread {
            return ActionUpdateThread.BGT
        }

        override fun actionPerformed(e: AnActionEvent) {
            val project = e.project ?: return
            val problemGrabberService = project.getService(ProblemGrabberService::class.java)

            val problems = when (scope) {
                Scope.FILE -> {
                    val psiFile = e.getData(CommonDataKeys.PSI_FILE)
                    if (psiFile == null) {
                        Messages.showWarningDialog(
                            project,
                            "No file is selected. Please select a file in the editor or project view.",
                            "No File Selected"
                        )
                        return
                    }
                    problemGrabberService.getProblemsForFile(psiFile, severityFilter)
                }
                Scope.PROJECT -> {
                    problemGrabberService.getProblemsForProject(severityFilter)
                }
            }

            displayProblems(project, problems)
        }

        private fun displayProblems(project: Project, problems: List<ProblemData>) {
            // Format the problems as text
            val formattedText = formatProblems(project, problems)

            // Copy to clipboard
            val clipboard = CopyPasteManager.getInstance()
            clipboard.setContents(StringSelection(formattedText))

            // Show notification
            NotificationGroupManager.getInstance()
                .getNotificationGroup("Problem Grabber")
                .createNotification(
                    "Problems Captured",
                    "${problems.size} problems copied to clipboard",
                    NotificationType.INFORMATION
                )
                .notify(project)
        }

        private fun formatProblems(project: Project, problems: List<ProblemData>): String {
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

            return sb.toString()
        }
    }

    // Concrete action implementations
    class CaptureErrorsFileAction : BaseProblemCaptureAction(
        "Capture Errors Only (File)",
        "Capture error-level problems in the current file"
    ) {
        override val scope = Scope.FILE
        override val severityFilter = setOf(HighlightSeverity.ERROR)
    }

    class CaptureErrorsProjectAction : BaseProblemCaptureAction(
        "Capture Errors Only (Project)",
        "Capture error-level problems in the entire project"
    ) {
        override val scope = Scope.PROJECT
        override val severityFilter = setOf(HighlightSeverity.ERROR)
    }

    class CaptureAllFileAction : BaseProblemCaptureAction(
        "Capture All Problems (File)",
        "Capture all problems in the current file"
    ) {
        override val scope = Scope.FILE
        override val severityFilter = null // null means no filter
    }

    class CaptureAllProjectAction : BaseProblemCaptureAction(
        "Capture All Problems (Project)",
        "Capture all problems in the entire project"
    ) {
        override val scope = Scope.PROJECT
        override val severityFilter = null
    }

    // TODO actions
    class CaptureApiIssuesAction : AnAction("Capture API Issues (Project)") {
        override fun getActionUpdateThread(): ActionUpdateThread {
            return ActionUpdateThread.BGT
        }

        override fun actionPerformed(e: AnActionEvent) {
            val project = e.project ?: return

            Messages.showInfoMessage(
                project,
                "API issues capture will be implemented in a future version.",
                "Coming Soon"
            )
        }
    }

    class CaptureCodeStyleIssuesAction : AnAction("Capture Code Style Issues (Project)") {
        override fun getActionUpdateThread(): ActionUpdateThread {
            return ActionUpdateThread.BGT
        }

        override fun actionPerformed(e: AnActionEvent) {
            val project = e.project ?: return

            Messages.showInfoMessage(
                project,
                "Code style issues capture will be implemented in a future version.",
                "Coming Soon"
            )
        }
    }

    class ConfigureFiltersAction : AnAction("Configure Problem Filters...") {
        override fun getActionUpdateThread(): ActionUpdateThread {
            return ActionUpdateThread.BGT
        }

        override fun actionPerformed(e: AnActionEvent) {
            val project = e.project ?: return

            Messages.showInfoMessage(
                project,
                "Problem filter configuration will be implemented in a future version.",
                "Coming Soon"
            )
        }
    }
}
