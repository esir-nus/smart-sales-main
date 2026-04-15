// File: app-core/src/main/java/com/smartsales/prism/data/audio/SimBadgeSyncIslandEvent.kt
// Module: :app-core
// Summary: 动态岛同步事件 — rec# 下载、手动同步开始/完成/已最新
// Author: created on 2026-04-13
package com.smartsales.prism.data.audio

sealed class SimBadgeSyncIslandEvent {
    data class RecFileDownloaded(val filename: String) : SimBadgeSyncIslandEvent()
    data object ManualSyncStarted : SimBadgeSyncIslandEvent()
    data class ManualSyncComplete(val count: Int) : SimBadgeSyncIslandEvent()
    data object AlreadyUpToDate : SimBadgeSyncIslandEvent()
}
