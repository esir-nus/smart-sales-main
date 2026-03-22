package com.smartsales.prism.data.notification

/**
 * 提醒可靠性提示闸门。
 *
 * 作用：
 * - 统一判断当前是否需要做提醒可靠性引导
 * - 包含精确闹钟权限与 OEM 通知/后台策略加固
 * - 只在进程生命周期内放行一次提示，避免同一批创建/改期反复弹窗
 */
interface ExactAlarmPermissionGate {
    fun shouldPromptForExactAlarm(): Boolean
}
