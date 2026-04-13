package com.smartsales.prism.ui.sim

import com.smartsales.prism.ui.components.DynamicIslandItem
import com.smartsales.prism.ui.components.DynamicIslandLane
import com.smartsales.prism.ui.components.DynamicIslandVisualState
import com.smartsales.prism.ui.components.connectivity.ConnectionState
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SimShellDynamicIslandCoordinatorTest {

    @Test
    fun `scheduler lane remains default and rotates every five seconds`() = runTest {
        val coordinator = createCoordinator(
            parentScope = this,
            schedulerItems = MutableStateFlow(
                listOf(
                    schedulerItem("最近：客户回访 · 09:00"),
                    schedulerItem("最近：发送报价 · 10:00")
                )
            ),
            connectivityState = MutableStateFlow(ConnectionState.CONNECTED)
        )

        runCurrent()
        assertVisible(
            coordinator = coordinator,
            expectedLane = DynamicIslandLane.SCHEDULER,
            expectedText = "最近：客户回访 · 09:00"
        )

        advanceTimeBy(4_999L)
        runCurrent()
        assertVisible(
            coordinator = coordinator,
            expectedLane = DynamicIslandLane.SCHEDULER,
            expectedText = "最近：客户回访 · 09:00"
        )

        advanceTimeBy(1L)
        runCurrent()
        assertVisible(
            coordinator = coordinator,
            expectedLane = DynamicIslandLane.SCHEDULER,
            expectedText = "最近：发送报价 · 10:00"
        )

        coordinator.close()
    }

    @Test
    fun `session title item rotates after three seconds when visible`() = runTest {
        val coordinator = createCoordinator(
            parentScope = this,
            schedulerItems = MutableStateFlow(
                listOf(
                    titleItem("Q4复盘"),
                    schedulerItem("最近：客户回访 · 09:00")
                )
            ),
            connectivityState = MutableStateFlow(ConnectionState.CONNECTED)
        )

        runCurrent()
        assertVisible(
            coordinator = coordinator,
            expectedLane = DynamicIslandLane.SCHEDULER,
            expectedText = "Q4复盘"
        )

        advanceTimeBy(2_999L)
        runCurrent()
        assertVisible(
            coordinator = coordinator,
            expectedLane = DynamicIslandLane.SCHEDULER,
            expectedText = "Q4复盘"
        )

        advanceTimeBy(1L)
        runCurrent()
        assertVisible(
            coordinator = coordinator,
            expectedLane = DynamicIslandLane.SCHEDULER,
            expectedText = "最近：客户回访 · 09:00"
        )

        coordinator.close()
    }

    @Test
    fun `title interrupt jumps to title item immediately`() = runTest {
        val coordinator = createCoordinator(
            parentScope = this,
            schedulerItems = MutableStateFlow(
                listOf(
                    titleItem("Q4复盘"),
                    schedulerItem("最近：客户回访 · 09:00")
                )
            ),
            connectivityState = MutableStateFlow(ConnectionState.CONNECTED)
        )

        runCurrent()
        advanceTimeBy(3_000L)
        runCurrent()
        assertVisible(
            coordinator = coordinator,
            expectedLane = DynamicIslandLane.SCHEDULER,
            expectedText = "最近：客户回访 · 09:00"
        )

        coordinator.updateSessionTitleInterruptToken(1)
        runCurrent()
        assertVisible(
            coordinator = coordinator,
            expectedLane = DynamicIslandLane.SCHEDULER,
            expectedText = "Q4复盘"
        )

        coordinator.close()
    }

    @Test
    fun `projection caps mixed title and scheduler rotation at three items`() {
        val items = buildSimDynamicIslandItems(
            sessionTitle = "Q4复盘",
            sessionHasAudioContextHistory = true,
            orderedTasks = listOf(
                scheduledTask("task_1", "客户回访", "09:00"),
                scheduledTask("task_2", "发送报价", "10:00"),
                scheduledTask("task_3", "确认合同", "11:00")
            )
        )

        assertEquals(3, items.size)
        assertEquals("最近：客户回访 · 09:00", items[0].displayText)
        assertEquals("最近：发送报价 · 10:00", items[1].displayText)
        assertEquals("最近：确认合同 · 11:00", items[2].displayText)
    }

    @Test
    fun `disconnected change interrupts for three seconds then yields back to scheduler`() = runTest {
        val connectivityState = MutableStateFlow(ConnectionState.CONNECTED)
        val coordinator = createCoordinator(
            parentScope = this,
            schedulerItems = MutableStateFlow(listOf(schedulerItem("最近：客户回访 · 09:00"))),
            connectivityState = connectivityState
        )

        runCurrent()
        connectivityState.value = ConnectionState.DISCONNECTED
        runCurrent()
        assertVisible(
            coordinator = coordinator,
            expectedLane = DynamicIslandLane.CONNECTIVITY,
            expectedText = "Badge 已断开"
        )

        advanceTimeBy(2_999L)
        runCurrent()
        assertVisible(
            coordinator = coordinator,
            expectedLane = DynamicIslandLane.CONNECTIVITY,
            expectedText = "Badge 已断开"
        )

        advanceTimeBy(1L)
        runCurrent()
        assertVisible(
            coordinator = coordinator,
            expectedLane = DynamicIslandLane.SCHEDULER,
            expectedText = "最近：客户回访 · 09:00"
        )

        coordinator.close()
    }

    @Test
    fun `connected change interrupts for five seconds then yields back to scheduler`() = runTest {
        val connectivityState = MutableStateFlow(ConnectionState.DISCONNECTED)
        val coordinator = createCoordinator(
            parentScope = this,
            schedulerItems = MutableStateFlow(listOf(schedulerItem("最近：客户回访 · 09:00"))),
            connectivityState = connectivityState
        )

        runCurrent()
        connectivityState.value = ConnectionState.CONNECTED
        runCurrent()
        assertVisible(
            coordinator = coordinator,
            expectedLane = DynamicIslandLane.CONNECTIVITY,
            expectedText = "Badge 已连接"
        )

        advanceTimeBy(4_999L)
        runCurrent()
        assertVisible(
            coordinator = coordinator,
            expectedLane = DynamicIslandLane.CONNECTIVITY,
            expectedText = "Badge 已连接"
        )

        advanceTimeBy(1L)
        runCurrent()
        assertVisible(
            coordinator = coordinator,
            expectedLane = DynamicIslandLane.SCHEDULER,
            expectedText = "最近：客户回访 · 09:00"
        )

        coordinator.close()
    }

    @Test
    fun `heartbeat surfaces connected lane for five seconds every thirty seconds with mock battery`() = runTest {
        val batteryLevel = MutableStateFlow(85)
        val coordinator = createCoordinator(
            parentScope = this,
            schedulerItems = MutableStateFlow(listOf(schedulerItem("最近：客户回访 · 09:00"))),
            connectivityState = MutableStateFlow(ConnectionState.CONNECTED),
            batteryLevel = batteryLevel
        )

        runCurrent()
        advanceTimeBy(30_000L)
        runCurrent()
        assertVisible(
            coordinator = coordinator,
            expectedLane = DynamicIslandLane.CONNECTIVITY,
            expectedText = "Badge 已连接"
        )
        assertEquals(85, coordinator.presentation.value.visibleItem?.batteryPercentage)
        assertEquals(
            DynamicIslandVisualState.CONNECTIVITY_CONNECTED,
            coordinator.presentation.value.visibleItem?.visualState
        )

        advanceTimeBy(4_999L)
        runCurrent()
        assertVisible(
            coordinator = coordinator,
            expectedLane = DynamicIslandLane.CONNECTIVITY,
            expectedText = "Badge 已连接"
        )

        advanceTimeBy(1L)
        runCurrent()
        assertVisible(
            coordinator = coordinator,
            expectedLane = DynamicIslandLane.SCHEDULER,
            expectedText = "最近：客户回访 · 09:00"
        )

        coordinator.close()
    }

    @Test
    fun `persistent connectivity lane yields only while suppression is active`() = runTest {
        val connectivityState = MutableStateFlow(ConnectionState.CONNECTED)
        val takeoverSuppressed = MutableStateFlow(false)
        val coordinator = createCoordinator(
            parentScope = this,
            schedulerItems = MutableStateFlow(listOf(schedulerItem("最近：客户回访 · 09:00"))),
            connectivityState = connectivityState,
            takeoverSuppressed = takeoverSuppressed
        )

        runCurrent()
        connectivityState.value = ConnectionState.RECONNECTING
        runCurrent()
        assertVisible(
            coordinator = coordinator,
            expectedLane = DynamicIslandLane.CONNECTIVITY,
            expectedText = "Badge 重连中..."
        )

        takeoverSuppressed.value = true
        runCurrent()
        assertVisible(
            coordinator = coordinator,
            expectedLane = DynamicIslandLane.SCHEDULER,
            expectedText = "最近：客户回访 · 09:00"
        )

        takeoverSuppressed.value = false
        runCurrent()
        assertVisible(
            coordinator = coordinator,
            expectedLane = DynamicIslandLane.CONNECTIVITY,
            expectedText = "Badge 重连中..."
        )

        connectivityState.value = ConnectionState.CONNECTED
        runCurrent()
        assertVisible(
            coordinator = coordinator,
            expectedLane = DynamicIslandLane.CONNECTIVITY,
            expectedText = "Badge 已连接"
        )

        advanceTimeBy(5_000L)
        runCurrent()
        assertVisible(
            coordinator = coordinator,
            expectedLane = DynamicIslandLane.SCHEDULER,
            expectedText = "最近：客户回访 · 09:00"
        )

        coordinator.close()
    }

    @Test
    fun `suppressed transient interrupt reveals after suppression clears with full dwell`() = runTest {
        val connectivityState = MutableStateFlow(ConnectionState.CONNECTED)
        val takeoverSuppressed = MutableStateFlow(true)
        val coordinator = createCoordinator(
            parentScope = this,
            schedulerItems = MutableStateFlow(listOf(schedulerItem("最近：客户回访 · 09:00"))),
            connectivityState = connectivityState,
            takeoverSuppressed = takeoverSuppressed
        )

        runCurrent()
        connectivityState.value = ConnectionState.DISCONNECTED
        runCurrent()
        assertVisible(
            coordinator = coordinator,
            expectedLane = DynamicIslandLane.SCHEDULER,
            expectedText = "最近：客户回访 · 09:00"
        )

        advanceTimeBy(10_000L)
        runCurrent()
        takeoverSuppressed.value = false
        runCurrent()
        assertVisible(
            coordinator = coordinator,
            expectedLane = DynamicIslandLane.CONNECTIVITY,
            expectedText = "Badge 已断开"
        )

        advanceTimeBy(2_999L)
        runCurrent()
        assertVisible(
            coordinator = coordinator,
            expectedLane = DynamicIslandLane.CONNECTIVITY,
            expectedText = "Badge 已断开"
        )

        advanceTimeBy(1L)
        runCurrent()
        assertVisible(
            coordinator = coordinator,
            expectedLane = DynamicIslandLane.SCHEDULER,
            expectedText = "最近：客户回访 · 09:00"
        )

        coordinator.close()
    }

    @Test
    fun `latest suppressed transient wins when multiple state changes happen before reveal`() = runTest {
        val connectivityState = MutableStateFlow(ConnectionState.CONNECTED)
        val takeoverSuppressed = MutableStateFlow(true)
        val coordinator = createCoordinator(
            parentScope = this,
            schedulerItems = MutableStateFlow(listOf(schedulerItem("最近：客户回访 · 09:00"))),
            connectivityState = connectivityState,
            takeoverSuppressed = takeoverSuppressed
        )

        runCurrent()
        connectivityState.value = ConnectionState.DISCONNECTED
        runCurrent()
        connectivityState.value = ConnectionState.CONNECTED
        runCurrent()

        takeoverSuppressed.value = false
        runCurrent()
        assertVisible(
            coordinator = coordinator,
            expectedLane = DynamicIslandLane.CONNECTIVITY,
            expectedText = "Badge 已连接"
        )

        coordinator.close()
    }

    @Test
    fun `persistent state clears deferred transient when suppression ends`() = runTest {
        val connectivityState = MutableStateFlow(ConnectionState.CONNECTED)
        val takeoverSuppressed = MutableStateFlow(true)
        val coordinator = createCoordinator(
            parentScope = this,
            schedulerItems = MutableStateFlow(listOf(schedulerItem("最近：客户回访 · 09:00"))),
            connectivityState = connectivityState,
            takeoverSuppressed = takeoverSuppressed
        )

        runCurrent()
        connectivityState.value = ConnectionState.DISCONNECTED
        runCurrent()
        connectivityState.value = ConnectionState.RECONNECTING
        runCurrent()

        takeoverSuppressed.value = false
        runCurrent()
        assertVisible(
            coordinator = coordinator,
            expectedLane = DynamicIslandLane.CONNECTIVITY,
            expectedText = "Badge 重连中..."
        )

        coordinator.close()
    }

    private fun createCoordinator(
        parentScope: kotlinx.coroutines.CoroutineScope,
        schedulerItems: MutableStateFlow<List<DynamicIslandItem>>,
        connectivityState: MutableStateFlow<ConnectionState>,
        batteryLevel: MutableStateFlow<Int> = MutableStateFlow(85),
        takeoverSuppressed: MutableStateFlow<Boolean> = MutableStateFlow(false),
        syncEvents: kotlinx.coroutines.flow.MutableSharedFlow<com.smartsales.prism.data.audio.SimBadgeSyncIslandEvent> = kotlinx.coroutines.flow.MutableSharedFlow()
    ): SimShellDynamicIslandCoordinator {
        return SimShellDynamicIslandCoordinator(
            parentScope = parentScope,
            schedulerItems = schedulerItems,
            connectivityState = connectivityState,
            batteryLevel = batteryLevel,
            takeoverSuppressed = takeoverSuppressed,
            syncEvents = syncEvents
        )
    }

    private fun schedulerItem(text: String): DynamicIslandItem {
        return DynamicIslandItem(
            sessionTitle = "SIM",
            schedulerSummary = text
        )
    }

    private fun titleItem(text: String): DynamicIslandItem {
        return DynamicIslandItem(
            sessionTitle = text,
            displayText = text,
            visualState = DynamicIslandVisualState.SESSION_TITLE_HIGHLIGHT
        )
    }

    private fun scheduledTask(
        id: String,
        title: String,
        timeDisplay: String
    ) = com.smartsales.prism.domain.scheduler.ScheduledTask(
        id = id,
        title = title,
        timeDisplay = timeDisplay,
        startTime = Instant.parse("2026-03-22T08:00:00Z"),
        durationMinutes = 30
    )

    private fun assertVisible(
        coordinator: SimShellDynamicIslandCoordinator,
        expectedLane: DynamicIslandLane,
        expectedText: String
    ) {
        val visibleItem = coordinator.presentation.value.visibleItem
        assertEquals(expectedLane, visibleItem?.lane)
        assertEquals(expectedText, visibleItem?.displayText)
    }
}
