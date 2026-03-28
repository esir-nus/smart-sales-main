package com.smartsales.prism.data.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred

/**
 * 设备端短语音识别结果。
 */
sealed interface DeviceSpeechRecognitionResult {
    data class Success(val text: String) : DeviceSpeechRecognitionResult
    data class Failure(
        val reason: DeviceSpeechFailureReason,
        val message: String
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
    fun startListening()
    suspend fun finishListening(): DeviceSpeechRecognitionResult
    fun cancelListening()
    fun isListening(): Boolean
}

/**
 * Android SpeechRecognizer 实现。
 */
@Singleton
class RealDeviceSpeechRecognizer @Inject constructor(
    @ApplicationContext private val context: Context
) : DeviceSpeechRecognizer {

    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var isActivelyListening = false

    private var speechRecognizer: SpeechRecognizer? = null
    private var session: RecognitionSession? = null

    override fun startListening() {
        clearFinishedSession()
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            val deferred = CompletableDeferred<DeviceSpeechRecognitionResult>().also {
                it.complete(
                    DeviceSpeechRecognitionResult.Failure(
                        reason = DeviceSpeechFailureReason.UNAVAILABLE,
                        message = "当前设备不支持语音识别"
                    )
                )
            }
            session = RecognitionSession(deferred = deferred)
            isActivelyListening = false
            return
        }

        val deferred = CompletableDeferred<DeviceSpeechRecognitionResult>()
        session = RecognitionSession(deferred = deferred)
        val recognizer = ensureRecognizer()
        mainHandler.post {
            recognizer.setRecognitionListener(createListener(deferred))
            runCatching {
                recognizer.startListening(buildIntent())
            }.onSuccess {
                isActivelyListening = true
            }.onFailure {
                completeSession(
                    deferred = deferred,
                    result = DeviceSpeechRecognitionResult.Failure(
                        reason = DeviceSpeechFailureReason.ERROR,
                        message = "当前无法识别语音，请重试"
                    )
                )
            }
        }
    }

    override suspend fun finishListening(): DeviceSpeechRecognitionResult {
        val activeSession = session ?: return DeviceSpeechRecognitionResult.Failure(
            reason = DeviceSpeechFailureReason.CANCELLED,
            message = "当前没有活动的语音识别"
        )
        mainHandler.post {
            if (isActivelyListening) {
                speechRecognizer?.stopListening()
            }
        }
        return activeSession.deferred.await().also {
            if (session === activeSession) {
                session = null
            }
        }
    }

    override fun cancelListening() {
        val activeSession = session ?: return
        isActivelyListening = false
        mainHandler.post {
            speechRecognizer?.cancel()
        }
        completeSession(
            deferred = activeSession.deferred,
            result = DeviceSpeechRecognitionResult.Failure(
                reason = DeviceSpeechFailureReason.CANCELLED,
                message = "语音识别已取消"
            )
        )
    }

    override fun isListening(): Boolean = isActivelyListening

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
                        message = mapFailureMessage(error)
                    )
                )
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                val transcript = matches.firstOrNull().orEmpty().trim()
                val result = if (transcript.isBlank()) {
                    DeviceSpeechRecognitionResult.Failure(
                        reason = DeviceSpeechFailureReason.NO_MATCH,
                        message = "没有识别到清晰语音"
                    )
                } else {
                    DeviceSpeechRecognitionResult.Success(text = transcript)
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

    private data class RecognitionSession(
        val deferred: CompletableDeferred<DeviceSpeechRecognitionResult>
    )
}
