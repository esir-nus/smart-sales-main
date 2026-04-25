package com.smartsales.prism.ui.sim

import com.smartsales.prism.data.audio.SIM_BADGE_SYNC_CONNECTIVITY_UNAVAILABLE_MESSAGE
import com.smartsales.prism.domain.connectivity.BadgeManagerStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SimBadgeSyncAvailabilityTest {

    @Test
    fun `resolveSimBadgeSyncAvailability maps ready and ble held manager states`() {
        assertEquals(
            SimBadgeSyncAvailability.READY,
            resolveSimBadgeSyncAvailability(BadgeManagerStatus.Ready(ssid = "Office"))
        )
        assertEquals(
            SimBadgeSyncAvailability.BLE_CONNECTED_NETWORK_PENDING,
            resolveSimBadgeSyncAvailability(BadgeManagerStatus.BlePairedNetworkUnknown)
        )
        assertEquals(
            SimBadgeSyncAvailability.BLE_CONNECTED_NETWORK_OFFLINE,
            resolveSimBadgeSyncAvailability(BadgeManagerStatus.BlePairedNetworkOffline)
        )
        assertEquals(
            SimBadgeSyncAvailability.UNAVAILABLE,
            resolveSimBadgeSyncAvailability(BadgeManagerStatus.Disconnected)
        )
    }

    @Test
    fun `resolveSimBadgeManualSyncBlockedMessage returns specific copy for partial connectivity`() =
        runTest {
            assertEquals(
                SIM_BADGE_SYNC_NETWORK_PENDING_MESSAGE,
                resolveSimBadgeManualSyncBlockedMessage(
                    availability = SimBadgeSyncAvailability.BLE_CONNECTED_NETWORK_PENDING,
                    canSyncFromBadge = { true }
                )
            )
            assertEquals(
                SIM_BADGE_SYNC_NETWORK_OFFLINE_MESSAGE,
                resolveSimBadgeManualSyncBlockedMessage(
                    availability = SimBadgeSyncAvailability.BLE_CONNECTED_NETWORK_OFFLINE,
                    canSyncFromBadge = { true }
                )
            )
        }

    @Test
    fun `resolveSimBadgeManualSyncBlockedMessage uses strict preflight for ready state`() =
        runTest {
            var canSyncChecks = 0
            assertEquals(
                SIM_BADGE_SYNC_CONNECTIVITY_UNAVAILABLE_MESSAGE,
                resolveSimBadgeManualSyncBlockedMessage(
                    availability = SimBadgeSyncAvailability.READY,
                    canSyncFromBadge = {
                        canSyncChecks += 1
                        false
                    }
                )
            )
            assertEquals(1, canSyncChecks)
        }

    @Test
    fun `resolveSimBadgeManualSyncBlockedMessage preserves strict readiness for unavailable state`() =
        runTest {
            assertEquals(
                null,
                resolveSimBadgeManualSyncBlockedMessage(
                    availability = SimBadgeSyncAvailability.UNAVAILABLE,
                    canSyncFromBadge = { true }
                )
            )
            assertEquals(
                SIM_BADGE_SYNC_CONNECTIVITY_UNAVAILABLE_MESSAGE,
                resolveSimBadgeManualSyncBlockedMessage(
                    availability = SimBadgeSyncAvailability.UNAVAILABLE,
                    canSyncFromBadge = { false }
                )
            )
        }

    @Test
    fun `resolveSimBadgeManualSyncGateDecision distinguishes manager blocks from strict precheck`() =
        runTest {
            assertEquals(
                SimBadgeManualSyncGateBranch.MANAGER_PENDING_BLOCK,
                resolveSimBadgeManualSyncGateDecision(
                    availability = SimBadgeSyncAvailability.BLE_CONNECTED_NETWORK_PENDING,
                    canSyncFromBadge = { true }
                ).branch
            )
            assertEquals(
                SimBadgeManualSyncGateBranch.MANAGER_OFFLINE_BLOCK,
                resolveSimBadgeManualSyncGateDecision(
                    availability = SimBadgeSyncAvailability.BLE_CONNECTED_NETWORK_OFFLINE,
                    canSyncFromBadge = { true }
                ).branch
            )
            assertEquals(
                SimBadgeManualSyncGateBranch.STRICT_PRECHECK_ALLOWED,
                resolveSimBadgeManualSyncGateDecision(
                    availability = SimBadgeSyncAvailability.READY,
                    canSyncFromBadge = { true }
                ).branch
            )
            assertEquals(
                SimBadgeManualSyncGateBranch.STRICT_PRECHECK_BLOCKED,
                resolveSimBadgeManualSyncGateDecision(
                    availability = SimBadgeSyncAvailability.READY,
                    canSyncFromBadge = { false }
                ).branch
            )
            assertEquals(
                SimBadgeManualSyncGateBranch.STRICT_PRECHECK_ALLOWED,
                resolveSimBadgeManualSyncGateDecision(
                    availability = SimBadgeSyncAvailability.UNAVAILABLE,
                    canSyncFromBadge = { true }
                ).branch
            )
            assertEquals(
                SimBadgeManualSyncGateBranch.STRICT_PRECHECK_BLOCKED,
                resolveSimBadgeManualSyncGateDecision(
                    availability = SimBadgeSyncAvailability.UNAVAILABLE,
                    canSyncFromBadge = { false }
                ).branch
            )
        }

    @Test
    fun `strict precheck block does not open wifi mismatch prompt`() {
        assertEquals(
            false,
            shouldPromptWifiMismatchForManualSyncBlock(
                SimBadgeManualSyncGateBranch.STRICT_PRECHECK_BLOCKED
            )
        )
        assertEquals(
            true,
            shouldPromptWifiMismatchForManualSyncBlock(
                SimBadgeManualSyncGateBranch.MANAGER_OFFLINE_BLOCK
            )
        )
    }
}
