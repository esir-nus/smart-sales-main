package com.smartsales.prism.domain.scheduler

import com.smartsales.prism.domain.core.TaskMutation
import com.smartsales.prism.domain.core.UnifiedMutation
import com.smartsales.prism.domain.memory.ConflictPolicy
import com.smartsales.prism.domain.time.TimeProvider
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlinx.serialization.json.Json

internal class SchedulerLinterLegacySupport(
    private val jsonInterpreter: Json,
    private val timeProviderResolver: () -> TimeProvider?
) {

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
                    } else if (result is LintResult.Incomplete &&
                        tasks.isEmpty() &&
                        profileMutations.isEmpty()
                    ) {
                        return result
                    }
                }
            }

            when {
                tasks.size > 1 -> LintResult.MultiTask(tasks, profileMutations)
                tasks.size == 1 && singleSuccessResult != null -> singleSuccessResult.copy(
                    profileMutations = profileMutations
                )
                profileMutations.isNotEmpty() -> LintResult.Success(
                    task = null,
                    profileMutations = profileMutations
                )
                else -> LintResult.Error("无法解析任务列表或记录更新")
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

        val timeProvider = timeProviderResolver()
            ?: return LintResult.Error("TimeProvider not available for legacy lint()")

        val startTime = parseDateTimeLegacy(taskMutation.startTime, timeProvider)
        if (startTime == null) {
            return LintResult.Incomplete(
                missingField = "startTime",
                question = "请问具体是什么时候？",
                partialClues = partialClues
            )
        }

        val endTime = taskMutation.endTime?.let { parseDateTimeLegacy(it, timeProvider) }
        if (endTime != null && endTime.isBefore(startTime)) {
            return LintResult.Error("结束时间不能早于开始时间")
        }

        val todayStart = timeProvider.today.atStartOfDay(timeProvider.zoneId).toInstant()
        if (startTime.isBefore(todayStart)) {
            return LintResult.Error("不能创建过去的任务")
        }

        val explicitDuration = taskMutation.duration
        val durationMinutes = when {
            !explicitDuration.isNullOrBlank() -> schedulerLinterParseDuration(explicitDuration) ?: 0
            endTime != null -> ChronoUnit.MINUTES.between(startTime, endTime).toInt().coerceAtLeast(1)
            else -> 0
        }

        val urgencyLevel = schedulerLinterNormalizeLegacyUrgency(taskMutation.urgency)
        val policy = if (urgencyLevel == UrgencyLevel.FIRE_OFF) {
            ConflictPolicy.COEXISTING
        } else {
            ConflictPolicy.EXCLUSIVE
        }
        val alarmCascade = UrgencyLevel.buildCascade(urgencyLevel)

        return LintResult.Success(
            task = ScheduledTask(
                id = "",
                timeDisplay = formatTimeDisplayLegacy(startTime, timeProvider),
                title = title,
                isDone = false,
                hasAlarm = alarmCascade.isNotEmpty(),
                isSmartAlarm = urgencyLevel == UrgencyLevel.L1_CRITICAL ||
                    urgencyLevel == UrgencyLevel.L2_IMPORTANT,
                startTime = startTime,
                endTime = endTime,
                durationMinutes = durationMinutes,
                conflictPolicy = policy,
                dateRange = formatDateRangeLegacy(startTime, endTime, timeProvider),
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

    private fun parseDateTimeLegacy(dateTimeStr: String, timeProvider: TimeProvider): Instant? {
        return try {
            val normalized = schedulerLinterNormalizeTime(dateTimeStr)
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            LocalDateTime.parse(normalized, formatter)
                .atZone(timeProvider.zoneId)
                .toInstant()
        } catch (_: Exception) {
            null
        }
    }

    private fun formatTimeDisplayLegacy(start: Instant, timeProvider: TimeProvider): String {
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        return start.atZone(timeProvider.zoneId).format(formatter)
    }

    private fun formatDateRangeLegacy(
        start: Instant,
        end: Instant?,
        timeProvider: TimeProvider
    ): String {
        val dateParams = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val timeParams = DateTimeFormatter.ofPattern("HH:mm")
        val startZone = start.atZone(timeProvider.zoneId)
        if (end == null) {
            return "${startZone.format(dateParams)} ${startZone.format(timeParams)} - ..."
        }
        val endZone = end.atZone(timeProvider.zoneId)
        return if (startZone.toLocalDate() == endZone.toLocalDate()) {
            "${startZone.format(timeParams)} - ${endZone.format(timeParams)}"
        } else {
            val fullFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            "${startZone.format(fullFormatter)} ~ ${endZone.format(fullFormatter)}"
        }
    }
}
