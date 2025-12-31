package com.smartsales.aitest.audio

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteBuffer

// 文件：app/src/main/java/com/smartsales/aitest/audio/AudioSlicer.kt
// 模块：:app
// 说明：V1 音频切片工具（预热窗口），只做确定性裁切与复用编码轨。

data class SliceResult(
    val sliceFile: File,
    val requestedCaptureStartMs: Long,
    val actualCaptureStartMs: Long,
    val captureEndMs: Long,
    val durationMs: Long,
    val windowKey: String
)

sealed class SliceError(val reasonCode: String, val detail: String? = null) {
    data class SourceNotFound(val path: String) : SliceError("source_not_found", path)
    data class InvalidRange(val startMs: Long, val endMs: Long) : SliceError("invalid_range", "$startMs-$endMs")
    data object UnsupportedFormat : SliceError("unsupported_format")
    data object NoSamplesInRange : SliceError("no_samples_in_range")
    data object SeekPastRequested : SliceError("seek_past_requested")
    data class IoFailure(val message: String?) : SliceError("io_failure", message)
}

sealed class SliceOutcome {
    data class Success(val result: SliceResult) : SliceOutcome()
    data class Failure(val error: SliceError) : SliceOutcome()
}

class AudioSlicer(
    private val tempDir: File
) {
    fun sliceAudio(
        source: File,
        requestedCaptureStartMs: Long,
        captureEndMs: Long,
        windowKey: String
    ): SliceOutcome {
        if (!source.exists() || source.length() == 0L) {
            return SliceOutcome.Failure(SliceError.SourceNotFound(source.absolutePath))
        }
        if (requestedCaptureStartMs < 0 || captureEndMs <= requestedCaptureStartMs) {
            return SliceOutcome.Failure(SliceError.InvalidRange(requestedCaptureStartMs, captureEndMs))
        }
        if (!tempDir.exists() && !tempDir.mkdirs()) {
            return SliceOutcome.Failure(SliceError.IoFailure("failed to create temp dir"))
        }
        val outputFile = File(tempDir, "${sanitizeFileName(windowKey)}.m4a")
        var shouldDeleteOutput = true
        var extractor: MediaExtractor? = null
        var muxerRef: MediaMuxer? = null
        var muxerStarted = false
        try {
            extractor = MediaExtractor().apply { setDataSource(source.absolutePath) }
            val audioTrackIndex = findAudioTrack(extractor) ?: return SliceOutcome.Failure(
                // 说明：找不到音轨通常意味着格式不受支持，按确定性失败处理。
                SliceError.UnsupportedFormat
            )
            extractor.selectTrack(audioTrackIndex)
            val format = extractor.getTrackFormat(audioTrackIndex)
            val muxer = try {
                MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            } catch (_: Exception) {
                // 说明：Muxer 构造失败，视为格式不受支持（确定性失败）。
                return SliceOutcome.Failure(SliceError.UnsupportedFormat)
            }
            muxerRef = muxer
            val muxerTrackIndex = try {
                muxer.addTrack(format)
            } catch (_: Exception) {
                // 说明：Muxer 初始化失败，视为格式不受支持（确定性失败）。
                return SliceOutcome.Failure(SliceError.UnsupportedFormat)
            }
            try {
                muxer.start()
            } catch (_: Exception) {
                // 说明：Muxer 启动失败，视为格式不受支持（确定性失败）。
                return SliceOutcome.Failure(SliceError.UnsupportedFormat)
            }
            muxerStarted = true

            val startUs = requestedCaptureStartMs * 1000L
            val endUs = captureEndMs * 1000L
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            val maxInputSize = format.getIntegerOrNull(MediaFormat.KEY_MAX_INPUT_SIZE) ?: (256 * 1024)
            val buffer = ByteBuffer.allocate(maxInputSize)
            val bufferInfo = MediaCodec.BufferInfo()

            var firstSampleTimeUs: Long? = null
            var lastSampleTimeUs: Long? = null
            while (true) {
                val sampleTimeUs = extractor.sampleTime
                if (sampleTimeUs < 0) break
                if (sampleTimeUs > endUs) break
                val size = extractor.readSampleData(buffer, 0)
                if (size < 0) break
                if (firstSampleTimeUs == null) {
                    firstSampleTimeUs = sampleTimeUs
                    // 说明：采样对齐会导致实际切片起点早于请求起点（只允许向前对齐）。
                    if (firstSampleTimeUs > startUs) {
                        return SliceOutcome.Failure(SliceError.SeekPastRequested)
                    }
                }
                bufferInfo.offset = 0
                bufferInfo.size = size
                bufferInfo.flags = extractor.sampleFlags
                bufferInfo.presentationTimeUs = sampleTimeUs - (firstSampleTimeUs ?: sampleTimeUs)
                muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                lastSampleTimeUs = sampleTimeUs
                extractor.advance()
            }
            val actualStartUs = firstSampleTimeUs ?: return SliceOutcome.Failure(SliceError.NoSamplesInRange)
            val actualEndUs = lastSampleTimeUs ?: actualStartUs
            val durationMs = ((actualEndUs - actualStartUs).coerceAtLeast(0L)) / 1000L
            shouldDeleteOutput = false
            return SliceOutcome.Success(
                SliceResult(
                    sliceFile = outputFile,
                    requestedCaptureStartMs = requestedCaptureStartMs,
                    actualCaptureStartMs = actualStartUs / 1000L,
                    captureEndMs = captureEndMs,
                    durationMs = durationMs,
                    windowKey = windowKey
                )
            )
        } catch (exception: Exception) {
            return SliceOutcome.Failure(SliceError.IoFailure(exception.message))
        } finally {
            try {
                extractor?.release()
            } catch (_: Exception) {
            }
            if (muxerStarted) {
            try {
                muxerRef?.stop()
            } catch (_: Exception) {
            }
        }
        try {
            muxerRef?.release()
        } catch (_: Exception) {
        }
            if (shouldDeleteOutput) {
                runCatching { outputFile.delete() }
            }
        }
    }

    private fun findAudioTrack(extractor: MediaExtractor): Int? {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) {
                return i
            }
        }
        return null
    }

    private fun MediaFormat.getIntegerOrNull(key: String): Int? = runCatching { getInteger(key) }.getOrNull()

    private fun sanitizeFileName(raw: String): String =
        raw.replace(Regex("[^A-Za-z0-9._-]"), "_")
}
