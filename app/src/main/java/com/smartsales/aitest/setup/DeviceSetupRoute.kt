package com.smartsales.aitest.setup

// 文件：app/src/main/java/com/smartsales/aitest/setup/DeviceSetupRoute.kt
// 模块：:app
// 说明：为 DeviceSetup 标签提供占位 Compose 视图，待真实流程合入时补充
// 作者：创建于 2025-11-20

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartsales.feature.connectivity.setup.DeviceSetupScreen
import com.smartsales.feature.connectivity.setup.DeviceSetupViewModel
import com.smartsales.feature.connectivity.setup.DeviceSetupTestTags
import com.smartsales.feature.connectivity.setup.DeviceSetupEvent
import kotlinx.coroutines.flow.collect

@Composable
fun DeviceSetupRoute(
    modifier: Modifier = Modifier,
    onCompleted: () -> Unit,
    onBackToHome: () -> Unit = {},
    viewModel: DeviceSetupViewModel = hiltViewModel()
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                DeviceSetupEvent.OpenDeviceManager -> onCompleted()
                DeviceSetupEvent.BackToHome -> onBackToHome()
            }
        }
    }
    DeviceSetupScreen(
        state = state,
        onPrimaryClick = viewModel::onPrimaryClick,
        onSecondaryClick = viewModel::onSecondaryClick,
        onRetry = viewModel::onRetry,
        onDismissError = viewModel::onDismissError,
        onWifiSsidChanged = viewModel::onWifiSsidChanged,
        onWifiPasswordChanged = viewModel::onWifiPasswordChanged,
        modifier = modifier.testTag(DeviceSetupRouteTestTags.PAGE)
    )
}

object DeviceSetupRouteTestTags {
    const val PAGE = "device_setup_screen_root"
    // 兼容旧测试：指向主按钮
    const val COMPLETE_BUTTON = "device_setup_primary_button"
}
