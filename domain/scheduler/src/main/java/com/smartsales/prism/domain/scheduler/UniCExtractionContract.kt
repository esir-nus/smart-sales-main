package com.smartsales.prism.domain.scheduler

import kotlinx.serialization.Serializable

/**
 * Uni-C 轻量提取请求。
 * 说明：这是 Path A Inspiration 的边界输入，不是机器路由输出 schema。
 */
data class UniCExtractionRequest(
    val transcript: String,
    val nowIso: String,
    val timezone: String,
    val unifiedId: String
)

/**
 * Uni-C 轻量提取响应。
 * 必须由轻量模型严格输出，并由 SchedulerLinter 直接反序列化。
 */
@Serializable
data class UniCExtractionPayload(
    /** MUST BE one of: INSPIRATION_CREATE, NOT_INSPIRATION */
    val decision: String = "NOT_INSPIRATION",
    /** NOT_INSPIRATION 时用于说明退出原因；INSPIRATION_CREATE 时可为空。 */
    val reason: String? = null,
    /** INSPIRATION_CREATE 时必须提供；NOT_INSPIRATION 时必须为空。 */
    val idea: UniCIdeaPayload? = null
)

/**
 * Uni-C 灵感载荷。
 */
@Serializable
data class UniCIdeaPayload(
    /** 核心内容必须为非空自然语言。 */
    val content: String,
    /** 可选短标题，仅作展示辅助。 */
    val title: String? = null
)
