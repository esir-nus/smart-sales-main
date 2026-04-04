package com.smartsales.prism.ui.sim

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.smartsales.prism.ui.AgentIntelligenceScreen
import com.smartsales.prism.ui.AgentIntelligenceVisualMode
import com.smartsales.prism.ui.components.DynamicIslandItem
import com.smartsales.prism.ui.components.DynamicIslandLane
import com.smartsales.prism.ui.components.DynamicIslandTapAction
import com.smartsales.prism.ui.components.DynamicIslandUiState
import com.smartsales.prism.ui.components.DynamicIslandVisualState
import com.smartsales.prism.ui.drawers.scheduler.SCHEDULER_CALENDAR_HANDLE_TEST_TAG
import com.smartsales.prism.ui.drawers.SCHEDULER_DRAWER_HANDLE_TEST_TAG
import com.smartsales.prism.ui.drawers.SchedulerDrawer
import com.smartsales.prism.ui.drawers.scheduler.FakeSchedulerViewModel
import com.smartsales.prism.ui.drawers.scheduler.SchedulerDrawerVisualMode
import com.smartsales.prism.ui.fakes.FakeAgentViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SimDrawerGestureTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private companion object {
        const val SIM_AUDIO_BROWSE_GRIP_TEST_TAG = "sim_audio_browse_grip"
    }

    @Test
    fun dynamicIslandDragDown_opensSchedulerCallback() {
        var openCount = 0

        setMountedSimGestureShell(
            viewModel = activeChatViewModel(),
            onOpenScheduler = { openCount += 1 }
        )

        composeTestRule.onNodeWithTag(SIM_DYNAMIC_ISLAND_TEST_TAG)
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
    fun headerDragDown_outsideDynamicIsland_doesNotOpenSchedulerCallback() {
        var openCount = 0

        setMountedSimGestureShell(
            viewModel = activeChatViewModel(),
            onOpenScheduler = { openCount += 1 }
        )

        composeTestRule.onNodeWithTag(SIM_HEADER_TEST_TAG)
            .assertExists()
            .performTouchInput {
                val start = Offset(left + 24f, centerY)
                down(start)
                moveBy(Offset(0f, 300f))
                up()
            }

        composeTestRule.runOnIdle {
            assertEquals(0, openCount)
        }
    }

    @Test
    fun activeChatBottomZoneDragUp_opensAudioBrowseCallback() {
        var openCount = 0

        setMountedSimGestureShell(
            viewModel = activeChatViewModel(),
            onOpenAudioBrowse = { openCount += 1 }
        )

        composeTestRule.onNodeWithTag(SIM_INPUT_BAR_TEST_TAG)
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

        composeTestRule.onNodeWithTag(SIM_HEADER_TEST_TAG)
            .assertExists()
            .performTouchInput {
                down(center)
                moveBy(Offset(0f, 300f))
                up()
            }

        composeTestRule.onNodeWithTag(SIM_INPUT_BAR_TEST_TAG)
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

        composeTestRule.onNodeWithTag(SIM_DYNAMIC_ISLAND_TEST_TAG).assertExists()
        composeTestRule.onNodeWithTag(SIM_INPUT_BAR_TEST_TAG).assertDoesNotExist()

        composeTestRule.onNodeWithTag(SIM_DYNAMIC_ISLAND_TEST_TAG)
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
            shellState = RuntimeShellState(
                activeDrawer = RuntimeDrawerType.SCHEDULER,
                showHistory = true
            )
        )

        composeTestRule.onNodeWithTag(SIM_HEADER_TEST_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(SIM_INPUT_BAR_TEST_TAG).assertDoesNotExist()
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
    fun simAudioBrowseGrip_dragUp_pastThreshold_triggersManualSync() {
        var syncCount = 0
        var dismissCount = 0
        var pullThresholdPx = 0f

        composeTestRule.setContent {
            val density = LocalDensity.current
            LaunchedEffect(density) {
                pullThresholdPx = with(density) { 55.dp.toPx() }
            }
            SimAudioBrowseGrip(
                syncVisualState = SimAudioSyncVisualState.READY,
                onDismiss = { dismissCount += 1 },
                onSyncFromBadge = { syncCount += 1 },
                onBrowsePullOffsetChanged = {},
                onBrowsePullSettled = {},
                modifier = Modifier.testTag(SIM_AUDIO_BROWSE_GRIP_TEST_TAG)
            )
        }

        composeTestRule.onNodeWithTag(SIM_AUDIO_BROWSE_GRIP_TEST_TAG)
            .assertExists()
            .performTouchInput {
                down(center)
                moveBy(Offset(0f, -(pullThresholdPx + 96f)))
                up()
            }

        composeTestRule.runOnIdle {
            assertEquals(1, syncCount)
            assertEquals(0, dismissCount)
        }
    }

    @Test
    fun simAudioBrowseGrip_smallDragUp_doesNotTriggerSync() {
        var syncCount = 0
        var dismissCount = 0
        var pullThresholdPx = 0f

        composeTestRule.setContent {
            val density = LocalDensity.current
            LaunchedEffect(density) {
                pullThresholdPx = with(density) { 55.dp.toPx() }
            }
            SimAudioBrowseGrip(
                syncVisualState = SimAudioSyncVisualState.READY,
                onDismiss = { dismissCount += 1 },
                onSyncFromBadge = { syncCount += 1 },
                onBrowsePullOffsetChanged = {},
                onBrowsePullSettled = {},
                modifier = Modifier.testTag(SIM_AUDIO_BROWSE_GRIP_TEST_TAG)
            )
        }

        composeTestRule.onNodeWithTag(SIM_AUDIO_BROWSE_GRIP_TEST_TAG)
            .assertExists()
            .performTouchInput {
                down(center)
                moveBy(Offset(0f, -(pullThresholdPx * 0.25f)))
                up()
            }

        composeTestRule.runOnIdle {
            assertEquals(0, syncCount)
            assertEquals(0, dismissCount)
        }
    }

    @Test
    fun simAudioBrowseGrip_dragDown_dismissesDrawer() {
        var syncCount = 0
        var dismissCount = 0
        var dismissThresholdPx = 0f

        composeTestRule.setContent {
            val density = LocalDensity.current
            LaunchedEffect(density) {
                dismissThresholdPx = with(density) { 56.dp.toPx() }
            }
            SimAudioBrowseGrip(
                syncVisualState = SimAudioSyncVisualState.READY,
                onDismiss = { dismissCount += 1 },
                onSyncFromBadge = { syncCount += 1 },
                onBrowsePullOffsetChanged = {},
                onBrowsePullSettled = {},
                modifier = Modifier.testTag(SIM_AUDIO_BROWSE_GRIP_TEST_TAG)
            )
        }

        composeTestRule.onNodeWithTag(SIM_AUDIO_BROWSE_GRIP_TEST_TAG)
            .assertExists()
            .performTouchInput {
                down(center)
                moveBy(Offset(0f, dismissThresholdPx + 96f))
                up()
            }

        composeTestRule.runOnIdle {
            assertEquals(0, syncCount)
            assertEquals(1, dismissCount)
        }
    }

    @Test
    fun simAudioBrowseGrip_blockedDragUp_doesNotTriggerCallbacks() {
        var syncCount = 0
        var dismissCount = 0
        var pullThresholdPx = 0f

        composeTestRule.setContent {
            val density = LocalDensity.current
            LaunchedEffect(density) {
                pullThresholdPx = with(density) { 55.dp.toPx() }
            }
            SimAudioBrowseGrip(
                syncVisualState = SimAudioSyncVisualState.BLOCKED,
                onDismiss = { dismissCount += 1 },
                onSyncFromBadge = { syncCount += 1 },
                onBrowsePullOffsetChanged = {},
                onBrowsePullSettled = {},
                modifier = Modifier.testTag(SIM_AUDIO_BROWSE_GRIP_TEST_TAG)
            )
        }

        composeTestRule.onNodeWithTag(SIM_AUDIO_BROWSE_GRIP_TEST_TAG)
            .assertExists()
            .performTouchInput {
                down(center)
                moveBy(Offset(0f, -(pullThresholdPx + 96f)))
                up()
            }

        composeTestRule.runOnIdle {
            assertEquals(0, syncCount)
            assertEquals(0, dismissCount)
        }
    }

    @Test
    fun schedulerHandle_dragDown_dismissesDrawer() {
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
                moveBy(Offset(0f, 300f))
                up()
            }

        composeTestRule.runOnIdle {
            assertEquals(1, dismissCount)
        }
    }

    @Test
    fun simSchedulerHeader_hidesSideButtonsButKeepsDynamicIsland() {
        composeTestRule.setContent {
            SimHomeHeroShellFrame(
                inputText = "",
                isSending = false,
                dynamicIslandState = DynamicIslandUiState.Visible(
                    DynamicIslandItem(
                        sessionTitle = "SIM",
                        schedulerSummary = "最近：客户回访 · 15:00",
                        tapAction = DynamicIslandTapAction.OpenSchedulerDrawer()
                    )
                ),
                onMenuClick = {},
                onNewSessionClick = {},
                onSchedulerClick = {},
                showMenuButton = false,
                showNewSessionButton = false,
                onTextChanged = {},
                onSend = {},
                onAttachClick = {},
                showBottomComposer = false,
                enableSchedulerPullGesture = true
            ) { modifier ->
                Box(modifier = modifier.fillMaxSize())
            }
        }

        composeTestRule.onNodeWithTag(SIM_HEADER_TEST_TAG).assertExists()
        composeTestRule.onNodeWithTag(SIM_DYNAMIC_ISLAND_TEST_TAG).assertExists()
        composeTestRule.onNodeWithTag(SIM_HEADER_MENU_BUTTON_TEST_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(SIM_HEADER_NEW_CHAT_BUTTON_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun simConnectedHeader_showsAmbientFlankIcons() {
        composeTestRule.setContent {
            SimHomeHeroShellFrame(
                inputText = "",
                isSending = false,
                dynamicIslandState = DynamicIslandUiState.Visible(
                    DynamicIslandItem(
                        displayText = "Badge 已连接",
                        lane = DynamicIslandLane.CONNECTIVITY,
                        visualState = DynamicIslandVisualState.CONNECTIVITY_CONNECTED,
                        batteryPercentage = 85,
                        tapAction = DynamicIslandTapAction.OpenConnectivityEntry
                    )
                ),
                onMenuClick = {},
                onNewSessionClick = {},
                onSchedulerClick = {},
                onTextChanged = {},
                onSend = {},
                onAttachClick = {},
                showBottomComposer = false
            ) { modifier ->
                Box(modifier = modifier.fillMaxSize())
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(SIM_HEADER_LEFT_AMBIENT_ICON_TEST_TAG).assertExists()
        composeTestRule.onNodeWithTag(SIM_HEADER_RIGHT_AMBIENT_ICON_TEST_TAG).assertExists()
    }

    @Test
    fun simNonConnectedHeader_hidesAmbientFlankIcons() {
        composeTestRule.setContent {
            SimHomeHeroShellFrame(
                inputText = "",
                isSending = false,
                dynamicIslandState = DynamicIslandUiState.Visible(
                    DynamicIslandItem(
                        sessionTitle = "SIM",
                        schedulerSummary = "最近：客户回访 · 15:00",
                        tapAction = DynamicIslandTapAction.OpenSchedulerDrawer()
                    )
                ),
                onMenuClick = {},
                onNewSessionClick = {},
                onSchedulerClick = {},
                onTextChanged = {},
                onSend = {},
                onAttachClick = {},
                showBottomComposer = false
            ) { modifier ->
                Box(modifier = modifier.fillMaxSize())
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(SIM_HEADER_LEFT_AMBIENT_ICON_TEST_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(SIM_HEADER_RIGHT_AMBIENT_ICON_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun simSchedulerHandle_dragUp_dismissesDrawer() {
        var dismissCount = 0

        composeTestRule.setContent {
            SchedulerDrawer(
                isOpen = true,
                onDismiss = { dismissCount += 1 },
                visualMode = SchedulerDrawerVisualMode.SIM,
                enableInspirationMultiSelect = false,
                viewModel = FakeSchedulerViewModel()
            )
        }

        composeTestRule.onNodeWithTag(SCHEDULER_DRAWER_HANDLE_TEST_TAG)
            .assertExists()
            .performTouchInput {
                down(center)
                moveBy(Offset(0f, -120f))
                up()
            }

        composeTestRule.runOnIdle {
            assertEquals(1, dismissCount)
        }
    }

    @Test
    fun simSchedulerHandle_tapDismissesDrawer() {
        var dismissCount = 0

        composeTestRule.setContent {
            SchedulerDrawer(
                isOpen = true,
                onDismiss = { dismissCount += 1 },
                visualMode = SchedulerDrawerVisualMode.SIM,
                enableInspirationMultiSelect = false,
                viewModel = FakeSchedulerViewModel()
            )
        }

        composeTestRule.onNodeWithTag(SCHEDULER_DRAWER_HANDLE_TEST_TAG)
            .assertExists()
            .performTouchInput {
                click(center)
            }

        composeTestRule.runOnIdle {
            assertEquals(1, dismissCount)
        }
    }

    @Test
    fun simSchedulerHandle_dragDown_doesNotDismissDrawer() {
        var dismissCount = 0

        composeTestRule.setContent {
            SchedulerDrawer(
                isOpen = true,
                onDismiss = { dismissCount += 1 },
                visualMode = SchedulerDrawerVisualMode.SIM,
                enableInspirationMultiSelect = false,
                viewModel = FakeSchedulerViewModel()
            )
        }

        composeTestRule.onNodeWithTag(SCHEDULER_DRAWER_HANDLE_TEST_TAG)
            .assertExists()
            .performTouchInput {
                down(center)
                moveBy(Offset(0f, 120f))
                up()
            }

        composeTestRule.runOnIdle {
            assertEquals(0, dismissCount)
        }
    }

    @Test
    fun simSchedulerHandle_smallDragUp_doesNotDismissDrawer() {
        var dismissCount = 0

        composeTestRule.setContent {
            SchedulerDrawer(
                isOpen = true,
                onDismiss = { dismissCount += 1 },
                visualMode = SchedulerDrawerVisualMode.SIM,
                enableInspirationMultiSelect = false,
                viewModel = FakeSchedulerViewModel()
            )
        }

        composeTestRule.onNodeWithTag(SCHEDULER_DRAWER_HANDLE_TEST_TAG)
            .assertExists()
            .performTouchInput {
                down(center)
                moveBy(Offset(0f, -12f))
                up()
            }

        composeTestRule.runOnIdle {
            assertEquals(0, dismissCount)
        }
    }

    @Test
    fun simSchedulerCalendarHandle_dragUp_dismissesDrawer() {
        var dismissCount = 0

        composeTestRule.setContent {
            SchedulerDrawer(
                isOpen = true,
                onDismiss = { dismissCount += 1 },
                visualMode = SchedulerDrawerVisualMode.SIM,
                enableInspirationMultiSelect = false,
                viewModel = FakeSchedulerViewModel()
            )
        }

        composeTestRule.onNodeWithTag(SCHEDULER_CALENDAR_HANDLE_TEST_TAG)
            .assertExists()
            .performTouchInput {
                down(center)
                moveBy(Offset(0f, -120f))
                up()
            }

        composeTestRule.runOnIdle {
            assertEquals(1, dismissCount)
        }
    }

    private fun setMountedSimGestureShell(
        viewModel: FakeAgentViewModel,
        shellState: RuntimeShellState = RuntimeShellState(),
        isImeVisible: Boolean = false,
        onOpenScheduler: () -> Unit = {},
        onOpenAudioBrowse: () -> Unit = {},
        bindStartNewSession: ((() -> Unit) -> Unit)? = null
    ) {
        composeTestRule.setContent {
            var activeViewModel by remember { mutableStateOf(viewModel) }

            bindStartNewSession?.invoke {
                activeViewModel = FakeAgentViewModel()
            }

            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                AgentIntelligenceScreen(
                    viewModel = activeViewModel,
                    visualMode = AgentIntelligenceVisualMode.SIM,
                    showDebugButton = false,
                    enableSimSchedulerPullGesture = canOpenSimSchedulerFromEdge(shellState),
                    enableSimAudioPullGesture = canOpenSimAudioFromEdge(shellState, isImeVisible),
                    onSimSchedulerPullOpen = onOpenScheduler,
                    onSimAudioPullOpen = onOpenAudioBrowse
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
