package com.smartsales.aitest.ui.components

// 文件：app/src/main/java/com/smartsales/aitest/ui/components/ProfileHeaderCard.kt
// 模块：:app
// 说明：用户中心的头像与基本信息卡片
// 作者：创建于 2025-12-02

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.smartsales.aitest.ui.screens.user.model.SubscriptionTierUi
import com.smartsales.aitest.ui.screens.user.model.TierColors
import com.smartsales.aitest.ui.screens.user.model.UserProfileUi
import com.smartsales.aitest.ui.screens.user.model.colors
import com.smartsales.aitest.ui.screens.user.model.label

@Composable
fun ProfileHeaderCard(
    userProfile: UserProfileUi?,
    onEditClick: () -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AvatarBox(userProfile?.avatarUrl)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = userProfile?.fullName ?: "未登录",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                userProfile?.email?.let { email ->
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                userProfile?.let { profile ->
                    SubscriptionBadge(tier = profile.subscriptionTier)
                }
            }
            if (userProfile != null) {
                OutlinedButton(onClick = onEditClick) {
                    Text(text = "编辑资料")
                }
            } else {
                Button(onClick = onLoginClick) {
                    Text(text = "登录")
                }
            }
        }
    }
}

@Composable
private fun AvatarBox(avatarUrl: String?) {
    val size = 80.dp
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        if (avatarUrl != null) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = "用户头像",
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            androidx.compose.material3.Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SubscriptionBadge(tier: SubscriptionTierUi) {
    val colors: TierColors = tier.colors(MaterialTheme.colorScheme)
    Surface(
        color = colors.container,
        contentColor = colors.onContainer,
        shape = RoundedCornerShape(50),
        tonalElevation = 0.dp
    ) {
        Text(
            text = tier.label(),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
