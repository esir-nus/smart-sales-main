package com.smartsales.prism.domain.scheduler

import kotlinx.serialization.Serializable

/**
 * Uni-M 多任务拆解请求。
 * 说明：用于 Path A 多任务 create 预拆解，不负责持久化。
 */
data class UniMExtractionRequest(
    val transcript: String,
    val normalizedTranscript: String? = null,
    val nowIso: String,
    val timezone: String,
    val batchId: String,
    val displayedDateIso: String? = null
)

/**
 * Uni-M 多任务拆解响应。
 * 必须由轻量模型严格输出，并由 SchedulerLinter 直接反序列化。
 */
@Serializable
data class UniMExtractionPayload(
    /** MUST BE one of: MULTI_CREATE, NOT_MULTI */
    val decision: String = "NOT_MULTI",
    val reason: String? = null,
    val fragments: List<UniMTaskFragmentPayload> = emptyList()
)

/**
 * Uni-M 多任务片段。
 */
@Serializable
data class UniMTaskFragmentPayload(
    val title: String,
    /** MUST BE one of: EXACT, VAGUE */
    val mode: String = "EXACT",
    /** MUST BE one of: ABSOLUTE, NOW_OFFSET, NOW_DAY_OFFSET, PREVIOUS_EXACT_OFFSET, PREVIOUS_DAY_OFFSET */
    val anchorKind: String = "ABSOLUTE",
    /** EXACT + ABSOLUTE 时使用，必须是严格 ISO-8601。 */
    val startTimeIso: String? = null,
    /** VAGUE + ABSOLUTE 时使用，必须是 yyyy-MM-dd。 */
    val anchorDateIso: String? = null,
    /** EXACT + PREVIOUS_DAY_OFFSET 时使用，必须是 HH:mm。 */
    val clockTime: String? = null,
    /** VAGUE 或降级兜底时可带时间线索。 */
    val timeHint: String? = null,
    /** EXACT 片段建议时长；未给时默认 0。 */
    val durationMinutes: Int = 0,
    /** 可选的商务关键人物提示。 */
    val keyPerson: String? = null,
    /** 可选的地点提示。 */
    val location: String? = null,
    /** PREVIOUS_EXACT_OFFSET 时使用，单位分钟。 */
    val relativeOffsetMinutes: Int? = null,
    /** PREVIOUS_DAY_OFFSET 时使用，可为 0/1/2...。 */
    val relativeDayOffset: Int? = null,
    /** MUST BE one of: L1, L2, L3, FIRE_OFF */
    val urgency: String = "L3"
)

sealed interface UniMExtractionResult {
    data class MultiCreate(val fragments: List<UniMTaskFragment>) : UniMExtractionResult
    data class NotMulti(val reason: String) : UniMExtractionResult
}

data class UniMTaskFragment(
    val title: String,
    val mode: UniMTaskMode,
    val anchorKind: UniMAnchorKind,
    val urgency: UrgencyEnum,
    val startTimeIso: String? = null,
    val anchorDateIso: String? = null,
    val clockTime: String? = null,
    val timeHint: String? = null,
    val durationMinutes: Int = 0,
    val keyPerson: String? = null,
    val location: String? = null,
    val relativeOffsetMinutes: Int? = null,
    val relativeDayOffset: Int? = null
)

enum class UniMTaskMode {
    EXACT,
    VAGUE
}

enum class UniMAnchorKind {
    ABSOLUTE,
    NOW_OFFSET,
    NOW_DAY_OFFSET,
    PREVIOUS_EXACT_OFFSET,
    PREVIOUS_DAY_OFFSET
}
