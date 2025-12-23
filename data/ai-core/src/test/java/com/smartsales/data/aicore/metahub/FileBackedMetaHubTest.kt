package com.smartsales.data.aicore.metahub

// 文件：data/ai-core/src/test/java/com/smartsales/data/aicore/metahub/FileBackedMetaHubTest.kt
// 模块：:data:ai-core
// 说明：验证 FileBackedMetaHub 的持久化与覆盖行为
// 作者：创建于 2025-12-23

import com.google.gson.Gson
import com.smartsales.core.metahub.AcceptedAndCandidate
import com.smartsales.core.metahub.ConversationDerivedStateDelta
import com.smartsales.core.metahub.DerivedEnumWithProv
import com.smartsales.core.metahub.M2PatchRecord
import com.smartsales.core.metahub.Provenance
import com.smartsales.core.metahub.RawSignals
import com.smartsales.core.metahub.RenamingMetadata
import com.smartsales.core.metahub.SessionMetadata
import java.nio.file.Files
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FileBackedMetaHubTest {

    @Test
    fun getSession_returnsNullWhenMissing() = runTest {
        val dir = Files.createTempDirectory("metahub-test").toFile()
        val metaHub = FileBackedMetaHub(dir, Gson())

        val stored = metaHub.getSession("missing")

        assertNull(stored)
    }

    @Test
    fun upsertSession_roundTripsRenaming() = runTest {
        val dir = Files.createTempDirectory("metahub-test").toFile()
        val gson = Gson()
        val metaHub = FileBackedMetaHub(dir, gson)
        val metadata = SessionMetadata(
            sessionId = "s1",
            mainPerson = "客户A",
            renaming = RenamingMetadata(
                sessionTitle = AcceptedAndCandidate(accepted = "已确认标题")
            )
        )

        metaHub.upsertSession(metadata)

        val reloaded = FileBackedMetaHub(dir, gson)
        val stored = reloaded.getSession("s1")
        assertEquals("客户A", stored?.mainPerson)
        assertEquals("已确认标题", stored?.renaming?.sessionTitle?.accepted)
    }

    @Test
    fun upsertSession_preservesMergeBehavior() = runTest {
        val dir = Files.createTempDirectory("metahub-test").toFile()
        val metaHub = FileBackedMetaHub(dir, Gson())
        val first = SessionMetadata(
            sessionId = "s2",
            mainPerson = "客户A",
            shortSummary = "旧摘要"
        )
        val second = SessionMetadata(
            sessionId = "s2",
            mainPerson = "客户B"
        )

        metaHub.upsertSession(first)
        metaHub.upsertSession(second)

        val stored = metaHub.getSession("s2")
        assertEquals("客户B", stored?.mainPerson)
        assertEquals("旧摘要", stored?.shortSummary)
    }

    @Test
    fun appendM2Patch_persistsEffectiveAndHistory() = runTest {
        val dir = Files.createTempDirectory("metahub-test").toFile()
        val gson = Gson()
        val metaHub = FileBackedMetaHub(dir, gson)
        val prov = Provenance(source = "test", updatedAt = 100L)
        val patch = M2PatchRecord(
            patchId = "p1",
            createdAt = 10L,
            prov = prov,
            payload = ConversationDerivedStateDelta(
                rawSignals = RawSignals(
                    dealRiskLevel = DerivedEnumWithProv(value = "LOW", prov = prov)
                )
            )
        )

        metaHub.appendM2Patch("s3", patch)

        val reloaded = FileBackedMetaHub(dir, gson)
        val effective = reloaded.getEffectiveM2("s3")
        val stored = reloaded.getSession("s3")
        assertEquals("LOW", effective?.rawSignals?.dealRiskLevel?.value)
        assertEquals(1, stored?.m2PatchHistory?.size)
        assertEquals("p1", stored?.m2PatchHistory?.firstOrNull()?.patchId)
    }
}
