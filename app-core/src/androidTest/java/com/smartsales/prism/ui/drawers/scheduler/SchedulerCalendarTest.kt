package com.smartsales.prism.ui.drawers.scheduler

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import java.time.format.DateTimeFormatter
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SchedulerCalendarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun calendarExpansion_verifiesObjectPermanence() {
        val today = LocalDate.now()
        val todayDay = today.dayOfMonth
        val weekStart = ((todayDay - 1) / 7) * 7 + 1
        val hiddenDay = if (weekStart > 1) weekStart - 1 else weekStart + 7

        composeTestRule.setContent {
            var isExpanded by remember { mutableStateOf(false) }
            var activeDay by remember { mutableStateOf(0) }
            SchedulerCalendar(
                isExpanded = isExpanded,
                onExpandChange = { isExpanded = it },
                activeDay = activeDay,
                onDateSelected = { activeDay = it }
            )
        }

        composeTestRule.onNode(hasTextExactly(todayDay.toString())).assertExists()
        composeTestRule.onNode(hasTextExactly(hiddenDay.toString())).assertDoesNotExist()
    }
    
    @Test
    fun calendarExpansion_stateTransition() {
        val today = LocalDate.now()
        val todayDay = today.dayOfMonth
        val weekStart = ((todayDay - 1) / 7) * 7 + 1
        val offWeekDay = if (weekStart > 1) weekStart - 1 else weekStart + 7

         composeTestRule.setContent {
            SchedulerCalendar(
                isExpanded = true,
                onExpandChange = {},
                activeDay = 0,
                onDateSelected = {}
            )
        }
        
        composeTestRule.onNode(hasTextExactly(todayDay.toString())).assertExists()
        composeTestRule.onAllNodes(hasTextExactly(offWeekDay.toString())).onFirst().assertExists()
    }

    @Test
    fun calendarDateAttention_exposesNormalSemantics() {
        val today = LocalDate.now()
        val targetOffset = if (today.dayOfMonth <= 28) 2 else -2
        val targetDay = today.dayOfMonth + targetOffset

        composeTestRule.setContent {
            SchedulerCalendar(
                isExpanded = true,
                onExpandChange = {},
                activeDay = 0,
                onDateSelected = {},
                unacknowledgedDates = setOf(targetOffset)
            )
        }

        composeTestRule.onNode(
            hasTextExactly(targetDay.toString()) and hasDateAttentionKind("normal")
        ).assertExists()
    }

    @Test
    fun calendarDateAttention_exposesWarningSemantics() {
        val today = LocalDate.now()
        val targetOffset = if (today.dayOfMonth <= 28) 2 else -2
        val targetDay = today.dayOfMonth + targetOffset

        composeTestRule.setContent {
            SchedulerCalendar(
                isExpanded = true,
                onExpandChange = {},
                activeDay = 0,
                onDateSelected = {},
                unacknowledgedDates = setOf(targetOffset),
                rescheduledDates = setOf(targetOffset)
            )
        }

        composeTestRule.onNode(
            hasTextExactly(targetDay.toString()) and hasDateAttentionKind("warning")
        ).assertExists()
    }

    @Test
    fun calendarDateAttention_clearsOnDateTap() {
        val today = LocalDate.now()
        val targetOffset = if (today.dayOfMonth <= 28) 2 else -2
        val targetDay = today.dayOfMonth + targetOffset

        composeTestRule.setContent {
            var activeDay by remember { mutableStateOf(0) }
            var unacknowledgedDates by remember { mutableStateOf(setOf(targetOffset)) }
            var rescheduledDates by remember { mutableStateOf(setOf(targetOffset)) }

            SchedulerCalendar(
                isExpanded = true,
                onExpandChange = {},
                activeDay = activeDay,
                onDateSelected = { offset ->
                    activeDay = offset
                    unacknowledgedDates = unacknowledgedDates - offset
                    rescheduledDates = rescheduledDates - offset
                },
                unacknowledgedDates = unacknowledgedDates,
                rescheduledDates = rescheduledDates
            )
        }

        composeTestRule.onAllNodes(
            hasTextExactly(targetDay.toString()) and hasDateAttentionKind("warning")
        ).onFirst().performClick()

        composeTestRule.onAllNodes(
            hasTextExactly(targetDay.toString()) and hasDateAttentionKind("none")
        ).onFirst().assertExists()
    }

    @Test
    fun calendarMonthPaging_chevronsNavigateVisibleMonth() {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy年 M月")
        val nextMonth = today.plusMonths(1).withDayOfMonth(1)

        composeTestRule.setContent {
            SchedulerCalendar(
                isExpanded = true,
                onExpandChange = {},
                activeDay = 0,
                onDateSelected = {}
            )
        }

        composeTestRule.onNodeWithText(today.withDayOfMonth(1).format(formatter)).assertExists()
        composeTestRule.onNodeWithContentDescription("Next month").performClick()
        composeTestRule.onNodeWithText(nextMonth.format(formatter)).assertExists()
        composeTestRule.onNodeWithContentDescription("Previous month").performClick()
        composeTestRule.onNodeWithText(today.withDayOfMonth(1).format(formatter)).assertExists()
    }

    @Test
    fun calendarMonthPaging_doesNotAcknowledgeAttentionUntilExplicitDayTap() {
        val today = LocalDate.now()
        val targetDate = today.plusMonths(1).withDayOfMonth(1)
        val targetOffset = java.time.temporal.ChronoUnit.DAYS.between(today, targetDate).toInt()
        val selectedOffsets = mutableListOf<Int>()

        composeTestRule.setContent {
            var activeDay by remember { mutableStateOf(0) }
            var unacknowledgedDates by remember { mutableStateOf(setOf(targetOffset)) }
            var rescheduledDates by remember { mutableStateOf(setOf(targetOffset)) }

            SchedulerCalendar(
                isExpanded = true,
                onExpandChange = {},
                activeDay = activeDay,
                onDateSelected = { offset ->
                    selectedOffsets += offset
                    activeDay = offset
                    unacknowledgedDates = unacknowledgedDates - offset
                    rescheduledDates = rescheduledDates - offset
                },
                unacknowledgedDates = unacknowledgedDates,
                rescheduledDates = rescheduledDates
            )
        }

        composeTestRule.onNodeWithContentDescription("Next month").performClick()
        composeTestRule.runOnIdle {
            assertTrue(selectedOffsets.isEmpty())
        }

        composeTestRule.onAllNodes(
            hasTextExactly(targetDate.dayOfMonth.toString()) and hasDateAttentionKind("warning")
        ).onFirst().assertExists()

        composeTestRule.onAllNodes(
            hasTextExactly(targetDate.dayOfMonth.toString()) and hasDateAttentionKind("warning")
        ).onFirst().performClick()

        composeTestRule.runOnIdle {
            assertEquals(listOf(targetOffset), selectedOffsets)
        }

        composeTestRule.onAllNodes(
            hasTextExactly(targetDate.dayOfMonth.toString()) and hasDateAttentionKind("none")
        ).onFirst().assertExists()
    }

    @Test
    fun calendarHandle_tapStillTogglesExpansionWhenDismissHookExists() {
        composeTestRule.setContent {
            var isExpanded by remember { mutableStateOf(false) }
            SchedulerCalendar(
                isExpanded = isExpanded,
                onExpandChange = { isExpanded = it },
                activeDay = 0,
                onDateSelected = {},
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithTag(SCHEDULER_CALENDAR_HANDLE_TEST_TAG)
            .assertExists()
            .performClick()

        val todayDay = LocalDate.now().dayOfMonth
        val weekStart = ((todayDay - 1) / 7) * 7 + 1
        val offWeekDay = if (weekStart > 1) weekStart - 1 else weekStart + 7
        composeTestRule.onAllNodes(hasTextExactly(offWeekDay.toString())).onFirst().assertExists()
    }

    private fun hasDateAttentionKind(kind: String): SemanticsMatcher {
        return SemanticsMatcher.expectValue(SchedulerDateAttentionKindKey, kind)
    }
}
