package com.github.qczone.switch2vscode.actions

import com.github.qczone.switch2vscode.settings.AppSettingsState
import com.github.qczone.switch2vscode.utils.WindowUtils
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import java.util.Locale

class OpenProjectInVSCodeAction : AnAction() {
    private val logger = Logger.getInstance(OpenProjectInVSCodeAction::class.java)

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return
        val projectPath = project.basePath ?: return

        val settings = AppSettingsState.getInstance()
        val vscodePath = settings.vscodePath

        val command = when {
            SystemInfo.isMac && isMacAppPath(vscodePath) -> arrayOf("open", "-a", vscodePath, "--args", projectPath)
            SystemInfo.isMac -> arrayOf(vscodePath, projectPath)
            SystemInfo.isWindows -> arrayOf("cmd", "/c", vscodePath, projectPath)
            else -> arrayOf(vscodePath, projectPath)
        }
        try {
            logger.info("Executing command: ${command.joinToString(" ")}")
            ProcessBuilder(*command).start()
        } catch (ex: Exception) {
            logger.error("Failed to execute VS Code command: ${ex.message}", ex)
            com.intellij.openapi.ui.Messages.showErrorDialog(
                project,
                """
                ${ex.message}
                
                Please check:
                1. VS Code path is correctly configured in Settings > Tools > Switch2VSCode
                2. VS Code is properly installed on your system
                3. The configured path points to a valid VS Code executable
                """.trimIndent(),
                "Error"
            )
        }

        WindowUtils.activeWindow()
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabledAndVisible = project != null
    }

    private fun isMacAppPath(path: String): Boolean {
        val normalized = path.lowercase(Locale.getDefault())
        return normalized.endsWith(".app") || normalized.contains("visual studio code")
    }
} 
