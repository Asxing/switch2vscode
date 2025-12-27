package com.github.qczone.switch2vscode.discovery

import com.github.qczone.switch2vscode.model.EditorConfig
import com.github.qczone.switch2vscode.model.EditorType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.SystemInfo
import java.io.File
import java.util.*

/**
 * macOS 编辑器发现策略
 * 通过扫描Applications目录和.app包来发现编辑器
 */
class MacOSDiscoveryStrategy : EditorDiscoveryStrategy {
    private val logger = Logger.getInstance(MacOSDiscoveryStrategy::class.java)

    override fun isSupported(): Boolean = SystemInfo.isMac

    override fun discover(): List<EditorConfig> {
        val discovered = mutableListOf<EditorConfig>()

        // 仅使用 which 命令查找可执行文件路径
        discovered.addAll(discoverViaWhichCommand())

        logger.info("macOS strategy discovered ${discovered.size} editors via which command")
        return discovered
    }

    override fun discoverWithDebug(debugCallback: (String) -> Unit): List<EditorConfig> {
        val discovered = mutableListOf<EditorConfig>()

        // 仅使用 which 命令查找可执行文件路径
        discovered.addAll(discoverViaWhichCommandWithDebug(debugCallback))

        debugCallback("macOS strategy discovered ${discovered.size} editors via which command")
        return discovered
    }

    /**
     * 使用 which 命令发现可执行文件路径
     */
    private fun discoverViaWhichCommand(): List<EditorConfig> {
        logger.info("MacOS: Starting which command discovery")
        val discovered = mutableListOf<EditorConfig>()

        // 定义编辑器命令映射
        val editorCommands = mapOf(
            "code" to EditorType.VSCODE,
            "cursor" to EditorType.CURSOR,
            "windsurf" to EditorType.WINDSURF,
            "antigravity" to EditorType.ANTIGRAVITY,
            "catpaw" to EditorType.CATPAW,
            "trae" to EditorType.TRAE
        )

        logger.info("MacOS: Testing ${editorCommands.size} commands: ${editorCommands.keys}")

        editorCommands.forEach { (command, editorType) ->
            try {
                logger.info("MacOS: === Executing 'which $command' ===")

                // 检查环境变量
                val pathEnv = System.getenv("PATH")
                logger.info("MacOS: Original PATH environment = ${pathEnv?.take(200)}...")

                // 为macOS添加常见的编辑器安装路径
                val additionalPaths = listOf(
                    "/opt/homebrew/bin",
                    "/usr/local/bin",
                    "/Applications/Visual Studio Code.app/Contents/Resources/app/bin",
                    "/Applications/Cursor.app/Contents/Resources/app/bin",
                    "/Applications/Windsurf.app/Contents/Resources/app/bin",
                    "/Applications/AntiGravity.app/Contents/Resources/app/bin",
                    "/Applications/CatPaw.app/Contents/Resources/app/bin",
                    "/Applications/Trae.app/Contents/Resources/app/bin"
                )

                val enhancedPath = if (pathEnv != null) {
                    val existingPaths = pathEnv.split(":")
                    val newPaths = additionalPaths.filter { path ->
                        !existingPaths.contains(path) && File(path).exists()
                    }
                    if (newPaths.isNotEmpty()) {
                        (newPaths + existingPaths).joinToString(":")
                    } else {
                        pathEnv
                    }
                } else {
                    additionalPaths.filter { File(it).exists() }.joinToString(":")
                }

                logger.info("MacOS: Enhanced PATH = ${enhancedPath.take(200)}...")

                val processBuilder = ProcessBuilder("which", command)
                    .redirectErrorStream(true)

                // 设置增强的PATH环境变量
                if (enhancedPath != pathEnv) {
                    processBuilder.environment()["PATH"] = enhancedPath
                    logger.info("MacOS: Updated ProcessBuilder PATH environment")
                }

                logger.info("MacOS: ProcessBuilder created with command: ${processBuilder.command()}")

                val process = processBuilder.start()
                logger.info("MacOS: Process started for 'which $command'")

                val output = process.inputStream.bufferedReader().use { it.readText().trim() }
                logger.info("MacOS: Raw output from 'which $command': '$output'")

                val completed = process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)
                val exitValue = if (completed) process.exitValue() else -1

                logger.info("MacOS: Process completed=$completed, exitValue=$exitValue")
                logger.info("MacOS: Output length=${output.length}, isEmpty=${output.isEmpty()}")
                logger.info("MacOS: Output contains 'not found'=${output.contains("not found")}")

                if (completed && exitValue == 0 && output.isNotEmpty() && !output.contains("not found")) {
                    val config = EditorConfig(
                        id = editorType.id,
                        displayName = editorType.displayName,
                        executablePath = output,
                        isAutoDiscovered = true,
                        lastValidated = System.currentTimeMillis()
                    )
                    discovered.add(config)
                    logger.info("MacOS: ✓ Successfully found ${editorType.displayName} via which: $output")
                } else {
                    logger.info("MacOS: ✗ Command '$command' not found or failed (completed=$completed, exitValue=$exitValue, output='$output')")
                }
            } catch (e: Exception) {
                logger.warn("MacOS: ✗ Exception while finding $command via which command: ${e.message}", e)
            }
        }

        logger.info("MacOS: === Discovery completed, found ${discovered.size} editors ===")
        discovered.forEach { config ->
            logger.info("MacOS: Found editor: ${config.displayName} at ${config.executablePath}")
        }
        return discovered
    }

    /**
     * 使用 which 命令发现可执行文件路径（带调试回调）
     */
    private fun discoverViaWhichCommandWithDebug(debugCallback: (String) -> Unit): List<EditorConfig> {
        debugCallback("MacOS: Starting which command discovery")
        val discovered = mutableListOf<EditorConfig>()

        // 定义编辑器命令映射
        val editorCommands = mapOf(
            "code" to EditorType.VSCODE,
            "cursor" to EditorType.CURSOR,
            "windsurf" to EditorType.WINDSURF,
            "antigravity" to EditorType.ANTIGRAVITY,
            "catpaw" to EditorType.CATPAW,
            "trae" to EditorType.TRAE
        )

        debugCallback("MacOS: Testing ${editorCommands.size} commands: ${editorCommands.keys}")

        editorCommands.forEach { (command, editorType) ->
            try {
                debugCallback("MacOS: === Executing 'which $command' ===")

                // 检查环境变量
                val pathEnv = System.getenv("PATH")
                debugCallback("MacOS: Original PATH environment = ${pathEnv?.take(200)}...")

                // 为macOS添加常见的编辑器安装路径
                val additionalPaths = listOf(
                    "/opt/homebrew/bin",
                    "/usr/local/bin",
                    "/Applications/Visual Studio Code.app/Contents/Resources/app/bin",
                    "/Applications/Cursor.app/Contents/Resources/app/bin",
                    "/Applications/Windsurf.app/Contents/Resources/app/bin",
                    "/Applications/AntiGravity.app/Contents/Resources/app/bin",
                    "/Applications/CatPaw.app/Contents/Resources/app/bin",
                    "/Applications/Trae.app/Contents/Resources/app/bin"
                )

                val enhancedPath = if (pathEnv != null) {
                    val existingPaths = pathEnv.split(":")
                    val newPaths = additionalPaths.filter { path ->
                        !existingPaths.contains(path) && File(path).exists()
                    }
                    if (newPaths.isNotEmpty()) {
                        (newPaths + existingPaths).joinToString(":")
                    } else {
                        pathEnv
                    }
                } else {
                    additionalPaths.filter { File(it).exists() }.joinToString(":")
                }

                debugCallback("MacOS: Enhanced PATH = ${enhancedPath.take(200)}...")

                val processBuilder = ProcessBuilder("which", command)
                    .redirectErrorStream(true)

                // 设置增强的PATH环境变量
                if (enhancedPath != pathEnv) {
                    processBuilder.environment()["PATH"] = enhancedPath
                    debugCallback("MacOS: Updated ProcessBuilder PATH environment")
                }

                debugCallback("MacOS: ProcessBuilder created with command: ${processBuilder.command()}")

                val process = processBuilder.start()
                debugCallback("MacOS: Process started for 'which $command'")

                val output = process.inputStream.bufferedReader().use { it.readText().trim() }
                debugCallback("MacOS: Raw output from 'which $command': '$output'")

                val completed = process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)
                val exitValue = if (completed) process.exitValue() else -1

                debugCallback("MacOS: Process completed=$completed, exitValue=$exitValue")
                debugCallback("MacOS: Output length=${output.length}, isEmpty=${output.isEmpty()}")
                debugCallback("MacOS: Output contains 'not found'=${output.contains("not found")}")

                if (completed && exitValue == 0 && output.isNotEmpty() && !output.contains("not found")) {
                    val config = EditorConfig(
                        id = editorType.id,
                        displayName = editorType.displayName,
                        executablePath = output,
                        isAutoDiscovered = true,
                        lastValidated = System.currentTimeMillis()
                    )
                    discovered.add(config)
                    debugCallback("MacOS: ✓ Successfully found ${editorType.displayName} via which: $output")
                } else {
                    debugCallback("MacOS: ✗ Command '$command' not found or failed (completed=$completed, exitValue=$exitValue, output='$output')")
                }
            } catch (e: Exception) {
                debugCallback("MacOS: ✗ Exception while finding $command via which command: ${e.message}")
                logger.warn("MacOS: ✗ Exception while finding $command via which command: ${e.message}", e)
            }
        }

        debugCallback("MacOS: === Discovery completed, found ${discovered.size} editors ===")
        discovered.forEach { config ->
            debugCallback("MacOS: Found editor: ${config.displayName} at ${config.executablePath}")
        }
        return discovered
    }

    override fun validatePath(path: String): EditorConfig? {
        val file = File(path)
        return if (file.exists() && file.canExecute()) {
            val editorType = EditorType.detectFromPath(path)
            EditorConfig(
                id = editorType.id,
                displayName = editorType.displayName,
                executablePath = path,
                isAutoDiscovered = false,
                lastValidated = System.currentTimeMillis()
            )
        } else null
    }
}

/**
 * Windows 编辑器发现策略
 * 通过注册表查询、标准安装路径和PATH环境变量来发现编辑器
 */
class WindowsDiscoveryStrategy : EditorDiscoveryStrategy {
    private val logger = Logger.getInstance(WindowsDiscoveryStrategy::class.java)

    override fun isSupported(): Boolean = SystemInfo.isWindows

    override fun discover(): List<EditorConfig> {
        val discovered = mutableListOf<EditorConfig>()

        // 仅使用 where 命令查找可执行文件路径
        discovered.addAll(discoverViaWhereCommand())

        logger.info("Windows strategy discovered ${discovered.size} editors via where command")
        return discovered.distinctBy { it.executablePath }
    }

    override fun discoverWithDebug(debugCallback: (String) -> Unit): List<EditorConfig> {
        val discovered = mutableListOf<EditorConfig>()

        // 仅使用 where 命令查找可执行文件路径
        discovered.addAll(discoverViaWhereCommandWithDebug(debugCallback))

        debugCallback("Windows strategy discovered ${discovered.size} editors via where command")
        return discovered.distinctBy { it.executablePath }
    }

    /**
     * 使用 where 命令发现可执行文件路径
     */
    private fun discoverViaWhereCommand(): List<EditorConfig> {
        logger.info("Windows: Starting where command discovery")
        val discovered = mutableListOf<EditorConfig>()

        // 定义编辑器命令映射
        val editorCommands = mapOf(
            "code" to EditorType.VSCODE,
            "cursor" to EditorType.CURSOR,
            "windsurf" to EditorType.WINDSURF,
            "antigravity" to EditorType.ANTIGRAVITY,
            "catpaw" to EditorType.CATPAW,
            "trae" to EditorType.TRAE
        )

        logger.info("Windows: Testing ${editorCommands.size} commands: ${editorCommands.keys}")

        editorCommands.forEach { (command, editorType) ->
            try {
                logger.info("Windows: === Executing 'where $command' ===")

                // 检查环境变量
                val pathEnv = System.getenv("PATH")
                logger.info("Windows: PATH environment = ${pathEnv?.take(200)}...")

                val processBuilder = ProcessBuilder("where", command)
                    .redirectErrorStream(true)

                logger.info("Windows: ProcessBuilder created with command: ${processBuilder.command()}")

                val process = processBuilder.start()
                logger.info("Windows: Process started for 'where $command'")

                val output = process.inputStream.bufferedReader().use { it.readText().trim() }
                logger.info("Windows: Raw output from 'where $command': '$output'")

                val completed = process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)
                val exitValue = if (completed) process.exitValue() else -1

                logger.info("Windows: Process completed=$completed, exitValue=$exitValue")
                logger.info("Windows: Output length=${output.length}, isEmpty=${output.isEmpty()}")
                logger.info("Windows: Output contains 'Could not find'=${output.contains("Could not find")}")

                if (completed && exitValue == 0 && output.isNotEmpty() && !output.contains("Could not find")) {
                    // Windows where 命令可能返回多个路径，取第一个
                    val firstPath = output.split("\r\n", "\n").first().trim()
                    logger.info("Windows: First path extracted: '$firstPath'")

                    if (File(firstPath).exists()) {
                        val config = EditorConfig(
                            id = editorType.id,
                            displayName = editorType.displayName,
                            executablePath = firstPath,
                            isAutoDiscovered = true,
                            lastValidated = System.currentTimeMillis()
                        )
                        discovered.add(config)
                        logger.info("Windows: ✓ Successfully found ${editorType.displayName} via where: $firstPath")
                    } else {
                        logger.info("Windows: ✗ Path does not exist: $firstPath")
                    }
                } else {
                    logger.info("Windows: ✗ Command '$command' not found or failed (completed=$completed, exitValue=$exitValue, output='$output')")
                }
            } catch (e: Exception) {
                logger.warn("Windows: ✗ Exception while finding $command via where command: ${e.message}", e)
            }
        }

        logger.info("Windows: === Discovery completed, found ${discovered.size} editors ===")
        discovered.forEach { config ->
            logger.info("Windows: Found editor: ${config.displayName} at ${config.executablePath}")
        }
        return discovered
    }

    /**
     * 使用 where 命令发现可执行文件路径（带调试回调）
     */
    private fun discoverViaWhereCommandWithDebug(debugCallback: (String) -> Unit): List<EditorConfig> {
        debugCallback("Windows: Starting where command discovery")
        val discovered = mutableListOf<EditorConfig>()

        // 定义编辑器命令映射
        val editorCommands = mapOf(
            "code" to EditorType.VSCODE,
            "cursor" to EditorType.CURSOR,
            "windsurf" to EditorType.WINDSURF,
            "antigravity" to EditorType.ANTIGRAVITY,
            "catpaw" to EditorType.CATPAW,
            "trae" to EditorType.TRAE
        )

        debugCallback("Windows: Testing ${editorCommands.size} commands: ${editorCommands.keys}")

        editorCommands.forEach { (command, editorType) ->
            try {
                debugCallback("Windows: === Executing 'where $command' ===")

                // 检查环境变量
                val pathEnv = System.getenv("PATH")
                debugCallback("Windows: PATH environment = ${pathEnv?.take(200)}...")

                val processBuilder = ProcessBuilder("where", command)
                    .redirectErrorStream(true)

                debugCallback("Windows: ProcessBuilder created with command: ${processBuilder.command()}")

                val process = processBuilder.start()
                debugCallback("Windows: Process started for 'where $command'")

                val output = process.inputStream.bufferedReader().use { it.readText().trim() }
                debugCallback("Windows: Raw output from 'where $command': '$output'")

                val completed = process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)
                val exitValue = if (completed) process.exitValue() else -1

                debugCallback("Windows: Process completed=$completed, exitValue=$exitValue")
                debugCallback("Windows: Output length=${output.length}, isEmpty=${output.isEmpty()}")
                debugCallback("Windows: Output contains 'Could not find'=${output.contains("Could not find")}")

                if (completed && exitValue == 0 && output.isNotEmpty() && !output.contains("Could not find")) {
                    // Windows where 命令可能返回多个路径，取第一个
                    val firstPath = output.split("\r\n", "\n").first().trim()
                    debugCallback("Windows: First path extracted: '$firstPath'")

                    if (File(firstPath).exists()) {
                        val config = EditorConfig(
                            id = editorType.id,
                            displayName = editorType.displayName,
                            executablePath = firstPath,
                            isAutoDiscovered = true,
                            lastValidated = System.currentTimeMillis()
                        )
                        discovered.add(config)
                        debugCallback("Windows: ✓ Successfully found ${editorType.displayName} via where: $firstPath")
                    } else {
                        debugCallback("Windows: ✗ Path does not exist: $firstPath")
                    }
                } else {
                    debugCallback("Windows: ✗ Command '$command' not found or failed (completed=$completed, exitValue=$exitValue, output='$output')")
                }
            } catch (e: Exception) {
                debugCallback("Windows: ✗ Exception while finding $command via where command: ${e.message}")
                logger.warn("Windows: ✗ Exception while finding $command via where command: ${e.message}", e)
            }
        }

        debugCallback("Windows: === Discovery completed, found ${discovered.size} editors ===")
        discovered.forEach { config ->
            debugCallback("Windows: Found editor: ${config.displayName} at ${config.executablePath}")
        }
        return discovered
    }

    override fun validatePath(path: String): EditorConfig? {
        val file = File(path)
        return if (file.exists() && file.canExecute()) {
            val editorType = EditorType.detectFromPath(path)
            EditorConfig(
                id = editorType.id,
                displayName = editorType.displayName,
                executablePath = path,
                isAutoDiscovered = false,
                lastValidated = System.currentTimeMillis()
            )
        } else null
    }
}

/**
 * Linux 编辑器发现策略
 * 通过which命令、标准路径和包管理器来发现编辑器
 */
class LinuxDiscoveryStrategy : EditorDiscoveryStrategy {
    private val logger = Logger.getInstance(LinuxDiscoveryStrategy::class.java)

    override fun isSupported(): Boolean = SystemInfo.isLinux

    override fun discover(): List<EditorConfig> {
        val discovered = mutableListOf<EditorConfig>()

        // 仅使用which命令查找
        discovered.addAll(scanWithWhichCommand())

        logger.info("Linux strategy discovered ${discovered.size} editors via which command")
        return discovered.distinctBy { it.executablePath }
    }

    override fun discoverWithDebug(debugCallback: (String) -> Unit): List<EditorConfig> {
        val discovered = mutableListOf<EditorConfig>()

        // 仅使用which命令查找
        discovered.addAll(scanWithWhichCommandWithDebug(debugCallback))

        debugCallback("Linux strategy discovered ${discovered.size} editors via which command")
        return discovered.distinctBy { it.executablePath }
    }

    override fun validatePath(path: String): EditorConfig? {
        val file = File(path)
        return if (file.exists() && file.canExecute()) {
            val editorType = EditorType.detectFromPath(path)
            EditorConfig(
                id = editorType.id,
                displayName = editorType.displayName,
                executablePath = path,
                isAutoDiscovered = false,
                lastValidated = System.currentTimeMillis()
            )
        } else null
    }

    private fun scanWithWhichCommand(): List<EditorConfig> {
        logger.info("Linux: Starting which command discovery")
        val discovered = mutableListOf<EditorConfig>()

        // 定义编辑器命令映射
        val editorCommands = mapOf(
            "code" to EditorType.VSCODE,
            "cursor" to EditorType.CURSOR,
            "windsurf" to EditorType.WINDSURF,
            "antigravity" to EditorType.ANTIGRAVITY,
            "catpaw" to EditorType.CATPAW,
            "trae" to EditorType.TRAE
        )

        logger.info("Linux: Testing ${editorCommands.size} commands: ${editorCommands.keys}")

        editorCommands.forEach { (command, editorType) ->
            try {
                logger.info("Linux: === Executing 'which $command' ===")

                // 检查环境变量
                val pathEnv = System.getenv("PATH")
                logger.info("Linux: PATH environment = ${pathEnv?.take(200)}...")

                val processBuilder = ProcessBuilder("which", command)
                    .redirectErrorStream(true)

                logger.info("Linux: ProcessBuilder created with command: ${processBuilder.command()}")

                val process = processBuilder.start()
                logger.info("Linux: Process started for 'which $command'")

                val output = process.inputStream.bufferedReader().use { it.readText().trim() }
                logger.info("Linux: Raw output from 'which $command': '$output'")

                val completed = process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)
                val exitValue = if (completed) process.exitValue() else -1

                logger.info("Linux: Process completed=$completed, exitValue=$exitValue")
                logger.info("Linux: Output length=${output.length}, isEmpty=${output.isEmpty()}")
                logger.info("Linux: Output contains 'not found'=${output.contains("not found")}")

                if (completed && exitValue == 0 && output.isNotEmpty() && !output.contains("not found")) {
                    val config = EditorConfig(
                        id = editorType.id,
                        displayName = editorType.displayName,
                        executablePath = output,
                        isAutoDiscovered = true,
                        lastValidated = System.currentTimeMillis()
                    )
                    discovered.add(config)
                    logger.info("Linux: ✓ Successfully found ${editorType.displayName} via which: $output")
                } else {
                    logger.info("Linux: ✗ Command '$command' not found or failed (completed=$completed, exitValue=$exitValue, output='$output')")
                }
            } catch (e: Exception) {
                logger.warn("Linux: ✗ Exception while finding $command via which command: ${e.message}", e)
            }
        }

        logger.info("Linux: === Discovery completed, found ${discovered.size} editors ===")
        discovered.forEach { config ->
            logger.info("Linux: Found editor: ${config.displayName} at ${config.executablePath}")
        }
        return discovered
    }

    private fun scanWithWhichCommandWithDebug(debugCallback: (String) -> Unit): List<EditorConfig> {
        debugCallback("Linux: Starting which command discovery")
        val discovered = mutableListOf<EditorConfig>()

        // 定义编辑器命令映射
        val editorCommands = mapOf(
            "code" to EditorType.VSCODE,
            "cursor" to EditorType.CURSOR,
            "windsurf" to EditorType.WINDSURF,
            "antigravity" to EditorType.ANTIGRAVITY,
            "catpaw" to EditorType.CATPAW,
            "trae" to EditorType.TRAE
        )

        debugCallback("Linux: Testing ${editorCommands.size} commands: ${editorCommands.keys}")

        editorCommands.forEach { (command, editorType) ->
            try {
                debugCallback("Linux: === Executing 'which $command' ===")

                // 检查环境变量
                val pathEnv = System.getenv("PATH")
                debugCallback("Linux: PATH environment = ${pathEnv?.take(200)}...")

                val processBuilder = ProcessBuilder("which", command)
                    .redirectErrorStream(true)

                debugCallback("Linux: ProcessBuilder created with command: ${processBuilder.command()}")

                val process = processBuilder.start()
                debugCallback("Linux: Process started for 'which $command'")

                val output = process.inputStream.bufferedReader().use { it.readText().trim() }
                debugCallback("Linux: Raw output from 'which $command': '$output'")

                val completed = process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)
                val exitValue = if (completed) process.exitValue() else -1

                debugCallback("Linux: Process completed=$completed, exitValue=$exitValue")
                debugCallback("Linux: Output length=${output.length}, isEmpty=${output.isEmpty()}")
                debugCallback("Linux: Output contains 'not found'=${output.contains("not found")}")

                if (completed && exitValue == 0 && output.isNotEmpty() && !output.contains("not found")) {
                    val config = EditorConfig(
                        id = editorType.id,
                        displayName = editorType.displayName,
                        executablePath = output,
                        isAutoDiscovered = true,
                        lastValidated = System.currentTimeMillis()
                    )
                    discovered.add(config)
                    debugCallback("Linux: ✓ Successfully found ${editorType.displayName} via which: $output")
                } else {
                    debugCallback("Linux: ✗ Command '$command' not found or failed (completed=$completed, exitValue=$exitValue, output='$output')")
                }
            } catch (e: Exception) {
                debugCallback("Linux: ✗ Exception while finding $command via which command: ${e.message}")
                logger.warn("Linux: ✗ Exception while finding $command via which command: ${e.message}", e)
            }
        }

        debugCallback("Linux: === Discovery completed, found ${discovered.size} editors ===")
        discovered.forEach { config ->
            debugCallback("Linux: Found editor: ${config.displayName} at ${config.executablePath}")
        }
        return discovered
    }
}