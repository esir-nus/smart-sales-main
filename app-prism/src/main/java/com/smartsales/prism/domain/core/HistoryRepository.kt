package com.smartsales.prism.domain.core

/**
 * 历史会话仓库 — 获取会话列表
 */
interface HistoryRepository {
    /**
     * 获取分组会话列表
     * @return 按日期分组的会话映射
     */
    fun getGroupedSessions(): Map<String, List<SessionPreview>>
    
    /**
     * 获取所有会话
     */
    fun getSessions(): List<SessionPreview>
}
