package com.smartsales.prism.ui.sim

import com.smartsales.prism.ui.components.DynamicIslandItem
import com.smartsales.prism.ui.components.DynamicIslandLane
import com.smartsales.prism.ui.components.DynamicIslandVisualState
import com.smartsales.prism.ui.components.connectivity.ConnectionState
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
    fun `connectivity change interrupts for three seconds then yields back to scheduler`() = runTest {
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
    fun `heartbeat surfaces connected lane every thirty seconds with mock battery`() = runTest {
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

        advanceTimeBy(2_499L)
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

        advanceTimeBy(3_000L)
        runCurrent()
        assertVisible(
            coordinator = coordinator,
            expectedLane = DynamicIslandLane.SCHEDULER,
            expectedText = "最近：客户回访 · 09:00"
        )

        coordinator.close()
    }

    private fun createCoordinator(
        parentScope: kotlinx.coroutines.CoroutineScope,
        schedulerItems: MutableStateFlow<List<DynamicIslandItem>>,
        connectivityState: MutableStateFlow<ConnectionState>,
        batteryLevel: MutableStateFlow<Int> = MutableStateFlow(85),
        takeoverSuppressed: MutableStateFlow<Boolean> = MutableStateFlow(false)
    ): SimShellDynamicIslandCoordinator {
        return SimShellDynamicIslandCoordinator(
            parentScope = parentScope,
            schedulerItems = schedulerItems,
            connectivityState = connectivityState,
            batteryLevel = batteryLevel,
            takeoverSuppressed = takeoverSuppressed
        )
    }

    private fun schedulerItem(text: String): DynamicIslandItem {
        return DynamicIslandItem(
            sessionTitle = "SIM",
            schedulerSummary = text
        )
    }

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
