package com.github.qczone.switch2vscode.discovery

import com.github.qczone.switch2vscode.model.EditorConfig
import com.github.qczone.switch2vscode.model.EditorType
import java.io.File

/**
 * 应用程序发现服务接口
 * 定义应用程序级编辑器发现的统一接口
 */
interface ApplicationDiscoveryService {
    /**
     * 发现已安装的编辑器
     * @return 发现的编辑器配置列表
     */
    fun discoverEditors(): List<EditorConfig>

    /**
     * 检查当前平台是否支持此服务
     * @return 是否支持
     */
    fun isSupported(): Boolean

    /**
     * 获取服务名称
     * @return 服务名称
     */
    fun getServiceName(): String

    /**
     * 获取预计执行时间（毫秒）
     * @return 预计执行时间
     */
    fun getEstimatedTimeMs(): Long = 3000L
}

/**
 * 编辑器应用程序元数据
 * 包含应用程序的详细信息
 */
data class EditorAppMetadata(
    val appName: String,                    // 应用程序名称
    val appPath: String,                    // 应用程序路径
    val executablePath: String,             // 可执行文件路径
    val version: String? = null,            // 版本信息
    val bundleId: String? = null,           // macOS Bundle ID
    val displayName: String? = null,        // 显示名称
    val description: String? = null,        // 描述信息
    val installLocation: String? = null     // 安装位置
) {
    /**
     * 检测编辑器类型
     */
    fun detectEditorType(): EditorType {
        return EditorType.detectFromPath(appPath)
    }

    /**
     * 转换为编辑器配置
     */
    fun toEditorConfig(): EditorConfig {
        val editorType = detectEditorType()
        val finalDisplayName = displayName ?: editorType.displayName

        return EditorConfig(
            id = editorType.id,
            displayName = finalDisplayName,
            executablePath = executablePath,
            version = version,
            isDefault = editorType == EditorType.VSCODE,
            isAutoDiscovered = true
        )
    }
}

/**
 * 应用程序发现服务的抽象基类
 * 提供通用的发现逻辑和工具方法
 */
abstract class BaseApplicationDiscoveryService : ApplicationDiscoveryService {

    /**
     * 从应用程序元数据创建编辑器配置
     */
    protected fun createEditorConfig(metadata: EditorAppMetadata): EditorConfig? {
        return try {
            val config = metadata.toEditorConfig()

            // 验证可执行文件是否存在
            if (File(config.executablePath).exists()) {
                config
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 检查文件是否为已知的编辑器（仅使用白名单模式匹配）
     */
    protected fun isKnownEditor(fileName: String): Boolean {
        val lowerName = fileName.lowercase()
        // 只使用严格的模式匹配，避免 executableNames 的宽泛匹配导致误识别
        return isKnownEditorByPattern(lowerName)
    }

    /**
     * 通过模式匹配检查是否为已知编辑器（仅VS Code系列白名单）
     */
    private fun isKnownEditorByPattern(lowerName: String): Boolean {
        // 精确匹配VS Code
        if (lowerName.contains("visual studio code") || lowerName.contains("vscode")) {
            return true
        }

        // 严格匹配"code"，避免误匹配包含"code"的其他工具
        if ((lowerName == "code" || lowerName.endsWith("/code") || lowerName.endsWith("\\code") ||
             lowerName.endsWith("code.exe") || lowerName.endsWith("/code.exe") || lowerName.endsWith("\\code.exe")) &&
            !lowerName.contains("findinput") && !lowerName.contains("tool")) {
            return true
        }

        // 其他VS Code系列编辑器
        val otherVSCodeEditors = listOf("cursor", "windsurf", "antigravity", "catpaw", "trae")
        return otherVSCodeEditors.any { pattern ->
            lowerName.contains(pattern)
        }
    }

    /**
     * 执行系统命令并获取输出
     */
    protected fun executeCommand(vararg command: String): List<String> {
        return try {
            val process = ProcessBuilder(*command)
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readLines()
            process.waitFor()

            if (process.exitValue() == 0) {
                output.filter { it.isNotBlank() }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 安全地扫描目录
     */
    protected fun safeScanDirectory(dirPath: String, filter: (File) -> Boolean = { true }): List<File> {
        return try {
            val dir = File(dirPath)
            if (dir.exists() && dir.isDirectory && dir.canRead()) {
                dir.listFiles()?.filter { filter(it) } ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 提取版本信息
     */
    protected fun extractVersion(path: String): String? {
        return try {
            val versionOutput = executeCommand(path, "--version")
            versionOutput.firstOrNull()?.let { line ->
                // 尝试提取版本号 (例如: "1.85.0", "v1.85.0", "Version 1.85.0")
                val versionRegex = Regex("""(\d+\.\d+(?:\.\d+)?)""")
                versionRegex.find(line)?.value
            }
        } catch (e: Exception) {
            null
        }
    }
}