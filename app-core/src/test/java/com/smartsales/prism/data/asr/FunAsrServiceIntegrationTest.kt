package com.smartsales.prism.data.asr

import com.smartsales.data.aicore.BuildConfig
import com.smartsales.data.aicore.DashscopeCredentials
import com.smartsales.data.aicore.DashscopeCredentialsProvider
import com.smartsales.data.oss.FakeOssUploader
import com.smartsales.prism.domain.asr.AsrResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class FunAsrServiceIntegrationTest {

    @Test
    @Ignore("需要有效 API Key 与真实网络环境 — 本地手动运行")
    fun `transcribe with real DashScope call handles API errors gracefully`() = runTest {
        val tempFile = File.createTempFile("test_audio", ".wav")
        tempFile.writeText("dummy content")

        val service = FunAsrService(
            ossUploader = FakeOssUploader(),
            credentialsProvider = object : DashscopeCredentialsProvider {
                override fun obtain(): DashscopeCredentials = DashscopeCredentials(
                    apiKey = System.getenv("DASHSCOPE_API_KEY").orEmpty().ifBlank {
                        BuildConfig.DASHSCOPE_API_KEY
                    },
                    model = "qwen-turbo"
                )
            },
            transcriptionClient = RealFunAsrTranscriptionClient()
        )

        val result = service.transcribe(tempFile)

        assertTrue(result is AsrResult.Error)
        result as AsrResult.Error
        assertTrue(
            result.code == AsrResult.ErrorCode.API_ERROR ||
                result.code == AsrResult.ErrorCode.AUTH_FAILED
        )

        tempFile.delete()
    }
}
