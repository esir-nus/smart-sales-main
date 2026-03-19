package com.smartsales.prism.ui.sim

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartsales.prism.ui.components.PrismButton
import com.smartsales.prism.ui.components.PrismSurface
import com.smartsales.prism.ui.drawers.AudioSource
import com.smartsales.prism.ui.drawers.AudioStatus
import com.smartsales.prism.ui.theme.AccentSecondary
import com.smartsales.prism.ui.theme.BackgroundSurface
import com.smartsales.prism.ui.theme.BackdropScrim
import com.smartsales.prism.ui.theme.TextPrimary
import com.smartsales.prism.ui.theme.TextSecondary

@Composable
fun SimAudioDrawer(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onAskAi: (SimAudioDiscussion) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SimAudioDrawerViewModel
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.uiEvents.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(
            visible = isOpen,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackdropScrim)
                    .clickable { onDismiss() }
            )
        }

        AnimatedVisibility(
            visible = isOpen,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        ) {
            PrismSurface(
                modifier = modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                backgroundColor = BackgroundSurface.copy(alpha = 0.96f),
                elevation = 16.dp
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(4.dp)
                                .background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "录音文件",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 20.sp
                            ),
                            color = TextPrimary
                        )

                        Surface(
                            color = AccentSecondary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Smartphone,
                                    contentDescription = null,
                                    tint = AccentSecondary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "SIM 本地样本",
                                    color = AccentSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        items(entries, key = { it.item.id }) { entry ->
                            SimAudioCard(
                                entry = entry,
                                onToggleStar = { viewModel.toggleStar(entry.item.id) },
                                onTranscribe = { viewModel.startTranscription(entry.item.id) },
                                onAskAi = {
                                    viewModel.createDiscussion(entry.item.id)?.let(onAskAi)
                                }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SimAudioCard(
    entry: SimAudioEntry,
    onToggleStar: () -> Unit,
    onTranscribe: () -> Unit,
    onAskAi: () -> Unit
) {
    var expanded by remember { mutableStateOf(entry.item.status == AudioStatus.TRANSCRIBED) }

    PrismSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(20.dp),
        backgroundColor = Color.White.copy(alpha = 0.92f),
        elevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.item.filename,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = buildString {
                            append(entry.item.timeDisplay)
                            append(" • ")
                            append(
                                when (entry.item.source) {
                                    AudioSource.SMARTBADGE -> "SmartBadge"
                                    AudioSource.PHONE -> "Phone"
                                }
                            )
                        },
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = if (entry.item.isStarred) Color(0xFFF59E0B) else Color(0xFFB0BEC5),
                    modifier = Modifier.clickable(onClick = onToggleStar)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val statusLabel = when (entry.item.status) {
                AudioStatus.TRANSCRIBED -> "已转写"
                AudioStatus.TRANSCRIBING -> "转写中"
                AudioStatus.PENDING -> "待处理"
            }
            Surface(
                color = Color(0xFFF3F7FA),
                shape = RoundedCornerShape(999.dp)
            ) {
                Text(
                    text = statusLabel,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = entry.item.summary ?: entry.preview,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                entry.item.progress?.let { progress ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "当前进度 ${(progress * 100).toInt()}%",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (entry.item.status) {
                    AudioStatus.TRANSCRIBED -> {
                        PrismButton(
                            text = "Ask AI",
                            onClick = onAskAi,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                        )
                    }
                    AudioStatus.PENDING -> {
                        PrismButton(
                            text = "开始转写",
                            onClick = onTranscribe,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                        )
                    }
                    AudioStatus.TRANSCRIBING -> {
                        Text(
                            text = "SIM 正在演示状态流转，后续波次接入真实 Tingwu 生命周期。",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
