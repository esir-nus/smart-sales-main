package com.smartsales.prism.data.fakes

import com.smartsales.prism.domain.analyst.AnalystTool
import com.smartsales.prism.domain.analyst.ToolRegistry
import com.smartsales.prism.domain.analyst.ToolResult
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fake Tool Registry — 用于 Analyst 模式烟雾测试
 * 
 * 提供 5 个假工具，支持角色过滤。
 */
@Singleton
class FakeToolRegistry @Inject constructor() : ToolRegistry {

    private val tools = listOf(
        AnalystTool(
            id = "exec_pdf",
            icon = "📊",
            label = "高管摘要 (1页PDF)",
            description = "生成一页精简的高管汇报文档",
            targetRoles = setOf("executive", "manager")
        ),
        AnalystTool(
            id = "visual_report",
            icon = "📈",
            label = "数据可视化报告",
            description = "包含图表和趋势分析的完整报告",
            targetRoles = emptySet()  // 所有角色可见
        ),
        AnalystTool(
            id = "mind_map",
            icon = "🗺️",
            label = "思维导图",
            description = "展示关键信息和关联关系",
            targetRoles = emptySet()
        ),
        AnalystTool(
            id = "crm_csv",
            icon = "📋",
            label = "CRM导入文件 (CSV)",
            description = "可直接导入 Salesforce/HubSpot 的格式",
            targetRoles = setOf("sales_rep", "manager")
        ),
        AnalystTool(
            id = "email_draft",
            icon = "📧",
            label = "预写邮件草稿",
            description = "可直接发送给客户的邮件内容",
            targetRoles = emptySet()
        )
    )

    override fun getAllTools(): List<AnalystTool> = tools

    override fun getToolsForRole(role: String): List<AnalystTool> {
        return tools.filter { tool ->
            tool.targetRoles.isEmpty() || role in tool.targetRoles
        }
    }

    override suspend fun executeTool(toolId: String, context: String): ToolResult {
        val tool = tools.find { it.id == toolId }
            ?: return ToolResult(
                toolId = toolId,
                title = "未知工具",
                previewText = "工具不存在",
                success = false
            )

        android.util.Log.d("TaskBoardTool", "🔨 FakeToolRegistry: executing tool \$toolId")

        // 模拟执行延迟
        delay(800)

        // 生成假结果
        return when (toolId) {
            "exec_pdf" -> ToolResult(
                toolId = toolId,
                title = "Q3 APAC 业绩分析摘要",
                previewText = """
                    【高管摘要】
                    
                    核心发现：供应链延迟导致 APAC 区域 Q3 营收下降 5%
                    
                    关键数据：
                    • 物流成本上升 12%
                    • 交付周期延长 8 天
                    • 客户满意度下降 3%
                    
                    建议行动：优化供应商组合，建立安全库存
                """.trimIndent()
            )
            "visual_report" -> ToolResult(
                toolId = toolId,
                title = "数据可视化分析报告",
                previewText = """
                    【可视化报告预览】
                    
                    📊 营收趋势图 — 显示 Q1-Q3 对比
                    📈 成本分解图 — 物流占比突出
                    🗺️ 区域热力图 — APAC 标红
                    
                    完整报告包含 12 页图表和详细分析
                """.trimIndent()
            )
            "mind_map" -> ToolResult(
                toolId = toolId,
                title = "业绩分析思维导图",
                previewText = """
                    【思维导图结构】
                    
                    Q3业绩下滑
                    ├── 供应链问题
                    │   ├── 物流延迟
                    │   └── 成本上升
                    ├── 区域差异
                    │   ├── APAC (主要)
                    │   └── EMEA (次要)
                    └── 客户反馈
                        └── 交付不及时
                """.trimIndent()
            )
            "crm_csv" -> ToolResult(
                toolId = toolId,
                title = "CRM 导入文件",
                previewText = """
                    【CSV 预览】
                    
                    Account,Contact,Issue,Priority,Next Action
                    张总公司,张总,交付延迟,High,安排回访
                    李氏集团,李经理,价格竞争,Medium,发送方案
                    
                    共 15 条记录，可直接导入 CRM
                """.trimIndent()
            )
            "email_draft" -> ToolResult(
                toolId = toolId,
                title = "客户邮件草稿",
                previewText = """
                    【邮件预览】
                    
                    主题：Q3 业绩分析及改进方案
                    
                    尊敬的张总：
                    
                    感谢您一直以来的信任。针对近期的交付延迟问题，
                    我们已完成深入分析并制定了改进方案...
                    
                    [点击查看完整邮件]
                """.trimIndent()
            )
            else -> ToolResult(
                toolId = toolId,
                title = tool.label,
                previewText = "工具执行完成",
                success = true
            )
        }
    }
}
