# Harmony Tingwu Container Root

This is the dedicated Harmony-owned root for the transient `tingwu-container` app.

Current role:

- host Harmony-native packaging, lifecycle, storage, and runtime files for the transient Harmony app
- keep that work isolated from the Android lineage

Current capability boundary:

- supported: local audio import/selection, Tingwu processing, transcript/artifact viewing, persisted artifact reopen
- operator-only backend verification: Harmony scheduler Path A mini lab for Uni-A exact create and Uni-B vague create, with owner-chain and Path A commit visibility
- hidden and unsupported: scheduler, reminders, follow-up mutation, Ask-AI/chat, onboarding scheduler handoff, badge pairing/sync/download

Current scaffold files:

- `AppScope/app.json5`
- `build-profile.json5`
- `hvigorfile.ts`
- `oh-package.json5`
- `entry/oh-package.json5`
- `entry/src/main/module.json5`
- `entry/src/main/ets/entryability/EntryAbility.ets`
- `entry/src/main/ets/pages/Index.ets`
- `entry/src/main/ets/config/HarmonyContainerConfig.ets`
- `entry/src/main/ets/config/HarmonyContainerRuntimeConfig.ets`
- `entry/src/main/ets/config/HarmonyContainerConfig.local.ets` (auto-generated, ignored)
- `entry/src/main/ets/model/HarmonyTingwuModels.ets`
- `entry/src/main/ets/model/HarmonySchedulerModels.ets`
- `entry/src/main/ets/services/HarmonyContainerRepository.ets`
- `entry/src/main/ets/services/HarmonySchedulerIngressCoordinator.ets`
- `entry/src/main/ets/services/HarmonySchedulerRepository.ets`
- `entry/src/main/ets/services/HarmonySchedulerStore.ets`
- `entry/src/main/ets/services/HarmonySchedulerTelemetry.ets`
- `entry/src/main/ets/services/HarmonyFileStore.ets`
- `entry/src/main/ets/services/HarmonyHttpClient.ets`
- `entry/src/main/ets/services/HarmonyOssService.ets`
- `entry/src/main/ets/services/HarmonyPickerService.ets`
- `entry/src/main/ets/services/HarmonyTingwuService.ets`
- `entry/src/main/ets/utils/HarmonyFormatUtils.ets`
- `entry/src/main/ets/utils/HarmonySignatureUtils.ets`

Current runtime behavior:

- imports one local audio file at a time through the Harmony document picker
- persists metadata as `harmony_tingwu_metadata.json`
- stores imported media as `harmony_<audioId>.<ext>`
- stores provider artifacts as `harmony_<audioId>_artifacts.json`
- owns Harmony-local OSS upload and Tingwu polling seams inside the Harmony root
- keeps an operator-only Harmony scheduler Path A mini lab with local task/trace persistence, ingress-owner visibility, owner-chain visibility, and Path A commit inspection
- treats `hvigorfile.ts` as the Harmony root build-owned generator for `entry/src/main/ets/config/HarmonyContainerConfig.local.ets`
- keeps tracked ArkTS imports stable through `entry/src/main/ets/config/HarmonyContainerRuntimeConfig.ets`, which falls back to empty config when the generated artifact is absent

Guardrail:

- do not place Harmony-native files for this app under `app/**`, `app-core/**`, `core/**`, `data/**`, or `domain/**`

Current limits:

- fresh clones and editor indexing remain safe because tracked code no longer hard-imports the ignored generated config artifact
- live Tingwu processing still depends on repo-root `local.properties` being present and the Harmony build generating `entry/src/main/ets/config/HarmonyContainerConfig.local.ets`
- OSS upload is a direct in-memory PUT path for now, so large-file hardening remains future work
- this root is still not claiming build-verified packaging, signing, or device validation
