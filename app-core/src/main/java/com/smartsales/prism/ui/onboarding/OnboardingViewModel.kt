package com.smartsales.prism.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.prism.domain.pairing.*
import com.smartsales.prism.domain.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Onboarding ViewModel — 注入 PairingService + UserProfileRepository
 * 
 * 负责协调设备配对流程 + 保存用户资料
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val pairingService: PairingService,
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {
    
    /**
     * 配对状态流
     * UI 观察此流以响应扫描、配对、成功或错误状态
     */
    val pairingState: StateFlow<PairingState> = pairingService.state
    
    /**
     * 开始扫描 BLE 设备
     */
    fun startScan() {
        viewModelScope.launch {
            pairingService.startScan()
        }
    }
    
    /**
     * 配对设备并配置 WiFi
     * 
     * @param badge 扫描发现的设备
     * @param wifiCreds WiFi 凭证
     */
    fun pairBadge(badge: DiscoveredBadge, wifiCreds: WifiCredentials) {
        viewModelScope.launch {
            pairingService.pairBadge(badge, wifiCreds)
        }
    }
    
    /**
     * 取消配对流程
     */
    fun cancelPairing() {
        pairingService.cancelPairing()
    }

    /**
     * 保存用户资料（Onboarding ProfileStep）
     */
    fun saveProfile(displayName: String, role: String) {
        viewModelScope.launch {
            val current = userProfileRepository.getProfile()
            userProfileRepository.updateProfile(
                current.copy(
                    displayName = displayName.ifBlank { current.displayName },
                    role = role.ifBlank { current.role },
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }
}
