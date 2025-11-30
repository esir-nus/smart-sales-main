package com.smartsales.aitest.usercenter

// 文件：app/src/main/java/com/smartsales/aitest/usercenter/UserCenterRoute.kt
// 模块：:app
// 说明：用户中心页面的 Route，连接 ViewModel 与 UI
// 作者：创建于 2025-11-30

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartsales.feature.usercenter.UserCenterEvent
import com.smartsales.feature.usercenter.UserCenterScreen
import com.smartsales.feature.usercenter.UserCenterViewModel

@Composable
fun UserCenterRoute(
    modifier: Modifier = Modifier,
    onLogout: () -> Unit = {},
    onOpenDeviceManager: () -> Unit = {},
    onOpenSubscription: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onOpenGeneral: () -> Unit = {}
) {
    val viewModel: UserCenterViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                UserCenterEvent.Logout -> onLogout()
                UserCenterEvent.DeviceManager -> onOpenDeviceManager()
                UserCenterEvent.Subscription -> onOpenSubscription()
                UserCenterEvent.Privacy -> onOpenPrivacy()
                UserCenterEvent.General -> onOpenGeneral()
                UserCenterEvent.Login -> Unit
            }
        }
    }

    UserCenterScreen(
        uiState = uiState,
        onDeviceManagerClick = viewModel::onDeviceManagerClick,
        onSubscriptionClick = viewModel::onSubscriptionClick,
        onPrivacyClick = viewModel::onPrivacyClick,
        onGeneralSettingsClick = viewModel::onGeneralSettingsClick,
        onLoginClick = viewModel::onLoginClick,
        onLogoutClick = viewModel::onLogoutClick,
        modifier = modifier
    )
}
