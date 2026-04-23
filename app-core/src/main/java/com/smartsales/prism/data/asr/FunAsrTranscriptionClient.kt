package com.smartsales.prism.data.asr

import com.alibaba.dashscope.audio.asr.transcription.Transcription
import com.alibaba.dashscope.audio.asr.transcription.TranscriptionParam
import com.alibaba.dashscope.audio.asr.transcription.TranscriptionQueryParam
import com.alibaba.dashscope.utils.Constants
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

interface FunAsrTranscriptionClient {
    suspend fun transcribe(fileUrl: String, apiKey: String): String
}

@Singleton
class RealFunAsrTranscriptionClient @Inject constructor() : FunAsrTranscriptionClient {

    companion object {
        private const val MODEL = "fun-asr"
    }

    init {
        // 设置 DashScope 基础 URL (北京区域)
        Constants.baseHttpApiUrl = "https://dashscope.aliyuncs.com/api/v1"
    }

    override suspend fun transcribe(fileUrl: String, apiKey: String): String {
        val transcription = Transcription()
        val param = TranscriptionParam.builder()
            .model(MODEL)
            .apiKey(apiKey)
            .fileUrls(listOf(fileUrl))
            .parameter("language_hints", arrayOf("zh", "en"))
            .build()

        android.util.Log.d("FunAsrService", "Submitting async task to DashScope...")
        val asyncResult = transcription.asyncCall(param)
        val taskId = asyncResult.taskId
        android.util.Log.d("FunAsrService", "TaskId received: $taskId")
        checkNotNull(taskId) { "未返回 taskId" }

        android.util.Log.d("FunAsrService", "Waiting for transcription result...")
        val taskResult = transcription.wait(
            TranscriptionQueryParam.FromTranscriptionParam(param, taskId)
        )
        android.util.Log.d("FunAsrService", "Wait finished. Results size: ${taskResult.results?.size}")

        val results = taskResult.results.orEmpty()
        require(results.isNotEmpty()) { "转写结果为空" }

        val taskResultItem = results.first()
        require(taskResultItem.subTaskStatus?.toString() == "SUCCEEDED") {
            "转写任务失败: ${taskResultItem.subTaskStatus}"
        }

        val transcriptionUrl = requireNotNull(taskResultItem.transcriptionUrl) {
            "未返回转写结果 URL"
        }

        return downloadAndParseResult(transcriptionUrl)
    }

    private fun downloadAndParseResult(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connect()

        val responseCode = connection.responseCode
        check(responseCode == 200) { "HTTP $responseCode: ${connection.responseMessage}" }

        val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
        val jsonObject = JSONObject(jsonString)
        val transcripts = requireNotNull(jsonObject.optJSONArray("transcripts")) {
            "JSON 缺少 transcripts 字段"
        }
        require(transcripts.length() > 0) { "transcripts 数组为空" }
        return transcripts.getJSONObject(0).optString("text", "")
    }
}
