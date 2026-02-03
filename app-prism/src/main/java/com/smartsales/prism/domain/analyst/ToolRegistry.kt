package com.smartsales.prism.domain.analyst

/**
 * 工具注册中心接口
 * 
 * 提供 Analyst 模式可用的工具列表和执行能力。
 */
interface ToolRegistry {
    /**
     * 获取指定角色可用的工具列表
     * @param role 用户角色 (sales_rep, manager, executive)
     */
    fun getToolsForRole(role: String): List<AnalystTool>

    /**
     * 执行指定工具
     * @param toolId 工具 ID
     * @param context 执行上下文 (如用户输入、计划内容)
     */
    suspend fun executeTool(toolId: String, context: String): ToolResult

    /**
     * 获取所有工具
     */
    fun getAllTools(): List<AnalystTool>
}
