package com.smartsales.prism.ui.drawers.scheduler

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import java.time.LocalDate
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
        composeTestRule.onNode(hasTextExactly(offWeekDay.toString())).assertExists()
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

        composeTestRule.onNode(
            hasTextExactly(targetDay.toString()) and hasDateAttentionKind("warning")
        ).performClick()

        composeTestRule.onNode(
            hasTextExactly(targetDay.toString()) and hasDateAttentionKind("none")
        ).assertExists()
    }

    private fun hasDateAttentionKind(kind: String): SemanticsMatcher {
        return SemanticsMatcher.expectValue(SchedulerDateAttentionKindKey, kind)
    }
}
