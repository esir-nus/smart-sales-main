package com.smartsales.data.aicore

import com.smartsales.data.aicore.tingwu.api.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.util.concurrent.ConcurrentLinkedQueue

open class FakeTingwuApi : TingwuApi {
    private val statusQueue = ConcurrentLinkedQueue<TingwuStatusResponse>()
    var resultData: TingwuResultResponse? = null
    var failResultWith404: Boolean = false
    var lastCreateRequest: TingwuCreateTaskRequest? = null

    fun enqueueStatus(data: TingwuStatusResponse) {
        statusQueue += data
    }

    override suspend fun createTranscriptionTask(
        type: String,
        operation: String?,
        body: TingwuCreateTaskRequest
    ): TingwuCreateTaskResponse {
        lastCreateRequest = body
        return TingwuCreateTaskResponse(
            requestId = "req-create",
            code = "0",
            message = "Success",
            data = TingwuCreateTaskData(
                taskId = "job-1",
                taskKey = body.input.taskKey,
                taskStatus = "QUEUED",
                meetingJoinUrl = null
            )
        )
    }

    override suspend fun getTaskStatus(
        taskId: String,
        type: String
    ): TingwuStatusResponse {
        return statusQueue.poll()
            ?: statusResponse(
                status = "FAILED",
                progress = 0,
                errorCode = "Empty",
                errorMessage = "状态为空"
            )
    }

    override suspend fun getTaskResult(
        taskId: String,
        format: String
    ): TingwuResultResponse {
        if (failResultWith404) {
            throw HttpException(
                Response.error<TingwuResultResponse>(
                    404,
                    "Not Found".toResponseBody("application/json".toMediaType())
                )
            )
        }
        return resultData
            ?: TingwuResultResponse(
                requestId = "req-result-empty",
                code = "0",
                message = "Success",
                data = TingwuResultData(
                    taskId = taskId,
                    transcription = TingwuTranscription(
                        text = "",
                        segments = emptyList(),
                        speakers = emptyList(),
                        language = "zh",
                        duration = 0.0,
                        url = null
                    ),
                    resultLinks = emptyMap(),
                    outputMp3Path = null,
                    outputMp4Path = null,
                    outputThumbnailPath = null,
                    outputSpectrumPath = null
                )
            )
    }
}

fun statusResponse(
    status: String,
    progress: Int,
    errorCode: String? = null,
    errorMessage: String? = null,
    resultLinks: Map<String, String>? = null
): TingwuStatusResponse = TingwuStatusResponse(
    requestId = "req-$status-$progress",
    code = "0",
    message = "Success",
    data = TingwuStatusData(
        taskId = "job-1",
        taskKey = "job-1",
        taskStatus = status,
        taskProgress = progress,
        errorCode = errorCode,
        errorMessage = errorMessage,
        outputMp3Path = null,
        outputMp4Path = null,
        outputThumbnailPath = null,
        outputSpectrumPath = null,
        resultLinks = resultLinks,
        meetingJoinUrl = null
    )
)
