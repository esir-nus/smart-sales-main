package com.smartsales.domain.prism.core

/**
 * Session Cache — 任务内快速上下文访问
 * @see Prism-V1.md §2.2 #1b
 */
interface SessionCache {
    /**
     * 获取当前缓存快照
     */
    fun getSnapshot(): SessionCacheSnapshot
    
    /**
     * 更新缓存条目
     */
    fun update(key: String, value: String)
    
    /**
     * 清空缓存（任务结束时调用）
     */
    fun clear()
}
