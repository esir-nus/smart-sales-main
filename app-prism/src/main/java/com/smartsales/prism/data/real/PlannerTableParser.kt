package com.smartsales.prism.data.real

import com.smartsales.prism.domain.analyst.PlannerStep
import com.smartsales.prism.domain.analyst.PlannerTable
import com.smartsales.prism.domain.analyst.StepStatus
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Planner Table 解析器
 * 
 * 解析 LLM 结构化响应为 PlannerTable。
 * 支持 JSON 和 Markdown 表格两种格式。
 * 
 * @see prism-ui-ux-contract.md "Planner Table (Rich Chat Bubble)"
 */
@Singleton
class PlannerTableParser @Inject constructor() {

    /**
     * 解析 LLM 响应为 PlannerTable
     * 
     * @param llmResponse LLM 的原始响应内容
     * @return PlannerTable 或 null（如果无法解析）
     */
    fun parse(llmResponse: String): PlannerTable? {
        // 优先尝试 JSON 格式
        val jsonTable = parseJsonFormat(llmResponse)
        if (jsonTable != null) return jsonTable

        // 回退到 Markdown 表格格式
        return parseMarkdownFormat(llmResponse)
    }

    /**
     * 解析 JSON 格式的 Planner Table
     * 
     * 期望格式:
     * ```json
     * {
     *   "title": "周度客户分析报告",
     *   "steps": [
     *     {"index": 1, "task": "数据汇总", "status": "complete"},
     *     {"index": 2, "task": "趋势分析", "status": "pending"}
     *   ],
     *   "insight": "拜访量上升20%...",
     *   "readyMessage": "分析已完成，请选择后续操作"
     * }
     * ```
     */
    private fun parseJsonFormat(content: String): PlannerTable? {
        val json = extractJson(content) ?: return null

        return try {
            val jsonObj = JSONObject(json)
            
            // 1. Strict Array Finding: Must be 'steps' or 'sections'
            val stepsArray = jsonObj.optJSONArray("steps") 
                ?: jsonObj.optJSONArray("sections")
                ?: return null
            
            val steps = mutableListOf<PlannerStep>()
            for (i in 0 until stepsArray.length()) {
                val stepObj = stepsArray.optJSONObject(i) ?: continue
                
                // Strict Field Matching: 'task' is primary, 'title' is fallback
                val taskName = stepObj.optString("task")
                    .takeIf { it.isNotBlank() }
                    ?: stepObj.optString("title")
                    .takeIf { it.isNotBlank() }
                    ?: "任务 ${i + 1}" // Minimum fallback for valid objects
                
                steps.add(
                    PlannerStep(
                        index = stepObj.optInt("index", i + 1),
                        task = taskName,
                        status = parseStatus(stepObj.optString("status", "pending"))
                    )
                )
            }

            if (steps.isEmpty()) return null

            PlannerTable(
                title = jsonObj.optString("title").takeIf { it.isNotBlank() } ?: "分析计划",
                steps = steps,
                insight = jsonObj.optString("insight").takeIf { it.isNotBlank() },
                readyMessage = jsonObj.optString("readyMessage").takeIf { it.isNotBlank() }
            )
        } catch (e: Exception) {
            android.util.Log.e("PlannerParser", "Strict JSON parse failed", e)
            null
        }
    }

    /**
     * 解析 Markdown 表格格式的 Planner Table
     * 
     * 期望格式:
     * ```
     * | 步骤 | 任务 | 状态 |
     * |------|------|------|
     * | 1    | 数据汇总 | ✅ |
     * | 2    | 趋势分析 | ⏳ |
     * ```
     */
    private fun parseMarkdownFormat(content: String): PlannerTable? {
        // 查找表格模式
        val tablePattern = """\|[^\n]+\|[\s\S]*?\|[^\n]+\|""".toRegex()
        val tableMatch = tablePattern.find(content) ?: return null

        val tableContent = tableMatch.value
        val lines = tableContent.lines()
            .filter { it.contains("|") && !it.contains("---") }
            .drop(1) // 跳过表头

        if (lines.isEmpty()) return null

        val steps = lines.mapIndexedNotNull { index, line ->
            parseMarkdownRow(line, index + 1)
        }

        if (steps.isEmpty()) return null

        // 尝试提取标题（表格前的加粗文本）
        val titlePattern = """\*\*([^*]+)\*\*""".toRegex()
        val titleMatch = titlePattern.find(content.substringBefore(tableMatch.value))
        val title = titleMatch?.groupValues?.get(1) ?: "分析计划"

        // 尝试提取洞察（表格后的加粗文本）
        val afterTable = content.substringAfter(tableMatch.value)
        val insightPattern = """\*\*当前洞察[：:]*\s*\*\*:?\s*(.+)""".toRegex()
        val insight = insightPattern.find(afterTable)?.groupValues?.get(1)

        return PlannerTable(
            title = title,
            steps = steps,
            insight = insight,
            readyMessage = if (steps.all { it.status == StepStatus.COMPLETE }) {
                "分析已完成，请选择后续操作"
            } else null
        )
    }

    private fun parseMarkdownRow(row: String, defaultIndex: Int): PlannerStep? {
        val cells = row.split("|")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (cells.size < 2) return null

        val indexCell = cells.getOrNull(0) ?: return null
        val taskCell = cells.getOrNull(1) ?: return null
        val statusCell = cells.getOrNull(2) ?: ""

        val index = indexCell.toIntOrNull() ?: defaultIndex

        return PlannerStep(
            index = index,
            task = taskCell,
            status = parseStatusEmoji(statusCell)
        )
    }

    private fun parseStatus(status: String): StepStatus {
        return when (status.lowercase()) {
            "complete", "completed", "done" -> StepStatus.COMPLETE
            "in_progress", "running", "processing" -> StepStatus.IN_PROGRESS
            else -> StepStatus.PENDING
        }
    }

    private fun parseStatusEmoji(cell: String): StepStatus {
        return when {
            cell.contains("✅") || cell.contains("☑️") || cell.contains("完成") -> StepStatus.COMPLETE
            cell.contains("⏳") || cell.contains("🔄") || cell.contains("进行") -> StepStatus.IN_PROGRESS
            else -> StepStatus.PENDING
        }
    }

    /**
     * 提取 JSON 块（支持 ```json 包裹或裸 JSON）
     */
    private fun extractJson(content: String): String? {
        // 尝试提取 markdown 代码块
        val codeBlockRegex = """```(?:json)?\s*(\{[\s\S]*?\})\s*```""".toRegex()
        val codeMatch = codeBlockRegex.find(content)
        if (codeMatch != null) {
            return codeMatch.groupValues[1]
        }

        // 尝试提取裸 JSON (包含 steps 字段)
        val jsonRegex = """\{[\s\S]*"steps"[\s\S]*\}""".toRegex()
        val jsonMatch = jsonRegex.find(content)
        if (jsonMatch != null) {
            return jsonMatch.value
        }

        return null
    }
}
