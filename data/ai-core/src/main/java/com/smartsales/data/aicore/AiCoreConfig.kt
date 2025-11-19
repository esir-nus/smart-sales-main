package com.smartsales.data.aicore

// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/AiCoreConfig.kt
// 模块：:data:ai-core
// 说明：定义AI核心模块可配置项，便于不同App切换真/假实现
// 作者：创建于 2025-11-16
data class AiCoreConfig(
    val preferFakeAiChat: Boolean = false,
    val dashscopeMaxRetries: Int = 1,
    val dashscopeRequestTimeoutMillis: Long = 15_000,
    val dashscopeEnableStreaming: Boolean = false,
    val preferFakeTingwu: Boolean = false,
    val preferFakeExport: Boolean = false,
    val tingwuPollIntervalMillis: Long = 5_000,
    val tingwuPollTimeoutMillis: Long = 600_000,
    val enableTingwuHttpLogging: Boolean = false,
    val tingwuVerboseLogging: Boolean = false,
    val requireTingwuSecurityToken: Boolean = false,
    val tingwuPresignUrlValiditySeconds: Long = 172_800,
    val tingwuModelOverride: String? = null,
    val tingwuBaseUrlOverride: String? = null,
    val tingwuConnectionPoolMaxIdle: Int = 5,
    val tingwuConnectionPoolKeepAliveMinutes: Long = 1,
    val tingwuConnectTimeoutMillis: Long = 20_000,
    val tingwuReadTimeoutMillis: Long = 20_000,
    val tingwuWriteTimeoutMillis: Long = 20_000,
    val tingwuInitialPollDelayMillis: Long = 2_000,
    val enableTingwuHttpDns: Boolean = false,
    val enableTingwuNetworkEventLog: Boolean = false,
    val tingwuGlobalPollTimeoutMillis: Long = 600_000
)
