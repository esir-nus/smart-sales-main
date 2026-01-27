package com.smartsales.prism.domain.core

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
}
