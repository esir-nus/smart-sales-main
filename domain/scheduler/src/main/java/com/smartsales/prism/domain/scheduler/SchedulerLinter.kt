package com.smartsales.prism.domain.scheduler

import com.smartsales.prism.domain.time.TimeProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 调度器校验器入口。
 *
 * Wave 2A 将具体解析与兼容逻辑下沉到稳定支持文件，
 * 此文件只保留公共 seam 与委托关系。
 */
@Singleton
class SchedulerLinter @Inject constructor() {

    /** 兼容旧调用方式: SchedulerLinter(timeProvider) */
    @Suppress("unused")
    constructor(timeProvider: TimeProvider) : this() {
        this.legacyTimeProvider = timeProvider
    }

    private var legacyTimeProvider: TimeProvider? = null

    private val jsonInterpreter = createSchedulerLinterJsonInterpreter()
    private val parsingSupport = SchedulerLinterParsingSupport(jsonInterpreter)
    private val legacySupport = SchedulerLinterLegacySupport(jsonInterpreter) { legacyTimeProvider }

    fun parseFastTrackIntent(input: String): FastTrackResult {
        return parsingSupport.parseFastTrackIntent(input)
    }

    fun parseUniAExtraction(
        input: String,
        unifiedId: String,
        transcript: String? = null,
        nowIso: String? = null,
        timezone: String? = null,
        displayedDateIso: String? = null
    ): FastTrackResult {
        return parsingSupport.parseUniAExtraction(
            input = input,
            unifiedId = unifiedId,
            transcript = transcript,
            nowIso = nowIso,
            timezone = timezone,
            displayedDateIso = displayedDateIso
        )
    }

    fun parseUniBExtraction(input: String, unifiedId: String): FastTrackResult {
        return parsingSupport.parseUniBExtraction(input, unifiedId)
    }

    fun parseUniBExtraction(
        input: String,
        unifiedId: String,
        transcript: String?,
        nowIso: String?,
        timezone: String?,
        displayedDateIso: String?
    ): FastTrackResult {
        return parsingSupport.parseUniBExtraction(
            input = input,
            unifiedId = unifiedId,
            transcript = transcript,
            nowIso = nowIso,
            timezone = timezone,
            displayedDateIso = displayedDateIso
        )
    }

    fun parseUniMExtraction(input: String): UniMExtractionResult {
        return parsingSupport.parseUniMExtraction(input)
    }

    fun parseFollowUpRescheduleExtraction(
        input: String,
        transcript: String
    ): FollowUpRescheduleExtractionResult {
        return parsingSupport.parseFollowUpRescheduleExtraction(
            input = input,
            transcript = transcript
        )
    }

    fun parseGlobalRescheduleExtraction(input: String): GlobalRescheduleExtractionResult {
        return parsingSupport.parseGlobalRescheduleExtraction(input)
    }

    fun parseUniCExtraction(
        input: String,
        unifiedId: String,
        transcript: String? = null
    ): FastTrackResult {
        return parsingSupport.parseUniCExtraction(input, unifiedId, transcript)
    }

    /**
     * @deprecated 使用 parseFastTrackIntent() 代替
     */
    fun lint(llmOutput: String): LintResult {
        return legacySupport.lint(llmOutput)
    }
}
