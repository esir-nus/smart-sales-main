package com.smartsales.prism.domain.scheduler

import com.smartsales.prism.domain.time.TimeProvider
import org.json.JSONObject
import java.time.Instant
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
            
            val title = json.optString("title", "")
            if (title.isBlank()) {
                return LintResult.Error("任务标题不能为空")
            }

            val startTimeStr = json.optString("startTime", "")
            val endTimeStr = if (json.isNull("endTime")) null else json.optString("endTime", null)
            
            // 验证日期合理性
            val startTime = parseDateTime(startTimeStr)
                ?: return LintResult.Error("无法解析开始时间: $startTimeStr")
            
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

            val location = if (json.isNull("location")) null else json.getString("location")
            val notes = if (json.isNull("notes")) null else json.getString("notes")
            val keyPerson = if (json.isNull("keyPerson")) null else json.getString("keyPerson")
            val highlights = if (json.isNull("highlights")) null else json.getString("highlights")
            val reminder = if (json.isNull("reminder")) null else json.getString("reminder")
            
            // Infer alarm cascade from reminder type
            val alarmCascade: List<String>? = when (reminder) {
                "smart" -> listOf("-1h", "-15m", "-5m")
                null -> null
                else -> listOf("-15m") // Simple single reminder
            }

            LintResult.Success(
                task = TimelineItemModel.Task(
                    id = "", // 新任务，ID 由 Repository 生成
                    timeDisplay = formatTimeDisplay(startTime, endTime),
                    title = title,
                    isDone = false,
                    hasAlarm = reminder != null,
                    isSmartAlarm = reminder == "smart",
                    startTime = startTime,
                    endTime = endTime,
                    dateRange = formatDateRange(startTime, endTime),
                    location = location,
                    notes = notes,
                    keyPerson = keyPerson,
                    highlights = highlights,
                    alarmCascade = alarmCascade
                ),
                reminderType = when (reminder) {
                    "smart" -> ReminderType.SMART_CASCADE
                    null -> null
                    else -> ReminderType.SINGLE
                },
                taskTypeHint = inferTaskType(title),
                parsedClues = ParsedClues(
                    personAlias = keyPerson,           // 原始人物别名
                    location = location,               // 地点
                    briefSummary = title               // 摘要（使用标题）
                )
            )
        } catch (e: Exception) {
            LintResult.Error("JSON 解析失败: ${e.message}")
        }
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
        
        return if (startZone.toLocalDate() == endZone.toLocalDate()) {
            // Same day: 03:00 - 04:00 (Spec aligned)
            "${startZone.format(timeParams)} - ${endZone.format(timeParams)}"
        } else {
            // Diff day: 2026-02-03 03:00 ~ 2026-02-04 09:00
            val fullFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            "${startZone.format(fullFormatter)} ~ ${endZone.format(fullFormatter)}"
        }
    }

    private fun inferTaskType(title: String): TaskTypeHint {
        val lower = title.lowercase()
        return when {
            "会议" in lower || "meeting" in lower -> TaskTypeHint.MEETING
            "电话" in lower || "call" in lower -> TaskTypeHint.CALL
            "紧急" in lower || "urgent" in lower -> TaskTypeHint.URGENT
            else -> TaskTypeHint.PERSONAL
        }
    }
}

/**
 * 校验结果
 */
sealed class LintResult {
    /**
     * 成功解析的结果
     * 
     * @param task 解析后的任务模型
     * @param reminderType 提醒类型
     * @param taskTypeHint 任务类型提示
     * @param parsedClues Phase 1 解析的线索（用于 Phase 2 实体解析）
     */
    data class Success(
        val task: TimelineItemModel.Task,
        val reminderType: ReminderType?,
        val taskTypeHint: TaskTypeHint,
        val parsedClues: ParsedClues = ParsedClues()
    ) : LintResult()

    data class Error(val message: String) : LintResult()
}

/**
 * Phase 1 解析的线索 — 用于 Phase 2 LLM 实体解析
 * 
 * @property personAlias 人物别名（原始输入，未解析），如 "张总"
 * @property location 地点，如 "会议室"
 * @property briefSummary 摘要，如 "开会"
 * 
 * 注意：startTime 和 duration 已经在 task 中，不需要重复存储
 */
data class ParsedClues(
    val personAlias: String? = null,
    val location: String? = null,
    val briefSummary: String? = null
)
