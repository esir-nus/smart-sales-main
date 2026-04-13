# HUI-02 Tingwu Docking Contract

> **Purpose**: Define the page-facing state interface that connects the tingwu-container backend to the ui-verification Tingwu page through manual type projection.
> **Status**: Defined (not yet docked)
> **Last Updated**: 2026-04-12
> **Primary Laws**:
> - `docs/platforms/harmony/ui-verification.md`
> - `docs/platforms/harmony/tingwu-container.md`
> - `docs/specs/platform-governance.md`
> **Related Docs**:
> - `docs/plans/harmony-ui-translation-tracker.md` (HUI-02 row)
> - `docs/plans/harmony-tracker.md` (H-01, H-03)

---

## 1. Contract Purpose

HUI-02 is the Tingwu page-native rewrite inside the internal `ui-verification` package. Today it consumes seed data from `HarmonyUiTingwuSeedRepository`. This contract defines the exact page-facing state interface so that the seed repository can later be replaced by a projection from the real `HarmonyContainerRepository` in tingwu-container without changing the page adapter or ArkUI page.

---

## 2. Type Sharing Strategy

Manual type projection. The two Harmony roots are separate `oh-package.json5` projects with zero cross-root imports. This contract does not introduce shared modules or cross-root dependencies.

Rules:

- the ui-verification adapter snapshot types are the contract surface
- the tingwu-container backend types are implementation detail, not imported by ui-verification
- the projection logic lives in ui-verification and is owned by the page adapter lane
- when backend types change, the projection is updated; the page-facing interface stays stable

---

## 3. Source Types (tingwu-container)

These types exist in `platforms/harmony/tingwu-container/entry/src/main/ets/model/HarmonyTingwuModels.ets`. They are not imported by ui-verification. They are documented here as the projection source.

### HarmonyTingwuAudioItem (21 fields)

| Field | Type | Docking Role |
|---|---|---|
| `id` | `string` | projected to adapter item `id` |
| `displayName` | `string` | projected to adapter item `displayName` |
| `status` | `HarmonyJobStatus` | projected to adapter item `status` (mapped to string union) |
| `progressPercent` | `number` | projected to adapter item `progressPercent` |
| `summary` | `string?` | projected to adapter item `summary` |
| `transcriptPreview` | `string?` | projected to adapter item `transcriptPreview` |
| `lastErrorMessage` | `string?` | projected to adapter item `lastErrorMessage` |
| `keywords` | `string[]` | projected to adapter item `keywords` |
| `sourceUri` | `string` | not projected (import detail) |
| `localFileName` | `string` | not projected (storage detail) |
| `localPath` | `string` | not projected (storage detail) |
| `extension` | `string` | not projected (storage detail) |
| `sizeBytes` | `number` | not projected (storage detail) |
| `importedAt` | `number` | not projected (metadata) |
| `updatedAt` | `number` | not projected (metadata) |
| `objectKey` | `string?` | not projected (upload detail) |
| `publicUrl` | `string?` | not projected (upload detail) |
| `jobId` | `string?` | not projected (pipeline detail) |
| `completedAt` | `number?` | not projected (metadata) |

### HarmonyTingwuArtifacts (13 fields)

| Field | Type | Docking Role |
|---|---|---|
| `transcriptMarkdown` | `string?` | projected to adapter artifacts `hasTranscript` (presence check) |
| `smartSummary` | `HarmonySmartSummary?` | projected to adapter artifacts `hasSummary` (presence check) |
| `chapters` | `HarmonyChapter[]` | projected to adapter artifacts `hasChapters` (non-empty check) |
| `outputMp3Path` | `string?` | not projected (media detail) |
| `outputMp4Path` | `string?` | not projected (media detail) |
| `outputThumbnailPath` | `string?` | not projected (media detail) |
| `outputSpectrumPath` | `string?` | not projected (media detail) |
| `resultLinks` | `HarmonyResultLink[]` | not projected (link detail) |
| `transcriptionUrl` | `string?` | not projected (URL detail) |
| `autoChaptersUrl` | `string?` | not projected (URL detail) |
| `customPromptUrl` | `string?` | not projected (URL detail) |
| `diarizedSegments` | `HarmonyDiarizedSegment[]` | not projected (transcript detail) |
| `speakerLabels` | `Record<string, string>` | not projected (transcript detail) |

### HarmonyContainerSnapshot (6 fields)

| Field | Type | Docking Role |
|---|---|---|
| `items` | `HarmonyTingwuAudioItem[]` | projected item-by-item through field mapping above |
| `configMissingKeys` | `string[]` | projected directly |
| `isLoading` | `boolean` | projected directly |
| `isBusy` | `boolean` | projected directly |
| `busyMessage` | `string` | projected directly |
| `lastErrorMessage` | `string?` | projected directly |

---

## 4. Target Types (ui-verification)

These types are the page-facing contract surface. They exist in `platforms/harmony/ui-verification/entry/src/main/ets/model/HarmonyUiModels.ets`.

### HarmonyUiTingwuAdapterItem (8 fields)

| Field | Type | Source |
|---|---|---|
| `id` | `string` | from `HarmonyTingwuAudioItem.id` |
| `displayName` | `string` | from `HarmonyTingwuAudioItem.displayName` |
| `status` | `HarmonyUiTingwuJobStatus` | from `HarmonyTingwuAudioItem.status` (enum to string union) |
| `progressPercent` | `number` | from `HarmonyTingwuAudioItem.progressPercent` |
| `summary` | `string?` | from `HarmonyTingwuAudioItem.summary` |
| `transcriptPreview` | `string?` | from `HarmonyTingwuAudioItem.transcriptPreview` |
| `lastErrorMessage` | `string?` | from `HarmonyTingwuAudioItem.lastErrorMessage` |
| `keywords` | `string[]` | from `HarmonyTingwuAudioItem.keywords` |

### HarmonyUiTingwuAdapterArtifacts (3 fields)

| Field | Type | Source |
|---|---|---|
| `hasTranscript` | `boolean` | `true` when `HarmonyTingwuArtifacts.transcriptMarkdown` is present and non-empty |
| `hasSummary` | `boolean` | `true` when `HarmonyTingwuArtifacts.smartSummary` is present |
| `hasChapters` | `boolean` | `true` when `HarmonyTingwuArtifacts.chapters` is non-empty |

### HarmonyUiTingwuAdapterSnapshot (7 fields)

| Field | Type | Source |
|---|---|---|
| `items` | `HarmonyUiTingwuAdapterItem[]` | projected item-by-item from `HarmonyContainerSnapshot.items` |
| `artifactsById` | `Record<string, HarmonyUiTingwuAdapterArtifacts>` | projected from loaded `HarmonyTingwuArtifacts` per item (requires `loadArtifacts(id)` call per completed item) |
| `configMissingKeys` | `string[]` | from `HarmonyContainerSnapshot.configMissingKeys` |
| `isLoading` | `boolean` | from `HarmonyContainerSnapshot.isLoading` |
| `isBusy` | `boolean` | from `HarmonyContainerSnapshot.isBusy` |
| `busyMessage` | `string` | from `HarmonyContainerSnapshot.busyMessage` |
| `lastErrorMessage` | `string?` | from `HarmonyContainerSnapshot.lastErrorMessage` |

---

## 5. Docking Point

The docking swap happens inside `HarmonyUiDriver.loadTingwuState()` at:

```
platforms/harmony/ui-verification/entry/src/main/ets/services/HarmonyUiDriver.ets
```

Current flow:

```
HarmonyUiTingwuSeedRepository.loadSnapshot()
  -> HarmonyUiTingwuAdapterSnapshot
  -> HarmonyUiTingwuPageAdapter.buildState(snapshot)
  -> HarmonyUiTingwuState
  -> Index.ets renders
```

Docked flow (future):

```
HarmonyUiTingwuDockingProjection.project(containerSnapshot, artifactsMap)
  -> HarmonyUiTingwuAdapterSnapshot   (same shape as seed)
  -> HarmonyUiTingwuPageAdapter.buildState(snapshot)  (unchanged)
  -> HarmonyUiTingwuState  (unchanged)
  -> Index.ets renders  (unchanged)
```

### What changes at docking time

- a new `HarmonyUiTingwuDockingProjection` class is added to ui-verification
- `HarmonyUiDriver` calls the projection instead of the seed repository
- the projection accepts data that matches the documented source shapes and produces `HarmonyUiTingwuAdapterSnapshot`

### What does NOT change at docking time

- `HarmonyUiTingwuPageAdapter` — unchanged, still transforms adapter snapshot to page state
- `HarmonyUiModels.ets` — the page-facing types remain stable
- `Index.ets` — the ArkUI page consumes `HarmonyUiTingwuState` as before
- tingwu-container code — no modifications to the backend root

---

## 6. Artifacts Projection Detail

The `artifactsById` field requires special handling because artifacts are loaded per-item from file storage, not included in the container snapshot directly.

Current backend API:

```
HarmonyContainerRepository.loadArtifacts(audioId: string): Promise<HarmonyTingwuArtifacts | undefined>
```

Projection rule:

- for each item with `status === 'COMPLETED'`, load artifacts
- project to `HarmonyUiTingwuAdapterArtifacts` using the presence checks defined in section 4
- items without artifacts or with non-completed status get no entry in `artifactsById`

This means docking requires either:
- the projection runs in ui-verification and reads artifact files from a shared or copied location, or
- the projection is pre-computed and passed as serialized data

The mechanism is deferred to implementation. The contract only requires that the `artifactsById` shape is satisfied.

---

## 7. Prerequisites

Docking must not proceed until all are true:

1. **Signing gate**: a device-accepted install of `smartsales.HOS.ui` has been recorded in `docs/platforms/harmony/test-signing-ledger.md`
2. **Runtime config**: `HarmonyContainerRuntimeConfig` is loadable in the ui-verification context, or an equivalent config seam is wired
3. **Data access**: ui-verification can access the same file storage that tingwu-container uses for metadata and artifacts, or a data relay mechanism exists
4. **Contract stability**: the adapter snapshot shape defined in this doc has not been invalidated by backend changes

---

## 8. Acceptance Criteria

Docking is complete when:

- `HarmonyUiDriver.loadTingwuState()` produces `HarmonyUiTingwuState` from real backend data instead of seed data
- the page adapter (`HarmonyUiTingwuPageAdapter`) is unchanged
- the ArkUI page (`Index.ets` HUI-02 section) is unchanged
- items in all five statuses (IMPORTED, UPLOADING, PROCESSING, COMPLETED, FAILED) render correctly
- artifact presence tags (Transcript, Summary, Chapters) appear for completed items with real artifacts
- config-missing, loading, busy, and error container states propagate to the page summary
- the seed repository is no longer called in the docked configuration
- docs and trackers are updated to reflect docked status

---

## 9. Scope Boundary

This contract covers HUI-02 Tingwu docking only.

It does not cover:

- HUI-03 scheduler docking (deferred until scheduler backend graduates from operator-only)
- shell-management contracts (deferred until HUI-02 docking succeeds and shell evolution is evaluated)
- cross-root module sharing (manual projection is the chosen strategy)
- public capability widening (docking is an internal milestone, not a public parity claim)
