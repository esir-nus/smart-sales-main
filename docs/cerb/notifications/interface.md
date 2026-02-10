# Notification Service — Interface

> **Consumers**: Scheduler, Coach, Badge Pipeline, Memory Center
> **OS Layer**: Infrastructure (Layer 1)
> **Rule**: All Android `NotificationManager` access goes through this interface. Never build notifications directly.

---

## Interface

```kotlin
interface NotificationService {
    /**
     * 显示任务提醒通知
     * @param taskId 用于去重和取消
     * @param title 通知标题
     * @param body 通知正文
     * @param channel 通知渠道
     * @param priority 优先级
     */
    fun show(
        id: String,
        title: String,
        body: String,
        channel: NotificationChannel = NotificationChannel.TASK_REMINDER,
        priority: NotificationPriority = NotificationPriority.HIGH
    )

    /**
     * 取消指定通知
     */
    fun cancel(id: String)

    /**
     * 检查通知权限是否已授予
     */
    fun hasPermission(): Boolean
}
```

### Channels

```kotlin
enum class NotificationChannel(val id: String, val displayName: String) {
    TASK_REMINDER("prism_task_reminders", "任务提醒"),
    COACH_NUDGE("prism_coach_nudge", "教练提示"),
    BADGE_STATUS("prism_badge_status", "设备状态"),
    MEMORY_UPDATE("prism_memory_update", "记忆更新")
}
```

### Priority

```kotlin
enum class NotificationPriority {
    LOW,      // 静默
    DEFAULT,  // 正常
    HIGH      // 横幅 + 振动
}
```

---

## You Should NOT

| ❌ Don't | ✅ Do Instead |
|----------|--------------|
| Call `NotificationManagerCompat` directly | Use `NotificationService.show()` |
| Create `NotificationChannel` in feature code | Declare in `NotificationChannel` enum |
| Check permission in feature code | Call `NotificationService.hasPermission()` |
| Build `NotificationCompat.Builder` in features | Let `RealNotificationService` handle construction |

---

## Data Flow

```
Feature (Scheduler/Coach/Badge)
    → NotificationService.show(id, title, body, channel)
    → RealNotificationService
    → Android NotificationManager
    → System notification tray
```
