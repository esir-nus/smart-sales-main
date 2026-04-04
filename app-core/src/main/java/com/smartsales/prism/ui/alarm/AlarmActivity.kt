package com.smartsales.prism.ui.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.smartsales.prism.data.notification.AlarmDismissReceiver
import com.smartsales.prism.domain.notification.NotificationPriority
import com.smartsales.prism.domain.notification.NotificationService
import com.smartsales.prism.domain.notification.PrismNotificationChannel
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.delay

val AlarmStackCountKey = SemanticsPropertyKey<Int>("AlarmStackCount")
var SemanticsPropertyReceiver.alarmStackCount by AlarmStackCountKey

private val AlarmOverlayScrim = Color(0xEB0A0A0C)
private val AlarmGlassCard = Color(0x0FFFFFFF)
private val AlarmGlassBorder = Color(0x26FFFFFF)
private val AlarmHeaderMeta = Color(0xFFAEAEB2)
private val AlarmCardMeta = Color(0xFF86868B)
private val AlarmCritical = Color(0xFFFF453A)
private val AlarmButtonContainer = Color(0x1AFFFFFF)
private val AlarmCountBadge = Color.White
private val AlarmCountText = Color(0xFF0A0A0C)
private val AlarmVibeContainer = Color(0x1AFF453A)

internal fun upsertAlarmStack(existing: List<AlarmItem>, incoming: AlarmItem): List<AlarmItem> {
    return listOf(incoming) + existing.filterNot { it.taskId == incoming.taskId }
}

/**
 * 闹钟全屏 Activity
 *
 * 半透明覆盖层，显示任务名称和时间。
 * 支持多个闹钟同时显示（堆叠）。
 * launchMode="singleTop" —— 新闹钟通过 onNewIntent() 进入顶部。
 */
class AlarmActivity : ComponentActivity() {

    companion object {
        private const val TAG = "AlarmActivity"
        const val EXTRA_TASK_TITLE = "task_title"
        const val EXTRA_TIME_TEXT = "time_text"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        const val EXTRA_TASK_ID = "task_id"
        const val ACTION_FINISH_ALARM = "com.smartsales.prism.FINISH_ALARM"
        internal const val AUTO_DISMISS_MS = 60_000L
    }

    @dagger.hilt.EntryPoint
    @dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
    interface NotificationEntryPoint {
        fun notificationService(): NotificationService
    }

    private val alarmStack = mutableStateListOf<AlarmItem>()
    private var isDismissing = false

    private val finishReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != ACTION_FINISH_ALARM) return
            val targetTaskId = intent.getStringExtra(AlarmDismissReceiver.EXTRA_TASK_ID)
            if (!targetTaskId.isNullOrEmpty()) {
                removeAlarmByTaskId(targetTaskId)
            } else {
                Log.d(TAG, "收到通用关闭广播，关闭所有闹钟")
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        super.onCreate(savedInstanceState)

        val filter = IntentFilter(ACTION_FINISH_ALARM)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(finishReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(finishReceiver, filter)
        }

        addAlarmFromIntent(intent)

        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            NotificationEntryPoint::class.java
        )
        entryPoint.notificationService().startPersistentVibration()
        Log.d(TAG, "持续振动已启动 (onCreate)")

        setContent {
            BackHandler(enabled = true) {
                // 锁屏闹钟不允许直接返回离开
            }
            AlarmStackScreen(
                alarms = alarmStack,
                onDismissTop = { dismissAlarm(it) },
                onAutoDismiss = { autoDismissAll() }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        addAlarmFromIntent(intent)
        Log.d(TAG, "onNewIntent: 新闹钟已加入栈，当前数量: ${alarmStack.size}")
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(finishReceiver)
        } catch (_: Exception) {
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            Log.d(TAG, "音量键按下，关闭所有闹钟")
            dismissAllAlarms()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun addAlarmFromIntent(intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "任务提醒"
        val time = intent.getStringExtra(EXTRA_TIME_TEXT) ?: ""
        val notifId = intent.getStringExtra(EXTRA_NOTIFICATION_ID) ?: ""
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        val next = AlarmItem(taskId = taskId, notificationId = notifId, title = title, timeText = time)
        val updated = upsertAlarmStack(alarmStack.toList(), next)
        alarmStack.clear()
        alarmStack.addAll(updated)
    }

    private fun removeAlarmByTaskId(id: String) {
        alarmStack.removeAll { it.taskId == id }
        if (alarmStack.isEmpty()) {
            finish()
        }
    }

    private fun dismissAlarm(item: AlarmItem) {
        sendDismissBroadcast(item)
        alarmStack.remove(item)
        if (alarmStack.isEmpty()) {
            finish()
        }
    }

    private fun dismissAllAlarms() {
        if (isDismissing) return
        isDismissing = true
        val copy = alarmStack.toList()
        copy.forEach(::sendDismissBroadcast)
        alarmStack.clear()
        finish()
    }

    private fun autoDismissAll() {
        if (isDismissing) return
        isDismissing = true
        Log.d(TAG, "自动关闭: 1分钟无操作")

        val copy = alarmStack.toList()
        copy.forEach { item ->
            sendDismissBroadcast(item)
            sendMissedNotification(item)
        }
        alarmStack.clear()
        finish()
    }

    private fun sendDismissBroadcast(item: AlarmItem) {
        if (item.notificationId.isNotEmpty()) {
            val dismissIntent = Intent(this, AlarmDismissReceiver::class.java).apply {
                action = AlarmDismissReceiver.ACTION_DISMISS
                putExtra(AlarmDismissReceiver.EXTRA_NOTIFICATION_ID, item.notificationId)
                putExtra(AlarmDismissReceiver.EXTRA_TASK_ID, item.taskId)
                putExtra(AlarmDismissReceiver.EXTRA_OPEN_APP, false)
            }
            sendBroadcast(dismissIntent)
        }
    }

    private fun sendMissedNotification(item: AlarmItem) {
        try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                NotificationEntryPoint::class.java
            )
            entryPoint.notificationService().show(
                id = "${item.taskId}-missed",
                title = "未接提醒: ${item.title}",
                body = item.timeText,
                channel = PrismNotificationChannel.TASK_REMINDER_EARLY,
                priority = NotificationPriority.LOW
            )
        } catch (error: Exception) {
            Log.w(TAG, "未接提醒通知发送失败: ${error.message}")
        }
    }
}

internal data class AlarmItem(
    val taskId: String,
    val notificationId: String,
    val title: String,
    val timeText: String
)

@Composable
private fun AlarmStackScreen(
    alarms: List<AlarmItem>,
    onDismissTop: (AlarmItem) -> Unit,
    onAutoDismiss: () -> Unit
) {
    LaunchedEffect(alarms.size) {
        if (alarms.isEmpty()) return@LaunchedEffect
        delay(AlarmActivity.AUTO_DISMISS_MS)
        onAutoDismiss()
    }

    val primaryAlarm = alarms.firstOrNull() ?: return
    val secondaryAlarms = alarms.drop(1).take(2)
    val isStacked = alarms.size > 1

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AlarmOverlayScrim)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AlarmHeader(primaryAlarm = primaryAlarm, alarmCount = alarms.size)
            Spacer(modifier = Modifier.height(28.dp))
            AlarmCardsStack(
                alarms = alarms,
                primaryAlarm = primaryAlarm,
                secondaryAlarms = secondaryAlarms,
                isStacked = isStacked,
                onDismissTop = onDismissTop
            )
        }
    }
}

@Composable
private fun AlarmHeader(primaryAlarm: AlarmItem, alarmCount: Int) {
    val transition = rememberInfiniteTransition(label = "alarmHeader")
    val bellRotation = transition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alarmBellRotation"
    )
    val pulseScale = transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ),
        label = "alarmPulseScale"
    )
    val pulseAlpha = transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ),
        label = "alarmPulseAlpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp)
    ) {
        Box(
            modifier = Modifier.size(72.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = pulseScale.value
                        scaleY = pulseScale.value
                        alpha = pulseAlpha.value
                    }
                    .border(2.dp, AlarmCritical, CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(AlarmVibeContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = null,
                    tint = AlarmCritical,
                    modifier = Modifier
                        .size(32.dp)
                        .graphicsLayer { rotationZ = bellRotation.value }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (alarmCount > 1) {
                "有 $alarmCount 个待办已到期"
            } else {
                primaryAlarm.title
            },
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (alarmCount > 1) {
                "请务必尽快处理"
            } else {
                "现已到时间 (${primaryAlarm.timeText.ifBlank { "现在" }})"
            },
            color = AlarmHeaderMeta,
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AlarmCardsStack(
    alarms: List<AlarmItem>,
    primaryAlarm: AlarmItem,
    secondaryAlarms: List<AlarmItem>,
    isStacked: Boolean,
    onDismissTop: (AlarmItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { alarmStackCount = alarms.size },
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AlarmPrimaryCard(
            item = primaryAlarm,
            alarmCount = alarms.size,
            showCountBadge = isStacked,
            onDismiss = { onDismissTop(primaryAlarm) }
        )

        secondaryAlarms.forEachIndexed { index, item ->
            AlarmSecondaryCard(
                item = item,
                scale = if (index == 0) 0.96f else 0.92f,
                translationYOffset = if (index == 0) -8.dp else -16.dp,
                alpha = if (index == 0) 0.7f else 0.3f
            )
        }
    }
}

@Composable
private fun AlarmPrimaryCard(
    item: AlarmItem,
    alarmCount: Int,
    showCountBadge: Boolean,
    onDismiss: () -> Unit
) {
    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(AlarmGlassCard)
                .border(BorderStroke(0.5.dp, AlarmGlassBorder), RoundedCornerShape(24.dp))
                .drawBehind {
                    drawRoundRect(
                        color = AlarmCritical,
                        size = androidx.compose.ui.geometry.Size(4.dp.toPx(), size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx(), 8.dp.toPx())
                    )
                }
                .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 24.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = item.timeText.ifBlank { "请尽快处理" },
                        color = AlarmCardMeta,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AlarmButtonContainer,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "知道了",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (showCountBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 10.dp)
                    .size(24.dp)
                    .background(AlarmCountBadge, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = alarmCount.toString(),
                    color = AlarmCountText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AlarmSecondaryCard(
    item: AlarmItem,
    scale: Float,
    translationYOffset: androidx.compose.ui.unit.Dp,
    alpha: Float
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.translationY = translationYOffset.toPx()
                this.alpha = alpha
            }
            .clip(RoundedCornerShape(24.dp))
            .background(AlarmGlassCard)
            .border(BorderStroke(0.5.dp, AlarmGlassBorder), RoundedCornerShape(24.dp))
            .drawBehind {
                drawRoundRect(
                    color = AlarmCritical,
                    size = androidx.compose.ui.geometry.Size(4.dp.toPx(), size.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx(), 8.dp.toPx())
                )
            }
            .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 20.dp)
    ) {
        Text(
            text = item.title,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 24.sp
        )
        if (item.timeText.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = item.timeText,
                color = AlarmCardMeta,
                fontSize = 13.sp
            )
        }
    }
}
