package com.smartsales.prism.data.connectivity.legacy.gateway

import com.smartsales.prism.data.connectivity.legacy.DeviceNetworkStatus
import com.smartsales.prism.data.connectivity.legacy.WifiCredentials
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.delay
import org.json.JSONObject

private const val NETWORK_QUERY_COMMAND = "wifi#address#ip#name"
internal const val PROVISIONING_COMMAND_GAP_MS = 300L

internal class GattBleGatewayProtocolSupport {

    fun buildSsidPayload(ssid: String): ByteArray {
        val sanitized = sanitizeSegment(ssid)
        return "SD#$sanitized".toByteArray(Charsets.UTF_8)
    }

    fun buildPasswordPayload(password: String): ByteArray {
        val sanitized = sanitizeSegment(password)
        return "PD#$sanitized".toByteArray(Charsets.UTF_8)
    }

    fun buildNetworkQueryPayload(): ByteArray =
        NETWORK_QUERY_COMMAND.toByteArray(Charsets.UTF_8)

    fun formatCurrentTime(): String {
        val sdf = java.text.SimpleDateFormat("yyyyMMddHHmmss", java.util.Locale.US)
        return sdf.format(java.util.Date())
    }

    suspend fun dispatchProvisioningCredentials(
        credentials: WifiCredentials,
        sendSsid: suspend (ByteArray) -> Unit,
        sendPassword: suspend (ByteArray) -> Unit,
        waitBetweenWrites: suspend (Long) -> Unit = { delay(it) }
    ): BleGatewayResult.Success {
        sendSsid(buildSsidPayload(credentials.ssid))
        waitBetweenWrites(PROVISIONING_COMMAND_GAP_MS)
        sendPassword(buildPasswordPayload(credentials.password))
        return provisioningDispatchAccepted(credentials)
    }

    fun parseHotspot(bytes: ByteArray): WifiCredentials {
        try {
            val json = JSONObject(bytes.decodeToString())
            val ssid = json.optString("ssid")
            val password = json.optString("password")
            if (ssid.isBlank() || password.isBlank()) {
                throw IllegalStateException("热点返回值缺失")
            } else {
                return WifiCredentials(ssid = ssid, password = password)
            }
        } catch (ex: Exception) {
            throw IllegalStateException("热点响应解析失败：${ex.message}")
        }
    }

    // 解析设备 Provisioning 确认响应，返回是否成功及可选错误原因。
    fun parseProvisioningAck(payload: ByteArray): ProvisioningAckResult {
        val raw = payload.decodeToString().trim()
        return if (raw.startsWith("ok", ignoreCase = true)) {
            ProvisioningAckResult.Accepted
        } else {
            ProvisioningAckResult.Rejected(raw)
        }
    }

    fun parseWavResponse(payload: ByteArray): WavCommandResult {
        val raw = payload.decodeToString().trim()
        if (raw.isBlank()) throw IllegalStateException("WAV响应为空")

        val parts = raw.split("#")
        if (parts.size < 2) throw IllegalStateException("WAV响应格式错误：$raw")
        if (!parts[0].equals("wav", true)) {
            throw IllegalStateException("WAV响应类型不符：$raw")
        }

        return when (parts[1].lowercase()) {
            "send" -> WavCommandResult.Ready
            "ok" -> WavCommandResult.Done
            else -> throw IllegalStateException("未知WAV响应：$raw")
        }
    }

    private fun sanitizeSegment(input: String): String = input.replace("#", "-")

    private fun provisioningDispatchAccepted(
        credentials: WifiCredentials
    ): BleGatewayResult.Success = BleGatewayResult.Success(
        handshakeId = UUID.randomUUID().toString(),
        credentialsHash = sha256("${credentials.ssid}${credentials.password}")
    )

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}

internal fun parseBadgeNotificationPayload(rawPayload: String): BadgeNotification? {
    val raw = rawPayload.trim()
    return when {
        raw.isBlank() -> null
        raw.startsWith("tim#get", ignoreCase = true) -> BadgeNotification.TimeSyncRequested
        raw.startsWith("log#", ignoreCase = true) -> {
            val filename = raw.removePrefix("log#").trim()
            if (filename.isBlank()) BadgeNotification.Unknown(raw)
            else BadgeNotification.RecordingReady(filename)
        }
        raw.startsWith("rec#", ignoreCase = true) -> {
            val token = raw.removePrefix("rec#").trim()
            if (token.isBlank()) BadgeNotification.Unknown(raw)
            else BadgeNotification.AudioRecordingReady(token)
        }
        raw.startsWith("Bat#", ignoreCase = true) -> {
            val percent = raw.substringAfter('#', missingDelimiterValue = "").trim().toIntOrNull()
            if (percent == null || percent !in 0..100) BadgeNotification.Unknown(raw)
            else BadgeNotification.BatteryLevel(percent)
        }
        raw.startsWith("Ver#", ignoreCase = true) -> {
            val version = raw.substringAfter('#', missingDelimiterValue = "").trim()
            if (isFirmwareVersionPayload(version)) BadgeNotification.FirmwareVersion(version)
            else BadgeNotification.Unknown(raw)
        }
        raw.startsWith("SD#", ignoreCase = true) -> {
            val parts = raw.split("#")
            if (
                parts.size >= 3 &&
                parts[0].equals("SD", ignoreCase = true) &&
                parts[1].equals("space", ignoreCase = true)
            ) {
                val formattedSize = parts.drop(2).joinToString("#").trim()
                if (formattedSize.isBlank()) BadgeNotification.Unknown(raw)
                else BadgeNotification.SdCardSpace(formattedSize)
            } else {
                BadgeNotification.Unknown(raw)
            }
        }
        else -> BadgeNotification.Unknown(raw)
    }
}

private fun isFirmwareVersionPayload(version: String): Boolean {
    return version.isNotBlank() && !version.equals("get", ignoreCase = true)
}

internal fun mergeNetworkFragments(rawResponses: List<String>): DeviceNetworkStatus {
    val fragments = mutableMapOf<String, String>()
    for (raw in rawResponses) {
        val fragment = raw.toNetworkStatusFragment() ?: continue
        fragments[fragment.first] = fragment.second
    }

    val ip = fragments["IP"]
        ?: throw IllegalStateException("网络响应缺少 IP（收到: ${fragments.keys}）")
    val ssid = fragments["SD"]
        ?.takeUnless { it.equals("N/A", ignoreCase = true) }
        .orEmpty()

    return DeviceNetworkStatus(
        ipAddress = ip,
        deviceWifiName = ssid,
        phoneWifiName = "",
        rawResponse = fragments.entries.joinToString(", ") { "${it.key}#${it.value}" }
    )
}

internal fun String.toNetworkStatusFragment(): Pair<String, String>? {
    val raw = trim()
    val parts = raw.split("#", limit = 3)
    return when {
        parts.size >= 2 && parts[0].equals("IP", ignoreCase = true) ->
            "IP" to parts.drop(1).joinToString("#").trim()

        parts.size >= 2 &&
            parts[0].equals("SD", ignoreCase = true) &&
            !parts[1].equals("space", ignoreCase = true) ->
            "SD" to parts.drop(1).joinToString("#").trim()

        else -> null
    }
}

internal fun List<String>.hasNetworkStatusFragment(key: String): Boolean {
    return any { raw ->
        val fragment = raw.toNetworkStatusFragment()
        fragment?.first == key
    }
}

internal class CredentialRejectedException(message: String) : RuntimeException(message)
