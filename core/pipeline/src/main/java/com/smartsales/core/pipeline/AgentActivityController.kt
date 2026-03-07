package com.smartsales.core.pipeline

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
    val activity: StateFlow<AgentActivity?> = _activity.asStateFlow()
    
    fun startPhase(phase: ActivityPhase, action: ActivityAction? = null) {
        _activity.value = AgentActivity(phase = phase, action = action)
    }
    
    fun updateAction(action: ActivityAction) {
        val current = _activity.value ?: return
        _activity.value = current.copy(action = action)
    }
    
    fun appendTrace(line: String) {
        val current = _activity.value ?: return
        _activity.value = current.copy(trace = current.trace + line)
    }
    
    fun setTrace(lines: List<String>) {
        val current = _activity.value ?: return
        _activity.value = current.copy(trace = lines)
    }
    
    fun complete() {
        _activity.value = AgentActivity(phase = ActivityPhase.COMPLETED)
    }
    
    fun reset() {
        _activity.value = null
    }
    
    fun error(message: String) {
        _activity.value = AgentActivity(
            phase = ActivityPhase.ERROR,
            trace = listOf("错误: $message")
        )
    }
}
