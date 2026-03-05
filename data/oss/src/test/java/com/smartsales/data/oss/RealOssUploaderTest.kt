package com.smartsales.data.oss

import com.alibaba.sdk.android.oss.ClientException
import com.alibaba.sdk.android.oss.OSS
import com.alibaba.sdk.android.oss.ServiceException
import com.alibaba.sdk.android.oss.model.PutObjectRequest
import com.alibaba.sdk.android.oss.model.PutObjectResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class RealOssUploaderTest {

    private lateinit var uploader: RealOssUploader
    private lateinit var mockOss: OSS

    companion object {
        const val ENDPOINT = "https://oss-cn-beijing.aliyuncs.com"
        const val BUCKET = "test-bucket"
    }

    @Before
    fun setup() {
        mockOss = mock()
        uploader = RealOssUploader(mockOss, BUCKET, ENDPOINT)
    }

    @Test
    fun `upload with non-existent file returns Error`() = runTest {
        val fakeFile = File("non_existent_file.wav")
        val result = uploader.upload(fakeFile, "audio/test.wav")

        assertTrue(result is OssUploadResult.Error)
        result as OssUploadResult.Error
        assertEquals("文件不存在: non_existent_file.wav", result.message)
    }

    @Test
    fun `upload success returns correct public url`() = runTest {
        val tempFile = File.createTempFile("test_audio", ".wav")
        tempFile.writeText("dummy content")

        val expectedResult = PutObjectResult().apply { eTag = "fake_etag" }
        whenever(mockOss.putObject(anyOrNull())).thenReturn(expectedResult)

        val result = uploader.upload(tempFile, "audio/test.wav")

        assertTrue(result is OssUploadResult.Success)
        result as OssUploadResult.Success
        assertEquals("https://test-bucket.oss-cn-beijing.aliyuncs.com/audio/test.wav", result.publicUrl)

        verify(mockOss).putObject(argThat { request ->
            request.bucketName == BUCKET && request.objectKey == "audio/test.wav"
        })

        tempFile.delete()
    }

    @Test
    fun `upload fails with ClientException returns NETWORK_ERROR`() = runTest {
        val tempFile = File.createTempFile("test_audio", ".wav")
        tempFile.writeText("dummy content")

        whenever(mockOss.putObject(anyOrNull())).thenThrow(ClientException("Simulated network timeout"))

        val result = uploader.upload(tempFile, "audio/test.wav")

        assertTrue(result is OssUploadResult.Error)
        result as OssUploadResult.Error
        assertEquals(OssErrorCode.NETWORK_ERROR, result.code)

        tempFile.delete()
    }

    @Test
    fun `upload fails with ServiceException InvalidAccessKeyId returns AUTH_FAILED`() = runTest {
        val tempFile = File.createTempFile("test_audio", ".wav")
        tempFile.writeText("dummy content")

        val exception = ServiceException(
            403, "Simulated message", "InvalidAccessKeyId", "req_id", "host_id", "raw_message"
        )
        whenever(mockOss.putObject(anyOrNull())).thenThrow(exception)

        val result = uploader.upload(tempFile, "audio/test.wav")

        assertTrue(result is OssUploadResult.Error)
        result as OssUploadResult.Error
        assertEquals(OssErrorCode.AUTH_FAILED, result.code)

        tempFile.delete()
    }

    @Test
    fun `upload fails with ServiceException NoSuchBucket returns BUCKET_NOT_FOUND`() = runTest {
        val tempFile = File.createTempFile("test_audio", ".wav")
        tempFile.writeText("dummy content")

        val exception = ServiceException(
            404, "Simulated message", "NoSuchBucket", "req_id", "host_id", "raw_message"
        )
        whenever(mockOss.putObject(anyOrNull())).thenThrow(exception)

        val result = uploader.upload(tempFile, "audio/test.wav")

        assertTrue(result is OssUploadResult.Error)
        result as OssUploadResult.Error
        assertEquals(OssErrorCode.BUCKET_NOT_FOUND, result.code)

        tempFile.delete()
    }
}
