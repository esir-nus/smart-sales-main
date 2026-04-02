package com.smartsales.prism.ui.drawers.scheduler

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CollapsibleInspirationShelfTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun shelfAskAiVisibleWhenCallbackPresentAndReturnsDisplayedTitle() {
        var capturedTitle: String? = null

        composeTestRule.setContent {
            CollapsibleInspirationShelf(
                items = listOf(
                    TimelineItem.Inspiration(
                        id = "insp-1",
                        timeDisplay = "💡",
                        title = "i want to learn guitar"
                    )
                ),
                isExpanded = true,
                onToggle = {},
                onDelete = {},
                onAskAI = { capturedTitle = it }
            )
        }

        composeTestRule.onNodeWithText("Ask AI").assertExists().performClick()

        assertEquals("i want to learn guitar", capturedTitle)
    }


    @Test
    fun simShelfUsesIconOnlyAskAiAffordance() {
        var capturedTitle: String? = null

        composeTestRule.setContent {
            androidx.compose.runtime.CompositionLocalProvider(
                LocalSchedulerDrawerVisuals provides schedulerDrawerVisualsFor(SchedulerDrawerVisualMode.SIM),
                LocalSchedulerDrawerVisualMode provides SchedulerDrawerVisualMode.SIM
            ) {
                CollapsibleInspirationShelf(
                    items = listOf(
                        TimelineItem.Inspiration(
                            id = "insp-1",
                            timeDisplay = "💡",
                            title = "i want to learn guitar"
                        )
                    ),
                    isExpanded = true,
                    onToggle = {},
                    onDelete = {},
                    onAskAI = { capturedTitle = it }
                )
            }
        }

        composeTestRule.onNodeWithText("Ask AI").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Ask AI").assertExists().performClick()

        assertEquals("i want to learn guitar", capturedTitle)
    }

    @Test
    fun shelfAskAiHiddenWhenCallbackAbsent() {
        composeTestRule.setContent {
            CollapsibleInspirationShelf(
                items = listOf(
                    TimelineItem.Inspiration(
                        id = "insp-1",
                        timeDisplay = "💡",
                        title = "i want to learn guitar"
                    )
                ),
                isExpanded = true,
                onToggle = {},
                onDelete = {},
                onAskAI = null
            )
        }

        composeTestRule.onNodeWithText("Ask AI").assertDoesNotExist()
        composeTestRule.onNodeWithText("i want to learn guitar").assertExists()
    }

    @Test
    fun longTitleStillKeepsAskAiButtonVisible() {
        composeTestRule.setContent {
            CollapsibleInspirationShelf(
                items = listOf(
                    TimelineItem.Inspiration(
                        id = "insp-1",
                        timeDisplay = "💡",
                        title = "i want to learn guitar and also keep practicing long enough to verify the shelf layout does not clip the ask ai button"
                    )
                ),
                isExpanded = true,
                onToggle = {},
                onDelete = {},
                onAskAI = {}
            )
        }

        composeTestRule.onNodeWithText("Ask AI").assertExists()
    }
}
