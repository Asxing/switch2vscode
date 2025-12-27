package com.github.qczone.switch2vscode.actions

import com.github.qczone.switch2vscode.editor.EditorFactory
import com.github.qczone.switch2vscode.error.ErrorContext
import com.github.qczone.switch2vscode.error.ErrorHandler
import com.github.qczone.switch2vscode.error.OperationType
import com.github.qczone.switch2vscode.model.ValidationResult
import com.github.qczone.switch2vscode.settings.AppSettingsState
import com.github.qczone.switch2vscode.settings.AppSettingsConfigurable
import com.github.qczone.switch2vscode.utils.WindowUtils
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages

class OpenProjectInVSCodeAction : AnAction() {
    private val logger = Logger.getInstance(OpenProjectInVSCodeAction::class.java)

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return
        val projectPath = project.basePath ?: return

        val settings = AppSettingsState.getInstance()

        // 获取当前选中的编辑器配置
        val editorConfig = settings.getSelectedEditorConfig()

        if (editorConfig == null) {
            showConfigurationDialog(project)
            return
        }

        // 创建编辑器实例
        val editorInstance = EditorFactory.createEditor(editorConfig)

        // 验证编辑器配置
        val validation = editorInstance.validate()

        when (validation) {
            is ValidationResult.Valid -> {
                executeProjectCommand(project, editorInstance, projectPath)
            }
            is ValidationResult.Invalid -> {
                ErrorHandler.handleValidationError(project, validation, editorConfig)
            }
            is ValidationResult.Warning -> {
                // 对于警告，询问用户是否继续
                val result = Messages.showYesNoDialog(
                    project,
                    "${validation.message}\n\nDo you want to continue anyway?",
                    "Warning - ${editorConfig.displayName}",
                    Messages.getWarningIcon()
                )

                if (result == Messages.YES) {
                    executeProjectCommand(project, editorInstance, projectPath)
                }
            }
        }
    }

    private fun executeProjectCommand(
        project: Project,
        editorInstance: com.github.qczone.switch2vscode.editor.Editor,
        projectPath: String
    ) {
        try {
            val command = editorInstance.buildProjectCommand(projectPath)

            logger.info("Executing project command: ${command.joinToString(" ")}")
            ProcessBuilder(*command).start()

            // 激活编辑器窗口
            WindowUtils.activateEditorWindow(editorInstance.id)

        } catch (ex: Exception) {
            logger.error("Failed to execute ${editorInstance.displayName} command: ${ex.message}", ex)

            val context = ErrorContext(
                operation = OperationType.PROJECT_OPEN,
                projectPath = projectPath,
                error = ex
            )

            ErrorHandler.handleEditorError(project, ex, editorInstance.config, context)
        }
    }

    private fun showConfigurationDialog(project: Project) {
        val result = Messages.showYesNoDialog(
            project,
            "No editor is configured. Would you like to open settings to configure an editor?",
            "Editor Not Configured",
            "Open Settings",
            "Cancel",
            Messages.getQuestionIcon()
        )

        if (result == Messages.YES) {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, AppSettingsConfigurable::class.java)
        }
    }


    override fun update(e: AnActionEvent) {
        val project = e.project
        val settings = AppSettingsState.getInstance()

        // 动态更新Action的显示文本
        val editorConfig = settings.getSelectedEditorConfig()
        if (editorConfig != null) {
            e.presentation.text = "Open Project in ${editorConfig.displayName}"
            e.presentation.description = "Open the current project in ${editorConfig.displayName}"
        } else {
            e.presentation.text = "Open Project in External Editor"
            e.presentation.description = "Open the current project in external editor (not configured)"
        }

        e.presentation.isEnabledAndVisible = project != null
    }
} 
