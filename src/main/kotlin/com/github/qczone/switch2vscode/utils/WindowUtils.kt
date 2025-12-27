package com.github.qczone.switch2vscode.utils

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.SystemInfo

object WindowUtils {

    private val logger = Logger.getInstance(WindowUtils::class.java)

    /**
     * 激活编辑器窗口
     * @param editorId 编辑器ID
     */
    fun activateEditorWindow(editorId: String) {
        if (!SystemInfo.isWindows) {
            return
        }

        try {
            val processPattern = getProcessPattern(editorId)
            val command = buildWindowActivationCommand(processPattern)

            logger.info("Executing PowerShell command to activate $editorId window: $command")

            val processBuilder = ProcessBuilder("powershell", "-command", command)
            processBuilder.redirectErrorStream(true)

            val process = processBuilder.start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            logger.info("Command output: $output")

            val exitCode = process.waitFor()
            logger.info("Command completed with exit code: $exitCode")

            if (exitCode != 0) {
                logger.warn("Command failed with exit code: $exitCode")
            }
        } catch (e: Exception) {
            logger.error("Failed to activate $editorId window", e)
        }
    }

    /**
     * 向后兼容的方法
     * @deprecated 使用 activateEditorWindow(String) 替代
     */
    @Deprecated("Use activateEditorWindow(String) instead", ReplaceWith("activateEditorWindow(\"vscode\")"))
    fun activeWindow() {
        activateEditorWindow("vscode")
    }

    /**
     * 根据编辑器ID获取进程匹配模式
     */
    private fun getProcessPattern(editorId: String): String {
        return when (editorId.lowercase()) {
            "vscode" -> "Code"
            "cursor" -> "Cursor"
            "windsurf" -> "Windsurf"
            "antigravity" -> "AntiGravity"
            "catpaw" -> "CatPaw"
            else -> "Code" // 默认使用Code模式
        }
    }

    /**
     * 构建窗口激活命令
     */
    private fun buildWindowActivationCommand(processPattern: String): String {
        return """
            Get-Process |
            Where-Object {
                ${'$'}_.ProcessName -match '$processPattern' -and
                ${'$'}_.MainWindowTitle -ne ''
            } |
            Sort-Object { ${'$'}_.StartTime } -Descending |
            Select-Object -First 1 |
            ForEach-Object {
                (New-Object -ComObject WScript.Shell).AppActivate(${'$'}_.Id)
            }
        """.trimIndent().replace("\n", " ")
    }

    /**
     * 检查编辑器进程是否正在运行
     * @param editorId 编辑器ID
     * @return 是否正在运行
     */
    fun isEditorRunning(editorId: String): Boolean {
        if (!SystemInfo.isWindows) {
            return false // 目前只支持Windows检测
        }

        return try {
            val processPattern = getProcessPattern(editorId)
            val command = """
                Get-Process |
                Where-Object { ${'$'}_.ProcessName -match '$processPattern' } |
                Measure-Object |
                Select-Object -ExpandProperty Count
            """.trimIndent().replace("\n", " ")

            val processBuilder = ProcessBuilder("powershell", "-command", command)
            processBuilder.redirectErrorStream(true)

            val process = processBuilder.start()
            val output = process.inputStream.bufferedReader().use { it.readText().trim() }

            val exitCode = process.waitFor()
            if (exitCode == 0) {
                val count = output.toIntOrNull() ?: 0
                count > 0
            } else {
                false
            }
        } catch (e: Exception) {
            logger.debug("Failed to check if $editorId is running", e)
            false
        }
    }

    /**
     * 获取正在运行的编辑器进程信息
     * @param editorId 编辑器ID
     * @return 进程信息列表
     */
    fun getRunningEditorProcesses(editorId: String): List<ProcessInfo> {
        if (!SystemInfo.isWindows) {
            return emptyList()
        }

        return try {
            val processPattern = getProcessPattern(editorId)
            val command = """
                Get-Process |
                Where-Object { ${'$'}_.ProcessName -match '$processPattern' } |
                Select-Object Id, ProcessName, MainWindowTitle, StartTime |
                ConvertTo-Json
            """.trimIndent().replace("\n", " ")

            val processBuilder = ProcessBuilder("powershell", "-command", command)
            processBuilder.redirectErrorStream(true)

            val process = processBuilder.start()
            val output = process.inputStream.bufferedReader().use { it.readText() }

            val exitCode = process.waitFor()
            if (exitCode == 0 && output.isNotBlank()) {
                parseProcessInfo(output)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            logger.debug("Failed to get running processes for $editorId", e)
            emptyList()
        }
    }

    /**
     * 解析进程信息JSON
     */
    private fun parseProcessInfo(json: String): List<ProcessInfo> {
        return try {
            // 简单的JSON解析，实际项目中可能需要使用专门的JSON库
            val processes = mutableListOf<ProcessInfo>()
            // 这里简化处理，实际实现可能需要更复杂的JSON解析
            processes
        } catch (e: Exception) {
            logger.debug("Failed to parse process info JSON", e)
            emptyList()
        }
    }

    /**
     * 进程信息数据类
     */
    data class ProcessInfo(
        val id: Int,
        val processName: String,
        val mainWindowTitle: String,
        val startTime: String
    )
}
