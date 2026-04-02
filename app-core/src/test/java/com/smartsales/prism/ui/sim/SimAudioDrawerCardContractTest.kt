package com.smartsales.prism.ui.sim

import com.smartsales.prism.domain.audio.AudioLocalAvailability
import com.smartsales.prism.ui.drawers.AudioStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SimAudioDrawerCardContractTest {

    @Test
    fun `right swipe transcribe is browse pending collapsed only`() {
        assertTrue(
            canSwipeRightToTranscribe(
                mode = RuntimeAudioDrawerMode.BROWSE,
                status = AudioStatus.PENDING,
                localAvailability = AudioLocalAvailability.READY,
                expanded = false
            )
        )
        assertFalse(
            canSwipeRightToTranscribe(
                mode = RuntimeAudioDrawerMode.BROWSE,
                status = AudioStatus.PENDING,
                localAvailability = AudioLocalAvailability.READY,
                expanded = true
            )
        )
        assertFalse(
            canSwipeRightToTranscribe(
                mode = RuntimeAudioDrawerMode.BROWSE,
                status = AudioStatus.TRANSCRIBED,
                localAvailability = AudioLocalAvailability.READY,
                expanded = false
            )
        )
        assertFalse(
            canSwipeRightToTranscribe(
                mode = RuntimeAudioDrawerMode.CHAT_RESELECT,
                status = AudioStatus.PENDING,
                localAvailability = AudioLocalAvailability.READY,
                expanded = false
            )
        )
        assertFalse(
            canSwipeRightToTranscribe(
                mode = RuntimeAudioDrawerMode.BROWSE,
                status = AudioStatus.PENDING,
                localAvailability = AudioLocalAvailability.QUEUED,
                expanded = false
            )
        )
    }

    @Test
    fun `left swipe delete is browse collapsed pending and transcribed only`() {
        assertTrue(
            canSwipeLeftToDelete(
                mode = RuntimeAudioDrawerMode.BROWSE,
                status = AudioStatus.PENDING,
                expanded = false
            )
        )
        assertTrue(
            canSwipeLeftToDelete(
                mode = RuntimeAudioDrawerMode.BROWSE,
                status = AudioStatus.TRANSCRIBED,
                expanded = false
            )
        )
        assertFalse(
            canSwipeLeftToDelete(
                mode = RuntimeAudioDrawerMode.BROWSE,
                status = AudioStatus.TRANSCRIBED,
                expanded = true
            )
        )
        assertFalse(
            canSwipeLeftToDelete(
                mode = RuntimeAudioDrawerMode.BROWSE,
                status = AudioStatus.TRANSCRIBING,
                expanded = false
            )
        )
        assertFalse(
            canSwipeLeftToDelete(
                mode = RuntimeAudioDrawerMode.CHAT_RESELECT,
                status = AudioStatus.PENDING,
                expanded = false
            )
        )
    }
}
