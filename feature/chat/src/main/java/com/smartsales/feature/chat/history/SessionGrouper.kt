package com.smartsales.feature.chat.history

import com.smartsales.feature.chat.home.SessionListItemUi

/**
 * Logic extracted from ChatHistoryViewModel to group sessions by time buckets.
 * Legacy Logic: "7天内", "30天内", "更早".
 */
object SessionGrouper {
    
    data class GroupedSession(
        val label: String,
        val items: List<SessionListItemUi>
    )

    fun groupSessions(
        sessions: List<SessionListItemUi>,
        nowMillis: Long = System.currentTimeMillis()
    ): List<GroupedSession> {
        val sevenDays = 7L * 24 * 60 * 60 * 1000
        val thirtyDays = 30L * 24 * 60 * 60 * 1000

        return sessions
            .sortedByDescending { it.updatedAtMillis } // Ensure sorted
            .groupBy { session ->
                val diff = (nowMillis - session.updatedAtMillis).coerceAtLeast(0)
                when {
                    diff <= sevenDays -> "7天内"
                    diff <= thirtyDays -> "30天内"
                    else -> "更早"
                }
            }
            .map { (label, items) ->
                GroupedSession(label, items)
            }
            .sortedBy { bucketOrder(it.label) }
    }

    private fun bucketOrder(label: String): Int = when (label) {
        "7天内" -> 0
        "30天内" -> 1
        "更早" -> 2
        else -> 3
    }
}
