package com.smartsales.prism.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.prism.domain.pairing.DiscoveredBadge
import com.smartsales.prism.domain.pairing.PairingService
import com.smartsales.prism.domain.pairing.PairingState
import com.smartsales.prism.domain.pairing.WifiCredentials
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 配对流程 ViewModel。
 *
 * 只负责 BLE 扫描、配网与取消；不携带完整 onboarding 的资料保存职责。
 */
@HiltViewModel
class PairingFlowViewModel @Inject constructor(
    private val pairingService: PairingService
) : ViewModel() {

    val pairingState: StateFlow<PairingState> = pairingService.state

    fun startScan() {
        viewModelScope.launch {
            pairingService.startScan()
        }
    }

    fun pairBadge(badge: DiscoveredBadge, wifiCreds: WifiCredentials) {
        viewModelScope.launch {
            pairingService.pairBadge(badge, wifiCreds)
        }
    }

    fun cancelPairing() {
        pairingService.cancelPairing()
    }
}
