package com.smartsales.prism.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectivityModalDeviceCardSubtitleTest {

    @Test
    fun `eligible inactive registered card shows click to connect instead of out of range`() {
        val subtitle = connectivityDeviceCardSubtitle(
            isConnected = false,
            batteryLevel = null,
            firmwareVersion = null,
            isCheckingUpdate = false,
            isUpdateFound = false,
            isUpdating = false,
            isReconnecting = false,
            isBlePaired = false,
            bleDetected = false,
            manuallyDisconnected = false,
            isActive = false
        )

        assertEquals("点击连接", subtitle)
    }

    @Test
    fun `ble detected inactive registered card shows detected click to connect`() {
        val subtitle = connectivityDeviceCardSubtitle(
            isConnected = false,
            batteryLevel = null,
            firmwareVersion = null,
            isCheckingUpdate = false,
            isUpdateFound = false,
            isUpdating = false,
            isReconnecting = false,
            isBlePaired = false,
            bleDetected = true,
            manuallyDisconnected = false,
            isActive = false
        )

        assertEquals("已检测到 · 点击连接", subtitle)
    }

    @Test
    fun `ble detected active registered card stays disconnected first`() {
        val subtitle = connectivityDeviceCardSubtitle(
            isConnected = false,
            batteryLevel = null,
            firmwareVersion = null,
            isCheckingUpdate = false,
            isUpdateFound = false,
            isUpdating = false,
            isReconnecting = false,
            isBlePaired = false,
            bleDetected = true,
            manuallyDisconnected = false,
            isActive = true
        )

        assertEquals("已断开 · 点击重连", subtitle)
    }

    @Test
    fun `active disconnected card keeps out of range copy`() {
        val subtitle = connectivityDeviceCardSubtitle(
            isConnected = false,
            batteryLevel = null,
            firmwareVersion = null,
            isCheckingUpdate = false,
            isUpdateFound = false,
            isUpdating = false,
            isReconnecting = false,
            isBlePaired = false,
            bleDetected = false,
            manuallyDisconnected = false,
            isActive = true
        )

        assertEquals("不在范围内", subtitle)
    }
}
