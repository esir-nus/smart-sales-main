package com.smartsales.aitest.ui.screens.user.model

// 文件：app/src/main/java/com/smartsales/aitest/ui/screens/user/model/UserModels.kt
// 模块：:app
// 说明：用户中心底部导航页的 UI 专用模型（本地模拟，无业务依赖）
// 作者：创建于 2025-12-02

import androidx.compose.material3.ColorScheme

data class UserProfileUi(
    val id: String,
    val fullName: String,
    val email: String,
    val phoneNumber: String?,
    val avatarUrl: String?,
    val role: UserRoleUi,
    val subscriptionTier: SubscriptionTierUi,
    val joinedDate: Long
)

enum class UserRoleUi { USER, ADMIN }

enum class SubscriptionTierUi {
    FREE,
    PREMIUM,
    ENTERPRISE
}

fun SubscriptionTierUi.label(): String = when (this) {
    SubscriptionTierUi.FREE -> "免费版"
    SubscriptionTierUi.PREMIUM -> "高级版"
    SubscriptionTierUi.ENTERPRISE -> "企业版"
}

data class TierColors(
    val container: androidx.compose.ui.graphics.Color,
    val onContainer: androidx.compose.ui.graphics.Color
)

fun SubscriptionTierUi.colors(colorScheme: ColorScheme): TierColors = when (this) {
    SubscriptionTierUi.FREE -> TierColors(
        container = colorScheme.surfaceVariant,
        onContainer = colorScheme.onSurfaceVariant
    )
    SubscriptionTierUi.PREMIUM -> TierColors(
        container = colorScheme.secondary,
        onContainer = colorScheme.onSecondary
    )
    SubscriptionTierUi.ENTERPRISE -> TierColors(
        container = colorScheme.tertiary,
        onContainer = colorScheme.onTertiary
    )
}
