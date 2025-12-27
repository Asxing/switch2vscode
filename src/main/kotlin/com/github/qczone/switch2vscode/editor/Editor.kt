package com.github.qczone.switch2vscode.editor

import com.github.qczone.switch2vscode.model.EditorConfig
import com.github.qczone.switch2vscode.model.ValidationResult

/**
 * 编辑器抽象接口
 * 定义所有编辑器必须实现的基本功能
 */
interface Editor {
    /**
     * 编辑器唯一标识符
     */
    val id: String

    /**
     * 编辑器显示名称
     */
    val displayName: String

    /**
     * 可执行文件路径
     */
    val executablePath: String

    /**
     * 编辑器版本信息
     */
    val version: String?

    /**
     * 编辑器配置
     */
    val config: EditorConfig

    /**
     * 构建打开文件的命令
     * @param filePath 文件路径
     * @param line 行号（1-based）
     * @param column 列号（1-based）
     * @return 命令数组
     */
    fun buildFileCommand(filePath: String, line: Int, column: Int): Array<String>

    /**
     * 构建打开项目的命令
     * @param projectPath 项目路径
     * @return 命令数组
     */
    fun buildProjectCommand(projectPath: String): Array<String>

    /**
     * 验证编辑器配置是否有效
     * @return 验证结果
     */
    fun validate(): ValidationResult

    /**
     * 检查编辑器是否支持指定的功能
     * @param feature 功能名称
     * @return 是否支持
     */
    fun supportsFeature(feature: EditorFeature): Boolean

    /**
     * 获取编辑器支持的文件扩展名
     * @return 支持的扩展名列表
     */
    fun getSupportedExtensions(): List<String>

    /**
     * 检查编辑器是否可用
     * @return 是否可用
     */
    fun isAvailable(): Boolean {
        return validate() is ValidationResult.Valid
    }
}

/**
 * 编辑器功能枚举
 * 定义编辑器可能支持的各种功能
 */
enum class EditorFeature {
    /**
     * 支持goto命令（跳转到指定行列）
     */
    GOTO_COMMAND,

    /**
     * 支持等待模式
     */
    WAIT_MODE,

    /**
     * 支持新窗口模式
     */
    NEW_WINDOW,

    /**
     * 支持工作区
     */
    WORKSPACE,

    /**
     * 支持扩展
     */
    EXTENSIONS,

    /**
     * 支持调试
     */
    DEBUGGING,

    /**
     * 支持集成终端
     */
    INTEGRATED_TERMINAL,

    /**
     * 支持Git集成
     */
    GIT_INTEGRATION,

    /**
     * 支持LSP（Language Server Protocol）
     */
    LANGUAGE_SERVER_PROTOCOL
}