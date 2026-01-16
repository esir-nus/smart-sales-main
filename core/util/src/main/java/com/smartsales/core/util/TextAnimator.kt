package com.smartsales.core.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.math.max

/**
 * Utility for progressive text animation (typewriter effect).
 * Used to simulate streaming display for pre-fetched content.
 */
object TextAnimator {
    
    private const val TARGET_FPS = 60
    private const val FRAME_MS = 1000L / TARGET_FPS
    
    /**
     * Animates text over a fixed duration.
     * Good for transcripts where you want consistent reveal time regardless of length.
     * 
     * @param text The complete text to animate.
     * @param durationMs Target animation duration in milliseconds. Default 2500ms.
     */
    fun animateOverDuration(
        text: String, 
        durationMs: Long = 2500
    ): Flow<String> = flow {
        if (text.isEmpty()) {
            emit("")
            return@flow
        }

        val totalChars = text.length
        val totalFrames = max(1, durationMs / FRAME_MS)
        val charsPerFrame = max(1, (totalChars / totalFrames).toInt())
        
        var currentIndex = 0
        while (currentIndex < totalChars) {
            currentIndex = (currentIndex + charsPerFrame).coerceAtMost(totalChars)
            emit(text.substring(0, currentIndex))
            delay(FRAME_MS)
        }
    }
    
    /**
     * Animates text at a fixed character speed.
     * Good for short messages where duration should scale with length.
     * 
     * @param text The complete text to animate.
     * @param charsPerSecond Characters revealed per second. Default 80.
     */
    fun animateAtSpeed(
        text: String,
        charsPerSecond: Int = 80
    ): Flow<String> = flow {
        if (text.isEmpty()) {
            emit("")
            return@flow
        }
        
        val charsPerFrame = max(1, (charsPerSecond / TARGET_FPS.toFloat()).toInt())
        val totalChars = text.length
        
        var currentIndex = 0
        while (currentIndex < totalChars) {
            currentIndex = (currentIndex + charsPerFrame).coerceAtMost(totalChars)
            emit(text.substring(0, currentIndex))
            delay(FRAME_MS)
        }
    }
}
