package com.github.davidmoshal.problemgrabber.actions

import com.github.davidmoshal.problemgrabber.models.ProblemData
import com.github.davidmoshal.problemgrabber.services.ProblemGrabberService
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import java.awt.datatransfer.StringSelection

/**
 * Action to capture error-level problems in the current file
 */
class CaptureFileErrorsAction : AnAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null && e.getData(CommonDataKeys.PSI_FILE) != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
        val problemGrabberService = project.getService(ProblemGrabberService::class.java)

        val problems = problemGrabberService.getProblemsForFile(psiFile, setOf(HighlightSeverity.ERROR))
        displayProblems(project, problems)
    }
}

/**
 * Action to capture all problems in the current file
 */
class CaptureFileProblemsAction : AnAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null && e.getData(CommonDataKeys.PSI_FILE) != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
        val problemGrabberService = project.getService(ProblemGrabberService::class.java)

        val problems = problemGrabberService.getProblemsForFile(psiFile, null)
        displayProblems(project, problems)
    }
}

/**
 * Action to capture error-level problems in the entire project
 */
class CaptureProjectErrorsAction : AnAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val problemGrabberService = project.getService(ProblemGrabberService::class.java)

        val problems = problemGrabberService.getProblemsForProject(setOf(HighlightSeverity.ERROR))
        displayProblems(project, problems)
    }
}

/**
 * Action to capture all problems in the entire project
 */
class CaptureProjectProblemsAction : AnAction() {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val problemGrabberService = project.getService(ProblemGrabberService::class.java)

        val problems = problemGrabberService.getProblemsForProject(null)
        displayProblems(project, problems)
    }
}

/**
 * Common utility function to display problems
 */
private fun displayProblems(project: Project, problems: List<ProblemData>) {
    if (problems.isEmpty()) {
        Messages.showInfoMessage(project, "No problems found.", "Problem Grabber")
        return
    }

    // Format the problems as text
    val formattedText = ProblemData.formatProblems(project, problems)

    // Copy to clipboard
    val clipboard = CopyPasteManager.getInstance()
    clipboard.setContents(StringSelection(formattedText))

    // Show notification
    NotificationGroupManager.getInstance()
        .getNotificationGroup("ProblemGrabber")
        .createNotification(
            "Problems Captured",
            "${problems.size} problems copied to clipboard",
            NotificationType.INFORMATION
        )
        .notify(project)
}
