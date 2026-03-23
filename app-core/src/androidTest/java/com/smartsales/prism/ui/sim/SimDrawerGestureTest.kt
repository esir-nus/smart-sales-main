package com.smartsales.prism.ui.sim

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.smartsales.prism.ui.AgentIntelligenceScreen
import com.smartsales.prism.ui.AgentIntelligenceVisualMode
import com.smartsales.prism.ui.drawers.SCHEDULER_DRAWER_HANDLE_TEST_TAG
import com.smartsales.prism.ui.drawers.SchedulerDrawer
import com.smartsales.prism.ui.drawers.scheduler.FakeSchedulerViewModel
import com.smartsales.prism.ui.fakes.FakeAgentViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SimDrawerGestureTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun activeChatTopZoneDragDown_opensSchedulerCallback() {
        var openCount = 0

        setMountedSimGestureShell(
            viewModel = activeChatViewModel(),
            onOpenScheduler = { openCount += 1 }
        )

        composeTestRule.onNodeWithTag(SIM_SCHEDULER_EDGE_ZONE_TEST_TAG)
            .assertExists()
            .performTouchInput {
                down(center)
                moveBy(Offset(0f, 300f))
                up()
            }

        composeTestRule.runOnIdle {
            assertEquals(1, openCount)
        }
    }

    @Test
    fun activeChatBottomZoneDragUp_opensAudioBrowseCallback() {
        var openCount = 0

        setMountedSimGestureShell(
            viewModel = activeChatViewModel(),
            onOpenAudioBrowse = { openCount += 1 }
        )

        composeTestRule.onNodeWithTag(SIM_AUDIO_EDGE_ZONE_TEST_TAG)
            .assertExists()
            .performTouchInput {
                down(center)
                moveBy(Offset(0f, -300f))
                up()
            }

        composeTestRule.runOnIdle {
            assertEquals(1, openCount)
        }
    }

    @Test
    fun middleConversationDrag_doesNotOpenDrawerCallbacks() {
        var schedulerOpenCount = 0
        var audioOpenCount = 0

        setMountedSimGestureShell(
            viewModel = activeChatViewModel(),
            onOpenScheduler = { schedulerOpenCount += 1 },
            onOpenAudioBrowse = { audioOpenCount += 1 }
        )

        composeTestRule.onRoot()
            .performTouchInput {
                val start = Offset(width / 2f, height / 2f)
                down(start)
                moveBy(Offset(0f, -300f))
                up()
            }

        composeTestRule.runOnIdle {
            assertEquals(0, schedulerOpenCount)
            assertEquals(0, audioOpenCount)
        }
    }

    @Test
    fun afterNewSessionFromPriorChat_edgeGesturesStillOpenDrawers() {
        var schedulerOpenCount = 0
        var audioOpenCount = 0
        lateinit var startNewSession: () -> Unit

        setMountedSimGestureShell(
            viewModel = activeChatViewModel(),
            onOpenScheduler = { schedulerOpenCount += 1 },
            onOpenAudioBrowse = { audioOpenCount += 1 },
            bindStartNewSession = { startNewSession = it }
        )

        composeTestRule.runOnIdle {
            startNewSession()
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(SIM_SCHEDULER_EDGE_ZONE_TEST_TAG)
            .assertExists()
            .performTouchInput {
                down(center)
                moveBy(Offset(0f, 300f))
                up()
            }

        composeTestRule.onNodeWithTag(SIM_AUDIO_EDGE_ZONE_TEST_TAG)
            .assertExists()
            .performTouchInput {
                down(center)
                moveBy(Offset(0f, -300f))
                up()
            }

        composeTestRule.runOnIdle {
            assertEquals(1, schedulerOpenCount)
            assertEquals(1, audioOpenCount)
        }
    }

    @Test
    fun imeVisible_blocksOnlyAudioZone() {
        var schedulerOpenCount = 0

        setMountedSimGestureShell(
            viewModel = activeChatViewModel(),
            isImeVisible = true,
            onOpenScheduler = { schedulerOpenCount += 1 }
        )

        composeTestRule.onNodeWithTag(SIM_SCHEDULER_EDGE_ZONE_TEST_TAG).assertExists()
        composeTestRule.onNodeWithTag(SIM_AUDIO_EDGE_ZONE_TEST_TAG).assertDoesNotExist()

        composeTestRule.onNodeWithTag(SIM_SCHEDULER_EDGE_ZONE_TEST_TAG)
            .performTouchInput {
                down(center)
                moveBy(Offset(0f, 300f))
                up()
            }

        composeTestRule.runOnIdle {
            assertEquals(1, schedulerOpenCount)
        }
    }

    @Test
    fun edgeZones_hideWhenBlockedByOverlayState() {
        setMountedSimGestureShell(
            viewModel = activeChatViewModel(),
            shellState = SimShellState(
                activeDrawer = SimDrawerType.SCHEDULER,
                showHistory = true
            )
        )

        composeTestRule.onNodeWithTag(SIM_SCHEDULER_EDGE_ZONE_TEST_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(SIM_AUDIO_EDGE_ZONE_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun audioHandle_dragDownAndTap_bothDismiss() {
        var dismissCount = 0

        composeTestRule.setContent {
            SimDrawerHandle(
                dismissDirection = SimVerticalGestureDirection.DOWN,
                onDismiss = { dismissCount += 1 },
                testTag = SIM_AUDIO_HANDLE_TEST_TAG,
                dismissOnTap = true
            )
        }

        composeTestRule.onNodeWithTag(SIM_AUDIO_HANDLE_TEST_TAG)
            .assertExists()
            .performTouchInput {
                down(center)
                moveBy(Offset(0f, 300f))
                up()
            }

        composeTestRule.onNodeWithTag(SIM_AUDIO_HANDLE_TEST_TAG)
            .performTouchInput { click(center) }

        composeTestRule.runOnIdle {
            assertEquals(2, dismissCount)
        }
    }

    @Test
    fun schedulerHandle_dragUp_dismissesDrawer() {
        var dismissCount = 0

        composeTestRule.setContent {
            SchedulerDrawer(
                isOpen = true,
                onDismiss = { dismissCount += 1 },
                viewModel = FakeSchedulerViewModel()
            )
        }

        composeTestRule.onNodeWithTag(SCHEDULER_DRAWER_HANDLE_TEST_TAG)
            .assertExists()
            .performTouchInput {
                down(center)
                moveBy(Offset(0f, -300f))
                up()
            }

        composeTestRule.runOnIdle {
            assertEquals(1, dismissCount)
        }
    }

    private fun setMountedSimGestureShell(
        viewModel: FakeAgentViewModel,
        shellState: SimShellState = SimShellState(),
        isImeVisible: Boolean = false,
        onOpenScheduler: () -> Unit = {},
        onOpenAudioBrowse: () -> Unit = {},
        bindStartNewSession: ((() -> Unit) -> Unit)? = null
    ) {
        composeTestRule.setContent {
            var activeViewModel by remember { mutableStateOf(viewModel) }
            var headerBottomPx by remember { mutableStateOf<Float?>(null) }
            var composerTopPx by remember { mutableStateOf<Float?>(null) }
            var rootHeightPx by remember { mutableStateOf(0f) }
            val gestureAnchors = remember(headerBottomPx, composerTopPx, rootHeightPx) {
                buildSimGestureAnchors(
                    headerBottomPx = headerBottomPx,
                    composerTopPx = composerTopPx,
                    rootHeightPx = rootHeightPx
                )
            }

            bindStartNewSession?.invoke {
                activeViewModel = FakeAgentViewModel()
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coordinates ->
                        rootHeightPx = coordinates.size.height.toFloat()
                    }
            ) {
                AgentIntelligenceScreen(
                    viewModel = activeViewModel,
                    visualMode = AgentIntelligenceVisualMode.SIM,
                    showDebugButton = false,
                    onSimHeaderBoundsChanged = { bounds ->
                        headerBottomPx = bounds.bottom
                    },
                    onSimComposerBoundsChanged = { bounds ->
                        composerTopPx = bounds.top
                    }
                )
                SimDrawerEdgeGestureLayer(
                    state = shellState,
                    isImeVisible = isImeVisible,
                    gestureAnchors = gestureAnchors,
                    onOpenScheduler = onOpenScheduler,
                    onOpenAudioBrowse = onOpenAudioBrowse
                )
            }
        }

        composeTestRule.waitForIdle()
    }

    private fun activeChatViewModel(): FakeAgentViewModel {
        return FakeAgentViewModel().apply {
            updateInput("hello sim")
            send()
        }
    }
}
