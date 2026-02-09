package com.smartsales.feature.connectivity.gateway

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * 测试 mergeNetworkFragments —— WiFi 查询的 BLE 分片响应合并逻辑。
 * 
 * Badge 回复 wifi#address#ip#name 时，分多条 BLE 通知发送：
 *   IP#<地址>  SD#<SSID>
 * 顺序不保证，需全部收集后合并。
 */
class MergeNetworkFragmentsTest {

    @Test
    fun `happy path - IP then SD`() {
        val result = mergeNetworkFragments(listOf("IP#192.168.0.101", "SD#MstRobot"))
        
        assertEquals("192.168.0.101", result.ipAddress)
        assertEquals("MstRobot", result.deviceWifiName)
        assertEquals("MstRobot", result.phoneWifiName)
    }

    @Test
    fun `reversed order - SD then IP`() {
        val result = mergeNetworkFragments(listOf("SD#MstRobot", "IP#192.168.0.101"))
        
        assertEquals("192.168.0.101", result.ipAddress)
        assertEquals("MstRobot", result.deviceWifiName)
    }

    @Test
    fun `IP only - SD defaults to unknown`() {
        val result = mergeNetworkFragments(listOf("IP#10.0.0.5"))
        
        assertEquals("10.0.0.5", result.ipAddress)
        assertEquals("未知Wi-Fi", result.deviceWifiName)
    }

    @Test
    fun `no IP fragment - throws`() {
        val ex = assertThrows(IllegalStateException::class.java) {
            mergeNetworkFragments(listOf("SD#MstRobot"))
        }
        assert(ex.message!!.contains("IP"))
    }

    @Test
    fun `0_0_0_0 IP - throws badge offline`() {
        val ex = assertThrows(IllegalStateException::class.java) {
            mergeNetworkFragments(listOf("IP#0.0.0.0", "SD#MstRobot"))
        }
        assert(ex.message!!.contains("WiFi"))
    }

    @Test
    fun `empty list - throws`() {
        assertThrows(IllegalStateException::class.java) {
            mergeNetworkFragments(emptyList())
        }
    }

    @Test
    fun `case insensitive keys`() {
        val result = mergeNetworkFragments(listOf("ip#172.16.0.1", "sd#TestWiFi"))
        
        assertEquals("172.16.0.1", result.ipAddress)
        assertEquals("TestWiFi", result.deviceWifiName)
    }

    @Test
    fun `rawResponse contains all fragments`() {
        val result = mergeNetworkFragments(listOf("IP#192.168.1.1", "SD#Home"))
        
        assert(result.rawResponse.contains("IP#192.168.1.1"))
        assert(result.rawResponse.contains("SD#Home"))
    }
}
