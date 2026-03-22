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
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartsales.prism.data.notification.AlarmDismissReceiver
import com.smartsales.prism.domain.notification.NotificationPriority
import com.smartsales.prism.domain.notification.NotificationService
import com.smartsales.prism.domain.notification.PrismNotificationChannel
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.delay

/**
 * 闹钟全屏 Activity
 *
 * 半透明覆盖层，显示任务名称和时间。
 * 支持多个闹钟同时显示 (堆叠)。
 * 5 种关闭方式：知道了按钮、上滑、音量键、通知滑动、通知按钮。
 * 1分钟无操作自动关闭，留"未接提醒"静默通知。
 * launchMode="singleTop" — 新闹钟通过 onNewIntent() 加入栈。
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

    // 闹钟栈 — 支持多个闹钟同时显示
    private val alarmStack = mutableStateListOf<AlarmItem>()

    // 防止重复 dismiss (用于全局关闭操作)
    private var isDismissing = false

    // 监听来自 AlarmDismissReceiver 的关闭广播
    private val finishReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_FINISH_ALARM) {
                val targetTaskId = intent.getStringExtra(AlarmDismissReceiver.EXTRA_TASK_ID)
                if (!targetTaskId.isNullOrEmpty()) {
                    // 关闭特定闹钟
                    removeAlarmByTaskId(targetTaskId)
                } else {
                    // 关闭所有 (fallback: 如果 Receiver 没传 ID)
                    Log.d(TAG, "收到通用关闭广播，关闭所有闹钟")
                    finish()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 锁屏上显示 + 唤醒屏幕
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        super.onCreate(savedInstanceState)

        // 注册关闭广播
        val filter = IntentFilter(ACTION_FINISH_ALARM)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(finishReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(finishReceiver, filter)
        }

        // 从 Intent 读取数据并加入栈
        addAlarmFromIntent(intent)

        // 启动持续振动 — 仅当 Activity 成功启动后（Ghost Alarm 修复：振动不在 Receiver 启动）
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            NotificationEntryPoint::class.java
        )
        entryPoint.notificationService().startPersistentVibration()
        Log.d(TAG, "持续振动已启动 (onCreate)")

        setContent {
            AlarmStackScreen(
                alarms = alarmStack,
                onDismiss = { item -> dismissAlarm(item) },
                onAutoDismiss = { autoDismissAll() }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // singleTop: 新闹钟到达时加入栈
        addAlarmFromIntent(intent)
        Log.d(TAG, "onNewIntent: 新闹钟已加入栈，当前数量: ${alarmStack.size}")
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(finishReceiver)
        } catch (_: Exception) { }
    }

    /**
     * 音量键关闭所有闹钟
     */
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
        val tId = intent.getStringExtra(EXTRA_TASK_ID) ?: ""

        // 避免重复添加同一个任务
        if (tId.isNotEmpty() && alarmStack.none { it.taskId == tId }) {
            alarmStack.add(AlarmItem(tId, notifId, title, time))
        }
    }

    private fun removeAlarmByTaskId(id: String) {
        alarmStack.removeAll { it.taskId == id }
        if (alarmStack.isEmpty()) {
            finish()
        }
    }

    private fun dismissAlarm(item: AlarmItem) {
        // 用户点击"知道了" -> 关闭单个闹钟
        sendDismissBroadcast(item)
        alarmStack.remove(item)
        if (alarmStack.isEmpty()) {
            finish()
        }
    }

    private fun dismissAllAlarms() {
        if (isDismissing) return
        isDismissing = true
        
        // Copy list to avoid concurrent modification during iteration
        val copy = alarmStack.toList()
        copy.forEach { sendDismissBroadcast(it) }
        alarmStack.clear()
        finish()
    }

    private fun autoDismissAll() {
        if (isDismissing) return
        isDismissing = true
        Log.d(TAG, "自动关闭: 1分钟无操作")

        val copy = alarmStack.toList()
        copy.forEach { item ->
            // 停止振动 + 取消原始通知
            sendDismissBroadcast(item)
            
            // 留一条静默的"未接提醒"通知
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
                channel = PrismNotificationChannel.TASK_REMINDER_EARLY,  // 静默通知用 EARLY 渠道
                priority = NotificationPriority.LOW
            )
        } catch (e: Exception) {
            Log.w(TAG, "未接提醒通知发送失败: ${e.message}")
        }
    }
}

// 闹钟项数据模型
private data class AlarmItem(
    val taskId: String,
    val notificationId: String,
    val title: String,
    val timeText: String
)

/**
 * 闹钟堆叠 UI
 * 显示所有激活的闹钟卡片
 */
@Composable
private fun AlarmStackScreen(
    alarms: List<AlarmItem>,
    onDismiss: (AlarmItem) -> Unit,
    onAutoDismiss: () -> Unit
) {
    // 1分钟无操作自动关闭 (每次列表变动重置计时)
    LaunchedEffect(alarms.size) {
        delay(AlarmActivity.AUTO_DISMISS_MS)
        onAutoDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xCC000000),
                        Color(0xE6000000),
                        Color(0xCC000000)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 40.dp)
        ) {
            items(alarms, key = { it.taskId }) { item ->
                AlarmCard(item, onDismiss)
            }
        }
        
        if (alarms.size > 1) {
             Text(
                text = "${alarms.size} 个提醒待处理",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            )
        }
    }
}

@Composable
private fun AlarmCard(
    item: AlarmItem,
    onDismiss: (AlarmItem) -> Unit
) {
    // 简单的卡片 UI
     Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2D2D2D), RoundedCornerShape(16.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 图标
        Icon(
            imageVector = Icons.Filled.Notifications,
            contentDescription = null,
            tint = Color(0xFFFFB74D),
            modifier = Modifier.size(48.dp)
        )

        // 任务标题
        Text(
            text = item.title,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2
        )

        // 时间文本
        Text(
            text = item.timeText,
            color = Color(0xB3FFFFFF),
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 知道了按钮
        Button(
            onClick = { onDismiss(item) },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFB74D)
                )
            ) {
                Text(
                    text = "知道了",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
            }
    }
}
