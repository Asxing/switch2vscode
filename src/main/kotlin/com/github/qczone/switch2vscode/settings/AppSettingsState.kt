package com.github.qczone.switch2vscode.settings

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
    var vscodePath: String = "code"

    override fun getState(): AppSettingsState = this

    override fun loadState(state: AppSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        fun getInstance(): AppSettingsState = ApplicationManager
            .getApplication()
            .getService(AppSettingsState::class.java)
    }
} 
