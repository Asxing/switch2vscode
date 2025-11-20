package com.github.qczone.switch2vscode.settings

import com.intellij.openapi.options.Configurable
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder

class AppSettingsConfigurable : Configurable {
    private var mySettingsComponent: AppSettingsComponent? = null

    override fun getDisplayName(): String = "Switch2VSCode"

    override fun createComponent(): JComponent {
        mySettingsComponent = AppSettingsComponent()
        return mySettingsComponent!!.panel
    }

    override fun isModified(): Boolean {
        val settings = AppSettingsState.getInstance()
        return mySettingsComponent!!.vscodePath != settings.vscodePath
    }

    override fun apply() {
        val settings = AppSettingsState.getInstance()
        settings.vscodePath = mySettingsComponent!!.vscodePath
    }

    override fun reset() {
        val settings = AppSettingsState.getInstance()
        mySettingsComponent!!.vscodePath = settings.vscodePath
    }

    override fun disposeUIResources() {
        mySettingsComponent = null
    }
}

class AppSettingsComponent {
    val panel: JPanel
    private val vscodePathText = JTextField()

    init {
        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("VS Code Path: "), vscodePathText, 1, false)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    var vscodePath: String
        get() = vscodePathText.text
        set(value) {
            vscodePathText.text = value
        }
} 
