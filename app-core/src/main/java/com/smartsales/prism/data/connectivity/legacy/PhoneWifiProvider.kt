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

sealed interface PhoneWifiSnapshot {
    data object Unavailable : PhoneWifiSnapshot
    data class Connected(
        val normalizedSsid: String?,
        val rawSsid: String? = null
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

            PhoneWifiSnapshot.Connected(
                normalizedSsid = normalizeWifiSsid(rawSsid),
                rawSsid = rawSsid
            )
        }.getOrElse {
            PhoneWifiSnapshot.Unavailable
        }
    }
}

class FakePhoneWifiProvider(
    var snapshot: PhoneWifiSnapshot = PhoneWifiSnapshot.Unavailable
) : PhoneWifiProvider {
    constructor(currentSsid: String?) : this(
        snapshot = if (currentSsid == null) {
            PhoneWifiSnapshot.Unavailable
        } else {
            PhoneWifiSnapshot.Connected(
                normalizedSsid = normalizeWifiSsid(currentSsid),
                rawSsid = currentSsid
            )
        }
    )

    override fun currentWifiSnapshot(): PhoneWifiSnapshot = snapshot
}
