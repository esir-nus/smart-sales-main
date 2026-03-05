# Notification Service Interface

> **Blackbox contract** — For consumers (Scheduler, Mascot, Badge Pipeline, Memory Center). Don't read implementation.
> **OS Layer**: Infrastructure (Layer 1)
> **Rule**: All Android `NotificationManager` access goes through this interface. Never build notifications directly.

---

## You Can Call

### NotificationService

```kotlin
interface NotificationService {
    /**
     * 显示通知
     * @param id 通知唯一标识 (用于去重和取消)
     * @param title 通知标题
     * @param body 通知正文
     * @param channel 通知渠道
     * @param priority 优先级
     * @param contentIntent 点击通知时的跳转 (可选)
     */
    fun show(
        id: String,
        title: String,
        body: String,
        channel: PrismNotificationChannel = PrismNotificationChannel.TASK_REMINDER_EARLY,
        priority: NotificationPriority = NotificationPriority.HIGH,
        contentIntent: PendingIntent? = null
    )

    /**
     * 取消指定通知
     */
    fun cancel(id: String)

    /**
     * 检查通知权限是否已授予
     */
    fun hasPermission(): Boolean

    /**
     * 启动持续振动 (URGENT 通知专用)
     * 使用独立 Vibrator，不依赖通知自带振动
     */
    fun startPersistentVibration()

    /**
     * 停止持续振动
     */
    fun stopVibration()
}
```

---

## Output Types

### NotificationChannel

```kotlin
enum class PrismNotificationChannel(val channelId: String, val displayName: String) {
    TASK_REMINDER_EARLY("prism_task_reminders_v3_early", "任务提醒（提前预警）"),
    TASK_REMINDER_DEADLINE("prism_task_reminders_v3_deadline", "任务闹钟（到点提醒）"),
    COACH_NUDGE("prism_coach_nudge", "教练提示"),
    BADGE_STATUS("prism_badge_status", "设备状态"),
    MEMORY_UPDATE("prism_memory_update", "记忆更新")
}
```

### NotificationPriority

```kotlin
enum class NotificationPriority {
    LOW,      // 静默
    DEFAULT,  // 正常
    HIGH,     // 横幅 + 振动
    URGENT    // 持续振动 + 锁屏 + 全屏 Intent + 绕过勿扰
}
```

---

## Guarantees

| Operation | Guarantee |
|-----------|-----------|
| `show` | Thread-safe — concurrent calls OK |
| `show` | No-op if `POST_NOTIFICATIONS` permission denied (logs warning silently) |
| `show` | Channels created lazily on first use |
| `show` | All channels use `VISIBILITY_PUBLIC` (visible on lock screen) |
| `cancel` | Idempotent — safe to call with non-existent id |
| `hasPermission` | Always returns `true` on API < 33 |
| `startPersistentVibration` | Uses `VibrationEffect.createWaveform` with loop, independent of notification channel |
| `stopVibration` | Idempotent — safe to call when not vibrating |

---

## You Should NOT

- ❌ Call `NotificationManagerCompat` directly — use `NotificationService.show()`
- ❌ Create `NotificationChannel` in feature code — declare in `NotificationChannel` enum
- ❌ Check permission in feature code — call `NotificationService.hasPermission()`
- ❌ Build `NotificationCompat.Builder` in features — let `RealNotificationService` handle construction

---

## When to Read Full Spec

Read `spec.md` only if:
- You are implementing `RealNotificationService`
- You need to understand channel importance levels or vibration patterns
- You are adding a new notification channel

Otherwise, **trust this interface**.
