package com.github.qczone.switch2vscode.model

/**
 * 编辑器发现策略枚举
 * 定义不同的编辑器发现方式和策略
 */
enum class DiscoveryStrategy(
    val id: String,
    val displayName: String,
    val description: String,
    val estimatedTime: String
) {
    /**
     * 快速发现 - 仅使用命令行方式
     * 最快速度，但可能遗漏一些编辑器
     */
    FAST(
        "fast",
        "Fast Discovery",
        "Quick command-line discovery only",
        "< 1 second"
    ),

    /**
     * 全面发现 - 命令行 + 应用程序扫描
     * 平衡速度和完整性
     */
    COMPREHENSIVE(
        "comprehensive",
        "Comprehensive Discovery",
        "Command-line + application directory scanning",
        "2-5 seconds"
    ),

    /**
     * 智能发现 - 全部方式 + 缓存优化
     * 最全面的发现，带智能缓存
     */
    SMART(
        "smart",
        "Smart Discovery",
        "All discovery methods with intelligent caching",
        "1-3 seconds (cached)"
    );

    /**
     * 是否包含命令行发现
     */
    fun includesCommandLine(): Boolean = true

    /**
     * 是否包含应用程序发现
     */
    fun includesApplicationScan(): Boolean = when (this) {
        FAST -> false
        COMPREHENSIVE, SMART -> true
    }

    /**
     * 是否使用缓存
     */
    fun usesCaching(): Boolean = when (this) {
        FAST, COMPREHENSIVE -> false
        SMART -> true
    }

    /**
     * 获取超时时间（毫秒）
     */
    fun getTimeoutMs(): Long = when (this) {
        FAST -> 2000L        // 2 秒
        COMPREHENSIVE -> 10000L   // 10 秒
        SMART -> 15000L      // 15 秒
    }

    companion object {
        /**
         * 根据ID获取策略
         */
        fun fromId(id: String): DiscoveryStrategy? {
            return values().find { it.id == id }
        }

        /**
         * 获取默认策略
         */
        fun getDefault(): DiscoveryStrategy = COMPREHENSIVE
    }
}