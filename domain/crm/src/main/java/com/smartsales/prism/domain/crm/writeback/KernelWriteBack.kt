package com.smartsales.prism.domain.crm.writeback

import com.smartsales.prism.domain.memory.EntityRef
import com.smartsales.prism.domain.memory.EntityEntry
import com.smartsales.prism.domain.crm.ActivityType

/**
 * Kernel 写回接口 — Application (EntityWriter) → Kernel 的写入通道
 *
 * 核心契约:
 * - SSD 写入后调用，确保 RAM Working Set 与 SSD 同步
 * - 所有方法为 suspend（Kernel 内部可能触发异步操作）
 *
 * 为何独立接口:
 * - ContextBuilder 面向 Pipeline 消费方（只读）
 * - KernelWriteBack 面向 EntityWriter（写入）
 * - 职责分离，避免 EntityWriter 依赖 Kernel 具体类
 *
 * @see docs/cerb/session-context/interface.md
 */
interface KernelWriteBack {

    /**
     * 写入实体到 RAM Section 1
     *
     * EntityWriter 在 SSD 写入后调用此方法。
     * Kernel 同时刷新 Section 3（客户习惯）。
     */
    suspend fun updateEntityInSession(entityId: String, ref: EntityRef, entry: EntityEntry? = null)

    /**
     * 从 RAM Section 1 移除实体（删除时调用）
     */
    suspend fun removeEntityFromSession(entityId: String)

    /**
     * 记录实体变更历史（Profile 变更检测后调用）
     */
    suspend fun recordActivity(entityId: String, type: ActivityType, summary: String)
}
