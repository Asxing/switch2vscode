package com.github.qczone.switch2vscode.discovery

import com.github.qczone.switch2vscode.model.EditorConfig
import com.github.qczone.switch2vscode.model.DiscoveryStrategy
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import java.util.concurrent.CompletableFuture

/**
 * 编辑器智能发现引擎
 * 负责跨平台自动发现已安装的编辑器，支持多种发现策略
 */
class EditorDiscoveryEngine {
    private val logger = Logger.getInstance(EditorDiscoveryEngine::class.java)
    private val commandLineStrategies: List<EditorDiscoveryStrategy>
    private val applicationServices: List<ApplicationDiscoveryService>

    init {
        // 初始化命令行发现策略（原有逻辑）
        commandLineStrategies = listOf(
            MacOSDiscoveryStrategy(),
            WindowsDiscoveryStrategy(),
            LinuxDiscoveryStrategy()
        ).filter { it.isSupported() }

        // 初始化应用程序发现服务（新增逻辑）
        applicationServices = listOf(
            MacOSApplicationDiscoveryService(),
            WindowsApplicationDiscoveryService(),
            LinuxApplicationDiscoveryService()
        ).filter { it.isSupported() }

        logger.info("Initialized EditorDiscoveryEngine")
        logger.info("  Command line strategies: ${commandLineStrategies.size}")
        logger.info("  Application services: ${applicationServices.size}")
    }

    /**
     * 同步发现编辑器（使用默认策略）
     * @return 发现的编辑器配置列表
     */
    fun discoverEditors(): List<EditorConfig> {
        return discoverWithStrategy(DiscoveryStrategy.getDefault())
    }

    /**
     * 使用指定策略发现编辑器
     * @param strategy 发现策略
     * @return 发现的编辑器配置列表
     */
    fun discoverWithStrategy(strategy: DiscoveryStrategy): List<EditorConfig> {
        logger.info("=== Starting Editor Discovery with ${strategy.displayName} ===")

        val discovered = mutableListOf<EditorConfig>()

        try {
            // 根据策略选择发现方法
            if (strategy.includesCommandLine()) {
                discovered.addAll(discoverWithCommandLine())
            }

            if (strategy.includesApplicationScan()) {
                discovered.addAll(discoverWithApplicationScan())
            }

        } catch (e: Exception) {
            logger.warn("Discovery with strategy ${strategy.displayName} failed", e)
        }

        logger.info("Total discovered before deduplication: ${discovered.size}")

        // 去重并排序
        val result = deduplicateAndSort(discovered)

        logger.info("Final result after deduplication: ${result.size}")
        result.forEach { config ->
            logger.info("  Final: ${config.displayName} at ${config.executablePath}")
        }

        return result
    }

    /**
     * 使用命令行策略发现编辑器
     */
    private fun discoverWithCommandLine(): List<EditorConfig> {
        logger.info("Executing command line discovery strategies")
        val discovered = mutableListOf<EditorConfig>()

        for (strategy in commandLineStrategies) {
            try {
                logger.info("Executing strategy: ${strategy.javaClass.simpleName}")
                val configs = strategy.discover()
                discovered.addAll(configs)
                logger.info("${strategy.javaClass.simpleName} discovered ${configs.size} editors")
                configs.forEach { config ->
                    logger.info("  Found: ${config.displayName} at ${config.executablePath}")
                }
            } catch (e: Exception) {
                logger.warn("Discovery strategy ${strategy.javaClass.simpleName} failed", e)
            }
        }

        return discovered
    }

    /**
     * 使用应用程序扫描发现编辑器
     */
    private fun discoverWithApplicationScan(): List<EditorConfig> {
        logger.info("Executing application discovery services")
        val discovered = mutableListOf<EditorConfig>()

        for (service in applicationServices) {
            try {
                logger.info("Executing service: ${service.getServiceName()}")
                val configs = service.discoverEditors()
                discovered.addAll(configs)
                logger.info("${service.getServiceName()} discovered ${configs.size} editors")
                configs.forEach { config ->
                    logger.info("  Found: ${config.displayName} at ${config.executablePath}")
                }
            } catch (e: Exception) {
                logger.warn("Discovery service ${service.getServiceName()} failed", e)
            }
        }

        return discovered
    }

    /**
     * 去重和排序
     */
    private fun deduplicateAndSort(editors: List<EditorConfig>): List<EditorConfig> {
        return editors
            .distinctBy { it.executablePath }
            .sortedWith(compareBy<EditorConfig> { !it.isDefault }
                .thenBy { it.displayName })
    }

    /**
     * 异步发现编辑器（使用默认策略）
     * @param callback 发现完成后的回调函数
     */
    fun discoverAsync(callback: (List<EditorConfig>) -> Unit) {
        discoverAsyncWithStrategy(DiscoveryStrategy.getDefault(), callback)
    }

    /**
     * 使用指定策略异步发现编辑器
     * @param strategy 发现策略
     * @param callback 发现完成后的回调函数
     */
    fun discoverAsyncWithStrategy(strategy: DiscoveryStrategy, callback: (List<EditorConfig>) -> Unit) {
        ApplicationManager.getApplication().executeOnPooledThread {
            val discovered = discoverWithStrategy(strategy)
            ApplicationManager.getApplication().invokeLater {
                callback(discovered)
            }
        }
    }

    /**
     * 异步发现编辑器，带调试日志回调（使用默认策略）
     */
    fun discoverAsyncWithDebug(debugCallback: (String) -> Unit, callback: (List<EditorConfig>) -> Unit) {
        discoverAsyncWithStrategyAndDebug(DiscoveryStrategy.getDefault(), debugCallback, callback)
    }

    /**
     * 使用指定策略异步发现编辑器，带调试日志回调
     */
    fun discoverAsyncWithStrategyAndDebug(
        strategy: DiscoveryStrategy,
        debugCallback: (String) -> Unit,
        callback: (List<EditorConfig>) -> Unit
    ) {
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                debugCallback("Starting discovery engine with ${strategy.displayName}...")
                val discovered = discoverWithStrategyAndDebug(strategy, debugCallback)
                debugCallback("Discovery engine completed with ${discovered.size} editors")
                ApplicationManager.getApplication().invokeLater {
                    callback(discovered)
                }
            } catch (e: Exception) {
                debugCallback("Discovery engine failed: ${e.message}")
                ApplicationManager.getApplication().invokeLater {
                    callback(emptyList())
                }
            }
        }
    }

    /**
     * 使用指定策略同步发现编辑器，带调试回调
     */
    private fun discoverWithStrategyAndDebug(strategy: DiscoveryStrategy, debugCallback: (String) -> Unit): List<EditorConfig> {
        debugCallback("=== Starting Editor Discovery with ${strategy.displayName} ===")
        debugCallback("Command line strategies: ${commandLineStrategies.size}")
        debugCallback("Application services: ${applicationServices.size}")

        val discovered = mutableListOf<EditorConfig>()

        try {
            // 根据策略选择发现方法
            if (strategy.includesCommandLine()) {
                debugCallback("Executing command line discovery...")
                discovered.addAll(discoverWithCommandLineAndDebug(debugCallback))
            }

            if (strategy.includesApplicationScan()) {
                debugCallback("Executing application discovery...")
                discovered.addAll(discoverWithApplicationScanAndDebug(debugCallback))
            }

        } catch (e: Exception) {
            debugCallback("Discovery with strategy ${strategy.displayName} failed: ${e.message}")
            logger.warn("Discovery with strategy ${strategy.displayName} failed", e)
        }

        debugCallback("Total discovered before deduplication: ${discovered.size}")

        // 去重并排序
        val result = deduplicateAndSort(discovered)

        debugCallback("Final result after deduplication: ${result.size}")
        result.forEach { config ->
            debugCallback("  Final: ${config.displayName} at ${config.executablePath}")
        }

        return result
    }

    /**
     * 使用命令行策略发现编辑器，带调试回调
     */
    private fun discoverWithCommandLineAndDebug(debugCallback: (String) -> Unit): List<EditorConfig> {
        val discovered = mutableListOf<EditorConfig>()

        for (strategy in commandLineStrategies) {
            try {
                debugCallback("Executing strategy: ${strategy.javaClass.simpleName}")
                val configs = strategy.discoverWithDebug(debugCallback)
                discovered.addAll(configs)
                debugCallback("${strategy.javaClass.simpleName} discovered ${configs.size} editors")
                configs.forEach { config ->
                    debugCallback("  Found: ${config.displayName} at ${config.executablePath}")
                }
            } catch (e: Exception) {
                debugCallback("Discovery strategy ${strategy.javaClass.simpleName} failed: ${e.message}")
                logger.warn("Discovery strategy ${strategy.javaClass.simpleName} failed", e)
            }
        }

        return discovered
    }

    /**
     * 使用应用程序扫描发现编辑器，带调试回调
     */
    private fun discoverWithApplicationScanAndDebug(debugCallback: (String) -> Unit): List<EditorConfig> {
        val discovered = mutableListOf<EditorConfig>()

        for (service in applicationServices) {
            try {
                debugCallback("Executing service: ${service.getServiceName()}")
                val configs = service.discoverEditors()
                discovered.addAll(configs)
                debugCallback("${service.getServiceName()} discovered ${configs.size} editors")
                configs.forEach { config ->
                    debugCallback("  Found: ${config.displayName} at ${config.executablePath}")
                }
            } catch (e: Exception) {
                debugCallback("Discovery service ${service.getServiceName()} failed: ${e.message}")
                logger.warn("Discovery service ${service.getServiceName()} failed", e)
            }
        }

        return discovered
    }

    /**
     * 异步发现编辑器，返回CompletableFuture
     */
    fun discoverAsyncFuture(): CompletableFuture<List<EditorConfig>> {
        val future = CompletableFuture<List<EditorConfig>>()

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val discovered = discoverEditors()
                future.complete(discovered)
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        }

        return future
    }

    /**
     * 验证特定路径是否为有效的编辑器
     * @param path 编辑器路径
     * @return 验证结果和编辑器配置
     */
    fun validateEditorPath(path: String): Pair<Boolean, EditorConfig?> {
        // 首先尝试命令行策略验证
        for (strategy in commandLineStrategies) {
            try {
                val config = strategy.validatePath(path)
                if (config != null) {
                    return Pair(true, config)
                }
            } catch (e: Exception) {
                logger.debug("Strategy ${strategy.javaClass.simpleName} validation failed for path: $path", e)
            }
        }
        return Pair(false, null)
    }

    /**
     * 获取支持的发现策略数量
     */
    fun getSupportedStrategiesCount(): Int = commandLineStrategies.size + applicationServices.size

    /**
     * 获取所有支持的策略名称
     */
    fun getSupportedStrategyNames(): List<String> {
        val names = mutableListOf<String>()
        names.addAll(commandLineStrategies.map { it.javaClass.simpleName })
        names.addAll(applicationServices.map { it.getServiceName() })
        return names
    }

    /**
     * 获取可用的发现策略列表
     */
    fun getAvailableStrategies(): List<DiscoveryStrategy> = DiscoveryStrategy.values().toList()

    /**
     * 获取推荐的发现策略
     */
    fun getRecommendedStrategy(): DiscoveryStrategy = DiscoveryStrategy.COMPREHENSIVE
}

/**
 * 编辑器发现策略接口
 * 定义跨平台发现编辑器的统一接口
 */
interface EditorDiscoveryStrategy {
    /**
     * 检查当前策略是否支持当前平台
     */
    fun isSupported(): Boolean

    /**
     * 发现编辑器
     * @return 发现的编辑器配置列表
     */
    fun discover(): List<EditorConfig>

    /**
     * 发现编辑器，带调试回调
     * @param debugCallback 调试信息回调
     * @return 发现的编辑器配置列表
     */
    fun discoverWithDebug(debugCallback: (String) -> Unit): List<EditorConfig> = discover()

    /**
     * 验证指定路径是否为有效的编辑器
     * @param path 编辑器路径
     * @return 如果有效则返回EditorConfig，否则返回null
     */
    fun validatePath(path: String): EditorConfig?

    /**
     * 获取策略名称
     */
    fun getStrategyName(): String = this.javaClass.simpleName
}