package com.smartsales.prism.ui.sim

import android.util.Log
import com.smartsales.core.pipeline.RealGlobalRescheduleExtractionService
import com.smartsales.core.pipeline.SchedulerIntelligenceRouter
import com.smartsales.core.pipeline.SchedulerPathACreateInterpreter
import com.smartsales.core.pipeline.RealUniAExtractionService
import com.smartsales.core.pipeline.RealUniBExtractionService
import com.smartsales.core.pipeline.RealUniCExtractionService
import com.smartsales.core.pipeline.RealUniMExtractionService
import com.smartsales.core.telemetry.PipelineValve
import com.smartsales.prism.domain.memory.ScheduleBoard
import com.smartsales.prism.domain.scheduler.ActiveTaskResolveResult
import com.smartsales.prism.domain.scheduler.ActiveTaskRetrievalIndex
import com.smartsales.prism.domain.scheduler.GlobalRescheduleExtractionRequest
import com.smartsales.prism.domain.scheduler.GlobalRescheduleExtractionResult
import com.smartsales.prism.domain.scheduler.CreateTasksParams
import com.smartsales.prism.domain.scheduler.CreateVagueTaskParams
import com.smartsales.prism.domain.scheduler.ExactTimeCueResolver
import com.smartsales.prism.domain.scheduler.FastTrackResult
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.scheduler.ScheduledTaskRepository
import com.smartsales.prism.domain.scheduler.TaskDefinition
import com.smartsales.prism.domain.scheduler.UniAExtractionRequest
import com.smartsales.prism.domain.scheduler.UniBExtractionRequest
import com.smartsales.prism.domain.scheduler.UniCExtractionRequest
import com.smartsales.prism.domain.scheduler.UniMAnchorKind
import com.smartsales.prism.domain.scheduler.UniMExtractionRequest
import com.smartsales.prism.domain.scheduler.UniMExtractionResult
import com.smartsales.prism.domain.scheduler.UniMTaskFragment
import com.smartsales.prism.domain.scheduler.UniMTaskMode
import com.smartsales.prism.domain.scheduler.RelativeTimeResolver
import com.smartsales.prism.domain.time.TimeProvider
import com.smartsales.prism.ui.scheduler.SharedPathACreateInterpreter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.UUID

internal fun buildSimSchedulerTranscriptLog(
    transcript: String,
    source: String
): String {
    return "transcript_ingress source=$source length=${transcript.length} text=$transcript"
}

internal fun buildSimSchedulerRouterPreflightLog(
    transcript: String,
    displayedDateIso: String?,
    mightReschedule: Boolean,
    shortlistSize: Int
): String {
    return buildString {
        append("route_preflight ")
        append("length=")
        append(transcript.length)
        append(" displayedDateIso=")
        append(displayedDateIso ?: "null")
        append(" mightReschedule=")
        append(mightReschedule)
        append(" shortlistSize=")
        append(shortlistSize)
        append(" transcript=")
        append(transcript)
    }
}

internal fun buildSimSchedulerRouterDecisionLog(
    metadata: SchedulerIntelligenceRouter.RouteMetadata
): String {
    return buildString {
        append("route_decision ")
        append("intent=")
        append(metadata.intentKind)
        append(" shape=")
        append(metadata.taskShape)
        append(" owner=")
        append(metadata.owner)
        append(" terminal=")
        append(metadata.schedulerTerminalOnCommit)
        append(" reason=")
        append(metadata.reason ?: "none")
    }
}

internal fun buildSimSchedulerCreateResultLog(
    resultKind: String,
    telemetry: SchedulerPathACreateInterpreter.Telemetry,
    itemCount: Int,
    parseUnresolvedCount: Int,
    downgradedCount: Int
): String {
    return buildString {
        append("create_result ")
        append("kind=")
        append(resultKind)
        append(" routeStage=")
        append(telemetry.routeStage)
        append(" uniM=")
        append(telemetry.uniMAttemptOutcome)
        append(" itemCount=")
        append(itemCount)
        append(" parseUnresolved=")
        append(parseUnresolvedCount)
        append(" downgraded=")
        append(downgradedCount)
    }
}

internal fun buildSimSchedulerUiFailureLog(
    branch: String,
    metadata: SchedulerIntelligenceRouter.RouteMetadata,
    displayedMessage: String,
    createReason: String? = null,
    uniCReason: String? = null
): String {
    return buildString {
        append("ui_failure ")
        append("branch=")
        append(branch)
        append(" intent=")
        append(metadata.intentKind)
        append(" owner=")
        append(metadata.owner)
        if (createReason != null) {
            append(" createReason=")
            append(createReason)
        }
        if (uniCReason != null) {
            append(" uniCReason=")
            append(uniCReason)
        }
        append(" displayed=")
        append(displayedMessage)
    }
}

internal fun normalizeSimSchedulerDrawerFailureMessage(
    intentKind: SchedulerIntelligenceRouter.SchedulerIntentKind,
    rawMessage: String
): String {
    val trimmed = rawMessage.trim()
    if (trimmed.isBlank()) {
        return when (intentKind) {
            SchedulerIntelligenceRouter.SchedulerIntentKind.RESCHEDULE ->
                "改期目标解析失败，请稍后重试"
            SchedulerIntelligenceRouter.SchedulerIntentKind.DELETE_UNSUPPORTED ->
                "SIM 当前不支持语音删除，请在面板手动操作"
            SchedulerIntelligenceRouter.SchedulerIntentKind.CREATE,
            SchedulerIntelligenceRouter.SchedulerIntentKind.NONE ->
                "未能解析为可创建日程，请换一种更明确的说法"
        }
    }
    if (
        intentKind == SchedulerIntelligenceRouter.SchedulerIntentKind.RESCHEDULE ||
        intentKind == SchedulerIntelligenceRouter.SchedulerIntentKind.DELETE_UNSUPPORTED
    ) {
        return trimmed
    }

    val normalized = trimmed.lowercase()
    return if (
        normalized.contains("安排日程") ||
        normalized.contains("时间信息") ||
        normalized.contains("schedulable") ||
        normalized.contains("可安排的日程提醒") ||
        normalized.contains("排程承诺") ||
        normalized.contains("可执行的日程安排") ||
        normalized.contains("输入包含") ||
        normalized.contains("灵感提取") ||
        normalized.contains("inspiration") ||
        normalized.contains("not_inspiration") ||
        normalized.contains("not inspiration") ||
        normalized.contains("not_vague") ||
        normalized.contains("not vague") ||
        normalized.contains("not_exact") ||
        normalized.contains("not exact") ||
        normalized.startsWith("uni-") ||
        normalized.contains("extractor") ||
        normalized.contains("json")
    ) {
        "未能解析为可创建日程，请换一种更明确的说法"
    } else {
        trimmed
    }
}

internal class SimSchedulerIngressCoordinator(
    private val taskRepository: ScheduledTaskRepository,
    private val scheduleBoard: ScheduleBoard,
    private val activeTaskRetrievalIndex: ActiveTaskRetrievalIndex,
    private val globalRescheduleExtractionService: RealGlobalRescheduleExtractionService,
    private val uniMExtractionService: RealUniMExtractionService,
    private val uniAExtractionService: RealUniAExtractionService,
    private val uniBExtractionService: RealUniBExtractionService,
    private val uniCExtractionService: RealUniCExtractionService,
    private val timeProvider: TimeProvider,
    private val projectionSupport: SimSchedulerProjectionSupport,
    private val mutationCoordinator: SimSchedulerMutationCoordinator
) {

    private val createInterpreter = SharedPathACreateInterpreter(
        uniMExtractionService = uniMExtractionService,
        uniAExtractionService = uniAExtractionService,
        uniBExtractionService = uniBExtractionService,
        timeProvider = timeProvider
    )

    private val schedulerRouter = SchedulerIntelligenceRouter(
        timeProvider = timeProvider,
        createInterpreter = createInterpreter,
        globalRescheduleExtractionService = globalRescheduleExtractionService
    )

    private enum class SingleTaskTelemetryAnchor {
        NOW_OFFSET,
        NOW_DAY_OFFSET
    }

    private data class FragmentAnchorState(
        val exactStart: Instant? = null,
        val anchorDate: LocalDate? = null
    )

    private data class DeterministicRelativeCreateCandidate(
        val title: String,
        val startTimeIso: String,
        val matchedText: String,
        val normalizedTranscript: String
    )

    private data class DeterministicWakeCreateCandidate(
        val title: String,
        val startTimeIso: String
    )

    private sealed interface ResolvedMultiTaskFragment {
        data class Resolved(
            val intent: FastTrackResult,
            val nextState: FragmentAnchorState,
            val downgraded: Boolean = false
        ) : ResolvedMultiTaskFragment

        data class Unresolved(val reason: String) : ResolvedMultiTaskFragment
    }

    companion object {
        private const val FAILURE_TAG = "SimSchedulerIngress"
        private val NOW_DAY_OFFSET_REGEX = Regex(
            pattern = "(明天|后天|tomorrow|day after tomorrow)"
        )
        private val CLOCK_HINT_REGEX = Regex(
            pattern = "(上午|下午|中午|晚上|凌晨|早上|\\d{1,2}:\\d{2}|\\d{1,2}点半?|\\d{1,2}時|\\d{1,2}时)"
        )
    }

    suspend fun processTranscript(transcript: String) {
        if (transcript.isBlank()) {
            projectionSupport.emitFailure("未识别到有效日程内容")
            return
        }

        val displayedDateIso = projectionSupport.displayedDateIso()
        val mightReschedule = schedulerRouter.mightExpressReschedule(transcript)
        val shortlist = if (mightReschedule) {
            activeTaskRetrievalIndex.buildShortlist(transcript)
        } else {
            emptyList()
        }
        Log.d(
            FAILURE_TAG,
            buildSimSchedulerRouterPreflightLog(
                transcript = transcript,
                displayedDateIso = displayedDateIso,
                mightReschedule = mightReschedule,
                shortlistSize = shortlist.size
            )
        )

        val decision = schedulerRouter.routeGeneral(
            SchedulerIntelligenceRouter.GeneralContext(
                transcript = transcript,
                surface = SchedulerIntelligenceRouter.SchedulerSurface.SCHEDULER_DRAWER,
                displayedDateIso = displayedDateIso,
                activeTaskShortlist = shortlist
            )
        )
        Log.d(FAILURE_TAG, buildSimSchedulerRouterDecisionLog(decision.metadata))

        when (decision) {
            is SchedulerIntelligenceRouter.Decision.Create -> {
                when (val result = decision.result) {
                    is SchedulerPathACreateInterpreter.Result.SingleMatched -> {
                        Log.d(
                            FAILURE_TAG,
                            buildSimSchedulerCreateResultLog(
                                resultKind = result.intent::class.simpleName ?: "unknown",
                                telemetry = result.telemetry,
                                itemCount = 1,
                                parseUnresolvedCount = result.telemetry.parseUnresolvedCount,
                                downgradedCount = result.telemetry.downgradedCount
                            )
                        )
                        emitSingleTaskExtractionTelemetry(transcript, result.intent)
                        mutationCoordinator.handleMutation(result.intent)
                    }
                    is SchedulerPathACreateInterpreter.Result.MultiMatched -> {
                        Log.d(
                            FAILURE_TAG,
                            buildSimSchedulerCreateResultLog(
                                resultKind = "MultiMatched",
                                telemetry = result.telemetry,
                                itemCount = result.intents.size,
                                parseUnresolvedCount = result.parseUnresolvedCount,
                                downgradedCount = result.downgradedCount
                            )
                        )
                        handleMultiTaskCreate(
                            batchId = result.telemetry.batchId ?: UUID.randomUUID().toString(),
                            intents = result.intents,
                            parseUnresolvedCount = result.parseUnresolvedCount,
                            downgradedCount = result.downgradedCount
                        )
                    }
                    else -> Unit
                }
            }

            is SchedulerIntelligenceRouter.Decision.GlobalReschedule -> {
                handleResolvedGlobalReschedule(decision.extracted)
            }

            is SchedulerIntelligenceRouter.Decision.FollowUpReschedule -> {
                projectionSupport.emitFailure("SIM 当前不支持从此入口直接进入选中任务改期")
            }

            is SchedulerIntelligenceRouter.Decision.Reject -> {
                surfaceNormalizedFailure(
                    branch = "reject",
                    metadata = decision.metadata,
                    rawMessage = decision.message
                )
            }

            is SchedulerIntelligenceRouter.Decision.NotMatched -> {
                Log.w(
                    FAILURE_TAG,
                    "create_not_matched reason=${decision.reason}"
                )
                val inspiration = uniCExtractionService.extract(
                    UniCExtractionRequest(
                        transcript = transcript,
                        nowIso = timeProvider.now.toString(),
                        timezone = timeProvider.zoneId.id,
                        unifiedId = UUID.randomUUID().toString()
                    )
                )
                if (inspiration !is FastTrackResult.NoMatch) {
                    mutationCoordinator.handleMutation(inspiration)
                    return
                }
                Log.w(
                    FAILURE_TAG,
                    "uni_c_not_matched reason=${inspiration.reason}"
                )
                surfaceNormalizedFailure(
                    branch = "not_matched_uni_c",
                    metadata = decision.metadata,
                    rawMessage = inspiration.reason,
                    createReason = decision.reason,
                    uniCReason = inspiration.reason
                )
            }
        }
    }

    private suspend fun handleMultiTaskCreate(
        batchId: String,
        intents: List<FastTrackResult>,
        parseUnresolvedCount: Int,
        downgradedCount: Int
    ) {
        Log.d("SimSchedulerMulti", "batch=$batchId intents=${intents.size} parseUnresolved=$parseUnresolvedCount")

        val createdTasks = mutableListOf<ScheduledTask>()
        val unresolvedReasons = MutableList(parseUnresolvedCount) { "片段未创建" }

        intents.forEachIndexed { index, intent ->
            val execution = mutationCoordinator.executeCreateIntent(intent)
            if (execution.createdTasks.isNotEmpty()) {
                createdTasks += execution.createdTasks
            } else {
                unresolvedReasons += execution.unresolvedReasons.ifEmpty {
                    listOf("片段${index + 1}未创建")
                }
            }
        }

        if (createdTasks.isEmpty()) {
            projectionSupport.emitFailure(unresolvedReasons.firstOrNull() ?: "未解析到可创建的多任务片段")
            return
        }

        projectionSupport.applyAggregatedConflictState(createdTasks)
        val summary = projectionSupport.buildMultiTaskStatus(
            createdCount = createdTasks.size,
            unresolvedCount = unresolvedReasons.size,
            downgradedCount = downgradedCount
        )
        Log.d(
            "SimSchedulerMulti",
            "batch=$batchId created=${createdTasks.size} unresolved=${unresolvedReasons.size} downgraded=$downgradedCount"
        )
        projectionSupport.emitStatus(summary, autoClear = false)
    }

    private fun emitSingleTaskExtractionTelemetry(transcript: String, result: FastTrackResult) {
        val create = result as? FastTrackResult.CreateTasks ?: return
        val task = create.params.tasks.singleOrNull() ?: return
        val anchor = classifySingleTaskTelemetryAnchor(transcript) ?: return
        val summary = when (anchor) {
            SingleTaskTelemetryAnchor.NOW_OFFSET -> "SIM scheduler single-task NOW_OFFSET extracted"
            SingleTaskTelemetryAnchor.NOW_DAY_OFFSET -> "SIM scheduler single-task NOW_DAY_OFFSET extracted"
        }
        val rawDump = buildString {
            append("title=")
            append(task.title)
            append(", startTimeIso=")
            append(task.startTimeIso)
            append(", transcript=")
            append(transcript)
        }
        PipelineValve.tag(
            checkpoint = PipelineValve.Checkpoint.TASK_EXTRACTED,
            payloadSize = 1,
            summary = summary,
            rawDataDump = rawDump
        )
        Log.d(
            "SimSchedulerSingle",
            "anchor=$anchor title=${task.title} start=${task.startTimeIso}"
        )
    }

    private fun classifySingleTaskTelemetryAnchor(transcript: String): SingleTaskTelemetryAnchor? {
        val normalized = transcript.lowercase().replace("：", ":")
        if (
            RelativeTimeResolver.resolveExact(
                userText = normalized,
                nowIso = timeProvider.now.toString(),
                timezone = timeProvider.zoneId.id
            ) != null
        ) {
            return SingleTaskTelemetryAnchor.NOW_OFFSET
        }
        if (NOW_DAY_OFFSET_REGEX.containsMatchIn(normalized) && CLOCK_HINT_REGEX.containsMatchIn(normalized)) {
            return SingleTaskTelemetryAnchor.NOW_DAY_OFFSET
        }
        return null
    }

    private fun buildDeterministicRelativeCreateCandidate(
        transcript: String,
        normalizedTranscript: String
    ): DeterministicRelativeCreateCandidate? {
        if (looksLikeMultiTaskCreateTranscript(transcript)) return null

        val resolution = RelativeTimeResolver.resolveExact(
            userText = transcript,
            nowIso = timeProvider.now.toString(),
            timezone = timeProvider.zoneId.id
        ) ?: return null

        val strippedTitle = stripDeterministicRelativeTimePhrase(
            transcript = normalizedTranscript,
            matchedText = resolution.matchedText
        ) ?: return null

        return DeterministicRelativeCreateCandidate(
            title = strippedTitle,
            startTimeIso = resolution.startTimeIso,
            matchedText = resolution.matchedText,
            normalizedTranscript = normalizedTranscript
        )
    }

    private fun buildDeterministicWakeCreateCandidate(transcript: String): DeterministicWakeCreateCandidate? {
        if (looksLikeMultiTaskCreateTranscript(transcript)) return null

        val startTimeIso = ExactTimeCueResolver.resolveExactDayClockStartTime(
            transcript = transcript,
            nowIso = timeProvider.now.toString(),
            timezone = timeProvider.zoneId.id,
            displayedDateIso = projectionSupport.displayedDateIso()
        ) ?: return null

        val strippedTitle = stripDeterministicDayClockPhrase(transcript)
            ?.takeIf(::looksLikeWakeReminderBody)
            ?: return null

        return DeterministicWakeCreateCandidate(
            title = strippedTitle,
            startTimeIso = startTimeIso
        )
    }

    private fun stripDeterministicRelativeTimePhrase(
        transcript: String,
        matchedText: String
    ): String? {
        val removed = transcript.replaceFirst(matchedText, "")
        val cleaned = removed
            .replace("提醒我", "")
            .replace("请提醒我", "")
            .replace("帮我", "")
            .replace("给我", "")
            .replace("请", "")
            .replace("记得", "")
            .replace("一下", "")
            .replace("去", "")
            .replace("  ", " ")
            .trim()
            .trim('，', ',', '。', '；', ';', '、', ' ')

        return cleaned.takeIf {
            it.isNotBlank() &&
                !it.contains("待会") &&
                !it.contains("过会") &&
                !it.contains("以后想") &&
                !it.contains("之后想")
        }
    }

    private fun stripDeterministicDayClockPhrase(transcript: String): String? {
        val removedDay = transcript
            .replace(Regex("(明天|后天|tomorrow|day after tomorrow)", RegexOption.IGNORE_CASE), "")
            .replace(
                Regex("(凌晨|早上|上午|中午|下午|晚上|今晚|午夜|半夜)?\\s*[零一二两三四五六七八九十百\\d]{1,3}点半?"),
                ""
            )
            .replace(Regex("\\b\\d{1,2}:\\d{2}\\b"), "")

        val cleaned = removedDay
            .replace("请提醒我", "")
            .replace("提醒我", "")
            .replace("请", "")
            .replace("一下", "")
            .replace("  ", " ")
            .trim()
            .trim('，', ',', '。', '；', ';', '、', ' ')

        return cleaned.takeIf { it.isNotBlank() }
    }

    private fun looksLikeWakeReminderTranscript(text: String): Boolean {
        val normalized = text.lowercase()
        return listOf("喊我起来", "叫我起来", "提醒我起床", "叫醒我", "起床").any(normalized::contains)
    }

    private fun looksLikeWakeReminderBody(text: String): Boolean {
        val normalized = text.lowercase()
        return listOf("喊我起来", "叫我起来", "起床", "叫醒我", "起来").any(normalized::contains)
    }

    private fun sanitizeTerminalCreateFailure(reason: String): String {
        return normalizeSimSchedulerDrawerFailureMessage(
            intentKind = SchedulerIntelligenceRouter.SchedulerIntentKind.NONE,
            rawMessage = reason
        )
    }

    private fun looksLikeMultiTaskCreateTranscript(text: String): Boolean {
        val normalized = text.lowercase()
        return listOf("，", ",", "、", "然后", "再", "以及").any(normalized::contains)
    }

    private fun resolveMultiTaskFragment(
        fragment: UniMTaskFragment,
        previous: FragmentAnchorState
    ): ResolvedMultiTaskFragment {
        return when (fragment.anchorKind) {
            UniMAnchorKind.ABSOLUTE -> resolveAbsoluteFragment(fragment)
            UniMAnchorKind.NOW_OFFSET -> resolveNowOffsetFragment(fragment)
            UniMAnchorKind.NOW_DAY_OFFSET -> resolveNowDayOffsetFragment(fragment)
            UniMAnchorKind.PREVIOUS_EXACT_OFFSET -> resolvePreviousExactOffsetFragment(fragment, previous)
            UniMAnchorKind.PREVIOUS_DAY_OFFSET -> resolvePreviousDayOffsetFragment(fragment, previous)
        }
    }

    private fun resolveAbsoluteFragment(fragment: UniMTaskFragment): ResolvedMultiTaskFragment {
        return when (fragment.mode) {
            UniMTaskMode.EXACT -> {
                val startTimeIso = fragment.startTimeIso
                    ?: return ResolvedMultiTaskFragment.Unresolved("缺少绝对精确时间")
                val start = parseExactInstant(startTimeIso)
                    ?: return ResolvedMultiTaskFragment.Unresolved("绝对精确时间格式无法解析")
                ResolvedMultiTaskFragment.Resolved(
                    intent = buildExactCreateResult(
                        title = fragment.title,
                        startTimeIso = startTimeIso,
                        durationMinutes = fragment.durationMinutes,
                        urgency = fragment.urgency
                    ),
                    nextState = FragmentAnchorState(
                        exactStart = start,
                        anchorDate = LocalDate.ofInstant(start, timeProvider.zoneId)
                    )
                )
            }
            UniMTaskMode.VAGUE -> {
                val anchorDateIso = fragment.anchorDateIso
                    ?: return ResolvedMultiTaskFragment.Unresolved("缺少绝对日期锚点")
                val anchorDate = runCatching { LocalDate.parse(anchorDateIso) }.getOrNull()
                    ?: return ResolvedMultiTaskFragment.Unresolved("绝对日期锚点格式非法")
                ResolvedMultiTaskFragment.Resolved(
                    intent = buildVagueCreateResult(
                        title = fragment.title,
                        anchorDateIso = anchorDateIso,
                        timeHint = fragment.timeHint,
                        urgency = fragment.urgency
                    ),
                    nextState = FragmentAnchorState(
                        exactStart = null,
                        anchorDate = anchorDate
                    )
                )
            }
        }
    }

    private fun resolvePreviousExactOffsetFragment(
        fragment: UniMTaskFragment,
        previous: FragmentAnchorState
    ): ResolvedMultiTaskFragment {
        val offsetMinutes = fragment.relativeOffsetMinutes
            ?: return ResolvedMultiTaskFragment.Unresolved("缺少相对分钟偏移")

        previous.exactStart?.let { previousExact ->
            val targetStart = previousExact.plusSeconds(offsetMinutes.toLong() * 60)
            val startTimeIso = targetStart.atZone(timeProvider.zoneId).toOffsetDateTime().toString()
            return when (fragment.mode) {
                UniMTaskMode.EXACT -> ResolvedMultiTaskFragment.Resolved(
                    intent = buildExactCreateResult(
                        title = fragment.title,
                        startTimeIso = startTimeIso,
                        durationMinutes = fragment.durationMinutes,
                        urgency = fragment.urgency
                    ),
                    nextState = FragmentAnchorState(
                        exactStart = targetStart,
                        anchorDate = LocalDate.ofInstant(targetStart, timeProvider.zoneId)
                    )
                )
                UniMTaskMode.VAGUE -> {
                    val anchorDate = LocalDate.ofInstant(targetStart, timeProvider.zoneId)
                    ResolvedMultiTaskFragment.Resolved(
                        intent = buildVagueCreateResult(
                            title = fragment.title,
                            anchorDateIso = anchorDate.toString(),
                            timeHint = fragment.timeHint,
                            urgency = fragment.urgency
                        ),
                        nextState = FragmentAnchorState(
                            exactStart = null,
                            anchorDate = anchorDate
                        )
                    )
                }
            }
        }

        previous.anchorDate?.let { anchorDate ->
            return ResolvedMultiTaskFragment.Resolved(
                intent = buildVagueCreateResult(
                    title = fragment.title,
                    anchorDateIso = anchorDate.toString(),
                    timeHint = fragment.timeHint,
                    urgency = fragment.urgency
                ),
                nextState = FragmentAnchorState(
                    exactStart = null,
                    anchorDate = anchorDate
                ),
                downgraded = true
            )
        }

        return ResolvedMultiTaskFragment.Unresolved("缺少可用的前序时间锚点")
    }

    private fun resolveNowOffsetFragment(fragment: UniMTaskFragment): ResolvedMultiTaskFragment {
        val offsetMinutes = fragment.relativeOffsetMinutes
            ?: return ResolvedMultiTaskFragment.Unresolved("缺少当前时刻相对分钟偏移")
        val targetStart = timeProvider.now.plusSeconds(offsetMinutes.toLong() * 60)
        val startTimeIso = targetStart.atZone(timeProvider.zoneId).toOffsetDateTime().toString()

        return when (fragment.mode) {
            UniMTaskMode.EXACT -> ResolvedMultiTaskFragment.Resolved(
                intent = buildExactCreateResult(
                    title = fragment.title,
                    startTimeIso = startTimeIso,
                    durationMinutes = fragment.durationMinutes,
                    urgency = fragment.urgency
                ),
                nextState = FragmentAnchorState(
                    exactStart = targetStart,
                    anchorDate = LocalDate.ofInstant(targetStart, timeProvider.zoneId)
                )
            )
            UniMTaskMode.VAGUE -> {
                val anchorDate = LocalDate.ofInstant(targetStart, timeProvider.zoneId)
                ResolvedMultiTaskFragment.Resolved(
                    intent = buildVagueCreateResult(
                        title = fragment.title,
                        anchorDateIso = anchorDate.toString(),
                        timeHint = fragment.timeHint,
                        urgency = fragment.urgency
                    ),
                    nextState = FragmentAnchorState(
                        exactStart = null,
                        anchorDate = anchorDate
                    )
                )
            }
        }
    }

    private fun resolveNowDayOffsetFragment(fragment: UniMTaskFragment): ResolvedMultiTaskFragment {
        val relativeDayOffset = fragment.relativeDayOffset
            ?: return ResolvedMultiTaskFragment.Unresolved("缺少当前日期相对偏移")
        val targetDate = timeProvider.today.plusDays(relativeDayOffset.toLong())

        return when (fragment.mode) {
            UniMTaskMode.EXACT -> {
                val clockTime = fragment.clockTime
                    ?: return ResolvedMultiTaskFragment.Unresolved("缺少当前日期链式精确时钟")
                val localTime = runCatching { LocalTime.parse(clockTime) }.getOrNull()
                    ?: return ResolvedMultiTaskFragment.Unresolved("当前日期链式精确时钟格式非法")
                val startTimeIso = targetDate.atTime(localTime).atZone(timeProvider.zoneId).toOffsetDateTime().toString()
                val start = parseExactInstant(startTimeIso)
                    ?: return ResolvedMultiTaskFragment.Unresolved("当前日期链式精确时间无法解析")
                ResolvedMultiTaskFragment.Resolved(
                    intent = buildExactCreateResult(
                        title = fragment.title,
                        startTimeIso = startTimeIso,
                        durationMinutes = fragment.durationMinutes,
                        keyPerson = fragment.keyPerson,
                        location = fragment.location,
                        urgency = fragment.urgency
                    ),
                    nextState = FragmentAnchorState(
                        exactStart = start,
                        anchorDate = targetDate
                    )
                )
            }
            UniMTaskMode.VAGUE -> ResolvedMultiTaskFragment.Resolved(
                intent = buildVagueCreateResult(
                    title = fragment.title,
                    anchorDateIso = targetDate.toString(),
                    timeHint = fragment.timeHint,
                    keyPerson = fragment.keyPerson,
                    location = fragment.location,
                    urgency = fragment.urgency
                ),
                nextState = FragmentAnchorState(
                    exactStart = null,
                    anchorDate = targetDate
                )
            )
        }
    }

    private fun resolvePreviousDayOffsetFragment(
        fragment: UniMTaskFragment,
        previous: FragmentAnchorState
    ): ResolvedMultiTaskFragment {
        val previousDate = previous.anchorDate
            ?: return ResolvedMultiTaskFragment.Unresolved("缺少可用的前序日期锚点")
        val relativeDayOffset = fragment.relativeDayOffset
            ?: return ResolvedMultiTaskFragment.Unresolved("缺少相对日期偏移")
        val targetDate = previousDate.plusDays(relativeDayOffset.toLong())

        return when (fragment.mode) {
            UniMTaskMode.EXACT -> {
                val clockTime = fragment.clockTime
                    ?: return ResolvedMultiTaskFragment.Unresolved("缺少链式精确时钟")
                val localTime = runCatching { LocalTime.parse(clockTime) }.getOrNull()
                    ?: return ResolvedMultiTaskFragment.Unresolved("链式精确时钟格式非法")
                val startTimeIso = targetDate.atTime(localTime).atZone(timeProvider.zoneId).toOffsetDateTime().toString()
                val start = parseExactInstant(startTimeIso)
                    ?: return ResolvedMultiTaskFragment.Unresolved("链式精确时间无法解析")
                ResolvedMultiTaskFragment.Resolved(
                    intent = buildExactCreateResult(
                        title = fragment.title,
                        startTimeIso = startTimeIso,
                        durationMinutes = fragment.durationMinutes,
                        keyPerson = fragment.keyPerson,
                        location = fragment.location,
                        urgency = fragment.urgency
                    ),
                    nextState = FragmentAnchorState(
                        exactStart = start,
                        anchorDate = targetDate
                    )
                )
            }
            UniMTaskMode.VAGUE -> ResolvedMultiTaskFragment.Resolved(
                intent = buildVagueCreateResult(
                    title = fragment.title,
                    anchorDateIso = targetDate.toString(),
                    timeHint = fragment.timeHint,
                    keyPerson = fragment.keyPerson,
                    location = fragment.location,
                    urgency = fragment.urgency
                ),
                nextState = FragmentAnchorState(
                    exactStart = null,
                    anchorDate = targetDate
                )
            )
        }
    }

    private fun buildExactCreateResult(
        title: String,
        startTimeIso: String,
        durationMinutes: Int,
        keyPerson: String? = null,
        location: String? = null,
        urgency: com.smartsales.prism.domain.scheduler.UrgencyEnum
    ): FastTrackResult {
        return FastTrackResult.CreateTasks(
            params = CreateTasksParams(
                unifiedId = UUID.randomUUID().toString(),
                tasks = listOf(
                    TaskDefinition(
                        title = title,
                        startTimeIso = startTimeIso,
                        durationMinutes = durationMinutes,
                        keyPerson = keyPerson,
                        location = location,
                        urgency = urgency
                    )
                )
            )
        )
    }

    private fun buildVagueCreateResult(
        title: String,
        anchorDateIso: String,
        timeHint: String?,
        keyPerson: String? = null,
        location: String? = null,
        urgency: com.smartsales.prism.domain.scheduler.UrgencyEnum
    ): FastTrackResult {
        return FastTrackResult.CreateVagueTask(
            params = CreateVagueTaskParams(
                unifiedId = UUID.randomUUID().toString(),
                title = title,
                anchorDateIso = anchorDateIso,
                timeHint = timeHint,
                keyPerson = keyPerson,
                location = location,
                urgency = urgency
            )
        )
    }

    private suspend fun handleResolvedGlobalReschedule(
        supported: GlobalRescheduleExtractionResult.Supported
    ) {
        supported.newTitle?.let { newTitle ->
            when (
                val resolution = activeTaskRetrievalIndex.resolveTargetByClockAnchor(
                    clockCue = supported.timeInstruction,
                    nowIso = timeProvider.now.toString(),
                    timezone = timeProvider.zoneId.id,
                    displayedDateIso = projectionSupport.displayedDateIso()
                )
            ) {
                is ActiveTaskResolveResult.Resolved -> {
                    val original = taskRepository.getTask(resolution.taskId)
                        ?: return projectionSupport.emitFailure("找不到要改名的日程")
                    PipelineValve.tag(
                        PipelineValve.Checkpoint.UI_STATE_EMITTED,
                        1,
                        SIM_SCHEDULER_GLOBAL_TIME_ANCHOR_RESOLVED_SUMMARY,
                        "taskId=${original.id} clockCue=${supported.timeInstruction}"
                    )
                    mutationCoordinator.executeResolvedReschedule(
                        original = original,
                        timeInstruction = supported.timeInstruction,
                        newTitle = newTitle
                    )
                }

                is ActiveTaskResolveResult.Ambiguous -> {
                    projectionSupport.emitFailure("该时间存在多个日程，无法确定改名目标")
                }

                is ActiveTaskResolveResult.NoMatch -> {
                    projectionSupport.emitFailure("未找到该时间的日程，无法改名")
                }
            }
            return
        }

        val shortlist = activeTaskRetrievalIndex.buildShortlist(supported.target.targetQuery)
        PipelineValve.tag(
            PipelineValve.Checkpoint.UI_STATE_EMITTED,
            shortlist.size,
            SIM_SCHEDULER_GLOBAL_SHORTLIST_BUILT_SUMMARY,
            "shortlistSize=${shortlist.size}"
        )

        PipelineValve.tag(
            PipelineValve.Checkpoint.UI_STATE_EMITTED,
            supported.suggestedTaskId.hashCode(),
            SIM_SCHEDULER_GLOBAL_SUGGESTION_RECEIVED_SUMMARY,
            "suggestedTaskId=${supported.suggestedTaskId ?: "null"}"
        )

        when (
            val resolution = activeTaskRetrievalIndex.resolveTarget(
                target = supported.target,
                suggestedTaskId = supported.suggestedTaskId
            )
        ) {
            is ActiveTaskResolveResult.Resolved -> {
                val original = taskRepository.getTask(resolution.taskId)
                    ?: return projectionSupport.emitFailure("找不到要改期的日程")
                mutationCoordinator.executeResolvedReschedule(original, supported.timeInstruction)
            }

            is ActiveTaskResolveResult.Ambiguous -> {
                projectionSupport.emitFailure("目标不明确，未执行改动")
            }

            is ActiveTaskResolveResult.NoMatch -> {
                projectionSupport.emitFailure("未找到匹配的日程，请更具体一些。")
            }
        }
    }

    private fun surfaceNormalizedFailure(
        branch: String,
        metadata: SchedulerIntelligenceRouter.RouteMetadata,
        rawMessage: String,
        createReason: String? = null,
        uniCReason: String? = null
    ) {
        val normalizedMessage = normalizeSimSchedulerDrawerFailureMessage(
            intentKind = metadata.intentKind,
            rawMessage = rawMessage
        )
        Log.w(
            FAILURE_TAG,
            buildSimSchedulerUiFailureLog(
                branch = branch,
                metadata = metadata,
                displayedMessage = normalizedMessage,
                createReason = createReason,
                uniCReason = uniCReason ?: rawMessage.takeIf { branch == "not_matched_uni_c" }
            )
        )
        projectionSupport.emitFailure(normalizedMessage)
    }

    // 处理语音改期转录文本的入口桩，后续由全局改期流程承接实际执行。
    private suspend fun handleVoiceRescheduleTranscript(transcript: String) {
        processTranscript(transcript)
    }

    private fun parseExactInstant(raw: String): Instant? {
        return runCatching { Instant.parse(raw) }
            .recoverCatching { OffsetDateTime.parse(raw).toInstant() }
            .getOrNull()
    }
}
