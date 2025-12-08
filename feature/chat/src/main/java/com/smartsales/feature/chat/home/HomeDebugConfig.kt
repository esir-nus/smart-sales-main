package com.smartsales.feature.chat.home

import androidx.annotation.VisibleForTesting
import com.smartsales.feature.chat.BuildConfig

/**
 * Home 调试 HUD 的统一开关，避免各处直接依赖 BuildConfig.DEBUG。
 */
@VisibleForTesting
var chatDebugHudOverride: Boolean? = null

val CHAT_DEBUG_HUD_ENABLED: Boolean
    get() = chatDebugHudOverride ?: BuildConfig.DEBUG
