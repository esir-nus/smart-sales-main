package com.smartsales.feature.connectivity.setup

// 文件：feature/connectivity/src/main/java/com/smartsales/feature/connectivity/setup/DeviceSetupModels.kt
// 模块：:feature:connectivity
// 说明：定义设备配网的步骤、状态与 UI 模型
// 作者：创建于 2025-11-21

enum class DeviceSetupStep {
    Idle,
    Scanning,
    Pairing,
    WifiProvisioning,
    WaitingForDeviceOnline,
    Ready,
    Error
}

enum class DeviceSetupErrorReason {
    ScanTimeout,
    ProvisioningFailed,
    DeviceNotOnline,
    Unknown
}

data class DeviceSetupUiState(
    val step: DeviceSetupStep = DeviceSetupStep.Idle,
    val progressMessage: String = "准备开始连接设备",
    val errorMessage: String? = null,
    val errorReason: DeviceSetupErrorReason? = null,
    val isActionInProgress: Boolean = false,
    val deviceName: String? = null,
    val wifiSsid: String? = null,
    val isScanning: Boolean = false,
    val isSubmittingWifi: Boolean = false,
    val deviceIp: String? = null,
    val isDeviceOnline: Boolean = false
)
