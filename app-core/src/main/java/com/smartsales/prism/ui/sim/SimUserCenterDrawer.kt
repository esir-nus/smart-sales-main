package com.smartsales.prism.ui.sim

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.smartsales.prism.ui.settings.EditProfileScreen
import com.smartsales.prism.ui.settings.UserCenterViewModel

private val SimUserCenterDrawerShape = RoundedCornerShape(topStart = 36.dp, bottomStart = 36.dp)
private val SimUserCenterDrawerSurface = Color(0xED141416)
private val SimUserCenterDrawerCard = Color(0xA61C1C1E)
private val SimUserCenterDrawerBorder = Color.White.copy(alpha = 0.08f)
private val SimUserCenterDivider = Color.White.copy(alpha = 0.05f)
private val SimUserCenterSecondary = Color(0xFF8D8D93)
private val SimUserCenterMuted = Color(0xFFAEAEB2)
private val SimUserCenterAccent = Color(0xFF0A84FF)
private val SimUserCenterDanger = Color(0xFFFF453A)
private val SimUserCenterLogoutSurface = Color(0x14FF453A)
private val SimUserCenterAvatarSurface = Color.White.copy(alpha = 0.05f)
private val SimUserCenterAvatarBorder = Color.White.copy(alpha = 0.14f)
private val SimUserCenterHeroGlow = Color.White.copy(alpha = 0.04f)
private val SimUserCenterCardHighlight = Color.White.copy(alpha = 0.03f)

@Composable
internal fun SimUserCenterDrawer(
    onClose: () -> Unit,
    viewModel: UserCenterViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    var isEditing by remember { mutableStateOf(false) }

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
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.CenterEnd
    ) {
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .width(330.dp),
            shape = SimUserCenterDrawerShape,
            color = SimUserCenterDrawerSurface,
            contentColor = Color.White
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(0.5.dp, SimUserCenterDrawerBorder, SimUserCenterDrawerShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                SimUserCenterHeroGlow,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.14f)
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "关闭个人中心",
                                tint = Color.White
                            )
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(28.dp)
                    ) {
                        item {
                            profile?.let { user ->
                                SimUserCenterProfileHero(
                                    profile = user,
                                    onEdit = { isEditing = true }
                                )
                            }
                        }

                        item {
                            SimUserCenterSection(title = "偏好设置") {
                                SimUserCenterSelectRow(label = "主题外观", value = "跟随系统") {}
                                SimUserCenterToggleRow(label = "AI 实验室", checked = true) {}
                                SimNotificationSettingsRow(viewModel = viewModel)
                            }
                        }

                        item {
                            SimUserCenterSection(title = "空间管理") {
                                SimUserCenterActionRow(label = "本地缓存", actionLabel = "清除 (128MB)") {}
                            }
                        }

                        item {
                            SimUserCenterSection(title = "安全与隐私") {
                                SimUserCenterNavRow(label = "修改密码") {}
                                SimUserCenterNavRow(label = "面容 ID") {}
                            }
                        }

                        item {
                            SimUserCenterSection(title = "关于") {
                                SimUserCenterInfoRow(label = "版本", value = "Prism v1.2 (Pro Max)")
                                SimUserCenterNavRow(label = "帮助中心") {}
                            }
                        }

                        item {
                            SimUserCenterLogoutButton(onClick = onClose)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SimUserCenterProfileHero(
    profile: UserProfile,
    onEdit: () -> Unit
) {
    val chips = remember(profile) {
        buildList {
            profile.industry.trim().takeIf { it.isNotEmpty() }?.let(::add)
            profile.experienceYears.trim().takeIf { it.isNotEmpty() }?.let(::add)
            profile.communicationPlatform.trim().takeIf { it.isNotEmpty() }?.let(::add)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(SimUserCenterAvatarSurface, CircleShape)
                .border(1.dp, SimUserCenterAvatarBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (profile.displayName.isNotBlank()) {
                Text(
                    text = profile.displayName.take(2).uppercase(),
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = profile.displayName,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = profile.role,
                color = SimUserCenterSecondary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }

        if (chips.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                chips.forEach { chip ->
                    SimUserCenterMetadataChip(text = chip)
                }
                SimUserCenterMetadataChip(text = "PRO", accent = true)
            }
        } else {
            SimUserCenterMetadataChip(text = "PRO", accent = true)
        }

        Surface(
            modifier = Modifier.clickable(onClick = onEdit),
            color = Color.White.copy(alpha = 0.06f),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "编辑资料",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun SimUserCenterMetadataChip(
    text: String,
    accent: Boolean = false
) {
    val background = if (accent) {
        SimUserCenterAccent.copy(alpha = 0.15f)
    } else {
        Color.White.copy(alpha = 0.06f)
    }
    val border = if (accent) {
        SimUserCenterAccent.copy(alpha = 0.35f)
    } else {
        SimUserCenterDrawerBorder
    }
    val textColor = if (accent) SimUserCenterAccent else SimUserCenterMuted

    Surface(
        color = background,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, border)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (accent) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun SimUserCenterSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            color = SimUserCenterSecondary,
            fontSize = 13.sp,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(start = 12.dp)
        )
        Surface(
            color = SimUserCenterDrawerCard,
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, SimUserCenterDrawerBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                SimUserCenterCardHighlight,
                                Color.Transparent
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
private fun SimUserCenterSelectRow(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    SimUserCenterRow(
        label = label,
        onClick = onClick,
        rightContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    color = SimUserCenterMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.18f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    )
}

@Composable
private fun SimUserCenterNavRow(
    label: String,
    onClick: () -> Unit
) {
    SimUserCenterRow(
        label = label,
        onClick = onClick,
        rightContent = {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.18f),
                modifier = Modifier.size(18.dp)
            )
        }
    )
}

@Composable
private fun SimUserCenterActionRow(
    label: String,
    actionLabel: String,
    onClick: () -> Unit
) {
    SimUserCenterRow(
        label = label,
        rightContent = {
            Text(
                text = actionLabel,
                color = SimUserCenterAccent,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        },
        onClick = onClick
    )
}

@Composable
private fun SimUserCenterInfoRow(
    label: String,
    value: String
) {
    SimUserCenterRow(
        label = label,
        rightContent = {
            Text(
                text = value,
                color = SimUserCenterMuted,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        onClick = null
    )
}

@Composable
private fun SimUserCenterToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    SimUserCenterRow(
        label = label,
        onClick = null,
        rightContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = SimUserCenterAccent,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color.White.copy(alpha = 0.12f),
                    uncheckedBorderColor = Color.Transparent
                )
            )
        }
    )
}

@Composable
private fun SimNotificationSettingsRow(
    viewModel: UserCenterViewModel
) {
    val context = LocalContext.current
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

    SimUserCenterToggleRow(
        label = "消息通知",
        checked = notificationsEnabled,
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
private fun SimUserCenterRow(
    label: String,
    rightContent: @Composable () -> Unit,
    onClick: (() -> Unit)?
) {
    val rowModifier = Modifier
        .fillMaxWidth()
        .then(
            if (onClick != null) {
                Modifier.clickable(onClick = onClick)
            } else {
                Modifier
            }
        )
        .padding(horizontal = 16.dp, vertical = 15.dp)

    Column {
        Row(
            modifier = rowModifier,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
            rightContent()
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            thickness = 0.5.dp,
            color = SimUserCenterDivider
        )
    }
}

@Composable
private fun SimUserCenterLogoutButton(onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SimUserCenterLogoutSurface,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, SimUserCenterDanger.copy(alpha = 0.24f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                tint = SimUserCenterDanger,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "退出登录",
                color = SimUserCenterDanger,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
