package com.smartsales.aitest

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// 文件：aiFeatureTestApp/src/main/java/com/smartsales/aitest/PhoneWifiMonitor.kt
// 模块：:aiFeatureTestApp
// 说明：监听手机当前连接的 Wi-Fi 名称，供测试页面比对网络
// 作者：创建于 2025-11-18
interface PhoneWifiMonitor {
    val currentSsid: StateFlow<String?>
}

@Singleton
class AndroidPhoneWifiMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) : PhoneWifiMonitor {

    private val wifiManager: WifiManager? =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private val connectivityManager: ConnectivityManager? =
        context.getSystemService(ConnectivityManager::class.java)

    private val _ssid = MutableStateFlow(readCurrentSsid())
    override val currentSsid: StateFlow<String?> = _ssid.asStateFlow()

    private val wifiReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            _ssid.value = readCurrentSsid()
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _ssid.value = readCurrentSsid()
        }

        override fun onLost(network: Network) {
            _ssid.value = readCurrentSsid()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                _ssid.value = readCurrentSsid()
            }
        }
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && connectivityManager != null && hasNetworkPermission()) {
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()
            registerNetworkCallback(request)
        } else {
            val filter = IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION).apply {
                @Suppress("DEPRECATION")
                addAction(ConnectivityManager.CONNECTIVITY_ACTION)
            }
            context.registerReceiver(wifiReceiver, filter)
        }
    }

    @SuppressLint("MissingPermission")
    private fun readCurrentSsid(): String? {
        if (!hasLocationPermission()) return null
        return try {
            val wifiInfo: WifiInfo? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val manager = connectivityManager
                manager
                    ?.getNetworkCapabilities(manager.activeNetwork)
                    ?.transportInfo as? WifiInfo ?: legacyConnectionInfo()
            } else {
                legacyConnectionInfo()
            }
            sanitizeSsid(wifiInfo?.ssid)
        } catch (ex: SecurityException) {
            null
        }
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    private fun hasNetworkPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_NETWORK_STATE
        ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private fun registerNetworkCallback(request: NetworkRequest) {
        runCatching { connectivityManager?.registerNetworkCallback(request, networkCallback) }
    }

    private fun sanitizeSsid(raw: String?): String? {
        if (raw.isNullOrBlank() || raw == "<unknown ssid>") return null
        return raw.trim().trim('"').ifBlank { null }
    }

    @Suppress("DEPRECATION")
    private fun legacyConnectionInfo(): WifiInfo? = wifiManager?.connectionInfo
}

@Module
@InstallIn(SingletonComponent::class)
abstract class WifiMonitorModule {
    @Binds
    @Singleton
    abstract fun bindPhoneWifiMonitor(impl: AndroidPhoneWifiMonitor): PhoneWifiMonitor
}
