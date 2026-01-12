package com.smartsales.data.aicore.util

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.File
import java.nio.ByteBuffer

// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/util/TranscodeSlicer.kt
// 模块：:data:ai-core
// 说明：为不支持直接 Mux 的格式（MP3、WAV）提供转码切片能力。
//       流程：Decode → PCM → Encode AAC → Mux M4A

/**
 * Transcodes audio files to AAC/M4A format while slicing to specified time range.
 * Used as fallback when [AudioSlicer] direct muxing fails for formats like MP3.
 */
open class TranscodeSlicer(
    private val tempDir: File
) {
    companion object {
        private const val TAG = "TranscodeSlicer"
        private const val TIMEOUT_US = 10_000L
        private const val OUTPUT_MIME = MediaFormat.MIMETYPE_AUDIO_AAC
        private const val OUTPUT_BIT_RATE = 128_000
        private const val OUTPUT_AAC_PROFILE = MediaCodecInfo.CodecProfileLevel.AACObjectLC
    }

    /**
     * Transcode and slice audio file to specified time range.
     *
     * @param source Source audio file (MP3, WAV, etc.)
     * @param requestedStartMs Start time in milliseconds
     * @param endMs End time in milliseconds
     * @param windowKey Unique key for output file naming
     * @return SliceOutcome with result or error
     */
    open fun transcodeSlice(
        source: File,
        requestedStartMs: Long,
        endMs: Long,
        windowKey: String
    ): SliceOutcome {
        if (!source.exists() || source.length() == 0L) {
            return SliceOutcome.Failure(SliceError.SourceNotFound(source.absolutePath))
        }
        if (requestedStartMs < 0 || endMs <= requestedStartMs) {
            return SliceOutcome.Failure(SliceError.InvalidRange(requestedStartMs, endMs))
        }
        if (!tempDir.exists() && !tempDir.mkdirs()) {
            return SliceOutcome.Failure(SliceError.IoFailure("failed to create temp dir"))
        }

        val outputFile = File(tempDir, "${sanitizeFileName(windowKey)}.m4a")
        var extractor: MediaExtractor? = null
        var decoder: MediaCodec? = null
        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var muxerStarted = false
        var shouldDeleteOutput = true

        try {
            Log.d(TAG, "transcodeSlice: source=${source.absolutePath} range=${requestedStartMs}-${endMs}ms")
            // 1. Setup extractor
            extractor = MediaExtractor().apply { setDataSource(source.absolutePath) }
            val audioTrackIndex = findAudioTrack(extractor)
                ?: return SliceOutcome.Failure(SliceError.UnsupportedFormat)
            extractor.selectTrack(audioTrackIndex)
            val inputFormat = extractor.getTrackFormat(audioTrackIndex)

            val inputMime = inputFormat.getString(MediaFormat.KEY_MIME)
                ?: return SliceOutcome.Failure(SliceError.UnsupportedFormat)
            val sampleRate = inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channelCount = inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

            // 2. Setup decoder
            decoder = MediaCodec.createDecoderByType(inputMime)
            decoder.configure(inputFormat, null, null, 0)
            decoder.start()

            // 3. Setup encoder (AAC output)
            val outputFormat = MediaFormat.createAudioFormat(OUTPUT_MIME, sampleRate, channelCount).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, OUTPUT_BIT_RATE)
                setInteger(MediaFormat.KEY_AAC_PROFILE, OUTPUT_AAC_PROFILE)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384) // Required for some devices
            }
            Log.d(TAG, "transcodeSlice: inputMime=$inputMime sampleRate=$sampleRate channels=$channelCount")
            encoder = MediaCodec.createEncoderByType(OUTPUT_MIME)
            encoder.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()

            // 4. Setup muxer (will be started after encoder output format is available)
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var muxerTrackIndex = -1

            // Seek to start position
            val startUs = requestedStartMs * 1000L
            val endUs = endMs * 1000L
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            // Buffers
            val bufferInfo = MediaCodec.BufferInfo()
            var inputDone = false
            var decoderDone = false
            var encoderDone = false
            var firstPresentationTimeUs: Long? = null
            var lastPresentationTimeUs: Long? = null
            var samplesWritten = 0

            // Main transcode loop
            while (!encoderDone) {
                // Feed extractor → decoder
                if (!inputDone) {
                    val inputBufferIndex = decoder.dequeueInputBuffer(TIMEOUT_US)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = decoder.getInputBuffer(inputBufferIndex)!!
                        val sampleTime = extractor.sampleTime
                        
                        if (sampleTime < 0 || sampleTime > endUs) {
                            // End of input or past end time
                            decoder.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            val sampleSize = extractor.readSampleData(inputBuffer, 0)
                            if (sampleSize < 0) {
                                decoder.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputDone = true
                            } else {
                                decoder.queueInputBuffer(inputBufferIndex, 0, sampleSize, sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }
                }

                // Decoder → encoder (PCM transfer)
                if (!decoderDone) {
                    val decoderOutputIndex = decoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                    when {
                        decoderOutputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            // Decoder output format changed, continue
                        }
                        decoderOutputIndex >= 0 -> {
                            val decoderOutputBuffer = decoder.getOutputBuffer(decoderOutputIndex)!!
                            
                            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                                decoderDone = true
                                // Signal encoder EOS
                                val encoderInputIndex = encoder.dequeueInputBuffer(TIMEOUT_US)
                                if (encoderInputIndex >= 0) {
                                    encoder.queueInputBuffer(encoderInputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                }
                            } else if (bufferInfo.size > 0) {
                                // Filter samples within requested range
                                val sampleTimeUs = bufferInfo.presentationTimeUs
                                if (sampleTimeUs >= startUs) {
                                    // Adjust presentation time relative to start
                                    if (firstPresentationTimeUs == null) {
                                        firstPresentationTimeUs = sampleTimeUs
                                    }
                                    val adjustedTimeUs = sampleTimeUs - firstPresentationTimeUs!!
                                    
                                    // Send to encoder
                                    val encoderInputIndex = encoder.dequeueInputBuffer(TIMEOUT_US)
                                    if (encoderInputIndex >= 0) {
                                        val encoderInputBuffer = encoder.getInputBuffer(encoderInputIndex)!!
                                        encoderInputBuffer.clear()
                                        encoderInputBuffer.put(decoderOutputBuffer)
                                        encoder.queueInputBuffer(
                                            encoderInputIndex,
                                            0,
                                            bufferInfo.size,
                                            adjustedTimeUs,
                                            0
                                        )
                                    }
                                }
                            }
                            decoder.releaseOutputBuffer(decoderOutputIndex, false)
                        }
                    }
                }

                // Encoder → muxer
                val encoderOutputIndex = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
                when {
                    encoderOutputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        // Start muxer with encoder output format
                        val newFormat = encoder.outputFormat
                        muxerTrackIndex = muxer.addTrack(newFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                    encoderOutputIndex >= 0 -> {
                        val encoderOutputBuffer = encoder.getOutputBuffer(encoderOutputIndex)!!
                        
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            encoderDone = true
                        } else if (bufferInfo.size > 0 && muxerStarted) {
                            muxer.writeSampleData(muxerTrackIndex, encoderOutputBuffer, bufferInfo)
                            lastPresentationTimeUs = bufferInfo.presentationTimeUs
                            samplesWritten++
                        }
                        encoder.releaseOutputBuffer(encoderOutputIndex, false)
                    }
                }
            }

            if (samplesWritten == 0) {
                return SliceOutcome.Failure(SliceError.NoSamplesInRange)
            }

            val actualStartMs = (firstPresentationTimeUs ?: 0L) / 1000L
            val durationMs = ((lastPresentationTimeUs ?: 0L) - (firstPresentationTimeUs ?: 0L)) / 1000L

            shouldDeleteOutput = false
            return SliceOutcome.Success(
                SliceResult(
                    sliceFile = outputFile,
                    requestedCaptureStartMs = requestedStartMs,
                    actualCaptureStartMs = actualStartMs,
                    captureEndMs = endMs,
                    durationMs = durationMs.coerceAtLeast(0L),
                    windowKey = windowKey
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "transcodeSlice failed: ${e.message}", e)
            return SliceOutcome.Failure(SliceError.IoFailure("${e.javaClass.simpleName}: ${e.message}"))
        } finally {
            runCatching { decoder?.stop() }
            runCatching { decoder?.release() }
            runCatching { encoder?.stop() }
            runCatching { encoder?.release() }
            if (muxerStarted) {
                runCatching { muxer?.stop() }
            }
            runCatching { muxer?.release() }
            runCatching { extractor?.release() }
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

    private fun sanitizeFileName(raw: String): String =
        raw.replace(Regex("[^A-Za-z0-9._-]"), "_")
}
