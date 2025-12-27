package com.github.qczone.switch2vscode.settings

import com.github.qczone.switch2vscode.discovery.EditorDiscoveryEngine
import com.github.qczone.switch2vscode.model.EditorConfig
import com.github.qczone.switch2vscode.model.DiscoveryStrategy
import com.github.qczone.switch2vscode.model.ValidationResult
import com.github.qczone.switch2vscode.validation.PathValidator
import com.intellij.icons.AllIcons
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.io.File
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class AppSettingsConfigurable : Configurable {
    private var mySettingsComponent: AppSettingsComponent? = null

    override fun getDisplayName(): String = "Switch2VSCode"

    override fun createComponent(): JComponent {
        mySettingsComponent = AppSettingsComponent()
        return mySettingsComponent!!.panel
    }

    override fun isModified(): Boolean {
        val settings = AppSettingsState.getInstance()
        val component = mySettingsComponent ?: return false

        return component.getSelectedEditorId() != settings.selectedEditorId ||
               component.getEditorConfigs() != settings.editorConfigs
    }

    override fun apply() {
        val settings = AppSettingsState.getInstance()
        val component = mySettingsComponent ?: return

        // 确保当前UI状态被保存到组件的配置中
        component.saveCurrentUIState()

        settings.selectedEditorId = component.getSelectedEditorId()
        settings.editorConfigs.clear()
        settings.editorConfigs.putAll(component.getEditorConfigs())

        // 保持向后兼容
        settings.vscodePath = settings.editorConfigs["vscode"]?.executablePath ?: "code"
    }

    override fun reset() {
        val settings = AppSettingsState.getInstance()
        val component = mySettingsComponent ?: return

        // 始终触发自动发现，确保获取最新的路径信息
        component.refreshEditorList()

        // 设置已保存的选择（如果有的话）
        if (settings.selectedEditorId.isNotEmpty()) {
            // 发现完成后会自动恢复选择，这里不需要立即设置
        }
    }

    override fun disposeUIResources() {
        mySettingsComponent = null
    }
}

/**
 * 多编辑器配置组件
 * 支持下拉选择编辑器和实时验证
 */
class AppSettingsComponent {
    // UI组件
    private val editorComboBox = ComboBox<EditorDisplayItem>()
    private val pathField = JBTextField()
    private val browseButton = JButton("Browse...")
    private val refreshButton = JButton("Refresh")
    private val validateButton = JButton("Validate")
    private val statusLabel = JBLabel()
    private val statusIcon = JLabel()
    private val progressBar = JProgressBar()
    private val progressLabel = JLabel("Ready")

    // 调试日志组件
    private val debugLogArea = javax.swing.JTextArea(8, 50).apply {
        isEditable = false
        font = java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 12)
        background = java.awt.Color(248, 248, 248)
    }
    private val debugScrollPane = javax.swing.JScrollPane(debugLogArea).apply {
        verticalScrollBarPolicy = javax.swing.JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        horizontalScrollBarPolicy = javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
    }

    // 编辑器详细信息组件
    private val editorInfoPanel = JPanel(BorderLayout())
    private val editorNameLabel = JBLabel("No editor selected").apply {
        font = font.deriveFont(font.style or java.awt.Font.BOLD, font.size + 2.0f)
    }
    private val editorVersionLabel = JBLabel("")
    private val editorTypeLabel = JBLabel("")
    private val editorPathLabel = JBLabel("")
    private val editorStatusLabel = JBLabel("")

    // 数据和服务
    private val discoveryEngine = EditorDiscoveryEngine()
    private val pathValidator = PathValidator()
    private var editorConfigs = mutableMapOf<String, EditorConfig>()
    private var isRefreshing = false

    // 主面板 - 延迟初始化
    val panel: JPanel by lazy {
        FormBuilder.createFormBuilder()
            .addLabeledComponent("Editor:", createEditorSelectorPanel())
            .addLabeledComponent("Path:", createPathPanel())
            .addLabeledComponent("Status:", createStatusPanel())
            .addLabeledComponent("Progress:", createProgressPanel())
            .addLabeledComponent("Editor Info:", createEditorInfoPanel())
            .addLabeledComponent("Debug Log:", createDebugLogPanel())
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    init {
        addDebugLog("AppSettingsComponent initialized")
        setupListeners()
        initializeDefaults()
        refreshEditorList()
    }

    /**
     * 添加调试日志
     */
    private fun addDebugLog(message: String) {
        val timestamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
        val logMessage = "[$timestamp] $message\n"
        javax.swing.SwingUtilities.invokeLater {
            debugLogArea.append(logMessage)
            debugLogArea.caretPosition = debugLogArea.document.length
        }
    }

    private fun setupUI() {
        // UI已在panel延迟初始化时设置
    }


    private fun createProgressPanel(): JPanel {
        val panel = JPanel(BorderLayout())

        // 进度条
        progressBar.isStringPainted = true
        progressBar.string = "Ready"
        panel.add(progressBar, BorderLayout.CENTER)

        // 状态标签
        progressLabel.foreground = JBColor.GRAY
        panel.add(progressLabel, BorderLayout.SOUTH)

        return panel
    }

    private fun createEditorSelectorPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.add(editorComboBox, BorderLayout.CENTER)

        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0))
        buttonPanel.add(refreshButton)
        panel.add(buttonPanel, BorderLayout.EAST)

        return panel
    }

    private fun createPathPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.add(pathField, BorderLayout.CENTER)

        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0))
        buttonPanel.add(browseButton)
        buttonPanel.add(validateButton)
        panel.add(buttonPanel, BorderLayout.EAST)

        return panel
    }

    private fun createStatusPanel(): JPanel {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0))
        panel.add(statusIcon)
        panel.add(statusLabel)
        return panel
    }

    private fun createEditorInfoPanel(): JPanel {
        val panel = JPanel(BorderLayout())

        // 创建信息显示区域
        val infoPanel = JPanel()
        infoPanel.layout = BoxLayout(infoPanel, BoxLayout.Y_AXIS)
        infoPanel.border = javax.swing.BorderFactory.createTitledBorder("Editor Details")

        // 添加信息标签
        infoPanel.add(editorNameLabel)
        infoPanel.add(Box.createVerticalStrut(5))
        infoPanel.add(editorVersionLabel)
        infoPanel.add(editorTypeLabel)
        infoPanel.add(editorPathLabel)
        infoPanel.add(editorStatusLabel)

        panel.add(infoPanel, BorderLayout.CENTER)

        return panel
    }

    private fun createDebugLogPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.add(debugScrollPane, BorderLayout.CENTER)

        val clearButton = JButton("Clear Log")
        clearButton.addActionListener {
            debugLogArea.text = ""
            addDebugLog("Debug log cleared")
        }

        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        buttonPanel.add(clearButton)
        panel.add(buttonPanel, BorderLayout.SOUTH)

        return panel
    }

    private fun setupListeners() {
        // 编辑器选择变化
        editorComboBox.addActionListener { onEditorSelected() }

        // 路径字段变化
        pathField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = validateCurrentPath()
            override fun removeUpdate(e: DocumentEvent) = validateCurrentPath()
            override fun changedUpdate(e: DocumentEvent) = validateCurrentPath()
        })

        // 按钮事件
        browseButton.addActionListener { browseForExecutable() }
        refreshButton.addActionListener { refreshEditorList() }
        validateButton.addActionListener { validateCurrentPath() }
    }

    private fun initializeDefaults() {
        // 不在这里初始化默认配置，让发现过程完成后再处理
        // val settings = AppSettingsState.getInstance()
        // settings.initializeDefaultConfigs()
    }

    private fun onEditorSelected() {
        if (isRefreshing) return

        val selectedItem = editorComboBox.selectedItem as? EditorDisplayItem ?: return
        val config = selectedItem.config

        // 更新路径字段
        pathField.text = config.executablePath

        // 更新编辑器详细信息
        updateEditorInfo(config)

        // 验证路径
        validateCurrentPath()

        // 更新配置
        editorConfigs[config.id] = config
    }

    /**
     * 更新编辑器详细信息显示
     */
    private fun updateEditorInfo(config: EditorConfig) {
        editorNameLabel.text = config.displayName

        // 显示版本信息
        if (config.version?.isNotEmpty() == true) {
            editorVersionLabel.text = "Version: ${config.version}"
            editorVersionLabel.isVisible = true
        } else {
            editorVersionLabel.text = "Version: Unknown"
            editorVersionLabel.isVisible = true
        }

        // 显示编辑器类型
        val editorType = com.github.qczone.switch2vscode.model.EditorType.fromId(config.id)
        editorTypeLabel.text = "Type: ${editorType?.displayName ?: "Unknown"}"
        editorTypeLabel.isVisible = true

        // 显示路径信息
        editorPathLabel.text = "Path: ${config.executablePath}"
        editorPathLabel.isVisible = true

        // 显示状态信息
        val statusText = when {
            config.isAutoDiscovered -> "Auto-discovered"
            config.isDefault -> "Default configuration"
            else -> "Manual configuration"
        }
        editorStatusLabel.text = "Status: $statusText"
        editorStatusLabel.isVisible = true

        // 设置颜色
        editorVersionLabel.foreground = JBColor.GRAY
        editorTypeLabel.foreground = JBColor.GRAY
        editorPathLabel.foreground = JBColor.GRAY
        editorStatusLabel.foreground = if (config.isAutoDiscovered) JBColor.GREEN else JBColor.GRAY
    }

    /**
     * 清空编辑器详细信息显示
     */
    private fun clearEditorInfo() {
        editorNameLabel.text = "No editor selected"
        editorVersionLabel.isVisible = false
        editorTypeLabel.isVisible = false
        editorPathLabel.isVisible = false
        editorStatusLabel.isVisible = false
    }

    private fun validateCurrentPath() {
        val path = pathField.text.trim()
        val selectedItem = editorComboBox.selectedItem as? EditorDisplayItem

        if (path.isEmpty()) {
            updateStatus(ValidationResult.Invalid("Please enter a path"))
            return
        }

        // 获取编辑器类型用于验证
        val editorType = selectedItem?.let { item ->
            com.github.qczone.switch2vscode.model.EditorType.fromId(item.config.id)
        }

        // 先显示验证中状态
        updateStatus(ValidationResult.Warning("Validating..."))

        // 在后台线程执行验证，避免阻塞UI
        com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread {
            val result = pathValidator.validate(path, editorType)

            // 回到UI线程更新状态
            com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                updateStatus(result)
            }
        }

        // 立即更新当前编辑器配置（不依赖验证结果）
        selectedItem?.let { item ->
            val updatedConfig = item.config.copy(executablePath = path)
            editorConfigs[updatedConfig.id] = updatedConfig
        }
    }

    private fun updateStatus(result: ValidationResult) {
        when (result) {
            is ValidationResult.Valid -> {
                statusIcon.icon = AllIcons.General.InspectionsOK
                statusLabel.text = "Valid path"
                statusLabel.foreground = JBColor.GREEN
            }
            is ValidationResult.Invalid -> {
                statusIcon.icon = AllIcons.General.Error
                statusLabel.text = result.reason
                statusLabel.foreground = JBColor.RED
            }
            is ValidationResult.Warning -> {
                statusIcon.icon = AllIcons.General.Warning
                statusLabel.text = result.message
                statusLabel.foreground = JBColor.YELLOW
            }
        }
    }

    private fun browseForExecutable() {
        val descriptor = FileChooserDescriptor(
            true,  // chooseFiles
            false, // chooseFolders
            false, // chooseJars
            false, // chooseJarsAsFiles
            false, // chooseJarContents
            false  // chooseMultiple
        ).apply {
            title = "Select Editor Executable"
            description = "Choose the editor executable file"
        }

        val selectedFile = FileChooser.chooseFile(descriptor, null, null)
        selectedFile?.let { file ->
            pathField.text = file.path
            validateCurrentPath()
        }
    }

    fun refreshEditorList() {
        if (isRefreshing) return

        addDebugLog("Starting editor discovery...")
        isRefreshing = true
        refreshButton.isEnabled = false
        refreshButton.text = "Discovering..."

        // 更新进度显示
        progressBar.isIndeterminate = true
        progressBar.string = "Discovering editors..."
        progressLabel.text = "Initializing discovery..."

        // 使用默认的发现策略
        val selectedStrategy = DiscoveryStrategy.getDefault()

        addDebugLog("Using discovery strategy: ${selectedStrategy.displayName}")

        discoveryEngine.discoverAsyncWithStrategyAndDebug(
            strategy = selectedStrategy,
            debugCallback = { message ->
                addDebugLog("Engine: $message")
                SwingUtilities.invokeLater {
                    progressLabel.text = message
                }
            },
            callback = { discoveredConfigs ->
                SwingUtilities.invokeLater {
                    addDebugLog("Discovery completed. Found ${discoveredConfigs.size} editors:")
                    discoveredConfigs.forEach { config ->
                        addDebugLog("  - ${config.displayName}: ${config.executablePath} (auto-discovered: ${config.isAutoDiscovered})")
                    }

                    // 重置进度显示
                    progressBar.isIndeterminate = false
                    progressBar.string = "Discovery completed"
                    progressLabel.text = "Found ${discoveredConfigs.size} editors"

                    updateEditorList(discoveredConfigs)
                    isRefreshing = false
                    refreshButton.isEnabled = true
                    refreshButton.text = "Refresh"
                }
            }
        )
    }

    private fun updateEditorList(discoveredConfigs: List<EditorConfig>) {
        val currentSelection = editorComboBox.selectedItem as? EditorDisplayItem
        addDebugLog("Updating editor list. Current selection: ${currentSelection?.config?.displayName}")

        // 合并发现的配置和现有配置
        val mergedConfigs = mutableMapOf<String, EditorConfig>()

        // 添加现有配置
        addDebugLog("Existing configs: ${editorConfigs.size}")
        editorConfigs.forEach { (id, config) ->
            addDebugLog("  - Existing: $id -> ${config.executablePath}")
        }
        mergedConfigs.putAll(editorConfigs)

        // 添加发现的配置，自动发现的配置优先级更高
        addDebugLog("Merging discovered configs...")
        discoveredConfigs.forEach { config ->
            val existingConfig = mergedConfigs[config.id]
            if (existingConfig == null || !existingConfig.isAutoDiscovered) {
                // 如果没有现有配置，或者现有配置不是自动发现的，则使用新发现的配置
                addDebugLog("  - Adding/Updating: ${config.id} -> ${config.executablePath}")
                mergedConfigs[config.id] = config
            } else {
                addDebugLog("  - Keeping existing: ${config.id} -> ${existingConfig.executablePath}")
            }
        }

        // 检查是否发现了任何编辑器
        if (discoveredConfigs.isEmpty() && mergedConfigs.isEmpty()) {
            // 没有发现任何编辑器，创建默认配置并显示友好提示
            updateStatus(ValidationResult.Warning(
                "No editors found. Please install an editor or manually specify the path. " +
                "Use 'which code' (macOS/Linux) or 'where code' (Windows) to find executable paths."
            ))

            // 添加默认的VS Code配置供用户手动配置
            mergedConfigs["vscode"] = EditorConfig(
                id = "vscode",
                displayName = "Visual Studio Code",
                executablePath = "code",
                isDefault = true,
                isAutoDiscovered = false
            )
        } else if (discoveredConfigs.isEmpty() && mergedConfigs.isNotEmpty()) {
            // 有现有配置但没有发现新的编辑器
            updateStatus(ValidationResult.Warning(
                "No new editors discovered. Use 'which <editor>' (macOS/Linux) or 'where <editor>' (Windows) to find paths manually."
            ))
        } else if (discoveredConfigs.isNotEmpty()) {
            // 发现了编辑器，显示成功信息
            updateStatus(ValidationResult.Valid)
        }

        // 更新下拉框
        val items = mergedConfigs.values.map { EditorDisplayItem(it) }.sortedBy { it.config.displayName }
        editorComboBox.removeAllItems()
        items.forEach { editorComboBox.addItem(it) }

        // 如果没有编辑器，清空详细信息显示
        if (items.isEmpty()) {
            clearEditorInfo()
        }

        // 恢复选择，优先选择已保存的编辑器
        val settings = AppSettingsState.getInstance()
        val savedEditorId = settings.selectedEditorId

        if (savedEditorId.isNotEmpty()) {
            val savedItem = items.find { it.config.id == savedEditorId }
            if (savedItem != null) {
                editorComboBox.selectedItem = savedItem
            } else if (items.isNotEmpty()) {
                editorComboBox.selectedItem = items.first()
            }
        } else {
            // 如果没有保存的选择，恢复当前选择或选择第一个
            currentSelection?.let { selected ->
                val matchingItem = items.find { it.config.id == selected.config.id }
                if (matchingItem != null) {
                    editorComboBox.selectedItem = matchingItem
                } else if (items.isNotEmpty()) {
                    editorComboBox.selectedItem = items.first()
                }
            } ?: run {
                if (items.isNotEmpty()) {
                    editorComboBox.selectedItem = items.first()
                }
            }
        }

        // 更新配置
        editorConfigs = mergedConfigs

        // 强制触发一次编辑器选择事件，确保路径字段被正确更新
        SwingUtilities.invokeLater {
            onEditorSelected()
        }
    }

    // 公共接口
    fun getSelectedEditorId(): String {
        val selectedItem = editorComboBox.selectedItem as? EditorDisplayItem
        return selectedItem?.config?.id ?: "vscode"
    }

    fun setSelectedEditorId(editorId: String) {
        for (i in 0 until editorComboBox.itemCount) {
            val item = editorComboBox.getItemAt(i)
            if (item.config.id == editorId) {
                editorComboBox.selectedIndex = i
                break
            }
        }
    }

    fun getEditorConfigs(): Map<String, EditorConfig> = editorConfigs.toMap()

    fun setEditorConfigs(configs: Map<String, EditorConfig>) {
        editorConfigs = configs.toMutableMap()
        updateEditorList(configs.values.toList())
    }


    /**
     * 保存当前UI状态到内部配置
     * 确保当前选择的编辑器和路径被正确保存
     */
    fun saveCurrentUIState() {
        val selectedItem = editorComboBox.selectedItem as? EditorDisplayItem ?: return
        val currentPath = pathField.text.trim()

        // 更新当前选中编辑器的配置
        val updatedConfig = selectedItem.config.copy(executablePath = currentPath)
        editorConfigs[updatedConfig.id] = updatedConfig
    }

    // 向后兼容
    var vscodePath: String
        get() = editorConfigs["vscode"]?.executablePath ?: "code"
        set(value) {
            val vscodeConfig = editorConfigs["vscode"]?.copy(executablePath = value)
                ?: EditorConfig(
                    id = "vscode",
                    displayName = "Visual Studio Code",
                    executablePath = value
                )
            editorConfigs["vscode"] = vscodeConfig
        }
}

/**
 * 编辑器显示项
 * 用于下拉框显示
 */
data class EditorDisplayItem(val config: EditorConfig) {
    override fun toString(): String = config.getDisplayText()
} 
