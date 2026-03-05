package com.smartsales.prism.data.asr

import com.smartsales.data.oss.FakeOssUploader
import com.smartsales.data.oss.OssErrorCode
import com.smartsales.data.oss.OssUploadResult
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

    @Before
    fun setup() {
        fakeOssUploader = FakeOssUploader()
        asrService = FunAsrService(fakeOssUploader)
    }

    @Test
    fun `transcribe with non-existent file returns INVALID_FORMAT error`() = runTest {
        val fakeFile = File("non_existent_file.wav")
        val result = asrService.transcribe(fakeFile)

        assertTrue("Result should be Error", result is AsrResult.Error)
        result as AsrResult.Error
        assertEquals(AsrResult.ErrorCode.INVALID_FORMAT, result.code)
    }

    @Test
    fun `transcribe when OSS upload fails returns OSS_UPLOAD_FAILED error`() = runTest {
        val tempFile = File.createTempFile("test_audio", ".wav")
        tempFile.writeText("dummy content")

        // Configure fake to fail upload
        fakeOssUploader.shouldFail = true
        fakeOssUploader.failCode = OssErrorCode.NETWORK_ERROR
        fakeOssUploader.failMessage = "Simulated network failure"

        val result = asrService.transcribe(tempFile)

        assertTrue("Result should be Error", result is AsrResult.Error)
        result as AsrResult.Error
        assertEquals(AsrResult.ErrorCode.OSS_UPLOAD_FAILED, result.code)
        assertTrue(result.message.contains("Simulated network failure"))

        tempFile.delete()
    }

    // Note: We cannot easily mock the DashScope Transcription API in a pure unit test 
    // without PowerMock because FunAsrService directly instantiates `Transcription()`.
    // However, we can verify that the error mapping logic works by allowing it to fail naturally.
    @Test
    fun `transcribe with real DashScope call handles API errors gracefully`() = runTest {
        val tempFile = File.createTempFile("test_audio", ".wav")
        tempFile.writeText("dummy content")

        // Let OSS upload succeed
        val result = asrService.transcribe(tempFile)

        // Since we are likely using a dummy file and it reaches real DashScope, 
        // it should return either AUTH_FAILED (if no key) or API_ERROR.
        assertTrue("Result should be Error", result is AsrResult.Error)
        result as AsrResult.Error
        assertTrue(
            result.code == AsrResult.ErrorCode.API_ERROR || 
            result.code == AsrResult.ErrorCode.AUTH_FAILED
        )

        tempFile.delete()
    }
}
