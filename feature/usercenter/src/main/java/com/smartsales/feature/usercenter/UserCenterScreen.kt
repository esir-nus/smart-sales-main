package com.smartsales.feature.usercenter

// 文件：feature/usercenter/src/main/java/com/smartsales/feature/usercenter/UserCenterScreen.kt
// 模块：:feature:usercenter
// 说明：用户中心 Compose 界面，支持编辑个人信息
// 作者：创建于 2025-11-30

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
    onPrivacyClick: () -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onRoleChange: (String) -> Unit,
    onIndustryChange: (String) -> Unit,
    onSave: () -> Unit,
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
                role = uiState.role,
                industry = uiState.industry,
                isLoading = uiState.isLoading,
                errorMessage = uiState.errorMessage,
                onDisplayNameChange = onDisplayNameChange,
                onRoleChange = onRoleChange,
                onIndustryChange = onIndustryChange,
                onSave = onSave
            )
            ShortcutMenuCard(
                onOpenDeviceManager = onDeviceManagerClick,
                onOpenPrivacy = onPrivacyClick,
                versionText = uiState.appVersion
            )
        }
    }
}

object UserCenterTestTags {
    const val ROOT = "user_center_screen_root"
    const val ROW_DEVICE_MANAGER = "user_center_row_device_manager"
    const val ROW_PRIVACY = "user_center_row_privacy"
    const val ROW_ABOUT = "user_center_row_about"
    const val FIELD_NAME = "user_center_field_name"
    const val FIELD_ROLE = "user_center_field_role"
    const val FIELD_INDUSTRY = "user_center_field_industry"
    const val BUTTON_SAVE = "user_center_button_save"
}

@Composable
private fun ProfileHeader(
    displayName: String,
    role: String,
    industry: String,
    isLoading: Boolean,
    errorMessage: String?,
    onDisplayNameChange: (String) -> Unit,
    onRoleChange: (String) -> Unit,
    onIndustryChange: (String) -> Unit,
    onSave: () -> Unit
) {
    val resolvedName = displayName.ifBlank { "SmartSales 用户" }
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
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                EditableField(
                    label = "姓名",
                    value = resolvedName,
                    onValueChange = onDisplayNameChange,
                    testTag = UserCenterTestTags.FIELD_NAME
                )
                EditableField(
                    label = "职位/角色",
                    value = role,
                    onValueChange = onRoleChange,
                    testTag = UserCenterTestTags.FIELD_ROLE
                )
                EditableField(
                    label = "行业",
                    value = industry,
                    onValueChange = onIndustryChange,
                    testTag = UserCenterTestTags.FIELD_INDUSTRY
                )
                OutlinedButton(
                    onClick = onSave,
                    modifier = Modifier.testTag(UserCenterTestTags.BUTTON_SAVE)
                ) {
                    Text(text = "保存")
                }
            }
        }
    }
}

@Composable
private fun ShortcutMenuCard(
    onOpenDeviceManager: () -> Unit,
    onOpenPrivacy: () -> Unit,
    versionText: String,
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
                text = "偏好与安全",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ShortcutRow(
                title = "隐私与安全",
                subtitle = "密码、隐私声明、数据控制",
                icon = Icons.Outlined.PrivacyTip,
                onClick = onOpenPrivacy,
                modifier = Modifier.testTag(UserCenterTestTags.ROW_PRIVACY)
            )
            HorizontalDivider()
            ShortcutRow(
                title = "关于",
                subtitle = versionText.ifBlank { "版本信息" },
                icon = Icons.Filled.Info,
                onClick = {},
                modifier = Modifier.testTag(UserCenterTestTags.ROW_ABOUT)
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
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(
            imageVector = Icons.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EditableField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    testTag: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        )
    }
}
