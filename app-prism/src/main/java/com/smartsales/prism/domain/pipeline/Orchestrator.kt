package com.smartsales.prism.domain.pipeline

import com.smartsales.prism.domain.model.Mode
import com.smartsales.prism.domain.model.UiState
import kotlinx.coroutines.flow.StateFlow

/**
 * Pipeline 调度器接口
 * @see Prism-V1.md §2.2
 */
interface Orchestrator {
    /**
     * 处理用户输入，返回 UI 状态
     */
    suspend fun processInput(input: String): UiState
    
    /**
     * 切换运行模式
     */
    suspend fun switchMode(newMode: Mode)
    
    /**
     * 当前运行模式
     */
    val currentMode: StateFlow<Mode>
    
    /**
     * 创建日程任务 — 直接路由到 Scheduler Pipeline
     * 用于 Scheduler Drawer 内的操作，无需切换全局 Mode
     */
    suspend fun createScheduledTask(input: String): UiState
    
    /**
     * 处理日程操作 (改期、解决冲突等)
     * LLM 解析在这里发生，然后调用 Repo 更新数据
     */
    suspend fun processSchedulerAction(itemId: String, userText: String): SchedulerActionResult
}

/**
 * Scheduler 操作结果
 */
sealed class SchedulerActionResult {
    data class Success(val reply: String) : SchedulerActionResult()
    data class Failure(val error: String) : SchedulerActionResult()
}

