package com.github.qczone.switch2vscode.discovery

import com.github.qczone.switch2vscode.model.EditorConfig
import com.intellij.openapi.util.SystemInfo
import java.io.File

/**
 * macOS 应用程序发现服务
 * 通过扫描应用程序目录和查询 Launch Services 来发现编辑器
 */
class MacOSApplicationDiscoveryService : BaseApplicationDiscoveryService() {

    override fun discoverEditors(): List<EditorConfig> {
        if (!isSupported()) {
            return emptyList()
        }

        val editors = mutableListOf<EditorConfig>()

        try {
            // 1. 扫描 /Applications 目录
            editors.addAll(scanApplicationsDirectory("/Applications"))

            // 2. 扫描用户应用目录
            val userAppsDir = "${System.getProperty("user.home")}/Applications"
            editors.addAll(scanApplicationsDirectory(userAppsDir))

            // 3. 使用 mdfind 查询代码编辑器（可选，如果 mdfind 可用）
            editors.addAll(queryWithMdfind())

        } catch (e: Exception) {
            // 忽略错误，返回已发现的编辑器
        }

        return editors.distinctBy { it.executablePath }
    }

    override fun isSupported(): Boolean = SystemInfo.isMac

    override fun getServiceName(): String = "macOS Application Discovery"

    /**
     * 扫描指定的应用程序目录
     */
    private fun scanApplicationsDirectory(dirPath: String): List<EditorConfig> {
        val appFiles = safeScanDirectory(dirPath) { file ->
            file.isDirectory &&
            file.name.endsWith(".app") &&
            isKnownEditor(file.name)
        }

        return appFiles.mapNotNull { appFile ->
            createEditorConfigFromApp(appFile)
        }
    }

    /**
     * 从 .app 包创建编辑器配置
     */
    private fun createEditorConfigFromApp(appFile: File): EditorConfig? {
        try {
            val appName = appFile.name
            val appPath = appFile.absolutePath

            // 查找可执行文件
            val executablePath = findMacAppExecutable(appPath) ?: return null

            // 读取应用信息
            val metadata = readAppMetadata(appFile)

            return createEditorConfig(
                EditorAppMetadata(
                    appName = appName,
                    appPath = appPath,
                    executablePath = executablePath,
                    version = metadata.version,
                    bundleId = metadata.bundleId,
                    displayName = metadata.displayName ?: appName.removeSuffix(".app"),
                    description = metadata.description,
                    installLocation = appPath
                )
            )
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * 查找 macOS .app 包内的可执行文件
     */
    private fun findMacAppExecutable(appPath: String): String? {
        return try {
            val contentsDir = File(appPath, "Contents")
            val macOSDir = File(contentsDir, "MacOS")

            if (macOSDir.exists() && macOSDir.isDirectory) {
                // 查找可执行文件
                val executables = macOSDir.listFiles { file ->
                    file.isFile && file.canExecute()
                }

                executables?.firstOrNull()?.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 读取应用程序元数据
     */
    private fun readAppMetadata(appFile: File): AppMetadata {
        try {
            val infoPlistFile = File(appFile, "Contents/Info.plist")
            if (infoPlistFile.exists()) {
                return parseInfoPlist(infoPlistFile)
            }
        } catch (e: Exception) {
            // 忽略解析错误
        }

        return AppMetadata()
    }

    /**
     * 解析 Info.plist 文件
     */
    private fun parseInfoPlist(plistFile: File): AppMetadata {
        try {
            val content = plistFile.readText()

            return AppMetadata(
                bundleId = extractPlistValue(content, "CFBundleIdentifier"),
                displayName = extractPlistValue(content, "CFBundleDisplayName")
                    ?: extractPlistValue(content, "CFBundleName"),
                version = extractPlistValue(content, "CFBundleShortVersionString")
                    ?: extractPlistValue(content, "CFBundleVersion"),
                description = extractPlistValue(content, "CFBundleGetInfoString")
            )
        } catch (e: Exception) {
            return AppMetadata()
        }
    }

    /**
     * 从 plist 内容中提取值
     */
    private fun extractPlistValue(content: String, key: String): String? {
        try {
            // 简单的 XML 解析，查找 <key>key</key><string>value</string> 模式
            val keyPattern = "<key>$key</key>\\s*<string>([^<]+)</string>"
            val regex = Regex(keyPattern)
            return regex.find(content)?.groupValues?.get(1)?.trim()
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * 使用 mdfind 查询编辑器应用程序
     */
    private fun queryWithMdfind(): List<EditorConfig> {
        try {
            // 查询应用程序包
            val results = executeCommand("mdfind", "kMDItemContentType=com.apple.application-bundle")

            return results.mapNotNull { path ->
                val appFile = File(path)
                if (appFile.exists() && appFile.name.endsWith(".app") && isKnownEditor(appFile.name)) {
                    createEditorConfigFromApp(appFile)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            return emptyList()
        }
    }

    /**
     * 应用程序元数据数据类
     */
    private data class AppMetadata(
        val bundleId: String? = null,
        val displayName: String? = null,
        val version: String? = null,
        val description: String? = null
    )
}