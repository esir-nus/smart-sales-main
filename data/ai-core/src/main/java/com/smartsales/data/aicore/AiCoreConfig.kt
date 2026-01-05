package com.smartsales.data.aicore

// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/AiCoreConfig.kt
// 模块：:data:ai-core
// 说明：定义AI核心模块可配置项，便于不同App切换真/假实现
// 作者：创建于 2025-11-16

/**
 * 说话人展示配置，用于将 Tingwu 返回的 SpeakerId 映射为更友好的角色名称。
 *
 * @param defaultLabelsByIndex 按索引顺序给出的默认角色名，例如 ["客户", "销售"]。调试用途，生产不应依赖此角色默认值。
 * @param hideLabelWhenSingleSpeaker 当只有一个说话人时，是否隐藏标签，仅展示时间 + 文本。
 */
data class SpeakerDisplayConfig(
    val defaultLabelsByIndex: List<String> = emptyList(),
    val hideLabelWhenSingleSpeaker: Boolean = true,
)

data class AiCoreConfig(
    val preferFakeAiChat: Boolean = false,
    val enableV1ChatPublisher: Boolean = false,
    val enableV1TingwuMacroWindowFilter: Boolean = true,
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
    val tingwuGlobalPollTimeoutMillis: Long = 600_000,
    // V1 spec §8.1: Tingwu retry policy
    val tingwuMaxRetries: Int = 3,
    val tingwuRetryBackoffSeconds: List<Int> = listOf(60, 120, 300),
    val speakerDisplayConfig: SpeakerDisplayConfig = SpeakerDisplayConfig(),
)
