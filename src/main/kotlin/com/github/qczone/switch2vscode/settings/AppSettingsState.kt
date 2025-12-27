package com.github.qczone.switch2vscode.settings

import com.github.qczone.switch2vscode.model.EditorConfig
import com.github.qczone.switch2vscode.model.EditorType
import com.github.qczone.switch2vscode.model.DiscoveryStrategy
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "com.github.qczone.switch2vscode.settings.AppSettingsState",
    storages = [Storage("Switch2VSCodeSettings.xml")]
)
class AppSettingsState : PersistentStateComponent<AppSettingsState> {
    // 向后兼容的原有配置
    var vscodePath: String = "code"

    // 新的多编辑器配置
    var selectedEditorId: String = "vscode"
    var editorConfigs: MutableMap<String, EditorConfig> = mutableMapOf()
    var discoveryStrategy: DiscoveryStrategy = DiscoveryStrategy.getDefault()
    var autoDiscoveryEnabled: Boolean = true
    var lastDiscoveryTime: Long = 0
    var configVersion: Int = 2

    override fun getState(): AppSettingsState = this

    override fun loadState(state: AppSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
        migrateFromLegacyIfNeeded()
    }

    /**
     * 从旧版本配置迁移到新版本
     * 确保向后兼容性
     */
    private fun migrateFromLegacyIfNeeded() {
        if (configVersion < 2 && editorConfigs.isEmpty() && vscodePath.isNotEmpty()) {
            // 迁移旧的VS Code配置
            editorConfigs["vscode"] = EditorConfig(
                id = "vscode",
                displayName = "Visual Studio Code",
                executablePath = vscodePath,
                isDefault = true
            )
            selectedEditorId = "vscode"
            configVersion = 2
        }
    }

    /**
     * 获取当前选中的编辑器配置
     */
    fun getSelectedEditorConfig(): EditorConfig? {
        return editorConfigs[selectedEditorId]
    }

    /**
     * 设置选中的编辑器
     */
    fun setSelectedEditor(editorId: String) {
        if (editorConfigs.containsKey(editorId)) {
            selectedEditorId = editorId
            // 同步更新vscodePath以保持向后兼容
            if (editorId == "vscode") {
                vscodePath = editorConfigs[editorId]?.executablePath ?: "code"
            }
        }
    }

    /**
     * 添加或更新编辑器配置
     */
    fun addOrUpdateEditorConfig(config: EditorConfig) {
        editorConfigs[config.id] = config
        // 如果是VS Code，同步更新vscodePath
        if (config.id == "vscode") {
            vscodePath = config.executablePath
        }
    }

    /**
     * 移除编辑器配置
     */
    fun removeEditorConfig(editorId: String) {
        editorConfigs.remove(editorId)
        // 如果移除的是当前选中的编辑器，选择第一个可用的
        if (selectedEditorId == editorId && editorConfigs.isNotEmpty()) {
            selectedEditorId = editorConfigs.keys.first()
        }
    }

    /**
     * 获取所有编辑器配置
     */
    fun getAllEditorConfigs(): List<EditorConfig> {
        return editorConfigs.values.toList()
    }

    /**
     * 检查是否有有效的编辑器配置
     */
    fun hasValidEditorConfig(): Boolean {
        return getSelectedEditorConfig()?.isValid() == true
    }

    /**
     * 初始化默认编辑器配置
     */
    fun initializeDefaultConfigs() {
        if (editorConfigs.isEmpty()) {
            // 添加默认的VS Code配置
            addOrUpdateEditorConfig(
                EditorConfig(
                    id = "vscode",
                    displayName = "Visual Studio Code",
                    executablePath = "code",
                    isDefault = true
                )
            )
            selectedEditorId = "vscode"
        }
    }

    companion object {
        fun getInstance(): AppSettingsState = ApplicationManager
            .getApplication()
            .getService(AppSettingsState::class.java)
    }
} 
