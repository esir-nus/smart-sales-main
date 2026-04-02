package com.smartsales.prism.data.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.HandlerThread
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import com.alibaba.idst.nui.AsrResult
import com.alibaba.idst.nui.Constants
import com.alibaba.idst.nui.INativeNuiCallback
import com.alibaba.idst.nui.KwsResult
import com.alibaba.idst.nui.NativeNui
import com.smartsales.data.aicore.DashscopeCredentialsProvider
import com.smartsales.data.aicore.DashscopeRealtimeAuthDiagnostic
import com.smartsales.data.aicore.DashscopeRealtimeAuthFailureCategory
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject

internal const val FUN_ASR_REALTIME_SERVICE_URL = "wss://dashscope.aliyuncs.com/api-ws/v1/inference"

enum class SimRealtimeSpeechFailureReason {
    UNAVAILABLE,
    NO_MATCH,
    ERROR,
    CANCELLED
}

sealed interface SimRealtimeSpeechRecognitionResult {
    data class Success(val text: String) : SimRealtimeSpeechRecognitionResult

    data class Failure(
        val reason: SimRealtimeSpeechFailureReason,
        val message: String,
        val diagnostic: DashscopeRealtimeAuthDiagnostic? = null
    ) : SimRealtimeSpeechRecognitionResult
}

sealed interface SimRealtimeSpeechEvent {
    data object ListeningStarted : SimRealtimeSpeechEvent
    data object CaptureLimitReached : SimRealtimeSpeechEvent
    data class PartialTranscript(val text: String) : SimRealtimeSpeechEvent
    data class FinalTranscript(val text: String) : SimRealtimeSpeechEvent
    data class Failure(
        val reason: SimRealtimeSpeechFailureReason,
        val message: String,
        val diagnostic: DashscopeRealtimeAuthDiagnostic? = null
    ) : SimRealtimeSpeechEvent

    data object Cancelled : SimRealtimeSpeechEvent
}

interface SimRealtimeSpeechRecognizer {
    val events: Flow<SimRealtimeSpeechEvent>
        get() = emptyFlow()

    fun startListening()
    suspend fun finishListening(): SimRealtimeSpeechRecognitionResult
    fun cancelListening()
    fun isListening(): Boolean
}

@Singleton
class FunAsrRealtimeSpeechRecognizer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val credentialsProvider: DashscopeCredentialsProvider
) : SimRealtimeSpeechRecognizer, INativeNuiCallback {

    companion object {
        private const val TAG = "FunAsrRealtime"
        private const val SAMPLE_RATE = 16_000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val MAX_CAPTURE_DURATION_MILLIS = 60_000L
        private const val START_STOP_TIMEOUT_MILLIS = 10_000L
        private const val MODEL_NAME = "fun-asr-realtime"
        private const val AUTH_FAILED_CODE = 240070
    }

    private val nui = NativeNui()
    private val workerThread = HandlerThread("sim_funasr_realtime").apply { start() }
    private val workerHandler = Handler(workerThread.looper)
    private val sessionLock = Any()
    private val _events = MutableSharedFlow<SimRealtimeSpeechEvent>(extraBufferCapacity = 16)

    @Volatile
    private var initialized = false

    private var session: RecognitionSession? = null
    private var audioRecord: AudioRecord? = null

    override val events: Flow<SimRealtimeSpeechEvent> = _events.asSharedFlow()

    override fun startListening() {
        Log.d(TAG, "start_requested")
        clearFinishedSession()
        val activeSession = RecognitionSession()
        synchronized(sessionLock) {
            session = activeSession
        }
        workerHandler.post {
            val apiKey = credentialsProvider.obtain().apiKey.trim()
            Log.d(TAG, "auth_prepare directKeyConfigured=${apiKey.isNotBlank()}")
            if (apiKey.isBlank()) {
                failSession(
                    activeSession = activeSession,
                    reason = SimRealtimeSpeechFailureReason.UNAVAILABLE,
                    message = "当前无法获取语音识别凭证，请稍后重试",
                    diagnostic = buildMissingApiKeyAuthDiagnostic()
                )
                return@post
            }
            if (!ensureInitialized(apiKey)) {
                failSession(
                    activeSession = activeSession,
                    reason = SimRealtimeSpeechFailureReason.UNAVAILABLE,
                    message = "当前无法初始化语音识别，请稍后重试"
                )
                return@post
            }
            if (!prepareAudioRecord()) {
                failSession(
                    activeSession = activeSession,
                    reason = SimRealtimeSpeechFailureReason.UNAVAILABLE,
                    message = "无法录音：未授予麦克风权限或录音器不可用"
                )
                return@post
            }

            activeSession.latestTranscript = ""
            if (!startDialog(activeSession)) {
                releaseAudioRecord()
            }
        }
    }

    override suspend fun finishListening(): SimRealtimeSpeechRecognitionResult {
        val activeSession = synchronized(sessionLock) { session }
            ?: return SimRealtimeSpeechRecognitionResult.Failure(
                reason = SimRealtimeSpeechFailureReason.CANCELLED,
                message = "当前没有活动的语音识别"
            )
        Log.d(TAG, "finish_requested")
        requestStop(activeSession, notifyLimitReached = false)
        return withTimeoutOrNull(START_STOP_TIMEOUT_MILLIS) {
            activeSession.deferred.await()
        } ?: SimRealtimeSpeechRecognitionResult.Failure(
            reason = SimRealtimeSpeechFailureReason.ERROR,
            message = "语音识别超时，请重试"
        ).also {
            cancelListening()
        }
    }

    override fun cancelListening() {
        val activeSession = synchronized(sessionLock) { session } ?: return
        Log.d(TAG, "cancel_requested")
        workerHandler.post {
            cancelCaptureLimit(activeSession)
            nui.stopDialog()
            releaseAudioRecord()
        }
        _events.tryEmit(SimRealtimeSpeechEvent.Cancelled)
        completeSession(
            activeSession,
            SimRealtimeSpeechRecognitionResult.Failure(
                reason = SimRealtimeSpeechFailureReason.CANCELLED,
                message = "语音识别已取消"
            )
        )
    }

    override fun isListening(): Boolean {
        return synchronized(sessionLock) {
            session?.deferred?.isCompleted == false
        }
    }

    override fun onNuiEventCallback(
        event: Constants.NuiEvent,
        resultCode: Int,
        arg2: Int,
        kwsResult: KwsResult?,
        asrResult: AsrResult?
    ) {
        val activeSession = synchronized(sessionLock) { session } ?: return
        val currentText = sanitizeFunAsrTranscript(asrResult?.asrResult)
        if (currentText.isNotBlank()) {
            activeSession.latestTranscript = currentText
        }
        when (event) {
            Constants.NuiEvent.EVENT_ASR_PARTIAL_RESULT,
            Constants.NuiEvent.EVENT_SENTENCE_END -> {
                currentText.takeIf { it.isNotBlank() && !activeSession.stopRequested }?.let {
                    _events.tryEmit(SimRealtimeSpeechEvent.PartialTranscript(it))
                }
            }

            Constants.NuiEvent.EVENT_TRANSCRIBER_COMPLETE -> {
                Log.d(TAG, "nui_event complete")
                releaseAudioRecord()
                val finalText = activeSession.latestTranscript.trim()
                finalText.takeIf { it.isNotBlank() }?.let {
                    _events.tryEmit(SimRealtimeSpeechEvent.FinalTranscript(it))
                }
                completeSession(
                    activeSession,
                    if (finalText.isBlank()) {
                        SimRealtimeSpeechRecognitionResult.Failure(
                            reason = SimRealtimeSpeechFailureReason.NO_MATCH,
                            message = "没有识别到清晰语音"
                        )
                    } else {
                        SimRealtimeSpeechRecognitionResult.Success(finalText)
                    }
                )
            }

            Constants.NuiEvent.EVENT_ASR_ERROR -> {
                Log.w(TAG, "nui_event asr_error code=$resultCode")
                releaseAudioRecord()
                val message = sanitizeFunAsrErrorMessage(asrResult?.asrResult)
                    ?: "语音识别失败（$resultCode）"
                failSession(
                    activeSession = activeSession,
                    reason = if (resultCode == AUTH_FAILED_CODE) {
                        SimRealtimeSpeechFailureReason.UNAVAILABLE
                    } else {
                        SimRealtimeSpeechFailureReason.ERROR
                    },
                    message = if (resultCode == AUTH_FAILED_CODE) {
                        "当前无法获取有效的语音识别凭证，请稍后重试"
                    } else {
                        message
                    },
                    diagnostic = if (resultCode == AUTH_FAILED_CODE) {
                        DashscopeRealtimeAuthDiagnostic(
                            category = DashscopeRealtimeAuthFailureCategory.SDK_AUTH_REJECTED,
                            vendorCode = resultCode,
                            safeMessage = "FunASR 实时鉴权被拒绝"
                        )
                    } else {
                        null
                    }
                )
            }

            Constants.NuiEvent.EVENT_MIC_ERROR -> {
                Log.w(TAG, "nui_event mic_error")
                releaseAudioRecord()
                failSession(
                    activeSession = activeSession,
                    reason = SimRealtimeSpeechFailureReason.ERROR,
                    message = "录音输入异常，请检查麦克风权限或设备占用"
                )
            }

            else -> Unit
        }
    }

    override fun onNuiNeedAudioData(buffer: ByteArray, len: Int): Int {
        val recorder = synchronized(sessionLock) { audioRecord } ?: return -1
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            return -1
        }
        val audioSize = recorder.read(buffer, 0, len)
        return if (audioSize > 0) audioSize else -1
    }

    override fun onNuiAudioStateChanged(state: Constants.AudioState) {
        Log.d(TAG, "audio_state_changed state=$state")
        when (state) {
            Constants.AudioState.STATE_OPEN -> {
                synchronized(sessionLock) { audioRecord }?.startRecording()
            }

            Constants.AudioState.STATE_PAUSE -> {
                synchronized(sessionLock) { audioRecord }?.runCatchingStop()
            }

            Constants.AudioState.STATE_CLOSE -> {
                releaseAudioRecord()
            }
        }
    }

    override fun onNuiAudioRMSChanged(valRms: Float) = Unit

    override fun onNuiVprEventCallback(event: Constants.NuiVprEvent) = Unit

    override fun onNuiLogTrackCallback(level: Constants.LogLevel, log: String?) = Unit

    private fun ensureInitialized(apiKey: String): Boolean {
        if (initialized) return true
        val initCode = nui.initialize(
            this,
            buildFunAsrInitParams(
                apiKey = apiKey,
                deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                    ?: "sim_device"
            ),
            Constants.LogLevel.LOG_LEVEL_DEBUG,
            false
        )
        initialized = initCode == Constants.NuiResultCode.SUCCESS
        if (!initialized) {
            Log.w(TAG, "initialize_failed code=$initCode")
        }
        return initialized
    }

    private fun buildRealtimeParams(): String {
        val nlsConfig = JSONObject()
        nlsConfig.put("sr_format", "pcm")
        nlsConfig.put("model", MODEL_NAME)
        nlsConfig.put("sample_rate", SAMPLE_RATE)
        nlsConfig.put("semantic_punctuation_enabled", false)

        val params = JSONObject()
        params.put("nls_config", nlsConfig)
        params.put("service_type", Constants.kServiceTypeSpeechTranscriber)
        return params.toString()
    }

    private fun startDialog(activeSession: RecognitionSession): Boolean {
        nui.setParams(buildRealtimeParams())
        val startCode = nui.startDialog(
            Constants.VadMode.TYPE_P2T,
            buildFunAsrDialogParams()
        )
        Log.d(TAG, "start_dialog code=$startCode")
        if (startCode == Constants.NuiResultCode.SUCCESS) {
            activeSession.stopRequested = false
            cancelCaptureLimit(activeSession)
            scheduleCaptureLimit(activeSession)
            if (!activeSession.hasEmittedListeningStarted) {
                _events.tryEmit(SimRealtimeSpeechEvent.ListeningStarted)
                activeSession.hasEmittedListeningStarted = true
            }
            return true
        }
        failSession(
            activeSession = activeSession,
            reason = if (startCode == AUTH_FAILED_CODE) {
                SimRealtimeSpeechFailureReason.UNAVAILABLE
            } else {
                SimRealtimeSpeechFailureReason.ERROR
            },
            message = if (startCode == AUTH_FAILED_CODE) {
                "当前无法获取有效的语音识别凭证，请稍后重试"
            } else {
                "当前无法开始语音识别，请重试"
            },
            diagnostic = if (startCode == AUTH_FAILED_CODE) {
                DashscopeRealtimeAuthDiagnostic(
                    category = DashscopeRealtimeAuthFailureCategory.SDK_AUTH_REJECTED,
                    vendorCode = startCode,
                    safeMessage = "FunASR 实时鉴权被拒绝"
                )
            } else {
                null
            }
        )
        return false
    }

    private fun prepareAudioRecord(): Boolean {
        if (
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        releaseAudioRecord()
        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        )
        if (minBufferSize <= 0) return false
        val createdRecorder = AudioRecord(
            MediaRecorder.AudioSource.DEFAULT,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            minBufferSize * 2
        )
        if (createdRecorder.state != AudioRecord.STATE_INITIALIZED) {
            createdRecorder.release()
            return false
        }
        synchronized(sessionLock) {
            audioRecord = createdRecorder
        }
        return true
    }

    private fun releaseAudioRecord() {
        val recorder = synchronized(sessionLock) {
            audioRecord.also { audioRecord = null }
        } ?: return
        Log.d(TAG, "release_audio_record")
        recorder.runCatchingStop()
        recorder.release()
    }

    private fun completeSession(
        targetSession: RecognitionSession,
        result: SimRealtimeSpeechRecognitionResult
    ) {
        if (targetSession.deferred.isCompleted) return
        cancelCaptureLimit(targetSession)
        targetSession.deferred.complete(result)
        synchronized(sessionLock) {
            if (session === targetSession) {
                session = null
            }
        }
    }

    private fun scheduleCaptureLimit(targetSession: RecognitionSession) {
        workerHandler.postDelayed(targetSession.captureLimitRunnable, MAX_CAPTURE_DURATION_MILLIS)
    }

    private fun cancelCaptureLimit(targetSession: RecognitionSession) {
        workerHandler.removeCallbacks(targetSession.captureLimitRunnable)
    }

    private fun requestStop(
        targetSession: RecognitionSession,
        notifyLimitReached: Boolean
    ) {
        synchronized(sessionLock) {
            if (session !== targetSession || targetSession.deferred.isCompleted || targetSession.stopRequested) {
                return
            }
            targetSession.stopRequested = true
        }
        workerHandler.post {
            cancelCaptureLimit(targetSession)
            if (notifyLimitReached) {
                _events.tryEmit(SimRealtimeSpeechEvent.CaptureLimitReached)
            }
            nui.stopDialog()
        }
    }

    private fun failSession(
        activeSession: RecognitionSession,
        reason: SimRealtimeSpeechFailureReason,
        message: String,
        diagnostic: DashscopeRealtimeAuthDiagnostic? = null
    ) {
        Log.w(
            TAG,
            "session_failed reason=$reason authCategory=${diagnostic.logName} vendorCode=${diagnostic?.vendorCode.logValue} message=$message"
        )
        _events.tryEmit(
            SimRealtimeSpeechEvent.Failure(
                reason = reason,
                message = message,
                diagnostic = diagnostic
            )
        )
        completeSession(
            activeSession,
            SimRealtimeSpeechRecognitionResult.Failure(
                reason = reason,
                message = message,
                diagnostic = diagnostic
            )
        )
    }

    private fun clearFinishedSession() {
        synchronized(sessionLock) {
            if (session?.deferred?.isCompleted == true) {
                session = null
            }
        }
    }

    private inner class RecognitionSession(
        val deferred: CompletableDeferred<SimRealtimeSpeechRecognitionResult> = CompletableDeferred()
    ) {
        var latestTranscript: String = ""
        var stopRequested: Boolean = false
        var hasEmittedListeningStarted: Boolean = false
        val captureLimitRunnable = Runnable {
            // 保持 stop 请求幂等，避免 60 秒自动收口后二次 stop。
            requestStop(this, notifyLimitReached = true)
        }
    }
}

private fun AudioRecord.runCatchingStop() {
    runCatching {
        if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            stop()
        }
    }
}

private val DashscopeRealtimeAuthDiagnostic?.logName: String
    get() = this?.category?.logName ?: "none"

private val DashscopeRealtimeAuthFailureCategory.logName: String
    get() = when (this) {
        DashscopeRealtimeAuthFailureCategory.CONFIG_MISSING -> "config_missing"
        DashscopeRealtimeAuthFailureCategory.SDK_AUTH_REJECTED -> "sdk_auth_rejected"
        DashscopeRealtimeAuthFailureCategory.UNKNOWN -> "unknown"
    }

private val Int?.logValue: String
    get() = this?.toString() ?: "none"

internal fun buildMissingApiKeyAuthDiagnostic(): DashscopeRealtimeAuthDiagnostic =
    DashscopeRealtimeAuthDiagnostic(
        category = DashscopeRealtimeAuthFailureCategory.CONFIG_MISSING,
        safeMessage = "缺少 DASHSCOPE_API_KEY"
    )

internal fun buildFunAsrInitParams(
    apiKey: String,
    deviceId: String
): String {
    val initParams = JSONObject()
    initParams.put("device_id", deviceId)
    initParams.put("url", FUN_ASR_REALTIME_SERVICE_URL)
    initParams.put("service_mode", Constants.ModeFullCloud)
    initParams.put("apikey", apiKey)
    return initParams.toString()
}

internal fun buildFunAsrDialogParams(): String = JSONObject().toString()

internal fun sanitizeFunAsrTranscript(raw: String?): String {
    val payload = raw?.trim().orEmpty()
    if (payload.isBlank()) return ""
    if (!payload.looksLikeJsonPayload()) return payload
    return parseFunAsrPayload(payload)?.trim().orEmpty()
}

internal fun sanitizeFunAsrErrorMessage(raw: String?): String? {
    val payload = raw?.trim().orEmpty()
    if (payload.isBlank()) return null
    if (!payload.looksLikeJsonPayload()) return payload
    return parseFunAsrErrorPayload(payload)?.trim().takeUnless { it.isNullOrBlank() }
}

private fun String.looksLikeJsonPayload(): Boolean {
    return startsWith("{") || startsWith("[")
}

private fun parseFunAsrPayload(payload: String): String? {
    return runCatching {
        when {
            payload.startsWith("{") -> extractTranscriptFromJsonObject(JSONObject(payload))
            payload.startsWith("[") -> extractTranscriptFromJsonArray(JSONArray(payload))
            else -> null
        }
    }.getOrNull()?.takeIf { it.isNotBlank() }
}

private fun parseFunAsrErrorPayload(payload: String): String? {
    return runCatching {
        when {
            payload.startsWith("{") -> extractErrorMessageFromJsonObject(JSONObject(payload))
            payload.startsWith("[") -> extractErrorMessageFromJsonArray(JSONArray(payload))
            else -> null
        }
    }.getOrNull()?.takeIf { it.isNotBlank() }
}

private fun extractTranscriptFromJsonObject(json: JSONObject): String? {
    preferredTranscriptKeys.forEach { key ->
        extractTranscriptValue(json.opt(key))?.let { return it }
    }
    extractWordsTranscript(json.optJSONArray("words"))?.let { return it }
    val iterator = json.keys()
    while (iterator.hasNext()) {
        extractTranscriptValue(json.opt(iterator.next()))?.let { return it }
    }
    return null
}

private fun extractTranscriptFromJsonArray(array: JSONArray): String? {
    for (index in 0 until array.length()) {
        extractTranscriptValue(array.opt(index))?.let { return it }
    }
    return null
}

private fun extractTranscriptValue(value: Any?): String? {
    return when (value) {
        is String -> sanitizeLeafText(value)
        is JSONObject -> extractTranscriptFromJsonObject(value)
        is JSONArray -> extractTranscriptFromJsonArray(value)
        else -> null
    }
}

private fun extractErrorMessageFromJsonObject(json: JSONObject): String? {
    preferredErrorKeys.forEach { key ->
        extractErrorValue(json.opt(key))?.let { return it }
    }
    structuralErrorKeys.forEach { key ->
        extractErrorValue(json.opt(key))?.let { return it }
    }
    return null
}

private fun extractErrorMessageFromJsonArray(array: JSONArray): String? {
    for (index in 0 until array.length()) {
        extractErrorValue(array.opt(index))?.let { return it }
    }
    return null
}

private fun extractErrorValue(value: Any?): String? {
    return when (value) {
        is String -> sanitizeLeafText(value)
        is JSONObject -> extractErrorMessageFromJsonObject(value)
        is JSONArray -> extractErrorMessageFromJsonArray(value)
        else -> null
    }
}

private fun extractWordsTranscript(words: JSONArray?): String? {
    if (words == null || words.length() == 0) return null
    val combined = buildString {
        for (index in 0 until words.length()) {
            val wordText = words.optJSONObject(index)
                ?.optString("text")
                ?.trim()
                .orEmpty()
            if (wordText.isNotBlank()) {
                append(wordText)
            }
        }
    }.trim()
    return combined.takeIf { it.isNotBlank() }
}

private fun sanitizeLeafText(raw: String): String? {
    val text = raw.trim()
    if (text.isBlank() || text.looksLikeJsonPayload()) return null
    return text
}

private val preferredTranscriptKeys = listOf(
    "sentence",
    "text",
    "transcript",
    "result",
    "output",
    "payload",
    "data"
)

private val preferredErrorKeys = listOf(
    "message",
    "msg",
    "error_message",
    "errorMsg",
    "status_message",
    "statusMessage",
    "error"
)

private val structuralErrorKeys = listOf(
    "payload",
    "output",
    "data",
    "detail",
    "details"
)
