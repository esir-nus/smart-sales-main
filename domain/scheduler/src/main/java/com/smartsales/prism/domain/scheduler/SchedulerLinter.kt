package com.smartsales.prism.domain.scheduler

import com.smartsales.prism.domain.core.UnifiedMutation
import com.smartsales.prism.domain.core.TaskMutation
import com.smartsales.prism.domain.memory.ConflictPolicy
import com.smartsales.prism.domain.time.TimeProvider
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 调度器校验器 — 验证 LLM 输出的 JSON 结构和日期合理性 (Project Mono Wave 2)
 */
@Singleton
class SchedulerLinter @Inject constructor(
    private val timeProvider: TimeProvider
) {
    private val jsonInterpreter = Json { ignoreUnknownKeys = true }

    /**
     * 验证并解析 LLM 输出
     * @return LintResult.Success 包含解析后的任务，或 LintResult.Error 包含错误信息
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
            
            // Parse Profile Mutations
            val profileMutations = mutation.profileMutations.map {
                ParsedProfileMutation(it.entityId, it.field, it.value)
            }

            // Parse Tasks
            val tasks = mutableListOf<TimelineItemModel.Task>()
            var singleSuccessResult: LintResult.Success? = null
            
            if (mutation.tasks.isNotEmpty()) {
                for (taskMutation in mutation.tasks) {
                    val result = parseSingleTask(taskMutation)
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
    
    /**
     * 解析单个任务对象 (Project Mono Wave 2: Using TaskMutation)
     */
    private fun parseSingleTask(taskMutation: TaskMutation): LintResult {
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
        
        // 验证日期 — 如果缺失，返回 Incomplete 而不是 Error（Phase 1 循环）
        val startTime = parseDateTime(taskMutation.startTime)
        if (startTime == null) {
            return LintResult.Incomplete(
                missingField = "startTime",
                question = "请问具体是什么时候？",
                partialClues = partialClues
            )
        }
        
        // endTime 可选，null 表示开放式任务
        val endTime: Instant? = taskMutation.endTime?.let { parseDateTime(it) }

        if (endTime != null && endTime.isBefore(startTime)) {
            return LintResult.Error("结束时间不能早于开始时间")
        }

        // 检查是否是过去的时间 (允许今天的任务)
        val todayStart = timeProvider.today.atStartOfDay(timeProvider.zoneId).toInstant()
        if (startTime.isBefore(todayStart)) {
            return LintResult.Error("不能创建过去的任务")
        }

        // Duration: LLM 推断为主，无需确认
        val explicitDuration = taskMutation.duration
        
        val durationMinutes: Int = when {
            !explicitDuration.isNullOrBlank() -> parseDuration(explicitDuration) ?: 0
            endTime != null -> ChronoUnit.MINUTES.between(startTime, endTime).toInt().coerceAtLeast(1)
            else -> 0  // fire-off 提醒没有时长
        }
        
        // UrgencyLevel 解析
        val urgencyStr = taskMutation.urgency
        val urgencyLevel = try {
            UrgencyLevel.valueOf(urgencyStr.uppercase())
        } catch (e: IllegalArgumentException) {
            when {
                urgencyStr.uppercase().startsWith("L1") -> UrgencyLevel.L1_CRITICAL
                urgencyStr.uppercase().startsWith("L2") -> UrgencyLevel.L2_IMPORTANT
                urgencyStr.uppercase() == "FIRE_OFF" -> UrgencyLevel.FIRE_OFF
                else -> UrgencyLevel.L3_NORMAL // 安全默认
            }
        }
        
        // 冲突策略: 根据紧急程度决定 (FIRE_OFF -> COEXISTING)
        val policy = if (urgencyLevel == UrgencyLevel.FIRE_OFF) 
            ConflictPolicy.COEXISTING else ConflictPolicy.EXCLUSIVE

        // 级联提醒: 由 UrgencyLevel 决定
        val alarmCascade = UrgencyLevel.buildCascade(urgencyLevel)

        return LintResult.Success(
            task = TimelineItemModel.Task(
                id = "", // 新任务，ID 由 Repository 生成
                timeDisplay = formatTimeDisplay(startTime, endTime),
                title = title,
                isDone = false,
                hasAlarm = alarmCascade.isNotEmpty(), // 有级联就有提醒
                isSmartAlarm = urgencyLevel == UrgencyLevel.L1_CRITICAL || urgencyLevel == UrgencyLevel.L2_IMPORTANT,
                startTime = startTime,
                endTime = endTime,
                durationMinutes = durationMinutes,
                conflictPolicy = policy,
                dateRange = formatDateRange(startTime, endTime),
                location = taskMutation.location,
                notes = taskMutation.notes,
                keyPerson = taskMutation.keyPerson,
                highlights = taskMutation.highlights,
                alarmCascade = alarmCascade
            ),
            urgencyLevel = urgencyLevel,
            // Phase 1 线索 → Phase 2 实体消歧
            parsedClues = partialClues.copy(durationMinutes = durationMinutes)
        )
    }

    private fun parseDateTime(dateTimeStr: String): Instant? {
        return try {
            // 规范化 LLM 输出格式 (处理缺失空格等问题)
            val normalized = dateTimeStr
                .replace(Regex("(\\d{4}-\\d{2}-\\d{2})(\\d{2}:\\d{2})"), "$1 $2")
                .trim()
            
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            java.time.LocalDateTime.parse(normalized, formatter)
                .atZone(timeProvider.zoneId)
                .toInstant()
        } catch (e: Exception) {
            null
        }
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

    private fun formatTimeDisplay(start: Instant, end: Instant?): String {
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val startStr = start.atZone(timeProvider.zoneId).format(formatter)
        return startStr
    }

    private fun formatDateRange(start: Instant, end: Instant?): String {
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
        val task: TimelineItemModel.Task? = null,
        val urgencyLevel: UrgencyLevel = UrgencyLevel.L3_NORMAL,
        val parsedClues: ParsedClues = ParsedClues(),
        val profileMutations: List<ParsedProfileMutation> = emptyList()
    ) : LintResult()
    
    data class MultiTask(
        val tasks: List<TimelineItemModel.Task>,
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
}
