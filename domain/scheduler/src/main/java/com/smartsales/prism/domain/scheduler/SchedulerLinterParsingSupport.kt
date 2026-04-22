package com.smartsales.prism.domain.scheduler

import com.smartsales.prism.domain.core.UnifiedMutation
import com.smartsales.prism.domain.memory.TargetResolutionRequest
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

internal class SchedulerLinterParsingSupport(
    private val jsonInterpreter: Json
) {

    fun parseFastTrackIntent(input: String): FastTrackResult {
        return try {
            val mutation = jsonInterpreter.decodeFromString<UnifiedMutation>(input)
            when (mutation.classification.lowercase()) {
                "non_intent", "none" -> FastTrackResult.NoMatch(
                    reason = mutation.reason ?: "未检测到日程安排意图"
                )

                "deletion" -> FastTrackResult.NoMatch(
                    reason = "语音删除操作已被系统限制，请在面板手动操作。"
                )

                "inspiration" -> {
                    val content = mutation.tasks.firstOrNull()?.notes
                        ?: mutation.response
                        ?: mutation.thought
                        ?: return FastTrackResult.NoMatch("未检测到灵感内容")
                    FastTrackResult.CreateInspiration(
                        params = CreateInspirationParams(content = content)
                    )
                }

                "reschedule" -> {
                    val targetTitle = mutation.targetTitle ?: ""
                    val newInstruction = mutation.newInstruction ?: ""
                    if (targetTitle.isBlank() || newInstruction.isBlank()) {
                        return FastTrackResult.NoMatch("改期请求缺少目标任务或新指令")
                    }
                    FastTrackResult.RescheduleTask(
                        params = RescheduleTaskParams(
                            targetQuery = targetTitle,
                            newStartTimeIso = newInstruction
                        )
                    )
                }

                "schedulable" -> {
                    if (mutation.tasks.isEmpty()) {
                        return FastTrackResult.NoMatch("无可用日程")
                    }
                    val definitions = mutation.tasks.mapNotNull(::schedulerLinterParseTaskDefinition)
                    if (definitions.isEmpty()) {
                        return FastTrackResult.NoMatch("时间格式无法解析为精准日程")
                    }
                    FastTrackResult.CreateTasks(
                        params = CreateTasksParams(tasks = definitions)
                    )
                }

                else -> FastTrackResult.NoMatch("未知的操作意图: ${mutation.classification}")
            }
        } catch (e: SerializationException) {
            FastTrackResult.NoMatch("JSON 结构异常: ${e.message}")
        } catch (e: Exception) {
            FastTrackResult.NoMatch("无法解析该意图: ${e.message}")
        }
    }

    fun parseUniAExtraction(
        input: String,
        unifiedId: String,
        transcript: String? = null,
        nowIso: String? = null,
        timezone: String? = null,
        displayedDateIso: String? = null
    ): FastTrackResult {
        return try {
            val payload = jsonInterpreter.decodeFromString<UniAExtractionPayload>(schedulerLinterCleanJson(input))
            when (payload.decision.uppercase()) {
                "EXACT_CREATE" -> {
                    val task = payload.task
                        ?: return FastTrackResult.NoMatch("Uni-A exact result missing task payload")
                    val title = task.title.trim()
                    if (title.isBlank()) {
                        return FastTrackResult.NoMatch("Uni-A exact task title is blank")
                    }

                    val startTimeIso = task.startTimeIso.trim()
                    if (startTimeIso.isBlank()) {
                        return FastTrackResult.NoMatch("Uni-A exact task time is blank")
                    }
                    val normalizedStartTimeIso = ExactTimeCueResolver.normalizeRelativeDayStartTime(
                        transcript = transcript,
                        startTimeIso = startTimeIso,
                        nowIso = nowIso,
                        timezone = timezone,
                        displayedDateIso = displayedDateIso
                    ) ?: return FastTrackResult.NoMatch(
                        "Uni-A exact date anchor requires displayed page context"
                    )
                    if (schedulerLinterParseStrictOffsetDateTime(normalizedStartTimeIso) == null) {
                        return FastTrackResult.NoMatch("Uni-A exact task time must be strict ISO-8601")
                    }

                    val antiFabricationReason = rejectFabricatedExactTime(
                        transcript = transcript,
                        startTimeIso = normalizedStartTimeIso
                    )
                    if (antiFabricationReason != null) {
                        return FastTrackResult.NoMatch(antiFabricationReason)
                    }

                    FastTrackResult.CreateTasks(
                        params = CreateTasksParams(
                            unifiedId = unifiedId,
                            tasks = listOf(
                                TaskDefinition(
                                    title = title,
                                    startTimeIso = normalizedStartTimeIso,
                                    durationMinutes = task.durationMinutes.coerceAtLeast(0),
                                    keyPerson = task.keyPerson?.trim()?.takeIf { it.isNotBlank() },
                                    location = task.location?.trim()?.takeIf { it.isNotBlank() },
                                    urgency = schedulerLinterNormalizeUrgency(task.urgency)
                                )
                            )
                        )
                    )
                }

                "NOT_EXACT" -> FastTrackResult.NoMatch(
                    payload.reason ?: "Uni-A decided input is not exact"
                )

                else -> FastTrackResult.NoMatch("Unknown Uni-A decision: ${payload.decision}")
            }
        } catch (e: SerializationException) {
            FastTrackResult.NoMatch("Uni-A JSON 结构异常: ${e.message}")
        } catch (e: Exception) {
            FastTrackResult.NoMatch("Uni-A 解析失败: ${e.message}")
        }
    }

    fun parseUniBExtraction(input: String, unifiedId: String): FastTrackResult {
        return try {
            val payload = jsonInterpreter.decodeFromString<UniBExtractionPayload>(schedulerLinterCleanJson(input))
            when (payload.decision.uppercase()) {
                "VAGUE_CREATE" -> {
                    val task = payload.task
                        ?: return FastTrackResult.NoMatch("Uni-B vague result missing task payload")
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

                    FastTrackResult.CreateVagueTask(
                        params = CreateVagueTaskParams(
                            unifiedId = unifiedId,
                            title = title,
                            anchorDateIso = anchorDateIso,
                            timeHint = task.timeHint?.trim()?.takeIf { it.isNotBlank() },
                            keyPerson = task.keyPerson?.trim()?.takeIf { it.isNotBlank() },
                            location = task.location?.trim()?.takeIf { it.isNotBlank() },
                            urgency = schedulerLinterNormalizeUrgency(task.urgency)
                        )
                    )
                }

                "NOT_VAGUE" -> FastTrackResult.NoMatch(
                    payload.reason ?: "Uni-B decided input is not vague-create"
                )

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
            val payload = jsonInterpreter.decodeFromString<UniBExtractionPayload>(schedulerLinterCleanJson(input))
            when (payload.decision.uppercase()) {
                "VAGUE_CREATE" -> {
                    val task = payload.task
                        ?: return FastTrackResult.NoMatch("Uni-B vague result missing task payload")
                    val title = task.title.trim()
                    if (title.isBlank()) {
                        return FastTrackResult.NoMatch("Uni-B vague task title is blank")
                    }

                    val rawAnchorDateIso = task.anchorDateIso.trim()
                    if (rawAnchorDateIso.isBlank()) {
                        return FastTrackResult.NoMatch("Uni-B vague task anchor date is blank")
                    }
                    val normalizedAnchorDateIso = ExactTimeCueResolver.normalizeRelativeDayAnchorDate(
                        transcript = transcript,
                        anchorDateIso = rawAnchorDateIso,
                        nowIso = nowIso,
                        timezone = timezone,
                        displayedDateIso = displayedDateIso
                    ) ?: return FastTrackResult.NoMatch(
                        "Uni-B page-relative anchor requires displayed page context"
                    )
                    try {
                        LocalDate.parse(normalizedAnchorDateIso)
                    } catch (_: Exception) {
                        return FastTrackResult.NoMatch("Uni-B anchor date must be yyyy-MM-dd")
                    }
                    val normalizedTimeHint = task.timeHint?.trim()?.takeIf { it.isNotBlank() }
                    val exactStartTimeIso = ExactTimeCueResolver.buildExactStartTimeFromExplicitCue(
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
                                        keyPerson = task.keyPerson?.trim()?.takeIf { it.isNotBlank() },
                                        location = task.location?.trim()?.takeIf { it.isNotBlank() },
                                        urgency = schedulerLinterNormalizeUrgency(task.urgency)
                                    )
                                )
                            )
                        )
                    }

                    FastTrackResult.CreateVagueTask(
                        params = CreateVagueTaskParams(
                            unifiedId = unifiedId,
                            title = title,
                            anchorDateIso = normalizedAnchorDateIso,
                            timeHint = normalizedTimeHint,
                            keyPerson = task.keyPerson?.trim()?.takeIf { it.isNotBlank() },
                            location = task.location?.trim()?.takeIf { it.isNotBlank() },
                            urgency = schedulerLinterNormalizeUrgency(task.urgency)
                        )
                    )
                }

                "NOT_VAGUE" -> FastTrackResult.NoMatch(
                    payload.reason ?: "Uni-B decided input is not vague-create"
                )

                else -> FastTrackResult.NoMatch("Unknown Uni-B decision: ${payload.decision}")
            }
        } catch (e: SerializationException) {
            FastTrackResult.NoMatch("Uni-B JSON 结构异常: ${e.message}")
        } catch (e: Exception) {
            FastTrackResult.NoMatch("Uni-B 解析失败: ${e.message}")
        }
    }

    fun parseUniMExtraction(input: String): UniMExtractionResult {
        return try {
            val payload = jsonInterpreter.decodeFromString<UniMExtractionPayload>(schedulerLinterCleanJson(input))
            when (payload.decision.uppercase()) {
                "MULTI_CREATE" -> {
                    if (payload.fragments.isEmpty()) {
                        return UniMExtractionResult.NotMulti("Uni-M multi-create result missing fragments")
                    }
                    if (payload.fragments.size > 4) {
                        return UniMExtractionResult.NotMulti("Uni-M fragment count exceeds limit")
                    }

                    val fragments = payload.fragments.mapIndexed(::parseUniMFragment)
                    UniMExtractionResult.MultiCreate(fragments)
                }

                "NOT_MULTI" -> UniMExtractionResult.NotMulti(
                    payload.reason ?: "Uni-M decided input is not multi-create"
                )

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

    fun parseFollowUpRescheduleExtraction(
        input: String,
        transcript: String
    ): FollowUpRescheduleExtractionResult {
        return try {
            val payload = jsonInterpreter.decodeFromString<FollowUpRescheduleExtractionPayload>(schedulerLinterCleanJson(input))
            when (payload.decision.uppercase()) {
                "NOT_SUPPORTED" -> FollowUpRescheduleExtractionResult.Unsupported(
                    payload.reason ?: "Follow-up reschedule V2 decided input is unsupported"
                )

                "RESCHEDULE_EXACT" -> {
                    val timeKind = try {
                        FollowUpRescheduleTimeKind.valueOf(payload.timeKind?.uppercase().orEmpty())
                    } catch (_: Exception) {
                        return FollowUpRescheduleExtractionResult.Invalid(
                            "Follow-up reschedule V2 timeKind is invalid"
                        )
                    }

                    when (timeKind) {
                        FollowUpRescheduleTimeKind.DELTA_FROM_TARGET -> {
                            if (payload.relativeDayOffset != null ||
                                payload.clockTime != null ||
                                payload.absoluteStartIso != null
                            ) {
                                return FollowUpRescheduleExtractionResult.Invalid(
                                    "Follow-up reschedule V2 delta payload contains illegal extra fields"
                                )
                            }
                            val deltaMinutes = payload.deltaFromTargetMinutes
                                ?: return FollowUpRescheduleExtractionResult.Invalid(
                                    "Follow-up reschedule V2 deltaFromTargetMinutes missing"
                                )
                            if (deltaMinutes == 0) {
                                return FollowUpRescheduleExtractionResult.Invalid(
                                    "Follow-up reschedule V2 deltaFromTargetMinutes must not be 0"
                                )
                            }
                            FollowUpRescheduleExtractionResult.Supported(
                                timeKind = timeKind,
                                operand = FollowUpRescheduleOperand.DeltaFromTarget(deltaMinutes)
                            )
                        }

                        FollowUpRescheduleTimeKind.RELATIVE_DAY_CLOCK -> {
                            if (schedulerLinterContainsPageRelativeDayCue(transcript)) {
                                return FollowUpRescheduleExtractionResult.Unsupported(
                                    "Follow-up reschedule V2 does not allow page-relative day anchors"
                                )
                            }
                            if (payload.deltaFromTargetMinutes != null || payload.absoluteStartIso != null) {
                                return FollowUpRescheduleExtractionResult.Invalid(
                                    "Follow-up reschedule V2 relative day payload contains illegal extra fields"
                                )
                            }
                            val dayOffset = payload.relativeDayOffset
                                ?: return FollowUpRescheduleExtractionResult.Invalid(
                                    "Follow-up reschedule V2 relative day offset missing"
                                )
                            if (dayOffset < 0) {
                                return FollowUpRescheduleExtractionResult.Invalid(
                                    "Follow-up reschedule V2 relative day offset must be >= 0"
                                )
                            }
                            val clockTime = payload.clockTime?.trim().orEmpty()
                            if (clockTime.isBlank() || ExactTimeCueResolver.parseClockTime(clockTime) == null) {
                                return FollowUpRescheduleExtractionResult.Invalid(
                                    "Follow-up reschedule V2 clockTime must be HH:mm"
                                )
                            }
                            FollowUpRescheduleExtractionResult.Supported(
                                timeKind = timeKind,
                                operand = FollowUpRescheduleOperand.RelativeDayClock(
                                    dayOffset = dayOffset,
                                    clockTime = clockTime
                                )
                            )
                        }

                        FollowUpRescheduleTimeKind.ABSOLUTE -> {
                            if (payload.deltaFromTargetMinutes != null ||
                                payload.relativeDayOffset != null ||
                                payload.clockTime != null
                            ) {
                                return FollowUpRescheduleExtractionResult.Invalid(
                                    "Follow-up reschedule V2 absolute payload contains illegal extra fields"
                                )
                            }
                            val startIso = payload.absoluteStartIso?.trim().orEmpty()
                            if (startIso.isBlank() || schedulerLinterParseStrictOffsetDateTime(startIso) == null) {
                                return FollowUpRescheduleExtractionResult.Invalid(
                                    "Follow-up reschedule V2 absoluteStartIso must be strict ISO-8601"
                                )
                            }
                            FollowUpRescheduleExtractionResult.Supported(
                                timeKind = timeKind,
                                operand = FollowUpRescheduleOperand.AbsoluteStart(startIso)
                            )
                        }
                    }
                }

                else -> FollowUpRescheduleExtractionResult.Invalid(
                    "Unknown follow-up reschedule V2 decision: ${payload.decision}"
                )
            }
        } catch (e: SerializationException) {
            FollowUpRescheduleExtractionResult.Invalid(
                "Follow-up reschedule V2 JSON 结构异常: ${e.message}"
            )
        } catch (e: Exception) {
            FollowUpRescheduleExtractionResult.Invalid(
                "Follow-up reschedule V2 解析失败: ${e.message}"
            )
        }
    }

    fun parseGlobalRescheduleExtraction(input: String): GlobalRescheduleExtractionResult {
        return try {
            val payload = jsonInterpreter.decodeFromString<GlobalRescheduleExtractionPayload>(
                schedulerLinterCleanJson(input)
            )
            when (payload.decision.uppercase()) {
                "NOT_SUPPORTED" -> GlobalRescheduleExtractionResult.Unsupported(
                    payload.reason ?: "Global reschedule extractor decided input is unsupported"
                )

                "RESCHEDULE_TARGETED" -> {
                    val timeInstruction = payload.timeInstruction?.trim().orEmpty()
                    if (timeInstruction.isBlank()) {
                        return GlobalRescheduleExtractionResult.Invalid(
                            "Global reschedule extraction timeInstruction is blank"
                        )
                    }

                    val targetQuery = payload.targetQuery?.trim().orEmpty()
                    val targetPerson = payload.targetPerson?.trim()?.takeIf { it.isNotBlank() }
                    val targetLocation = payload.targetLocation?.trim()?.takeIf { it.isNotBlank() }
                    if (targetQuery.isBlank() && targetPerson == null && targetLocation == null) {
                        return GlobalRescheduleExtractionResult.Invalid(
                            "Global reschedule extraction target clues are blank"
                        )
                    }

                    GlobalRescheduleExtractionResult.Supported(
                        target = TargetResolutionRequest(
                            targetQuery = targetQuery,
                            targetPerson = targetPerson,
                            targetLocation = targetLocation
                        ),
                        timeInstruction = timeInstruction,
                        suggestedTaskId = payload.suggestedTaskId?.trim()?.takeIf { it.isNotBlank() },
                        preferredTaskIds = payload.preferredTaskIds
                    )
                }

                else -> GlobalRescheduleExtractionResult.Invalid(
                    "Unknown global reschedule extraction decision: ${payload.decision}"
                )
            }
        } catch (e: SerializationException) {
            GlobalRescheduleExtractionResult.Invalid(
                "Global reschedule extraction JSON 结构异常: ${e.message}"
            )
        } catch (e: Exception) {
            GlobalRescheduleExtractionResult.Invalid(
                "Global reschedule extraction 解析失败: ${e.message}"
            )
        }
    }

    fun parseUniCExtraction(
        input: String,
        unifiedId: String,
        transcript: String? = null
    ): FastTrackResult {
        return try {
            val payload = jsonInterpreter.decodeFromString<UniCExtractionPayload>(schedulerLinterCleanJson(input))
            when (payload.decision.uppercase()) {
                "INSPIRATION_CREATE" -> {
                    val idea = payload.idea
                        ?: return FastTrackResult.NoMatch("Uni-C inspiration result missing idea payload")
                    val content = resolveUniCInspirationContent(
                        content = idea.content,
                        title = idea.title,
                        transcript = transcript
                    )
                    if (content.isBlank()) {
                        return FastTrackResult.NoMatch("Uni-C inspiration content is blank")
                    }
                    FastTrackResult.CreateInspiration(
                        params = CreateInspirationParams(
                            unifiedId = unifiedId,
                            content = content
                        )
                    )
                }

                "NOT_INSPIRATION" -> FastTrackResult.NoMatch(
                    payload.reason ?: "Uni-C decided input is not inspiration-create"
                )

                else -> FastTrackResult.NoMatch("Unknown Uni-C decision: ${payload.decision}")
            }
        } catch (e: SerializationException) {
            FastTrackResult.NoMatch("Uni-C JSON 结构异常: ${e.message}")
        } catch (e: Exception) {
            FastTrackResult.NoMatch("Uni-C 解析失败: ${e.message}")
        }
    }

    private fun resolveUniCInspirationContent(
        content: String,
        title: String?,
        transcript: String?
    ): String {
        return listOf(content, title, transcript)
            .firstOrNull { !it.isNullOrBlank() }
            ?.trim()
            .orEmpty()
    }

    private fun rejectFabricatedExactTime(
        transcript: String?,
        startTimeIso: String
    ): String? {
        val normalized = transcript?.lowercase()?.trim() ?: return null
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

        val parsedDateTime = schedulerLinterParseStrictOffsetDateTime(startTimeIso) ?: return null
        return "Uni-A exact time rejected: date-only input must not fabricate exact clock time (${parsedDateTime.truncatedTo(ChronoUnit.SECONDS)})"
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
        val urgency = schedulerLinterNormalizeUrgency(fragment.urgency)
        require(fragment.durationMinutes >= 0) { "Uni-M fragment[$index] durationMinutes must be >= 0" }

        when (anchorKind) {
            UniMAnchorKind.ABSOLUTE -> {
                when (mode) {
                    UniMTaskMode.EXACT -> {
                        val startTimeIso = fragment.startTimeIso?.trim().orEmpty()
                        require(startTimeIso.isNotBlank()) {
                            "Uni-M fragment[$index] exact absolute time is blank"
                        }
                        require(schedulerLinterParseStrictOffsetDateTime(startTimeIso) != null) {
                            "Uni-M fragment[$index] exact absolute time must be strict ISO-8601"
                        }
                    }

                    UniMTaskMode.VAGUE -> {
                        val anchorDateIso = fragment.anchorDateIso?.trim().orEmpty()
                        require(anchorDateIso.isNotBlank()) {
                            "Uni-M fragment[$index] vague absolute anchor is blank"
                        }
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
                    require(clockTime.isNotBlank()) {
                        "Uni-M fragment[$index] exact day-offset clockTime is blank"
                    }
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
            keyPerson = fragment.keyPerson?.trim()?.takeIf { it.isNotBlank() },
            location = fragment.location?.trim()?.takeIf { it.isNotBlank() },
            relativeOffsetMinutes = fragment.relativeOffsetMinutes,
            relativeDayOffset = fragment.relativeDayOffset
        )
    }
}
