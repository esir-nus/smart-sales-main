package com.smartsales.prism.data.audio

sealed class SimBadgeSyncIslandEvent {
    data class RecFileDownloaded(val filename: String) : SimBadgeSyncIslandEvent()
    object ManualSyncStarted : SimBadgeSyncIslandEvent()
    data class ManualSyncComplete(val count: Int) : SimBadgeSyncIslandEvent()
    object AlreadyUpToDate : SimBadgeSyncIslandEvent()
}
