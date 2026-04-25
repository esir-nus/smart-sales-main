package com.smartsales.prism.data.connectivity.registry

import com.smartsales.prism.data.connectivity.legacy.BlePeripheral
import com.smartsales.prism.data.connectivity.legacy.BleSession

/**
 * 设备注册表数据模型
 *
 * 支持多设备管理：注册、重命名、设为默认、切换连接。
 * MAC 地址为唯一标识，匹配 BlePeripheral.id / BleSession.peripheralId。
 */
data class RegisteredDevice(
    val macAddress: String,
    val displayName: String,
    val profileId: String?,
    val registeredAtMillis: Long,
    val lastConnectedAtMillis: Long,
    val isDefault: Boolean,
    val manuallyDisconnected: Boolean = false,
    val bleDetected: Boolean = false
) {
    companion object {
        fun fromPairing(
            peripheral: BlePeripheral,
            session: BleSession,
            isDefault: Boolean = false
        ): RegisteredDevice = RegisteredDevice(
            macAddress = peripheral.id,
            displayName = peripheral.name,
            profileId = peripheral.profileId,
            registeredAtMillis = session.establishedAtMillis,
            lastConnectedAtMillis = session.establishedAtMillis,
            isDefault = isDefault
        )

        fun fromSession(
            session: BleSession,
            isDefault: Boolean = true
        ): RegisteredDevice = RegisteredDevice(
            macAddress = session.peripheralId,
            displayName = session.peripheralName,
            profileId = session.profileId,
            registeredAtMillis = session.establishedAtMillis,
            lastConnectedAtMillis = System.currentTimeMillis(),
            isDefault = isDefault
        )
    }

    val macSuffix: String
        get() {
            val parts = macAddress.split(":")
            return if (parts.size >= 2) "...${parts.takeLast(2).joinToString(":")}" else macAddress
        }
}
