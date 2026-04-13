package com.smartsales.prism.ui.onboarding

import android.os.SystemClock
import android.util.Log

import com.smartsales.core.pipeline.RealGlobalRescheduleExtractionService
import com.smartsales.core.pipeline.SchedulerIntelligenceRouter
import com.smartsales.core.pipeline.SchedulerPathACreateInterpreter
import com.smartsales.core.pipeline.RealUniAExtractionService
import com.smartsales.core.pipeline.RealUniBExtractionService
import com.smartsales.core.pipeline.RealUniMExtractionService
import com.smartsales.prism.data.scheduler.TaskRetrievalCandidate
import com.smartsales.prism.data.scheduler.TaskRetrievalScoring
import com.smartsales.prism.domain.memory.TargetResolutionRequest
import com.smartsales.prism.domain.scheduler.ActiveTaskContext
import com.smartsales.prism.domain.scheduler.CreateTasksParams
import com.smartsales.prism.domain.scheduler.CreateVagueTaskParams
import com.smartsales.prism.domain.scheduler.ExactTimeCueResolver
import com.smartsales.prism.domain.scheduler.FastTrackResult
import com.smartsales.prism.domain.scheduler.GlobalRescheduleExtractionRequest
import com.smartsales.prism.domain.scheduler.GlobalRescheduleExtractionResult
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
import com.smartsales.prism.domain.scheduler.UrgencyLevel
import com.smartsales.prism.domain.time.TimeProvider
import com.smartsales.prism.ui.scheduler.SharedPathACreateInterpreter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * onboarding quick start 真实语音日程体验服务。
 */
interface OnboardingQuickStartService {
    suspend fun applyTranscript(
        transcript: String,
        currentItems: List<OnboardingQuickStartItem>
    ): OnboardingQuickStartServiceResult
}

sealed interface OnboardingQuickStartServiceResult {
    data class Success(
        val items: List<OnboardingQuickStartItem>,
        val touchedExactTask: Boolean,
        val mutationKind: MutationKind
    ) : OnboardingQuickStartServiceResult {
        enum class MutationKind {
            CREATE,
            UPDATE
        }
    }

    data class Failure(val message: String) : OnboardingQuickStartServiceResult
}

/**
 * onboarding sandbox 目标解析器。
 */
interface OnboardingQuickStartSandboxResolver {
    fun buildShortlist(
        items: List<OnboardingQuickStartItem>,
        limit: Int = 8
    ): List<ActiveTaskContext>

    fun resolveTarget(
        items: List<OnboardingQuickStartItem>,
        target: TargetResolutionRequest,
        suggestedTaskId: String? = null
    ): OnboardingQuickStartSandboxTargetResult
}

sealed interface OnboardingQuickStartSandboxTargetResult {
    data class Resolved(val item: OnboardingQuickStartItem) : OnboardingQuickStartSandboxTargetResult
    data class Ambiguous(val query: String) : OnboardingQuickStartSandboxTargetResult
    data class NoMatch(val query: String) : OnboardingQuickStartSandboxTargetResult
}

@Singleton
class RealOnboardingQuickStartSandboxResolver @Inject constructor() :
    OnboardingQuickStartSandboxResolver {

    override fun buildShortlist(
        items: List<OnboardingQuickStartItem>,
        limit: Int
    ): List<ActiveTaskContext> {
        return items.takeLast(limit).map { item ->
            ActiveTaskContext(
                taskId = item.stableId,
                title = item.title,
                timeSummary = buildString {
                    append(item.dateLabel)
                    append(" ")
                    append(item.timeLabel)
                }.trim(),
                isVague = !item.isExact,
                keyPerson = item.keyPerson,
                location = item.location,
                notesDigest = item.notesDigest ?: item.timeHint
            )
        }
    }

    override fun resolveTarget(
        items: List<OnboardingQuickStartItem>,
        target: TargetResolutionRequest,
        suggestedTaskId: String?
    ): OnboardingQuickStartSandboxTargetResult {
        if (items.isEmpty()) {
            return OnboardingQuickStartSandboxTargetResult.NoMatch(target.describeForFailure())
        }

        val ordinalMatch = resolveOrdinalTarget(items, target.targetQuery)
        if (ordinalMatch != null) {
            return OnboardingQuickStartSandboxTargetResult.Resolved(ordinalMatch)
        }

        val ranked = items.map { item ->
            item to TaskRetrievalScoring.scoreCandidate(
                query = TaskRetrievalScoring.normalize(target.targetQuery),
                person = TaskRetrievalScoring.normalize(target.targetPerson),
                location = TaskRetrievalScoring.normalize(target.targetLocation),
                candidate = TaskRetrievalCandidate(
                    id = item.stableId,
                    title = item.title,
                    participants = listOfNotNull(item.keyPerson),
                    location = item.location,
                    notes = item.notesDigest ?: item.timeHint
                )
            )
        }
            .filter { (_, score) -> score > 0 }
            .sortedByDescending { (_, score) -> score }

        val top = ranked.firstOrNull()
            ?: return OnboardingQuickStartSandboxTargetResult.NoMatch(target.describeForFailure())
        if (top.second < TaskRetrievalScoring.MIN_RESOLUTION_SCORE) {
            return OnboardingQuickStartSandboxTargetResult.NoMatch(target.describeForFailure())
        }

        val runnerUpScore = ranked.getOrNull(1)?.second ?: 0
        if (runnerUpScore > 0 && top.second - runnerUpScore < TaskRetrievalScoring.MIN_MARGIN_SCORE) {
            return OnboardingQuickStartSandboxTargetResult.Ambiguous(target.describeForFailure())
        }

        if (suggestedTaskId != null && suggestedTaskId != top.first.stableId) {
            val suggested = ranked.firstOrNull { it.first.stableId == suggestedTaskId }
                ?: return OnboardingQuickStartSandboxTargetResult.NoMatch(target.describeForFailure())
            if (top.second - suggested.second < TaskRetrievalScoring.MIN_MARGIN_SCORE) {
                return OnboardingQuickStartSandboxTargetResult.Ambiguous(target.describeForFailure())
            }
            return OnboardingQuickStartSandboxTargetResult.NoMatch(target.describeForFailure())
        }

        return OnboardingQuickStartSandboxTargetResult.Resolved(top.first)
    }

    private fun resolveOrdinalTarget(
        items: List<OnboardingQuickStartItem>,
        rawQuery: String
    ): OnboardingQuickStartItem? {
        val query = rawQuery.lowercase()
        return when {
            query.contains("最后") || query.contains("last") -> items.lastOrNull()
            query.contains("第一") || query.contains("第一个") || query.contains("first") -> items.firstOrNull()
            else -> null
        }
    }
}

@Singleton
class RealOnboardingQuickStartService @Inject constructor(
    private val uniAExtractionService: RealUniAExtractionService,
    private val uniBExtractionService: RealUniBExtractionService,
    private val uniMExtractionService: RealUniMExtractionService,
    private val globalRescheduleExtractionService: RealGlobalRescheduleExtractionService,
    private val sandboxResolver: OnboardingQuickStartSandboxResolver,
    private val timeProvider: TimeProvider
) : OnboardingQuickStartService {

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

    private data class FragmentAnchorState(
        val exactStart: Instant? = null,
        val anchorDate: LocalDate? = null
    )

    private sealed interface ResolvedMultiTaskFragment {
        data class Resolved(
            val result: FastTrackResult,
            val nextState: FragmentAnchorState
        ) : ResolvedMultiTaskFragment

        data class Unresolved(val reason: String) : ResolvedMultiTaskFragment
    }

    private data class QuickStartUpdate(
        val updatedItem: OnboardingQuickStartItem,
        val touchedExactTask: Boolean
    )

    private data class MultiTaskGateDecision(
        val shouldAttempt: Boolean,
        val taskClauseCount: Int,
        val timeAnchorCount: Int,
        val cueLabel: String
    )

    override suspend fun applyTranscript(
        transcript: String,
        currentItems: List<OnboardingQuickStartItem>
    ): OnboardingQuickStartServiceResult {
        val normalized = transcript.trim()
        if (normalized.isBlank()) {
            return OnboardingQuickStartServiceResult.Failure("未识别到有效日程内容")
        }
        val startedAt = SystemClock.elapsedRealtime()
        Log.d(
            TAG,
            "apply_start transcriptLength=${normalized.length} stagedItems=${currentItems.size}"
        )
        return try {
            when (
                val decision = schedulerRouter.routeGeneral(
                    SchedulerIntelligenceRouter.GeneralContext(
                        transcript = normalized,
                        surface = SchedulerIntelligenceRouter.SchedulerSurface.ONBOARDING_SANDBOX,
                        displayedDateIso = null,
                        activeTaskShortlist = sandboxResolver.buildShortlist(currentItems),
                        uniMTimeoutMs = ONBOARDING_UNI_M_TIMEOUT_MS
                    )
                )
            ) {
                is SchedulerIntelligenceRouter.Decision.Create -> {
                    logRouteDecision(decision.metadata, startedAt)
                    when (val result = decision.result) {
                        is SchedulerPathACreateInterpreter.Result.SingleMatched -> {
                            logCreateRoute(result.telemetry)
                            createSuccess(currentItems, result.intent.toQuickStartItems())
                        }
                        is SchedulerPathACreateInterpreter.Result.MultiMatched -> {
                            logCreateRoute(result.telemetry)
                            createSuccess(
                                currentItems,
                                result.intents.flatMap { it.toQuickStartItems() }
                            )
                        }
                        is SchedulerPathACreateInterpreter.Result.DirectFailure -> {
                            logCreateRoute(result.telemetry)
                            OnboardingQuickStartServiceResult.Failure(result.message)
                        }
                        is SchedulerPathACreateInterpreter.Result.NotMatched -> {
                            logCreateRoute(result.telemetry)
                            OnboardingQuickStartServiceResult.Failure(
                                sanitizeTerminalCreateFailure(result.reason)
                            )
                        }
                    }
                }

                is SchedulerIntelligenceRouter.Decision.GlobalReschedule -> {
                    logRouteDecision(decision.metadata, startedAt)
                    resolveSandboxUpdate(decision.extracted, currentItems)
                }

                is SchedulerIntelligenceRouter.Decision.FollowUpReschedule -> {
                    logRouteDecision(decision.metadata, startedAt)
                    OnboardingQuickStartServiceResult.Failure("当前体验不支持已选中任务跟进改期")
                }

                is SchedulerIntelligenceRouter.Decision.Reject -> {
                    logRouteDecision(decision.metadata, startedAt)
                    OnboardingQuickStartServiceResult.Failure(decision.message)
                }

                is SchedulerIntelligenceRouter.Decision.NotMatched -> {
                    logRouteDecision(decision.metadata, startedAt)
                    OnboardingQuickStartServiceResult.Failure(
                        sanitizeTerminalCreateFailure(decision.reason)
                    )
                }
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            Log.w(
                TAG,
                "apply_exception durationMs=${SystemClock.elapsedRealtime() - startedAt} message=${error.message}",
                error
            )
            throw error
        }
    }

    private suspend fun resolveSandboxUpdate(
        extracted: GlobalRescheduleExtractionResult.Supported,
        currentItems: List<OnboardingQuickStartItem>
    ): OnboardingQuickStartServiceResult {
        return when (
            val resolution = sandboxResolver.resolveTarget(
                items = currentItems,
                target = extracted.target,
                suggestedTaskId = extracted.suggestedTaskId
            )
        ) {
            is OnboardingQuickStartSandboxTargetResult.Resolved -> {
                val update = resolveQuickStartUpdate(resolution.item, extracted.timeInstruction)
                    ?: return OnboardingQuickStartServiceResult.Failure("改期时间格式无法解析，请换一种明确说法")
                val updatedItems = currentItems.map { item ->
                    if (item.stableId == resolution.item.stableId) {
                        update.updatedItem.copy(highlightToken = item.highlightToken + 1)
                    } else {
                        item
                    }
                }
                OnboardingQuickStartServiceResult.Success(
                    items = updatedItems,
                    touchedExactTask = update.touchedExactTask,
                    mutationKind = OnboardingQuickStartServiceResult.Success.MutationKind.UPDATE
                )
            }
            is OnboardingQuickStartSandboxTargetResult.Ambiguous -> {
                OnboardingQuickStartServiceResult.Failure("目标不明确，请换一种更具体的说法")
            }
            is OnboardingQuickStartSandboxTargetResult.NoMatch -> {
                OnboardingQuickStartServiceResult.Failure("未找到要修改的体验日程，请先创建或说得更具体一些")
            }
        }
    }

    private suspend fun resolveCreate(
        transcript: String,
        currentItems: List<OnboardingQuickStartItem>
    ): OnboardingQuickStartServiceResult {
        return when (
            val result = createInterpreter.interpret(
                transcript = transcript,
                displayedDateIso = null,
                uniMTimeoutMs = ONBOARDING_UNI_M_TIMEOUT_MS
            )
        ) {
            is SchedulerPathACreateInterpreter.Result.SingleMatched -> {
                logCreateRoute(result.telemetry)
                createSuccess(currentItems, result.intent.toQuickStartItems())
            }
            is SchedulerPathACreateInterpreter.Result.MultiMatched -> {
                logCreateRoute(result.telemetry)
                createSuccess(
                    currentItems,
                    result.intents.flatMap { it.toQuickStartItems() }
                )
            }
            is SchedulerPathACreateInterpreter.Result.DirectFailure -> {
                logCreateRoute(result.telemetry)
                OnboardingQuickStartServiceResult.Failure(result.message)
            }
            is SchedulerPathACreateInterpreter.Result.NotMatched -> {
                logCreateRoute(result.telemetry)
                OnboardingQuickStartServiceResult.Failure(
                    sanitizeTerminalCreateFailure(result.reason)
                )
            }
        }
    }

    private fun logCreateRoute(telemetry: SchedulerPathACreateInterpreter.Telemetry) {
        Log.d(
            TAG,
            "create_route stage=${telemetry.routeStage} uniM=${telemetry.uniMAttemptOutcome} parseUnresolved=${telemetry.parseUnresolvedCount} downgraded=${telemetry.downgradedCount}"
        )
    }

    private fun logRouteDecision(
        metadata: SchedulerIntelligenceRouter.RouteMetadata,
        startedAt: Long
    ) {
        Log.d(
            TAG,
            "route_decision intent=${metadata.intentKind} shape=${metadata.taskShape} owner=${metadata.owner} terminal=${metadata.schedulerTerminalOnCommit} durationMs=${SystemClock.elapsedRealtime() - startedAt} reason=${metadata.reason ?: "none"}"
        )
    }

    private fun createSuccess(
        currentItems: List<OnboardingQuickStartItem>,
        createdItems: List<OnboardingQuickStartItem>
    ): OnboardingQuickStartServiceResult {
        return OnboardingQuickStartServiceResult.Success(
            items = currentItems + createdItems,
            touchedExactTask = createdItems.any { it.isExact },
            mutationKind = OnboardingQuickStartServiceResult.Success.MutationKind.CREATE
        )
    }

    private fun buildDeterministicRelativeCreateResult(
        transcript: String,
        normalizedTranscript: String
    ): OnboardingQuickStartItem? {
        if (shouldAttemptMultiCreate(transcript, normalizedTranscript).shouldAttempt) return null
        val resolution = RelativeTimeResolver.resolveExact(
            userText = transcript,
            nowIso = timeProvider.now.toString(),
            timezone = timeProvider.zoneId.id
        ) ?: return null
        val title = stripDeterministicRelativeTimePhrase(
            transcript = normalizedTranscript,
            matchedText = resolution.matchedText
        ) ?: return null
        return FastTrackResult.CreateTasks(
            CreateTasksParams(
                unifiedId = UUID.randomUUID().toString(),
                tasks = listOf(
                    TaskDefinition(
                        title = title,
                        startTimeIso = resolution.startTimeIso,
                        durationMinutes = 0,
                        urgency = UrgencyEnum.L3_NORMAL
                    )
                )
            )
        ).toQuickStartItems().singleOrNull()
    }

    private fun buildDeterministicWakeCreateResult(transcript: String): OnboardingQuickStartItem? {
        if (shouldAttemptMultiCreate(transcript, transcript).shouldAttempt) return null
        val startTimeIso = ExactTimeCueResolver.resolveExactDayClockStartTime(
            transcript = transcript,
            nowIso = timeProvider.now.toString(),
            timezone = timeProvider.zoneId.id,
            displayedDateIso = null
        ) ?: return null
        val title = stripDeterministicDayClockPhrase(transcript)
            ?.takeIf(::looksLikeWakeReminderBody)
            ?: return null
        return FastTrackResult.CreateTasks(
            CreateTasksParams(
                unifiedId = UUID.randomUUID().toString(),
                tasks = listOf(
                    TaskDefinition(
                        title = title,
                        startTimeIso = startTimeIso,
                        durationMinutes = 0,
                        urgency = UrgencyEnum.FIRE_OFF
                    )
                )
            )
        ).toQuickStartItems().singleOrNull()
    }

    private fun resolveMultiTaskCreate(
        fragments: List<UniMTaskFragment>
    ): List<OnboardingQuickStartItem> {
        var anchorState = FragmentAnchorState()
        val results = mutableListOf<OnboardingQuickStartItem>()

        fragments.forEach { fragment ->
            when (val resolved = resolveMultiTaskFragment(fragment, anchorState)) {
                is ResolvedMultiTaskFragment.Resolved -> {
                    val nextItems = resolved.result.toQuickStartItems()
                    if (nextItems.isNotEmpty()) {
                        results += nextItems
                        anchorState = resolved.nextState
                    }
                }
                is ResolvedMultiTaskFragment.Unresolved -> Unit
            }
        }
        return results
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
                    result = buildExactCreateResult(
                        unifiedId = UUID.randomUUID().toString(),
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
                    result = buildVagueCreateResult(
                        unifiedId = UUID.randomUUID().toString(),
                        title = fragment.title,
                        anchorDateIso = anchorDateIso,
                        timeHint = fragment.timeHint,
                        keyPerson = fragment.keyPerson,
                        location = fragment.location,
                        urgency = fragment.urgency
                    ),
                    nextState = FragmentAnchorState(anchorDate = anchorDate)
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
                result = buildExactCreateResult(
                    unifiedId = UUID.randomUUID().toString(),
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
                    result = buildVagueCreateResult(
                        unifiedId = UUID.randomUUID().toString(),
                        title = fragment.title,
                        anchorDateIso = anchorDate.toString(),
                        timeHint = fragment.timeHint,
                        keyPerson = fragment.keyPerson,
                        location = fragment.location,
                        urgency = fragment.urgency
                    ),
                    nextState = FragmentAnchorState(anchorDate = anchorDate)
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
                    result = buildExactCreateResult(
                        unifiedId = UUID.randomUUID().toString(),
                        title = fragment.title,
                        startTimeIso = startTimeIso,
                        durationMinutes = fragment.durationMinutes,
                        keyPerson = fragment.keyPerson,
                        location = fragment.location,
                        urgency = fragment.urgency
                    ),
                    nextState = FragmentAnchorState(exactStart = start, anchorDate = targetDate)
                )
            }
            UniMTaskMode.VAGUE -> ResolvedMultiTaskFragment.Resolved(
                result = buildVagueCreateResult(
                    unifiedId = UUID.randomUUID().toString(),
                    title = fragment.title,
                    anchorDateIso = targetDate.toString(),
                    timeHint = fragment.timeHint,
                    keyPerson = fragment.keyPerson,
                    location = fragment.location,
                    urgency = fragment.urgency
                ),
                nextState = FragmentAnchorState(anchorDate = targetDate)
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
                    result = buildExactCreateResult(
                        unifiedId = UUID.randomUUID().toString(),
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
                        result = buildVagueCreateResult(
                            unifiedId = UUID.randomUUID().toString(),
                            title = fragment.title,
                            anchorDateIso = anchorDate.toString(),
                            timeHint = fragment.timeHint,
                            keyPerson = fragment.keyPerson,
                            location = fragment.location,
                            urgency = fragment.urgency
                        ),
                        nextState = FragmentAnchorState(anchorDate = anchorDate)
                    )
                }
            }
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
                    result = buildExactCreateResult(
                        unifiedId = UUID.randomUUID().toString(),
                        title = fragment.title,
                        startTimeIso = startTimeIso,
                        durationMinutes = fragment.durationMinutes,
                        keyPerson = fragment.keyPerson,
                        location = fragment.location,
                        urgency = fragment.urgency
                    ),
                    nextState = FragmentAnchorState(exactStart = start, anchorDate = targetDate)
                )
            }
            UniMTaskMode.VAGUE -> ResolvedMultiTaskFragment.Resolved(
                result = buildVagueCreateResult(
                    unifiedId = UUID.randomUUID().toString(),
                    title = fragment.title,
                    anchorDateIso = targetDate.toString(),
                    timeHint = fragment.timeHint,
                    keyPerson = fragment.keyPerson,
                    location = fragment.location,
                    urgency = fragment.urgency
                ),
                nextState = FragmentAnchorState(anchorDate = targetDate)
            )
        }
    }

    private suspend fun resolveQuickStartUpdate(
        original: OnboardingQuickStartItem,
        timeInstruction: String
    ): QuickStartUpdate? {
        resolveRelativeDayOnlyInstruction(timeInstruction)?.let { targetDate ->
            val updated = if (original.isExact) {
                original.copy(
                    dateIso = targetDate.toString(),
                    dateLabel = targetDate.toQuickStartDateLabel(timeProvider.today)
                )
            } else {
                original.copy(
                    dateIso = targetDate.toString(),
                    dateLabel = targetDate.toQuickStartDateLabel(timeProvider.today)
                )
            }
            return QuickStartUpdate(updatedItem = updated, touchedExactTask = original.isExact)
        }

        val exactTimeIso = ExactTimeCueResolver.resolveExactDayClockStartTime(
            transcript = timeInstruction,
            nowIso = timeProvider.now.toString(),
            timezone = timeProvider.zoneId.id,
            displayedDateIso = original.dateIso
        )
        if (exactTimeIso != null) {
            val updatedStart = parseExactInstant(exactTimeIso) ?: return null
            return QuickStartUpdate(
                updatedItem = original.withExactStart(updatedStart, timeProvider.today, timeProvider.zoneId),
                touchedExactTask = true
            )
        }

        if (original.isExact) {
            val exact = uniAExtractionService.extract(
                UniAExtractionRequest(
                    transcript = normalizeExactRescheduleInstruction(timeInstruction),
                    nowIso = timeProvider.now.toString(),
                    timezone = timeProvider.zoneId.id,
                    unifiedId = original.stableId,
                    displayedDateIso = original.dateIso
                )
            )
            val updatedStartIso = (exact as? FastTrackResult.CreateTasks)
                ?.params
                ?.tasks
                ?.singleOrNull()
                ?.startTimeIso
            if (updatedStartIso != null) {
                val updatedStart = parseExactInstant(updatedStartIso) ?: return null
                return QuickStartUpdate(
                    updatedItem = original.withExactStart(updatedStart, timeProvider.today, timeProvider.zoneId),
                    touchedExactTask = true
                )
            }
        }

        return null
    }

    private fun resolveRelativeDayOnlyInstruction(text: String): LocalDate? {
        if (containsExplicitClock(text)) return null
        val normalized = text.lowercase()
        return when {
            normalized.contains("大后天") -> timeProvider.today.plusDays(3)
            normalized.contains("后天") || normalized.contains("day after tomorrow") -> timeProvider.today.plusDays(2)
            normalized.contains("明天") || normalized.contains("tomorrow") -> timeProvider.today.plusDays(1)
            normalized.contains("今天") || normalized.contains("today") || normalized.contains("今晚") -> timeProvider.today
            else -> null
        }
    }

    private fun normalizeExactRescheduleInstruction(transcript: String): String {
        val trimmed = transcript.trim()
        val prefix = EXACT_RESCHEDULE_PREFIXES.firstOrNull { trimmed.startsWith(it, ignoreCase = true) }
            ?: return trimmed
        return trimmed.removePrefix(prefix).trim()
    }

    private fun containsExplicitClock(text: String): Boolean {
        return CLOCK_HINT_REGEX.containsMatchIn(text.lowercase())
    }

    private fun looksLikeRescheduleTranscript(text: String): Boolean {
        return schedulerRouter.mightExpressReschedule(text)
    }

    private fun looksLikeDeletionTranscript(text: String): Boolean {
        val normalized = text.lowercase()
        return listOf("删除", "取消", "删掉", "delete", "cancel").any { normalized.contains(it) }
    }

    private fun shouldAttemptMultiCreate(
        transcript: String,
        normalizedTranscript: String
    ): MultiTaskGateDecision {
        val clauses = splitMultiTaskClauses(transcript)
        val taskClauseCount = clauses.count(::looksLikeIndependentTaskClause)
        if (taskClauseCount >= 2) {
            return MultiTaskGateDecision(
                shouldAttempt = true,
                taskClauseCount = taskClauseCount,
                timeAnchorCount = countLikelyTimeAnchors(normalizedTranscript),
                cueLabel = "multi_clause"
            )
        }

        val hasWeakConnector = WEAK_MULTI_CONNECTORS.any(transcript::contains)
        val timeAnchorCount = countLikelyTimeAnchors(normalizedTranscript)
        return if (hasWeakConnector && timeAnchorCount >= 2) {
            MultiTaskGateDecision(
                shouldAttempt = true,
                taskClauseCount = taskClauseCount,
                timeAnchorCount = timeAnchorCount,
                cueLabel = "weak_connector_with_multi_anchor"
            )
        } else {
            MultiTaskGateDecision(
                shouldAttempt = false,
                taskClauseCount = taskClauseCount,
                timeAnchorCount = timeAnchorCount,
                cueLabel = "single_task_bias"
            )
        }
    }

    private fun splitMultiTaskClauses(text: String): List<String> {
        return STRONG_MULTI_SEPARATOR_REGEX
            .split(text)
            .map(String::trim)
            .filter(String::isNotEmpty)
    }

    private fun looksLikeIndependentTaskClause(text: String): Boolean {
        val stripped = STRIP_TIME_LANGUAGE_REGEX.replace(text, "")
            .replace("提醒我", "")
            .replace("请提醒我", "")
            .replace("请", "")
            .replace("一下", "")
            .replace("帮我", "")
            .replace("给我", "")
            .replace("记得", "")
            .replace("我要", "")
            .replace("我想", "")
            .replace("去", "")
            .replace("和", "")
            .replace("跟", "")
            .replace("再", "")
            .replace("然后", "")
            .replace("以及", "")
            .replace("并且", "")
            .replace("还有", "")
            .replace("，", "")
            .replace(",", "")
            .replace("、", "")
            .replace("。", "")
            .replace("；", "")
            .replace(";", "")
            .replace(" ", "")
        return stripped.length >= 2
    }

    private fun countLikelyTimeAnchors(text: String): Int {
        return TIME_ANCHOR_REGEX.findAll(text).count()
    }

    private fun logMultiTaskGate(decision: MultiTaskGateDecision) {
        Log.d(
            TAG,
            "create_route uniMAttempt=${decision.shouldAttempt} cue=${decision.cueLabel} taskClauses=${decision.taskClauseCount} timeAnchors=${decision.timeAnchorCount}"
        )
    }

    private fun looksLikeWakeReminderBody(text: String): Boolean {
        val normalized = text.lowercase()
        return listOf("喊我起来", "叫我起来", "起床", "叫醒我", "起来").any(normalized::contains)
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
            .replace(Regex("(今天|明天|后天|tomorrow|day after tomorrow)", RegexOption.IGNORE_CASE), "")
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

    private fun sanitizeTerminalCreateFailure(reason: String): String {
        val normalized = reason.lowercase()
        return if (
            normalized.contains("安排日程") ||
            normalized.contains("明确时间点") ||
            normalized.contains("模糊任务") ||
            normalized.contains("时间信息") ||
            normalized.contains("schedulable") ||
            normalized.contains("uni-b") ||
            normalized.startsWith("uni-") ||
            normalized.contains("json") ||
            normalized.contains("not_vague") ||
            normalized.contains("not_exact")
        ) {
            "未能解析为可创建日程，请换一种更明确的说法"
        } else {
            reason
        }
    }

    private fun buildExactCreateResult(
        unifiedId: String,
        title: String,
        startTimeIso: String,
        durationMinutes: Int,
        keyPerson: String? = null,
        location: String? = null,
        urgency: UrgencyEnum
    ): FastTrackResult {
        return FastTrackResult.CreateTasks(
            CreateTasksParams(
                unifiedId = unifiedId,
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
        unifiedId: String,
        title: String,
        anchorDateIso: String,
        timeHint: String?,
        keyPerson: String? = null,
        location: String? = null,
        urgency: UrgencyEnum
    ): FastTrackResult {
        return FastTrackResult.CreateVagueTask(
            CreateVagueTaskParams(
                unifiedId = unifiedId,
                title = title,
                anchorDateIso = anchorDateIso,
                timeHint = timeHint,
                keyPerson = keyPerson,
                location = location,
                urgency = urgency
            )
        )
    }

    private fun FastTrackResult.toQuickStartItems(): List<OnboardingQuickStartItem> {
        return when (this) {
            is FastTrackResult.CreateTasks -> params.tasks.map { task ->
                val start = parseExactInstant(task.startTimeIso) ?: return emptyList()
                val zoned = start.atZone(timeProvider.zoneId)
                OnboardingQuickStartItem(
                    stableId = params.unifiedId?.takeIf { params.tasks.size == 1 } ?: UUID.randomUUID().toString(),
                    title = task.title,
                    timeLabel = TIME_LABEL_FORMATTER.format(zoned),
                    dateLabel = zoned.toLocalDate().toQuickStartDateLabel(timeProvider.today),
                    dateIso = zoned.toLocalDate().toString(),
                    urgencyLevel = task.urgency.toUrgencyLevel(),
                    startHour = zoned.hour,
                    startMinute = zoned.minute,
                    keyPerson = task.keyPerson,
                    location = task.location,
                    notesDigest = listOfNotNull(task.keyPerson, task.location).joinToString(" · ").takeIf { it.isNotBlank() }
                )
            }
            is FastTrackResult.CreateVagueTask -> listOf(
                OnboardingQuickStartItem(
                    stableId = params.unifiedId ?: UUID.randomUUID().toString(),
                    title = params.title,
                    timeLabel = "待定",
                    dateLabel = LocalDate.parse(params.anchorDateIso).toQuickStartDateLabel(timeProvider.today),
                    dateIso = params.anchorDateIso,
                    urgencyLevel = params.urgency.toUrgencyLevel(),
                    keyPerson = params.keyPerson,
                    location = params.location,
                    timeHint = params.timeHint,
                    notesDigest = listOfNotNull(params.timeHint, params.keyPerson, params.location)
                        .joinToString(" · ")
                        .takeIf { it.isNotBlank() }
                )
            )
            else -> emptyList()
        }
    }

    private fun OnboardingQuickStartItem.toExactInstant(zoneId: java.time.ZoneId): Instant? {
        val hour = startHour ?: return null
        val minute = startMinute ?: return null
        return runCatching {
            LocalDate.parse(dateIso).atTime(hour, minute).atZone(zoneId).toInstant()
        }.getOrNull()
    }

    private fun OnboardingQuickStartItem.withExactStart(
        start: Instant,
        today: LocalDate,
        zoneId: java.time.ZoneId
    ): OnboardingQuickStartItem {
        val zoned = start.atZone(zoneId)
        return copy(
            timeLabel = TIME_LABEL_FORMATTER.format(zoned),
            dateLabel = zoned.toLocalDate().toQuickStartDateLabel(today),
            dateIso = zoned.toLocalDate().toString(),
            startHour = zoned.hour,
            startMinute = zoned.minute,
            timeHint = null,
            notesDigest = listOfNotNull(keyPerson, location).joinToString(" · ").takeIf { it.isNotBlank() }
        )
    }

    private fun LocalDate.toQuickStartDateLabel(today: LocalDate): String {
        return when (java.time.temporal.ChronoUnit.DAYS.between(today, this).toInt()) {
            0 -> "今天"
            1 -> "明天"
            2 -> "后天"
            3 -> "大后天"
            else -> MONTH_DAY_FORMATTER.format(this)
        }
    }

    private fun parseExactInstant(raw: String): Instant? {
        return runCatching { Instant.parse(raw) }
            .recoverCatching { OffsetDateTime.parse(raw).toInstant() }
            .getOrNull()
    }

    private fun UrgencyEnum.toUrgencyLevel(): UrgencyLevel {
        return when (this) {
            UrgencyEnum.L1_CRITICAL -> UrgencyLevel.L1_CRITICAL
            UrgencyEnum.L2_IMPORTANT -> UrgencyLevel.L2_IMPORTANT
            UrgencyEnum.L3_NORMAL -> UrgencyLevel.L3_NORMAL
            UrgencyEnum.FIRE_OFF -> UrgencyLevel.FIRE_OFF
        }
    }

    private companion object {
        private const val TAG = "OnboardingQuickStart"
        private const val ONBOARDING_UNI_M_TIMEOUT_MS = 1_500L
        private val TIME_LABEL_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")
        private val MONTH_DAY_FORMATTER = DateTimeFormatter.ofPattern("M月d日")
        private val CLOCK_HINT_REGEX = Regex(
            pattern = "(上午|下午|中午|晚上|凌晨|早上|\\d{1,2}:\\d{2}|\\d{1,2}点半?|\\d{1,2}時|\\d{1,2}时)"
        )
        private val TIME_ANCHOR_REGEX = Regex(
            pattern = "(今天|明天|后天|大后天|今晚|tomorrow|day after tomorrow|\\d{1,2}:\\d{2}|\\d{1,2}点半?|\\d+小时后|\\d+分钟后)"
        )
        private val STRONG_MULTI_SEPARATOR_REGEX = Regex("(，|,|、|；|;|然后|接着|以及|并且|还有)")
        private val STRIP_TIME_LANGUAGE_REGEX = Regex(
            pattern = "(今天|明天|后天|大后天|今晚|tomorrow|day after tomorrow|凌晨|早上|上午|中午|下午|晚上|午夜|半夜|\\d{1,2}:\\d{2}|\\d{1,2}点半?|\\d+小时后|\\d+分钟后)"
        )
        private val WEAK_MULTI_CONNECTORS = listOf("再")
        private val EXACT_RESCHEDULE_PREFIXES = listOf(
            "改期到",
            "改到",
            "改成",
            "挪到",
            "推迟到",
            "提前到",
            "reschedule to",
            "move to"
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
}
