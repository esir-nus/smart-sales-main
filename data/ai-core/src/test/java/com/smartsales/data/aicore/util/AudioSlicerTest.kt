package com.smartsales.data.aicore.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Unit tests for [AudioSlicer] input validation logic.
 * 
 * Note: Full integration tests with actual audio slicing require Android instrumentation
 * due to MediaExtractor/MediaMuxer APIs. These tests cover validation-only paths.
 */
class AudioSlicerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var slicer: AudioSlicer
    private lateinit var tempDir: File

    @Before
    fun setup() {
        tempDir = tempFolder.newFolder("slices")
        slicer = AudioSlicer(tempDir)
    }

    // --- Input Validation Tests ---

    @Test
    fun sliceAudio_sourceNotFound_returnsSourceNotFoundError() {
        val nonExistentFile = File(tempDir, "nonexistent.m4a")

        val result = slicer.sliceAudio(
            source = nonExistentFile,
            requestedCaptureStartMs = 0,
            captureEndMs = 60_000,
            windowKey = "test"
        )

        assertTrue(result is SliceOutcome.Failure)
        val error = (result as SliceOutcome.Failure).error
        assertTrue(error is SliceError.SourceNotFound)
        assertEquals("source_not_found", error.reasonCode)
    }

    @Test
    fun sliceAudio_emptyFile_returnsSourceNotFoundError() {
        val emptyFile = tempFolder.newFile("empty.m4a")
        // File exists but is 0 bytes

        val result = slicer.sliceAudio(
            source = emptyFile,
            requestedCaptureStartMs = 0,
            captureEndMs = 60_000,
            windowKey = "test"
        )

        assertTrue(result is SliceOutcome.Failure)
        val error = (result as SliceOutcome.Failure).error
        assertTrue(error is SliceError.SourceNotFound)
    }

    @Test
    fun sliceAudio_negativeStartTime_returnsInvalidRangeError() {
        val dummyFile = tempFolder.newFile("dummy.m4a")
        dummyFile.writeBytes(ByteArray(1024)) // Non-empty

        val result = slicer.sliceAudio(
            source = dummyFile,
            requestedCaptureStartMs = -1000,
            captureEndMs = 60_000,
            windowKey = "test"
        )

        assertTrue(result is SliceOutcome.Failure)
        val error = (result as SliceOutcome.Failure).error
        assertTrue(error is SliceError.InvalidRange)
        assertEquals("invalid_range", error.reasonCode)
    }

    @Test
    fun sliceAudio_endBeforeStart_returnsInvalidRangeError() {
        val dummyFile = tempFolder.newFile("dummy.m4a")
        dummyFile.writeBytes(ByteArray(1024))

        val result = slicer.sliceAudio(
            source = dummyFile,
            requestedCaptureStartMs = 60_000,
            captureEndMs = 30_000, // End before start
            windowKey = "test"
        )

        assertTrue(result is SliceOutcome.Failure)
        val error = (result as SliceOutcome.Failure).error
        assertTrue(error is SliceError.InvalidRange)
    }

    @Test
    fun sliceAudio_startEqualsEnd_returnsInvalidRangeError() {
        val dummyFile = tempFolder.newFile("dummy.m4a")
        dummyFile.writeBytes(ByteArray(1024))

        val result = slicer.sliceAudio(
            source = dummyFile,
            requestedCaptureStartMs = 60_000,
            captureEndMs = 60_000, // Same as start
            windowKey = "test"
        )

        assertTrue(result is SliceOutcome.Failure)
        val error = (result as SliceOutcome.Failure).error
        assertTrue(error is SliceError.InvalidRange)
    }

    // --- Data Class Tests ---

    @Test
    fun sliceResult_propertiesCorrect() {
        val file = File("/tmp/test.m4a")
        val result = SliceResult(
            sliceFile = file,
            requestedCaptureStartMs = 1000,
            actualCaptureStartMs = 950,
            captureEndMs = 60_000,
            durationMs = 59_050,
            windowKey = "batch_1"
        )

        assertEquals(file, result.sliceFile)
        assertEquals(1000L, result.requestedCaptureStartMs)
        assertEquals(950L, result.actualCaptureStartMs)
        assertEquals(60_000L, result.captureEndMs)
        assertEquals(59_050L, result.durationMs)
        assertEquals("batch_1", result.windowKey)
    }

    @Test
    fun sliceError_reasonCodes() {
        assertEquals("source_not_found", SliceError.SourceNotFound("/path").reasonCode)
        assertEquals("invalid_range", SliceError.InvalidRange(0, 100).reasonCode)
        assertEquals("unsupported_format", SliceError.UnsupportedFormat.reasonCode)
        assertEquals("no_samples_in_range", SliceError.NoSamplesInRange.reasonCode)
        assertEquals("seek_past_requested", SliceError.SeekPastRequested.reasonCode)
        assertEquals("io_failure", SliceError.IoFailure("test").reasonCode)
    }
}
