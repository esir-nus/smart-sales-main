package com.smartsales.prism.data.connectivity

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class BadgeRuntimeKey(
    val peripheralId: String,
    val secureToken: String
) {
    fun toLogString(): String {
        return "$peripheralId/${secureToken.takeLast(6)}"
    }
}

data class BadgeEndpointSnapshot(
    val runtimeKey: BadgeRuntimeKey,
    val badgeIp: String,
    val baseUrl: String
)

@Singleton
class BadgeEndpointRecoveryCoordinator @Inject constructor() {

    private val stateMutex = Mutex()
    private var currentRuntimeKey: BadgeRuntimeKey? = null
    private var latestResolvedEndpoint: BadgeEndpointSnapshot? = null
    private var postCredentialGrace: PostCredentialGraceState? = null

    suspend fun noteCurrentRuntimeKey(runtimeKey: BadgeRuntimeKey?) {
        stateMutex.withLock {
            if (currentRuntimeKey == runtimeKey) return
            currentRuntimeKey = runtimeKey
            if (runtimeKey == null || latestResolvedEndpoint?.runtimeKey != runtimeKey) {
                latestResolvedEndpoint = null
            }
            if (runtimeKey == null || postCredentialGrace?.runtimeKey != runtimeKey) {
                postCredentialGrace = null
            }
        }
    }

    suspend fun currentRuntimeKey(): BadgeRuntimeKey? = stateMutex.withLock { currentRuntimeKey }

    suspend fun noteResolvedEndpoint(snapshot: BadgeEndpointSnapshot) {
        stateMutex.withLock {
            latestResolvedEndpoint = snapshot
        }
    }

    suspend fun latestResolvedEndpoint(): BadgeEndpointSnapshot? =
        stateMutex.withLock { latestResolvedEndpoint }

    suspend fun armPostCredentialGrace(runtimeKey: BadgeRuntimeKey) {
        stateMutex.withLock {
            postCredentialGrace = PostCredentialGraceState(runtimeKey = runtimeKey)
        }
    }

    suspend fun consumePostCredentialProbeDelayMs(
        runtimeKey: BadgeRuntimeKey,
        allowImplicitArm: Boolean
    ): Long? {
        return stateMutex.withLock {
            val activeGrace = when {
                postCredentialGrace?.runtimeKey == runtimeKey -> postCredentialGrace
                postCredentialGrace == null && allowImplicitArm ->
                    PostCredentialGraceState(runtimeKey = runtimeKey).also {
                        postCredentialGrace = it
                    }
                else -> null
            } ?: return@withLock null

            val nextDelay = POST_CREDENTIAL_GRACE_DELAYS_MS.getOrNull(activeGrace.nextProbeIndex)
                ?: return@withLock null
            activeGrace.nextProbeIndex += 1
            nextDelay
        }
    }

    suspend fun hasPendingPostCredentialGrace(runtimeKey: BadgeRuntimeKey?): Boolean {
        if (runtimeKey == null) return false
        return stateMutex.withLock {
            val activeGrace = postCredentialGrace ?: return@withLock false
            activeGrace.runtimeKey == runtimeKey &&
                activeGrace.nextProbeIndex < POST_CREDENTIAL_GRACE_DELAYS_MS.size
        }
    }

    suspend fun clearPostCredentialGrace(runtimeKey: BadgeRuntimeKey?) {
        stateMutex.withLock {
            if (runtimeKey == null || postCredentialGrace?.runtimeKey == runtimeKey) {
                postCredentialGrace = null
            }
        }
    }

    private class PostCredentialGraceState(
        val runtimeKey: BadgeRuntimeKey,
        var nextProbeIndex: Int = 0
    )

    private companion object {
        val POST_CREDENTIAL_GRACE_DELAYS_MS = listOf(2_000L, 3_000L, 5_000L)
    }
}
