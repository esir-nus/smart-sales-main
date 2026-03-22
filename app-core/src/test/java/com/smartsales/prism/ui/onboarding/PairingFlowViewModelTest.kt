package com.smartsales.prism.ui.onboarding

import com.smartsales.prism.domain.pairing.DiscoveredBadge
import com.smartsales.prism.domain.pairing.PairingResult
import com.smartsales.prism.domain.pairing.PairingService
import com.smartsales.prism.domain.pairing.PairingState
import com.smartsales.prism.domain.pairing.WifiCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.After
import org.junit.Test

class PairingFlowViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var service: FakePairingService
    private lateinit var viewModel: PairingFlowViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        service = FakePairingService()
        viewModel = PairingFlowViewModel(service)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `exposes pairing service state`() {
        val expected = PairingState.DeviceFound(
            DiscoveredBadge("badge-1", "SmartBadge Pro", -42)
        )
        service.stateFlow.value = expected

        assertEquals(expected, viewModel.pairingState.value)
    }

    @Test
    fun `delegates startScan pairBadge and cancelPairing`() = runTest {
        val badge = DiscoveredBadge("badge-1", "SmartBadge Pro", -42)
        val creds = WifiCredentials("OfficeWifi", "secret")

        viewModel.startScan()
        viewModel.pairBadge(badge, creds)
        viewModel.cancelPairing()
        advanceUntilIdle()

        assertEquals(1, service.startScanCalls)
        assertEquals(badge to creds, service.pairCalls.single())
        assertEquals(1, service.cancelCalls)
    }

    private class FakePairingService : PairingService {
        val stateFlow = MutableStateFlow<PairingState>(PairingState.Idle)
        override val state: StateFlow<PairingState> = stateFlow

        var startScanCalls = 0
        var cancelCalls = 0
        val pairCalls = mutableListOf<Pair<DiscoveredBadge, WifiCredentials>>()

        override suspend fun startScan() {
            startScanCalls++
        }

        override suspend fun pairBadge(
            badge: DiscoveredBadge,
            wifiCreds: WifiCredentials
        ): PairingResult {
            pairCalls += badge to wifiCreds
            return PairingResult.Success(badge.id)
        }

        override fun cancelPairing() {
            cancelCalls++
        }
    }
}
