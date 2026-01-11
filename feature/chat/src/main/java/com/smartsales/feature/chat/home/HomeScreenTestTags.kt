// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenTestTags.kt
// 模块：:feature:chat
// 说明：HomeScreen 测试标签常量
// 作者：从 HomeScreen.kt 提取于 2026-01-11

package com.smartsales.feature.chat.home

/**
 * HomeScreen 及其子组件的测试标签。
 *
 * 这些常量用于 UI 测试中定位元素。
 */
object HomeScreenTestTags {
    const val ROOT = "home_screen_root"
    // 兼容旧测试
    const val PAGE = ROOT
    const val PAGE_HOME = "page_home"
    const val DEVICE_ENTRY = "home_device_entry"
    const val AUDIO_ENTRY = "home_audio_entry"
    const val SESSION_HEADER = "home_session_header"
    const val LIST = "home_messages_list"
    const val DEVICE_BANNER = DEVICE_ENTRY
    const val AUDIO_CARD = AUDIO_ENTRY
    const val PROFILE_BUTTON = "home_profile_button"
    const val ACTIVE_SKILL_CHIP = "active_skill_chip"
    const val ACTIVE_SKILL_CHIP_CLOSE = "active_skill_chip_close"
    const val SESSION_LIST = "home_session_list"
    const val SESSION_LOADING = "home_session_loading"
    const val SESSION_EMPTY = "home_session_empty"
    const val SESSION_LIST_ITEM_PREFIX = "home_session_item_"
    const val SESSION_CURRENT_PREFIX = "home_session_current_"
    const val NEW_CHAT_BUTTON = "home_new_chat_button"
    const val EXPORT_PDF = "home_export_pdf"
    const val EXPORT_CSV = "home_export_csv"
    const val SESSION_TITLE = "home_session_title"
    const val USER_MESSAGE = "home_user_message"
    const val ASSISTANT_MESSAGE = "home_assistant_message"
    const val ASSISTANT_COPY_PREFIX = "home_assistant_copy_"
    const val INPUT_FIELD = "home_input_field"
    const val SEND_BUTTON = "home_send_button"
    const val SCROLL_TO_LATEST = "home_scroll_to_latest_button"
    const val HISTORY_TOGGLE = "home_history_toggle"
    const val HISTORY_PANEL = "home_history_panel"
    const val HISTORY_EMPTY = "home_history_empty"
    const val HISTORY_ITEM_PREFIX = "home_history_item_"
    const val HISTORY_USER_CENTER = "home_history_user_center"
    const val HERO = "home_hero"
    const val DEBUG_HUD_PANEL = "debug_hud_panel"
    const val DEBUG_HUD_TOGGLE = "debug_toggle_metadata"
    const val DEBUG_HUD_SCRIM = "debug_hud_scrim"
    const val DEBUG_HUD_CLOSE = "debug_hud_close"
    const val DEBUG_HUD_COPY = "debug_hud_copy"
    const val HOME_DEVICE_INDICATOR = "home_device_indicator"
    const val HISTORY_DEVICE_STATUS = "home_history_device_status"
}
