package com.smartsales.feature.usercenter

// 文件：feature/usercenter/src/main/java/com/smartsales/feature/usercenter/UserCenterScreen.kt
// 模块：:feature:usercenter
// 说明：用户中心 Compose 界面
// 作者：创建于 2025-11-30

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun UserCenterScreen(
    uiState: UserCenterUiState,
    onDeviceManagerClick: () -> Unit,
    onSubscriptionClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onGeneralSettingsClick: () -> Unit,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            .testTag(UserCenterTestTags.ROOT)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProfileHeader(
                displayName = uiState.displayName,
                email = uiState.email,
                isGuest = uiState.isGuest,
                onLoginClick = onLoginClick
            )
            ShortcutMenuCard(
                onOpenDeviceManager = onDeviceManagerClick,
                onOpenSubscription = onSubscriptionClick,
                onOpenPrivacy = onPrivacyClick,
                onOpenGeneral = onGeneralSettingsClick
            )
            if (uiState.canLogout) {
                DangerRow(
                    title = "退出登录",
                    icon = Icons.Default.Logout,
                    onClick = onLogoutClick,
                    modifier = Modifier.testTag(UserCenterTestTags.ROW_LOGOUT)
                )
            }
        }
    }
}

object UserCenterTestTags {
    const val ROOT = "user_center_screen_root"
    const val HEADER_NAME = "user_center_header_name"
    const val HEADER_EMAIL = "user_center_header_email"
    const val BUTTON_LOGIN = "user_center_button_login"
    const val ROW_DEVICE_MANAGER = "user_center_row_device_manager"
    const val ROW_SUBSCRIPTION = "user_center_row_subscription"
    const val ROW_PRIVACY = "user_center_row_privacy"
    const val ROW_GENERAL = "user_center_row_general"
    const val ROW_LOGOUT = "user_center_row_logout"
}

@Composable
private fun ProfileHeader(
    displayName: String,
    email: String,
    isGuest: Boolean,
    onLoginClick: () -> Unit
) {
    val resolvedName = if (isGuest) "访客用户" else displayName.ifBlank { "访客用户" }
    val resolvedEmail = if (isGuest) "请登录以管理账户" else email.ifBlank { "邮箱未填写" }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 22.dp, horizontal = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = resolvedName,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.testTag(UserCenterTestTags.HEADER_NAME)
        )
        Text(
            text = resolvedEmail,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag(UserCenterTestTags.HEADER_EMAIL)
        )
        if (isGuest) {
            OutlinedButton(
                onClick = onLoginClick,
                modifier = Modifier.testTag(UserCenterTestTags.BUTTON_LOGIN)
            ) {
                Text(text = "登录")
            }
        }
    }
    }
}

@Composable
private fun ShortcutMenuCard(
    onOpenDeviceManager: () -> Unit,
    onOpenSubscription: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenGeneral: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.extraLarge,
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ShortcutRow(
                title = "设备管理",
                subtitle = "管理已配对的设备与文件",
                icon = Icons.Outlined.Devices,
                onClick = onOpenDeviceManager,
                modifier = Modifier.testTag(UserCenterTestTags.ROW_DEVICE_MANAGER)
            )
            HorizontalDivider()
            Text(
                text = "账户",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ShortcutRow(
                title = "订阅管理",
                subtitle = "查看套餐、续费与发票",
                icon = Icons.Outlined.CreditCard,
                onClick = onOpenSubscription,
                modifier = Modifier.testTag(UserCenterTestTags.ROW_SUBSCRIPTION)
            )
            ShortcutRow(
                title = "隐私与安全",
                subtitle = "密码、双重认证、数据控制",
                icon = Icons.Outlined.PrivacyTip,
                onClick = onOpenPrivacy,
                modifier = Modifier.testTag(UserCenterTestTags.ROW_PRIVACY)
            )
            ShortcutRow(
                title = "通用设置",
                subtitle = "语言、通知与主题",
                icon = Icons.Outlined.Settings,
                onClick = onOpenGeneral,
                modifier = Modifier.testTag(UserCenterTestTags.ROW_GENERAL)
            )
        }
    }
}

@Composable
private fun ShortcutRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DangerRow(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.error
        )
    }
}
