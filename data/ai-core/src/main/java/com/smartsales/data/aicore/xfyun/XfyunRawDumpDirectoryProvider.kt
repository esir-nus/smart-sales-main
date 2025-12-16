// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/xfyun/XfyunRawDumpDirectoryProvider.kt
// 模块：:data:ai-core
// 说明：提供讯飞 raw 响应落盘目录（app 私有持久目录）
// 作者：创建于 2025-12-16
package com.smartsales.data.aicore.xfyun

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 说明：为避免数据层直接依赖 UI/Activity，这里用 Provider 提供 app-private 目录。
 * 重要：单元测试可提供 Fake Provider 指向临时目录。
 */
interface XfyunRawDumpDirectoryProvider {
    fun directory(): File
}

@Singleton
class DefaultXfyunRawDumpDirectoryProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : XfyunRawDumpDirectoryProvider {
    override fun directory(): File = File(context.filesDir, "xfyun_raw")
}

