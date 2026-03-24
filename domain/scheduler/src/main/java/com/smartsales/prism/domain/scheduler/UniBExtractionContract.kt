package com.smartsales.prism.domain.scheduler

import kotlinx.serialization.Serializable

/**
 * Uni-B 模糊提取请求。
 * 说明：用于 Path A vague-create，不是机器路由输出 schema。
 */
data class UniBExtractionRequest(
    val transcript: String,
    val normalizedTranscript: String? = null,
    val nowIso: String,
    val timezone: String,
    val unifiedId: String,
    /** 当前日历页锚点；仅当用户使用“下一天/后一天”等页面相对表达时提供。 */
    val displayedDateIso: String? = null
)

/**
 * Uni-B 模糊提取响应。
 * 必须由轻量模型严格输出，并由 SchedulerLinter 直接反序列化。
 */
@Serializable
data class UniBExtractionPayload(
    /** MUST BE one of: VAGUE_CREATE, NOT_VAGUE */
    val decision: String = "NOT_VAGUE",
    /** NOT_VAGUE 时用于说明退出原因；VAGUE_CREATE 时可为空。 */
    val reason: String? = null,
    /** VAGUE_CREATE 时必须提供；NOT_VAGUE 时必须为空。 */
    val task: UniBVagueTaskPayload? = null
)

/**
 * Uni-B 模糊任务载荷。
 */
@Serializable
data class UniBVagueTaskPayload(
    /** 标题必须为非空自然语言。 */
    val title: String,
    /** MUST BE yyyy-MM-dd。表示日桶锚点，不表示精确时刻。 */
    val anchorDateIso: String,
    /** 可选的时间线索，如“下午”“下班后”。 */
    val timeHint: String? = null,
    /** MUST BE one of: L1, L2, L3, FIRE_OFF */
    val urgency: String = "L3"
)
