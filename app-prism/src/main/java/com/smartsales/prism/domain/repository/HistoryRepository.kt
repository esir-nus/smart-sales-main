package com.smartsales.prism.domain.repository

import com.smartsales.prism.domain.model.SessionPreview

/**
 * 历史会话仓库 — 获取和管理会话列表
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
    
    /**
     * 置顶/取消置顶会话
     */
    fun togglePin(sessionId: String)
    
    /**
     * 重命名会话
     */
    fun renameSession(sessionId: String, newClientName: String, newSummary: String)
    
    /**
     * 删除会话
     */
    fun deleteSession(sessionId: String)
    
    /**
     * 获取会话详情
     */
    fun getSession(sessionId: String): SessionPreview?
}

