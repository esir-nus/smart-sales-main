package com.smartsales.prism.ui.sim

import com.smartsales.prism.ui.drawers.AudioStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SimAudioDrawerCardContractTest {

    @Test
    fun `right swipe transcribe is browse pending collapsed only`() {
        assertTrue(
            canSwipeRightToTranscribe(
                mode = SimAudioDrawerMode.BROWSE,
                status = AudioStatus.PENDING,
                expanded = false
            )
        )
        assertFalse(
            canSwipeRightToTranscribe(
                mode = SimAudioDrawerMode.BROWSE,
                status = AudioStatus.PENDING,
                expanded = true
            )
        )
        assertFalse(
            canSwipeRightToTranscribe(
                mode = SimAudioDrawerMode.BROWSE,
                status = AudioStatus.TRANSCRIBED,
                expanded = false
            )
        )
        assertFalse(
            canSwipeRightToTranscribe(
                mode = SimAudioDrawerMode.CHAT_RESELECT,
                status = AudioStatus.PENDING,
                expanded = false
            )
        )
    }

    @Test
    fun `left swipe delete is browse collapsed pending and transcribed only`() {
        assertTrue(
            canSwipeLeftToDelete(
                mode = SimAudioDrawerMode.BROWSE,
                status = AudioStatus.PENDING,
                expanded = false
            )
        )
        assertTrue(
            canSwipeLeftToDelete(
                mode = SimAudioDrawerMode.BROWSE,
                status = AudioStatus.TRANSCRIBED,
                expanded = false
            )
        )
        assertFalse(
            canSwipeLeftToDelete(
                mode = SimAudioDrawerMode.BROWSE,
                status = AudioStatus.TRANSCRIBED,
                expanded = true
            )
        )
        assertFalse(
            canSwipeLeftToDelete(
                mode = SimAudioDrawerMode.BROWSE,
                status = AudioStatus.TRANSCRIBING,
                expanded = false
            )
        )
        assertFalse(
            canSwipeLeftToDelete(
                mode = SimAudioDrawerMode.CHAT_RESELECT,
                status = AudioStatus.PENDING,
                expanded = false
            )
        )
    }
}
