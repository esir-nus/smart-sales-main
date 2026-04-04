package com.smartsales.core.pipeline

import com.smartsales.prism.domain.scheduler.CreateTasksParams
import com.smartsales.prism.domain.scheduler.CreateVagueTaskParams
import com.smartsales.prism.domain.scheduler.ExactTimeCueResolver
import com.smartsales.prism.domain.scheduler.FastTrackResult
import com.smartsales.prism.domain.scheduler.RelativeTimeResolver
import com.smartsales.prism.domain.scheduler.TaskDefinition
import com.smartsales.prism.domain.scheduler.UniAExtractionRequest
import com.smartsales.prism.domain.scheduler.UniBExtractionRequest
import com.smartsales.prism.domain.scheduler.UniMAnchorKind
import com.smartsales.prism.domain.scheduler.UniMExtractionRequest
import com.smartsales.prism.domain.scheduler.UniMExtractionResult
import com.smartsales.prism.domain.scheduler.UniMTaskFragment
import com.smartsales.prism.domain.scheduler.UniMTaskMode
import com.smartsales.prism.domain.scheduler.UrgencyEnum
import com.smartsales.prism.domain.time.TimeProvider
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

/**
 * 共享 Path A 创建解释器。
 *
 * 说明：统一 deterministic -> Uni-M -> Uni-A -> Uni-B 的创建级联，
 * 供顶层语音、调度器抽屉和 onboarding sandbox 复用。
 */
class SchedulerPathACreateInterpreter(
    private val uniMExtractionService: RealUniMExtractionService,
    private val uniAExtractionService: RealUniAExtractionService,
    private val uniBExtractionService: RealUniBExtractionService,
    private val timeProvider: TimeProvider
) {

    data class Telemetry(
        val routeStage: RouteStage,
        val uniMAttemptOutcome: UniMAttemptOutcome,
        val batchId: String? = null,
        val parseUnresolvedCount: Int = 0,
        val downgradedCount: Int = 0
    )

    enum class RouteStage {
        DETERMINISTIC_RELATIVE,
        DETERMINISTIC_DAY_CLOCK,
        DETERMINISTIC_CHAINED_DAY_CLOCK,
        UNI_M,
        UNI_A,
        UNI_B,
        NO_MATCH
    }

    enum class UniMAttemptOutcome {
        SKIPPED,
        MULTI_MATCHED,
        NOT_MULTI,
        TIMEOUT,
        EXCEPTION
    }

    sealed interface Result {
        data class SingleMatched(
            val intent: FastTrackResult,
            val telemetry: Telemetry
        ) : Result

        data class MultiMatched(
            val intents: List<FastTrackResult>,
            val parseUnresolvedCount: Int,
            val downgradedCount: Int,
            val telemetry: Telemetry
        ) : Result

        data class DirectFailure(
            val message: String,
            val telemetry: Telemetry
        ) : Result

        data class NotMatched(
            val reason: String,
            val telemetry: Telemetry
        ) : Result
    }

    private data class FragmentAnchorState(
        val exactStart: Instant? = null,
        val anchorDate: LocalDate? = null
    )

    private data class DeterministicCreateCandidate(
        val intent: FastTrackResult,
        val routeStage: RouteStage
    )

    private data class DeterministicMultiCreateCandidate(
        val intents: List<FastTrackResult>,
        val routeStage: RouteStage
    )

    private sealed interface ResolvedMultiTaskFragment {
        data class Resolved(
            val intent: FastTrackResult,
            val nextState: FragmentAnchorState,
            val downgraded: Boolean = false
        ) : ResolvedMultiTaskFragment

        data class Unresolved(val reason: String) : ResolvedMultiTaskFragment
    }

    private data class ResolvedMultiTaskBatch(
        val intents: List<FastTrackResult>,
        val unresolvedReasons: List<String>,
        val downgradedCount: Int
    )

    private data class DirectFailureCandidate(
        val message: String,
        val routeStage: RouteStage
    )

    suspend fun interpret(
        transcript: String,
        displayedDateIso: String?,
        uniMTimeoutMs: Long?
    ): Result {
        val normalizedTranscript = RelativeTimeResolver.normalizeExplicitRelativeTimeTranscript(transcript)
        val normalizedOverride = normalizedTranscript.takeIf { it != transcript }
        val batchLikeTranscript = splitDeterministicClockClauses(transcript).size > 1
        val pendingDirectFailure = if (batchLikeTranscript) {
            null
        } else {
            buildDirectFailureCandidate(
                transcript = transcript,
                normalizedTranscript = normalizedTranscript,
                displayedDateIso = displayedDateIso
            )
        }

        if (!batchLikeTranscript) {
            buildDeterministicRelativeCreateCandidate(
                transcript = transcript,
                normalizedTranscript = normalizedTranscript
            )?.let { candidate ->
                return Result.SingleMatched(
                    intent = candidate.intent,
                    telemetry = Telemetry(
                        routeStage = candidate.routeStage,
                        uniMAttemptOutcome = UniMAttemptOutcome.SKIPPED
                    )
                )
            }
        }

        buildDeterministicChainedDayClockCreateCandidate(
            transcript = transcript,
            displayedDateIso = displayedDateIso
        )?.let { candidate ->
            return Result.MultiMatched(
                intents = candidate.intents,
                parseUnresolvedCount = 0,
                downgradedCount = 0,
                telemetry = Telemetry(
                    routeStage = candidate.routeStage,
                    uniMAttemptOutcome = UniMAttemptOutcome.SKIPPED
                )
            )
        }

        if (!batchLikeTranscript) {
            buildDeterministicDayClockCreateCandidate(
                transcript = transcript,
                displayedDateIso = displayedDateIso
            )?.let { candidate ->
                return Result.SingleMatched(
                    intent = candidate.intent,
                    telemetry = Telemetry(
                        routeStage = candidate.routeStage,
                        uniMAttemptOutcome = UniMAttemptOutcome.SKIPPED
                    )
                )
            }
        }

        pendingDirectFailure?.let { failure ->
            return Result.DirectFailure(
                message = failure.message,
                telemetry = Telemetry(
                    routeStage = failure.routeStage,
                    uniMAttemptOutcome = UniMAttemptOutcome.SKIPPED
                )
            )
        }

        val batchId = UUID.randomUUID().toString()
        val uniMDecision = attemptUniM(
            transcript = transcript,
            normalizedOverride = normalizedOverride,
            displayedDateIso = displayedDateIso,
            batchId = batchId,
            timeoutMs = uniMTimeoutMs
        )
        when (val multi = uniMDecision.result) {
            is UniMExtractionResult.MultiCreate -> {
                val resolved = resolveMultiTaskCreate(multi.fragments)
                if (resolved.intents.isNotEmpty()) {
                    return Result.MultiMatched(
                        intents = resolved.intents,
                        parseUnresolvedCount = resolved.unresolvedReasons.size,
                        downgradedCount = resolved.downgradedCount,
                        telemetry = Telemetry(
                            routeStage = RouteStage.UNI_M,
                            uniMAttemptOutcome = uniMDecision.outcome,
                            batchId = batchId,
                            parseUnresolvedCount = resolved.unresolvedReasons.size,
                            downgradedCount = resolved.downgradedCount
                        )
                    )
                }
                return Result.DirectFailure(
                    message = resolved.unresolvedReasons.firstOrNull()
                        ?: "未解析到可创建的多任务片段",
                    telemetry = Telemetry(
                        routeStage = RouteStage.UNI_M,
                        uniMAttemptOutcome = uniMDecision.outcome,
                        batchId = batchId
                    )
                )
            }
            is UniMExtractionResult.NotMulti,
            null -> Unit
        }

        val unifiedId = UUID.randomUUID().toString()
        val exact = uniAExtractionService.extract(
            UniAExtractionRequest(
                transcript = transcript,
                normalizedTranscript = normalizedOverride,
                nowIso = timeProvider.now.toString(),
                timezone = timeProvider.zoneId.id,
                unifiedId = unifiedId,
                displayedDateIso = displayedDateIso
            )
        )
        if (exact !is FastTrackResult.NoMatch) {
            return Result.SingleMatched(
                intent = exact,
                telemetry = Telemetry(
                    routeStage = RouteStage.UNI_A,
                    uniMAttemptOutcome = uniMDecision.outcome,
                    batchId = batchId
                )
            )
        }

        val vague = uniBExtractionService.extract(
            UniBExtractionRequest(
                transcript = transcript,
                normalizedTranscript = normalizedOverride,
                nowIso = timeProvider.now.toString(),
                timezone = timeProvider.zoneId.id,
                unifiedId = unifiedId,
                displayedDateIso = displayedDateIso
            )
        )
        if (vague !is FastTrackResult.NoMatch) {
            return Result.SingleMatched(
                intent = vague,
                telemetry = Telemetry(
                    routeStage = RouteStage.UNI_B,
                    uniMAttemptOutcome = uniMDecision.outcome,
                    batchId = batchId
                )
            )
        }

        return Result.NotMatched(
            reason = vague.reason,
            telemetry = Telemetry(
                routeStage = RouteStage.NO_MATCH,
                uniMAttemptOutcome = uniMDecision.outcome,
                batchId = batchId
            )
        )
    }

    private data class UniMDecision(
        val result: UniMExtractionResult?,
        val outcome: UniMAttemptOutcome
    )

    private suspend fun attemptUniM(
        transcript: String,
        normalizedOverride: String?,
        displayedDateIso: String?,
        batchId: String,
        timeoutMs: Long?
    ): UniMDecision {
        val request = UniMExtractionRequest(
            transcript = transcript,
            normalizedTranscript = normalizedOverride,
            nowIso = timeProvider.now.toString(),
            timezone = timeProvider.zoneId.id,
            batchId = batchId,
            displayedDateIso = displayedDateIso
        )
        return try {
            val result = if (timeoutMs != null) {
                withTimeout(timeoutMs) {
                    uniMExtractionService.extract(request)
                }
            } else {
                uniMExtractionService.extract(request)
            }
            when (result) {
                is UniMExtractionResult.MultiCreate -> UniMDecision(result, UniMAttemptOutcome.MULTI_MATCHED)
                is UniMExtractionResult.NotMulti -> UniMDecision(result, UniMAttemptOutcome.NOT_MULTI)
            }
        } catch (_: TimeoutCancellationException) {
            UniMDecision(result = null, outcome = UniMAttemptOutcome.TIMEOUT)
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            UniMDecision(result = null, outcome = UniMAttemptOutcome.EXCEPTION)
        }
    }

    private fun buildDirectFailureCandidate(
        transcript: String,
        normalizedTranscript: String,
        displayedDateIso: String?
    ): DirectFailureCandidate? {
        val relativeExactResolution = RelativeTimeResolver.resolveExact(
            userText = transcript,
            nowIso = timeProvider.now.toString(),
            timezone = timeProvider.zoneId.id
        )
        if (
            relativeExactResolution != null &&
            stripDeterministicRelativeTimePhrase(
                transcript = normalizedTranscript,
                matchedText = relativeExactResolution.matchedText
            ) == null
        ) {
            return DirectFailureCandidate(
                message = "已识别为相对时间日程，但任务内容不完整",
                routeStage = RouteStage.DETERMINISTIC_RELATIVE
            )
        }

        val exactDayClockResolution = ExactTimeCueResolver.resolveExactDayClockStartTime(
            transcript = transcript,
            nowIso = timeProvider.now.toString(),
            timezone = timeProvider.zoneId.id,
            displayedDateIso = displayedDateIso
        )
        if (
            exactDayClockResolution != null &&
            stripDeterministicDayClockPhrase(transcript) == null
        ) {
            return DirectFailureCandidate(
                message = "已识别为明确时间日程，但任务内容不完整",
                routeStage = RouteStage.DETERMINISTIC_DAY_CLOCK
            )
        }
        return null
    }

    private fun buildDeterministicRelativeCreateCandidate(
        transcript: String,
        normalizedTranscript: String
    ): DeterministicCreateCandidate? {
        val resolution = RelativeTimeResolver.resolveExact(
            userText = transcript,
            nowIso = timeProvider.now.toString(),
            timezone = timeProvider.zoneId.id
        ) ?: return null
        val strippedTitle = stripDeterministicRelativeTimePhrase(
            transcript = normalizedTranscript,
            matchedText = resolution.matchedText
        ) ?: return null
        return DeterministicCreateCandidate(
            intent = buildExactCreateResult(
                title = strippedTitle,
                startTimeIso = resolution.startTimeIso,
                durationMinutes = 0,
                urgency = UrgencyEnum.L3_NORMAL
            ),
            routeStage = RouteStage.DETERMINISTIC_RELATIVE
        )
    }

    private fun buildDeterministicDayClockCreateCandidate(
        transcript: String,
        displayedDateIso: String?
    ): DeterministicCreateCandidate? {
        val startTimeIso = ExactTimeCueResolver.resolveExactDayClockStartTime(
            transcript = transcript,
            nowIso = timeProvider.now.toString(),
            timezone = timeProvider.zoneId.id,
            displayedDateIso = displayedDateIso
        ) ?: return null
        val strippedTitle = stripDeterministicDayClockPhrase(transcript)
            ?.takeIf(::looksLikeWakeReminderBody)
            ?: return null
        return DeterministicCreateCandidate(
            intent = buildExactCreateResult(
                title = strippedTitle,
                startTimeIso = startTimeIso,
                durationMinutes = 0,
                urgency = UrgencyEnum.FIRE_OFF
            ),
            routeStage = RouteStage.DETERMINISTIC_DAY_CLOCK
        )
    }

    private fun buildDeterministicChainedDayClockCreateCandidate(
        transcript: String,
        displayedDateIso: String?
    ): DeterministicMultiCreateCandidate? {
        val clauses = splitDeterministicClockClauses(transcript)
        if (clauses.size < 2) return null

        val firstStartTimeIso = ExactTimeCueResolver.resolveExactDayClockStartTime(
            transcript = clauses.first(),
            nowIso = timeProvider.now.toString(),
            timezone = timeProvider.zoneId.id,
            displayedDateIso = displayedDateIso
        ) ?: return null
        val anchorDate = OffsetDateTime.parse(firstStartTimeIso).toInstant()
            .atZone(timeProvider.zoneId)
            .toLocalDate()

        val intents = clauses.mapIndexed { index, clause ->
            val rawTitle = stripDeterministicDayClockPhrase(clause) ?: return null
            val title = sanitizeDeterministicClauseBody(rawTitle) ?: return null
            val startTimeIso = if (index == 0) {
                firstStartTimeIso
            } else {
                ExactTimeCueResolver.buildExactStartTimeFromExplicitCue(
                    transcript = clause,
                    anchorDateIso = anchorDate.toString(),
                    timeHint = null,
                    timezone = timeProvider.zoneId.id
                ) ?: return null
            }
            buildExactCreateResult(
                title = title,
                startTimeIso = startTimeIso,
                durationMinutes = 0,
                urgency = if (looksLikeWakeReminderBody(title)) {
                    UrgencyEnum.FIRE_OFF
                } else {
                    UrgencyEnum.L2_IMPORTANT
                }
            )
        }

        return DeterministicMultiCreateCandidate(
            intents = intents,
            routeStage = RouteStage.DETERMINISTIC_CHAINED_DAY_CLOCK
        )
    }

    private fun resolveMultiTaskCreate(fragments: List<UniMTaskFragment>): ResolvedMultiTaskBatch {
        var anchorState = FragmentAnchorState()
        val intents = mutableListOf<FastTrackResult>()
        val unresolvedReasons = mutableListOf<String>()
        var downgradedCount = 0

        fragments.forEachIndexed { index, fragment ->
            when (val resolved = resolveMultiTaskFragment(fragment, anchorState)) {
                is ResolvedMultiTaskFragment.Resolved -> {
                    intents += resolved.intent
                    anchorState = resolved.nextState
                    if (resolved.downgraded) {
                        downgradedCount += 1
                    }
                }
                is ResolvedMultiTaskFragment.Unresolved -> {
                    unresolvedReasons += "片段${index + 1}未创建：${resolved.reason}"
                }
            }
        }

        return ResolvedMultiTaskBatch(
            intents = intents,
            unresolvedReasons = unresolvedReasons,
            downgradedCount = downgradedCount
        )
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
                        keyPerson = fragment.keyPerson,
                        location = fragment.location,
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
                        keyPerson = fragment.keyPerson,
                        location = fragment.location,
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
                    keyPerson = fragment.keyPerson,
                    location = fragment.location,
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
                        keyPerson = fragment.keyPerson,
                        location = fragment.location,
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
                        keyPerson = fragment.keyPerson,
                        location = fragment.location,
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
                            keyPerson = fragment.keyPerson,
                            location = fragment.location,
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
                    keyPerson = fragment.keyPerson,
                    location = fragment.location,
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
            .replace(
                Regex(
                    "(今天|明天|后天|下周[一二三四五六日天]|下星期[一二三四五六日天]|下礼拜[一二三四五六日天]|tomorrow|day after tomorrow)",
                    RegexOption.IGNORE_CASE
                ),
                ""
            )
            .replace(
                Regex("(凌晨|早上|上午|中午|下午|晚上|今晚|午夜|半夜)?\\s*[零一二两三四五六七八九十百\\d]{1,3}点(?:半|钟)?"),
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

    private fun splitDeterministicClockClauses(transcript: String): List<String> {
        return transcript
            .split(Regex("\\s*(?:，|,|；|;|。|然后|接着)\\s*"))
            .map(String::trim)
            .filter(String::isNotBlank)
    }

    private fun sanitizeDeterministicClauseBody(raw: String): String? {
        val cleaned = raw
            .replace(Regex("^(要|再|然后|接着|并且|还要|需要)"), "")
            .trim()
        return cleaned.takeIf { it.isNotBlank() }
    }

    private fun looksLikeWakeReminderBody(text: String): Boolean {
        val normalized = text.lowercase()
        return listOf("喊我起来", "叫我起来", "起床", "叫醒我", "起来").any(normalized::contains)
    }

    private fun buildExactCreateResult(
        title: String,
        startTimeIso: String,
        durationMinutes: Int,
        keyPerson: String? = null,
        location: String? = null,
        urgency: UrgencyEnum
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
        urgency: UrgencyEnum
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

    private fun parseExactInstant(startTimeIso: String): Instant? {
        return runCatching { OffsetDateTime.parse(startTimeIso).toInstant() }.getOrNull()
    }
}
