package com.smartsales.prism.data.connectivity.registry

import com.smartsales.prism.data.connectivity.legacy.BlePeripheral
import com.smartsales.prism.data.connectivity.legacy.BleSession
import kotlinx.coroutines.flow.StateFlow

enum class DebugBleDetectionL25Scenario(
    val scenarioId: String,
    val manuallyDisconnectDefault: Boolean
) {
    ActiveOnlyDualAdvertise(
        scenarioId = "CONNECTIVITY_ACTIVE_ONLY_DUAL_ADVERTISE",
        manuallyDisconnectDefault = false
    ),
    ManualDefaultSuppression(
        scenarioId = "CONNECTIVITY_MANUAL_DEFAULT_SUPPRESSION",
        manuallyDisconnectDefault = true
    )
}

data class DebugBleDetectionL25Result(
    val scenarioId: String,
    val evidenceClass: String,
    val source: String,
    val defaultMac: String,
    val activeMac: String,
    val manuallyDisconnectedDefault: Boolean,
    val expectedSelectedMac: String,
    val selectedMac: String?,
    val defaultBleDetected: Boolean,
    val activeBleDetected: Boolean,
    val passed: Boolean
)

/**
 * 设备注册表管理器 — 多设备编排层
 *
 * 位于 DeviceConnectionManager（单设备）之上，管理"哪个设备"这个问题。
 * DeviceConnectionManager 继续管理"怎么连接"。
 */
interface DeviceRegistryManager {
    /** 所有已注册设备，响应式。注册/移除/切换后更新。 */
    val registeredDevices: StateFlow<List<RegisteredDevice>>

    /** 当前活跃设备（DeviceConnectionManager 正在操作的设备）。无设备时为 null。 */
    val activeDevice: StateFlow<RegisteredDevice?>

    /** 注册新配对的设备。第一个设备自动成为默认设备。 */
    fun registerDevice(peripheral: BlePeripheral, session: BleSession)

    /** 重命名设备。 */
    fun renameDevice(macAddress: String, newName: String)

    /** 设为默认设备。不会立即切换连接。 */
    fun setDefault(macAddress: String)

    /** 切换活跃设备。断开当前设备，启动新设备的重连。 */
    suspend fun switchToDevice(macAddress: String)

    /** 移除已注册设备。若为活跃设备，先断开。若为默认设备，自动提升其他设备。 */
    fun removeDevice(macAddress: String)

    /** 应用启动时调用。优先加载已注册的存储会话设备，执行迁移（如需），触发自动重连。 */
    fun initializeOnLaunch()

    /** 标记设备手动断开状态。值为 true 时跳过所有自动重连；connect() 时清除为 false。 */
    fun markManuallyDisconnected(macAddress: String, value: Boolean)

    /** 标记设备 BLE 可检测状态（在扫描范围内但尚未连接）。connect() 时自动清除。 */
    fun updateBleDetected(macAddress: String, value: Boolean)

    /** Debug：种子化历史默认优先级验证场景；正式路径不应调用。 */
    fun debugSeedDefaultPriorityScenario(): Boolean = false

    /** Debug：通过注册表候选选择路径模拟 BLE 检测；正式路径不应调用。 */
    suspend fun debugSimulateBleDetection(manuallyDisconnectDefault: Boolean): Boolean = false

    /** Debug L2.5：确定性模拟 BLE 候选输入，并返回可断言结果；正式路径不应调用。 */
    suspend fun debugRunBleDetectionL25Scenario(
        scenario: DebugBleDetectionL25Scenario
    ): DebugBleDetectionL25Result? = null
}
