package com.smartsales.prism.ui.sim

import com.smartsales.prism.domain.model.SessionPreview
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SimHistoryGroupingTest {

    private val zoneId: ZoneId = ZoneId.systemDefault()
    private val today: LocalDate = LocalDate.of(2026, 3, 24)
    private val nowMillis: Long = today.atTime(12, 0).atZone(zoneId).toInstant().toEpochMilli()

    @Test
    fun `groupSimHistorySessions separates pinned today last thirty days and monthly archives`() {
        val grouped = groupSimHistorySessions(
            previews = listOf(
                preview("pinned_old", 10, pinned = true),
                preview("today_new", 0, hour = 11),
                preview("today_old", 0, hour = 9),
                preview("month_mid", 3),
                preview("month_edge", 30),
                preview("feb_new", 40),
                preview("feb_old", 45),
                preview("sep_old", 180)
            ),
            nowMillis = nowMillis,
            zoneId = zoneId
        )

        assertEquals(
            listOf(
                SIM_HISTORY_GROUP_PINNED,
                SIM_HISTORY_GROUP_TODAY,
                SIM_HISTORY_GROUP_LAST_30_DAYS,
                "2026-02",
                "2025-09"
            ),
            grouped.keys.toList()
        )
        assertEquals(listOf("pinned_old"), grouped.getValue(SIM_HISTORY_GROUP_PINNED).map { it.id })
        assertEquals(
            listOf("today_new", "today_old"),
            grouped.getValue(SIM_HISTORY_GROUP_TODAY).map { it.id }
        )
        assertEquals(
            listOf("month_mid", "month_edge"),
            grouped.getValue(SIM_HISTORY_GROUP_LAST_30_DAYS).map { it.id }
        )
        assertEquals(listOf("feb_new", "feb_old"), grouped.getValue("2026-02").map { it.id })
        assertEquals(listOf("sep_old"), grouped.getValue("2025-09").map { it.id })
    }

    @Test
    fun `groupSimHistorySessions hides empty groups`() {
        val grouped = groupSimHistorySessions(
            previews = listOf(preview("today_only", 0)),
            nowMillis = nowMillis,
            zoneId = zoneId
        )

        assertEquals(listOf(SIM_HISTORY_GROUP_TODAY), grouped.keys.toList())
        assertTrue(SIM_HISTORY_GROUP_PINNED !in grouped)
        assertTrue(SIM_HISTORY_GROUP_LAST_30_DAYS !in grouped)
    }

    private fun preview(
        id: String,
        daysAgo: Long,
        hour: Int = 10,
        pinned: Boolean = false
    ): SessionPreview {
        val timestamp = today
            .minusDays(daysAgo)
            .atTime(hour, 0)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()

        return SessionPreview(
            id = id,
            clientName = id,
            summary = "summary",
            timestamp = timestamp,
            isPinned = pinned
        )
    }
}
