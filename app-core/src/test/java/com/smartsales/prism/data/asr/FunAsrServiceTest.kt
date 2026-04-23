package com.smartsales.prism.data.asr

import com.smartsales.data.aicore.DashscopeCredentials
import com.smartsales.data.aicore.DashscopeCredentialsProvider
import com.smartsales.data.oss.FakeOssUploader
import com.smartsales.data.oss.OssErrorCode
import com.smartsales.prism.domain.asr.AsrResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class FunAsrServiceTest {

    private lateinit var asrService: FunAsrService
    private lateinit var fakeOssUploader: FakeOssUploader
    private lateinit var fakeCredentialsProvider: FakeDashscopeCredentialsProvider
    private lateinit var fakeTranscriptionClient: FakeFunAsrTranscriptionClient

    @Before
    fun setup() {
        fakeOssUploader = FakeOssUploader()
        fakeCredentialsProvider = FakeDashscopeCredentialsProvider()
        fakeTranscriptionClient = FakeFunAsrTranscriptionClient()
        asrService = FunAsrService(
            ossUploader = fakeOssUploader,
            credentialsProvider = fakeCredentialsProvider,
            transcriptionClient = fakeTranscriptionClient
        )
    }

    @Test
    fun `transcribe with non-existent file returns INVALID_FORMAT error`() = runTest {
        val fakeFile = File("non_existent_file.wav")

        val result = asrService.transcribe(fakeFile)

        assertTrue(result is AsrResult.Error)
        result as AsrResult.Error
        assertEquals(AsrResult.ErrorCode.INVALID_FORMAT, result.code)
    }

    @Test
    fun `transcribe with blank credentials returns AUTH_FAILED error`() = runTest {
        val tempFile = File.createTempFile("test_audio", ".wav")
        tempFile.writeText("dummy content")
        fakeCredentialsProvider.apiKey = ""

        val result = asrService.transcribe(tempFile)

        assertTrue(result is AsrResult.Error)
        result as AsrResult.Error
        assertEquals(AsrResult.ErrorCode.AUTH_FAILED, result.code)
        assertTrue(result.message.contains("DASHSCOPE_API_KEY"))

        tempFile.delete()
    }

    @Test
    fun `transcribe when OSS upload fails returns OSS_UPLOAD_FAILED error`() = runTest {
        val tempFile = File.createTempFile("test_audio", ".wav")
        tempFile.writeText("dummy content")
        fakeOssUploader.shouldFail = true
        fakeOssUploader.failCode = OssErrorCode.NETWORK_ERROR
        fakeOssUploader.failMessage = "Simulated network failure"

        val result = asrService.transcribe(tempFile)

        assertTrue(result is AsrResult.Error)
        result as AsrResult.Error
        assertEquals(AsrResult.ErrorCode.OSS_UPLOAD_FAILED, result.code)
        assertTrue(result.message.contains("Simulated network failure"))

        tempFile.delete()
    }

    @Test
    fun `transcribe when transcription client throws auth error returns AUTH_FAILED`() = runTest {
        val tempFile = File.createTempFile("test_audio", ".wav")
        tempFile.writeText("dummy content")
        fakeTranscriptionClient.exception = IllegalStateException("401 auth failed")

        val result = asrService.transcribe(tempFile)

        assertTrue(result is AsrResult.Error)
        result as AsrResult.Error
        assertEquals(AsrResult.ErrorCode.AUTH_FAILED, result.code)

        tempFile.delete()
    }

    @Test
    fun `transcribe with successful upload and transcription returns Success`() = runTest {
        val tempFile = File.createTempFile("test_audio", ".wav")
        tempFile.writeText("dummy content")
        fakeTranscriptionClient.resultText = "测试转写文本"

        val result = asrService.transcribe(tempFile)

        assertTrue(result is AsrResult.Success)
        result as AsrResult.Success
        assertEquals("测试转写文本", result.text)

        tempFile.delete()
    }

    private class FakeDashscopeCredentialsProvider : DashscopeCredentialsProvider {
        var apiKey: String = "test-api-key"

        override fun obtain(): DashscopeCredentials = DashscopeCredentials(
            apiKey = apiKey,
            model = "qwen-turbo"
        )
    }

    private class FakeFunAsrTranscriptionClient : FunAsrTranscriptionClient {
        var resultText: String = "fake transcription"
        var exception: Exception? = null

        override suspend fun transcribe(fileUrl: String, apiKey: String): String {
            exception?.let { throw it }
            return resultText
        }
    }
}
