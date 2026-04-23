package com.smartsales.prism.ui.drawers.scheduler.dev

data class SchedulerTestScenario(
    val id: String,
    val utterance: String,
    val displayedDateOffset: Int? = null,
    val expectedCommitCount: Int,
    val expectedConflictCount: Int = 0,
    val expectedStatus: String? = null
)

object SchedulerTestScenarios {
    val all: List<SchedulerTestScenario> = listOf(
        SchedulerTestScenario(
            id = "A1",
            utterance = "明天下午三点客户拜访",
            displayedDateOffset = 2,
            expectedCommitCount = 1,
            expectedStatus = null
        ),
        SchedulerTestScenario(
            id = "B1",
            utterance = "周二下午两点、三点、四点各加一个客户拜访",
            expectedCommitCount = 3,
            expectedStatus = "✅ 已创建 3 项"
        )
    )

    fun find(id: String): SchedulerTestScenario? = all.firstOrNull { it.id == id }
}
