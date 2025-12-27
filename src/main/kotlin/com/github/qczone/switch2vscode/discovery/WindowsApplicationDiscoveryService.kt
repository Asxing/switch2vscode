package com.github.qczone.switch2vscode.discovery

import com.github.qczone.switch2vscode.model.EditorConfig
import com.intellij.openapi.util.SystemInfo
import java.io.File

/**
 * Windows 应用程序发现服务
 * 通过扫描 Program Files 目录和查询注册表来发现编辑器
 */
class WindowsApplicationDiscoveryService : BaseApplicationDiscoveryService() {

    override fun discoverEditors(): List<EditorConfig> {
        if (!isSupported()) {
            return emptyList()
        }

        val editors = mutableListOf<EditorConfig>()

        try {
            // 1. 扫描 Program Files 目录
            editors.addAll(scanProgramFiles())

            // 2. 查询注册表 Uninstall 项
            editors.addAll(queryUninstallRegistry())

            // 3. 查询 App Paths 注册表
            editors.addAll(queryAppPathsRegistry())

            // 4. 扫描用户本地程序目录
            editors.addAll(scanUserPrograms())

        } catch (e: Exception) {
            // 忽略错误，返回已发现的编辑器
        }

        return editors.distinctBy { it.executablePath }
    }

    override fun isSupported(): Boolean = SystemInfo.isWindows

    override fun getServiceName(): String = "Windows Application Discovery"

    /**
     * 扫描 Program Files 目录
     */
    private fun scanProgramFiles(): List<EditorConfig> {
        val programDirs = listOf(
            "C:\\Program Files",
            "C:\\Program Files (x86)"
        )

        val editors = mutableListOf<EditorConfig>()

        for (programDir in programDirs) {
            val appDirs = safeScanDirectory(programDir) { file ->
                file.isDirectory && isKnownEditorDirectory(file.name)
            }

            editors.addAll(appDirs.mapNotNull { appDir ->
                findExecutableInDirectory(appDir)
            })
        }

        return editors
    }

    /**
     * 扫描用户本地程序目录
     */
    private fun scanUserPrograms(): List<EditorConfig> {
        val userProgramsDir = "${System.getenv("LOCALAPPDATA")}\\Programs"

        val appDirs = safeScanDirectory(userProgramsDir) { file ->
            file.isDirectory && isKnownEditorDirectory(file.name)
        }

        return appDirs.mapNotNull { appDir ->
            findExecutableInDirectory(appDir)
        }
    }

    /**
     * 检查目录名是否为已知编辑器目录
     */
    private fun isKnownEditorDirectory(dirName: String): Boolean {
        val lowerName = dirName.lowercase()

        val editorDirPatterns = listOf(
            "visual studio code", "vscode", "microsoft vs code",
            "cursor", "windsurf", "zed",
            "intellij", "jetbrains", "webstorm", "pycharm", "phpstorm",
            "clion", "goland", "rider", "rubymine", "fleet",
            "sublime text", "atom", "notepad++", "brackets",
            "vim", "emacs"
        )

        return editorDirPatterns.any { pattern ->
            lowerName.contains(pattern)
        }
    }

    /**
     * 在目录中查找可执行文件
     */
    private fun findExecutableInDirectory(directory: File): EditorConfig? {
        try {
            // 递归查找 .exe 文件
            val executables = findExecutables(directory, maxDepth = 3)

            // 查找匹配的编辑器可执行文件
            val editorExecutable = executables.find { exe ->
                isKnownEditor(exe.name)
            }

            return editorExecutable?.let { exe ->
                val version = extractVersion(exe.absolutePath)

                createEditorConfig(
                    EditorAppMetadata(
                        appName = directory.name,
                        appPath = directory.absolutePath,
                        executablePath = exe.absolutePath,
                        version = version,
                        displayName = extractDisplayName(directory.name),
                        installLocation = directory.absolutePath
                    )
                )
            }
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * 递归查找可执行文件
     */
    private fun findExecutables(directory: File, currentDepth: Int = 0, maxDepth: Int = 3): List<File> {
        if (currentDepth >= maxDepth || !directory.exists() || !directory.isDirectory) {
            return emptyList()
        }

        val executables = mutableListOf<File>()

        try {
            directory.listFiles()?.forEach { file ->
                when {
                    file.isFile && file.name.endsWith(".exe", ignoreCase = true) -> {
                        executables.add(file)
                    }
                    file.isDirectory && currentDepth < maxDepth - 1 -> {
                        executables.addAll(findExecutables(file, currentDepth + 1, maxDepth))
                    }
                }
            }
        } catch (e: Exception) {
            // 忽略访问错误
        }

        return executables
    }

    /**
     * 查询注册表 Uninstall 项
     */
    private fun queryUninstallRegistry(): List<EditorConfig> {
        val editors = mutableListOf<EditorConfig>()

        // 查询系统和用户的 Uninstall 注册表项
        val registryPaths = listOf(
            "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall",
            "HKEY_CURRENT_USER\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall",
            "HKEY_LOCAL_MACHINE\\SOFTWARE\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall"
        )

        for (registryPath in registryPaths) {
            editors.addAll(queryRegistryPath(registryPath))
        }

        return editors
    }

    /**
     * 查询 App Paths 注册表
     */
    private fun queryAppPathsRegistry(): List<EditorConfig> {
        val registryPaths = listOf(
            "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\App Paths",
            "HKEY_CURRENT_USER\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\App Paths"
        )

        val editors = mutableListOf<EditorConfig>()

        for (registryPath in registryPaths) {
            editors.addAll(queryAppPaths(registryPath))
        }

        return editors
    }

    /**
     * 查询指定注册表路径
     */
    private fun queryRegistryPath(registryPath: String): List<EditorConfig> {
        try {
            // 使用 reg query 命令查询注册表
            val output = executeCommand("reg", "query", registryPath, "/s")

            return parseUninstallRegistryOutput(output)
        } catch (e: Exception) {
            return emptyList()
        }
    }

    /**
     * 查询 App Paths
     */
    private fun queryAppPaths(registryPath: String): List<EditorConfig> {
        try {
            val output = executeCommand("reg", "query", registryPath)

            return parseAppPathsOutput(output)
        } catch (e: Exception) {
            return emptyList()
        }
    }

    /**
     * 解析 Uninstall 注册表输出
     */
    private fun parseUninstallRegistryOutput(output: List<String>): List<EditorConfig> {
        val editors = mutableListOf<EditorConfig>()
        var currentKey: String? = null
        var displayName: String? = null
        var installLocation: String? = null

        for (line in output) {
            when {
                line.startsWith("HKEY_") -> {
                    // 处理之前的条目
                    if (currentKey != null && displayName != null && isKnownEditor(displayName)) {
                        installLocation?.let { location ->
                            findExecutableInDirectory(File(location))?.let { config ->
                                editors.add(config)
                            }
                        }
                    }

                    // 开始新条目
                    currentKey = line.trim()
                    displayName = null
                    installLocation = null
                }
                line.contains("DisplayName") -> {
                    displayName = extractRegistryValue(line)
                }
                line.contains("InstallLocation") -> {
                    installLocation = extractRegistryValue(line)
                }
            }
        }

        return editors
    }

    /**
     * 解析 App Paths 输出
     */
    private fun parseAppPathsOutput(output: List<String>): List<EditorConfig> {
        val editors = mutableListOf<EditorConfig>()

        for (line in output) {
            if (line.contains(".exe") && isKnownEditor(line)) {
                val exePath = extractRegistryValue(line)
                if (exePath != null && File(exePath).exists()) {
                    val version = extractVersion(exePath)
                    val appName = File(exePath).nameWithoutExtension

                    createEditorConfig(
                        EditorAppMetadata(
                            appName = appName,
                            appPath = exePath,
                            executablePath = exePath,
                            version = version,
                            displayName = extractDisplayName(appName)
                        )
                    )?.let { config ->
                        editors.add(config)
                    }
                }
            }
        }

        return editors
    }

    /**
     * 从注册表行中提取值
     */
    private fun extractRegistryValue(line: String): String? {
        return try {
            val parts = line.split("REG_SZ")
            if (parts.size >= 2) {
                parts[1].trim()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 提取显示名称
     */
    private fun extractDisplayName(rawName: String): String {
        return rawName
            .replace("_", " ")
            .split(" ")
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { it.uppercase() }
            }
    }
}