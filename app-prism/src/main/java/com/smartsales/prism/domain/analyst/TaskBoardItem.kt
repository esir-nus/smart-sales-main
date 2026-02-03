package com.smartsales.prism.domain.analyst

/**
 * Task Board 项目模型
 * 
 * 用于 Analyst 模式顶部任务板的工作流快捷项。
 * 
 * @see prism-ui-ux-contract.md "Task Board (Sticky Top Layer)"
 */
data class TaskBoardItem(
    val id: String,
    val icon: String,
    val title: String,
    val description: String
)
