package com.smartsales.feature.chat.home

import com.smartsales.feature.chat.BuildConfig

/**
 * Home 调试 HUD 的统一开关，避免各处直接依赖 BuildConfig.DEBUG。
 */
val CHAT_DEBUG_HUD_ENABLED: Boolean = BuildConfig.DEBUG
