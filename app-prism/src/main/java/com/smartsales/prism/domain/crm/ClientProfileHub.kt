package com.smartsales.prism.domain.crm

/**
 * 客户档案中心 — CRM 层次查询
 * @see Client Profile Hub spec
 */
interface ClientProfileHub {
    /**
     * 获取快速上下文（6秒速览）
     */
    suspend fun getQuickContext(entityIds: List<String>): QuickContext
    
    /**
     * 获取聚焦上下文（单个实体深度分析）
     */
    suspend fun getFocusedContext(entityId: String): FocusedContext
    
    /**
     * 获取统一时间轴
     */
    suspend fun getUnifiedTimeline(entityId: String): List<UnifiedActivity>
}
