package com.smartsales.prism.domain.scheduler

import com.smartsales.prism.domain.memory.ConflictPolicy
import com.smartsales.prism.domain.time.TimeProvider
import org.json.JSONObject
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 调度器校验器 — 验证 LLM 输出的 JSON 结构和日期合理性
 */
@Singleton
class SchedulerLinter @Inject constructor(
    private val timeProvider: TimeProvider
) {

    /**
     * 验证并解析 LLM 输出
     * @return LintResult.Success 包含解析后的任务，或 LintResult.Error 包含错误信息
     */
    fun lint(llmOutput: String): LintResult {
        return try {
            val json = JSONObject(llmOutput)
            
            // Wave 4.0: Check classification first
            val classification = json.optString("classification", "schedulable")
            
            when (classification) {
                "non_intent" -> {
                    return LintResult.NonIntent(
                        reason = json.optString("reason", "未检测到日程安排意图")
                    )
                }

                // Wave 7: NL Deletion
                "deletion" -> {
                    val targetTitle = json.optString("targetTitle", "")
                    if (targetTitle.isBlank()) {
                        return LintResult.Error("未指定要删除的任务")
                    }
                    return LintResult.Deletion(targetTitle = targetTitle)
                }
                // Wave 11: Global Reschedule
                "reschedule" -> {
                    val targetTitle = json.optString("targetTitle", "")
                    val newInstruction = json.optString("newInstruction", "")
                    if (targetTitle.isBlank() || newInstruction.isBlank()) {
                        return LintResult.Error("改期请求缺少目标任务或新指令")
                    }
                    return LintResult.Reschedule(targetTitle = targetTitle, newInstruction = newInstruction)
                }
                // "schedulable" → continue to full parsing below
            }
            
            // Parse Profile Mutations
            val mutationsArray = json.optJSONArray("profile_mutations")
            val profileMutations = mutableListOf<ParsedProfileMutation>()
            if (mutationsArray != null) {
                for (i in 0 until mutationsArray.length()) {
                    val m = mutationsArray.optJSONObject(i) ?: continue
                    val entityId = m.optString("entityId")
                    val field = m.optString("field")
                    val value = m.optString("value")
                    if (entityId.isNotBlank() && field.isNotBlank()) {
                        profileMutations.add(ParsedProfileMutation(entityId, field, value))
                    }
                }
            }

            // Wave 4.1: Parse tasks array
            val tasksArray = json.optJSONArray("tasks")
            
            val tasks = mutableListOf<com.smartsales.prism.domain.scheduler.TimelineItemModel.Task>()
            var singleSuccessResult: LintResult.Success? = null
            
            if (tasksArray != null && tasksArray.length() > 0) {
                for (i in 0 until tasksArray.length()) {
                    val result = parseSingleTask(tasksArray.getJSONObject(i))
                    if (result is LintResult.Success && result.task != null) {
                        tasks.add(result.task)
                        if (singleSuccessResult == null) singleSuccessResult = result
                    } else if (result is LintResult.Incomplete && tasks.isEmpty() && profileMutations.isEmpty()) {
                        return result
                    }
                }
            } else if (json.has("title")) {
                // Backward compat: try parsing as single object
                val result = parseSingleTask(json)
                if (result is LintResult.Success && result.task != null) {
                    tasks.add(result.task)
                    singleSuccessResult = result
                } else if (result is LintResult.Incomplete && profileMutations.isEmpty()) {
                    return result
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
     * 解析单个任务对象
     * Wave 4.1: 提取为独立方法，供 tasks 数组解析复用
     */
    private fun parseSingleTask(json: JSONObject): LintResult {
        val title = json.optString("title", "")
        if (title.isBlank()) {
            return LintResult.Error("任务标题不能为空")
        }

        val startTimeStr = json.optString("startTime", "")
        val endTimeStr = if (json.isNull("endTime")) null else json.optString("endTime", null)
        
        // Phase 1 Clues 先提取（用于 Incomplete 返回）
        val location = if (json.isNull("location")) null else json.optString("location", null)
        val notes = if (json.isNull("notes")) null else json.optString("notes", null)
        val keyPerson = if (json.isNull("keyPerson")) null else json.optString("keyPerson", null)
        val keyCompany = if (json.isNull("keyCompany")) null else json.optString("keyCompany", null)
        val partialClues = ParsedClues(
            person = keyPerson,
            company = keyCompany,
            location = location,
            briefSummary = if (notes.isNullOrBlank()) title else notes
        )
        
        // 验证日期 — 如果缺失，返回 Incomplete 而不是 Error（Phase 1 循环）
        val startTime = parseDateTime(startTimeStr)
        if (startTime == null) {
            return LintResult.Incomplete(
                missingField = "startTime",
                question = "请问具体是什么时候？",
                partialClues = partialClues
            )
        }
        
        // endTime 可选，null 表示开放式任务
        val endTime: Instant? = endTimeStr?.let { parseDateTime(it) }

        if (endTime != null && endTime.isBefore(startTime)) {
            return LintResult.Error("结束时间不能早于开始时间")
        }

        // 检查是否是过去的时间 (允许今天的任务)
        val todayStart = timeProvider.today.atStartOfDay(timeProvider.zoneId).toInstant()
        if (startTime.isBefore(todayStart)) {
            return LintResult.Error("不能创建过去的任务")
        }

        // Duration: LLM 推断为主，无需确认
        val explicitDuration = json.optString("duration", null)
        
        val durationMinutes: Int = when {
            !explicitDuration.isNullOrBlank() -> parseDuration(explicitDuration) ?: 0
            endTime != null -> ChronoUnit.MINUTES.between(startTime, endTime).toInt().coerceAtLeast(1)
            else -> 0  // fire-off 提醒没有时长
        }
        
        // UrgencyLevel 解析 (Wave 4.2)
        val urgencyStr = json.optString("urgency", "L3") // 默认 L3
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

        val highlights = if (json.isNull("highlights")) null else json.optString("highlights", null)
        
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
                location = location,
                notes = notes,
                keyPerson = keyPerson,
                highlights = highlights,
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
            // e.g. "2026-02-0303:00" -> "2026-02-03 03:00"
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
        // Simple parsing: "30m" -> 30, "1h" -> 60, "1.5h" -> 90
        val lower = durationStr.lowercase().trim()
        return try {
            if (lower.endsWith("min") || lower.endsWith("m")) {
                lower.filter { it.isDigit() }.toInt()
            } else if (lower.endsWith("h") || lower.endsWith("hour")) {
                val num = lower.filter { it.isDigit() || it == '.' }.toFloat()
                (num * 60).toInt()
            } else if (lower.all { it.isDigit() }) {
                lower.toInt() // Assume minutes
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
        return if (end == null) {
            startStr // Open-ended: just show start
        } else {
            startStr
        }
    }

    private fun formatDateRange(start: Instant, end: Instant?): String {
        val dateParams = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val timeParams = DateTimeFormatter.ofPattern("HH:mm")
        
        val startZone = start.atZone(timeProvider.zoneId)
        
        if (end == null) {
            // Open-ended: 2026-02-03 03:00 - ...
            return "${startZone.format(dateParams)} ${startZone.format(timeParams)} - ..."
        }
        
        val endZone = end.atZone(timeProvider.zoneId)
        
        val fullFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        return if (startZone.toLocalDate() == endZone.toLocalDate()) {
            // Same day: 03:00 - 04:00 (Spec aligned)
            "${startZone.format(timeParams)} - ${endZone.format(timeParams)}"
        } else {
            // Multi-day
            val fullFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            "${startZone.format(fullFormatter)} ~ ${endZone.format(fullFormatter)}"
        }
    }
}

/**
 * Phase 1 解析线索 — 传递到 Phase 2 进行实体消歧
 * 
 * @see docs/cerb/memory-center/spec.md §Two-Phase Scheduler Pipeline
 */
data class ParsedClues(
    val person: String? = null,        // 人物（原始提取，仅商务相关）
    val company: String? = null,       // 公司/组织（从输入或对话历史提取）
    val location: String? = null,      // 地点（原始提取，未解析）
    val briefSummary: String? = null,   // 简要摘要
    val durationMinutes: Int? = null    // 持续时间（用于冲突检测）
)

data class ParsedProfileMutation(
    val entityId: String,
    val field: String,
    val value: String
)

/**
 * 校验结果
 */
sealed class LintResult {
    data class Success(
        val task: TimelineItemModel.Task? = null,
        val urgencyLevel: UrgencyLevel = UrgencyLevel.L3_NORMAL,
        val parsedClues: ParsedClues = ParsedClues(),  // Phase 1 → Phase 2 桥梁
        val profileMutations: List<ParsedProfileMutation> = emptyList()
    ) : LintResult()
    
    /**
     * Wave 4.1: Multi-Task Detected
     * LLM 返回多个任务时使用此结果
     */
    data class MultiTask(
        val tasks: List<TimelineItemModel.Task>,
        val profileMutations: List<ParsedProfileMutation> = emptyList()
    ) : LintResult()
    
    /**
     * 缺少必填字段 — 需要用户澄清（Phase 1 循环）
     */
    data class Incomplete(
        val missingField: String,  // "startTime" | "duration"
        val question: String,      // 向用户展示的问题
        val partialClues: ParsedClues
    ) : LintResult()

    data class Error(val message: String) : LintResult()
    
    // Wave 4.0: Input Classification Results
    data class NonIntent(val reason: String) : LintResult()

    
    // Wave 7: NL Deletion
    data class Deletion(val targetTitle: String) : LintResult()
    
    // Wave 11: Global Reschedule
    data class Reschedule(val targetTitle: String, val newInstruction: String) : LintResult()
}
