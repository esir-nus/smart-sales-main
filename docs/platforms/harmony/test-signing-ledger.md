# Harmony Test Signing Ledger

> **Role**: Record of signing milestones and device-accepted install evidence for Harmony packages
> **Status**: Active
> **Date**: 2026-04-16
> **Primary Law**: `docs/specs/platform-governance.md`

---

## Purpose

Every Harmony package must have a recorded signing milestone before it can be used for device verification or docking. This ledger is the single surface for that evidence.

---

## Signing Status

| Package | Bundle ID | Status | Last Verified | Device | Notes |
|---------|-----------|--------|---------------|--------|-------|
| Backend Mini-Lab | `smartsales.HOS.test` | Signed, device-accepted | 2026-04-11 | `4NY0225613001090` | Compile -> signing -> hdc deploy -> launch -> hilog chain verified |
| UI Verification | `smartsales.HOS.ui` | Blocked | -- | -- | pkcs7 error 9568257; AGC asset set not yet created |
| Complete Native App | `smartsales.HOS.test` | Signed, device-accepted | 2026-04-17 | `4NY0225613001090` | App root at `platforms/harmony/smartsales-app/`; bundle identity switched to policy-approved `smartsales.HOS.test`; local sign -> `hdc install` -> `aa start` -> `hilog` chain verified |

---

## Resolution Path for smartsales.HOS.ui

Current blocker: pkcs7 verification error `9568257` on device install.

Steps to resolve:

1. Create `smartsales.HOS.ui` profile in AGC (AppGallery Connect)
2. Generate valid signing certificate for the UI package
3. Configure `build-profile.json5` with signing config
4. Build signed HAP: `hvigorw assembleHap --mode module -p product=default`
5. Install on device: `hdc install <path-to-signed.hap>`
6. Verify launch: `hdc shell aa start -a EntryAbility -b smartsales.HOS.ui`
7. Record evidence below

**This is a manual/operator step requiring AGC console access.**

---

## Resolution Path for smartsales.HOS.test (Complete Native App)

The `platforms/harmony/smartsales-app/` scaffold now uses the policy-approved `smartsales.HOS.test` bundle identity. Signing work is tied to the existing `smartsales.HOS.test` AGC lane rather than a separate `com.smartsales.harmony.app` app entry.

Steps to resolve:

1. Keep `platforms/harmony/smartsales-app/AppScope/app.json5` aligned to `smartsales.HOS.test`
2. Use the `smartsales.HOS.test` debug profile / certificate lane for signing
3. Configure signing in `platforms/harmony/smartsales-app/build-profile.json5` when the lane is formalized
4. Follow same build/install/verify chain as above
5. Record evidence below

---

## Evidence Records

### smartsales.HOS.test (Backend Mini-Lab)

- **Date**: 2026-04-11
- **Device**: `4NY0225613001090`
- **Build**: local `hvigorw` build
- **Install**: `hdc install` succeeded
- **Launch**: `hdc shell aa start` succeeded
- **Logs**: `hdc shell hilog` confirmed runtime entry and telemetry events
- **Signing**: local certificate, not AGC-distributed

### smartsales.HOS.ui (UI Verification)

_Pending — blocked on AGC asset set creation._

### smartsales.HOS.test (Complete Native App)

- **Date**: 2026-04-17
- **Device**: `4NY0225613001090`
- **Build**: `cd platforms/harmony/smartsales-app && ~/.local/bin/hvigorw assembleHap --mode module -p product=default` -> `BUILD SUCCESSFUL`
- **Bundle Identity**: switched `platforms/harmony/smartsales-app/AppScope/app.json5` from `com.smartsales.harmony.app` to `smartsales.HOS.test` per policy
- **Signing Inputs**: `~/.ohos/tingwu-container-signing/agc-debug/com.smartsales.harmony.appDebug.p7b` verified as debug profile for `smartsales.HOS.test`; local keystore `~/.ohos/tingwu-container-signing/agc-debug/tingwu-agc-debug.p12`
- **Signed Artifact**: `platforms/harmony/smartsales-app/entry/build/default/outputs/default/entry-default-signed-test-profile-2102.hap`
- **Signature Check**: `hap-sign-tool verify-app` passed; extracted profile re-verified as `bundle-name: smartsales.HOS.test`
- **Install**: `hdc install -r .../entry-default-signed-test-profile-2102.hap` succeeded
- **Launch**: `hdc shell aa start -a EntryAbility -b smartsales.HOS.test` succeeded
- **Runtime**: `hdc shell ps -A` showed resident process `smartsales.HOS.test` (`pid 31138`) after launch
- **Logs**: `hdc shell hilog -x -t app -L I | rg 'smartsales\\.HOS\\.test|JSAPP|HS-010|picker'` captured app-side lines including `[HS-010][OSS] upload attempt=1 ...` and `[picker] document select selectResult ...`
- **Note**: device acceptance is now proven for the complete app variant, but HS-010 joint-by-joint failure-mode evidence is still tracked in the sprint contract before final acceptance
- **Date**: 2026-04-18
- **Device**: `4NY0225613001090`
- **L3 branch: OSS retry / terminal fail**
- **Method**: temporary local-only rebuild with invalid `OSS_ACCESS_KEY_ID` / `OSS_ACCESS_KEY_SECRET`, signed and installed as `entry-default-signed-bad-oss.hap`, then retried `Process` on the failed `3speakertestings.wav` item
- **Logs**: `hdc shell hilog -x -t app | rg 'smartsales\\.HOS\\.test|HS-010\\]\\[OSS\\]'` captured retry sequence `upload attempt=2`, `upload attempt=3`, `upload attempt=4`
- **UI terminal state**: item returned to `Needs retry` with `OSS upload network failure: [object Object]` and `Reason: Network failed`
- **Interpretation**: on-device evidence confirms retry/backoff + terminal fail behavior, but this reproduction classified as `NETWORK_FAILED`, not `AUTH_FAILED`
- **Config hygiene**: repo-root `local.properties` restored after the bad-OSS pass
- **L3 branch: clean-build submit path**
- **Method**: regenerated clean `AppConfig.local.ets`, rebuilt, signed, and reinstalled `entry-default-signed-normal-l3.hap`, then retried `Process` on the same item
- **Logs**: app `hilog` captured `[HS-010][OSS] upload attempt=1 ...` on the clean build
- **UI terminal state**: item advanced out of `Needs retry`, then returned to terminal fail with `Tingwu HTTP 400 ... Code:\"SignatureDoesNotMatch\"` and `Reason: Unknown failure`
- **Interpretation**: OSS upload path advanced, but Tingwu submit still fails on-device due to signature mismatch; this keeps the branch in `FAILED` rather than reaching a completed artifact state needed for reopen/idempotency evidence
- **L3 branch: Tingwu submit contract repair**
- **Method**: patched Harmony request signing/resource canonicalization plus submit payload drift in `entry/src/main/ets/services/TingwuService.ets` and transport/auth handling in `entry/src/main/ets/services/HttpClient.ets` + `entry/src/main/ets/services/OssService.ets`, rebuilt, re-signed, and reinstalled `entry-default-signed-normal-l3.hap`, then retried `Process` on `3speakertestings.wav`
- **Intermediate evidence**: after the canonical signing fix, the device no longer returned `SignatureDoesNotMatch`; the same retry failed with `Tingwu HTTP 400 {\"Code\":\"ClientError\",\"Message\":\"ClientError\"...}`, proving the signer drift moved and the next blocker was request-contract level rather than raw signature calculation
- **Final rerun evidence**: after aligning submit payload drift (`IdentityRecognitionEnabled: false` without hint payload, diarization switched to `OutputLevel: 1`), `hdc shell hilog -r` cleared the buffer, `Process` was tapped once, and fresh app `hilog` captured `[HS-010][OSS] upload attempt=1 key=smartsales/harmony/audio/1776502380927/audio_mo2xao6b8k3xgs.wav`; the same item then transitioned from `Processing` to `Completed` on-device
- **UI completed state**: `5:00 PM` screen capture shows `3speakertestings.wav` in the detail pane with `Re-process` and `Completed`; after cold relaunch, `5:05 PM` screen capture shows the audio card itself as `Completed · 35.3 MB · 2026-04-17 21:07` with excerpt text rendered
- **L3 branch: reopen/idempotency**
- **Method**: after completion, `hdc shell hilog -r` cleared logs, `aa force-stop smartsales.HOS.test` stopped the app, then `aa start -a EntryAbility -b smartsales.HOS.test` cold-launched it again
- **Logs**: post-relaunch `hilog` grep for `smartsales\\.HOS\\.test|HS-010|Tingwu|JSAPP|upload attempt|submit` returned no matches, i.e. zero observed resubmit/upload activity during reopen
- **Interpretation**: the Harmony Tingwu lane now has real L3 evidence for successful end-to-end completion and persisted reopen without duplicate submit; remaining open HS-010 device branches are the bad-OSS auth classification drift (`NETWORK_FAILED` vs intended `AUTH_FAILED`) and explicit poll-timeout / task-expired mini-lab evidence
- **L3 branch: AUTH_FAILED fixed**
- **Method**: rebuilt and signed a temporary bad-OSS variant with invalid `OSS_ACCESS_KEY_ID` / `OSS_ACCESS_KEY_SECRET`, relaunched `smartsales.HOS.test`, selected `3speakertestings.mp3`, tapped `Process`, then read the persisted metadata file on-device
- **Logs**: `hilog` captured exactly one `[HS-010][OSS] upload attempt=1 key=smartsales/harmony/audio/.../audio_mo2x9gw6lkcub6.mp3`; no retry loop followed
- **Persisted terminal state**: `/data/app/el2/100/base/smartsales.HOS.test/haps/entry/files/smartsales_audio_metadata.json` recorded `lastErrorReason: "AUTH_FAILED"` and `lastErrorMessage` containing OSS `InvalidAccessKeyId`
- **Interpretation**: joint 1 now closes with typed auth classification instead of the earlier false `NETWORK_FAILED`
- **L3 branch: POLL_TIMEOUT**
- **Method**: local-only rebuild with `TINGWU_POLL_TIMEOUT_MS=1000`, re-signed and reinstalled, then tapped `Re-process` on the completed WAV item
- **Logs**: `hilog` captured `[HS-010][OSS] upload attempt=1 ...` followed by `[HS-010][Tingwu] submit taskKey=...`
- **Persisted terminal state**: metadata recorded `lastErrorReason: "POLL_TIMEOUT"` and `lastErrorMessage: "Tingwu polling timed out after 1000ms ..."`; UI detail pane rendered `Reason: Polling timed out`
- **Interpretation**: joint 3a now has direct L3 timeout evidence without waiting on a 30-minute live poll
- **L3 branch: TASK_EXPIRED**
- **Method**: because live provider-side expiry was not reproducible on demand, used the sprint-allowed local-only force path `TINGWU_FORCE_TASK_EXPIRED_JOB_ID=hs010-task-expired-missing-job`; rewrote the failed MP3 metadata to `PROCESSING` with that `jobId`, cold-launched the app, and let `resumeProcessingJobs()` exercise the path
- **Logs**: `hilog` captured `[HS-010][Tingwu] forced TaskNotFound for job hs010-task-expired-missing-job`
- **Persisted terminal state**: metadata recorded `lastErrorReason: "TASK_EXPIRED"` with `lastErrorMessage: "Tingwu task expired or missing: hs010-task-expired-missing-job"`
- **Interpretation**: joint 3b closes with deterministic L3 evidence while keeping the live-code path unchanged when the override is unset
- **L3 branch: chapter parity**
- **Method**: fixed `TingwuService.parseChapters()` to read provider `AutoChapters.Headline`, rebuilt a clean variant, reprocessed `3speakertestings.wav`, then compared the fresh device artifact JSON with the provider payload embedded in `providerPayloads.autoChapters`
- **Artifact check**: persisted chapter tuple now matches exactly: `title/startMs/endMs/summary` = provider `Headline/Start*1000/End*1000/Summary`
- **Interpretation**: Harmony now matches the Android `TingwuChapter` semantic shape instead of collapsing to synthetic `Chapter 1`
- **L3 branch: legacy chapter normalization**
- **Method**: downgraded the persisted completed artifact on-device back to `chapters: ["Legacy Chapter 1"]`, cold-launched the app, and inspected the loaded detail layout
- **UI/Layout evidence**: app launched without crash; `uitest dumpLayout` captured `· Legacy Chapter 1` in the detail panel while the item remained `Completed`
- **Interpretation**: `FileStore.loadArtifacts()` normalized legacy `string[]` chapter payloads into renderable `ChapterSegment[]` at load time
