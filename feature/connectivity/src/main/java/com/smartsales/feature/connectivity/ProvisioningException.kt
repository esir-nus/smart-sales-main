package com.smartsales.feature.connectivity

// 文件：feature/connectivity/src/main/java/com/smartsales/feature/connectivity/ProvisioningException.kt
// 模块：:feature:connectivity
// 说明：定义真实 BLE/Wi-Fi 配网过程中的异常分类
// 作者：创建于 2025-11-16
sealed class ProvisioningException(message: String) : RuntimeException(message) {
    class PermissionDenied(val permissions: Set<String>) : ProvisioningException(
        "缺少权限：${permissions.joinToString()}"
    )

    class Timeout(val timeoutMillis: Long) : ProvisioningException("配网超时 ${timeoutMillis}ms")

    class Transport(val reason: String) : ProvisioningException(reason)

    class CredentialRejected(message: String) : ProvisioningException(message)

}

internal fun Throwable.toConnectivityError(): ConnectivityError = when (this) {
    is ProvisioningException.PermissionDenied ->
        ConnectivityError.PermissionDenied(permissions)

    is ProvisioningException.Timeout ->
        ConnectivityError.Timeout(timeoutMillis)

    is ProvisioningException.Transport ->
        ConnectivityError.Transport(message ?: "传输失败")

    is ProvisioningException.CredentialRejected ->
        ConnectivityError.ProvisioningFailed(message ?: "凭据校验失败")

    else -> ConnectivityError.ProvisioningFailed(message ?: "未知错误")
}
