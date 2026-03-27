package com.smartsales.prism.data.connectivity.legacy.gateway

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GattBleGatewayNotificationParsingTest {

    @Test
    fun `parseBadgeNotificationPayload maps tim get to time sync request`() {
        assertEquals(
            BadgeNotification.TimeSyncRequested,
            parseBadgeNotificationPayload("tim#get")
        )
    }

    @Test
    fun `parseBadgeNotificationPayload maps log payload to recording ready`() {
        val notification = parseBadgeNotificationPayload(" log#20260322_170000 ")
        assertTrue(notification is BadgeNotification.RecordingReady)
        assertEquals(
            "20260322_170000",
            (notification as BadgeNotification.RecordingReady).filename
        )
    }

    @Test
    fun `parseBadgeNotificationPayload preserves unknown command`() {
        val notification = parseBadgeNotificationPayload("wifi#address#0.0.0.0")
        assertEquals(
            BadgeNotification.Unknown("wifi#address#0.0.0.0"),
            notification
        )
    }

    @Test
    fun `parseBadgeNotificationPayload ignores blank payload`() {
        assertNull(parseBadgeNotificationPayload("   "))
    }

    @Test
    fun `mergeNetworkFragments treats 0 dot 0 dot 0 dot 0 as offline status instead of parser failure`() {
        val status = mergeNetworkFragments(
            listOf(
                "IP#0.0.0.0",
                "SD#N/A"
            )
        )

        assertEquals("0.0.0.0", status.ipAddress)
        assertEquals("", status.deviceWifiName)
        assertEquals("", status.phoneWifiName)
    }
}
