package com.github.qczone.switch2vscode.model

/**
 * 编辑器配置数据类
 * 包含编辑器的基本信息和配置参数
 */
data class EditorConfig(
    val id: String,                              // 编辑器唯一标识符
    val displayName: String,                     // 显示名称
    var executablePath: String,                  // 可执行文件路径
    val version: String? = null,                 // 版本信息
    val isDefault: Boolean = false,              // 是否为默认编辑器
    val isAutoDiscovered: Boolean = false,       // 是否通过自动发现获得
    val lastValidated: Long = 0,                 // 最后验证时间
    val customArgs: List<String> = emptyList()   // 自定义参数
) {
    /**
     * 获取显示文本，包含版本信息和发现状态
     */
    fun getDisplayText(): String {
        val versionText = version?.let { " ($it)" } ?: ""
        val discoveredText = if (isAutoDiscovered) " (Auto-discovered)" else ""
        return "$displayName$versionText$discoveredText"
    }

    /**
     * 检查配置是否有效（路径不为空）
     */
    fun isValid(): Boolean = executablePath.isNotBlank()
}

/**
 * 编辑器类型枚举
 * 定义支持的编辑器类型
 */
enum class EditorType(val id: String, val displayName: String, val executableNames: List<String>) {
    // VS Code 系列编辑器（白名单）
    VSCODE("vscode", "Visual Studio Code", listOf("code", "Code.exe")),
    CURSOR("cursor", "Cursor", listOf("cursor", "Cursor.exe")),
    WINDSURF("windsurf", "Windsurf", listOf("windsurf", "Windsurf.exe")),
    ANTIGRAVITY("antigravity", "AntiGravity", listOf("antigravity", "AntiGravity.exe")),
    CATPAW("catpaw", "CatPaw", listOf("catpaw", "CatPaw.exe")),
    TRAE("trae", "Trae", listOf("trae", "Trae.exe")),

    CUSTOM("custom", "Custom Editor", emptyList());

    /**
     * 根据路径判断是否匹配此编辑器类型
     */
    fun matches(path: String): Boolean {
        val pathLower = path.lowercase()
        return when (this) {
            // VS Code 系列编辑器（白名单）
            VSCODE -> pathLower.contains("visual studio code") ||
                      pathLower.contains("vscode") ||
                      (pathLower.contains("code") && (pathLower.contains("visual") || pathLower.endsWith("code") || pathLower.endsWith("code.exe")))
            CURSOR -> pathLower.contains("cursor")
            WINDSURF -> pathLower.contains("windsurf")
            ANTIGRAVITY -> pathLower.contains("antigravity")
            CATPAW -> pathLower.contains("catpaw")
            TRAE -> pathLower.contains("trae")

            CUSTOM -> true
        }
    }

    companion object {
        /**
         * 根据路径自动检测编辑器类型
         */
        fun detectFromPath(path: String): EditorType {
            return values().find { it != CUSTOM && it.matches(path) } ?: CUSTOM
        }

        /**
         * 根据ID获取编辑器类型
         */
        fun fromId(id: String): EditorType? {
            return values().find { it.id == id }
        }
    }
}

/**
 * 验证结果密封类
 * 表示路径验证的不同结果状态
 */
sealed class ValidationResult {
    /**
     * 验证成功
     */
    object Valid : ValidationResult()

    /**
     * 验证失败
     * @param reason 失败原因
     * @param suggestion 修复建议
     */
    data class Invalid(val reason: String, val suggestion: String? = null) : ValidationResult()

    /**
     * 验证警告
     * @param message 警告信息
     */
    data class Warning(val message: String) : ValidationResult()

    /**
     * 是否为有效状态
     */
    fun isValid(): Boolean = this is Valid

    /**
     * 获取状态消息
     */
    fun getStatusMessage(): String = when (this) {
        is Valid -> "Valid path"
        is Invalid -> reason
        is Warning -> message
    }

    /**
     * 获取建议信息
     */
    fun getSuggestionText(): String? = when (this) {
        is Invalid -> suggestion
        else -> null
    }
}