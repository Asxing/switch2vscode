package com.github.qczone.switch2vscode.validation

import com.github.qczone.switch2vscode.model.EditorType
import com.github.qczone.switch2vscode.model.ValidationResult
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.SystemInfo
import java.io.File

/**
 * 编辑器路径验证器
 * 负责验证编辑器路径的有效性并提供修复建议
 */
class PathValidator {
    private val logger = Logger.getInstance(PathValidator::class.java)

    /**
     * 验证编辑器路径
     * @param path 要验证的路径
     * @param editorType 编辑器类型（可选）
     * @return 验证结果
     */
    fun validate(path: String, editorType: EditorType? = null): ValidationResult {
        return try {
            validateInternal(path, editorType)
        } catch (e: Exception) {
            logger.warn("Validation failed for path: $path", e)
            ValidationResult.Invalid(
                "Validation failed: ${e.message}",
                "Please check the path and try again"
            )
        }
    }

    private fun validateInternal(path: String, editorType: EditorType?): ValidationResult {
        // 1. 检查路径是否为空
        if (path.isBlank()) {
            return ValidationResult.Invalid(
                "Path cannot be empty",
                "Please enter a valid path to the editor executable"
            )
        }

        val file = File(path)

        // 2. 检查文件是否存在
        if (!file.exists()) {
            return ValidationResult.Invalid(
                "File does not exist: $path",
                getSuggestionForNonExistentPath(path, editorType)
            )
        }

        // 3. 检查是否为目录（macOS .app 包除外）
        if (file.isDirectory && !(SystemInfo.isMac && path.endsWith(".app"))) {
            return ValidationResult.Invalid(
                "Path points to a directory, not an executable file",
                "Please select the executable file inside the directory"
            )
        }

        // 4. 检查可执行权限
        if (!isExecutable(file, path)) {
            return ValidationResult.Warning(
                "File may not be executable"
            )
        }

        // 5. 检查编辑器兼容性
        val compatibilityResult = checkEditorCompatibility(path, editorType)
        if (compatibilityResult !is ValidationResult.Valid) {
            return compatibilityResult
        }

        // 6. 基本验证通过，跳过版本检查避免意外启动编辑器
        // 版本检查可能会触发编辑器启动，在验证阶段不需要

        return ValidationResult.Valid
    }

    private fun isExecutable(file: File, path: String): Boolean {
        return when {
            SystemInfo.isMac && path.endsWith(".app") -> {
                // macOS .app 包检查
                val executablePath = findMacAppExecutable(path)
                executablePath != null && File(executablePath).canExecute()
            }
            SystemInfo.isWindows -> {
                // Windows: 检查文件扩展名和权限
                file.canRead() && (path.endsWith(".exe", ignoreCase = true) || file.canExecute())
            }
            else -> {
                // Linux/Unix: 检查执行权限
                file.canExecute()
            }
        }
    }

    private fun findMacAppExecutable(appPath: String): String? {
        return try {
            val contentsDir = File(appPath, "Contents")
            val macOSDir = File(contentsDir, "MacOS")

            if (macOSDir.exists()) {
                // 查找可执行文件
                macOSDir.listFiles()?.find { it.canExecute() }?.absolutePath
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun checkEditorCompatibility(path: String, expectedType: EditorType?): ValidationResult {
        if (expectedType == null || expectedType == EditorType.CUSTOM) {
            return ValidationResult.Valid
        }

        val detectedType = EditorType.detectFromPath(path)

        return when {
            detectedType == expectedType -> ValidationResult.Valid
            detectedType == EditorType.CUSTOM -> ValidationResult.Warning(
                "Could not determine editor type from path"
            )
            else -> ValidationResult.Warning(
                "Path appears to be for ${detectedType.displayName}, but ${expectedType.displayName} was expected"
            )
        }
    }

    private fun checkVersion(path: String): ValidationResult {
        return try {
            val version = getEditorVersion(path)
            if (version != null) {
                ValidationResult.Valid
            } else {
                ValidationResult.Warning("Could not determine editor version")
            }
        } catch (e: Exception) {
            ValidationResult.Warning("Version check failed: ${e.message}")
        }
    }

    private fun getEditorVersion(path: String): String? {
        return try {
            val command = if (SystemInfo.isMac && path.endsWith(".app")) {
                // macOS .app 包需要特殊处理
                val execPath = findMacAppExecutable(path) ?: return null
                arrayOf(execPath, "--version")
            } else {
                arrayOf(path, "--version")
            }

            val process = ProcessBuilder(*command)
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().use { it.readLine() }

            // 添加超时机制，避免长时间等待
            val completed = process.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)

            if (completed && process.exitValue() == 0 && !output.isNullOrBlank()) {
                output.trim()
            } else {
                if (!completed) {
                    process.destroyForcibly() // 强制终止超时进程
                    logger.debug("Version check timed out for $path")
                }
                null
            }
        } catch (e: Exception) {
            logger.debug("Failed to get version for $path", e)
            null
        }
    }

    private fun getSuggestionForNonExistentPath(path: String, editorType: EditorType?): String {
        val suggestions = mutableListOf<String>()

        // 通用建议
        suggestions.add("Verify the path is correct")

        // 平台特定建议 - 专注于使用 which/where 命令
        when {
            SystemInfo.isMac -> {
                suggestions.add("Use 'which <command>' in terminal to find executable paths")
                if (editorType != null) {
                    editorType.executableNames.forEach { execName ->
                        suggestions.add("Try 'which $execName' in terminal")
                    }
                } else {
                    suggestions.add("Try 'which code' for VS Code, 'which cursor' for Cursor, etc.")
                }
            }
            SystemInfo.isWindows -> {
                suggestions.add("Use 'where <command>' in Command Prompt to find executable paths")
                if (editorType != null) {
                    editorType.executableNames.forEach { execName ->
                        suggestions.add("Try 'where $execName' in Command Prompt")
                    }
                } else {
                    suggestions.add("Try 'where code' for VS Code, 'where cursor' for Cursor, etc.")
                }
            }
            else -> {
                suggestions.add("Use 'which <command>' in terminal to find executable paths")
                if (editorType != null) {
                    editorType.executableNames.forEach { execName ->
                        suggestions.add("Try 'which $execName' in terminal")
                    }
                } else {
                    suggestions.add("Try 'which code' for VS Code, 'which cursor' for Cursor, etc.")
                }
            }
        }

        suggestions.add("Use the 'Refresh' button to auto-discover installed editors")
        suggestions.add("Ensure the editor is installed and added to your system PATH")

        return suggestions.joinToString("; ")
    }

    /**
     * 快速验证路径是否存在
     */
    fun quickValidate(path: String): Boolean {
        return try {
            path.isNotBlank() && File(path).exists()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取路径的基本信息
     */
    fun getPathInfo(path: String): PathInfo? {
        return try {
            val file = File(path)
            if (!file.exists()) return null

            PathInfo(
                exists = true,
                isDirectory = file.isDirectory,
                isExecutable = isExecutable(file, path),
                size = if (file.isFile) file.length() else 0,
                lastModified = file.lastModified(),
                version = getEditorVersion(path)
            )
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * 路径信息数据类
 */
data class PathInfo(
    val exists: Boolean,
    val isDirectory: Boolean,
    val isExecutable: Boolean,
    val size: Long,
    val lastModified: Long,
    val version: String?
)