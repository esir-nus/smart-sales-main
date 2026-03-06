package com.smartsales.prism.domain.pipeline

import com.smartsales.prism.domain.model.Mode
import com.smartsales.prism.domain.model.UiState

/**
 * 发布器 — 输出格式化策略
 * @see Prism-V1.md §2.2 #4
 */
interface Publisher {
    /**
     * 发布执行结果到 UI
     * @param result 执行结果
     * @param mode 当前模式
     * @return UI 状态
     */
    suspend fun publish(result: ExecutorResult, mode: Mode): UiState
}
