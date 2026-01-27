package com.smartsales.domain.prism.core.fakes

import com.smartsales.domain.prism.core.*

/**
 * Fake MemoryWriter — No-op 实现，仅打印日志
 */
class FakeMemoryWriter : MemoryWriter {
    
    override suspend fun persist(context: EnhancedContext, result: ExecutorResult) {
        println("[FakeMemoryWriter] 模拟持久化: mode=${context.mode}, contentLength=${result.displayContent.length}")
    }
}
