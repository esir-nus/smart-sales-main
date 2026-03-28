package com.smartsales.prism.ui.drawers.scheduler.components

import com.smartsales.prism.domain.scheduler.UrgencyLevel
import com.smartsales.prism.ui.theme.AccentAmber
import com.smartsales.prism.ui.theme.AccentBlue
import com.smartsales.prism.ui.theme.AccentDanger
import com.smartsales.prism.ui.theme.TextMuted
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskCardIndicatorTest {

    @Test
    fun `urgency colors map to expected tokens`() {
        assertEquals(
            AccentDanger,
            taskCardIndicatorColor(UrgencyLevel.L1_CRITICAL, isDone = false)
        )
        assertEquals(
            AccentAmber,
            taskCardIndicatorColor(UrgencyLevel.L2_IMPORTANT, isDone = false)
        )
        assertEquals(
            AccentBlue,
            taskCardIndicatorColor(UrgencyLevel.L3_NORMAL, isDone = false)
        )
        assertEquals(
            TextMuted,
            taskCardIndicatorColor(UrgencyLevel.FIRE_OFF, isDone = false)
        )
    }

    @Test
    fun `done state keeps urgency color but de emphasizes it`() {
        assertEquals(
            AccentDanger.copy(alpha = 0.45f),
            taskCardIndicatorColor(UrgencyLevel.L1_CRITICAL, isDone = true)
        )
        assertEquals(
            AccentBlue.copy(alpha = 0.45f),
            taskCardIndicatorColor(UrgencyLevel.L3_NORMAL, isDone = true)
        )
    }
}
