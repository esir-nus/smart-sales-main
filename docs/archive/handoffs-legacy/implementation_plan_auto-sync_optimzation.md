# Badge Audio Auto-Sync (`rec#`) + Drawer UX + Dynamic Island

Introduce a new `rec#YYYYMMDD_HHMMSS` BLE command for push-based audio auto-download, optimize the audio drawer button UX, and reflect sync status through the Dynamic Island.

## User Review Required

> [!IMPORTANT]
> **Firmware dependency**: This plan requires the hardware team to add `rec#YYYYMMDD_HHMMSS` BLE notification to ESP32 firmware. The command fires when a recording finishes and the file is ready for download. App constructs filename as `rec_YYYYMMDD_HHMMSS.wav` and downloads directly — no `/list` needed.

> [!IMPORTANT]
> **Two notification pipelines**: `log#` stays for scheduler pipeline (download+transcribe+schedule). `rec#` is the new audio-only pipeline (download+drawer placeholder). They are parallel, independent paths sharing the same `ConnectivityBridge.downloadRecording()` transport.

> [!WARNING]
> **`/list` fallback on reconnection**: `rec#` only works while BLE is connected. If the user records while the app is away (BLE disconnected), those files are only discoverable via `/list` on reconnection. We keep the existing `/list` auto-sync as a reconnection fallback.

---

## Proposed Changes

### Component 1: `rec#` BLE Notification Handling

Summary: Add BLE parsing for `rec#`, new domain notification variant, new `ConnectivityBridge` flow, and a lightweight auto-download handler that creates drawer placeholders and downloads in the background (no transcribe/schedule).

#### [MODIFY] [BleGateway.kt](file:///home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/gateway/BleGateway.kt)

Add `AudioRecordingReady(filename: String)` variant to `BadgeNotification` sealed class.

#### [MODIFY] [GattBleGatewayProtocolSupport.kt](file:///home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/gateway/GattBleGatewayProtocolSupport.kt)

Add `rec#` case to `parseBadgeNotificationPayload()`:

```diff
 raw.startsWith("log#", ignoreCase = true) -> {
     val filename = raw.removePrefix("log#").trim()
     if (filename.isBlank()) BadgeNotification.Unknown(raw)
     else BadgeNotification.RecordingReady(filename)
 }
+raw.startsWith("rec#", ignoreCase = true) -> {
+    val token = raw.removePrefix("rec#").trim()
+    if (token.isBlank()) BadgeNotification.Unknown(raw)
+    else BadgeNotification.AudioRecordingReady(token)
+}
```

#### [MODIFY] [DeviceConnectionManagerIngressSupport.kt](file:///home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/data/connectivity/legacy/DeviceConnectionManagerIngressSupport.kt)

Handle `BadgeNotification.AudioRecordingReady` → emit to new `runtime.audioRecordingReadyEvents` flow. Add `toBadgeAudioFilename()` converter: `rec_YYYYMMDD_HHMMSS.wav`.

#### [MODIFY] [RecordingNotification.kt](file:///home/cslh-frank/main_app/data/connectivity/src/main/java/com/smartsales/prism/domain/connectivity/RecordingNotification.kt)

Add `AudioRecordingReady(filename: String)` variant to domain sealed class.

#### [MODIFY] [ConnectivityBridge.kt](file:///home/cslh-frank/main_app/data/connectivity/src/main/java/com/smartsales/prism/domain/connectivity/ConnectivityBridge.kt)

Add `fun audioRecordingNotifications(): Flow<RecordingNotification.AudioRecordingReady>` — parallel to existing `recordingNotifications()`.

#### [NEW] [SimBadgeAudioAutoDownloader.kt](file:///home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/data/audio/SimBadgeAudioAutoDownloader.kt)

`@Singleton` handler that:
1. Collects `connectivityBridge.audioRecordingNotifications()`
2. Creates QUEUED placeholder in drawer immediately via `storeSupport`
3. Launches background download via `connectivityBridge.downloadRecording(filename)`
4. Upgrades placeholder to READY on success, FAILED on error
5. Emits `SimBadgeSyncIslandEvent` for Dynamic Island display
6. Sub-1KB filter: discard empty/header-only WAV files (same as existing sync logic)

---

### Component 2: `/list` Reconnection Fallback

Summary: Auto-sync via `/list` when badge reconnects, catching files recorded while disconnected.

#### [MODIFY] [SimAudioDrawerViewModel.kt](file:///home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/ui/sim/SimAudioDrawerViewModel.kt)

- Observe `connectivityBridge.managerStatus` in `init {}`
- On transition from non-Ready to `BadgeManagerStatus.Ready`: fire `syncFromBadge(AUTO)` with 3s debounce
- Auto-sync failures are silent (no toast, no spinner)
- Emit outcome to Dynamic Island via `syncIslandEvents`

---

### Component 3: Audio Drawer Capsule Redesign

Summary: Replace the single-state capsule with a two-part status indicator showing connection and sync state.

**New capsule layout:**

| Element | Visual | Tap Action |
|---------|--------|------------|
| Left: connection icon | 🔗 chained icon + "徽章" | Opens connectivity manager |
| Right: sync icon | 🔄 sync icon + relative time | Triggers manual sync |

**Sync label states:**
- `"未同步"` — badge connected, never synced in this session
- `"已同步 (1s)"` / `"已同步 (1min)"` / `"已同步 (1h)"` — relative time since last successful sync
- `"正在同步..."` — sync in progress
- Badge disconnected → connection icon only, sync part hidden

#### [MODIFY] [SimAudioDrawerSupport.kt](file:///home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/ui/sim/SimAudioDrawerSupport.kt)

- Refactor `resolveSimAudioSyncLabel()` to return sync-relative-time label
- Add `lastSyncTimestamp: Instant?` parameter to label resolution
- Add `formatRelativeSyncTime(lastSync: Instant): String` utility

#### [MODIFY] [SimAudioDrawerContent.kt](file:///home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/ui/sim/SimAudioDrawerContent.kt)

- Redesign `SimAudioSmartCapsule` as two-part indicator:
  - Left section: chained icon + "徽章" (taps → `onOpenConnectivity`)
  - Right section: sync icon + relative time label (taps → `onSyncFromBadge`)
- When disconnected: show only left section with unchained/broken icon

#### [MODIFY] [SimAudioDrawerViewModel.kt](file:///home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/ui/sim/SimAudioDrawerViewModel.kt)

- Track `lastSyncTimestamp: StateFlow<Instant?>` — updated on successful sync completion
- Expose to UI for relative time rendering

---

### Component 4: Dynamic Island Sync Reflection

Summary: Show transient sync status in Dynamic Island during and after sync.

#### [MODIFY] [DynamicIsland.kt](file:///home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/ui/components/DynamicIsland.kt)

- Add `SYNC` to `DynamicIslandLane`
- Add `SYNC_IN_PROGRESS`, `SYNC_COMPLETE`, `SYNC_UP_TO_DATE` to `DynamicIslandVisualState`

#### [MODIFY] [SimShellDynamicIslandCoordinator.kt](file:///home/cslh-frank/main_app/app-core/src/main/java/com/smartsales/prism/ui/sim/SimShellDynamicIslandCoordinator.kt)

- Accept `syncEvents: SharedFlow<SimBadgeSyncIslandEvent>` parameter
- **For `rec#` auto-sync (single file):**
  - `RecFileDownloaded(filename)` → transient "已同步：rec_20260409_150000" (3s, green)
- **For manual `/list` sync (batch):**
  - `ManualSyncStarted` → transient "正在同步录音..." (3s, blue pulse)
  - `ManualSyncComplete(count)` → transient "已同步 N 条录音" (3s, green)
- **When already up-to-date (no new files):**
  - `AlreadyUpToDate` → transient "已是最新" (3s, default)

**SYNC lane conflict resolution (three-tier priority model):**

```
Priority:  CONNECTIVITY (persistent) > CONNECTIVITY (transient) > SYNC (transient) > SCHEDULER (rotation)
```

| Scenario | Behavior |
|----------|----------|
| SYNC during active CONNECTIVITY (persistent, e.g. RECONNECTING) | SYNC is silently dropped — connectivity is critical |
| SYNC during active CONNECTIVITY (transient, e.g. "Badge 已连接" 5s) | SYNC is deferred — stored as `deferredSyncEvent`, revealed when connectivity transient expires |
| CONNECTIVITY fires during active SYNC | SYNC is preempted — connectivity takes over immediately |
| Multiple SYNC events in quick succession | Latest-wins — second event replaces first, timer resets |
| SYNC during takeover suppression (drawer open) | Deferred — revealed when suppression lifts (same as connectivity deferred pattern) |

**Typical reconnection timeline (no conflict):**

```
t=0s   BadgeManagerStatus → Ready
t=0s   CONNECTIVITY: "Badge 已连接" (5s transient)
t=3s   auto-sync /list starts (3s debounce fires)
t=5s   CONNECTIVITY transient expires → scheduler resumes
t=5.5s auto-sync /list returns → SYNC: "已同步 N 条" or "已是最新" (3s transient)
t=8.5s SYNC transient expires → scheduler resumes
```

The 3s debounce on auto-sync + network round-trip means the sync result naturally arrives after the connectivity transient ends — **no conflict in the common case**.

---

### Component 5: Spec/Doc Updates

#### [MODIFY] [esp32-protocol.md](file:///home/cslh-frank/main_app/docs/specs/esp32-protocol.md)

Add `rec#YYYYMMDD_HHMMSS` command documentation alongside `log#`.

#### [MODIFY] [audio-management spec.md](file:///home/cslh-frank/main_app/docs/cerb/audio-management/spec.md)

Document auto-sync-on-connection rule + `rec#` auto-download behavior.

---

## Verification Plan

### Automated Tests

```bash
# All affected test suites
./gradlew :app-core:testFullDebugUnitTest --tests "com.smartsales.prism.data.audio.SimAudioRepositorySyncSupportTest"
./gradlew :app-core:testFullDebugUnitTest --tests "com.smartsales.prism.ui.sim.SimBadgeSyncAvailabilityTest"
./gradlew :app-core:testFullDebugUnitTest --tests "com.smartsales.prism.ui.sim.SimShellDynamicIslandCoordinatorTest"
```

**New test cases:**
1. `parseBadgeNotificationPayload("rec#20260409_150000")` → `AudioRecordingReady("20260409_150000")`
2. `rec# notification → placeholder created immediately with QUEUED status`
3. `rec# download completes → placeholder upgraded to READY`
4. `rec# download sub-1KB → placeholder removed`
5. `managerStatus Ready transition → AUTO sync fires once with 3s debounce`
6. `SyncStarted → SYNC lane visible 3s → yields to scheduler`
7. `SyncComplete → SYNC lane visible 3s → yields to scheduler`

### Manual Verification
1. Connect badge → Dynamic Island: "Badge 已连接" → `/list` auto-sync fires → "已同步 N 条录音" or "已是最新" if nothing new
2. Record on badge while connected → `rec#` fires → card appears in drawer → Dynamic Island shows "已同步：rec_filename"
3. Record on badge while disconnected → reconnect → `/list` auto-sync catches the file
4. Pull up grip while connected → manual sync fires
5. Capsule shows two-part indicator: 🔗"徽章" + 🔄"已同步 (1min)" with relative timestamp
6. After first connect before any sync → capsule sync section shows "未同步"
