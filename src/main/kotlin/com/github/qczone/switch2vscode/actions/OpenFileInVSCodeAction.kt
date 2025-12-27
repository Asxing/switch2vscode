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
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile

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
                executeCommands(project, editorInstance, virtualFile, editor)
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
                    executeCommands(project, editorInstance, virtualFile, editor)
                }
            }
        }
    }

    private fun executeCommands(
        project: Project,
        editorInstance: com.github.qczone.switch2vscode.editor.Editor,
        virtualFile: VirtualFile,
        editor: Editor?
    ) {
        val line = editor?.caretModel?.logicalPosition?.line?.plus(1) ?: 1
        val column = editor?.caretModel?.logicalPosition?.column?.plus(1) ?: 1
        val filePath = virtualFile.path

        try {
            // 首先打开项目（如果有项目路径）
            project.basePath?.let { projectPath ->
                val projectCommand = editorInstance.buildProjectCommand(projectPath)
                try {
                    logger.info("Executing project command: ${projectCommand.joinToString(" ")}")
                    ProcessBuilder(*projectCommand).start()

                    // 给项目打开一点时间
                    Thread.sleep(500)
                } catch (ex: Exception) {
                    logger.warn("Failed to open project in ${editorInstance.displayName}", ex)
                }
            }

            // 然后打开文件到指定位置
            val fileCommand = editorInstance.buildFileCommand(filePath, line, column)

            logger.info("Executing file command: ${fileCommand.joinToString(" ")}")
            ProcessBuilder(*fileCommand).start()

            // 激活编辑器窗口
            WindowUtils.activateEditorWindow(editorInstance.id)

        } catch (ex: Exception) {
            logger.error("Failed to execute ${editorInstance.displayName} command: ${ex.message}", ex)

            val context = ErrorContext(
                operation = OperationType.FILE_OPEN,
                filePath = filePath,
                projectPath = project.basePath,
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
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val settings = AppSettingsState.getInstance()

        // 动态更新Action的显示文本
        val editorConfig = settings.getSelectedEditorConfig()
        if (editorConfig != null) {
            e.presentation.text = "Open in ${editorConfig.displayName}"
            e.presentation.description = "Open the current file in ${editorConfig.displayName}"
        } else {
            e.presentation.text = "Open in External Editor"
            e.presentation.description = "Open the current file in external editor (not configured)"
        }

        e.presentation.isEnabledAndVisible = project != null &&
                                           virtualFile != null &&
                                           !virtualFile.isDirectory
    }
} 
