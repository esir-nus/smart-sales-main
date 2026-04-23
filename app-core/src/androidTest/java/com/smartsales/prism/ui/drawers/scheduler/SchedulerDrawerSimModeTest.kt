package com.smartsales.prism.ui.drawers.scheduler

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.smartsales.prism.data.notification.ReminderReliabilityAdvisor
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.scheduler.UrgencyLevel
import com.smartsales.prism.ui.drawers.SchedulerDrawer
import com.smartsales.prism.ui.drawers.SCHEDULER_EMPTY_GUIDE_CARD_TEST_TAG
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class SchedulerDrawerSimModeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun simModeHidesDeprecatedInspirationMultiSelectBar() {
        val viewModel = FakeSchedulerViewModel().apply {
            toggleSelectionMode(true)
            toggleItemSelection("insp_1")
        }

        renderSimDrawer(viewModel)

        composeTestRule.onNodeWithText("问AI (1)").assertDoesNotExist()
    }

    @Test
    fun defaultModeStillShowsInspirationMultiSelectBarWhenStateIsActive() {
        val viewModel = FakeSchedulerViewModel().apply {
            toggleSelectionMode(true)
            toggleItemSelection("insp_1")
        }

        composeTestRule.setContent {
            SchedulerDrawer(
                isOpen = true,
                onDismiss = {},
                viewModel = viewModel
            )
        }

        composeTestRule.onNodeWithText("问AI (1)").assertExists()
    }

    @Test
    fun simModeKeepsShelfAskAiAsOnlyInspirationLauncher() {
        val viewModel = FakeSchedulerViewModel().apply {
            toggleSelectionMode(true)
            toggleItemSelection("insp_1")
        }
        var capturedPrompt: String? = null

        renderSimDrawer(
            viewModel = viewModel,
            onInspirationAskAi = { capturedPrompt = it }
        )

        composeTestRule.onNodeWithText("问AI (1)").assertDoesNotExist()
        composeTestRule.onNodeWithText("灵感记录").assertExists()
        composeTestRule.onNodeWithText("灵感箱 (1)").assertDoesNotExist()
        composeTestRule.onNodeWithText("Ask AI").assertDoesNotExist()
        composeTestRule.onAllNodesWithContentDescription("Ask AI").assertCountEquals(1)
        composeTestRule.onNodeWithContentDescription("Ask AI").performClick()

        assertEquals("Follow up with Acme Corp about the Q3 renewal.", capturedPrompt)
    }

    @Test
    fun simModeEmptyStateShowsSchedulerRecordingGuideCard() {
        val viewModel = FakeSchedulerViewModel().apply {
            debugRunScenario("EMPTY")
        }

        renderSimDrawer(viewModel)

        composeTestRule.onNodeWithTag(SCHEDULER_EMPTY_GUIDE_CARD_TEST_TAG).assertExists()
        composeTestRule.onNodeWithText("用工牌录音创建日程").assertExists()
        composeTestRule.onNodeWithText("长按工牌录音键，说出你的待办。处理完成后，日程会显示在这里。").assertExists()
    }

    @Test
    fun visibleDayRescheduleExitMotion_rendersThenClears() {
        composeTestRule.mainClock.autoAdvance = false

        val snapshot = ScheduledTask(
            id = "task_exit_visible",
            timeDisplay = "10:00",
            title = "Visible motion task",
            urgencyLevel = UrgencyLevel.L2_IMPORTANT,
            startTime = Instant.parse("2026-03-21T02:00:00Z"),
            durationMinutes = 60
        )
        val viewModel = FakeSchedulerViewModel().apply {
            debugSetActiveDayOffset(0)
            debugSetTimelineItems(emptyList())
            debugSetDateAttention(emptySet(), emptySet())
            debugSetExitingTasks(
                listOf(
                    RescheduleExitMotion(
                        renderKey = "task_exit_visible:exit",
                        sourceTaskId = snapshot.id,
                        sourceDayOffset = 0,
                        snapshot = snapshot,
                        exitDirection = ExitDirection.RIGHT
                    )
                )
            )
        }

        renderSimDrawer(viewModel)

        composeTestRule.onNodeWithText("Visible motion task").assertExists()

        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Visible motion task").assertDoesNotExist()
        composeTestRule.mainClock.autoAdvance = true
    }

    @Test
    fun offPageRescheduleMotionUsesDateAttentionInsteadOfVisibleSourceCard() {
        val today = LocalDate.now()
        val targetOffset = if (today.dayOfMonth % 7 == 0) -1 else 1
        val targetDay = today.dayOfMonth + targetOffset

        val snapshot = ScheduledTask(
            id = "task_exit_off_page",
            timeDisplay = "10:00",
            title = "Off page motion task",
            urgencyLevel = UrgencyLevel.L2_IMPORTANT,
            startTime = Instant.parse("2026-03-21T02:00:00Z"),
            durationMinutes = 60
        )
        val viewModel = FakeSchedulerViewModel().apply {
            debugSetActiveDayOffset(0)
            debugSetTimelineItems(emptyList())
            debugSetDateAttention(
                unacknowledgedDates = setOf(targetOffset),
                rescheduledDates = setOf(targetOffset)
            )
            debugSetExitingTasks(
                listOf(
                    RescheduleExitMotion(
                        renderKey = "task_exit_off_page:exit",
                        sourceTaskId = snapshot.id,
                        sourceDayOffset = targetOffset,
                        snapshot = snapshot,
                        exitDirection = ExitDirection.RIGHT
                    )
                )
            )
        }

        renderSimDrawer(viewModel)

        composeTestRule.onNodeWithText("Off page motion task").assertDoesNotExist()
        composeTestRule.onNode(
            hasTextExactly(targetDay.toString()) and hasDateAttentionKind("warning")
        ).assertExists()
    }

    @Test
    fun exactAlarmPromptShowsGuideAndRoutesPrimaryAction() {
        val viewModel = FakeSchedulerViewModel()
        val openedActions = mutableListOf<ReminderReliabilityAdvisor.Action>()
        val guide = ReminderReliabilityAdvisor.ReminderReliabilityGuide(
            title = "精确闹钟权限",
            message = "未授予精确闹钟权限时，提醒可能延迟。",
            checklist = listOf("开启闹钟和提醒权限"),
            primaryAction = ReminderReliabilityAdvisor.Action.EXACT_ALARM,
            primaryLabel = "闹钟权限",
            secondaryAction = ReminderReliabilityAdvisor.Action.APP_NOTIFICATION_SETTINGS,
            secondaryLabel = "通知设置"
        )

        composeTestRule.setContent {
            SchedulerDrawer(
                isOpen = true,
                onDismiss = {},
                visualMode = SchedulerDrawerVisualMode.SIM,
                enableInspirationMultiSelect = false,
                viewModel = viewModel,
                reminderGuideProvider = { guide },
                reminderActionOpener = { _, action ->
                    openedActions += action
                    true
                }
            )
        }

        composeTestRule.runOnIdle {
            viewModel.debugEmitExactAlarmPermissionNeeded()
        }

        composeTestRule.onNodeWithText("精确闹钟权限").assertExists()
        composeTestRule.onNodeWithText("闹钟权限").performClick()

        assertEquals(
            listOf(ReminderReliabilityAdvisor.Action.EXACT_ALARM),
            openedActions
        )
        composeTestRule.onNodeWithText("精确闹钟权限").assertDoesNotExist()
    }

    @Test
    fun simModeStartOnlyTaskHidesTrailingEllipsisTime() {
        val start = LocalDateTime.of(2026, 3, 26, 17, 0).atZone(ZoneId.systemDefault()).toInstant()
        val viewModel = FakeSchedulerViewModel().apply {
            debugSetTimelineItems(
                listOf(
                    ScheduledTask(
                        id = "task_start_only_sim",
                        timeDisplay = "17:00 - ...",
                        title = "SIM start-only task",
                        startTime = start,
                        endTime = null,
                        durationMinutes = 0
                    )
                )
            )
        }

        renderSimDrawer(viewModel)

        composeTestRule.onNodeWithText("17:00 - ...").assertDoesNotExist()
        composeTestRule.onAllNodesWithText("17:00").assertCountEquals(2)
    }

    @Test
    fun simModeDoneTaskKeepsCollapsedCardAndDoesNotOpenExpandedDetails() {
        val doneTask = ScheduledTask(
            id = "task_done_sim",
            timeDisplay = "09:00",
            title = "Finished follow-up call",
            startTime = LocalDateTime.of(2026, 3, 26, 9, 0).atZone(ZoneId.systemDefault()).toInstant(),
            durationMinutes = 30,
            isDone = true,
            notes = "This note should stay hidden for done cards."
        )
        val viewModel = FakeSchedulerViewModel().apply {
            debugSetTimelineItems(listOf(doneTask))
        }

        renderSimDrawer(viewModel)

        composeTestRule.onNodeWithText("Finished follow-up call").performClick()
        composeTestRule.onNodeWithText("This note should stay hidden for done cards.").assertDoesNotExist()
    }

    @Test
    fun simModeExpandedDetailsRendersLocationInRealSimPath() {
        val task = ScheduledTask(
            id = "task_detail_sim",
            timeDisplay = "11:00 - 12:00",
            title = "Client lunch",
            startTime = LocalDateTime.of(2026, 3, 26, 11, 0).atZone(ZoneId.systemDefault()).toInstant(),
            endTime = LocalDateTime.of(2026, 3, 26, 12, 0).atZone(ZoneId.systemDefault()).toInstant(),
            durationMinutes = 60,
            location = "静安嘉里中心"
        )
        val viewModel = FakeSchedulerViewModel().apply {
            debugSetTimelineItems(listOf(task))
        }

        renderSimDrawer(viewModel)

        composeTestRule.onNodeWithText("Client lunch").performClick()
        composeTestRule.onNodeWithText("静安嘉里中心").assertExists()
    }

    @Test
    fun simModeExpandedNoteRendersUserNoteCard() {
        val task = ScheduledTask(
            id = "task_note_sim",
            timeDisplay = "13:00 - 14:00",
            title = "Prepare proposal",
            startTime = LocalDateTime.of(2026, 3, 26, 13, 0).atZone(ZoneId.systemDefault()).toInstant(),
            endTime = LocalDateTime.of(2026, 3, 26, 14, 0).atZone(ZoneId.systemDefault()).toInstant(),
            durationMinutes = 60,
            notes = "记得带上报价单和样机。"
        )
        val viewModel = FakeSchedulerViewModel().apply {
            debugSetTimelineItems(listOf(task))
        }

        renderSimDrawer(viewModel)

        composeTestRule.onNodeWithText("Prepare proposal").performClick()
        composeTestRule.onNodeWithText("用户备注").assertExists()
        composeTestRule.onNodeWithText("记得带上报价单和样机。").assertExists()
    }

    @Test
    fun simModeExpandedTipsRendersCachedTipsCard() {
        val task = ScheduledTask(
            id = "task_1",
            timeDisplay = "10:00 - 11:00",
            title = "Call John Doe re: pricing",
            startTime = LocalDateTime.of(2026, 3, 26, 10, 0).atZone(ZoneId.systemDefault()).toInstant(),
            endTime = LocalDateTime.of(2026, 3, 26, 11, 0).atZone(ZoneId.systemDefault()).toInstant(),
            durationMinutes = 60,
            hasAlarm = true,
            alarmCascade = listOf("-30m", "0m")
        )
        val viewModel = FakeSchedulerViewModel().apply {
            debugSetTimelineItems(listOf(task))
        }

        renderSimDrawer(viewModel)

        composeTestRule.onNodeWithText("Call John Doe re: pricing").performClick()
        composeTestRule.onNodeWithText("AI 提示").assertExists()
        composeTestRule.onNodeWithText("Mention Q3 budget").assertExists()
        composeTestRule.onNodeWithText("Confirm travel dates").assertExists()
    }

    @Test
    fun simModeConflictCardRendersCollapsedBannerAndExpandedDetails() {
        val task = ScheduledTask(
            id = "task_conflict_sim",
            timeDisplay = "14:00 - 15:00",
            title = "Channel partner sync",
            startTime = LocalDateTime.of(2026, 3, 26, 14, 0).atZone(ZoneId.systemDefault()).toInstant(),
            endTime = LocalDateTime.of(2026, 3, 26, 15, 0).atZone(ZoneId.systemDefault()).toInstant(),
            durationMinutes = 60,
            hasConflict = true,
            conflictSummary = "与「季度复盘」时间冲突",
            location = "8F 会议室"
        )
        val viewModel = FakeSchedulerViewModel().apply {
            debugSetTimelineItems(listOf(task))
            debugSetDateAttention(emptySet(), emptySet())
        }

        renderSimDrawer(viewModel)

        composeTestRule.onNodeWithText("与「季度复盘」时间冲突").assertExists()
        composeTestRule.onNodeWithText("Channel partner sync").performClick()
        composeTestRule.onNodeWithText("8F 会议室").assertExists()
    }

    @Test
    fun standardModePreservesStartOnlyEllipsisRailText() {
        val start = LocalDateTime.of(2026, 3, 26, 17, 0).atZone(ZoneId.systemDefault()).toInstant()
        val viewModel = FakeSchedulerViewModel().apply {
            debugSetTimelineItems(
                listOf(
                    ScheduledTask(
                        id = "task_start_only_standard",
                        timeDisplay = "17:00 - ...",
                        title = "Standard start-only task",
                        startTime = start,
                        endTime = null,
                        durationMinutes = 0
                    )
                )
            )
        }

        composeTestRule.setContent {
            SchedulerDrawer(
                isOpen = true,
                onDismiss = {},
                viewModel = viewModel
            )
        }

        composeTestRule.onNodeWithText("17:00 - ...").assertExists()
    }

    @Test
    fun simModeRendersBatchRowsForAggregateBatchResult() {
        val baseStart = LocalDateTime.of(2026, 3, 26, 14, 0).atZone(ZoneId.systemDefault()).toInstant()
        val viewModel = FakeSchedulerViewModel().apply {
            debugSetTimelineItems(
                listOf(
                    ScheduledTask(
                        id = "batch_task_1",
                        timeDisplay = "14:00",
                        title = "客户拜访 A",
                        startTime = baseStart,
                        durationMinutes = 60
                    ),
                    ScheduledTask(
                        id = "batch_task_2",
                        timeDisplay = "15:00",
                        title = "客户拜访 B",
                        startTime = baseStart.plusSeconds(3600),
                        durationMinutes = 60
                    ),
                    ScheduledTask(
                        id = "batch_task_3",
                        timeDisplay = "16:00",
                        title = "客户拜访 C",
                        startTime = baseStart.plusSeconds(7200),
                        durationMinutes = 60
                    )
                )
            )
            debugSetPipelineStatus("✅ 已创建 3 项")
        }

        renderSimDrawer(viewModel)

        composeTestRule.onNodeWithText("客户拜访 A").assertExists()
        composeTestRule.onNodeWithText("客户拜访 B").assertExists()
        composeTestRule.onNodeWithText("客户拜访 C").assertExists()
    }

    private fun hasDateAttentionKind(kind: String): SemanticsMatcher {
        return SemanticsMatcher.expectValue(SchedulerDateAttentionKindKey, kind)
    }

    private fun renderSimDrawer(
        viewModel: FakeSchedulerViewModel,
        onInspirationAskAi: ((String) -> Unit)? = null
    ) {
        composeTestRule.setContent {
            SchedulerDrawer(
                isOpen = true,
                onDismiss = {},
                visualMode = SchedulerDrawerVisualMode.SIM,
                onInspirationAskAi = onInspirationAskAi,
                enableInspirationMultiSelect = false,
                viewModel = viewModel
            )
        }
    }
}
