package com.smartsales.domain.prism.core

import kotlinx.coroutines.flow.StateFlow

/**
 * 模式发布器 — 将执行结果转换为 UI 状态
 * @see Prism-V1.md §2.2 #4
 */
interface ModePublisher {
    /**
     * 发布执行结果，更新 UI 状态
     */
    suspend fun publish(result: ExecutorResult)
    
    /**
     * UI 状态流，供 ViewModel 订阅
     */
    val uiState: StateFlow<UiState>
}
