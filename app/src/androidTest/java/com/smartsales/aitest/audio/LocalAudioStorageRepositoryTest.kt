package com.smartsales.aitest.audio

// 文件：app/src/androidTest/java/com/smartsales/aitest/audio/LocalAudioStorageRepositoryTest.kt
// 模块：:app
// 说明：验证本地音频存储仓库的导入、列出与删除行为
// 作者：创建于 2025-11-21

import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import com.smartsales.core.util.DispatcherProvider
import com.smartsales.core.util.Result
import com.smartsales.feature.media.audiofiles.AudioOrigin
import com.smartsales.feature.media.devicemanager.DeviceMediaFile
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LocalAudioStorageRepositoryTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val dispatcherProvider = object : DispatcherProvider {
        override val io = Dispatchers.IO
        override val main = Dispatchers.Main
        override val default = Dispatchers.Default
    }
    private lateinit var audioDir: File
    private lateinit var repository: LocalAudioStorageRepository

    @Before
    fun setUp() {
        audioDir = File(context.filesDir, "audio")
        audioDir.deleteRecursively()
        repository = LocalAudioStorageRepository(
            context = context,
            dispatchers = dispatcherProvider,
            mediaDownloader = FakeDownloader()
        )
    }

    @After
    fun tearDown() {
        audioDir.deleteRecursively()
    }

    @Test
    fun importFromDevice_writesFileWithDeviceOrigin() = runBlocking {
        val file = DeviceMediaFile(
            name = "dev.wav",
            sizeBytes = 10,
            mimeType = "audio/wav",
            modifiedAtMillis = 1_000L,
            mediaUrl = "media",
            downloadUrl = "http://example.com/dev.wav"
        )

        repository.importFromDevice("http://example.com", file)
        val stored = repository.audios.first().first()

        assertEquals("dev.wav", stored.displayName)
        assertEquals(AudioOrigin.DEVICE, stored.origin)
        assertTrue(File(stored.localUri.path!!).exists())
    }

    @Test
    fun importFromPhone_copiesFileWithPhoneOrigin() = runBlocking {
        val temp = File.createTempFile("phone", ".mp3", context.cacheDir)
        temp.writeText("test-audio")
        val uri = Uri.fromFile(temp)

        repository.importFromPhone(uri)
        val stored = repository.audios.first().first()

        assertEquals(AudioOrigin.PHONE, stored.origin)
        assertTrue(File(stored.localUri.path!!).exists())
    }

    @Test
    fun delete_removesFile() = runBlocking {
        val temp = File.createTempFile("to-delete", ".wav", context.cacheDir)
        val uri = Uri.fromFile(temp)
        val stored = repository.importFromPhone(uri)

        repository.delete(stored.id)
        val remaining = repository.audios.first()

        assertTrue(remaining.isEmpty())
    }

    private class FakeDownloader : DeviceMediaDownloader {
        override suspend fun download(baseUrl: String, file: DeviceMediaFile): Result<File> {
            val temp = File.createTempFile("device", ".wav")
            temp.writeText("sample")
            return Result.Success(temp)
        }
    }
}
