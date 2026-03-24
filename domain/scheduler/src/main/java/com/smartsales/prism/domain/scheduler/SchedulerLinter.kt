package com.smartsales.prism.domain.scheduler

import com.smartsales.prism.domain.core.UnifiedMutation
import com.smartsales.prism.domain.core.TaskMutation
import com.smartsales.prism.domain.memory.ConflictPolicy
import com.smartsales.prism.domain.time.TimeProvider
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerializationException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 调度器校验器 — Path A FastTrack Parser (新) + Legacy lint() 兼容层 (旧)
 *
 * 新代码应使用 parseFastTrackIntent() 获取纯 DTO。
 * 旧代码 (L2DebugHud, L2StrictInterfaceIntegrityTest) 暂时继续使用 lint() → LintResult。
 */
@Singleton
class SchedulerLinter @Inject constructor() {

    /** 兼容旧调用方式: SchedulerLinter(timeProvider) */
    @Suppress("unused")
    constructor(timeProvider: TimeProvider) : this() {
        this.legacyTimeProvider = timeProvider
    }

    private var legacyTimeProvider: TimeProvider? = null

    private val jsonInterpreter = Json { 
        ignoreUnknownKeys = true 
        coerceInputValues = true 
    }

    /**
     * Parses real-time ASR audio intents into exact one-currency DTOs for Path A optimistic execution.
     * Operates purely on lexical/semantic input without requiring an LLM if local rules match, 
     * or via a fast LLM extraction pass.
     */
    fun parseFastTrackIntent(input: String): FastTrackResult {
        return try {
            val mutation = jsonInterpreter.decodeFromString<UnifiedMutation>(input)
            
            when (mutation.classification.lowercase()) {
                "non_intent", "none" -> {
                    return FastTrackResult.NoMatch(
                        reason = mutation.reason ?: "未检测到日程安排意图"
                    )
                }
                "deletion" -> {
                    // Path A specifically blocks global deletions due to safety constraints.
                    return FastTrackResult.NoMatch(
                        reason = "语音删除操作已被系统限制，请在面板手动操作。"
                    )
                }
                "inspiration" -> {
                    val content = mutation.tasks.firstOrNull()?.notes 
                        ?: mutation.response 
                        ?: mutation.thought
                        ?: return FastTrackResult.NoMatch("未检测到灵感内容")

                    return FastTrackResult.CreateInspiration(
                        params = CreateInspirationParams(content = content)
                    )
                }
                "reschedule" -> {
                    val targetTitle = mutation.targetTitle ?: ""
                    val newInstruction = mutation.newInstruction ?: ""
                    if (targetTitle.isBlank() || newInstruction.isBlank()) {
                        return FastTrackResult.NoMatch("改期请求缺少目标任务或新指令")
                    }
                    
                    return FastTrackResult.RescheduleTask(
                        params = RescheduleTaskParams(
                            targetQuery = targetTitle,
                            newStartTimeIso = newInstruction
                            // Note: actual Temporal targeting mapping depends on LLM explicit capability.
                            // Falling back to Lexical targeting for now as requested by Path A PRD.
                        )
                    )
                }
                // Default to creating tasks, but gracefully fail if missing data
                "schedulable" -> {
                    if (mutation.tasks.isEmpty()) {
                        return FastTrackResult.NoMatch("无可用日程")
                    }

                    val definitions = mutation.tasks.mapNotNull { parseTaskDefinition(it) }

                    if (definitions.isEmpty()) {
                        return FastTrackResult.NoMatch("时间格式无法解析为精准日程")
                    }

                    return FastTrackResult.CreateTasks(
                        params = CreateTasksParams(
                            tasks = definitions
                        )
                    )
                }
                else -> {
                    return FastTrackResult.NoMatch("未知的操作意图: ${mutation.classification}")
                }
            }
        } catch (e: SerializationException) {
            FastTrackResult.NoMatch("JSON 结构异常: ${e.message}")
        } catch (e: Exception) {
            FastTrackResult.NoMatch("无法解析该意图: ${e.message}")
        }
    }

    /**
     * 解析 Uni-A 轻量提取结果。
     * 说明：必须和 PromptCompiler 使用同一个 @Serializable contract。
     */
    fun parseUniAExtraction(
        input: String,
        unifiedId: String,
        transcript: String? = null,
        nowIso: String? = null,
        timezone: String? = null,
        displayedDateIso: String? = null
    ): FastTrackResult {
        return try {
            val payload = jsonInterpreter.decodeFromString<UniAExtractionPayload>(cleanJson(input))
            when (payload.decision.uppercase()) {
                "EXACT_CREATE" -> {
                    val task = payload.task ?: return FastTrackResult.NoMatch("Uni-A exact result missing task payload")
                    val title = task.title.trim()
                    if (title.isBlank()) {
                        return FastTrackResult.NoMatch("Uni-A exact task title is blank")
                    }

                    val startTimeIso = task.startTimeIso.trim()
                    if (startTimeIso.isBlank()) {
                        return FastTrackResult.NoMatch("Uni-A exact task time is blank")
                    }
                    val normalizedStartTimeIso = normalizeRelativeDayStartTime(
                        transcript = transcript,
                        startTimeIso = startTimeIso,
                        nowIso = nowIso,
                        timezone = timezone,
                        displayedDateIso = displayedDateIso
                    ) ?: return FastTrackResult.NoMatch("Uni-A exact date anchor requires displayed page context")
                    if (parseStrictOffsetDateTime(normalizedStartTimeIso) == null) {
                        return FastTrackResult.NoMatch("Uni-A exact task time must be strict ISO-8601")
                    }

                    val antiFabricationReason = rejectFabricatedExactTime(
                        transcript = transcript,
                        startTimeIso = normalizedStartTimeIso
                    )
                    if (antiFabricationReason != null) {
                        return FastTrackResult.NoMatch(antiFabricationReason)
                    }

                    val urgency = normalizeUrgency(task.urgency)
                    return FastTrackResult.CreateTasks(
                        params = CreateTasksParams(
                            unifiedId = unifiedId,
                            tasks = listOf(
                                TaskDefinition(
                                    title = title,
                                    startTimeIso = normalizedStartTimeIso,
                                    durationMinutes = task.durationMinutes.coerceAtLeast(0),
                                    urgency = urgency
                                )
                            )
                        )
                    )
                }
                "NOT_EXACT" -> {
                    FastTrackResult.NoMatch(payload.reason ?: "Uni-A decided input is not exact")
                }
                else -> FastTrackResult.NoMatch("Unknown Uni-A decision: ${payload.decision}")
            }
        } catch (e: SerializationException) {
            FastTrackResult.NoMatch("Uni-A JSON 结构异常: ${e.message}")
        } catch (e: Exception) {
            FastTrackResult.NoMatch("Uni-A 解析失败: ${e.message}")
        }
    }

    private fun rejectFabricatedExactTime(
        transcript: String?,
        startTimeIso: String
    ): String? {
        val normalized = transcript
            ?.lowercase()
            ?.trim()
            ?: return null

        val hasRelativeDayAnchor = listOf(
            "明天",
            "tomorrow",
            "下一天",
            "后一天",
            "后天",
            "nextday",
            "next day"
        ).any { normalized.contains(it) }
        if (!hasRelativeDayAnchor) return null

        val hasExplicitClockCue = listOf(
            Regex("""\b\d{1,2}:\d{1,2}\b"""),
            Regex("""\b\d{1,2}([:.]\d{1,2})?\s*(am|pm|a\.m\.|p\.m\.)\b"""),
            Regex("""\bat\s*\d{1,2}([:.]\d{1,2})?\s*(am|pm|a\.m\.|p\.m\.)\b"""),
            Regex("""(?:凌晨|早上|上午|中午|下午|晚上|今晚|午夜|半夜)?(?:[零一二两三四五六七八九十百\d]{1,3})点半"""),
            Regex("""(?:凌晨|早上|上午|中午|下午|晚上|今晚|午夜|半夜)?(?:[零一二两三四五六七八九十百\d]{1,3})点"""),
            Regex("""(?:凌晨|早上|上午|中午|下午|晚上|今晚|午夜|半夜)?一点半"""),
            Regex("""(?:凌晨|早上|上午|中午|下午|晚上|今晚|午夜|半夜)?一点"""),
            Regex("""(?:零点|午夜|midnight|noon)""")
        ).any { it.containsMatchIn(normalized) }

        if (hasExplicitClockCue) return null

        val parsedDateTime = parseStrictOffsetDateTime(startTimeIso) ?: return null

        return "Uni-A exact time rejected: date-only input must not fabricate exact clock time (${parsedDateTime.truncatedTo(ChronoUnit.SECONDS)})"
    }

    private fun parseStrictOffsetDateTime(raw: String): OffsetDateTime? {
        return try {
            OffsetDateTime.parse(raw)
        } catch (_: Exception) {
            return null
        }
    }

    /**
     * 解析 Uni-B 模糊提取结果。
     * 说明：必须和 PromptCompiler 使用同一个 @Serializable contract。
     */
    fun parseUniBExtraction(input: String, unifiedId: String): FastTrackResult {
        return try {
            val payload = jsonInterpreter.decodeFromString<UniBExtractionPayload>(cleanJson(input))
            when (payload.decision.uppercase()) {
                "VAGUE_CREATE" -> {
                    val task = payload.task ?: return FastTrackResult.NoMatch("Uni-B vague result missing task payload")
                    val title = task.title.trim()
                    if (title.isBlank()) {
                        return FastTrackResult.NoMatch("Uni-B vague task title is blank")
                    }

                    val anchorDateIso = task.anchorDateIso.trim()
                    if (anchorDateIso.isBlank()) {
                        return FastTrackResult.NoMatch("Uni-B vague task anchor date is blank")
                    }
                    try {
                        LocalDate.parse(anchorDateIso)
                    } catch (_: Exception) {
                        return FastTrackResult.NoMatch("Uni-B anchor date must be yyyy-MM-dd")
                    }

                    return FastTrackResult.CreateVagueTask(
                        params = CreateVagueTaskParams(
                            unifiedId = unifiedId,
                            title = title,
                            anchorDateIso = anchorDateIso,
                            timeHint = task.timeHint?.trim()?.takeIf { it.isNotBlank() },
                            urgency = normalizeUrgency(task.urgency)
                        )
                    )
                }
                "NOT_VAGUE" -> {
                    FastTrackResult.NoMatch(payload.reason ?: "Uni-B decided input is not vague-create")
                }
                else -> FastTrackResult.NoMatch("Unknown Uni-B decision: ${payload.decision}")
            }
        } catch (e: SerializationException) {
            FastTrackResult.NoMatch("Uni-B JSON 结构异常: ${e.message}")
        } catch (e: Exception) {
            FastTrackResult.NoMatch("Uni-B 解析失败: ${e.message}")
        }
    }

    fun parseUniBExtraction(
        input: String,
        unifiedId: String,
        transcript: String?,
        nowIso: String?,
        timezone: String?,
        displayedDateIso: String?
    ): FastTrackResult {
        return try {
            val payload = jsonInterpreter.decodeFromString<UniBExtractionPayload>(cleanJson(input))
            when (payload.decision.uppercase()) {
                "VAGUE_CREATE" -> {
                    val task = payload.task ?: return FastTrackResult.NoMatch("Uni-B vague result missing task payload")
                    val title = task.title.trim()
                    if (title.isBlank()) {
                        return FastTrackResult.NoMatch("Uni-B vague task title is blank")
                    }

                    val rawAnchorDateIso = task.anchorDateIso.trim()
                    if (rawAnchorDateIso.isBlank()) {
                        return FastTrackResult.NoMatch("Uni-B vague task anchor date is blank")
                    }
                    val normalizedAnchorDateIso = normalizeRelativeDayAnchorDate(
                        transcript = transcript,
                        anchorDateIso = rawAnchorDateIso,
                        nowIso = nowIso,
                        timezone = timezone,
                        displayedDateIso = displayedDateIso
                    ) ?: return FastTrackResult.NoMatch("Uni-B page-relative anchor requires displayed page context")
                    try {
                        LocalDate.parse(normalizedAnchorDateIso)
                    } catch (_: Exception) {
                        return FastTrackResult.NoMatch("Uni-B anchor date must be yyyy-MM-dd")
                    }
                    val normalizedTimeHint = task.timeHint?.trim()?.takeIf { it.isNotBlank() }
                    val exactStartTimeIso = buildExactStartTimeFromExplicitCue(
                        transcript = transcript,
                        anchorDateIso = normalizedAnchorDateIso,
                        timeHint = normalizedTimeHint,
                        timezone = timezone
                    )
                    if (exactStartTimeIso != null) {
                        return FastTrackResult.CreateTasks(
                            params = CreateTasksParams(
                                unifiedId = unifiedId,
                                tasks = listOf(
                                    TaskDefinition(
                                        title = title,
                                        startTimeIso = exactStartTimeIso,
                                        durationMinutes = 0,
                                        urgency = normalizeUrgency(task.urgency)
                                    )
                                )
                            )
                        )
                    }

                    return FastTrackResult.CreateVagueTask(
                        params = CreateVagueTaskParams(
                            unifiedId = unifiedId,
                            title = title,
                            anchorDateIso = normalizedAnchorDateIso,
                            timeHint = normalizedTimeHint,
                            urgency = normalizeUrgency(task.urgency)
                        )
                    )
                }
                "NOT_VAGUE" -> {
                    FastTrackResult.NoMatch(payload.reason ?: "Uni-B decided input is not vague-create")
                }
                else -> FastTrackResult.NoMatch("Unknown Uni-B decision: ${payload.decision}")
            }
        } catch (e: SerializationException) {
            FastTrackResult.NoMatch("Uni-B JSON 结构异常: ${e.message}")
        } catch (e: Exception) {
            FastTrackResult.NoMatch("Uni-B 解析失败: ${e.message}")
        }
    }

    /**
     * 解析 Uni-M 多任务拆解结果。
     * 说明：只负责多任务 create 片段合法性校验，不直接持久化。
     */
    fun parseUniMExtraction(input: String): UniMExtractionResult {
        return try {
            val payload = jsonInterpreter.decodeFromString<UniMExtractionPayload>(cleanJson(input))
            when (payload.decision.uppercase()) {
                "MULTI_CREATE" -> {
                    if (payload.fragments.isEmpty()) {
                        return UniMExtractionResult.NotMulti("Uni-M multi-create result missing fragments")
                    }
                    if (payload.fragments.size > 4) {
                        return UniMExtractionResult.NotMulti("Uni-M fragment count exceeds limit")
                    }

                    val fragments = payload.fragments.mapIndexed { index, fragment ->
                        parseUniMFragment(index, fragment)
                    }
                    UniMExtractionResult.MultiCreate(fragments)
                }
                "NOT_MULTI" -> {
                    UniMExtractionResult.NotMulti(payload.reason ?: "Uni-M decided input is not multi-create")
                }
                else -> UniMExtractionResult.NotMulti("Unknown Uni-M decision: ${payload.decision}")
            }
        } catch (e: SerializationException) {
            UniMExtractionResult.NotMulti("Uni-M JSON 结构异常: ${e.message}")
        } catch (e: IllegalArgumentException) {
            UniMExtractionResult.NotMulti(e.message ?: "Uni-M 片段结构非法")
        } catch (e: Exception) {
            UniMExtractionResult.NotMulti("Uni-M 解析失败: ${e.message}")
        }
    }

    private fun parseUniMFragment(index: Int, fragment: UniMTaskFragmentPayload): UniMTaskFragment {
        val title = fragment.title.trim()
        require(title.isNotBlank()) { "Uni-M fragment[$index] title is blank" }

        val mode = try {
            UniMTaskMode.valueOf(fragment.mode.uppercase())
        } catch (_: Exception) {
            throw IllegalArgumentException("Uni-M fragment[$index] mode is invalid")
        }
        val anchorKind = try {
            UniMAnchorKind.valueOf(fragment.anchorKind.uppercase())
        } catch (_: Exception) {
            throw IllegalArgumentException("Uni-M fragment[$index] anchorKind is invalid")
        }
        val urgency = normalizeUrgency(fragment.urgency)
        require(fragment.durationMinutes >= 0) { "Uni-M fragment[$index] durationMinutes must be >= 0" }

        when (anchorKind) {
            UniMAnchorKind.ABSOLUTE -> {
                when (mode) {
                    UniMTaskMode.EXACT -> {
                        val startTimeIso = fragment.startTimeIso?.trim().orEmpty()
                        require(startTimeIso.isNotBlank()) { "Uni-M fragment[$index] exact absolute time is blank" }
                        require(parseStrictOffsetDateTime(startTimeIso) != null) {
                            "Uni-M fragment[$index] exact absolute time must be strict ISO-8601"
                        }
                    }
                    UniMTaskMode.VAGUE -> {
                        val anchorDateIso = fragment.anchorDateIso?.trim().orEmpty()
                        require(anchorDateIso.isNotBlank()) { "Uni-M fragment[$index] vague absolute anchor is blank" }
                        require(runCatching { LocalDate.parse(anchorDateIso) }.isSuccess) {
                            "Uni-M fragment[$index] vague absolute anchor must be yyyy-MM-dd"
                        }
                    }
                }
            }
            UniMAnchorKind.NOW_OFFSET,
            UniMAnchorKind.PREVIOUS_EXACT_OFFSET -> {
                val offsetMinutes = fragment.relativeOffsetMinutes
                require(offsetMinutes != null && offsetMinutes > 0) {
                    "Uni-M fragment[$index] time offset must be positive minutes"
                }
            }
            UniMAnchorKind.NOW_DAY_OFFSET,
            UniMAnchorKind.PREVIOUS_DAY_OFFSET -> {
                val relativeDayOffset = fragment.relativeDayOffset
                require(relativeDayOffset != null && relativeDayOffset >= 0) {
                    "Uni-M fragment[$index] day offset must be >= 0"
                }
                if (mode == UniMTaskMode.EXACT) {
                    val clockTime = fragment.clockTime?.trim().orEmpty()
                    require(clockTime.isNotBlank()) { "Uni-M fragment[$index] exact day-offset clockTime is blank" }
                    require(ExactTimeCueResolver.parseClockTime(clockTime) != null) {
                        "Uni-M fragment[$index] exact day-offset clockTime must be HH:mm"
                    }
                }
            }
        }

        return UniMTaskFragment(
            title = title,
            mode = mode,
            anchorKind = anchorKind,
            urgency = urgency,
            startTimeIso = fragment.startTimeIso?.trim()?.takeIf { it.isNotBlank() },
            anchorDateIso = fragment.anchorDateIso?.trim()?.takeIf { it.isNotBlank() },
            clockTime = fragment.clockTime?.trim()?.takeIf { it.isNotBlank() },
            timeHint = fragment.timeHint?.trim()?.takeIf { it.isNotBlank() },
            durationMinutes = fragment.durationMinutes,
            relativeOffsetMinutes = fragment.relativeOffsetMinutes,
            relativeDayOffset = fragment.relativeDayOffset
        )
    }

    private fun normalizeRelativeDayStartTime(
        transcript: String?,
        startTimeIso: String,
        nowIso: String?,
        timezone: String?,
        displayedDateIso: String?
    ): String? {
        return ExactTimeCueResolver.normalizeRelativeDayStartTime(
            transcript = transcript,
            startTimeIso = startTimeIso,
            nowIso = nowIso,
            timezone = timezone,
            displayedDateIso = displayedDateIso
        )
    }

    private fun normalizeRelativeDayAnchorDate(
        transcript: String?,
        anchorDateIso: String,
        nowIso: String?,
        timezone: String?,
        displayedDateIso: String?
    ): String? {
        return ExactTimeCueResolver.normalizeRelativeDayAnchorDate(
            transcript = transcript,
            anchorDateIso = anchorDateIso,
            nowIso = nowIso,
            timezone = timezone,
            displayedDateIso = displayedDateIso
        )
    }

    private fun buildExactStartTimeFromExplicitCue(
        transcript: String?,
        anchorDateIso: String,
        timeHint: String?,
        timezone: String?
    ): String? {
        return ExactTimeCueResolver.buildExactStartTimeFromExplicitCue(
            transcript = transcript,
            anchorDateIso = anchorDateIso,
            timeHint = timeHint,
            timezone = timezone
        )
    }

    /**
     * 解析 Uni-C 灵感提取结果。
     * 说明：必须和 PromptCompiler 使用同一个 @Serializable contract。
     */
    fun parseUniCExtraction(input: String, unifiedId: String): FastTrackResult {
        return try {
            val payload = jsonInterpreter.decodeFromString<UniCExtractionPayload>(cleanJson(input))
            when (payload.decision.uppercase()) {
                "INSPIRATION_CREATE" -> {
                    val idea = payload.idea ?: return FastTrackResult.NoMatch("Uni-C inspiration result missing idea payload")
                    val content = idea.content.trim()
                    if (content.isBlank()) {
                        return FastTrackResult.NoMatch("Uni-C inspiration content is blank")
                    }

                    return FastTrackResult.CreateInspiration(
                        params = CreateInspirationParams(
                            unifiedId = unifiedId,
                            content = content
                        )
                    )
                }
                "NOT_INSPIRATION" -> {
                    FastTrackResult.NoMatch(payload.reason ?: "Uni-C decided input is not inspiration-create")
                }
                else -> FastTrackResult.NoMatch("Unknown Uni-C decision: ${payload.decision}")
            }
        } catch (e: SerializationException) {
            FastTrackResult.NoMatch("Uni-C JSON 结构异常: ${e.message}")
        } catch (e: Exception) {
            FastTrackResult.NoMatch("Uni-C 解析失败: ${e.message}")
        }
    }
    
    private fun parseTaskDefinition(taskMutation: TaskMutation): TaskDefinition? {
        val title = taskMutation.title
        if (title.isBlank()) return null
        
        // Path A requires an explicit ISO string. If it's missing or blank, we drop the task.
        // It will be caught downstream by Path B (which handles vague temporalities).
        if (taskMutation.startTime.isBlank()) return null
        
        // Ensure standard ISO-8601 formatting or standard UI format before pushing to DTO
        val normalizedStartTime = normalizeTime(taskMutation.startTime)

        val explicitDuration = taskMutation.duration
        val durationMinutes: Int = when {
            !explicitDuration.isNullOrBlank() -> parseDuration(explicitDuration) ?: 0
            else -> 0  // fire-off 提醒没有时长
        }
        
        val urgencyLevel = normalizeUrgency(taskMutation.urgency)
        
        return TaskDefinition(
            title = title,
            startTimeIso = normalizedStartTime,
            durationMinutes = durationMinutes,
            urgency = urgencyLevel
        )
    }

    private fun normalizeUrgency(raw: String): UrgencyEnum {
        return try {
            UrgencyEnum.valueOf(raw.uppercase())
        } catch (e: IllegalArgumentException) {
            when {
                raw.uppercase().startsWith("L1") -> UrgencyEnum.L1_CRITICAL
                raw.uppercase().startsWith("L2") -> UrgencyEnum.L2_IMPORTANT
                raw.uppercase() == "FIRE_OFF" -> UrgencyEnum.FIRE_OFF
                else -> UrgencyEnum.L3_NORMAL
            }
        }
    }

    private fun cleanJson(input: String): String {
        return input
            .replace("```json", "")
            .replace("```", "")
            .trim()
    }

    private fun parseDuration(durationStr: String): Int? {
        val lower = durationStr.lowercase().trim()
        return try {
            if (lower.endsWith("min") || lower.endsWith("m")) {
                lower.filter { it.isDigit() }.toInt()
            } else if (lower.endsWith("h") || lower.endsWith("hour")) {
                val num = lower.filter { it.isDigit() || it == '.' }.toFloat()
                (num * 60).toInt()
            } else if (lower.all { it.isDigit() }) {
                lower.toInt()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun normalizeTime(dateTimeStr: String): String {
        return dateTimeStr
            .replace(Regex("(\\d{4}-\\d{2}-\\d{2})(\\d{2}:\\d{2})"), "$1 $2")
            .trim()
    }

    // ════════════════════════════════════════════════════════════════
    // BACKWARD COMPAT: Legacy lint() method for L2DebugHud and tests
    // Will be removed when L2DebugHud migrates to parseFastTrackIntent()
    // ════════════════════════════════════════════════════════════════

    /**
     * @deprecated 使用 parseFastTrackIntent() 代替
     */
    fun lint(llmOutput: String): LintResult {
        return try {
            val mutation = jsonInterpreter.decodeFromString<UnifiedMutation>(llmOutput)
            
            when (mutation.classification) {
                "non_intent" -> {
                    return LintResult.NonIntent(
                        reason = mutation.reason ?: "未检测到日程安排意图"
                    )
                }
                "deletion" -> {
                    val targetTitle = mutation.targetTitle ?: ""
                    if (targetTitle.isBlank()) {
                        return LintResult.Error("未指定要删除的任务")
                    }
                    return LintResult.Deletion(targetTitle = targetTitle)
                }
                "reschedule" -> {
                    val targetTitle = mutation.targetTitle ?: ""
                    val newInstruction = mutation.newInstruction ?: ""
                    if (targetTitle.isBlank() || newInstruction.isBlank()) {
                        return LintResult.Error("改期请求缺少目标任务或新指令")
                    }
                    return LintResult.Reschedule(targetTitle = targetTitle, newInstruction = newInstruction)
                }
            }
            
            if (mutation.recommendedWorkflows.isNotEmpty()) {
                val recommendation = mutation.recommendedWorkflows.first()
                return LintResult.ToolDispatch(recommendation.workflowId, recommendation.parameters)
            }

            val profileMutations = mutation.profileMutations.map {
                ParsedProfileMutation(it.entityId, it.field, it.value)
            }

            val tasks = mutableListOf<ScheduledTask>()
            var singleSuccessResult: LintResult.Success? = null
            
            if (mutation.tasks.isNotEmpty()) {
                for (taskMutation in mutation.tasks) {
                    val result = parseSingleTaskLegacy(taskMutation)
                    if (result is LintResult.Success && result.task != null) {
                        tasks.add(result.task)
                        if (singleSuccessResult == null) singleSuccessResult = result
                    } else if (result is LintResult.Incomplete && tasks.isEmpty() && profileMutations.isEmpty()) {
                        return result
                    }
                }
            }
            
            return if (tasks.size > 1) {
                LintResult.MultiTask(tasks, profileMutations)
            } else if (tasks.size == 1 && singleSuccessResult != null) {
                singleSuccessResult.copy(profileMutations = profileMutations)
            } else if (profileMutations.isNotEmpty()) {
                LintResult.Success(
                    task = null,
                    profileMutations = profileMutations
                )
            } else {
                LintResult.Error("无法解析任务列表或记录更新")
            }
        } catch (e: Exception) {
            LintResult.Error("JSON 解析失败: ${e.message}")
        }
    }
    
    private fun parseSingleTaskLegacy(taskMutation: TaskMutation): LintResult {
        val title = taskMutation.title
        if (title.isBlank()) {
            return LintResult.Error("任务标题不能为空")
        }

        val partialClues = ParsedClues(
            person = taskMutation.keyPerson,
            company = taskMutation.keyCompany,
            location = taskMutation.location,
            briefSummary = if (taskMutation.notes.isNullOrBlank()) title else taskMutation.notes
        )
        
        val tp = legacyTimeProvider ?: return LintResult.Error("TimeProvider not available for legacy lint()")
        
        val startTime = parseDateTimeLegacy(taskMutation.startTime, tp)
        if (startTime == null) {
            return LintResult.Incomplete(
                missingField = "startTime",
                question = "请问具体是什么时候？",
                partialClues = partialClues
            )
        }
        
        val endTime: Instant? = taskMutation.endTime?.let { parseDateTimeLegacy(it, tp) }

        if (endTime != null && endTime.isBefore(startTime)) {
            return LintResult.Error("结束时间不能早于开始时间")
        }

        val todayStart = tp.today.atStartOfDay(tp.zoneId).toInstant()
        if (startTime.isBefore(todayStart)) {
            return LintResult.Error("不能创建过去的任务")
        }

        val explicitDuration = taskMutation.duration
        
        val durationMinutes: Int = when {
            !explicitDuration.isNullOrBlank() -> parseDuration(explicitDuration) ?: 0
            endTime != null -> ChronoUnit.MINUTES.between(startTime, endTime).toInt().coerceAtLeast(1)
            else -> 0
        }
        
        val urgencyStr = taskMutation.urgency
        val urgencyLevel = try {
            UrgencyLevel.valueOf(urgencyStr.uppercase())
        } catch (e: IllegalArgumentException) {
            when {
                urgencyStr.uppercase().startsWith("L1") -> UrgencyLevel.L1_CRITICAL
                urgencyStr.uppercase().startsWith("L2") -> UrgencyLevel.L2_IMPORTANT
                urgencyStr.uppercase() == "FIRE_OFF" -> UrgencyLevel.FIRE_OFF
                else -> UrgencyLevel.L3_NORMAL
            }
        }
        
        val policy = if (urgencyLevel == UrgencyLevel.FIRE_OFF) 
            ConflictPolicy.COEXISTING else ConflictPolicy.EXCLUSIVE

        val alarmCascade = UrgencyLevel.buildCascade(urgencyLevel)

        return LintResult.Success(
            task = ScheduledTask(
                id = "",
                timeDisplay = formatTimeDisplayLegacy(startTime, tp),
                title = title,
                isDone = false,
                hasAlarm = alarmCascade.isNotEmpty(),
                isSmartAlarm = urgencyLevel == UrgencyLevel.L1_CRITICAL || urgencyLevel == UrgencyLevel.L2_IMPORTANT,
                startTime = startTime,
                endTime = endTime,
                durationMinutes = durationMinutes,
                conflictPolicy = policy,
                dateRange = formatDateRangeLegacy(startTime, endTime, tp),
                location = taskMutation.location,
                notes = taskMutation.notes,
                keyPerson = taskMutation.keyPerson,
                highlights = taskMutation.highlights,
                alarmCascade = alarmCascade
            ),
            urgencyLevel = urgencyLevel,
            parsedClues = partialClues.copy(durationMinutes = durationMinutes)
        )
    }

    private fun parseDateTimeLegacy(dateTimeStr: String, tp: TimeProvider): Instant? {
        return try {
            val normalized = dateTimeStr
                .replace(Regex("(\\d{4}-\\d{2}-\\d{2})(\\d{2}:\\d{2})"), "$1 $2")
                .trim()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            java.time.LocalDateTime.parse(normalized, formatter)
                .atZone(tp.zoneId)
                .toInstant()
        } catch (e: Exception) {
            null
        }
    }

    private fun formatTimeDisplayLegacy(start: Instant, tp: TimeProvider): String {
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        return start.atZone(tp.zoneId).format(formatter)
    }

    private fun formatDateRangeLegacy(start: Instant, end: Instant?, tp: TimeProvider): String {
        val dateParams = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val timeParams = DateTimeFormatter.ofPattern("HH:mm")
        val startZone = start.atZone(tp.zoneId)
        if (end == null) {
            return "${startZone.format(dateParams)} ${startZone.format(timeParams)} - ..."
        }
        val endZone = end.atZone(tp.zoneId)
        return if (startZone.toLocalDate() == endZone.toLocalDate()) {
            "${startZone.format(timeParams)} - ${endZone.format(timeParams)}"
        } else {
            val fullFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            "${startZone.format(fullFormatter)} ~ ${endZone.format(fullFormatter)}"
        }
    }
}

// ════════════════════════════════════════════════════════════════
// BACKWARD COMPAT: Legacy data classes for L2DebugHud and tests
// ════════════════════════════════════════════════════════════════

data class ParsedClues(
    val person: String? = null,
    val company: String? = null,
    val location: String? = null,
    val briefSummary: String? = null,
    val durationMinutes: Int? = null
)

data class ParsedProfileMutation(
    val entityId: String,
    val field: String,
    val value: String
)

sealed class LintResult {
    data class Success(
        val task: ScheduledTask? = null,
        val urgencyLevel: UrgencyLevel = UrgencyLevel.L3_NORMAL,
        val parsedClues: ParsedClues = ParsedClues(),
        val profileMutations: List<ParsedProfileMutation> = emptyList()
    ) : LintResult()
    
    data class MultiTask(
        val tasks: List<ScheduledTask>,
        val profileMutations: List<ParsedProfileMutation> = emptyList()
    ) : LintResult()
    
    data class Incomplete(
        val missingField: String,
        val question: String,
        val partialClues: ParsedClues
    ) : LintResult()

    data class Error(val message: String) : LintResult()
    
    data class NonIntent(val reason: String) : LintResult()

    data class Deletion(val targetTitle: String) : LintResult()
    
    data class Reschedule(val targetTitle: String, val newInstruction: String) : LintResult()
    
    data class ToolDispatch(val workflowId: String, val params: Map<String, String>) : LintResult()
}
