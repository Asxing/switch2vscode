package com.github.qczone.switch2vscode.editor

import com.github.qczone.switch2vscode.model.EditorConfig
import com.github.qczone.switch2vscode.model.ValidationResult
import com.github.qczone.switch2vscode.validation.PathValidator
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.SystemInfo
import java.io.File

/**
 * VS Code编辑器实现
 * 支持标准的VS Code命令行参数和功能
 */
class VSCodeEditor(private val editorConfig: EditorConfig) : Editor {
    private val logger = Logger.getInstance(VSCodeEditor::class.java)
    private val pathValidator = PathValidator()

    override val id: String get() = editorConfig.id
    override val displayName: String get() = editorConfig.displayName
    override val executablePath: String
        get() = if (SystemInfo.isMac && editorConfig.executablePath.endsWith(".app")) {
            // macOS .app包需要找到实际的可执行文件
            val executableName = "Visual Studio Code"
            "${editorConfig.executablePath}/Contents/MacOS/$executableName"
        } else {
            editorConfig.executablePath
        }
    override val version: String? get() = editorConfig.version
    override val config: EditorConfig get() = editorConfig

    override fun buildFileCommand(filePath: String, line: Int, column: Int): Array<String> {
        val commands = mutableListOf<String>()

        // 基础命令
        commands.add(executablePath)

        // 添加goto参数（行:列）
        if (line > 0 && column > 0) {
            commands.add("--goto")
            commands.add("$filePath:$line:$column")
        } else if (line > 0) {
            commands.add("--goto")
            commands.add("$filePath:$line")
        } else {
            commands.add(filePath)
        }

        // 添加自定义参数
        commands.addAll(editorConfig.customArgs)

        return commands.toTypedArray()
    }

    override fun buildProjectCommand(projectPath: String): Array<String> {
        val commands = mutableListOf<String>()

        commands.add(executablePath)
        commands.add(projectPath)

        // 添加自定义参数
        commands.addAll(editorConfig.customArgs)

        return commands.toTypedArray()
    }

    override fun validate(): ValidationResult {
        return pathValidator.validate(editorConfig.executablePath, com.github.qczone.switch2vscode.model.EditorType.VSCODE)
    }

    override fun supportsFeature(feature: EditorFeature): Boolean {
        return when (feature) {
            EditorFeature.GOTO_COMMAND -> true
            EditorFeature.WAIT_MODE -> true
            EditorFeature.NEW_WINDOW -> true
            EditorFeature.WORKSPACE -> true
            EditorFeature.EXTENSIONS -> true
            EditorFeature.DEBUGGING -> true
            EditorFeature.INTEGRATED_TERMINAL -> true
            EditorFeature.GIT_INTEGRATION -> true
            EditorFeature.LANGUAGE_SERVER_PROTOCOL -> true
        }
    }

    override fun getSupportedExtensions(): List<String> {
        return listOf("*") // VS Code支持所有文件类型
    }
}

/**
 * Cursor编辑器实现
 * 基于VS Code，支持相同的命令行参数
 */
class CursorEditor(private val editorConfig: EditorConfig) : Editor {
    private val logger = Logger.getInstance(CursorEditor::class.java)
    private val pathValidator = PathValidator()

    override val id: String get() = editorConfig.id
    override val displayName: String get() = editorConfig.displayName
    override val executablePath: String
        get() = if (SystemInfo.isMac && editorConfig.executablePath.endsWith(".app")) {
            "${editorConfig.executablePath}/Contents/MacOS/Cursor"
        } else {
            editorConfig.executablePath
        }
    override val version: String? get() = editorConfig.version
    override val config: EditorConfig get() = editorConfig

    override fun buildFileCommand(filePath: String, line: Int, column: Int): Array<String> {
        val commands = mutableListOf<String>()

        commands.add(executablePath)

        // Cursor支持VS Code相同的goto语法
        if (line > 0 && column > 0) {
            commands.add("--goto")
            commands.add("$filePath:$line:$column")
        } else if (line > 0) {
            commands.add("--goto")
            commands.add("$filePath:$line")
        } else {
            commands.add(filePath)
        }

        commands.addAll(editorConfig.customArgs)

        return commands.toTypedArray()
    }

    override fun buildProjectCommand(projectPath: String): Array<String> {
        val commands = mutableListOf<String>()

        commands.add(executablePath)
        commands.add(projectPath)
        commands.addAll(editorConfig.customArgs)

        return commands.toTypedArray()
    }

    override fun validate(): ValidationResult {
        return pathValidator.validate(editorConfig.executablePath, com.github.qczone.switch2vscode.model.EditorType.CURSOR)
    }

    override fun supportsFeature(feature: EditorFeature): Boolean {
        return when (feature) {
            EditorFeature.GOTO_COMMAND -> true
            EditorFeature.WAIT_MODE -> true
            EditorFeature.NEW_WINDOW -> true
            EditorFeature.WORKSPACE -> true
            EditorFeature.EXTENSIONS -> true
            EditorFeature.DEBUGGING -> true
            EditorFeature.INTEGRATED_TERMINAL -> true
            EditorFeature.GIT_INTEGRATION -> true
            EditorFeature.LANGUAGE_SERVER_PROTOCOL -> true
        }
    }

    override fun getSupportedExtensions(): List<String> {
        return listOf("*") // Cursor支持所有文件类型
    }
}

/**
 * Windsurf编辑器实现
 * 基于VS Code，支持相同的命令行参数
 */
class WindsurfEditor(private val editorConfig: EditorConfig) : Editor {
    private val logger = Logger.getInstance(WindsurfEditor::class.java)
    private val pathValidator = PathValidator()

    override val id: String get() = editorConfig.id
    override val displayName: String get() = editorConfig.displayName
    override val executablePath: String
        get() = if (SystemInfo.isMac && editorConfig.executablePath.endsWith(".app")) {
            "${editorConfig.executablePath}/Contents/MacOS/Windsurf"
        } else {
            editorConfig.executablePath
        }
    override val version: String? get() = editorConfig.version
    override val config: EditorConfig get() = editorConfig

    override fun buildFileCommand(filePath: String, line: Int, column: Int): Array<String> {
        val commands = mutableListOf<String>()

        commands.add(executablePath)

        if (line > 0 && column > 0) {
            commands.add("--goto")
            commands.add("$filePath:$line:$column")
        } else if (line > 0) {
            commands.add("--goto")
            commands.add("$filePath:$line")
        } else {
            commands.add(filePath)
        }

        commands.addAll(editorConfig.customArgs)

        return commands.toTypedArray()
    }

    override fun buildProjectCommand(projectPath: String): Array<String> {
        val commands = mutableListOf<String>()

        commands.add(executablePath)
        commands.add(projectPath)
        commands.addAll(editorConfig.customArgs)

        return commands.toTypedArray()
    }

    override fun validate(): ValidationResult {
        return pathValidator.validate(editorConfig.executablePath, com.github.qczone.switch2vscode.model.EditorType.WINDSURF)
    }

    override fun supportsFeature(feature: EditorFeature): Boolean {
        return when (feature) {
            EditorFeature.GOTO_COMMAND -> true
            EditorFeature.WAIT_MODE -> true
            EditorFeature.NEW_WINDOW -> true
            EditorFeature.WORKSPACE -> true
            EditorFeature.EXTENSIONS -> true
            EditorFeature.DEBUGGING -> true
            EditorFeature.INTEGRATED_TERMINAL -> true
            EditorFeature.GIT_INTEGRATION -> true
            EditorFeature.LANGUAGE_SERVER_PROTOCOL -> true
        }
    }

    override fun getSupportedExtensions(): List<String> {
        return listOf("*")
    }
}

/**
 * AntiGravity编辑器实现
 * 基于VS Code，支持相同的命令行参数
 */
class AntiGravityEditor(private val editorConfig: EditorConfig) : Editor {
    private val logger = Logger.getInstance(AntiGravityEditor::class.java)
    private val pathValidator = PathValidator()

    override val id: String get() = editorConfig.id
    override val displayName: String get() = editorConfig.displayName
    override val executablePath: String
        get() = if (SystemInfo.isMac && editorConfig.executablePath.endsWith(".app")) {
            "${editorConfig.executablePath}/Contents/MacOS/AntiGravity"
        } else {
            editorConfig.executablePath
        }
    override val version: String? get() = editorConfig.version
    override val config: EditorConfig get() = editorConfig

    override fun buildFileCommand(filePath: String, line: Int, column: Int): Array<String> {
        val commands = mutableListOf<String>()

        commands.add(executablePath)

        if (line > 0 && column > 0) {
            commands.add("--goto")
            commands.add("$filePath:$line:$column")
        } else if (line > 0) {
            commands.add("--goto")
            commands.add("$filePath:$line")
        } else {
            commands.add(filePath)
        }

        commands.addAll(editorConfig.customArgs)

        return commands.toTypedArray()
    }

    override fun buildProjectCommand(projectPath: String): Array<String> {
        val commands = mutableListOf<String>()

        commands.add(executablePath)
        commands.add(projectPath)
        commands.addAll(editorConfig.customArgs)

        return commands.toTypedArray()
    }

    override fun validate(): ValidationResult {
        return pathValidator.validate(editorConfig.executablePath, com.github.qczone.switch2vscode.model.EditorType.ANTIGRAVITY)
    }

    override fun supportsFeature(feature: EditorFeature): Boolean {
        return when (feature) {
            EditorFeature.GOTO_COMMAND -> true
            EditorFeature.WAIT_MODE -> true
            EditorFeature.NEW_WINDOW -> true
            EditorFeature.WORKSPACE -> true
            EditorFeature.EXTENSIONS -> true
            EditorFeature.DEBUGGING -> true
            EditorFeature.INTEGRATED_TERMINAL -> true
            EditorFeature.GIT_INTEGRATION -> true
            EditorFeature.LANGUAGE_SERVER_PROTOCOL -> true
        }
    }

    override fun getSupportedExtensions(): List<String> {
        return listOf("*")
    }
}

/**
 * CatPaw编辑器实现
 * 基于VS Code，支持相同的命令行参数
 */
class CatPawEditor(private val editorConfig: EditorConfig) : Editor {
    private val logger = Logger.getInstance(CatPawEditor::class.java)
    private val pathValidator = PathValidator()

    override val id: String get() = editorConfig.id
    override val displayName: String get() = editorConfig.displayName
    override val executablePath: String
        get() = if (SystemInfo.isMac && editorConfig.executablePath.endsWith(".app")) {
            "${editorConfig.executablePath}/Contents/MacOS/CatPaw"
        } else {
            editorConfig.executablePath
        }
    override val version: String? get() = editorConfig.version
    override val config: EditorConfig get() = editorConfig

    override fun buildFileCommand(filePath: String, line: Int, column: Int): Array<String> {
        val commands = mutableListOf<String>()

        commands.add(executablePath)

        if (line > 0 && column > 0) {
            commands.add("--goto")
            commands.add("$filePath:$line:$column")
        } else if (line > 0) {
            commands.add("--goto")
            commands.add("$filePath:$line")
        } else {
            commands.add(filePath)
        }

        commands.addAll(editorConfig.customArgs)

        return commands.toTypedArray()
    }

    override fun buildProjectCommand(projectPath: String): Array<String> {
        val commands = mutableListOf<String>()

        commands.add(executablePath)
        commands.add(projectPath)
        commands.addAll(editorConfig.customArgs)

        return commands.toTypedArray()
    }

    override fun validate(): ValidationResult {
        return pathValidator.validate(editorConfig.executablePath, com.github.qczone.switch2vscode.model.EditorType.CATPAW)
    }

    override fun supportsFeature(feature: EditorFeature): Boolean {
        return when (feature) {
            EditorFeature.GOTO_COMMAND -> true
            EditorFeature.WAIT_MODE -> true
            EditorFeature.NEW_WINDOW -> true
            EditorFeature.WORKSPACE -> true
            EditorFeature.EXTENSIONS -> true
            EditorFeature.DEBUGGING -> true
            EditorFeature.INTEGRATED_TERMINAL -> true
            EditorFeature.GIT_INTEGRATION -> true
            EditorFeature.LANGUAGE_SERVER_PROTOCOL -> true
        }
    }

    override fun getSupportedExtensions(): List<String> {
        return listOf("*")
    }
}

/**
 * 通用编辑器实现
 * 用于自定义编辑器或未知编辑器类型
 */
class GenericEditor(private val editorConfig: EditorConfig) : Editor {
    private val logger = Logger.getInstance(GenericEditor::class.java)
    private val pathValidator = PathValidator()

    override val id: String get() = editorConfig.id
    override val displayName: String get() = editorConfig.displayName
    override val executablePath: String get() = editorConfig.executablePath
    override val version: String? get() = editorConfig.version
    override val config: EditorConfig get() = editorConfig

    override fun buildFileCommand(filePath: String, line: Int, column: Int): Array<String> {
        val commands = mutableListOf<String>()

        commands.add(executablePath)

        // 对于未知编辑器，尝试常见的参数格式
        if (line > 0) {
            // 尝试几种常见的行号格式
            when {
                supportsFeature(EditorFeature.GOTO_COMMAND) -> {
                    commands.add("--goto")
                    commands.add("$filePath:$line:$column")
                }
                else -> {
                    // 简单的文件路径
                    commands.add(filePath)
                }
            }
        } else {
            commands.add(filePath)
        }

        commands.addAll(editorConfig.customArgs)

        return commands.toTypedArray()
    }

    override fun buildProjectCommand(projectPath: String): Array<String> {
        val commands = mutableListOf<String>()

        commands.add(executablePath)
        commands.add(projectPath)
        commands.addAll(editorConfig.customArgs)

        return commands.toTypedArray()
    }

    override fun validate(): ValidationResult {
        return pathValidator.validate(executablePath, com.github.qczone.switch2vscode.model.EditorType.CUSTOM)
    }

    override fun supportsFeature(feature: EditorFeature): Boolean {
        // 对于通用编辑器，保守地假设只支持基本功能
        return when (feature) {
            EditorFeature.GOTO_COMMAND -> false // 不确定是否支持
            EditorFeature.WAIT_MODE -> false
            EditorFeature.NEW_WINDOW -> false
            EditorFeature.WORKSPACE -> false
            EditorFeature.EXTENSIONS -> false
            EditorFeature.DEBUGGING -> false
            EditorFeature.INTEGRATED_TERMINAL -> false
            EditorFeature.GIT_INTEGRATION -> false
            EditorFeature.LANGUAGE_SERVER_PROTOCOL -> false
        }
    }

    override fun getSupportedExtensions(): List<String> {
        return listOf("*") // 假设支持所有文件类型
    }
}

/**
 * Trae编辑器实现
 * 基于VS Code，支持相同的命令行参数
 */
class TraeEditor(private val editorConfig: EditorConfig) : Editor {
    private val logger = Logger.getInstance(TraeEditor::class.java)
    private val pathValidator = PathValidator()

    override val id: String get() = editorConfig.id
    override val displayName: String get() = editorConfig.displayName
    override val executablePath: String
        get() = if (SystemInfo.isMac && editorConfig.executablePath.endsWith(".app")) {
            "${editorConfig.executablePath}/Contents/MacOS/Trae"
        } else {
            editorConfig.executablePath
        }
    override val version: String? get() = editorConfig.version
    override val config: EditorConfig get() = editorConfig

    override fun buildFileCommand(filePath: String, line: Int, column: Int): Array<String> {
        val commands = mutableListOf<String>()

        commands.add(executablePath)

        if (line > 0 && column > 0) {
            commands.add("--goto")
            commands.add("$filePath:$line:$column")
        } else if (line > 0) {
            commands.add("--goto")
            commands.add("$filePath:$line")
        } else {
            commands.add(filePath)
        }

        commands.addAll(editorConfig.customArgs)

        return commands.toTypedArray()
    }

    override fun buildProjectCommand(projectPath: String): Array<String> {
        val commands = mutableListOf<String>()

        commands.add(executablePath)
        commands.add(projectPath)
        commands.addAll(editorConfig.customArgs)

        return commands.toTypedArray()
    }

    override fun validate(): ValidationResult {
        return pathValidator.validate(editorConfig.executablePath, com.github.qczone.switch2vscode.model.EditorType.TRAE)
    }

    override fun supportsFeature(feature: EditorFeature): Boolean {
        return when (feature) {
            EditorFeature.GOTO_COMMAND -> true
            EditorFeature.WAIT_MODE -> true
            EditorFeature.NEW_WINDOW -> true
            EditorFeature.WORKSPACE -> true
            EditorFeature.EXTENSIONS -> true
            EditorFeature.DEBUGGING -> true
            EditorFeature.INTEGRATED_TERMINAL -> true
            EditorFeature.GIT_INTEGRATION -> true
            EditorFeature.LANGUAGE_SERVER_PROTOCOL -> true
        }
    }

    override fun getSupportedExtensions(): List<String> {
        return listOf("*")
    }
}