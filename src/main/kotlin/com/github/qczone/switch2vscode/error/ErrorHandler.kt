package com.github.qczone.switch2vscode.error

import com.github.qczone.switch2vscode.model.EditorConfig
import com.github.qczone.switch2vscode.model.EditorType
import com.github.qczone.switch2vscode.model.ValidationResult
import com.github.qczone.switch2vscode.settings.AppSettingsConfigurable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.SystemInfo
import java.io.File

/**
 * 智能错误处理器
 * 提供分层错误分类、智能诊断和自动修复建议
 */
object ErrorHandler {
    private val logger = Logger.getInstance(ErrorHandler::class.java)

    /**
     * 处理编辑器执行错误
     * @param project 当前项目
     * @param error 错误信息
     * @param editorConfig 编辑器配置
     * @param context 错误上下文
     */
    fun handleEditorError(
        project: Project,
        error: Throwable,
        editorConfig: EditorConfig,
        context: ErrorContext
    ) {
        logger.warn("Editor execution failed", error)

        val errorType = classifyError(error, editorConfig, context)
        val diagnosis = diagnoseError(errorType, editorConfig, context)
        val recovery = suggestRecovery(errorType, editorConfig, context)

        showErrorDialog(project, errorType, diagnosis, recovery, editorConfig)
    }

    /**
     * 处理验证错误
     * @param project 当前项目
     * @param validation 验证结果
     * @param editorConfig 编辑器配置
     */
    fun handleValidationError(
        project: Project,
        validation: ValidationResult,
        editorConfig: EditorConfig
    ) {
        when (validation) {
            is ValidationResult.Invalid -> {
                val errorType = EditorErrorType.CONFIGURATION_ERROR
                val diagnosis = ErrorDiagnosis(
                    category = ErrorCategory.CONFIGURATION,
                    severity = ErrorSeverity.HIGH,
                    message = validation.reason,
                    technicalDetails = "Path validation failed for ${editorConfig.executablePath}",
                    possibleCauses = listOf(
                        "File does not exist",
                        "Insufficient permissions",
                        "Invalid file path",
                        "Editor not installed"
                    )
                )

                val recovery = RecoveryStrategy(
                    canAutoRecover = false,
                    immediateActions = listOf(
                        RecoveryAction.OPEN_SETTINGS,
                        RecoveryAction.REFRESH_DISCOVERY
                    ),
                    suggestions = listOf(
                        validation.suggestion ?: "Please check the editor path in settings"
                    )
                )

                showErrorDialog(project, errorType, diagnosis, recovery, editorConfig)
            }
            is ValidationResult.Warning -> {
                showWarningDialog(project, validation.message, editorConfig)
            }
            is ValidationResult.Valid -> {
                // 不应该到达这里
                logger.warn("handleValidationError called with Valid result")
            }
        }
    }

    /**
     * 错误分类
     */
    private fun classifyError(
        error: Throwable,
        editorConfig: EditorConfig,
        context: ErrorContext
    ): EditorErrorType {
        return when {
            error is SecurityException -> EditorErrorType.PERMISSION_ERROR
            error.message?.contains("cannot run program", ignoreCase = true) == true ->
                EditorErrorType.EXECUTABLE_NOT_FOUND
            error.message?.contains("no such file", ignoreCase = true) == true ->
                EditorErrorType.FILE_NOT_FOUND
            error.message?.contains("access denied", ignoreCase = true) == true ->
                EditorErrorType.PERMISSION_ERROR
            error.message?.contains("timeout", ignoreCase = true) == true ->
                EditorErrorType.EXECUTION_TIMEOUT
            context.operation == OperationType.FILE_OPEN && !File(context.filePath ?: "").exists() ->
                EditorErrorType.TARGET_FILE_NOT_FOUND
            else -> EditorErrorType.UNKNOWN_ERROR
        }
    }

    /**
     * 错误诊断
     */
    private fun diagnoseError(
        errorType: EditorErrorType,
        editorConfig: EditorConfig,
        context: ErrorContext
    ): ErrorDiagnosis {
        return when (errorType) {
            EditorErrorType.EXECUTABLE_NOT_FOUND -> ErrorDiagnosis(
                category = ErrorCategory.CONFIGURATION,
                severity = ErrorSeverity.HIGH,
                message = "Editor executable not found",
                technicalDetails = "Cannot execute: ${editorConfig.executablePath}",
                possibleCauses = listOf(
                    "Editor is not installed",
                    "Incorrect path configuration",
                    "Editor was uninstalled or moved",
                    "PATH environment variable not set"
                )
            )

            EditorErrorType.PERMISSION_ERROR -> ErrorDiagnosis(
                category = ErrorCategory.SYSTEM,
                severity = ErrorSeverity.MEDIUM,
                message = "Permission denied",
                technicalDetails = "Insufficient permissions to execute: ${editorConfig.executablePath}",
                possibleCauses = listOf(
                    "File is not executable",
                    "User lacks execution permissions",
                    "File is blocked by security software",
                    "macOS Gatekeeper restrictions"
                )
            )

            EditorErrorType.FILE_NOT_FOUND -> ErrorDiagnosis(
                category = ErrorCategory.CONFIGURATION,
                severity = ErrorSeverity.HIGH,
                message = "Editor file not found",
                technicalDetails = "File does not exist: ${editorConfig.executablePath}",
                possibleCauses = listOf(
                    "Path is incorrect",
                    "Editor was uninstalled",
                    "File was moved or deleted",
                    "Network path is unavailable"
                )
            )

            EditorErrorType.TARGET_FILE_NOT_FOUND -> ErrorDiagnosis(
                category = ErrorCategory.USAGE,
                severity = ErrorSeverity.LOW,
                message = "Target file not found",
                technicalDetails = "Cannot open file: ${context.filePath}",
                possibleCauses = listOf(
                    "File was deleted",
                    "File was moved",
                    "Network path is unavailable",
                    "Temporary file expired"
                )
            )

            EditorErrorType.EXECUTION_TIMEOUT -> ErrorDiagnosis(
                category = ErrorCategory.SYSTEM,
                severity = ErrorSeverity.MEDIUM,
                message = "Editor execution timeout",
                technicalDetails = "Editor failed to start within timeout period",
                possibleCauses = listOf(
                    "System is overloaded",
                    "Editor is taking too long to initialize",
                    "Antivirus software interference",
                    "Disk I/O issues"
                )
            )

            EditorErrorType.CONFIGURATION_ERROR -> ErrorDiagnosis(
                category = ErrorCategory.CONFIGURATION,
                severity = ErrorSeverity.HIGH,
                message = "Configuration error",
                technicalDetails = "Invalid editor configuration",
                possibleCauses = listOf(
                    "Missing required configuration",
                    "Invalid parameter values",
                    "Corrupted settings file"
                )
            )

            EditorErrorType.UNKNOWN_ERROR -> ErrorDiagnosis(
                category = ErrorCategory.UNKNOWN,
                severity = ErrorSeverity.MEDIUM,
                message = "Unknown error occurred",
                technicalDetails = context.error?.message ?: "No additional details available",
                possibleCauses = listOf(
                    "Unexpected system condition",
                    "Editor compatibility issue",
                    "Temporary system problem"
                )
            )
        }
    }

    /**
     * 建议恢复策略
     */
    private fun suggestRecovery(
        errorType: EditorErrorType,
        editorConfig: EditorConfig,
        context: ErrorContext
    ): RecoveryStrategy {
        return when (errorType) {
            EditorErrorType.EXECUTABLE_NOT_FOUND,
            EditorErrorType.FILE_NOT_FOUND -> RecoveryStrategy(
                canAutoRecover = true,
                immediateActions = listOf(
                    RecoveryAction.REFRESH_DISCOVERY,
                    RecoveryAction.OPEN_SETTINGS,
                    RecoveryAction.DOWNLOAD_EDITOR
                ),
                suggestions = listOf(
                    "Use 'Refresh' to auto-discover installed editors",
                    "Manually configure the correct path",
                    "Install ${editorConfig.displayName} if not present",
                    getPlatformSpecificInstallationGuide(editorConfig.id)
                )
            )

            EditorErrorType.PERMISSION_ERROR -> RecoveryStrategy(
                canAutoRecover = false,
                immediateActions = listOf(
                    RecoveryAction.FIX_PERMISSIONS,
                    RecoveryAction.OPEN_SETTINGS
                ),
                suggestions = getPlatformSpecificPermissionFix()
            )

            EditorErrorType.TARGET_FILE_NOT_FOUND -> RecoveryStrategy(
                canAutoRecover = false,
                immediateActions = listOf(
                    RecoveryAction.RETRY_OPERATION
                ),
                suggestions = listOf(
                    "Verify the file still exists",
                    "Refresh the project view",
                    "Check if the file was moved or renamed"
                )
            )

            EditorErrorType.EXECUTION_TIMEOUT -> RecoveryStrategy(
                canAutoRecover = true,
                immediateActions = listOf(
                    RecoveryAction.RETRY_OPERATION,
                    RecoveryAction.CHECK_SYSTEM_RESOURCES
                ),
                suggestions = listOf(
                    "Wait a moment and try again",
                    "Close unnecessary applications",
                    "Check system resources (CPU, Memory, Disk)",
                    "Temporarily disable antivirus real-time scanning"
                )
            )

            EditorErrorType.CONFIGURATION_ERROR -> RecoveryStrategy(
                canAutoRecover = true,
                immediateActions = listOf(
                    RecoveryAction.RESET_CONFIGURATION,
                    RecoveryAction.OPEN_SETTINGS
                ),
                suggestions = listOf(
                    "Reset to default configuration",
                    "Re-run auto-discovery",
                    "Manually reconfigure editor settings"
                )
            )

            EditorErrorType.UNKNOWN_ERROR -> RecoveryStrategy(
                canAutoRecover = false,
                immediateActions = listOf(
                    RecoveryAction.RETRY_OPERATION,
                    RecoveryAction.OPEN_SETTINGS,
                    RecoveryAction.CONTACT_SUPPORT
                ),
                suggestions = listOf(
                    "Try again in a moment",
                    "Check system logs for more details",
                    "Restart the IDE if the problem persists",
                    "Report the issue with log details"
                )
            )
        }
    }

    /**
     * 显示错误对话框
     */
    private fun showErrorDialog(
        project: Project,
        errorType: EditorErrorType,
        diagnosis: ErrorDiagnosis,
        recovery: RecoveryStrategy,
        editorConfig: EditorConfig
    ) {
        val message = buildErrorMessage(diagnosis, recovery)
        val actions = buildActionButtons(recovery)

        val result = Messages.showDialog(
            project,
            message,
            "${editorConfig.displayName} Error",
            actions.toTypedArray(),
            0,
            getErrorIcon(diagnosis.severity)
        )

        handleUserAction(project, result, recovery.immediateActions, editorConfig)
    }

    /**
     * 显示警告对话框
     */
    private fun showWarningDialog(project: Project, message: String, editorConfig: EditorConfig) {
        Messages.showWarningDialog(
            project,
            message,
            "${editorConfig.displayName} Warning"
        )
    }

    /**
     * 构建错误消息
     */
    private fun buildErrorMessage(diagnosis: ErrorDiagnosis, recovery: RecoveryStrategy): String {
        return buildString {
            appendLine(diagnosis.message)
            appendLine()

            if (diagnosis.technicalDetails.isNotEmpty()) {
                appendLine("Technical Details:")
                appendLine(diagnosis.technicalDetails)
                appendLine()
            }

            if (diagnosis.possibleCauses.isNotEmpty()) {
                appendLine("Possible Causes:")
                diagnosis.possibleCauses.forEach { cause ->
                    appendLine("• $cause")
                }
                appendLine()
            }

            if (recovery.suggestions.isNotEmpty()) {
                appendLine("Suggested Solutions:")
                recovery.suggestions.forEach { suggestion ->
                    appendLine("• $suggestion")
                }
            }
        }
    }

    /**
     * 构建操作按钮
     */
    private fun buildActionButtons(recovery: RecoveryStrategy): List<String> {
        val actions = mutableListOf<String>()

        if (recovery.immediateActions.contains(RecoveryAction.OPEN_SETTINGS)) {
            actions.add("Open Settings")
        }
        if (recovery.immediateActions.contains(RecoveryAction.REFRESH_DISCOVERY)) {
            actions.add("Refresh Discovery")
        }
        if (recovery.immediateActions.contains(RecoveryAction.RETRY_OPERATION)) {
            actions.add("Retry")
        }

        actions.add("Cancel")
        return actions
    }

    /**
     * 处理用户操作
     */
    private fun handleUserAction(
        project: Project,
        result: Int,
        actions: List<RecoveryAction>,
        editorConfig: EditorConfig
    ) {
        when (result) {
            0 -> {
                when {
                    actions.contains(RecoveryAction.OPEN_SETTINGS) -> {
                        ShowSettingsUtil.getInstance().showSettingsDialog(project, AppSettingsConfigurable::class.java)
                    }
                    actions.contains(RecoveryAction.REFRESH_DISCOVERY) -> {
                        // 触发重新发现
                        logger.info("User requested refresh discovery")
                    }
                    actions.contains(RecoveryAction.RETRY_OPERATION) -> {
                        // 重试操作
                        logger.info("User requested retry operation")
                    }
                }
            }
        }
    }

    /**
     * 获取错误图标
     */
    private fun getErrorIcon(severity: ErrorSeverity) = when (severity) {
        ErrorSeverity.HIGH -> Messages.getErrorIcon()
        ErrorSeverity.MEDIUM -> Messages.getWarningIcon()
        ErrorSeverity.LOW -> Messages.getInformationIcon()
    }

    /**
     * 获取平台特定的安装指南
     */
    private fun getPlatformSpecificInstallationGuide(editorId: String): String {
        val editorName = EditorType.fromId(editorId)?.displayName ?: editorId

        return when {
            SystemInfo.isMac -> "Install $editorName from the Mac App Store or official website"
            SystemInfo.isWindows -> "Download $editorName from the official website or Microsoft Store"
            else -> "Install $editorName using your package manager or from the official website"
        }
    }

    /**
     * 获取平台特定的权限修复建议
     */
    private fun getPlatformSpecificPermissionFix(): List<String> {
        return when {
            SystemInfo.isMac -> listOf(
                "Right-click the app and select 'Open' to bypass Gatekeeper",
                "Check System Preferences > Security & Privacy",
                "Grant executable permissions: chmod +x /path/to/editor"
            )
            SystemInfo.isWindows -> listOf(
                "Run as Administrator",
                "Check file properties and unblock if necessary",
                "Verify antivirus software isn't blocking execution"
            )
            else -> listOf(
                "Grant executable permissions: chmod +x /path/to/editor",
                "Check file ownership and permissions",
                "Ensure the file is not mounted with noexec option"
            )
        }
    }
}

/**
 * 错误上下文信息
 */
data class ErrorContext(
    val operation: OperationType,
    val filePath: String? = null,
    val projectPath: String? = null,
    val error: Throwable? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 操作类型枚举
 */
enum class OperationType {
    FILE_OPEN,
    PROJECT_OPEN,
    EDITOR_VALIDATION,
    DISCOVERY
}

/**
 * 编辑器错误类型
 */
enum class EditorErrorType {
    EXECUTABLE_NOT_FOUND,
    PERMISSION_ERROR,
    FILE_NOT_FOUND,
    TARGET_FILE_NOT_FOUND,
    EXECUTION_TIMEOUT,
    CONFIGURATION_ERROR,
    UNKNOWN_ERROR
}

/**
 * 错误分类
 */
enum class ErrorCategory {
    CONFIGURATION,
    SYSTEM,
    USAGE,
    UNKNOWN
}

/**
 * 错误严重程度
 */
enum class ErrorSeverity {
    HIGH,    // 阻止功能使用
    MEDIUM,  // 影响用户体验
    LOW      // 轻微问题
}

/**
 * 错误诊断信息
 */
data class ErrorDiagnosis(
    val category: ErrorCategory,
    val severity: ErrorSeverity,
    val message: String,
    val technicalDetails: String,
    val possibleCauses: List<String>
)

/**
 * 恢复策略
 */
data class RecoveryStrategy(
    val canAutoRecover: Boolean,
    val immediateActions: List<RecoveryAction>,
    val suggestions: List<String>
)

/**
 * 恢复操作
 */
enum class RecoveryAction {
    OPEN_SETTINGS,
    REFRESH_DISCOVERY,
    RETRY_OPERATION,
    FIX_PERMISSIONS,
    DOWNLOAD_EDITOR,
    RESET_CONFIGURATION,
    CHECK_SYSTEM_RESOURCES,
    CONTACT_SUPPORT
}