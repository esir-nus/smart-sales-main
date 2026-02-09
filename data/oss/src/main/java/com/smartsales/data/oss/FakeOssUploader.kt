package com.smartsales.data.oss

import java.io.File

/**
 * 测试用假 OSS 上传器 — 返回固定 URL，不做真实网络请求。
 */
class FakeOssUploader : OssUploader {

    /** 控制上传是否成功 */
    var shouldFail = false
    var failCode = OssErrorCode.NETWORK_ERROR
    var failMessage = "模拟上传失败"

    /** 记录上传历史 */
    val uploadHistory = mutableListOf<Pair<String, String>>()

    override suspend fun upload(file: File, objectKey: String): OssUploadResult {
        uploadHistory.add(file.name to objectKey)

        if (shouldFail) {
            return OssUploadResult.Error(failCode, failMessage)
        }

        return OssUploadResult.Success("https://fake-bucket.oss-cn-beijing.aliyuncs.com/$objectKey")
    }

    fun clear() {
        uploadHistory.clear()
        shouldFail = false
    }
}
