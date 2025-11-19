package com.smartsales.feature.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.smartsales.core.util.Result
import com.smartsales.data.aicore.ExportResult
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File

// 文件：feature/chat/src/main/java/com/smartsales/feature/chat/AndroidChatShareHandler.kt
// 模块：:feature:chat
// 说明：将 Markdown 复制到系统剪贴板并触发 Android 分享面板
// 作者：创建于 2025-11-16
class AndroidChatShareHandler(
    @ApplicationContext private val context: Context
) : ChatShareHandler {

    private val clipboard: ClipboardManager? =
        context.getSystemService(ClipboardManager::class.java)
    private val exportDir: File = File(context.cacheDir, "chat/exports").apply { mkdirs() }
    private val authority = "${context.packageName}.chatfileprovider"

    override suspend fun copyMarkdown(markdown: String): Result<Unit> =
        copyText("SmartSales Markdown", markdown)

    override suspend fun copyAssistantReply(text: String): Result<Unit> =
        copyText("SmartSales Assistant", text)

    override suspend fun shareExport(result: ExportResult): Result<Unit> = try {
        val file = result.localPath
            ?.let { File(it) }
            ?.takeIf { it.exists() }
            ?: File(exportDir, result.fileName).apply { writeBytes(result.payload) }
        val uri = FileProvider.getUriForFile(context, authority, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = result.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "分享 ${result.fileName}").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
        Result.Success(Unit)
    } catch (t: Throwable) {
        Result.Error(t)
    }

    private fun copyText(label: String, text: String): Result<Unit> = try {
        val clip = ClipData.newPlainText(label, text)
        clipboard?.setPrimaryClip(clip)
            ?: throw IllegalStateException("Clipboard manager unavailable")
        Result.Success(Unit)
    } catch (t: Throwable) {
        Result.Error(t)
    }
}
