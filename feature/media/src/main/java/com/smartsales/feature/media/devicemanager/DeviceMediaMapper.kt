package com.smartsales.feature.media.devicemanager

// 文件：feature/media/src/main/java/com/smartsales/feature/media/devicemanager/DeviceMediaMapper.kt
// 模块：:feature:media
// 说明：设备媒体文件映射和格式化工具（从 DeviceManagerViewModel 提取）
// 作者：提取于 2026-01-06

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 将设备媒体文件映射为 UI 模型
 * 接受所有固件返回的文件类型，不做额外过滤
 */
fun DeviceMediaFile.toUiOrNull(): DeviceFileUi? {
    val tab = when {
        isAudio() -> DeviceMediaTab.Audio
        isVideo() -> DeviceMediaTab.Videos
        isImage() -> DeviceMediaTab.Images
        else -> DeviceMediaTab.Audio // 未知类型默认归为音频
    }
    return DeviceFileUi(
        id = name,
        displayName = name,
        sizeText = formatSize(sizeBytes),
        mimeType = mimeType,
        mediaType = tab,
        mediaLabel = when (tab) {
            DeviceMediaTab.Videos -> "视频"
            DeviceMediaTab.Images -> "图片"
            DeviceMediaTab.Audio -> "音频"
        },
        modifiedAtText = formatTimestamp(modifiedAtMillis),
        mediaUrl = mediaUrl,
        downloadUrl = downloadUrl,
        durationText = durationMillis?.let { formatDuration(it) },
        thumbnailUrl = mediaUrl // 静态缩略图占位，禁止自动播放
    )
}

/**
 * 判断是否为视频文件
 */
fun DeviceMediaFile.isVideo(): Boolean {
    val lowerName = name.lowercase(Locale.ROOT)
    if (isAudio()) return false
    if (mimeType.startsWith("video", ignoreCase = true)) return true
    return lowerName.endsWith(".mp4") || lowerName.endsWith(".mov") || lowerName.endsWith(".mkv")
}


/**
 * 判断是否为图片文件
 */
fun DeviceMediaFile.isImage(): Boolean {
    val lowerMime = mimeType.lowercase(Locale.ROOT)
    if (lowerMime.startsWith("image")) return true
    val lowerName = name.lowercase(Locale.ROOT)
    return lowerName.endsWith(".png") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".webp") || lowerName.endsWith(".gif")
}

/**
 * 判断是否为音频文件
 */
fun DeviceMediaFile.isAudio(): Boolean {
    val lowerMime = mimeType.lowercase(Locale.ROOT)
    if (lowerMime.startsWith("audio")) return true
    val lowerName = name.lowercase(Locale.ROOT)
    return lowerName.endsWith(".mp3") || lowerName.endsWith(".wav") || lowerName.endsWith(".m4a") || lowerName.endsWith(".aac") || lowerName.endsWith(".flac") || lowerName.endsWith(".ogg")
}

/**
 * 格式化时长（毫秒 -> HH:MM:SS 或 MM:SS）
 */
fun formatDuration(durationMillis: Long): String {
    if (durationMillis <= 0) return ""
    val totalSeconds = (durationMillis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

/**
 * 格式化文件大小（字节 -> KB/MB/GB）
 */
fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0B"
    val units = arrayOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var index = 0
    while (value >= 1024 && index < units.lastIndex) {
        value /= 1024
        index++
    }
    return String.format(Locale.CHINA, "%.1f%s", value, units[index])
}

/**
 * 格式化时间戳（毫秒 -> yyyy-MM-dd HH:mm）
 */
fun formatTimestamp(timestamp: Long): String {
    if (timestamp <= 0) return "未知时间"
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
    return formatter.format(Date(timestamp))
}
