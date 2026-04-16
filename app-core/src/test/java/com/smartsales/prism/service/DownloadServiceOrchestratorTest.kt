package com.smartsales.prism.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times

/**
 * 下载服务编排器单元测试
 *
 * 验证 notifyDownloadStarting() 正确调用 startForegroundService
 */
class DownloadServiceOrchestratorTest {

    private lateinit var context: Context
    private lateinit var orchestrator: DownloadServiceOrchestrator

    @Before
    fun setup() {
        context = mock()
        orchestrator = DownloadServiceOrchestrator(context)
    }

    @Test
    fun `notifyDownloadStarting invokes startForegroundService`() {
        mockStatic(ContextCompat::class.java).use { mockedContextCompat ->
            orchestrator.notifyDownloadStarting()

            // 验证 startForegroundService 被调用一次，使用正确的 context 和一个 Intent
            mockedContextCompat.verify(
                { ContextCompat.startForegroundService(eq(context), any<Intent>()) },
                times(1)
            )
        }
    }

    @Test
    fun `notifyDownloadStarting is idempotent`() {
        mockStatic(ContextCompat::class.java).use { mockedContextCompat ->
            orchestrator.notifyDownloadStarting()
            orchestrator.notifyDownloadStarting()

            // 两次调用，两次都应触发 startForegroundService
            mockedContextCompat.verify(
                { ContextCompat.startForegroundService(eq(context), any<Intent>()) },
                times(2)
            )
        }
    }
}
