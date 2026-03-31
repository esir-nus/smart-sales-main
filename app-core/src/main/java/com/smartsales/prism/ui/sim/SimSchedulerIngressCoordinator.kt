package com.smartsales.prism.ui.sim

import android.util.Log
import com.smartsales.core.pipeline.RealGlobalRescheduleExtractionService
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
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.UUID

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
        private val NOW_DAY_OFFSET_REGEX = Regex(
            pattern = "(明天|后天|tomorrow|day after tomorrow)"
        )
        private val CLOCK_HINT_REGEX = Regex(
            pattern = "(上午|下午|中午|晚上|凌晨|早上|\\d{1,2}:\\d{2}|\\d{1,2}点半?|\\d{1,2}時|\\d{1,2}时)"
        )
        private val RESCHEDULE_KEYWORDS = listOf(
            "改期到",
            "改到",
            "改成",
            "改期",
            "挪到",
            "推迟到",
            "提前到",
            "往后推",
            "往前提",
            "推迟",
            "推后",
            "延后到",
            "延期到",
            "延后",
            "延期",
            "提前",
            "提早",
            "reschedule to",
            "reschedule",
            "move to",
            "move "
        )
    }

    suspend fun processTranscript(transcript: String) {
        if (transcript.isBlank()) {
            projectionSupport.emitFailure("未识别到有效日程内容")
            return
        }
        if (looksLikeDeletionTranscript(transcript)) {
            projectionSupport.emitFailure("SIM 当前不支持语音删除，请在面板手动操作")
            return
        }
        if (looksLikeRescheduleTranscript(transcript)) {
            handleVoiceRescheduleTranscript(transcript)
            return
        }

        val normalizedTranscript = RelativeTimeResolver.normalizeExplicitRelativeTimeTranscript(transcript)
        val normalizedOverride = normalizedTranscript.takeIf { it != transcript }

        buildDeterministicRelativeCreateCandidate(
            transcript = transcript,
            normalizedTranscript = normalizedTranscript
        )?.let { candidate ->
            Log.d(
                "SimSchedulerRelative",
                "single deterministic relative create matched=${candidate.matchedText} title=${candidate.title} start=${candidate.startTimeIso} transcript=$transcript normalized=${candidate.normalizedTranscript}"
            )
            val result = FastTrackResult.CreateTasks(
                params = CreateTasksParams(
                    unifiedId = UUID.randomUUID().toString(),
                    tasks = listOf(
                        TaskDefinition(
                            title = candidate.title,
                            startTimeIso = candidate.startTimeIso,
                            durationMinutes = 0,
                            urgency = com.smartsales.prism.domain.scheduler.UrgencyEnum.L3_NORMAL
                        )
                    )
                )
            )
            emitSingleTaskExtractionTelemetry(transcript, result)
            mutationCoordinator.handleMutation(result)
            return
        }
        if (
            RelativeTimeResolver.resolveExact(
                userText = transcript,
                nowIso = timeProvider.now.toString(),
                timezone = timeProvider.zoneId.id
            ) != null && !looksLikeMultiTaskCreateTranscript(transcript)
        ) {
            Log.w(
                "SimSchedulerRelative",
                "single deterministic relative create rejected transcript=$transcript normalized=$normalizedTranscript"
            )
            projectionSupport.emitFailure("已识别为相对时间日程，但任务内容不完整")
            return
        }

        buildDeterministicWakeCreateCandidate(transcript)?.let { candidate ->
            val result = FastTrackResult.CreateTasks(
                params = CreateTasksParams(
                    unifiedId = UUID.randomUUID().toString(),
                    tasks = listOf(
                        TaskDefinition(
                            title = candidate.title,
                            startTimeIso = candidate.startTimeIso,
                            durationMinutes = 0,
                            urgency = com.smartsales.prism.domain.scheduler.UrgencyEnum.FIRE_OFF
                        )
                    )
                )
            )
            emitSingleTaskExtractionTelemetry(transcript, result)
            mutationCoordinator.handleMutation(result)
            return
        }
        if (
            looksLikeWakeReminderTranscript(transcript) &&
            ExactTimeCueResolver.resolveExactDayClockStartTime(
                transcript = transcript,
                nowIso = timeProvider.now.toString(),
                timezone = timeProvider.zoneId.id,
                displayedDateIso = projectionSupport.displayedDateIso()
            ) != null &&
            !looksLikeMultiTaskCreateTranscript(transcript)
        ) {
            projectionSupport.emitFailure("已识别为明确时间日程，但任务内容不完整")
            return
        }

        val batchId = UUID.randomUUID().toString()
        when (val multi = uniMExtractionService.extract(
            UniMExtractionRequest(
                transcript = transcript,
                normalizedTranscript = normalizedOverride,
                nowIso = timeProvider.now.toString(),
                timezone = timeProvider.zoneId.id,
                batchId = batchId,
                displayedDateIso = projectionSupport.displayedDateIso()
            )
        )) {
            is UniMExtractionResult.MultiCreate -> {
                handleMultiTaskCreate(batchId, multi.fragments)
                return
            }
            is UniMExtractionResult.NotMulti -> Unit
        }

        val unifiedId = UUID.randomUUID().toString()
        val exact = uniAExtractionService.extract(
            UniAExtractionRequest(
                transcript = transcript,
                normalizedTranscript = normalizedOverride,
                nowIso = timeProvider.now.toString(),
                timezone = timeProvider.zoneId.id,
                unifiedId = unifiedId,
                displayedDateIso = projectionSupport.displayedDateIso()
            )
        )
        if (exact !is FastTrackResult.NoMatch) {
            emitSingleTaskExtractionTelemetry(transcript, exact)
            mutationCoordinator.handleMutation(exact)
            return
        }

        val vague = uniBExtractionService.extract(
            UniBExtractionRequest(
                transcript = transcript,
                normalizedTranscript = normalizedOverride,
                nowIso = timeProvider.now.toString(),
                timezone = timeProvider.zoneId.id,
                unifiedId = unifiedId,
                displayedDateIso = projectionSupport.displayedDateIso()
            )
        )
        if (vague !is FastTrackResult.NoMatch) {
            mutationCoordinator.handleMutation(vague)
            return
        }

        val inspiration = uniCExtractionService.extract(
            UniCExtractionRequest(
                transcript = transcript,
                nowIso = timeProvider.now.toString(),
                timezone = timeProvider.zoneId.id,
                unifiedId = unifiedId
            )
        )
        if (inspiration !is FastTrackResult.NoMatch) {
            mutationCoordinator.handleMutation(inspiration)
            return
        }

        projectionSupport.emitFailure(sanitizeTerminalCreateFailure(inspiration.reason))
    }

    private suspend fun handleMultiTaskCreate(batchId: String, fragments: List<UniMTaskFragment>) {
        Log.d("SimSchedulerMulti", "batch=$batchId fragments=${fragments.size}")

        var anchorState = FragmentAnchorState()
        val createdTasks = mutableListOf<ScheduledTask>()
        val unresolvedReasons = mutableListOf<String>()
        var downgradedCount = 0

        fragments.forEachIndexed { index, fragment ->
            when (val resolved = resolveMultiTaskFragment(fragment, anchorState)) {
                is ResolvedMultiTaskFragment.Resolved -> {
                    val execution = mutationCoordinator.executeCreateIntent(resolved.intent)
                    if (execution.createdTasks.isNotEmpty()) {
                        createdTasks += execution.createdTasks
                        anchorState = resolved.nextState
                        if (resolved.downgraded) downgradedCount += 1
                    } else {
                        unresolvedReasons += execution.unresolvedReasons.ifEmpty {
                            listOf("片段${index + 1}未创建")
                        }
                    }
                }
                is ResolvedMultiTaskFragment.Unresolved -> {
                    unresolvedReasons += "片段${index + 1}未创建：${resolved.reason}"
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
        val normalized = reason.lowercase()
        return if (
            normalized.contains("安排日程") ||
            normalized.contains("时间信息") ||
            normalized.contains("schedulable") ||
            normalized.startsWith("uni-") ||
            normalized.contains("json") ||
            normalized.contains("not_inspiration")
        ) {
            "未能解析为可创建日程，请换一种更明确的说法"
        } else {
            reason
        }
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

    private suspend fun handleVoiceRescheduleTranscript(transcript: String) {
        val shortlist = activeTaskRetrievalIndex.buildShortlist(transcript)
        PipelineValve.tag(
            PipelineValve.Checkpoint.UI_STATE_EMITTED,
            shortlist.size,
            SIM_SCHEDULER_GLOBAL_SHORTLIST_BUILT_SUMMARY,
            "shortlistSize=${shortlist.size}"
        )
        val extracted = globalRescheduleExtractionService.extract(
            GlobalRescheduleExtractionRequest(
                transcript = transcript,
                nowIso = timeProvider.now.toString(),
                timezone = timeProvider.zoneId.id,
                activeTaskShortlist = shortlist
            )
        )
        val supported = when (extracted) {
            is GlobalRescheduleExtractionResult.Supported -> extracted
            is GlobalRescheduleExtractionResult.Unsupported -> {
                projectionSupport.emitFailure("SIM 当前仅支持明确目标 + 明确时间改期")
                return
            }
            is GlobalRescheduleExtractionResult.Invalid -> {
                projectionSupport.emitFailure("改期目标或时间无法解析，请换一种明确说法")
                return
            }
            is GlobalRescheduleExtractionResult.Failure -> {
                projectionSupport.emitFailure("改期目标解析失败，请稍后重试")
                return
            }
        }

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

    private fun findRescheduleKeywordRange(text: String): IntRange? {
        val normalized = text.lowercase()
        return RESCHEDULE_KEYWORDS
            .mapNotNull { keyword ->
                normalized.indexOf(keyword)
                    .takeIf { it >= 0 }
                    ?.let { it until (it + keyword.length) }
            }
            .minByOrNull { it.first }
    }

    private fun looksLikeRescheduleTranscript(text: String): Boolean {
        val normalized = text.lowercase()
        return RESCHEDULE_KEYWORDS.any { normalized.contains(it) } || normalized.contains("actually")
    }

    private fun looksLikeDeletionTranscript(text: String): Boolean {
        val normalized = text.lowercase()
        return listOf("删除", "取消", "删掉", "delete", "cancel")
            .any { normalized.contains(it) }
    }

    private fun parseExactInstant(raw: String): Instant? {
        return runCatching { Instant.parse(raw) }
            .recoverCatching { OffsetDateTime.parse(raw).toInstant() }
            .getOrNull()
    }
}
