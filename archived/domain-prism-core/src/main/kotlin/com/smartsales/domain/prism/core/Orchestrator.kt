package com.smartsales.domain.prism.core

import kotlinx.coroutines.flow.StateFlow

/**
 * Pipeline 调度器 — 无状态路由，选择模式并调用管道
 * @see Prism-V1.md §2.2 #2
 */
interface Orchestrator {
    /**
     * 处理用户意图，返回完整的管道结果
     */
    suspend fun processUserIntent(input: String): OrchestratorResult
    
    /**
     * 切换运行模式
     */
    suspend fun switchMode(newMode: Mode)
    
    /**
     * 当前运行模式的状态流
     */
    val currentMode: StateFlow<Mode>
}
