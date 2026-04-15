package com.smartsales.prism.data.audio

import android.content.Context
import org.junit.Before
import org.junit.Test
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File
import com.smartsales.prism.domain.audio.TranscriptionStatus
import com.smartsales.prism.domain.connectivity.ConnectivityBridge
import com.smartsales.data.oss.OssUploader
import com.smartsales.prism.domain.tingwu.TingwuPipeline
import com.smartsales.prism.domain.connectivity.WavDownloadResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class RealAudioRepositoryBreakItTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    
    private lateinit var bridge: ConnectivityBridge
    private lateinit var ossUploader: OssUploader
    private lateinit var tingwuPipeline: TingwuPipeline
    private lateinit var repository: RealAudioRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() = runTest(testDispatcher) {
        context = mock()
        whenever(context.filesDir).thenReturn(tempFolder.root)

        bridge = mock()
        
        // Setup mock bridge
        whenever(bridge.listRecordings()).thenReturn(
            com.smartsales.core.util.Result.Success(listOf("test1.wav", "test2.wav", "test3.wav", "test4.wav", "test5.wav", "test6.wav", "test7.wav", "test8.wav", "test9.wav", "test10.wav"))
        )
        whenever(bridge.downloadRecording(org.mockito.kotlin.any(), org.mockito.kotlin.anyOrNull())).thenAnswer { invocation ->
            val filename = invocation.arguments[0] as String
            val tempFile = File(tempFolder.root, "temp_${java.util.UUID.randomUUID()}_$filename")
            tempFile.writeText("fake wav data")
            WavDownloadResult.Success(tempFile, filename, 100L)
        }
        
        ossUploader = mock()
        tingwuPipeline = mock()
        
        repository = RealAudioRepository(
            context = context,
            connectivityBridge = bridge,
            ossUploader = ossUploader,
            tingwuPipeline = tingwuPipeline
        )
    }

    @Test
    fun `test concurrent modifications do not corrupt state`() = runTest(testDispatcher) {
        val jobs = (1..10).map {
            launch {
                repository.syncFromDevice()
            }
        }
        jobs.forEach { it.join() }
        
        val files = repository.getAudioFiles().first()
        assertEquals("Should have exactly 10 files synced concurrently without dropping", 10, files.size)
    }

    @Test
    fun `test invalid audioId deletion handles gracefully`() = runTest(testDispatcher) {
        repository.syncFromDevice() // Adds 10 files based on mock listRecordings
        repository.deleteAudio("non-existent-id")
        
        val files = repository.getAudioFiles().first()
        assertEquals("Should still have 10 files", 10, files.size)
    }
}
