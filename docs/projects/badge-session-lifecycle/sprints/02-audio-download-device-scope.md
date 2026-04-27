# Sprint 02 — audio-download-device-scope

## Header

- Project: badge-session-lifecycle
- Sprint: 02
- Slug: audio-download-device-scope
- Date authored: 2026-04-27
- Author: Claude
- Operator: Codex
- Lane: `develop`
- Branch: `develop`
- Worktree: none

## Demand

**User ask (verbatim):** "when already have one badge paired, and we pair another new badge, say it's Badge B. after B paired successful, we switch to badge A, we failed to sync badge A's contents. that indicates we may not have a healthy lifecycle when treating multiple paired devices"

**Problem narrative:**

When the user pairs Badge B and audio sync starts downloading Badge B's audio files, those downloads run on a coroutine worker inside `SimAudioRepositoryRuntime.repositoryScope`. This scope is a `@Singleton` — it lives for the full app lifetime and has no concept of which device it belongs to. The download queue (`queuedBadgeDownloads: LinkedHashSet<String>`) stores plain filenames with no device MAC attached.

When the user then switches back to Badge A, `RealDeviceRegistryManager.switchToDevice()` updates the active device and triggers a BLE reconnect. It cancels BLE heartbeat and notification listener jobs. But the audio download worker for Badge B is still running — it was never told the device changed.

What happens next: Badge B's worker calls `connectivityBridge.downloadRecording(filename)`, which routes through the active device's connection. The active device is now Badge A. Badge A does not have Badge B's files. The downloads fail. More critically, Badge A's own audio sync never starts, because the download worker slot (`badgeDownloadWorkerJob`) is still occupied by Badge B's worker.

The user sees: Badge A has no audio content available even though it has recordings. The symptom looks like a connectivity bug or a sync trigger bug — but the root is that audio work is not device-scoped.

This problem exists because each sprint that added audio capability (sync trigger, download queue, cancel-per-file) was written assuming one active device. No sprint ever asked: "what happens to in-flight audio work when the device changes?" The `cancelBadgeDownload(filename)` method exists for per-file user-initiated cancellation but is never called for bulk teardown on a switch.

**Design rationale for the fix:**

The audio repository should observe `DeviceRegistryManager.activeDevice` and cancel all in-flight downloads when the active device changes. This keeps the dependency direction clean: the registry manager does not need to know that audio exists, and the audio repository reacts to the device signal without requiring a callback from the connectivity layer. The fix is self-contained to the audio layer.

**Important:** this contract prescribes a specific implementation approach based on static code analysis. Before executing, read `SimAudioRepositorySyncSupport.kt` fully, particularly `processBadgeDownloadQueue()` and the mutex usage. If the actual implementation has constraints (e.g., the queue holds structured objects with device affinity already, or there is a different cancellation mechanism) that make the prescribed approach incorrect, adapt and note the reason in the ledger.

## Scope

Files the operator may touch:

- `app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepositorySyncSupport.kt` — add `cancelAllBadgeDownloads()`
- `app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepository.kt` — inject `DeviceRegistryManager`, add `activeDevice` observer in `init`
- The Hilt DI module that provides `SimAudioRepository` — add `DeviceRegistryManager` parameter
- Any unit test files for `SimAudioRepository` or `SimAudioRepositorySyncSupport`
- `docs/projects/badge-session-lifecycle/sprints/02-audio-download-device-scope.md` (this file)
- `docs/projects/badge-session-lifecycle/tracker.md`
- `docs/core-flow/badge-session-lifecycle.md` — only to reference this sprint's change in the implementation notes section, if one exists

**Out of scope:**
- `RealDeviceRegistryManager.kt` — do not add any audio awareness here; dependency must stay one-directional
- `domain/` — no domain model changes
- `data/connectivity/` (other than reading for reference)
- `SimAudioRepositoryRuntime.kt` — do not restructure the scope or queue shape; just add cancellation support

## References

- `docs/core-flow/badge-session-lifecycle.md` — produced by Sprint 01; defines the "audio sync teardown on device switch" invariant this sprint implements
- `app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepositoryRuntime.kt:37,47-50` — `repositoryScope`, `queuedBadgeDownloads`, `badgeDownloadWorkerJob`, `activeBadgeDownloadJob`
- `app-core/src/main/java/com/smartsales/prism/data/audio/SimAudioRepositorySyncSupport.kt:429-443,486-495` — `cancelBadgeDownload()`, `enqueueBadgeDownloads()`
- `app-core/src/main/java/com/smartsales/prism/data/connectivity/registry/RealDeviceRegistryManager.kt:169-195` — `switchToDevice()` — read to confirm what it does NOT cancel

## Success Exit Criteria

1. **New method present:** `grep -n "cancelAllBadgeDownloads" SimAudioRepositorySyncSupport.kt` returns a hit.
2. **Observer present:** `grep -n "registryManager.activeDevice" SimAudioRepository.kt` returns a hit inside `init`.
3. **Tests green:** `./gradlew :app-core:testDebugUnitTest` exits 0. If any existing test breaks, fix it before declaring success.
4. **Logcat evidence:** after switching devices while Badge B downloads are in-flight, `adb logcat -d | grep "SIM badge sync"` shows "all downloads cancelled (device switch)" followed by Badge A's sync starting (listing or enqueueing its own files).

## Stop Exit Criteria

- `SimAudioRepository` is constructed without Hilt (e.g., manually in a test) in a way that makes adding a constructor parameter unworkable without a large refactor — stop and surface.
- `processsBadgeDownloadQueue()` holds the mutex across the full download (not just queue access), making the cancel approach a deadlock risk — stop and surface.
- The connectivity bridge (`downloadRecording()`) is found to be device-agnostic already (routes to a queue internally) — in that case the device-switch problem may be different than hypothesized; stop and surface.

## Iteration Bound

3 iterations or 45 minutes, whichever is reached first.

## Required Evidence Format

1. `grep` output for criteria 1 and 2.
2. `./gradlew :app-core:testDebugUnitTest` pass summary line.
3. `adb logcat` excerpt showing cancellation log on device switch and Badge A sync starting.

## Prescribed Fix (operator treats as a starting point, not a constraint)

### A. `SimAudioRepositorySyncSupport.kt` — add `cancelAllBadgeDownloads()`

```kotlin
suspend fun cancelAllBadgeDownloads() = withContext(runtime.ioDispatcher) {
    runtime.badgeDownloadQueueMutex.withLock {
        runtime.queuedBadgeDownloads.clear()
        runtime.activeBadgeDownloadJob?.cancel(
            CancellationException("device switched")
        )
        runtime.activeBadgeDownloadFilename = null
    }
    Log.d(SIM_AUDIO_SYNC_LOG_TAG, "SIM badge sync: all downloads cancelled (device switch)")
}
```

### B. `SimAudioRepository.kt` — observe `activeDevice`, cancel on change

Constructor injection: add `private val registryManager: DeviceRegistryManager`.

In `init {}`:
```kotlin
runtime.repositoryScope.launch {
    var previousMac: String? = null
    registryManager.activeDevice.collect { device ->
        val mac = device?.macAddress
        if (previousMac != null && mac != previousMac) {
            syncSupport.cancelAllBadgeDownloads()
        }
        previousMac = mac
    }
}
```

Update the Hilt module providing `SimAudioRepository` to inject `DeviceRegistryManager`.

## Iteration Ledger

<!-- Operator appends one entry per iteration. Not committed mid-sprint. -->

## Closeout

<!-- Operator fills on exit. -->

- Status:
- Summary:
- Evidence:
- Lesson proposals:
- CHANGELOG line:
