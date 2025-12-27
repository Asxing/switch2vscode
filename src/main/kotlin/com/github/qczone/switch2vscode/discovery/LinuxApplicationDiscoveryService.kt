package com.github.qczone.switch2vscode.discovery

import com.github.qczone.switch2vscode.model.EditorConfig
import com.intellij.openapi.util.SystemInfo
import java.io.File

/**
 * Linux 应用程序发现服务
 * 通过解析 .desktop 文件和扫描常见安装目录来发现编辑器
 */
class LinuxApplicationDiscoveryService : BaseApplicationDiscoveryService() {

    override fun discoverEditors(): List<EditorConfig> {
        if (!isSupported()) {
            return emptyList()
        }

        val editors = mutableListOf<EditorConfig>()

        try {
            // 1. 解析 .desktop 文件
            editors.addAll(parseDesktopFiles())

            // 2. 扫描常见安装目录
            editors.addAll(scanCommonDirectories())

            // 3. 查询包管理器（可选）
            editors.addAll(queryPackageManager())

        } catch (e: Exception) {
            // 忽略错误，返回已发现的编辑器
        }

        return editors.distinctBy { it.executablePath }
    }

    override fun isSupported(): Boolean = SystemInfo.isLinux

    override fun getServiceName(): String = "Linux Application Discovery"

    /**
     * 解析 .desktop 文件
     */
    private fun parseDesktopFiles(): List<EditorConfig> {
        val desktopDirs = listOf(
            "/usr/share/applications",
            "/usr/local/share/applications",
            "${System.getProperty("user.home")}/.local/share/applications",
            "/var/lib/flatpak/exports/share/applications",
            "${System.getProperty("user.home")}/.local/share/flatpak/exports/share/applications"
        )

        val editors = mutableListOf<EditorConfig>()

        for (desktopDir in desktopDirs) {
            val desktopFiles = safeScanDirectory(desktopDir) { file ->
                file.isFile && file.name.endsWith(".desktop")
            }

            editors.addAll(desktopFiles.mapNotNull { file ->
                parseDesktopFile(file)
            })
        }

        return editors
    }

    /**
     * 解析单个 .desktop 文件
     */
    private fun parseDesktopFile(desktopFile: File): EditorConfig? {
        try {
            val content = desktopFile.readText()
            val lines = content.lines()

            var name: String? = null
            var exec: String? = null
            var comment: String? = null
            var icon: String? = null
            var categories: String? = null

            for (line in lines) {
                when {
                    line.startsWith("Name=") -> name = line.substringAfter("Name=")
                    line.startsWith("Exec=") -> exec = line.substringAfter("Exec=")
                    line.startsWith("Comment=") -> comment = line.substringAfter("Comment=")
                    line.startsWith("Icon=") -> icon = line.substringAfter("Icon=")
                    line.startsWith("Categories=") -> categories = line.substringAfter("Categories=")
                }
            }

            // 检查是否为编辑器应用
            if (name != null && exec != null && isEditorApplication(name, categories)) {
                val executablePath = extractExecutablePath(exec)
                if (executablePath != null && File(executablePath).exists()) {
                    val version = extractVersion(executablePath)

                    return createEditorConfig(
                        EditorAppMetadata(
                            appName = desktopFile.nameWithoutExtension,
                            appPath = desktopFile.absolutePath,
                            executablePath = executablePath,
                            version = version,
                            displayName = name,
                            description = comment,
                            installLocation = File(executablePath).parent
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // 忽略解析错误
        }

        return null
    }

    /**
     * 检查是否为编辑器应用程序
     */
    private fun isEditorApplication(name: String, categories: String?): Boolean {
        // 检查名称
        if (isKnownEditor(name)) {
            return true
        }

        // 检查分类
        if (categories != null) {
            val editorCategories = listOf(
                "TextEditor", "Development", "IDE", "Programming"
            )

            val categoryList = categories.split(";")
            if (editorCategories.any { category -> categoryList.contains(category) }) {
                return isKnownEditor(name)
            }
        }

        return false
    }

    /**
     * 从 Exec 字段提取可执行文件路径
     */
    private fun extractExecutablePath(exec: String): String? {
        try {
            // 移除参数，只保留可执行文件路径
            val parts = exec.split(" ")
            val executablePath = parts[0]

            // 处理相对路径和绝对路径
            return if (executablePath.startsWith("/")) {
                executablePath
            } else {
                // 在 PATH 中查找
                findInPath(executablePath)
            }
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * 在 PATH 中查找可执行文件
     */
    private fun findInPath(executableName: String): String? {
        val pathEnv = System.getenv("PATH") ?: return null
        val pathDirs = pathEnv.split(":")

        for (pathDir in pathDirs) {
            val executableFile = File(pathDir, executableName)
            if (executableFile.exists() && executableFile.canExecute()) {
                return executableFile.absolutePath
            }
        }

        return null
    }

    /**
     * 扫描常见安装目录
     */
    private fun scanCommonDirectories(): List<EditorConfig> {
        val commonDirs = listOf(
            "/usr/bin",
            "/usr/local/bin",
            "/opt",
            "/snap/bin",
            "${System.getProperty("user.home")}/.local/bin",
            "/var/lib/flatpak/exports/bin"
        )

        val editors = mutableListOf<EditorConfig>()

        for (dir in commonDirs) {
            when (dir) {
                "/opt" -> {
                    // 扫描 /opt 目录下的应用程序
                    editors.addAll(scanOptDirectory(dir))
                }
                else -> {
                    // 扫描二进制目录
                    editors.addAll(scanBinaryDirectory(dir))
                }
            }
        }

        return editors
    }

    /**
     * 扫描 /opt 目录
     */
    private fun scanOptDirectory(optDir: String): List<EditorConfig> {
        val appDirs = safeScanDirectory(optDir) { file ->
            file.isDirectory && isKnownEditor(file.name)
        }

        return appDirs.mapNotNull { appDir ->
            findExecutableInOptDirectory(appDir)
        }
    }

    /**
     * 在 /opt 应用程序目录中查找可执行文件
     */
    private fun findExecutableInOptDirectory(appDir: File): EditorConfig? {
        try {
            // 查找 bin 目录
            val binDir = File(appDir, "bin")
            if (binDir.exists()) {
                val executables = binDir.listFiles { file ->
                    file.isFile && file.canExecute() && isKnownEditor(file.name)
                }

                val executable = executables?.firstOrNull()
                if (executable != null) {
                    val version = extractVersion(executable.absolutePath)

                    return createEditorConfig(
                        EditorAppMetadata(
                            appName = appDir.name,
                            appPath = appDir.absolutePath,
                            executablePath = executable.absolutePath,
                            version = version,
                            displayName = extractDisplayName(appDir.name),
                            installLocation = appDir.absolutePath
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // 忽略错误
        }

        return null
    }

    /**
     * 扫描二进制目录
     */
    private fun scanBinaryDirectory(binDir: String): List<EditorConfig> {
        val executables = safeScanDirectory(binDir) { file ->
            file.isFile && file.canExecute() && isKnownEditor(file.name)
        }

        return executables.mapNotNull { executable ->
            val version = extractVersion(executable.absolutePath)

            createEditorConfig(
                EditorAppMetadata(
                    appName = executable.name,
                    appPath = executable.absolutePath,
                    executablePath = executable.absolutePath,
                    version = version,
                    displayName = extractDisplayName(executable.name),
                    installLocation = executable.parent
                )
            )
        }
    }

    /**
     * 查询包管理器
     */
    private fun queryPackageManager(): List<EditorConfig> {
        val editors = mutableListOf<EditorConfig>()

        try {
            // 查询 dpkg (Debian/Ubuntu)
            editors.addAll(queryDpkg())

            // 查询 rpm (Red Hat/Fedora)
            editors.addAll(queryRpm())

            // 查询 pacman (Arch Linux)
            editors.addAll(queryPacman())

            // 查询 Snap
            editors.addAll(querySnap())

            // 查询 Flatpak
            editors.addAll(queryFlatpak())

        } catch (e: Exception) {
            // 忽略查询错误
        }

        return editors
    }

    /**
     * 查询 dpkg 包管理器
     */
    private fun queryDpkg(): List<EditorConfig> {
        try {
            val output = executeCommand("dpkg", "-l")
            return parseDpkgOutput(output)
        } catch (e: Exception) {
            return emptyList()
        }
    }

    /**
     * 查询 rpm 包管理器
     */
    private fun queryRpm(): List<EditorConfig> {
        try {
            val output = executeCommand("rpm", "-qa")
            return parseRpmOutput(output)
        } catch (e: Exception) {
            return emptyList()
        }
    }

    /**
     * 查询 pacman 包管理器
     */
    private fun queryPacman(): List<EditorConfig> {
        try {
            val output = executeCommand("pacman", "-Q")
            return parsePacmanOutput(output)
        } catch (e: Exception) {
            return emptyList()
        }
    }

    /**
     * 查询 Snap 包
     */
    private fun querySnap(): List<EditorConfig> {
        try {
            val output = executeCommand("snap", "list")
            return parseSnapOutput(output)
        } catch (e: Exception) {
            return emptyList()
        }
    }

    /**
     * 查询 Flatpak 应用
     */
    private fun queryFlatpak(): List<EditorConfig> {
        try {
            val output = executeCommand("flatpak", "list", "--app")
            return parseFlatpakOutput(output)
        } catch (e: Exception) {
            return emptyList()
        }
    }

    // 解析包管理器输出的方法
    private fun parseDpkgOutput(output: List<String>): List<EditorConfig> {
        return output.mapNotNull { line ->
            if (line.startsWith("ii") && isKnownEditor(line)) {
                val parts = line.split("\\s+".toRegex())
                if (parts.size >= 3) {
                    val packageName = parts[1]
                    val version = parts[2]
                    findPackageExecutable(packageName, version)
                } else null
            } else null
        }
    }

    private fun parseRpmOutput(output: List<String>): List<EditorConfig> = emptyList()
    private fun parsePacmanOutput(output: List<String>): List<EditorConfig> = emptyList()
    private fun parseSnapOutput(output: List<String>): List<EditorConfig> = emptyList()
    private fun parseFlatpakOutput(output: List<String>): List<EditorConfig> = emptyList()

    /**
     * 查找包的可执行文件
     */
    private fun findPackageExecutable(packageName: String, version: String): EditorConfig? {
        // 简化实现，在常见路径中查找
        val commonPaths = listOf("/usr/bin", "/usr/local/bin")

        for (path in commonPaths) {
            val executable = File(path, packageName)
            if (executable.exists() && executable.canExecute()) {
                return createEditorConfig(
                    EditorAppMetadata(
                        appName = packageName,
                        appPath = executable.absolutePath,
                        executablePath = executable.absolutePath,
                        version = version,
                        displayName = extractDisplayName(packageName)
                    )
                )
            }
        }

        return null
    }

    /**
     * 提取显示名称
     */
    private fun extractDisplayName(rawName: String): String {
        return rawName
            .replace("-", " ")
            .replace("_", " ")
            .split(" ")
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { it.uppercase() }
            }
    }
}