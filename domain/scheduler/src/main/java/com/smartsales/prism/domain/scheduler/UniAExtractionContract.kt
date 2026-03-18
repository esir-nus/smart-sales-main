package com.smartsales.prism.domain.scheduler

import kotlinx.serialization.Serializable

/**
 * Uni-A 轻量提取请求。
 * 说明：这是 Path A Exact Create 的边界输入，不是机器路由输出 schema。
 */
data class UniAExtractionRequest(
    val transcript: String,
    val nowIso: String,
    val timezone: String,
    val unifiedId: String,
    /** 当前日历页锚点；仅当用户使用“下一天/后一天”等页面相对表达时提供。 */
    val displayedDateIso: String? = null
)

/**
 * Uni-A 轻量提取响应。
 * 必须由轻量模型严格输出，并由 SchedulerLinter 直接反序列化。
 */
@Serializable
data class UniAExtractionPayload(
    /** MUST BE one of: EXACT_CREATE, NOT_EXACT */
    val decision: String = "NOT_EXACT",
    /** NOT_EXACT 时用于说明退出原因；EXACT_CREATE 时可为空。 */
    val reason: String? = null,
    /** EXACT_CREATE 时必须提供；NOT_EXACT 时必须为空。 */
    val task: UniAExactTaskPayload? = null
)

/**
 * Uni-A 精确任务载荷。
 */
@Serializable
data class UniAExactTaskPayload(
    /** 标题必须为非空自然语言。 */
    val title: String,
    /** MUST BE strict ISO-8601 with offset / Z, so deterministic code can parse it via Instant.parse(). */
    val startTimeIso: String,
    /** MUST BE integer minutes. FIRE_OFF 可为 0。 */
    val durationMinutes: Int = 0,
    /** MUST BE one of: L1, L2, L3, FIRE_OFF */
    val urgency: String = "L3"
)
