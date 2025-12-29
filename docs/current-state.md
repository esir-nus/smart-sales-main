---
# NON-NORMATIVE STATUS NOTE (Read Carefully)

This file is **non-normative**: it is a status/context snapshot and does **not** define requirements.
**Source of Truth (SoT)**: `docs/Orchestrator-V1.md` (CURRENT) + V1 schema/examples.
If any statement here conflicts with SoT, **ignore this file** and follow SoT.
Agents: treat this file as “facts observed” only; do not implement “should/might” statements from it.
---

<!-- File path: docs/current-state.md -->
<!-- Purpose: Summary of Smart Sales App current state -->
<!-- Goal: Align the team on layers, dependencies, risks, and next steps -->
<!-- Related files: agent.md, plans/dev_plan.md, plans/tdd-plan.md, docs/progress-log.md -->

# Current State Overview

This file is the single trusted snapshot. When any module README or major flow changes, update this file and trigger the logging workflow.

## System snapshot
- **Architecture spec**: `docs/Orchestrator-V1.md` is the only current spec; V7 is archived for historical reference.
- **JSON definitions**: `docs/orchestrator-v1.schema.json` (schema), `docs/orchestrator-v1.examples.json` (examples).
- **Overall layers**: `:core:util` and `:core:test` remain T0; `:feature:connectivity` and `:feature:chat` core paths are T1; `:feature:media` is between T0->T1 (DeviceManager, AudioFiles can drive media server + Tingwu pipeline, but still depend on Fake data); `:data:ai-core` is T1- (DashScope/Tingwu/OSS are connected, but lack full end-to-end integration tests); `:app` is a T0+ navigation shell (main experiences run, but no automated build verification).
- **End-to-end paths**:
  - **Connectivity path**: `DefaultDeviceConnectionManager` + `AndroidBleWifiProvisioner` + `GattBleGateway` support scan, provisioning, network diagnostics. WiFi/BLE Tester page can trigger all commands. Real BLE still needs more device validation.
  - **Chat path**: `HomeScreenRoute` -> `HomeScreenViewModel` -> `ChatController`. Default is Fake AI; `AiCoreConfig` can switch to DashScope/Qwen. Room `AiSessionRepository` caches history; new UI test covers scroll-to-bottom button.
  - **Media path**: DeviceManager screen reuses `MediaServerClient` to manage upload/delete/apply; AudioFiles screen uses `AudioFilesViewModel` to fetch media server list, download locally, upload to OSS and call Tingwu. States: Idle/Syncing/Transcribing/Transcribed/Error.
  - **Tingwu/OSS**: `RealTingwuCoordinator` handles submit + polling; `RealOssUploadClient` uploads and returns 900s presigned URL; AudioFiles flow connects both and shows transcription summary.
  - **App shell**: `AiFeatureTestActivity` integrates Home, WiFi/BLE, DeviceManager, DeviceSetup, Audio Library, user center placeholder tabs; `:app` remains the primary debug entry.
- **Test coverage**: Connectivity/DeviceManager have JVM tests; new `AudioFilesViewModelTest` + Compose UI tests (AudioFiles, DeviceManager, Home scroll-to-bottom). Due to environment limits, Gradle wrapper must be cached under `.gradle/wrapper` to run instrumentation.

## Dependencies and tooling
- **JDK/Gradle/AGP**: JDK 17.0.11+9 (pinned in `gradle.properties`), Gradle 8.13, AGP 8.13.0, Kotlin 1.9.25, Compose Compiler 1.5.15.
- **Compose/Material**: Compose BOM 2024.09.02, Material3 1.3.1, Compose Icons core/extended. Instrumentation uses `androidx.compose.ui:ui-test-junit4` and `ui-test-manifest` (also in `:feature:chat`).
- **DI/DB**: Hilt 2.52 (including `hilt-navigation-compose`), Room 2.6.1 (chat history).
- **Network/SDK**: DashScope SDK 2.14.0, Retrofit/OkHttp 4.12.0, Aliyun OSS SDK. `settings.gradle.kts` prefers Aliyun mirrors, fallback to `third_party/maven-repo` when missing.
- **Build constraints**: Gradle Wrapper download must be adapted to intranet/proxy; `org.gradle.jvmargs=-Xmx4g`, parallel/caching enabled. For offline usage, sync `.gradle/wrapper/dists` first.

## AI and cloud integrations
- **DashScope/Qwen**: `DashscopeAiChatService` calls SDK via `DashscopeClient`, supports streaming Flow (`AiChatStreamEvent`), failures throw `AiCoreException(source=DASH_SCOPE)`. UI uses `AiCoreConfig` to control Fake/real, retries, timeout, and streaming.
- **Tingwu**: `RealTingwuCoordinator` uses Retrofit + ROA signing interceptor, poll interval 2s, timeout 90s; `AiCoreConfig.preferFakeTingwu` can switch to Fake. AudioFiles integrates Tingwu job observer, state maps to Syncing/Transcribing/Transcribed/Error and syncs Markdown summary.
- **OSS upload**: `RealOssUploadClient` validates AccessKey/Bucket/Endpoint in `local.properties`, uploads and returns presigned URL. If credentials missing, throws `AiCoreException(source=OSS, reason=MISSING_CREDENTIALS)`.
- **Logs/errors**: Cloud modules return `AiCoreException`; UI reads `userFacingMessage`. Logcat tags: `SmartSalesAi`, `SmartSalesMedia`, etc.

## Module status matrix
| Module | Tier | Highlights | Limitations |
| --- | --- | --- | --- |
| `:core:util` | T0 | Provides Result, DispatcherProvider. | Lacks logging/time/file tools. |
| `:core:test` | T0 | `FakeDispatcherProvider` for unit tests. | No other fakes; hard to cover complex flows. |
| `:feature:connectivity` | T1 | State machine + WiFi/BLE flows with unit tests; AndroidBleScanner/BleGateway configurable. | Real hardware/permission flows not fully verified. |
| `:feature:chat` | T1- | ChatController + Room history + Home scroll-to-bottom UI test; clipboard/Markdown logic unified. | Still mostly Fake AI; DashScope streaming not integrated in UI. |
| `:feature:media` | T0->T1 | DeviceManager + AudioFiles screens integrate MediaServer, OSS, Tingwu; AudioFiles provides playback controller interface. | baseUrl requires manual input; media sync lacks automated tests and device validation. |
| `:data:ai-core` | T1- | DashScope/Tingwu/OSS real implementations wrapped; Fake switch via AiCoreConfig. | Lacks traffic governance/metrics; docs do not cover AudioFiles scenario. |
| `:app` | T0+ | AiFeatureTestActivity integrates pages and adds AudioFilesRoute. | Gradle wrapper download constrained; navigation lacks Compose tests. |

## Known risks
- Gradle Wrapper cannot download automatically in restricted networks; `.gradle/wrapper` must be cached or proxied or all `./gradlew` commands will fail.
- MediaServer base URL in AudioFiles/DeviceManager still depends on manual input; not integrated with Connectivity auto-discovery, easy to misconfigure.
- DashScope/Tingwu/OSS production credentials are in `local.properties`; missing or leaked keys will block real flows.
- Real BLE/Wi-Fi devices are not yet in routine regression; current implementation may diverge from actual device protocol.
- `:feature:media` playback uses `MediaPlayer` and does not handle lifecycle/AudioFocus; background playback may be killed by OS.

## Best practices
- Before changing a module, read its README and this file to align layer and test strategy.
- Use existing tags (`SmartSalesAi`, etc.) for logs; avoid adding new ones casually.
- Any PR touching DashScope/Tingwu/OSS must document required `local.properties` keys and how to switch Fake.
- For connectivity/media/AI critical paths, follow `plans/tdd-plan.md`: add tests where possible; if external deps block tests, record in `docs/progress-log.md`.
- Before build/test, ensure local `.gradle/wrapper` is available or cached via mirror for Gradle 8.13.

## Next steps
1. **Media path**: Let AudioFiles/DeviceManager consume `DeviceConnectionManager` network discovery, remove manual baseUrl; add UI hints and automation tests for OSS/Tingwu flow.
2. **Real AI integration**: Enable DashScope streaming on Home; ensure clipboard/export logic still works with real responses; add corresponding unit tests.
3. **BLE device validation**: Align with hardware team on BT311 or agreed device; improve scan permission/errors/logging; record protocol debugging in `docs/progress-log.md`.
4. **Build/test stability**: Cache Gradle Wrapper, restore `./gradlew :app:assembleDebug`, `:feature:media:testDebugUnitTest`, `:app:connectedDebugAndroidTest`, etc. for CI.
5. **Docs sync**: Module READMEs must stay in sync with this snapshot and logging workflow must record doc updates.

## V1.1 target architecture notes (Planned / Not yet implemented)

The following items are already defined in `docs/Orchestrator-V1.md` for V1.1. This section only helps coding agents align future refactors:

- **Chat dual-channel output**: AI Chatter outputs `<visible2user>` (HumanDraft) + `MachineArtifact` (structured); UI renders only `PublishedChatTurn.displayMarkdown`.
- **Artifact validation + retry**: If MachineArtifact fails parse/validation, Publisher runs `maxRetries=2` retry; if exhausted, `artifactStatus=FAILED` and no Metadata Hub writes.
- **Metadata Hub layers**: M1/M2/M2B/M3/M4 layered storage (recommended separate JSON); LLM Parser writes metadata only and must not modify transcript truth.
- **Audio identity split**: `audioAssetId` (content identity) separated from `disectorPlanId`/`batchAssetId` (plan identity) for dedupe and cache hits.
