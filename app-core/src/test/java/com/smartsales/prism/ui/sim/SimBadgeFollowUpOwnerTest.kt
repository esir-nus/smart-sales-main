package com.smartsales.prism.ui.sim

import com.smartsales.core.telemetry.PipelineValve
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SimBadgeFollowUpOwnerTest {

    @Test
    fun `startBadgeSchedulerFollowUp creates metadata only state`() {
        val owner = SimBadgeFollowUpOwner()

        owner.startBadgeSchedulerFollowUp(
            boundSessionId = "session_1",
            threadId = "thread_1",
            initialSurface = SimBadgeFollowUpSurface.SHELL
        )

        val state = owner.activeFollowUp.value
        requireNotNull(state)

        assertEquals("session_1", state.boundSessionId)
        assertEquals("thread_1", state.threadId)
        assertEquals(SimBadgeFollowUpOrigin.BADGE, state.origin)
        assertEquals(SimBadgeFollowUpLane.SCHEDULER, state.lane)
        assertEquals(SimBadgeFollowUpSurface.SHELL, state.lastActiveSurface)
    }

    @Test
    fun `startBadgeSchedulerFollowUp replaces previous binding cleanly`() {
        val owner = SimBadgeFollowUpOwner()

        owner.startBadgeSchedulerFollowUp("session_1")
        val firstThreadId = owner.activeFollowUp.value?.threadId

        owner.startBadgeSchedulerFollowUp(
            boundSessionId = "session_2",
            initialSurface = SimBadgeFollowUpSurface.CONNECTIVITY
        )

        val state = owner.activeFollowUp.value
        requireNotNull(state)

        assertEquals("session_2", state.boundSessionId)
        assertEquals(SimBadgeFollowUpSurface.CONNECTIVITY, state.lastActiveSurface)
        assertTrue(firstThreadId != state.threadId)
    }

    @Test
    fun `markSurface updates active surface`() {
        val owner = SimBadgeFollowUpOwner()

        owner.startBadgeSchedulerFollowUp("session_1")
        owner.markSurface(SimBadgeFollowUpSurface.CONNECTIVITY)

        assertEquals(
            SimBadgeFollowUpSurface.CONNECTIVITY,
            owner.activeFollowUp.value?.lastActiveSurface
        )
    }

    @Test
    fun `clear removes active follow up`() {
        val owner = SimBadgeFollowUpOwner()

        owner.startBadgeSchedulerFollowUp("session_1")
        owner.clear(SimBadgeFollowUpClearReason.NEW_SESSION)

        assertNull(owner.activeFollowUp.value)
    }

    @Test
    fun `owner telemetry uses UI state emitted summaries`() {
        val owner = SimBadgeFollowUpOwner()
        val summaries = mutableListOf<String>()

        PipelineValve.testInterceptor = { checkpoint, _, summary ->
            summaries += "${checkpoint.name}:$summary"
        }

        try {
            owner.startBadgeSchedulerFollowUp("session_1")
            owner.markSurface(SimBadgeFollowUpSurface.SETTINGS)
            owner.clear(SimBadgeFollowUpClearReason.SESSION_DELETED)

            assertTrue(
                summaries.contains(
                    "UI_STATE_EMITTED:$SIM_BADGE_FOLLOW_UP_OWNER_STARTED_SUMMARY"
                )
            )
            assertTrue(
                summaries.contains(
                    "UI_STATE_EMITTED:$SIM_BADGE_FOLLOW_UP_OWNER_SURFACE_UPDATED_SUMMARY"
                )
            )
            assertTrue(
                summaries.contains(
                    "UI_STATE_EMITTED:$SIM_BADGE_FOLLOW_UP_OWNER_CLEARED_SUMMARY"
                )
            )
        } finally {
            PipelineValve.testInterceptor = null
        }
    }
}
