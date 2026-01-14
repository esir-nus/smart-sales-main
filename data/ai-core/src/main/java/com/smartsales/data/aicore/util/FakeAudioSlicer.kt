// File: data/ai-core/src/main/java/com/smartsales/data/aicore/util/FakeAudioSlicer.kt
// Module: :data:ai-core
// Summary: Fake implementation of Slicer for testing (Lattice pattern)
// Author: created on 2026-01-14

package com.smartsales.data.aicore.util

import java.io.File

/**
 * FakeAudioSlicer: Test double for Slicer.
 */
class FakeAudioSlicer : Slicer {
    
    /** Override to inject specific outcome. */
    var stubOutcome: SliceOutcome? = null
    
    /** Track all slice calls. */
    val calls = mutableListOf<String>()
    
    override fun sliceAudio(
        source: File,
        requestedCaptureStartMs: Long,
        captureEndMs: Long,
        windowKey: String
    ): SliceOutcome {
        calls.add("$windowKey:${requestedCaptureStartMs}-${captureEndMs}")
        
        return stubOutcome ?: SliceOutcome.Success(
            SliceResult(
                sliceFile = File("/tmp/fake_slice_$windowKey.m4a"),
                requestedCaptureStartMs = requestedCaptureStartMs,
                actualCaptureStartMs = requestedCaptureStartMs,
                captureEndMs = captureEndMs,
                durationMs = captureEndMs - requestedCaptureStartMs,
                windowKey = windowKey
            )
        )
    }
    
    fun reset() {
        stubOutcome = null
        calls.clear()
    }
}
