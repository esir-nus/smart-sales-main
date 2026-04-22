// File: app-core/src/main/java/com/smartsales/prism/ui/util/ClipboardHelper.kt
// Module: :app-core
// Summary: 系统剪贴板封装，供聊天气泡长按复制调用
// Author: created on 2026-04-20

package com.smartsales.prism.ui.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

object ClipboardHelper {
    private const val CLIP_LABEL = "smartsales_chat"

    fun copy(context: Context, text: String) {
        if (text.isEmpty()) return
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return
        manager.setPrimaryClip(ClipData.newPlainText(CLIP_LABEL, text))
    }
}
