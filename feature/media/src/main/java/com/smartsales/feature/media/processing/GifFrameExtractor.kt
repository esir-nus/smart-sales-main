package com.smartsales.feature.media.processing

// 文件：feature/media/src/main/java/com/smartsales/feature/media/processing/GifFrameExtractor.kt
// 模块：:feature:media
// 说明：从GIF中提取帧，调整尺寸并保存为JPG，用于徽章上传
// 规范：docs/specs/esp32-protocol.md
// 作者：创建于 2026-01-09

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Movie
import android.net.Uri
import com.smartsales.core.util.DispatcherProvider
import com.smartsales.core.util.Result
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Extracts frames from a GIF and saves them as JPEG files.
 * 
 * Used for uploading GIF animations to ESP32 badge.
 * Each frame is resized to target dimensions and saved as numbered JPGs.
 * 
 * @see docs/specs/esp32-protocol.md
 */
interface GifFrameExtractor {
    /**
     * Extract all frames from a GIF.
     * 
     * @param source URI to GIF file (content:// or file://)
     * @param outputDir Directory to save extracted frames
     * @param targetWidth Target width for resize (default 240)
     * @param targetHeight Target height for resize (default 280)
     * @param onProgress Callback for progress updates (current, total)
     * @return List of extracted frame files ordered by frame number
     */
    suspend fun extractFrames(
        source: Uri,
        outputDir: File,
        targetWidth: Int = DEFAULT_WIDTH,
        targetHeight: Int = DEFAULT_HEIGHT,
        onProgress: suspend (current: Int, total: Int) -> Unit = { _, _ -> }
    ): Result<List<File>>

    companion object {
        const val DEFAULT_WIDTH = 240
        const val DEFAULT_HEIGHT = 280
        const val JPEG_QUALITY = 85
    }
}

/**
 * Exception types for GIF extraction.
 */
sealed class GifExtractionException(message: String) : Exception(message) {
    class InvalidFormatException(message: String) : GifExtractionException(message)
    class DecodeException(message: String) : GifExtractionException(message)
    class IoException(message: String) : GifExtractionException(message)
}

@Singleton
class DefaultGifFrameExtractor @Inject constructor(
    private val contentResolver: ContentResolver,
    private val dispatchers: DispatcherProvider
) : GifFrameExtractor {

    override suspend fun extractFrames(
        source: Uri,
        outputDir: File,
        targetWidth: Int,
        targetHeight: Int,
        onProgress: suspend (current: Int, total: Int) -> Unit
    ): Result<List<File>> = withContext(dispatchers.io) {
        runCatching {
            // Ensure output directory exists
            if (!outputDir.exists() && !outputDir.mkdirs()) {
                throw GifExtractionException.IoException("无法创建输出目录: ${outputDir.absolutePath}")
            }

            // Read GIF data
            val gifData = contentResolver.openInputStream(source)?.use { it.readBytes() }
                ?: throw GifExtractionException.InvalidFormatException("无法打开GIF文件")

            // Decode using Movie class (works on all API levels)
            val movie = Movie.decodeByteArray(gifData, 0, gifData.size)
                ?: throw GifExtractionException.DecodeException("无法解析GIF")

            val duration = movie.duration()
            if (duration <= 0) {
                // Static image or unsupported format
                throw GifExtractionException.InvalidFormatException("不是有效的动画GIF")
            }

            // Estimate frame count (assume ~100ms per frame typical)
            val frameInterval = 100 // ms
            val estimatedFrameCount = maxOf(1, duration / frameInterval)
            
            val extractedFrames = mutableListOf<File>()
            var frameIndex = 1

            // Extract frames at regular intervals
            var time = 0
            while (time < duration) {
                movie.setTime(time)
                
                // Create bitmap for this frame
                val bitmap = Bitmap.createBitmap(
                    movie.width(),
                    movie.height(),
                    Bitmap.Config.ARGB_8888
                )
                val canvas = Canvas(bitmap)
                movie.draw(canvas, 0f, 0f)

                // Resize to target dimensions (center crop)
                val resized = resizeCenterCrop(bitmap, targetWidth, targetHeight)
                bitmap.recycle()

                // Save as JPEG
                val outFile = File(outputDir, "$frameIndex.jpg")
                FileOutputStream(outFile).use { fos ->
                    resized.compress(Bitmap.CompressFormat.JPEG, GifFrameExtractor.JPEG_QUALITY, fos)
                }
                resized.recycle()

                extractedFrames.add(outFile)
                onProgress(frameIndex, estimatedFrameCount)

                frameIndex++
                time += frameInterval
            }

            if (extractedFrames.isEmpty()) {
                throw GifExtractionException.DecodeException("未能提取任何帧")
            }

            extractedFrames.toList()
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { e ->
                when (e) {
                    is GifExtractionException -> Result.Error(e)
                    else -> Result.Error(GifExtractionException.IoException(e.message ?: "处理失败"))
                }
            }
        )
    }

    /**
     * Resize bitmap using center crop to fill target dimensions.
     */
    private fun resizeCenterCrop(source: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val sourceWidth = source.width
        val sourceHeight = source.height

        val scale = maxOf(
            targetWidth.toFloat() / sourceWidth,
            targetHeight.toFloat() / sourceHeight
        )

        val scaledWidth = (sourceWidth * scale).toInt()
        val scaledHeight = (sourceHeight * scale).toInt()

        val scaled = Bitmap.createScaledBitmap(source, scaledWidth, scaledHeight, true)

        // Crop to center
        val xOffset = (scaledWidth - targetWidth) / 2
        val yOffset = (scaledHeight - targetHeight) / 2

        val cropped = Bitmap.createBitmap(scaled, xOffset, yOffset, targetWidth, targetHeight)
        if (scaled !== cropped) {
            scaled.recycle()
        }

        return cropped
    }
}
