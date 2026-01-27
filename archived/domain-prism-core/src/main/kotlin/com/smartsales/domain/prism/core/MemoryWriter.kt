package com.smartsales.domain.prism.core

/**
 * 内存写入器 — Fire-and-Forget 模式，后台持久化
 * @see Prism-V1.md §2.2 #5
 */
interface MemoryWriter {
    /**
     * 持久化上下文和结果到 Hot Zone
     * 此方法应在后台执行，不阻塞 UI
     */
    suspend fun persist(context: EnhancedContext, result: ExecutorResult)
}
