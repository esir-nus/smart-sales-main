package com.smartsales.feature.connectivity

import android.bluetooth.BluetoothManager
import android.content.Context
import com.smartsales.core.util.DispatcherProvider
import com.smartsales.feature.connectivity.BleProfileConfig
import com.smartsales.feature.connectivity.BuildConfig
import com.smartsales.feature.connectivity.gateway.BleGateway
import com.smartsales.feature.connectivity.gateway.GattBleGateway
import com.smartsales.feature.connectivity.scan.AndroidBleScanner
import com.smartsales.feature.connectivity.scan.BleScanner
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.UUID
import javax.inject.Singleton
import org.json.JSONArray

// 文件路径: feature/connectivity/src/main/java/com/smartsales/feature/connectivity/ConnectivityModule.kt
// 文件作用: 暴露默认的设备连接管理器实现
// 最近修改: 2025-11-14
@Module
@InstallIn(SingletonComponent::class)
interface ConnectivityModule {
    @Binds
    @Singleton
    fun bindDeviceConnectionManager(impl: DefaultDeviceConnectionManager): DeviceConnectionManager
}

@Module
@InstallIn(SingletonComponent::class)
object ConnectivityProvidesModule {
    @Provides
    @Singleton
    fun provideWifiProvisioner(
        real: AndroidBleWifiProvisioner,
        simulated: SimulatedWifiProvisioner
    ): WifiProvisioner {
        return if (BuildConfig.USE_SIMULATED_PROVISIONER) simulated else real
    }

    @Provides
    @Singleton
    fun provideBleGateway(
        @ApplicationContext context: Context,
        bluetoothManager: BluetoothManager,
        dispatchers: DispatcherProvider
    ): BleGateway = GattBleGateway(context, bluetoothManager, dispatchers)

    @Provides
    @Singleton
    fun provideBleProfiles(): List<BleProfileConfig> {
        val overrides = parseProfileOverrides(BuildConfig.BLE_PROFILE_OVERRIDES)
        if (!overrides.isNullOrEmpty()) {
            ConnectivityLogger.i("Loaded ${overrides.size} BLE profiles from overrides")
            return overrides
        }
        val defaultService = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
        val defaultProfile = BleProfileConfig(
            id = "bt311",
            displayName = "BT311 / Nordic UART",
            nameKeywords = listOf("BT311"),
            scanServiceUuids = listOf(defaultService)
        )
        return listOf(defaultProfile)
    }

    @Provides
    @Singleton
    fun provideBleScanner(
        @ApplicationContext context: Context,
        bluetoothManager: BluetoothManager,
        profiles: List<BleProfileConfig>
    ): BleScanner = AndroidBleScanner(context, bluetoothManager, profiles)

    @Provides
    fun provideBluetoothManager(
        @ApplicationContext context: Context
    ): BluetoothManager = context.getSystemService(BluetoothManager::class.java)

    private fun parseProfileOverrides(raw: String): List<BleProfileConfig>? {
        if (raw.isBlank()) return null
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val scanServiceUuids = when {
                        obj.has("scanServiceUuids") ->
                            obj.getJSONArray("scanServiceUuids").toUuidList()
                        obj.has("serviceUuid") ->
                            listOf(UUID.fromString(obj.getString("serviceUuid")))
                        else -> emptyList()
                    }
                    add(
                        BleProfileConfig(
                            id = obj.getString("id"),
                            displayName = obj.optString("displayName", obj.getString("id")),
                            nameKeywords = obj.optJSONArray("nameKeywords")?.toStringList() ?: emptyList(),
                            scanServiceUuids = scanServiceUuids
                        )
                    )
                }
            }
        }.onFailure { ConnectivityLogger.w("Failed to parse BLE profile overrides", it) }.getOrNull()
    }

    private fun JSONArray.toStringList(): List<String> {
        val result = mutableListOf<String>()
        for (i in 0 until length()) {
            val value = optString(i).takeIf { it.isNotBlank() } ?: continue
            result.add(value)
        }
        return result
    }

    private fun JSONArray.toUuidList(): List<UUID> {
        val result = mutableListOf<UUID>()
        for (i in 0 until length()) {
            val value = optString(i).takeIf { it.isNotBlank() } ?: continue
            result.add(UUID.fromString(value))
        }
        return result
    }
}
