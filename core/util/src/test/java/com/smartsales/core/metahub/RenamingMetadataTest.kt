package com.smartsales.core.metahub

// 文件：core/util/src/test/java/com/smartsales/core/metahub/RenamingMetadataTest.kt
// 模块：:core:util
// 说明：验证 M3 命名合并与有效名称解析规则
// 作者：创建于 2025-12-23

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RenamingMetadataTest {

    @Test
    fun mergeWith_preservesAcceptedWhenCandidateArrives() {
        val current = RenamingMetadata(
            sessionTitle = AcceptedAndCandidate(
                accepted = "已确认标题",
                candidate = "旧候选"
            )
        )
        val incoming = RenamingMetadata(
            sessionTitle = AcceptedAndCandidate(candidate = "新候选")
        )

        val merged = current.mergeWith(incoming)

        // accepted 仍为有效值，candidate 更新不覆盖 accepted
        assertEquals("已确认标题", merged.sessionTitle.effectiveName())
    }

    @Test
    fun metaHubEffectiveName_prefersAcceptedThenCandidate() = runTest {
        val metaHub = InMemoryMetaHub()
        val candidateProv = Provenance(source = "analysis", updatedAt = 10L)
        val acceptedProv = Provenance(source = "user", updatedAt = 20L)

        metaHub.setM3CandidateName(
            sessionId = "s1",
            target = RenamingTarget.EXPORT_TITLE,
            name = "候选导出名",
            prov = candidateProv
        )
        val candidateEffective = metaHub.getM3EffectiveName(
            sessionId = "s1",
            target = RenamingTarget.EXPORT_TITLE,
            fallback = "fallback"
        )
        assertEquals("候选导出名", candidateEffective)

        metaHub.setM3AcceptedName(
            sessionId = "s1",
            target = RenamingTarget.EXPORT_TITLE,
            name = "已确认导出名",
            prov = acceptedProv
        )
        val acceptedEffective = metaHub.getM3EffectiveName(
            sessionId = "s1",
            target = RenamingTarget.EXPORT_TITLE,
            fallback = "fallback"
        )
        assertEquals("已确认导出名", acceptedEffective)
    }
}
