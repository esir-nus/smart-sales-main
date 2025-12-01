package com.smartsales.feature.connectivity

// 文件：feature/connectivity/src/test/java/com/smartsales/feature/connectivity/BleProfileConfigTest.kt
// 模块：:feature:connectivity
// 说明：验证 BLE profile 关键字匹配对 BT311 命名的兼容性
// 作者：创建于 2025-11-21

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class BleProfileConfigTest {
    private val baseProfile = BleProfileConfig(
        id = "bt311",
        displayName = "BT311",
        nameKeywords = listOf("BT311", "BT-311"),
        scanServiceUuids = listOf(UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E"))
    )

    @Test
    fun `matches accepts hyphenated bt311 name`() {
        assertTrue(baseProfile.matches("BT-311_DEV", emptyList()))
    }

    @Test
    fun `matches accepts folded name without hyphen`() {
        assertTrue(baseProfile.matches("bt311_dev", emptyList()))
    }

    @Test
    fun `matches accepts advertised uuid hit`() {
        val uuid = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
        assertTrue(baseProfile.matches("Unknown", listOf(uuid)))
    }

    @Test
    fun `matches rejects unrelated device`() {
        assertFalse(baseProfile.matches("RandomSpeaker", emptyList()))
    }
}
