// 文件：feature/chat/src/test/java/com/smartsales/feature/chat/testutil/NoopDebugOrchestrator.kt
// 模块：:feature:chat
// 说明：测试用 DebugOrchestrator 空实现，避免引入真实依赖
// 作者：创建于 2025-12-22
package com.smartsales.feature.chat.testutil

import com.smartsales.data.aicore.debug.DebugOrchestrator
import com.smartsales.data.aicore.debug.DebugSnapshot

class NoopDebugOrchestrator : DebugOrchestrator {
    override suspend fun getDebugSnapshot(
        sessionId: String,
        jobId: String?,
        sessionTitle: String?,
        isTitleUserEdited: Boolean?,
    ): DebugSnapshot {
        return DebugSnapshot(
            section1EffectiveRunText = "section1",
            section2RawTranscriptionText = "section2",
            section3PreprocessedText = "section3",
            sessionId = sessionId,
            jobId = jobId,
        )
    }
}
