package com.smartsales.prism.data.audio

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import com.smartsales.prism.domain.asr.AsrResult
import com.smartsales.prism.domain.asr.AsrService
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

enum class DeviceSpeechMode {
    DEVICE_ONLY,
    DEVICE_WITH_LOCAL_ASR_FALLBACK
}

enum class DeviceSpeechBackend {
    PLATFORM_DEVICE,
    LOCAL_ASR_FALLBACK
}

/**
 * 设备端短语音识别结果。
 */
sealed interface DeviceSpeechRecognitionResult {
    data class Success(
        val text: String,
        val backend: DeviceSpeechBackend = DeviceSpeechBackend.PLATFORM_DEVICE
    ) : DeviceSpeechRecognitionResult

    data class Failure(
        val reason: DeviceSpeechFailureReason,
        val message: String,
        val backend: DeviceSpeechBackend? = null
    ) : DeviceSpeechRecognitionResult
}

/**
 * 设备端短语音识别失败原因。
 */
enum class DeviceSpeechFailureReason {
    UNAVAILABLE,
    NO_MATCH,
    ERROR,
    CANCELLED
}

/**
 * 设备端短语音识别接口。
 */
interface DeviceSpeechRecognizer {
    fun startListening(mode: DeviceSpeechMode = DeviceSpeechMode.DEVICE_ONLY)
    suspend fun finishListening(): DeviceSpeechRecognitionResult
    fun cancelListening()
    fun isListening(): Boolean
}

/**
 * Android SpeechRecognizer 实现。
 */
@Singleton
class RealDeviceSpeechRecognizer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val asrService: AsrService
) : DeviceSpeechRecognizer {

    companion object {
        private const val TAG = "DeviceSpeechRecognizer"
        private const val PLATFORM_RESULT_TIMEOUT_MILLIS = 1_200L
        private const val LOCAL_ASR_UNAVAILABLE_MESSAGE = "当前语音识别暂不可用，请稍后重试"
        private const val LOCAL_ASR_EMPTY_MESSAGE = "没有识别到清晰语音"
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var isActivelyListening = false

    private var speechRecognizer: SpeechRecognizer? = null
    private var session: RecognitionSession? = null

    override fun startListening(mode: DeviceSpeechMode) {
        clearFinishedSession()
        val nextSession = RecognitionSession(
            mode = mode,
            strategy = resolveInitialStrategy(mode),
            deferred = CompletableDeferred()
        )
        session = nextSession

        if (nextSession.strategy == DeviceSpeechStrategy.LOCAL_ASR_FALLBACK) {
            startLocalFallbackCapture(nextSession)
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            if (mode == DeviceSpeechMode.DEVICE_WITH_LOCAL_ASR_FALLBACK) {
                switchSessionToLocalFallback(nextSession)
            } else {
                completeSession(
                    deferred = nextSession.deferred,
                    result = DeviceSpeechRecognitionResult.Failure(
                        reason = DeviceSpeechFailureReason.UNAVAILABLE,
                        message = "当前设备不支持语音识别",
                        backend = DeviceSpeechBackend.PLATFORM_DEVICE
                    )
                )
            }
            return
        }

        val recognizer = ensureRecognizer()
        mainHandler.post {
            recognizer.setRecognitionListener(createListener(nextSession.deferred))
            runCatching {
                recognizer.startListening(buildIntent())
            }.onSuccess {
                isActivelyListening = true
            }.onFailure {
                if (!switchSessionToLocalFallback(nextSession)) {
                    completeSession(
                        deferred = nextSession.deferred,
                        result = DeviceSpeechRecognitionResult.Failure(
                            reason = DeviceSpeechFailureReason.ERROR,
                            message = "当前无法识别语音，请重试",
                            backend = DeviceSpeechBackend.PLATFORM_DEVICE
                        )
                    )
                }
            }
        }
    }

    override suspend fun finishListening(): DeviceSpeechRecognitionResult {
        val activeSession = session ?: return DeviceSpeechRecognitionResult.Failure(
            reason = DeviceSpeechFailureReason.CANCELLED,
            message = "当前没有活动的语音识别"
        )
        return try {
            when (activeSession.strategy) {
                DeviceSpeechStrategy.PLATFORM_DEVICE -> finishPlatformSession(activeSession)
                DeviceSpeechStrategy.LOCAL_ASR_FALLBACK -> finishLocalFallbackSession(activeSession)
            }
        } finally {
            if (session === activeSession) {
                session = null
            }
        }
    }

    override fun cancelListening() {
        val activeSession = session ?: return
        isActivelyListening = false
        activeSession.recorder?.cancel()
        mainHandler.post {
            speechRecognizer?.cancel()
        }
        completeSession(
            deferred = activeSession.deferred,
            result = DeviceSpeechRecognitionResult.Failure(
                reason = DeviceSpeechFailureReason.CANCELLED,
                message = "语音识别已取消",
                backend = activeSession.strategy.backend
            )
        )
        if (session === activeSession) {
            session = null
        }
    }

    override fun isListening(): Boolean {
        return isActivelyListening || session?.recorder?.isRecording() == true
    }

    private fun ensureRecognizer(): SpeechRecognizer {
        return speechRecognizer ?: SpeechRecognizer.createSpeechRecognizer(context).also {
            speechRecognizer = it
        }
    }

    @Suppress("MissingPermission")
    private fun buildIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
    }

    private fun createListener(
        deferred: CompletableDeferred<DeviceSpeechRecognitionResult>
    ): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit

            override fun onRmsChanged(rmsdB: Float) = Unit

            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() {
                isActivelyListening = false
            }

            override fun onError(error: Int) {
                completeSession(
                    deferred = deferred,
                    result = DeviceSpeechRecognitionResult.Failure(
                        reason = mapFailureReason(error),
                        message = mapFailureMessage(error),
                        backend = DeviceSpeechBackend.PLATFORM_DEVICE
                    )
                )
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                val transcript = matches.firstOrNull().orEmpty().trim()
                val result = if (transcript.isBlank()) {
                    DeviceSpeechRecognitionResult.Failure(
                        reason = DeviceSpeechFailureReason.NO_MATCH,
                        message = "没有识别到清晰语音",
                        backend = DeviceSpeechBackend.PLATFORM_DEVICE
                    )
                } else {
                    DeviceSpeechRecognitionResult.Success(
                        text = transcript,
                        backend = DeviceSpeechBackend.PLATFORM_DEVICE
                    )
                }
                completeSession(deferred = deferred, result = result)
            }

            override fun onPartialResults(partialResults: Bundle?) = Unit

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        }
    }

    private fun mapFailureReason(error: Int): DeviceSpeechFailureReason {
        return when (error) {
            SpeechRecognizer.ERROR_NO_MATCH,
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> DeviceSpeechFailureReason.NO_MATCH
            SpeechRecognizer.ERROR_CLIENT -> DeviceSpeechFailureReason.CANCELLED
            else -> DeviceSpeechFailureReason.ERROR
        }
    }

    private fun mapFailureMessage(error: Int): String {
        return when (error) {
            SpeechRecognizer.ERROR_NO_MATCH -> "没有识别到清晰语音"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "语音识别超时"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "语音识别服务正忙，请重试"
            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "设备语音识别网络异常"
            SpeechRecognizer.ERROR_CLIENT -> "语音识别已取消"
            else -> "当前无法识别语音，请重试"
        }
    }

    private fun completeSession(
        deferred: CompletableDeferred<DeviceSpeechRecognitionResult>,
        result: DeviceSpeechRecognitionResult
    ) {
        if (deferred.isCompleted) return
        isActivelyListening = false
        deferred.complete(result)
        if (session?.deferred === deferred) {
            session = null
        }
    }

    private fun clearFinishedSession() {
        val activeSession = session ?: return
        if (activeSession.deferred.isCompleted) {
            session = null
        }
    }

    private fun resolveInitialStrategy(
        mode: DeviceSpeechMode
    ): DeviceSpeechStrategy {
        return if (
            mode == DeviceSpeechMode.DEVICE_WITH_LOCAL_ASR_FALLBACK &&
            shouldPreferLocalAsrFallback(Build.MANUFACTURER, Build.BRAND)
        ) {
            DeviceSpeechStrategy.LOCAL_ASR_FALLBACK
        } else {
            DeviceSpeechStrategy.PLATFORM_DEVICE
        }
    }

    private fun switchSessionToLocalFallback(
        targetSession: RecognitionSession
    ): Boolean {
        if (targetSession.mode != DeviceSpeechMode.DEVICE_WITH_LOCAL_ASR_FALLBACK) {
            return false
        }
        targetSession.strategy = DeviceSpeechStrategy.LOCAL_ASR_FALLBACK
        isActivelyListening = false
        return startLocalFallbackCapture(targetSession)
    }

    private fun startLocalFallbackCapture(
        targetSession: RecognitionSession
    ): Boolean {
        return runCatching {
            val recorder = PhoneAudioRecorder(context)
            val file = recorder.startRecording()
            targetSession.recorder = recorder
            targetSession.recordingFile = file
            android.util.Log.i(
                TAG,
                "Starting local ASR fallback capture: manufacturer=${Build.MANUFACTURER}, brand=${Build.BRAND}"
            )
            true
        }.getOrElse { error ->
            android.util.Log.e(TAG, "Unable to start local ASR fallback capture", error)
            completeSession(
                deferred = targetSession.deferred,
                result = DeviceSpeechRecognitionResult.Failure(
                    reason = DeviceSpeechFailureReason.ERROR,
                    message = LOCAL_ASR_UNAVAILABLE_MESSAGE,
                    backend = DeviceSpeechBackend.LOCAL_ASR_FALLBACK
                )
            )
            false
        }
    }

    private suspend fun finishPlatformSession(
        activeSession: RecognitionSession
    ): DeviceSpeechRecognitionResult {
        mainHandler.post {
            if (isActivelyListening) {
                speechRecognizer?.stopListening()
            }
        }
        return withTimeoutOrNull(PLATFORM_RESULT_TIMEOUT_MILLIS) {
            activeSession.deferred.await()
        } ?: DeviceSpeechRecognitionResult.Failure(
            reason = DeviceSpeechFailureReason.NO_MATCH,
            message = "语音识别超时",
            backend = DeviceSpeechBackend.PLATFORM_DEVICE
        ).also {
            mainHandler.post { speechRecognizer?.cancel() }
        }
    }

    private suspend fun finishLocalFallbackSession(
        activeSession: RecognitionSession
    ): DeviceSpeechRecognitionResult {
        val recorder = activeSession.recorder ?: return DeviceSpeechRecognitionResult.Failure(
            reason = DeviceSpeechFailureReason.ERROR,
            message = LOCAL_ASR_UNAVAILABLE_MESSAGE,
            backend = DeviceSpeechBackend.LOCAL_ASR_FALLBACK
        )
        val recordedFile = runCatching {
            if (recorder.isRecording()) {
                recorder.stopRecording()
            } else {
                activeSession.recordingFile
            }
        }.getOrElse { error ->
            android.util.Log.e(TAG, "Stopping local ASR fallback capture failed", error)
            null
        } ?: return DeviceSpeechRecognitionResult.Failure(
            reason = DeviceSpeechFailureReason.ERROR,
            message = LOCAL_ASR_UNAVAILABLE_MESSAGE,
            backend = DeviceSpeechBackend.LOCAL_ASR_FALLBACK
        )

        return try {
            transcribeLocalFallback(recordedFile)
        } finally {
            recordedFile.delete()
        }
    }

    private suspend fun transcribeLocalFallback(
        file: File
    ): DeviceSpeechRecognitionResult {
        if (!file.exists() || file.length() <= 44L) {
            return DeviceSpeechRecognitionResult.Failure(
                reason = DeviceSpeechFailureReason.NO_MATCH,
                message = LOCAL_ASR_EMPTY_MESSAGE,
                backend = DeviceSpeechBackend.LOCAL_ASR_FALLBACK
            )
        }
        if (!asrService.isAvailable()) {
            android.util.Log.w(TAG, "Local ASR fallback unavailable: AsrService not ready")
            return DeviceSpeechRecognitionResult.Failure(
                reason = DeviceSpeechFailureReason.ERROR,
                message = LOCAL_ASR_UNAVAILABLE_MESSAGE,
                backend = DeviceSpeechBackend.LOCAL_ASR_FALLBACK
            )
        }
        return when (val result = asrService.transcribe(file)) {
            is AsrResult.Success -> {
                val transcript = result.text.trim()
                if (transcript.isBlank()) {
                    DeviceSpeechRecognitionResult.Failure(
                        reason = DeviceSpeechFailureReason.NO_MATCH,
                        message = LOCAL_ASR_EMPTY_MESSAGE,
                        backend = DeviceSpeechBackend.LOCAL_ASR_FALLBACK
                    )
                } else {
                    DeviceSpeechRecognitionResult.Success(
                        text = transcript,
                        backend = DeviceSpeechBackend.LOCAL_ASR_FALLBACK
                    )
                }
            }

            is AsrResult.Error -> {
                android.util.Log.w(
                    TAG,
                    "Local ASR fallback transcribe failed: code=${result.code}, message=${result.message}"
                )
                DeviceSpeechRecognitionResult.Failure(
                    reason = DeviceSpeechFailureReason.ERROR,
                    message = LOCAL_ASR_UNAVAILABLE_MESSAGE,
                    backend = DeviceSpeechBackend.LOCAL_ASR_FALLBACK
                )
            }
        }
    }

    private enum class DeviceSpeechStrategy(val backend: DeviceSpeechBackend) {
        PLATFORM_DEVICE(DeviceSpeechBackend.PLATFORM_DEVICE),
        LOCAL_ASR_FALLBACK(DeviceSpeechBackend.LOCAL_ASR_FALLBACK)
    }

    private class RecognitionSession(
        val mode: DeviceSpeechMode,
        var strategy: DeviceSpeechStrategy,
        val deferred: CompletableDeferred<DeviceSpeechRecognitionResult>,
        var recorder: PhoneAudioRecorder? = null,
        var recordingFile: File? = null
    )
}

internal fun shouldPreferLocalAsrFallback(
    manufacturer: String,
    brand: String
): Boolean {
    val normalizedManufacturer = manufacturer.trim().lowercase()
    val normalizedBrand = brand.trim().lowercase()
    val combined = "$normalizedManufacturer $normalizedBrand"
    val oemKeywords = listOf(
        "xiaomi",
        "redmi",
        "poco",
        "huawei",
        "honor",
        "oppo",
        "oneplus",
        "vivo",
        "realme",
        "meizu"
    )
    return oemKeywords.any { keyword -> combined.contains(keyword) }
}
