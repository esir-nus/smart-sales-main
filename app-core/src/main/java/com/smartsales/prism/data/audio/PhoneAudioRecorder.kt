package com.smartsales.prism.data.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 手机端 PCM WAV 录音工具 — DEBUG 专用 L2 测试
 *
 * 使用 AudioRecord (非 MediaRecorder) 直接录制 16kHz mono PCM，
 * 手动写 WAV 文件头，确保与 AsrService 输入规格完全兼容。
 *
 * @see AsrService — 要求 16kHz, mono, PCM WAV
 */
class PhoneAudioRecorder(private val context: Context) {

    companion object {
        private const val TAG = "PhoneRecorder"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var outputFile: File? = null

    @Volatile
    private var isCurrentlyRecording = false

    fun isRecording(): Boolean = isCurrentlyRecording

    /**
     * 开始录音 — 创建临时 WAV 文件并启动后台录音线程
     */
    fun startRecording(): File {
        val file = File(context.cacheDir, "asr_recording_${System.currentTimeMillis()}.wav")
        outputFile = file
        Log.d(TAG, "🎙️ Starting recording → $file")

        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)

        @Suppress("MissingPermission") // 调用方负责权限检查
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize
        )

        audioRecord?.startRecording()
        isCurrentlyRecording = true

        // 后台线程持续写入 PCM 数据
        recordingThread = Thread {
            writeWavFile(file, bufferSize)
        }.apply {
            name = "PhoneAudioRecorder"
            start()
        }

        return file
    }

    /**
     * 停止录音 — 返回完成的 WAV 文件
     */
    fun stopRecording(): File {
        Log.d(TAG, "⏹️ Stopping recording")
        isCurrentlyRecording = false

        recordingThread?.join(2000) // 等待写入线程结束
        recordingThread = null

        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        val file = outputFile!!
        Log.d(TAG, "✅ Recording saved: ${file.length()} bytes")
        return file
    }

    /**
     * 取消录音 — 丢弃文件
     */
    fun cancel() {
        Log.d(TAG, "🗑️ Recording cancelled")
        isCurrentlyRecording = false

        recordingThread?.join(2000)
        recordingThread = null

        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        outputFile?.delete()
        outputFile = null
    }

    /**
     * 写入 WAV 文件：先写占位文件头，再追加 PCM 数据，最后回填文件头大小字段
     */
    private fun writeWavFile(file: File, bufferSize: Int) {
        val buffer = ByteArray(bufferSize)

        FileOutputStream(file).use { fos ->
            // 1. 写占位 WAV 文件头 (44 字节)
            fos.write(ByteArray(44))

            var totalPcmBytes = 0

            // 2. 持续写入 PCM 数据
            while (isCurrentlyRecording) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                if (read > 0) {
                    fos.write(buffer, 0, read)
                    totalPcmBytes += read
                }
            }

            fos.flush()

            // 3. 回填 WAV 文件头
            patchWavHeader(file, totalPcmBytes)
        }
    }

    /**
     * 回填 WAV 文件头 — 标准 RIFF/WAVE PCM 格式
     *
     * Layout:
     * [0-3]   "RIFF"
     * [4-7]   fileSize - 8
     * [8-11]  "WAVE"
     * [12-15] "fmt "
     * [16-19] 16 (PCM子块大小)
     * [20-21] 1 (PCM格式)
     * [22-23] 1 (单声道)
     * [24-27] 16000 (采样率)
     * [28-31] 32000 (字节率 = 采样率 × 声道 × 位深/8)
     * [32-33] 2 (块对齐 = 声道 × 位深/8)
     * [34-35] 16 (位深)
     * [36-39] "data"
     * [40-43] pcmDataSize
     */
    private fun patchWavHeader(file: File, pcmDataSize: Int) {
        RandomAccessFile(file, "rw").use { raf ->
            val totalFileSize = pcmDataSize + 44
            val byteRate = SAMPLE_RATE * 1 * 16 / 8  // 32000
            val blockAlign = 1 * 16 / 8               // 2

            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
                // RIFF chunk
                put("RIFF".toByteArray())
                putInt(totalFileSize - 8)
                put("WAVE".toByteArray())

                // fmt sub-chunk
                put("fmt ".toByteArray())
                putInt(16)                    // PCM 子块大小
                putShort(1)                   // PCM 格式
                putShort(1)                   // 单声道
                putInt(SAMPLE_RATE)           // 采样率
                putInt(byteRate)              // 字节率
                putShort(blockAlign.toShort()) // 块对齐
                putShort(16)                  // 位深

                // data sub-chunk
                put("data".toByteArray())
                putInt(pcmDataSize)
            }

            raf.seek(0)
            raf.write(header.array())
        }

        Log.d(TAG, "📝 WAV header patched: pcmSize=$pcmDataSize, totalFile=${file.length()}")
    }
}
