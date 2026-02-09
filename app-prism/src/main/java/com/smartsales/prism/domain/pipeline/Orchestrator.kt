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
    /**
     * 创建日程任务 — 统一入口
     * @param input 用户自然语言输入
     * @param replaceItemId (可选) 指定要替换/改期的旧任务 ID。成功创建新任务后，旧任务将被删除。
     */
    suspend fun createScheduledTask(input: String, replaceItemId: String? = null): UiState
}


