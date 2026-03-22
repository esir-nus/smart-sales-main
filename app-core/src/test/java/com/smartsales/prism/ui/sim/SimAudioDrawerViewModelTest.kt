package com.smartsales.prism.ui.sim

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SimAudioDrawerViewModelTest {

    @Test
    fun `toggleExpandedAudioIds adds audio id when card is first opened`() {
        val expandedIds = toggleExpandedAudioIds(emptySet(), "audio-1")

        assertEquals(setOf("audio-1"), expandedIds)
    }

    @Test
    fun `toggleExpandedAudioIds removes audio id when card is collapsed again`() {
        val expandedIds = toggleExpandedAudioIds(setOf("audio-1"), "audio-1")

        assertEquals(emptySet<String>(), expandedIds)
    }

    @Test
    fun `canStartSimAudioSync blocks when auto sync already attempted`() {
        assertFalse(
            canStartSimAudioSync(
                hasAttemptedAutoSync = true,
                isSyncing = false
            )
        )
    }

    @Test
    fun `canStartSimAudioSync blocks when sync is already running`() {
        assertFalse(
            canStartSimAudioSync(
                hasAttemptedAutoSync = false,
                isSyncing = true
            )
        )
    }

    @Test
    fun `canStartSimAudioSync allows first idle sync attempt`() {
        assertTrue(
            canStartSimAudioSync(
                hasAttemptedAutoSync = false,
                isSyncing = false
            )
        )
    }

    @Test
    fun `shouldShowSimAudioAutoSyncMessage only returns true when imports were added`() {
        assertFalse(shouldShowSimAudioAutoSyncMessage(0))
        assertTrue(shouldShowSimAudioAutoSyncMessage(2))
    }
}
