// File: feature/chat/src/main/java/com/smartsales/feature/chat/platform/MediaInputCoordinator.kt
// Module: :feature:chat
// Summary: Platform-specific media input coordination for audio/image file handling
// Author: created on 2026-01-07

package com.smartsales.feature.chat.platform

import android.content.Context
import android.net.Uri
import com.smartsales.core.util.Result
import com.smartsales.domain.transcription.TranscriptionCoordinator
import com.smartsales.feature.chat.home.TranscriptionChatRequest
import com.smartsales.feature.media.audiofiles.AudioStorageRepository
import com.smartsales.feature.media.audiofiles.AudioTranscriptionCoordinator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Media Input Coordinator: handles audio and image file picking, storage, and transcription submission.
 *
 * Responsibilities:
 * - Import audio files from phone storage
 * - Upload audio to transcription service
 * - Submit transcription jobs
 * - Save images to cache
 *
 * Design:
 * - Pure coordinator, no UI state management
 * - Returns results for ViewModel to handle UI updates
 */
@Singleton
class MediaInputCoordinator @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val audioStorageRepository: AudioStorageRepository,
    private val transcriptionCoordinator: AudioTranscriptionCoordinator
) {

    /**
     * Handle audio file pick: import, upload, and submit transcription.
     */
    suspend fun handleAudioPick(uri: Uri, sessionId: String): Result<TranscriptionChatRequest> {
        // Import from phone storage
        val stored = withContext(Dispatchers.IO) {
            runCatching { audioStorageRepository.importFromPhone(uri) }
        }.getOrElse { error ->
            return Result.Error(IOException("Import failed: ${error.message}", error))
        }

        val localPath = stored.localUri.path
        if (localPath.isNullOrBlank()) {
            return Result.Error(IOException("Local audio path not found"))
        }

        // Upload audio
        val uploadPayload = when (val upload = transcriptionCoordinator.uploadAudio(File(localPath))) {
            is Result.Success -> upload.data
            is Result.Error -> return Result.Error(IOException("Upload failed: ${upload.throwable.message}", upload.throwable))
        }

        // Submit transcription job
        return when (val submit = transcriptionCoordinator.submitTranscription(
            audioAssetName = stored.displayName,
            language = "zh-CN",
            uploadPayload = uploadPayload,
            sessionId = sessionId
        )) {
            is Result.Success -> Result.Success(
                TranscriptionChatRequest(
                    jobId = submit.data,
                    fileName = stored.displayName,
                    recordingId = stored.id,
                    sessionId = sessionId
                )
            )
            is Result.Error -> Result.Error(IOException("Submit failed: ${submit.throwable.message}", submit.throwable))
        }
    }

    /**
     * Handle image pick: save to cache and return saved file info.
     */
    suspend fun handleImagePick(uri: Uri): Result<ImageSaveResult> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "image_$timestamp.jpg"
                val destFile = File(appContext.cacheDir, fileName)

                appContext.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }

                ImageSaveResult(
                    file = destFile,
                    name = fileName
                )
            }.fold(
                onSuccess = { Result.Success(it) },
                onFailure = { Result.Error(IOException("Image save failed: ${it.message}", it)) }
            )
        }
    }
}

/**
 * Result of image save operation.
 */
data class ImageSaveResult(
    val file: File,
    val name: String
)
