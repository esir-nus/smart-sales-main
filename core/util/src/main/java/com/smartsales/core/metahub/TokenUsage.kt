package com.smartsales.core.metahub

// 文件：core/util/src/main/java/com/smartsales/core/metahub/TokenUsage.kt
// 模块：:core:util
// 说明：记录一次模型调用的用量元数据
// 作者：创建于 2025-12-04

/**
 * 模型调用用量，仅记录数字和模型标识，不含业务内容。
 */
data class TokenUsage(
    val sessionId: String,
    val model: String,
    val tokensIn: Long,
    val tokensOut: Long,
    val latencyMs: Long,
    val timestamp: Long
)
