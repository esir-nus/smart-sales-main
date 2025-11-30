package com.smartsales.feature.usercenter

// 文件：feature/usercenter/src/main/java/com/smartsales/feature/usercenter/UserCenterModels.kt
// 模块：:feature:usercenter
// 说明：用户中心的状态与事件模型
// 作者：创建于 2025-11-30

data class UserCenterUiState(
    val displayName: String = "",
    val email: String = "",
    val isGuest: Boolean = true,
    val canLogout: Boolean = false
)

sealed interface UserCenterEvent {
    data object Logout : UserCenterEvent
    data object DeviceManager : UserCenterEvent
    data object Subscription : UserCenterEvent
    data object Privacy : UserCenterEvent
    data object General : UserCenterEvent
    data object Login : UserCenterEvent
}

data class UserProfile(
    val displayName: String,
    val email: String,
    val isGuest: Boolean
)
