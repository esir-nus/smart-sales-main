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
    fun setup() {
        context = mock()
        whenever(context.filesDir).thenReturn(tempFolder.root)

        bridge = mock()
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
        repository.syncFromDevice() // Add 1
        repository.deleteAudio("non-existent-id")
        
        val files = repository.getAudioFiles().first()
        assertEquals("Should still have 1 file", 1, files.size)
    }
}
