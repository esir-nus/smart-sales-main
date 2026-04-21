package com.smartsales.prism.service

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SchedulerPipelineOutcomeStore @Inject constructor() {

    private val missedOutcomes = ArrayDeque<String>()

    @Synchronized
    fun record(summary: String) {
        val normalized = summary.trim()
        if (normalized.isBlank()) return
        while (missedOutcomes.size >= MAX_MISSED_OUTCOMES) {
            missedOutcomes.removeFirst()
        }
        missedOutcomes.addLast(normalized)
    }

    @Synchronized
    fun consumeToastSummary(): String? {
        if (missedOutcomes.isEmpty()) return null
        val snapshot = missedOutcomes.toList()
        missedOutcomes.clear()
        return if (snapshot.size == 1) {
            snapshot.single()
        } else {
            buildString {
                append("Missed badge outcomes:")
                snapshot.forEach { line ->
                    append('\n')
                    append(line)
                }
            }
        }
    }

    private companion object {
        private const val MAX_MISSED_OUTCOMES = 5
    }
}
