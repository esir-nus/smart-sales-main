package com.smartsales.prism.domain.tingwu

import com.smartsales.core.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * 阿里云听悟 (Tingwu) 的长语音处理管道。
 * 此接口位于 Prism Domain 层，对特定数据层实现透明。
 * (负责语音转写、声纹分离、智能摘要等)
 */
interface TingwuPipeline {
    /**
     * 提交音频对应的 OSS URL 进行转写任务。
     * @return Result<String> 成功时返回 Aliyun 的 Task ID (jobId)
     */
    suspend fun submit(request: TingwuRequest): Result<String>

    /**
     * 观察任务状态的变更。
     * @param jobId 提交任务时返回的 Task ID
     */
    fun observeJob(jobId: String): Flow<TingwuJobState>
}
