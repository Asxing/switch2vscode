package com.github.qczone.switch2vscode.actions

import com.github.qczone.switch2vscode.settings.AppSettingsState
import com.github.qczone.switch2vscode.utils.WindowUtils
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.util.SystemInfo
import java.util.Locale

class OpenFileInVSCodeAction : AnAction() {
    private val logger = Logger.getInstance(OpenFileInVSCodeAction::class.java)

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return
        val virtualFile: VirtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        
        val editor: Editor? = e.getData(CommonDataKeys.EDITOR)
        
        val line = editor?.caretModel?.logicalPosition?.line?.plus(1) ?: 1
        val column = editor?.caretModel?.logicalPosition?.column?.plus(1) ?: 1
        
        val filePath = virtualFile.path
        val settings = AppSettingsState.getInstance()
        val vscodePath = settings.vscodePath

        // Ensure VS Code opens the project first so file contexts are available.
        project.basePath?.let { projectPath ->
            val projectCommand = buildProjectCommand(vscodePath, projectPath)
            try {
                logger.info("Executing project command before file: ${projectCommand.joinToString(" ")}")
                ProcessBuilder(*projectCommand).start()
            } catch (ex: Exception) {
                logger.warn("Failed to open project in VS Code before file", ex)
            }
        }

        val command = buildFileCommand(vscodePath, filePath, line, column)

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
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        
        e.presentation.isEnabledAndVisible = project != null && 
                                           virtualFile != null && 
                                           !virtualFile.isDirectory
    }

    private fun isMacAppPath(path: String): Boolean {
        val normalized = path.lowercase(Locale.getDefault())
        return normalized.endsWith(".app") || normalized.contains("visual studio code")
    }

    private fun buildFileCommand(vscodePath: String, filePath: String, line: Int, column: Int): Array<String> {
        return when {
            SystemInfo.isMac && isMacAppPath(vscodePath) -> arrayOf("open", "-a", vscodePath, "--args", "--goto", "$filePath:$line:$column")
            SystemInfo.isMac -> arrayOf(vscodePath, "--goto", "$filePath:$line:$column")
            SystemInfo.isWindows -> arrayOf("cmd", "/c", vscodePath, "--goto", "$filePath:$line:$column")
            else -> arrayOf(vscodePath, "--goto", "$filePath:$line:$column")
        }
    }

    private fun buildProjectCommand(vscodePath: String, projectPath: String): Array<String> {
        return when {
            SystemInfo.isMac && isMacAppPath(vscodePath) -> arrayOf("open", "-a", vscodePath, "--args", projectPath)
            SystemInfo.isMac -> arrayOf(vscodePath, projectPath)
            SystemInfo.isWindows -> arrayOf("cmd", "/c", vscodePath, projectPath)
            else -> arrayOf(vscodePath, projectPath)
        }
    }
} 
