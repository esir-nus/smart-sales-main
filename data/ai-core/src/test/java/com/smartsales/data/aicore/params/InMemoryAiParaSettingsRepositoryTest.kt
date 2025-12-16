// 文件：data/ai-core/src/test/java/com/smartsales/data/aicore/params/InMemoryAiParaSettingsRepositoryTest.kt
// 模块：:data:ai-core
// 说明：验证 AiParaSettingsRepository 的内存实现可被运行时更新
// 作者：创建于 2025-12-16
package com.smartsales.data.aicore.params

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class InMemoryAiParaSettingsRepositoryTest {

    @Test
    fun update_updatesSnapshot() = runTest {
        val repo = InMemoryAiParaSettingsRepository()
        assertEquals(TRANSCRIPTION_PROVIDER_XFYUN, repo.snapshot().transcriptionProvider)
        assertEquals(true, repo.snapshot().xfyunEngSmoothproc)

        repo.update {
            it.copy(
                transcriptionProvider = TRANSCRIPTION_PROVIDER_TINGWU,
                xfyunEngSmoothproc = false,
            )
        }

        assertEquals(TRANSCRIPTION_PROVIDER_TINGWU, repo.snapshot().transcriptionProvider)
        assertFalse(repo.snapshot().xfyunEngSmoothproc)
    }
}

