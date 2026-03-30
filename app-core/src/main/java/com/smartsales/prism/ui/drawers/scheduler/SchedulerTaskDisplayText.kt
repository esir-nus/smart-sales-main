package com.smartsales.prism.ui.drawers.scheduler

import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val SimSchedulerRailTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val SimSchedulerDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

internal fun TimelineItem.Task.simTimelineRailLabel(): String {
    return when {
        isVague -> "待定"
        isDone -> timeDisplay
        sortInstant != null -> sortInstant.atZone(ZoneId.systemDefault()).format(SimSchedulerRailTimeFormatter)
        else -> timeDisplay.substringBefore(" - ").ifBlank { timeDisplay }
    }
}

internal fun TimelineItem.Task.simCollapsedTimeLabel(): String = simTimelineRailLabel()

internal fun TimelineItem.Task.simCardTimeSummary(): String {
    val cleaned = timeDisplay.replace(" - ...", "").trim()
    return when {
        isVague -> "时间待定"
        cleaned.isNotBlank() -> cleaned
        else -> simCollapsedTimeLabel()
    }
}

internal fun TimelineItem.Task.simDetailDateLabel(): String {
    val zonedStart = sortInstant?.atZone(ZoneId.systemDefault())
    return when {
        isVague && zonedStart != null -> "${zonedStart.format(SimSchedulerDateFormatter)} · 时间待定"
        isVague -> "时间待定"
        zonedStart != null -> "${zonedStart.format(SimSchedulerDateFormatter)} · ${zonedStart.format(SimSchedulerRailTimeFormatter)}"
        else -> dateRange.ifBlank { simCollapsedTimeLabel() }
    }
}
