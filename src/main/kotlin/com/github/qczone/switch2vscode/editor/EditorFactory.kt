package com.github.qczone.switch2vscode.editor

import com.github.qczone.switch2vscode.model.EditorConfig
import com.github.qczone.switch2vscode.model.EditorType
import com.intellij.openapi.diagnostic.Logger

/**
 * 编辑器工厂类
 * 负责根据配置创建相应的编辑器实例
 */
object EditorFactory {
    private val logger = Logger.getInstance(EditorFactory::class.java)

    /**
     * 根据编辑器配置创建编辑器实例
     * @param config 编辑器配置
     * @return 编辑器实例
     */
    fun createEditor(config: EditorConfig): Editor {
        return try {
            when (config.id.lowercase()) {
                "vscode" -> VSCodeEditor(config)
                "cursor" -> CursorEditor(config)
                "windsurf" -> WindsurfEditor(config)
                "antigravity" -> AntiGravityEditor(config)
                "catpaw" -> CatPawEditor(config)
                "trae" -> TraeEditor(config)
                else -> {
                    logger.info("Creating generic editor for unknown type: ${config.id}")
                    GenericEditor(config)
                }
            }
        } catch (e: Exception) {
            logger.warn("Failed to create editor for config: ${config.id}, falling back to generic editor", e)
            GenericEditor(config)
        }
    }

    /**
     * 根据编辑器类型创建编辑器实例
     * @param editorType 编辑器类型
     * @param executablePath 可执行文件路径
     * @param version 版本信息（可选）
     * @return 编辑器实例
     */
    fun createEditor(
        editorType: EditorType,
        executablePath: String,
        version: String? = null
    ): Editor {
        val config = EditorConfig(
            id = editorType.id,
            displayName = editorType.displayName,
            executablePath = executablePath,
            version = version
        )
        return createEditor(config)
    }

    /**
     * 获取支持的编辑器类型列表
     * @return 支持的编辑器类型
     */
    fun getSupportedEditorTypes(): List<EditorType> {
        return EditorType.values().toList()
    }

    /**
     * 检查编辑器类型是否受支持
     * @param editorId 编辑器ID
     * @return 是否支持
     */
    fun isSupported(editorId: String): Boolean {
        return EditorType.fromId(editorId.lowercase()) != null
    }

    /**
     * 获取编辑器的默认配置
     * @param editorType 编辑器类型
     * @return 默认配置
     */
    fun getDefaultConfig(editorType: EditorType): EditorConfig {
        return EditorConfig(
            id = editorType.id,
            displayName = editorType.displayName,
            executablePath = getDefaultExecutablePath(editorType),
            isDefault = editorType == EditorType.VSCODE
        )
    }

    /**
     * 根据编辑器类型获取默认的可执行文件路径
     * @param editorType 编辑器类型
     * @return 默认路径
     */
    private fun getDefaultExecutablePath(editorType: EditorType): String {
        return when (editorType) {
            // VS Code 系列编辑器（白名单）
            EditorType.VSCODE -> "code"
            EditorType.CURSOR -> "cursor"
            EditorType.WINDSURF -> "windsurf"
            EditorType.ANTIGRAVITY -> "antigravity"
            EditorType.CATPAW -> "catpaw"
            EditorType.TRAE -> "trae"

            EditorType.CUSTOM -> ""
        }
    }

    /**
     * 验证编辑器配置并创建编辑器实例
     * @param config 编辑器配置
     * @return 验证通过的编辑器实例，如果验证失败则返回null
     */
    fun createValidatedEditor(config: EditorConfig): Editor? {
        return try {
            val editor = createEditor(config)
            val validation = editor.validate()

            if (validation.isValid()) {
                editor
            } else {
                logger.warn("Editor validation failed for ${config.id}: ${validation.getStatusMessage()}")
                null
            }
        } catch (e: Exception) {
            logger.warn("Failed to create or validate editor for ${config.id}", e)
            null
        }
    }

    /**
     * 批量创建编辑器实例
     * @param configs 编辑器配置列表
     * @return 编辑器实例列表（过滤掉创建失败的）
     */
    fun createEditors(configs: List<EditorConfig>): List<Editor> {
        return configs.mapNotNull { config ->
            try {
                createEditor(config)
            } catch (e: Exception) {
                logger.warn("Failed to create editor for config: ${config.id}", e)
                null
            }
        }
    }

    /**
     * 获取编辑器功能支持信息
     * @param editorType 编辑器类型
     * @return 功能支持映射
     */
    fun getFeatureSupport(editorType: EditorType): Map<EditorFeature, Boolean> {
        val tempConfig = getDefaultConfig(editorType)
        val editor = createEditor(tempConfig)

        return EditorFeature.values().associateWith { feature ->
            editor.supportsFeature(feature)
        }
    }

    /**
     * 获取编辑器的详细信息
     * @param config 编辑器配置
     * @return 编辑器信息字符串
     */
    fun getEditorInfo(config: EditorConfig): String {
        return try {
            val editor = createEditor(config)
            val version = editor.version
            val validation = editor.validate()

            buildString {
                appendLine("Editor: ${editor.displayName}")
                appendLine("ID: ${editor.id}")
                appendLine("Path: ${editor.executablePath}")
                if (version != null) {
                    appendLine("Version: $version")
                }
                appendLine("Status: ${validation.getStatusMessage()}")

                val supportedFeatures = EditorFeature.values().filter { editor.supportsFeature(it) }
                if (supportedFeatures.isNotEmpty()) {
                    appendLine("Supported Features:")
                    supportedFeatures.forEach { feature ->
                        appendLine("  - ${feature.name}")
                    }
                }
            }
        } catch (e: Exception) {
            "Failed to get editor info: ${e.message}"
        }
    }
}