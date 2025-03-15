package com.github.davidmoshal.problemgrabber.actions

import com.github.davidmoshal.problemgrabber.services.ProblemGrabberService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep

/**
 * Action to capture problems
 */
class CaptureProblemsAction : AnAction() {
    private val LOG = Logger.getInstance(CaptureProblemsAction::class.java)

    override fun actionPerformed(e: AnActionEvent) {
        LOG.info("Capture problems action triggered")

        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)

        // Show options popup
        val options = if (file != null) {
            listOf(
                "Capture problems in this file",
                "Capture all project problems",
                "Cancel"
            )
        } else {
            listOf(
                "Capture all project problems",
                "Cancel"
            )
        }

        JBPopupFactory.getInstance().createListPopup(
            object : BaseListPopupStep<String>("Capture Problems", options) {
                override fun onChosen(selectedValue: String, finalChoice: Boolean): PopupStep<*>? {
                    if (finalChoice) {
                        when (selectedValue) {
                            "Capture problems in this file" -> {
                                captureFileProblems(project, file!!)
                            }
                            "Capture all project problems" -> {
                                captureAllProblems(project)
                            }
                        }
                    }
                    return FINAL_CHOICE
                }
            }
        ).showInBestPositionFor(e.dataContext)
    }

    override fun update(e: AnActionEvent) {
        // Only enable if a project is open
        e.presentation.isEnabled = e.project != null
    }

    /**
     * Capture problems for a specific file
     */
    private fun captureFileProblems(project: Project, file: com.intellij.openapi.vfs.VirtualFile) {
        val service = ProblemGrabberService.getInstance(project)
        val problems = service.getProblemsForFile(file)

        if (problems.isEmpty()) {
            Messages.showInfoMessage(
                project,
                "No problems found in file: ${file.name}",
                "No Problems Found"
            )
            return
        }

        service.copyToClipboard(problems)

        Messages.showInfoMessage(
            project,
            "${problems.size} problem(s) copied to clipboard in LLM-friendly format.",
            "Problems Captured"
        )
    }

    /**
     * Capture all problems in the project
     */
    private fun captureAllProblems(project: Project) {
        val service = ProblemGrabberService.getInstance(project)
        val problems = service.getProblems()

        if (problems.isEmpty()) {
            Messages.showInfoMessage(
                project,
                "No problems found in the project.",
                "No Problems Found"
            )
            return
        }

        service.copyToClipboard(problems)

        Messages.showInfoMessage(
            project,
            "${problems.size} problem(s) copied to clipboard in LLM-friendly format.",
            "Problems Captured"
        )
    }
}
