package com.smartsales.feature.usercenter

// 文件：feature/usercenter/src/main/java/com/smartsales/feature/usercenter/UserCenterScreen.kt
// 模块：:feature:usercenter
// 说明：用户中心 Compose 界面
// 作者：创建于 2025-11-21

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun UserCenterScreen(
    uiState: UserCenterUiState,
    onUserNameChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onToggleFeatureFlag: (String) -> Unit,
    onSaveClicked: () -> Unit,
    onLogoutClicked: () -> Unit,
    onErrorDismissed: () -> Unit,
    onOpenDeviceManager: () -> Unit = {},
    onOpenSubscription: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onOpenGeneral: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.18f))
            .testTag(UserCenterTestTags.ROOT),
        topBar = {
        TopAppBar(
            title = { Text(text = "用户中心") },
            actions = {
                IconButton(onClick = onSaveClicked, enabled = !uiState.isSaving) {
                    Icon(Icons.Default.Save, contentDescription = "保存")
                }
            }
        )
    }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "管理账号、订阅和设置",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.size(4.dp))
            ProfileHeader(userName = uiState.userName, email = uiState.email)
            if (uiState.isSaving) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            uiState.errorMessage?.let { message ->
                ErrorBanner(
                    message = message,
                    onDismiss = onErrorDismissed,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.userName,
                        onValueChange = onUserNameChanged,
                        label = { Text(text = "用户名") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uiState.email,
                        onValueChange = onEmailChanged,
                        label = { Text(text = "邮箱") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = uiState.tokensRemaining?.let { "剩余 Tokens：$it" } ?: "剩余 Tokens：--",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            FeatureFlagCard(
                flags = uiState.featureFlags,
                onToggle = onToggleFeatureFlag,
                modifier = Modifier.fillMaxWidth()
            )
            ShortcutMenuCard(
                onOpenDeviceManager = onOpenDeviceManager,
                onOpenSubscription = onOpenSubscription,
                onOpenPrivacy = onOpenPrivacy,
                onOpenGeneral = onOpenGeneral,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onSaveClicked,
                    enabled = !uiState.isSaving,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = if (uiState.isSaving) "保存中..." else "保存")
                }
                TextButton(
                    onClick = onLogoutClicked,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = "退出登录")
                }
            }
        }
    }
}

object UserCenterTestTags {
    const val ROOT = "user_center_screen_root"
}

@Composable
private fun ProfileHeader(userName: String, email: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.large)
            .padding(vertical = 20.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = userName.take(1).ifBlank { "U" },
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = if (userName.isBlank()) "未命名用户" else userName,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = if (email.isBlank()) "邮箱未填写" else email,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ShortcutMenuCard(modifier: Modifier = Modifier) {
    ShortcutMenuCard(
        onOpenDeviceManager = {},
        onOpenSubscription = {},
        onOpenPrivacy = {},
        onOpenGeneral = {},
        modifier = modifier
    )
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
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "快捷入口", style = MaterialTheme.typography.titleMedium)
            ShortcutRow(
                title = "设备管理",
                subtitle = "管理已配对的设备与文件",
                icon = Icons.Outlined.Devices,
                onClick = onOpenDeviceManager
            )
            HorizontalDivider()
            ShortcutRow(
                title = "订阅管理",
                subtitle = "套餐、续费与发票",
                icon = Icons.Outlined.CreditCard,
                onClick = onOpenSubscription
            )
            HorizontalDivider()
            ShortcutRow(
                title = "隐私与安全",
                subtitle = "密码、双重认证、数据控制",
                icon = Icons.Outlined.PrivacyTip,
                onClick = onOpenPrivacy
            )
            HorizontalDivider()
            ShortcutRow(
                title = "通用设置",
                subtitle = "语言、通知与主题",
                icon = Icons.Outlined.Settings,
                onClick = onOpenGeneral
            )
        }
    }
}

@Composable
private fun ShortcutRow(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
private fun FeatureFlagCard(
    flags: Map<String, Boolean>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "功能开关", style = MaterialTheme.typography.titleMedium)
            if (flags.isEmpty()) {
                Text(
                    text = "暂无可配置的功能开关。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                flags.entries.forEachIndexed { index, entry ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = entry.key, style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = entry.value,
                            onCheckedChange = { onToggle(entry.key) }
                        )
                    }
                    if (index != flags.size - 1) {
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
        TextButton(onClick = onDismiss) {
            Text(text = "关闭")
        }
    }
}
