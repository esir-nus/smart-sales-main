package com.smartsales.prism.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartsales.prism.domain.memory.UserProfile
import com.smartsales.prism.ui.components.connectivity.ConnectivityViewModel
import com.smartsales.prism.ui.components.PrismStatusBarTopSafeArea
import com.smartsales.prism.ui.components.prismNavigationBarPadding
import com.smartsales.prism.ui.theme.PrismThemeDefaults
import com.smartsales.prism.ui.theme.toDisplayLabel

/**
 * Full-app User Center polished against the screenshot-first light overlay direction.
 */
@Composable
fun UserCenterScreen(
    onClose: () -> Unit,
    viewModel: UserCenterViewModel = hiltViewModel(),
    connectivityViewModel: ConnectivityViewModel = hiltViewModel()
) {
    val colors = PrismThemeDefaults.colors
    val isDarkTheme = PrismThemeDefaults.isDarkTheme
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val firmwareVersion by connectivityViewModel.firmwareVersion.collectAsStateWithLifecycle()
    val sdCardSpace by connectivityViewModel.sdCardSpace.collectAsStateWithLifecycle()
    var isEditing by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    BackHandler {
        if (isEditing) {
            isEditing = false
        } else {
            onClose()
        }
    }

    if (isEditing && profile != null) {
        EditProfileScreen(
            profile = profile!!,
            onSave = { name, role, industry, exp, platform ->
                viewModel.updateProfile(name, role, industry, exp, platform)
                isEditing = false
            },
            onBack = { isEditing = false }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 18.dp, end = 18.dp, bottom = 10.dp)
    ) {
        val sheetShape = RoundedCornerShape(34.dp)
        val sheetColor = if (isDarkTheme) {
            colors.surface.copy(alpha = 0.992f)
        } else {
            Color(0xFFFAFBFE)
        }
        val sheetBorder = if (isDarkTheme) {
            colors.borderSubtle.copy(alpha = 0.82f)
        } else {
            Color(0xFFE1E6EF)
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PrismStatusBarTopSafeArea()

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 356.dp)
                    .weight(1f)
                    .shadow(
                        elevation = if (isDarkTheme) 18.dp else 28.dp,
                        shape = sheetShape,
                        clip = false,
                        ambientColor = Color.Black.copy(alpha = if (isDarkTheme) 0.24f else 0.12f),
                        spotColor = Color.Black.copy(alpha = if (isDarkTheme) 0.30f else 0.10f)
                    ),
                shape = sheetShape,
                color = sheetColor,
                border = BorderStroke(1.dp, sheetBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isDarkTheme) 0.03f else 0.10f),
                                    Color.Transparent,
                                    Color(0xFFF4F7FB).copy(alpha = if (isDarkTheme) 0.015f else 0.04f)
                                )
                            )
                        )
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .prismNavigationBarPadding(),
                        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 26.dp, bottom = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            profile?.let { user ->
                                UserCenterHero(
                                    profile = user,
                                    onEdit = { isEditing = true }
                                )
                            }
                        }

                        item {
                            UserCenterSection(title = "偏好设置") {
                                UserCenterSelectRow(
                                    label = "主题外观",
                                    value = themeMode.toDisplayLabel(),
                                    onClick = { showThemeDialog = true },
                                    showDivider = true
                                )
                                UserCenterToggleRow(
                                    label = "AI 实验室",
                                    checked = true,
                                    showDivider = true,
                                    onCheckedChange = {}
                                )
                                NotificationSettingsRow(
                                    viewModel = viewModel,
                                    showDivider = false
                                )
                            }
                        }

                        item {
                            UserCenterSection(title = "设备控制") {
                                val voiceVolume by viewModel.voiceVolume.collectAsStateWithLifecycle()
                                UserCenterSliderRow(
                                    label = "徽章语音音量",
                                    value = voiceVolume,
                                    onValueChange = viewModel::onVoiceVolumeDrag,
                                    onValueChangeFinished = viewModel::onVoiceVolumeCommitted,
                                    showDivider = true
                                )
                                UserCenterRefreshInfoRow(
                                    label = "Badge firmware",
                                    value = firmwareVersion?.let { "Ver. $it" } ?: "Ver. —",
                                    showDivider = true,
                                    onRefresh = connectivityViewModel::requestFirmwareVersion
                                )
                                BadgeStorageRow(
                                    label = "徽章 SD 卡剩余空间",
                                    value = sdCardSpace ?: "轻触查询",
                                    onClick = connectivityViewModel::requestSdCardSpace,
                                    showDivider = false
                                )
                            }
                        }

                        item {
                            UserCenterSection(title = "空间管理") {
                                UserCenterInfoRow(
                                    label = "已用空间",
                                    value = "128 MB",
                                    leadingIcon = Icons.Default.Storage,
                                    showDivider = true
                                )
                                UserCenterActionRow(
                                    label = "清除缓存",
                                    actionLabel = "清理",
                                    showDivider = false,
                                    onClick = {}
                                )
                            }
                        }

                        item {
                            UserCenterSection(title = "安全与隐私") {
                                UserCenterNavRow(
                                    label = "修改密码",
                                    leadingIcon = Icons.Default.Lock,
                                    showDivider = false,
                                    onClick = {}
                                )
                            }
                        }

                        item {
                            UserCenterSection(title = "关于") {
                                UserCenterNavRow(
                                    label = "帮助中心",
                                    leadingIcon = Icons.AutoMirrored.Filled.HelpOutline,
                                    showDivider = true,
                                    onClick = {}
                                )
                                UserCenterInfoRow(
                                    label = "版本",
                                    value = "Prism v1.2 (Pro Max)",
                                    leadingIcon = Icons.Default.Info,
                                    showDivider = false
                                )
                            }
                        }

                        item {
                            UserCenterLogoutButton(onClick = onClose)
                        }
                    }

                    if (showThemeDialog) {
                        ThemeModeDialog(
                            currentMode = themeMode,
                            onDismiss = { showThemeDialog = false },
                            onSelectMode = viewModel::setThemeMode
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UserCenterHero(
    profile: UserProfile,
    onEdit: () -> Unit
) {
    val colors = PrismThemeDefaults.colors
    val isDarkTheme = PrismThemeDefaults.isDarkTheme
    val chips = remember(profile) {
        listOf(
            profile.industry,
            profile.experienceYears,
            profile.communicationPlatform
        ).filter { it.isNotBlank() }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            if (isDarkTheme) colors.surfaceHover.copy(alpha = 0.76f) else Color.White.copy(alpha = 0.98f),
                            if (isDarkTheme) colors.surface.copy(alpha = 0.96f) else Color(0xFFF1F4F9)
                        )
                    )
                )
                .border(1.dp, colors.borderSubtle.copy(alpha = if (isDarkTheme) 0.80f else 0.96f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            val initials = profile.displayName
                .split(" ", "-", "_")
                .filter { it.isNotBlank() }
                .map { it.first().uppercase() }
                .joinToString("")
                .take(2)
                .ifBlank { profile.displayName.take(2).uppercase() }

            if (initials.isNotBlank()) {
                Text(
                    text = initials,
                    color = colors.textPrimary,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = colors.textPrimary.copy(alpha = 0.82f),
                    modifier = Modifier.size(31.dp)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = profile.displayName,
                color = colors.textPrimary,
                fontSize = 21.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = profile.role,
                color = colors.textSecondary.copy(alpha = if (isDarkTheme) 0.88f else 0.74f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }

        if (chips.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                chips.forEach { chip ->
                    UserCenterMetadataChip(text = chip)
                }
                UserCenterMetadataChip(text = "PRO", accent = true)
            }
        } else {
            UserCenterMetadataChip(text = "PRO", accent = true)
        }

        Surface(
            modifier = Modifier.clickable(onClick = onEdit),
            color = if (isDarkTheme) colors.surface.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.96f),
            shape = RoundedCornerShape(17.dp),
            border = BorderStroke(
                0.75.dp,
                if (isDarkTheme) colors.borderSubtle.copy(alpha = 0.70f) else Color(0xFFE2E7F0)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = colors.textPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "编辑资料",
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun UserCenterMetadataChip(
    text: String,
    accent: Boolean = false
) {
    val colors = PrismThemeDefaults.colors
    val isDarkTheme = PrismThemeDefaults.isDarkTheme
    val background = if (accent) {
        colors.accentBlue.copy(alpha = if (isDarkTheme) 0.18f else 0.11f)
    } else {
        if (isDarkTheme) colors.surface.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.88f)
    }
    val border = if (accent) {
        colors.accentBlue.copy(alpha = if (isDarkTheme) 0.32f else 0.22f)
    } else {
        colors.borderSubtle.copy(alpha = if (isDarkTheme) 0.76f else 0.86f)
    }

    Surface(
        color = background,
        shape = RoundedCornerShape(11.dp),
        border = BorderStroke(0.75.dp, border)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            color = if (accent) {
                colors.accentBlue
            } else {
                colors.textSecondary.copy(alpha = if (isDarkTheme) 0.88f else 0.74f)
            },
            fontSize = 10.sp,
            fontWeight = if (accent) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun UserCenterSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = PrismThemeDefaults.colors
    val isDarkTheme = PrismThemeDefaults.isDarkTheme
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            color = colors.textSecondary.copy(alpha = if (isDarkTheme) 0.80f else 0.64f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 8.dp)
        )
        Surface(
            color = if (isDarkTheme) colors.surfaceHover.copy(alpha = 0.60f) else Color.White.copy(alpha = 0.985f),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(
                0.8.dp,
                if (isDarkTheme) colors.borderSubtle.copy(alpha = 0.82f) else Color(0xFFE3E8F1)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isDarkTheme) 0.03f else 0.08f),
                                Color.Transparent,
                                Color(0xFFF5F8FC).copy(alpha = if (isDarkTheme) 0.015f else 0.03f)
                            )
                        )
                    )
            ) {
                content()
            }
        }
    }
}

@Composable
private fun UserCenterSelectRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    showDivider: Boolean
) {
    UserCenterRow(
        label = label,
        leadingIcon = Icons.Default.Public,
        value = value,
        showChevron = true,
        onClick = onClick,
        showDivider = showDivider
    )
}

@Composable
private fun BadgeStorageRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    showDivider: Boolean
) {
    UserCenterRow(
        label = label,
        leadingIcon = Icons.Default.Storage,
        value = value,
        showChevron = true,
        onClick = onClick,
        showDivider = showDivider
    )
}

@Composable
private fun UserCenterSliderRow(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    onValueChangeFinished: () -> Unit,
    showDivider: Boolean
) {
    val colors = PrismThemeDefaults.colors
    Column {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = colors.textSecondary.copy(alpha = 0.82f),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = label,
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = value.toString(),
                    color = colors.textSecondary.copy(alpha = 0.72f),
                    fontSize = 12.sp
                )
            }
            // 拖动期间仅刷新本地状态，松手 (onValueChangeFinished) 才下发 BLE，
            // 避免 ESP32 在高频写入下崩溃。
            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.toInt()) },
                onValueChangeFinished = onValueChangeFinished,
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = colors.accentBlue,
                    activeTrackColor = colors.accentBlue,
                    inactiveTrackColor = colors.borderSubtle.copy(alpha = 0.6f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (showDivider) {
            HorizontalDivider(
                color = colors.borderSubtle.copy(alpha = 0.4f),
                thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = 14.dp)
            )
        }
    }
}

@Composable
private fun UserCenterToggleRow(
    label: String,
    checked: Boolean,
    showDivider: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = PrismThemeDefaults.colors
    UserCenterRow(
        label = label,
        leadingIcon = when (label) {
            "AI 实验室" -> Icons.Default.Psychology
            "消息通知" -> Icons.Default.NotificationsNone
            else -> Icons.Default.Person
        },
        onClick = null,
        showDivider = showDivider,
        rightContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = colors.accentBlue,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFFD8DDEA),
                    uncheckedBorderColor = Color.Transparent
                )
            )
        }
    )
}

@Composable
private fun NotificationSettingsRow(
    viewModel: UserCenterViewModel,
    showDivider: Boolean
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var notificationsEnabled by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsEnabled = viewModel.hasNotificationPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(viewModel) {
        notificationsEnabled = viewModel.hasNotificationPermission()
    }

    UserCenterToggleRow(
        label = "消息通知",
        checked = notificationsEnabled,
        showDivider = showDivider,
        onCheckedChange = {
            val intent = Intent().apply {
                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
            context.startActivity(intent)
        }
    )
}

@Composable
private fun UserCenterNavRow(
    label: String,
    leadingIcon: ImageVector,
    showDivider: Boolean,
    onClick: () -> Unit
) {
    UserCenterRow(
        label = label,
        leadingIcon = leadingIcon,
        showChevron = true,
        onClick = onClick,
        showDivider = showDivider
    )
}

@Composable
private fun UserCenterInfoRow(
    label: String,
    value: String,
    leadingIcon: ImageVector,
    showDivider: Boolean
) {
    UserCenterRow(
        label = label,
        leadingIcon = leadingIcon,
        value = value,
        onClick = null,
        showDivider = showDivider
    )
}

@Composable
private fun UserCenterRefreshInfoRow(
    label: String,
    value: String,
    showDivider: Boolean,
    onRefresh: () -> Unit
) {
    val colors = PrismThemeDefaults.colors
    UserCenterRow(
        label = label,
        leadingIcon = Icons.Default.Info,
        value = value,
        onClick = null,
        showDivider = showDivider,
        rightContent = {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Refresh firmware version",
                tint = colors.accentBlue,
                modifier = Modifier
                    .size(16.dp)
                    .clickable(onClick = onRefresh)
            )
        }
    )
}

@Composable
private fun UserCenterActionRow(
    label: String,
    actionLabel: String,
    showDivider: Boolean,
    onClick: () -> Unit
) {
    val colors = PrismThemeDefaults.colors
    UserCenterRow(
        label = label,
        leadingIcon = Icons.Default.Delete,
        value = actionLabel,
        valueColor = colors.accentBlue,
        valueFontWeight = FontWeight.Medium,
        onClick = onClick,
        showDivider = showDivider
    )
}

@Composable
private fun UserCenterRow(
    label: String,
    leadingIcon: ImageVector,
    value: String? = null,
    valueColor: Color? = null,
    valueFontWeight: FontWeight = FontWeight.Normal,
    showChevron: Boolean = false,
    rightContent: (@Composable RowScope.() -> Unit)? = null,
    onClick: (() -> Unit)?,
    showDivider: Boolean
) {
    val colors = PrismThemeDefaults.colors
    val rowModifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 48.dp)
        .then(
            if (onClick != null) {
                Modifier.clickable(onClick = onClick)
            } else {
                Modifier
            }
        )
        .padding(horizontal = 14.dp, vertical = 11.dp)

    Column {
        Row(
            modifier = rowModifier,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = colors.textSecondary.copy(alpha = 0.82f),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = label,
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                if (value != null) {
                    Text(
                        text = value,
                        color = (valueColor ?: colors.textSecondary).copy(
                            alpha = if (valueColor == null) 0.72f else 1f
                        ),
                        fontSize = 12.sp,
                        fontWeight = valueFontWeight
                    )
                }
                rightContent?.invoke(this)
                if (showChevron) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = colors.textTertiary.copy(alpha = 0.72f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 41.dp, end = 14.dp),
                thickness = 0.5.dp,
                color = colors.borderSubtle.copy(alpha = 0.84f)
            )
        }
    }
}

@Composable
private fun UserCenterLogoutButton(onClick: () -> Unit) {
    val colors = PrismThemeDefaults.colors
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = colors.surfaceDanger.copy(alpha = 0.72f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(0.75.dp, colors.accentDanger.copy(alpha = 0.16f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                tint = colors.accentDanger,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "退出登录",
                color = colors.accentDanger,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
