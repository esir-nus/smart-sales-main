package com.smartsales.prism.domain.pipeline

import kotlinx.coroutines.flow.StateFlow

/**
 * FSM 注册中心 - 统一的状态输出层
 *
 * 设计理念：
 * 1. 每个 FlowMode 对应一个 FlowController
 * 2. Controller 暴露 StateFlow<FlowState> 供 UI 订阅
 * 3. 新功能通过 /fsm-review 工作流增量添加
 *
 * @see Prism-V1.md §4.6.1
 */

// ============================================================================
// 流程模式 (可扩展)
// ============================================================================

sealed interface FlowMode {
    /** Analyst 模式 - 复杂分析流程 */
    data object Analyst : FlowMode

    /** Coach 模式 - 轻量对话 (未来可扩展) */
    data object Coach : FlowMode

    /** Scheduler 模式 - 日程管理 (未来可扩展) */
    data object Scheduler : FlowMode

    /** 系统通知 - 内存更新、客户偏好等 (未来可扩展) */
    data class SystemNotification(val type: String) : FlowMode
}

// ============================================================================
// 流程状态基类 (统一输出)
// ============================================================================

interface FlowState

// ============================================================================
// 流程控制器 (统一契约)
// ============================================================================

interface FlowController {
    /** 统一的状态输出 - 前端订阅此 Flow 获取所有状态变更 */
    val state: StateFlow<out FlowState>
}

// ============================================================================
// 注册中心 (薄封装)
// ============================================================================

interface FlowControllerRegistry {
    /** 获取指定模式的控制器 */
    fun getController(mode: FlowMode): FlowController?

    /** 注册新的控制器 */
    fun register(mode: FlowMode, controller: FlowController)
}

/**
 * 默认注册中心实现
 */
class DefaultFlowControllerRegistry : FlowControllerRegistry {
    private val controllers = mutableMapOf<FlowMode, FlowController>()

    override fun getController(mode: FlowMode): FlowController? = controllers[mode]

    override fun register(mode: FlowMode, controller: FlowController) {
        controllers[mode] = controller
    }
}
