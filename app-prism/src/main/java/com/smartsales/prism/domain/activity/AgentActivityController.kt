package com.smartsales.prism.domain.activity

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 代理活动控制器 — 管理全局"思考"可视化
 * 
 * 使用两层结构展示代理活动：Phase（阶段）+ Action（动作）+ Trace（痕迹）
 * 这是核心产品差异化功能。
 * 
 * @see Prism-V1.md §4.6.2
 */
@Singleton
class AgentActivityController @Inject constructor() {
    
    private val _activity = MutableStateFlow<AgentActivity?>(null)
    
    /**
     * 当前活动状态，UI 层订阅此 Flow
     */
    val activity: StateFlow<AgentActivity?> = _activity.asStateFlow()
    
    /**
     * 开始新阶段
     * 
     * @param phase 高层任务阶段
     * @param action 可选的具体动作
     */
    fun startPhase(phase: ActivityPhase, action: ActivityAction? = null) {
        _activity.value = AgentActivity(phase, action)
    }
    
    /**
     * 更新当前动作（保持阶段不变）
     */
    fun updateAction(action: ActivityAction) {
        _activity.value?.let { current ->
            _activity.value = current.copy(action = action)
        }
    }
    
    /**
     * 追加思考痕迹行
     */
    fun appendTrace(line: String) {
        _activity.value?.let { current ->
            _activity.value = current.copy(trace = current.trace + line)
        }
    }
    
    /**
     * 批量设置思考痕迹（用于完整 CoT 返回场景）
     */
    fun setTrace(lines: List<String>) {
        _activity.value?.let { current ->
            _activity.value = current.copy(trace = lines)
        }
    }
    
    /**
     * 完成当前活动，清空状态
     */
    fun complete() {
        _activity.value = null
    }
    
    /**
     * 设置错误状态
     */
    fun error(message: String) {
        _activity.value = AgentActivity(
            phase = ActivityPhase.ERROR,
            action = null,
            trace = listOf(message)
        )
    }
}
