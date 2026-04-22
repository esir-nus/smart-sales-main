package com.smartsales.prism.data.connectivity.legacy

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface PhoneWifiProvider {
    fun currentWifiSnapshot(): PhoneWifiSnapshot
}

fun PhoneWifiProvider.currentNormalizedSsid(): String? {
    return when (val snapshot = currentWifiSnapshot()) {
        PhoneWifiSnapshot.Unavailable -> null
        is PhoneWifiSnapshot.Connected -> snapshot.normalizedSsid
    }
}

sealed interface PhoneWifiSnapshot {
    data object Unavailable : PhoneWifiSnapshot
    data class Connected(
        val normalizedSsid: String?,
        val rawSsid: String? = null,
        // NET_CAPABILITY_VALIDATED: 手机 WiFi 已通过互联网可达性验证
        val isValidated: Boolean = false
    ) : PhoneWifiSnapshot
}

@Singleton
class AndroidPhoneWifiProvider @Inject constructor(
    @ApplicationContext context: Context
) : PhoneWifiProvider {
    private val appContext = context.applicationContext

    override fun currentWifiSnapshot(): PhoneWifiSnapshot {
        return runCatching {
            val connectivityManager = appContext.getSystemService(ConnectivityManager::class.java)
                ?: return PhoneWifiSnapshot.Unavailable
            val wifiManager = appContext.getSystemService(WifiManager::class.java)
                ?: return PhoneWifiSnapshot.Unavailable

            val activeNetwork = connectivityManager.activeNetwork
            val capabilities = activeNetwork?.let(connectivityManager::getNetworkCapabilities)
            val transportInfoSsid = (capabilities?.transportInfo as? WifiInfo)?.ssid
            val connectionInfo = wifiManager.connectionInfo
            val connectionInfoSsid = connectionInfo?.ssid

            val hasWifiTransport = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            val hasWifiConnectionInfo = connectionInfo?.networkId?.let { it != -1 } == true
            if (!hasWifiTransport && !hasWifiConnectionInfo) {
                return PhoneWifiSnapshot.Unavailable
            }

            val rawSsid = sequenceOf(transportInfoSsid, connectionInfoSsid)
                .mapNotNull { it?.trim()?.takeIf(String::isNotBlank) }
                .firstOrNull()

            // 检测网络是否通过 Android 可达性验证（隔离网络判断的关键信号）
            val isValidated = capabilities
                ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true

            PhoneWifiSnapshot.Connected(
                normalizedSsid = normalizeWifiSsid(rawSsid),
                rawSsid = rawSsid,
                isValidated = isValidated
            )
        }.getOrElse {
            PhoneWifiSnapshot.Unavailable
        }
    }
}

class FakePhoneWifiProvider(
    var snapshot: PhoneWifiSnapshot = PhoneWifiSnapshot.Unavailable
) : PhoneWifiProvider {
    constructor(currentSsid: String?, isValidated: Boolean = false) : this(
        snapshot = if (currentSsid == null) {
            PhoneWifiSnapshot.Unavailable
        } else {
            PhoneWifiSnapshot.Connected(
                normalizedSsid = normalizeWifiSsid(currentSsid),
                rawSsid = currentSsid,
                isValidated = isValidated
            )
        }
    )

    override fun currentWifiSnapshot(): PhoneWifiSnapshot = snapshot
}
