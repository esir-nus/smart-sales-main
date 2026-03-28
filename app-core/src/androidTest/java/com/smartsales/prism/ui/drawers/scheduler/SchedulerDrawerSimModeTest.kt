package com.smartsales.prism.ui.drawers.scheduler

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.smartsales.prism.data.notification.ReminderReliabilityAdvisor
import com.smartsales.prism.domain.scheduler.ScheduledTask
import com.smartsales.prism.domain.scheduler.UrgencyLevel
import com.smartsales.prism.ui.drawers.SchedulerDrawer
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

        composeTestRule.setContent {
            SchedulerDrawer(
                isOpen = true,
                onDismiss = {},
                enableInspirationMultiSelect = false,
                viewModel = viewModel
            )
        }

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

        composeTestRule.setContent {
            SchedulerDrawer(
                isOpen = true,
                onDismiss = {},
                onInspirationAskAi = { capturedPrompt = it },
                enableInspirationMultiSelect = false,
                viewModel = viewModel
            )
        }

        composeTestRule.onNodeWithText("问AI (1)").assertDoesNotExist()
        composeTestRule.onAllNodesWithText("Ask AI").assertCountEquals(1)
        composeTestRule.onNodeWithText("Ask AI").performClick()

        assertEquals("Follow up with Acme Corp about the Q3 renewal.", capturedPrompt)
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

        composeTestRule.setContent {
            SchedulerDrawer(
                isOpen = true,
                onDismiss = {},
                viewModel = viewModel
            )
        }

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

        composeTestRule.setContent {
            SchedulerDrawer(
                isOpen = true,
                onDismiss = {},
                viewModel = viewModel
            )
        }

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

        composeTestRule.setContent {
            SchedulerDrawer(
                isOpen = true,
                onDismiss = {},
                visualMode = SchedulerDrawerVisualMode.SIM,
                enableInspirationMultiSelect = false,
                viewModel = viewModel
            )
        }

        composeTestRule.onNodeWithText("17:00 - ...").assertDoesNotExist()
        composeTestRule.onAllNodesWithText("17:00").assertCountEquals(2)
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

    private fun hasDateAttentionKind(kind: String): SemanticsMatcher {
        return SemanticsMatcher.expectValue(SchedulerDateAttentionKindKey, kind)
    }
}
