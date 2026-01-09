package com.smartsales.feature.connectivity

import com.smartsales.core.util.DispatcherProvider
import com.smartsales.core.util.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Unit tests for BadgeHttpClient.
 * 
 * Tests HTTP communication with ESP32 badge per esp32-protocol.md spec.
 */
class BadgeHttpClientTest {

    private lateinit var mockServer: MockWebServer
    private lateinit var client: BadgeHttpClient
    private lateinit var tempDir: File

    // Real dispatcher for HTTP tests (MockWebServer works synchronously)
    private val testDispatchers = object : DispatcherProvider {
        override val io: CoroutineDispatcher = Dispatchers.IO
        override val main: CoroutineDispatcher = Dispatchers.IO
        override val default: CoroutineDispatcher = Dispatchers.IO
    }

    @Before
    fun setup() {
        mockServer = MockWebServer()
        mockServer.start()
        client = DefaultBadgeHttpClient(testDispatchers)
        tempDir = createTempDir("badge_test")
    }

    @After
    fun teardown() {
        mockServer.shutdown()
        tempDir.deleteRecursively()
    }

    private fun baseUrl(): String = mockServer.url("/").toString().trimEnd('/')

    // === Upload Tests ===

    @Test
    fun `uploadJpg returns success on 200`() = runBlocking {
        // Given: Server returns success (HTML response per spec)
        mockServer.enqueue(MockResponse().setBody("文件上传成功！").setResponseCode(200))
        val file = File(tempDir, "1.jpg").apply { writeBytes(ByteArray(100)) }

        // When
        val result = client.uploadJpg(baseUrl(), file)

        // Then
        assertTrue("Expected Success but got $result", result is Result.Success)
        assertEquals("/upload", mockServer.takeRequest().path)
    }

    @Test
    fun `uploadJpg returns error for non-jpg file`() = runBlocking {
        // Given: A PNG file (wrong extension)
        val file = File(tempDir, "test.png").apply { writeBytes(ByteArray(100)) }

        // When
        val result = client.uploadJpg(baseUrl(), file)

        // Then
        assertTrue(result is Result.Error)
        val error = (result as Result.Error).throwable
        assertTrue(error is BadgeHttpException.InvalidFormatException)
    }

    @Test
    fun `uploadJpg returns error for file too large`() = runBlocking {
        // Given: File larger than 10MB
        val file = File(tempDir, "large.jpg").apply { 
            writeBytes(ByteArray(11 * 1024 * 1024)) // 11MB
        }

        // When
        val result = client.uploadJpg(baseUrl(), file)

        // Then
        assertTrue(result is Result.Error)
        val error = (result as Result.Error).throwable
        assertTrue(error is BadgeHttpException.InvalidFormatException)
        assertTrue(error.message!!.contains("too large"))
    }

    @Test
    fun `uploadJpg returns error on 400`() = runBlocking {
        // Given: Server returns bad request
        mockServer.enqueue(MockResponse().setResponseCode(400).setBody("Unsafe characters"))
        val file = File(tempDir, "test.jpg").apply { writeBytes(ByteArray(100)) }

        // When
        val result = client.uploadJpg(baseUrl(), file)

        // Then
        assertTrue(result is Result.Error)
        val error = (result as Result.Error).throwable
        assertTrue(error is BadgeHttpException.ClientException)
    }

    // === List Tests ===

    @Test
    fun `listWavFiles returns file list on success`() = runBlocking {
        // Given: Server returns JSON array per spec
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""["rec1.wav", "rec2.wav"]"""))

        // When
        val result = client.listWavFiles(baseUrl())

        // Then
        assertTrue("Expected Success but got: $result", result is Result.Success)
        assertEquals(listOf("rec1.wav", "rec2.wav"), (result as Result.Success).data)
    }

    @Test
    fun `listWavFiles returns empty list when no files`() = runBlocking {
        // Given: Server returns empty array (SD card mounted but no files)
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("[]"))

        // When
        val result = client.listWavFiles(baseUrl())

        // Then
        assertTrue("Expected Success but got: $result", result is Result.Success)
        assertEquals(emptyList<String>(), (result as Result.Success).data)
    }

    // === Download Tests ===

    @Test
    fun `downloadWav saves file on success`() = runBlocking {
        // Given: Server returns WAV data
        val wavData = ByteArray(1000) { it.toByte() }
        mockServer.enqueue(MockResponse().setBody(okio.Buffer().write(wavData)))
        val dest = File(tempDir, "download.wav")

        // When
        val result = client.downloadWav(baseUrl(), "rec1.wav", dest)

        // Then
        assertTrue(result is Result.Success)
        assertTrue(dest.exists())
        assertArrayEquals(wavData, dest.readBytes())
    }

    @Test
    fun `downloadWav returns error for non-wav filename`() = runBlocking {
        // Given: Wrong extension
        val dest = File(tempDir, "download.wav")

        // When
        val result = client.downloadWav(baseUrl(), "test.mp3", dest)

        // Then
        assertTrue(result is Result.Error)
        val error = (result as Result.Error).throwable
        assertTrue(error is BadgeHttpException.InvalidFormatException)
    }

    @Test
    fun `downloadWav returns NotFound on 404`() = runBlocking {
        // Given: Server returns 404
        mockServer.enqueue(MockResponse().setResponseCode(404))
        val dest = File(tempDir, "download.wav")

        // When
        val result = client.downloadWav(baseUrl(), "missing.wav", dest)

        // Then
        assertTrue(result is Result.Error)
        val error = (result as Result.Error).throwable
        assertTrue(error is BadgeHttpException.NotFoundException)
    }

    // === Delete Tests ===

    @Test
    fun `deleteWav returns success on 200`() = runBlocking {
        // Given: Server returns success JSON per spec
        mockServer.enqueue(MockResponse().setBody("""{"status":"success"}"""))

        // When
        val result = client.deleteWav(baseUrl(), "rec1.wav")

        // Then
        assertTrue(result is Result.Success)
        
        // Verify request format: POST with body filename=...
        val request = mockServer.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/delete", request.path)
        assertEquals("filename=rec1.wav", request.body.readUtf8())
    }

    @Test
    fun `deleteWav returns error for non-wav filename`() = runBlocking {
        // When
        val result = client.deleteWav(baseUrl(), "test.jpg")

        // Then
        assertTrue(result is Result.Error)
        val error = (result as Result.Error).throwable
        assertTrue(error is BadgeHttpException.InvalidFormatException)
    }

    // === Reachability Tests ===

    @Test
    fun `isReachable returns true on 200`() = runBlocking {
        // Given
        mockServer.enqueue(MockResponse().setResponseCode(200))

        // When
        val reachable = client.isReachable(baseUrl())

        // Then
        assertTrue(reachable)
    }

    @Test
    fun `isReachable returns false on network error`() = runBlocking {
        // Given: Shutdown server to simulate network error
        mockServer.shutdown()

        // When
        val reachable = client.isReachable(baseUrl())

        // Then
        assertFalse(reachable)
    }
}
