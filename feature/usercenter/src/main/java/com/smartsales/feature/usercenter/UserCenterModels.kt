package com.smartsales.feature.usercenter

// 文件：feature/usercenter/src/main/java/com/smartsales/feature/usercenter/UserCenterModels.kt
// 模块：:feature:usercenter
// 说明：用户中心的状态与事件模型
// 作者：创建于 2025-11-21

data class UserCenterUiState(
    val userName: String = "",
    val email: String = "",
    val tokensRemaining: Int? = null,
    val featureFlags: Map<String, Boolean> = emptyMap(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val canLogout: Boolean = false
)

sealed interface UserCenterEvent {
    data object Logout : UserCenterEvent
}

data class UserProfile(
    val userName: String,
    val email: String,
    val tokensRemaining: Int?,
    val featureFlags: Map<String, Boolean>
)
