package com.smartsales.prism.domain.analyst

/**
 * Analyst 工具定义
 * 
 * 用于 Plan Card 中的交付物选项。
 * @see prism-ui-ux-contract.md "Proposed Deliverables"
 */
data class AnalystTool(
    val id: String,
    val icon: String,
    val label: String,
    val description: String,
    val targetRoles: Set<String> = emptySet(), // 空 = 所有角色可见
    val requiredParams: Map<String, String> = emptyMap()
)

/**
 * 工具执行结果
 */
data class ToolResult(
    val toolId: String,
    val title: String,
    val previewText: String,
    val success: Boolean = true
)
