package com.smartsales.data.aicore.tingwu

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

// 文件：data/ai-core/src/main/java/com/smartsales/data/aicore/tingwu/TingwuApi.kt
// 模块：:data:ai-core
// 说明：定义 Tingwu 官方 `/openapi/tingwu/v2` 请求/响应结构
// 作者：更新于 2025-11-19
interface TingwuApi {
    @PUT("tasks")
    suspend fun createTranscriptionTask(
        @Query("type") type: String = "offline",
        @Query("operation") operation: String? = null,
        @Body body: TingwuCreateTaskRequest
    ): TingwuCreateTaskResponse

    @GET("tasks/{taskId}")
    suspend fun getTaskStatus(
        @Path("taskId") taskId: String,
        @Query("type") type: String = "offline"
    ): TingwuStatusResponse

    @GET("tasks/{taskId}/transcription")
    suspend fun getTaskResult(
        @Path("taskId") taskId: String,
        @Query("format") format: String = "json"
    ): TingwuResultResponse
}

data class TingwuCreateTaskRequest(
    @SerializedName("AppKey") val appKey: String,
    @SerializedName("Input") val input: TingwuTaskInput,
    @SerializedName("Parameters") val parameters: TingwuTaskParameters
)

data class TingwuTaskInput(
    @SerializedName("SourceLanguage") val sourceLanguage: String,
    @SerializedName("TaskKey") val taskKey: String,
    @SerializedName("FileUrl") val fileUrl: String
)

data class TingwuCreateTaskResponse(
    @SerializedName("RequestId") val requestId: String?,
    @SerializedName("Code") val code: String?,
    @SerializedName("Message") val message: String?,
    @SerializedName("Data") val data: TingwuCreateTaskData?
)

data class TingwuCreateTaskData(
    @SerializedName("TaskId") val taskId: String?,
    @SerializedName("TaskKey") val taskKey: String?,
    @SerializedName("TaskStatus") val taskStatus: String?,
    @SerializedName("MeetingJoinUrl") val meetingJoinUrl: String?
)

data class TingwuStatusResponse(
    @SerializedName("RequestId") val requestId: String?,
    @SerializedName("Code") val code: String?,
    @SerializedName("Message") val message: String?,
    @SerializedName("Data") val data: TingwuStatusData?
)

data class TingwuResultResponse(
    @SerializedName("RequestId") val requestId: String?,
    @SerializedName("Code") val code: String?,
    @SerializedName("Message") val message: String?,
    @SerializedName("Data") val data: TingwuResultData?
)

data class TingwuTaskParameters(
    @SerializedName("Transcription") val transcription: TingwuTranscriptionParameters? = null,
    @SerializedName("TranslationEnabled") val translationEnabled: Boolean? = null
)

data class TingwuTranscriptSegment(
    @SerializedName("Id") val id: Int?,
    @SerializedName("Start") val start: Double?,
    @SerializedName("End") val end: Double?,
    @SerializedName("Text") val text: String?,
    @SerializedName("Speaker") val speaker: String?
)

data class TingwuSpeaker(
    @SerializedName("Id") val id: String,
    @SerializedName("Name") val name: String? = null,
    @SerializedName("TotalTime") val totalTime: Double? = null
)

data class TingwuStatusData(
    @SerializedName("TaskId") val taskId: String?,
    @SerializedName("TaskKey") val taskKey: String?,
    @SerializedName("TaskStatus") val taskStatus: String?,
    @SerializedName("TaskProgress") val taskProgress: Int?,
    @SerializedName("ErrorCode") val errorCode: String?,
    @SerializedName("ErrorMessage") val errorMessage: String?,
    @SerializedName("OutputMp3Path") val outputMp3Path: String?,
    @SerializedName("OutputMp4Path") val outputMp4Path: String?,
    @SerializedName("OutputThumbnailPath") val outputThumbnailPath: String?,
    @SerializedName("OutputSpectrumPath") val outputSpectrumPath: String?,
    @SerializedName("Result") val resultLinks: Map<String, String>?,
    @SerializedName("MeetingJoinUrl") val meetingJoinUrl: String?
)

data class TingwuResultData(
    @SerializedName("TaskId") val taskId: String?,
    @SerializedName("Transcription") val transcription: TingwuTranscription?,
    @SerializedName("Result") val resultLinks: Map<String, String>?,
    @SerializedName("OutputMp3Path") val outputMp3Path: String?,
    @SerializedName("OutputMp4Path") val outputMp4Path: String?,
    @SerializedName("OutputThumbnailPath") val outputThumbnailPath: String?,
    @SerializedName("OutputSpectrumPath") val outputSpectrumPath: String?
)

data class TingwuTranscriptionParameters(
    @SerializedName("DiarizationEnabled") val diarizationEnabled: Boolean? = null,
    @SerializedName("Diarization") val diarization: TingwuDiarizationParameters? = null,
    @SerializedName("Model") val model: String? = null
)

data class TingwuDiarizationParameters(
    @SerializedName("SpeakerCount") val speakerCount: Int? = null
)

data class TingwuTranscription(
    @SerializedName("Text") val text: String?,
    @SerializedName("Segments") val segments: List<TingwuTranscriptSegment>?,
    @SerializedName("Speakers") val speakers: List<TingwuSpeaker>?,
    @SerializedName("Language") val language: String?,
    @SerializedName("Duration") val duration: Double?,
    @SerializedName("Url") val url: String? = null
)
