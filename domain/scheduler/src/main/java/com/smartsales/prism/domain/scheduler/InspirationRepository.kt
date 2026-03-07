package com.smartsales.prism.domain.scheduler

import kotlinx.coroutines.flow.Flow

/**
 * 灵感仓库 — 存储非日程类输入（Wave 5）
 * 
 * 灵感是全局可见的，不绑定特定日期
 * 
 * @see docs/specs/modules/SchedulerDrawer.md L90
 */
interface InspirationRepository {
    /**
     * 插入新灵感
     * @return 生成的灵感 ID
     */
    suspend fun insert(text: String): String
    
    /**
     * 获取所有灵感（全局列表，按创建时间倒序）
     */
    fun getAll(): Flow<List<TimelineItemModel.Inspiration>>
    
    /**
     * 删除指定灵感
     */
    suspend fun delete(id: String)
}
