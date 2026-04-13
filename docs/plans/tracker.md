# Smart Sales Prism Tracker

> **Purpose**: Central project history and active epic tracking.
> **Operational Law**: No task is added to the Active Epic without a compiler error, a failing test, or a direct user command. The roadmap is adaptive but driven by evidence, not hallucination.
> **Campaign Lifecycle**: Every major initiative (rewrite, refactor, UI polish, large fix) is an "Epic" or "Campaign". Every Campaign MUST be initialized using the `/campaign-planner` workflow to enforce the following checklist sequence:
> 1. **Docs** (Ensure Specs exist) -> 2. **Interface Map** (Ensure Layer/Contract boundaries align) -> 3. **Plan** (Dev Planner) -> 4. **Execute** (Implementation) -> 5. **Test** (E2E/L2 Verification). 
> **Master Guide Alignment**: The Master Guide acts as the overarching strategy doc for a campaign. Agents MUST NEVER auto-update the Master Guide without strict explicit human review (like a Review Conference) to prevent architectural hallucination drift. Instead, run `/04-doc-sync` at the *end* of a campaign.
> **Last Updated**: 2026-04-13
> **Work Classification**: `shared-contract` = shared product docs/contracts · `android-beta` = current Android/AOSP beta-maintenance line · `harmony-native` = future native Harmony delivery · `cross-platform-governance` = branch/review/ownership guardrails

---

## Active Governance Campaign: Android Beta Freeze and Harmony-Native Split
> **Context**: Direct user-approved governance campaign to freeze the current Android/AOSP line as beta-maintenance while opening a clean native HarmonyOS forward lane without forking shared product truth.

- **Status**: Governance slice started on 2026-04-04; docs/control-plane and local repo guardrails landed first; the first live Harmony root now includes a transient `tingwu-container` app under `platforms/harmony/tingwu-container/` with a native ArkTS runtime slice for document-picker import, Harmony-local file persistence, OSS upload, Tingwu polling, artifact reopen, and auto-generated Harmony credentials sourced from repo-root `local.properties`; the Harmony config seam is now hardened so `hvigorfile.ts` remains the build-owned generator, tracked ArkTS imports route through a stable runtime config entrypoint, and missing generated local config falls back to explicit missing-key state instead of module-resolution failure; a native Harmony north-star framework doc now locks the repo posture to translation-first, doc-first native rewriting rather than Compose-structure porting or shared-JS-first re-platforming; broader full-capability Harmony app work remains deferred; Harmony tracking is now in Stage 2, with `docs/plans/harmony-tracker.md` acting as the program-summary tracker and `docs/plans/harmony-ui-translation-tracker.md` acting as the page-by-page ArkUI tracker; a dedicated internal `ui-verification` Harmony package now exists beside the backend mini-lab root so native page rewrites and later on-device UI checks can proceed without widening the public Tingwu container capability boundary; Harmony scheduler backend-first phase 1 remains an operator-only Path A mini-lab for Uni-A exact create and Uni-B vague create rather than a fake UI-parity rollout; lane-first dirty-tree prevention is now formalized as a harness with `ops/lane-registry.json`, `scripts/lane_guard.py`, shared `.githooks/`, and CI branch/path validation so mixed work is blocked before landing
- **Primary Law**:
  - [`docs/specs/platform-governance.md`](../specs/platform-governance.md)
- **Dirty-Tree Quarantine Ledger**:
  - [`docs/plans/dirty-tree-quarantine.md`](./dirty-tree-quarantine.md)
- **Dirty-Tree Audit**:
  - [`docs/reports/20260404-dirty-tree-quarantine-audit.md`](../reports/20260404-dirty-tree-quarantine-audit.md)
- **Platform Target Reference**:
  - [`docs/reference/platform-targets.md`](../reference/platform-targets.md)
- **Current Compatibility Reference**:
  - [`docs/reference/harmonyos-platform-guide.md`](../reference/harmonyos-platform-guide.md)
- **Current Overlay Root**:
  - [`docs/platforms/README.md`](../platforms/README.md)
- **Harmony Native Framework**:
  - [`docs/platforms/harmony/native-development-framework.md`](../platforms/harmony/native-development-framework.md)
- **Transient Harmony Container Overlay**:
  - [`docs/platforms/harmony/tingwu-container.md`](../platforms/harmony/tingwu-container.md)
- **Tracker Governance SOP**:
  - [`docs/sops/tracker-governance.md`](../sops/tracker-governance.md)
- **Lane Worktree Governance SOP**:
  - [`docs/sops/lane-worktree-governance.md`](../sops/lane-worktree-governance.md)
- **Lane Registry**:
  - [`ops/lane-registry.json`](../../ops/lane-registry.json)
- **Harmony Native Tracker**:
  - [`docs/plans/harmony-tracker.md`](./harmony-tracker.md)
- **Harmony UI Translation Tracker**:
  - [`docs/plans/harmony-ui-translation-tracker.md`](./harmony-ui-translation-tracker.md)
- **Harmony UI Verification Overlay**:
  - [`docs/platforms/harmony/ui-verification.md`](../platforms/harmony/ui-verification.md)
- **Harmony Scheduler Backend Phase 1**:
  - [`docs/platforms/harmony/scheduler-backend-first.md`](../platforms/harmony/scheduler-backend-first.md)
- **Harmony Scheduler Backend Brief**:
  - [`docs/plans/harmony-scheduler-backend-phase1-brief.md`](./harmony-scheduler-backend-phase1-brief.md)
- **Work Classification Rule**:
  - shared product flows, domain rules, scheduler semantics, and interface ownership stay `shared-contract`
  - current Kotlin/Gradle app lineage work is `android-beta`
  - native Harmony delivery work is `harmony-native`
  - branch, CODEOWNERS, CI, tracker, and ownership-law work is `cross-platform-governance`
- **Branch Restore Snapshot**:
  - active Harmony branch: `harmony/tingwu-container`
  - branch purpose: transient Harmony-native Tingwu container for quick restore and bounded delivery
  - capability class: `curated-container`
  - baseline commit or tag: `401772ab`
  - current head snapshot at tracker sync: `adbecd0a`
  - restore procedure reference: [`docs/plans/harmony-tracker.md`](./harmony-tracker.md) and [`docs/specs/platform-governance.md`](../specs/platform-governance.md)
  - current restore confidence: manual git restore is possible now; CI-backed Harmony restore remains deferred
- **Immediate Objectives**:
  - keep exactly one canonical protected trunk regardless of whether its branch name is `main` or `master`
  - freeze the current Android/AOSP line to beta-maintenance scope
  - quarantine the current mixed dirty worktree into bounded lanes before any promotion/tagging step
  - make one dirty lane equal one write scope and one CERB-style build state, with a handoff when paused or transferred
  - require every active dirty lane to carry explicit doc-code alignment state so no code stays ahead of docs and no shipped/current doc stays ahead of code
  - keep native Harmony artifacts out of `app/**`, `app-core/**`, `core/**`, `data/**`, and `domain/**`
  - keep native Harmony implementation translation-first and doc-first, rewriting platform code natively instead of porting Kotlin Compose structure or inventing a shared JS UI runtime by default
  - allow the transient Harmony Tingwu container to live only in the Harmony-owned root while it stays explicit about hiding scheduler, reminder, chat, and badge-hardware capability
  - keep Harmony Tingwu credentials/config local to the Harmony root by generating a Harmony-owned local config artifact from repo-root `local.properties` instead of inheriting Android `BuildConfig`
  - route platform-specific delivery differences into overlay docs instead of duplicating the full shared docs tree
  - keep Stage 2 Harmony tracking explicit: `docs/plans/harmony-tracker.md` owns program-summary state while `docs/plans/harmony-ui-translation-tracker.md` owns page-pass evidence; a separate Harmony dataflow tracker stays deferred until backend rewriting genuinely needs it
  - defer `release/harmony-alpha` until Harmony delivery moves beyond the transient Tingwu container and its first CI path is alive

---

## Active Structural Cleanup Campaign: God-File Trunk Cleanup
> **Context**: Direct user-requested refactor/rewrite campaign to clean the shared UI and ViewModel trunk before further major prototype transplants land into the current god files.

- **Status**: Wave 3C accepted; Wave 3D accepted; Wave 3E accepted; Wave E1 landed on 2026-03-31 with focused verification blocked by an unrelated `SimRealtimeSpeechRecognizer.kt` compile error
- **Primary Tracker**:
  - [`docs/plans/god-tracker.md`](./god-tracker.md)
- **Wave 0 Execution Brief**:
  - [`docs/plans/god-wave0-execution-brief.md`](./god-wave0-execution-brief.md)
- **Wave 1A Execution Brief**:
  - [`docs/plans/god-wave1a-execution-brief.md`](./god-wave1a-execution-brief.md)
- **Wave 1B Execution Brief**:
  - [`docs/plans/god-wave1b-execution-brief.md`](./god-wave1b-execution-brief.md)
- **Wave 1C Execution Brief**:
  - [`docs/plans/god-wave1c-execution-brief.md`](./god-wave1c-execution-brief.md)
- **Wave 2 Execution Brief**:
  - [`docs/plans/god-wave2-execution-brief.md`](./god-wave2-execution-brief.md)
- **Wave 2A Execution Brief**:
  - [`docs/plans/god-wave2a-execution-brief.md`](./god-wave2a-execution-brief.md)
- **Wave 2B Execution Brief**:
  - [`docs/plans/god-wave2b-execution-brief.md`](./god-wave2b-execution-brief.md)
- **Wave 2C Execution Brief**:
  - [`docs/plans/god-wave2c-execution-brief.md`](./god-wave2c-execution-brief.md)
- **Wave 3A Execution Brief**:
  - [`docs/plans/god-wave3a-execution-brief.md`](./god-wave3a-execution-brief.md)
- **Wave 3B Execution Brief**:
  - [`docs/plans/god-wave3b-execution-brief.md`](./god-wave3b-execution-brief.md)
- **Wave 3C Execution Brief**:
  - [`docs/plans/god-wave3c-execution-brief.md`](./god-wave3c-execution-brief.md)
- **Wave 3D Execution Brief**:
  - [`docs/plans/god-wave3d-execution-brief.md`](./god-wave3d-execution-brief.md)
- **Wave 3E Execution Brief**:
  - [`docs/plans/god-wave3e-execution-brief.md`](./god-wave3e-execution-brief.md)
- **Wave E1 Execution Brief**:
  - [`docs/plans/god-wavee1-onboarding-structure-execution-brief.md`](./god-wavee1-onboarding-structure-execution-brief.md)
- **Wave 1A L1 Validation**:
  - [`docs/reports/tests/L1-20260324-god-wave1a-guardrails.md`](../reports/tests/L1-20260324-god-wave1a-guardrails.md)
- **Wave 1B L1 Validation**:
  - [`docs/reports/tests/L1-20260324-god-wave1b-agent-intelligence.md`](../reports/tests/L1-20260324-god-wave1b-agent-intelligence.md)
- **Wave 1C L1 Validation**:
  - [`docs/reports/tests/L1-20260324-god-wave1c-sim-shell.md`](../reports/tests/L1-20260324-god-wave1c-sim-shell.md)
- **Wave 2A L1 Validation**:
  - [`docs/reports/tests/L1-20260324-god-wave2a-scheduler-linter.md`](../reports/tests/L1-20260324-god-wave2a-scheduler-linter.md)
- **Wave 2B L1 Validation**:
  - [`docs/reports/tests/L1-20260324-god-wave2b-connectivity.md`](../reports/tests/L1-20260324-god-wave2b-connectivity.md)
- **Wave 2C L1 Validation**:
  - [`docs/reports/tests/L1-20260324-god-wave2c-sim-audio-repository.md`](../reports/tests/L1-20260324-god-wave2c-sim-audio-repository.md)
- **Wave 3A L1 Validation**:
  - [`docs/reports/tests/L1-20260324-god-wave3a-sim-audio-drawer.md`](../reports/tests/L1-20260324-god-wave3a-sim-audio-drawer.md)
- **Wave 3B L1 Validation**:
  - [`docs/reports/tests/L1-20260331-god-wave3b-agent-shell.md`](../reports/tests/L1-20260331-god-wave3b-agent-shell.md)
- **Wave 3C L1 Validation**:
  - [`docs/reports/tests/L1-20260331-god-wave3c-agent-vm.md`](../reports/tests/L1-20260331-god-wave3c-agent-vm.md)
- **Wave 3D L1 Validation**:
  - [`docs/reports/tests/L1-20260331-god-wave3d-scheduler-viewmodel.md`](../reports/tests/L1-20260331-god-wave3d-scheduler-viewmodel.md)
- **Wave 3E L1 Validation**:
  - [`docs/reports/tests/L1-20260331-god-wave3e-sim-agent-vm-voice-draft.md`](../reports/tests/L1-20260331-god-wave3e-sim-agent-vm-voice-draft.md)
- **Wave E1 L1 Validation**:
  - [`docs/reports/tests/L1-20260331-god-wavee1-onboarding-structure.md`](../reports/tests/L1-20260331-god-wavee1-onboarding-structure.md)
- **Governance Split**:
  - UI SOP stack remains the primary UI-building workflow
  - Cerb / cerb-ui remain the feature-local UI truth
  - [`docs/specs/code-structure-contract.md`](../specs/code-structure-contract.md) now owns the long-lived anti-god-file structure law, while `god-tracker.md` carries the active campaign memory
- **Immediate Wave Scope**:
  - `OnboardingScreen.kt`
- **Current Objective**:
  - speed up prototype-to-Kotlin transplant by making the target structure easier for humans and agents to understand
  - keep accepted Wave 1 / Wave 2 trunk reductions enforced by guardrails
  - reopen only stable UI trunks for focused host/content/component cleanup when the user explicitly approves that scope
  - keep Wave 3C accepted with `AgentViewModel.kt` reduced to a thin compatibility host plus stable support owners
  - keep Wave 3D accepted with `SchedulerViewModel.kt` reduced to a thin compatibility host plus stable projection, legacy-action, and audio-ingress owners
  - keep Wave 3E accepted with `SimAgentViewModel.kt` reduced to a thin compatibility host plus stable session/chat/follow-up/voice-draft owners
  - keep Wave E1 landed with `OnboardingScreen.kt` reduced to a thin host/static-screen seam plus stable onboarding-owned frame, intro, pairing, and support files

---

## Active UI Slice: Dynamic Island Header Decoupling
> **Context**: Direct user-requested shell UI slice to formalize and deliver a Cerb-compliant Dynamic Island.

- **Status**: Shipped
- **Owning Shard**:
  - [`docs/cerb-ui/dynamic-island/spec.md`](../cerb-ui/dynamic-island/spec.md)
  - [`docs/cerb-ui/dynamic-island/interface.md`](../cerb-ui/dynamic-island/interface.md)
- **Current Scope**:
  - standard top-header center slot only
  - sticky one-line shell summary with scheduler as the default lane
  - scheduler-first copy using `冲突：...` / `最近：...` plus RuntimeShell-local connectivity takeover for approved transport states
  - truncate overflow rather than horizontal scrolling
  - visible-lane tap opens scheduler or connectivity entry as appropriate, while downward drag remains scheduler-only
  - SIM RuntimeShell may rotate scheduler items locally and may run the approved connectivity interrupt/heartbeat timing without broadening the shared header contract
  - real bridge-backed battery sourcing remains deferred debt

---

## Active Investigation: Base Runtime Unification
> **Context**: Direct user-requested investigation to eliminate SIM/full implementation drift by defining one canonical base runtime and treating Mono as the later architecture augmentation layer.

- **Status**: Investigation package delivered on 2026-03-31; Slice 1 wrapper cleanup accepted on 2026-03-31; Slice 2 wrapper cleanup accepted on 2026-03-31; Slice 3 wrapper cleanup accepted on 2026-03-31; Slice 4 wrapper cleanup accepted on 2026-03-31; Slice 5 truth lock accepted on 2026-03-31; final root/shell unification landed on 2026-04-01
- **North Star**:
  - [`docs/specs/base-runtime-unification.md`](../specs/base-runtime-unification.md)
- **Audit**:
  - [`docs/reports/20260331-base-runtime-unification-drift-audit.md`](../reports/20260331-base-runtime-unification-drift-audit.md)
- **Campaign Plan**:
  - [`docs/plans/base-runtime-unification-campaign.md`](./base-runtime-unification-campaign.md)
- **Truth-Lock Brief**:
  - [`docs/plans/base-runtime-truth-lock-execution-brief.md`](./base-runtime-truth-lock-execution-brief.md)
- **Truth-Lock Validation**:
  - [`docs/reports/tests/L1-20260331-base-runtime-truth-lock.md`](../reports/tests/L1-20260331-base-runtime-truth-lock.md)
- **Canonical Direction**:
  - one shared base runtime for shell/UI/UX, Tingwu/audio, Path A scheduler, and bounded local/session continuity
  - Mono later adds Kernel/session-memory, CRM/entity loading, Path B enrichment, plugin/tool runtime, and related deeper intelligence
  - non-Mono work must no longer fork into SIM truth vs full truth
- **Organic Structural Cleanup Rule**:
  - this campaign may trigger targeted god-file teardown only when a legacy seam mixes shared UI truth, base-runtime logic, Mono-only logic, or wrapper glue
  - current audit candidates were `AgentShell.kt`, `AgentViewModel.kt`, and `SchedulerViewModel.kt`; Slice 4 later also reopened the former SIM-side exception path in `SimAgentViewModel.kt`
  - `OnboardingScreen.kt` remains deferred unless later implementation proves it is a direct blocker
- **Current Implementation Landing**:
  - Slice 1 now keeps `AgentShell.kt` as a thin compatibility host and moves rendering/state/reducer/support ownership into `AgentShellContent.kt`, `AgentShellState.kt`, `AgentShellReducer.kt`, and `AgentShellSupport.kt`
  - this slice is intentionally wrapper-only and does not yet decide any full-vs-SIM behavior-by-behavior convergence
  - Slice 2 now keeps `SchedulerViewModel.kt` as a thin compatibility host and moves timeline projection, legacy action helpers, and scheduler-drawer audio ingress ownership into `SchedulerViewModelProjectionSupport.kt`, `SchedulerViewModelLegacyActions.kt`, and `SchedulerViewModelAudioIngressCoordinator.kt`
  - Slice 3 now keeps `AgentViewModel.kt` as a thin compatibility host and moves UI bridge, session lifecycle, pipeline dispatch, tool dispatch, runtime/dashboard support, and debug scenario ownership into `AgentUiBridge.kt`, `AgentSessionCoordinator.kt`, `AgentPipelineCoordinator.kt`, `AgentToolCoordinator.kt`, `AgentRuntimeSupport.kt`, and `AgentDebugSupport.kt`
  - Slice 4 now keeps `SimAgentViewModel.kt` as a thin compatibility host and moves the SIM voice-draft lane into `SimAgentVoiceDraftCoordinator.kt`, while extending the existing `SimAgentUiBridge` just enough to let the host-owned state remain source-compatible
  - Slice 5 now locks the repo's non-Mono planning truth in docs: current SIM-led shell/scheduler/audio docs stay the best available base-runtime baseline, separate SIM runtime boundaries remain lawful implementation seams, and Mono stays the only lawful deeper divergence layer
  - On **2026-04-01**, production root ownership was collapsed to one launcher and one shell host: `MainActivity` now mounts `RuntimeShell`, the shared onboarding/discoverability gates now read and write the former split-era flags so existing users keep their completion state, `AlarmDismissReceiver` now re-enters through `MainActivity`, and split-era production hosts `AgentMainActivity`, `SimMainActivity`, `AgentShell`, and `SimShell` are retired
  - On **2026-03-31**, live onboarding logcat proved the failing branch was realtime `recognizer_cancelled` before finger release rather than a slow timeout; the shared handshake now surfaces active-hold failures immediately, treats the later release as a no-op, and clears post-release failures through calm retry UI instead of synthetic content
  - On **2026-03-31**, both hosts now share one simplified onboarding interaction engine with dedicated onboarding model profiles, a `5s` post-capture recognition watchdog plus `2.5s` / `3.5s` generation watchdogs, stale-result invalidation, and no debug-vs-release fallback split
  - On **2026-03-31**, follow-up device proof showed the remaining onboarding blocker was an invented backend-token auth seam rather than the mic hold state machine; the shared realtime lane now uses direct `DASHSCOPE_API_KEY` SDK init and preserves typed realtime-auth diagnosis across direct-key preflight, SDK start, and session failure logs without reintroducing fallback branches
  - On **2026-04-01**, auth-drift closure landed: `docs/cerb/sim-audio-chat/**` now owns the active SIM auth truth, SIM shell docs defer to the shared realtime recognizer contract instead of restating auth architecture, historical Wave 14 notes are marked superseded, and fresh updated-build device repro confirmed no `auth_fetch_*` / `DASHSCOPE_AUTH_BASE_URL` path while SIM composer plus SIM-connectivity onboarding both reached direct-key realtime starts. Acceptance report: [`docs/reports/tests/L3-20260401-realtime-asr-auth-drift-closure.md`](../reports/tests/L3-20260401-realtime-asr-auth-drift-closure.md)
  - On **2026-04-02**, `RuntimeShell` landed the approved SIM dynamic-island transplant: scheduler remains the default lane, connectivity may interrupt or heartbeat in the center slot from transport-truth `connectionState`, visible tap follows the current lane, downward drag stays scheduler-only, and real battery sourcing remains deferred behind the current mock/viewmodel value
  - On **2026-04-03**, production onboarding was unified onto one canonical route: `VOICE_HANDSHAKE_PROFILE` still flows directly into `HARDWARE_WAKE`, successful `PROVISIONING` now always hands into the real voice-driven `SCHEDULER_QUICK_START` sandbox, the global top-right `跳过` was removed, local `跳过，直接体验日程` actions now live only on `HARDWARE_WAKE` and the Wi-Fi entry / Wi-Fi recovery surfaces, quick start now reuses scheduler Path A extraction seams plus a sandbox-only reschedule resolver, organic exact-alarm plus calendar permission prompts are owned inside the page, the existing `COMPLETE` screen remains in place, successful finalization now arms a one-shot shell handoff that auto-opens the real scheduler drawer once in home, exact quick-start items may mirror into system calendar only after final commit when permission was granted, quick-start writes roll back on later failure so the completion commit stays all-or-nothing, and connectivity setup replay / forced first-launch setup now also complete back into home instead of entering connectivity manager first
  - On **2026-04-03**, onboarding quick-start follow-up hardening landed for the first production polish pass: calendar-permission success copy inside the page is now transient rather than persistent completion text, staged task rows now give title readability priority on compact widths, and chained same-day explicit-clock utterances such as `明天早上7点叫我起床，9点要带合同去见老板` are recovered as exact staged items before any `Uni-B` vague terminal branch can leak into the user-facing onboarding result
  - On **2026-04-03**, onboarding quick-start reminder guidance was aligned with the live scheduler contract: the first successful staged exact item now reuses the shared `ReminderReliabilityAdvisor` modal inside onboarding, notification-disabled devices surface the app-notification-settings branch before exact-alarm/OEM follow-up, and quick start no longer jumps straight into an exact-alarm-only handoff
  - On **2026-04-03**, OEM reminder hardening follow-up was doc-locked: `docs/plans/oem-alarm-hardening-plan.md` now tracks Android 14+ full-screen-intent recovery, Xiaomi / HyperOS background-popup coverage, and Huawei / Honor locked-screen validation follow-up; `docs/sops/oem-alarm-notification-control-plane.md` now owns the broader OEM management matrix while `docs/sops/oem-alarm-notification-checklist.md` remains the fast operator checklist
  - On **2026-04-03**, onboarding mic control switched to one shared two-phase tap contract across consultation, profile, and quick start: first tap starts listening, second tap ends capture and submits
  - On **2026-04-03**, L3 device rerun proved the onboarding recognizer-timeout recovery no longer leaves `SCHEDULER_QUICK_START` with a dead mic button: after the earlier profile-lane timeout, fresh quick-start attempts reached realtime start, deterministic chained-day-clock create, `Uni-M` timeout -> later-lane fallback, and staged-item reschedule/update on device. Acceptance report: [`docs/reports/tests/L3-20260403-onboarding-quick-start-session-recovery.md`](../reports/tests/L3-20260403-onboarding-quick-start-session-recovery.md). Remaining gap: this sandbox still lacks dedicated `VALVE_PROTOCOL` checkpoints and the full outer `10s` quick-start watchdog branch remains unverified at L3.
  - On **2026-04-04**, the shared-surface cleanup pass locked the approved single-runtime v2 rule in code and active docs: `AgentIntelligenceScreen` and `SchedulerDrawer` no longer default to legacy wrapper owners, shell-owned SIM/base-runtime adjunct state now flows explicitly from `RuntimeShell`, `SimAgentViewModel` / `SimSchedulerViewModel` are the current canonical base-runtime owners under the one production shell, and `AgentViewModel` / `SchedulerViewModel` remain documented wrapper debt only.
  - On **2026-04-04**, focused L3 device acceptance for the unified shell slice remained conditionally accepted after the follow-up branch pass: cold launch and resumed-activity evidence stayed on `MainActivity`, the completed onboarding gate entered the shared home shell directly, scheduler entry/exit remained inside `RuntimeShell`, audio open/select/reselect stayed drawer-based instead of escaping to Android file management, forced onboarding launch/back-block was reproduced on device, a later organic onboarding rerun kept `MainActivity` foregrounded and proved the one-shot scheduler auto-open handoff with the scheduler drawer visible while the handoff gate consumed back to `false`, and the connectivity `NeedsSetup` island takeover now shows `Badge 需要配网`, routes visible-lane tap into connectivity entry, and keeps downward drag scheduler-only. Acceptance report: [`docs/reports/tests/L3-20260404-single-runtime-shell-acceptance.md`](../reports/tests/L3-20260404-single-runtime-shell-acceptance.md). Remaining gap: a cleaner direct scheduler-vs-audio drawer-conflict repro remains unverified at L3.
  - Deferred UX polish note on **2026-04-01**: `VOICE_HANDSHAKE_PROFILE` currently remains a one-shot extract-and-save onboarding step. Future polish should allow an in-step voice `补充或修改` path after the first extraction card appears, keep the current card visible, merge each follow-up pass into the current draft instead of replacing it wholesale, and allow repeated follow-up passes until `保存并继续`. Manual field editing inside onboarding remains deferred beyond that future slice.
  - focused Wave 3D reruns now pass after restoring the extracted scheduler audio-status strings to the evidence-owned legacy full-side values
  - focused Wave 3E reruns now pass with `SimAgentViewModel.kt` back under the transitional ViewModel budget and with the accepted Wave 14 SIM voice-draft behavior preserved

---

## Active Audit: Doc Control Plane and Drift Cleanup
> **Context**: Direct user-approved docs-first audit to reduce doc-reading overhead, replace deprecated SIM Cerb ownership, and classify current doc/code drift against the latest base-runtime direction.

- **Status**: Audit package delivered on 2026-03-31; Wave A clean-docs foundation landed on 2026-03-31; Wave D4 architecture posture cleanup landed on 2026-03-31; Wave E1 onboarding structure cleanup landed on 2026-03-31; Wave F1 historical-report wording cleanup landed on 2026-03-31; one-off follow-up backlog is closed and ongoing governance remains under `DOC-001`
- **Primary Ledger**:
  - [`docs/plans/doc-tracker.md`](./doc-tracker.md)
- **Audit Report**:
  - [`docs/reports/20260331-doc-code-drift-audit.md`](../reports/20260331-doc-code-drift-audit.md)
- **Replacement Map**:
  - [`docs/reports/20260331-sim-cerb-replacement-map.md`](../reports/20260331-sim-cerb-replacement-map.md)
- **Backlog**:
  - [`docs/plans/doc-code-drift-backlog.md`](./doc-code-drift-backlog.md)
- **Current Objective**:
  - make one lightweight doc control plane the first-stop read for humans and agents
  - stop deprecated `docs/cerb/sim-*` shards from acting as active non-Mono truth
  - repair active doc-vs-doc and doc-vs-code conflicts around shell, Tingwu/audio, onboarding, and Mono posture
  - stage code cleanup only after authority docs stop conflicting

---


## Planned Mission: SIM Standalone Prototype
> **Context**: Direct user-requested standalone prototype mission. This work is intentionally separate from the current agent app and must not contaminate the live agent runtime by default.

- **Status**: Wave 1 Accepted / Wave 2 Negative-Branch L3 Accepted / Wave 4 Scheduler Accepted / Wave 5 Connectivity Accepted / Wave 6 Isolation Accepted / Wave 7 Feature Acceptance Accepted / Wave 7 Isolation Acceptance Accepted / Wave 7 Closeout Synced / Wave 8 Task-Scoped Scheduler Follow-Up Accepted / Wave 9 Physical-Badge E2E Blocked / Wave 10 Badge Ingress Repair In Progress / Wave 11 General Chat Pivot L1 Accepted / Wave 12 Scheduler-Drawer Voice Reschedule L1 Accepted / Wave 13 Launcher-Core Theme Visibility L1 Accepted / Wave 14 Voice-Draft Composer L1 Verified
- **Historical Origin Docs**: [`docs/archive/to-cerb/sim-standalone-prototype/concept.md`](../archive/to-cerb/sim-standalone-prototype/concept.md), [`docs/archive/to-cerb/sim-standalone-prototype/mental-model.md`](../archive/to-cerb/sim-standalone-prototype/mental-model.md)
- **Historical Mission Record**: [`docs/archive/plans-completed/sim-tracker.md`](../archive/plans-completed/sim-tracker.md)
- **Current Active Truth**:
  - [`docs/specs/base-runtime-unification.md`](../specs/base-runtime-unification.md)
  - [`docs/core-flow/sim-shell-routing-flow.md`](../core-flow/sim-shell-routing-flow.md)
  - [`docs/core-flow/sim-scheduler-path-a-flow.md`](../core-flow/sim-scheduler-path-a-flow.md)
  - [`docs/core-flow/sim-audio-artifact-chat-flow.md`](../core-flow/sim-audio-artifact-chat-flow.md)
  - [`docs/cerb/interface-map.md`](../cerb/interface-map.md)
- **Evidence Rule**: acceptance/audit links below are historical evidence only; current product truth for this mission remains the `Current Active Truth` docs above.
- **Connectivity Bug Tracker**: [`docs/plans/bug-tracker.md`](./bug-tracker.md)
- **Implementation Brief**: [`docs/plans/sim_implementation_brief.md`](./sim_implementation_brief.md)
- **Wave 1 Execution Brief**: [`docs/plans/sim-wave1-execution-brief.md`](./sim-wave1-execution-brief.md)
- **Wave 2 Execution Brief**: [`docs/plans/sim-wave2-execution-brief.md`](./sim-wave2-execution-brief.md)
- **Wave 4 Execution Brief**: [`docs/plans/sim-wave4-execution-brief.md`](./sim-wave4-execution-brief.md)
- **Wave 5 Execution Brief**: [`docs/plans/sim-wave5-execution-brief.md`](./sim-wave5-execution-brief.md)
- **Wave 8 Execution Brief**: [`docs/plans/sim-wave8-execution-brief.md`](./sim-wave8-execution-brief.md)
- **Wave 9 Execution Brief**: [`docs/plans/sim-wave9-execution-brief.md`](./sim-wave9-execution-brief.md)
- **Wave 10 Execution Brief**: [`docs/plans/sim-wave10-execution-brief.md`](./sim-wave10-execution-brief.md)
- **Wave 12 Execution Brief**: [`docs/plans/sim-wave12-execution-brief.md`](./sim-wave12-execution-brief.md)
- **Wave 13 Execution Brief**: [`docs/plans/sim-wave13-execution-brief.md`](./sim-wave13-execution-brief.md)
- **Wave 14 Execution Brief**: [`docs/plans/sim-wave14-voice-draft-execution-brief.md`](./sim-wave14-voice-draft-execution-brief.md)
- **Wave 12 L1 Validation**: [`docs/reports/tests/L1-20260323-sim-wave12-scheduler-drawer-reschedule.md`](../reports/tests/L1-20260323-sim-wave12-scheduler-drawer-reschedule.md)
- **Wave 13 L1 Validation**: [`docs/reports/tests/L1-20260326-sim-wave13-launcher-core-theme-validation.md`](../reports/tests/L1-20260326-sim-wave13-launcher-core-theme-validation.md)
- **Wave 14 L1 Validation**: [`docs/reports/tests/L1-20260328-sim-wave14-voice-draft.md`](../reports/tests/L1-20260328-sim-wave14-voice-draft.md)
- **Current Scheduler Routing Contract**: follow-up reschedule now resolves targets through a global scheduler-owned contract before time execution; the current utterance must restate an explicit target plus a new exact time, selected/opened task state and visible page/date no longer carry semantic authority, create-time `keyPerson` / `location` hints are now persisted for later matching, the extractor now receives a scheduler-owned active shortlist derived from all non-done tasks rather than a 7-day UI window, omitted-target and delta-only reschedule phrases safe-fail, and the write-disabled V2 shadow contract still gathers time-semantics parity/mismatch telemetry only
- **2026-04-03 Scheduler Intelligence Overhaul**: the shared core routing stack is now wired through `SchedulerIntelligenceRouter` instead of being trapped in SIM-only coordinators. `IntentOrchestrator` voice routing now owns deterministic create, `Uni-M` batch create, vague create, and global reschedule through the same core router; `RealUnifiedPipeline` `PATH_B_TEXT` now uses that same shared scheduler router before the legacy `UnifiedMutation` JSON fallback; later-lane scheduler suppression is implemented through `SchedulerTerminalCommit`; multi-task follow-up without a selected task now reaches global target clarification / safe-fail instead of failing at selection-first gating. The shared exact-create anchor set now also accepts qualified next-week weekday cues such as `下周三早上八点`, and SIM scheduler drawer terminal failures keep scheduler-owned copy even when later classifiers describe the input as a schedulable reminder/commitment. Focused verification: `./gradlew :domain:scheduler:test --tests 'com.smartsales.prism.domain.scheduler.ExactTimeCueResolverTest'`, `./gradlew :core:pipeline:testDebugUnitTest --tests 'com.smartsales.core.pipeline.SchedulerIntelligenceRouterTest' --tests 'com.smartsales.core.pipeline.IntentOrchestratorTest'`, and `./gradlew :app-core:testDebugUnitTest --tests 'com.smartsales.prism.ui.sim.SimSchedulerViewModelTest'`.
- **2026-03-31 Audio Drawer Sync Correction**: approved doc+code alignment now restores spec-owned manual drawer sync in SIM audio drawer, removes the earlier browse-open auto-sync experiment, and ships badge-pipeline completion ingest into the SIM drawer namespace. Successful ingest now gates badge remote delete so failed ingest preserves manual-recovery path. Focused verification: `:app-core:compileDebugUnitTestKotlin`; targeted audio/SIM suites passed, while unrelated `L2DualEngineBridgeTest` still fails on a stale Uni-A timestamp expectation outside this slice.
- **2026-04-04 Audio Drawer Gesture/Gate Follow-Up**: the SIM browse grip now measures its manual-sync trigger against raw upward pull while keeping the rubber-band drawer translation visual-only, fixing the dead-motion branch where the old reduced travel could never cross the real threshold on device. The READY manual-sync lane now also runs the strict badge-readiness precheck inside the drawer-owned gate before repository sync begins, so unavailable transport fails through the existing blocked path instead of looking like an inert gesture. Focused verification: `./gradlew :app-core:testDebugUnitTest --tests "com.smartsales.prism.ui.sim.SimAudioDrawerViewModelTest" --tests "com.smartsales.prism.ui.sim.SimBadgeSyncAvailabilityTest" --tests "com.smartsales.prism.data.audio.SimAudioRepositorySyncSupportTest" --tests "com.smartsales.prism.ui.sim.SimAudioDrawerStructureTest"` and `./gradlew :app-core:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.smartsales.prism.ui.sim.SimDrawerGestureTest`.
- **2026-04-01 Seed Inventory Cleanup**: fresh installs now keep only one built-in demo audio entry (`SIM_Wave2_Seed.mp3`) instead of the old multi-item pending test list, and startup reconciliation prunes the retired built-in pending sample IDs so upgraded installs do not keep the long seeded drawer inventory by default.
- **Wave 1 Acceptance**: [`docs/reports/tests/L3-20260319-sim-wave1-shell-acceptance.md`](../reports/tests/L3-20260319-sim-wave1-shell-acceptance.md)
- **Wave 4 Acceptance**: [`docs/reports/tests/L3-20260320-sim-wave4-scheduler-validation.md`](../reports/tests/L3-20260320-sim-wave4-scheduler-validation.md)
- **Wave 5 Acceptance**: [`docs/reports/tests/L3-20260321-sim-wave5-connectivity-validation.md`](../reports/tests/L3-20260321-sim-wave5-connectivity-validation.md)
- **Wave 5 Connectivity-Absent Acceptance**: [`docs/reports/tests/L3-20260321-sim-wave5-connectivity-absent-validation.md`](../reports/tests/L3-20260321-sim-wave5-connectivity-absent-validation.md)
- **Wave 5 UX Cleanup Smoke**: [`docs/reports/tests/L3-20260321-sim-wave5-ux-cleanup-smoke.md`](../reports/tests/L3-20260321-sim-wave5-ux-cleanup-smoke.md)
- **Wave 6 Isolation Validation**: [`docs/reports/tests/L1-20260321-sim-wave6-isolation-validation.md`](../reports/tests/L1-20260321-sim-wave6-isolation-validation.md)
- **Wave 7 Audio Chat Validation**: [`docs/reports/tests/L3-20260322-sim-wave7-audio-chat-validation.md`](../reports/tests/L3-20260322-sim-wave7-audio-chat-validation.md)
- **Wave 7 Feature Acceptance**: [`docs/reports/tests/L3-20260322-sim-wave7-feature-acceptance.md`](../reports/tests/L3-20260322-sim-wave7-feature-acceptance.md)
- **Wave 7 Isolation Acceptance**: [`docs/reports/tests/L3-20260322-sim-wave7-isolation-acceptance.md`](../reports/tests/L3-20260322-sim-wave7-isolation-acceptance.md)
- **Wave 8 Follow-Up Validation**: [`docs/reports/tests/L3-20260322-sim-wave8-follow-up-validation.md`](../reports/tests/L3-20260322-sim-wave8-follow-up-validation.md)
- **Wave 9 Hardware Validation**: [`docs/reports/tests/L3-20260322-sim-wave9-hardware-validation.md`](../reports/tests/L3-20260322-sim-wave9-hardware-validation.md)
- **Wave 11 L1 Validation**: [`docs/reports/tests/L1-20260322-sim-wave11-general-chat-pivot.md`](../reports/tests/L1-20260322-sim-wave11-general-chat-pivot.md)
- **Wave 5 Boundary Audit**: [`docs/reports/20260321-sim-wave5-boundary-audit.md`](../reports/20260321-sim-wave5-boundary-audit.md)
- **Audit Output**: [`docs/reports/20260319-sim-standalone-code-audit.md`](../reports/20260319-sim-standalone-code-audit.md)
- **Clarification Audit**: [`docs/reports/20260319-sim-clarification-evidence-audit.md`](../reports/20260319-sim-clarification-evidence-audit.md)
- **Historical SIM Shards**:
  - Deprecated SIM Cerb shards remain useful as migration memory only and must not be read as active non-Mono product truth.
  - Current authority for this mission now routes through:
    - [`docs/specs/base-runtime-unification.md`](../specs/base-runtime-unification.md)
    - [`docs/cerb/interface-map.md`](../cerb/interface-map.md)
    - [`docs/core-flow/sim-shell-routing-flow.md`](../core-flow/sim-shell-routing-flow.md)
    - [`docs/core-flow/sim-scheduler-path-a-flow.md`](../core-flow/sim-scheduler-path-a-flow.md)
    - [`docs/core-flow/sim-audio-artifact-chat-flow.md`](../core-flow/sim-audio-artifact-chat-flow.md)
- **Core Flows**:
  - [`docs/core-flow/sim-shell-routing-flow.md`](../core-flow/sim-shell-routing-flow.md)
  - [`docs/core-flow/sim-scheduler-path-a-flow.md`](../core-flow/sim-scheduler-path-a-flow.md)
  - [`docs/core-flow/sim-audio-artifact-chat-flow.md`](../core-flow/sim-audio-artifact-chat-flow.md)
- **Scope**:
  - Scheduler using Path A only
  - General SIM chat backed by persona plus user metadata plus local session history
  - Tingwu transcription via Audio Drawer with optional audio context attachment into chat
  - Connectivity retained as a decoupled support module
  - Source-led Tingwu artifacts with optional readability polishing
  - shell-owned zone/handle drawer gestures for scheduler/audio entry and dismiss parity
  - No smart-agent tooling or non-SIM memory architecture
- **Current Carry Debt**:
  - On **2026-03-31**, the repo locked a new unification direction in [`docs/specs/base-runtime-unification.md`](../specs/base-runtime-unification.md): current SIM-led shell/scheduler/audio behavior should now be read as the best available base-runtime baseline for non-Mono work, while deeper memory/entity/plugin architecture remains Mono-only. Future non-Mono work must not reintroduce separate SIM-vs-full product truth.
  - This mission tracker remains campaign memory and evidence routing. It no longer acts as the active owner for shell, scheduler, audio/chat, or connectivity behavior.
  - SIM has now accepted `T8.0` on top of the Wave 7 closeout. `docs/reports/tests/L3-20260322-sim-wave8-follow-up-validation.md` proves prompt-first ingress, in-shell prompt opening, repository-backed single-task follow-up mutation, and multi-task no-selection safe-fail on device.
  - `docs/reports/tests/L3-20260322-sim-wave9-hardware-validation.md` records the first `Wave 9` execution attempt as blocked: device/build preflight succeeded, but no real badge-origin single-task or multi-task ingress was captured in-session, so the hardware-only L3 debt remains open. Earlier accepted debt status is unchanged for reminders, connectivity, and Wave 6 isolation.
  - `Wave 10` is the explicit repair mini-wave for that blocker. Code and L1 verification now harden BLE recording ingress plus connection truth; device-level ingress proof is still required before returning to `T9.0`.
  - On **2026-03-30**, the shared app `AudioManagement` path was brought to SmartBadge delete parity with SIM for the main drawer: the first SmartBadge delete per drawer-open session now requires confirmation, and pending-delete tombstones now suppress shared-path re-import when HTTP badge cleanup has not finished yet.
  - On **2026-03-30**, SIM badge-audio delete semantics were tightened: the first SmartBadge delete per drawer-open session now requires confirmation, and pending-delete tombstones now suppress re-import when HTTP badge cleanup has not finished yet.
  - On **2026-03-31**, shared and SIM badge delete handling were hardened against legacy persisted source drift: badge-like `log_YYYYMMDD_HHMMSS.wav` entries now normalize back to `SMARTBADGE` on load, the first-delete confirmation/delete-cleanup fallback still treats them as badge-origin before rewrite completes, and focused Compose AndroidTests now cover first-delete confirm, same-session skip, and drawer-reopen reset. Manual physical-device proof is still pending.
  - On **2026-04-02**, the reconnect-path connectivity contract was simplified to match current ESP32 firmware reality: reconnect now trusts the badge's own one-credential auto-reconnect, treats BLE + usable badge IP as connected immediately, removes reconnect-time phone SSID gating/permission prompts, and reserves remembered credentials for manual repair prefill instead of automatic replay or network switching.
  - On **2026-04-13**, live L3 rerun on device `fc8ede3e` confirmed the active docs had drifted behind the intended reconnect contract. `adb logcat` evidence showed the badge returning a usable IP while reconnect still blocked on unreadable phone SSID, manual repair still allowed confirmation failure to collapse into apparent success, and SIM badge sync stayed blocked behind strict HTTP preflight after reconnect never promoted to ready. Active connectivity docs now assume phone SSID may be unreadable by default on target OEMs, require reconnect to trust BLE + usable badge IP without SSID gating, and reserve remembered credentials for explicit manual repair only. Evidence report: [`docs/reports/tests/L3-20260413-connectivity-reconnect-oem-ssid-drift.md`](../reports/tests/L3-20260413-connectivity-reconnect-oem-ssid-drift.md)
  - `Wave 11` now has docs, code, and focused L1 evidence aligned on the general-chat-first contract. Further on-device proof is optional follow-up, not a blocker against this current pivot slice.
  - `Wave 12` now has docs, code, and focused L1 evidence aligned for the scheduler-drawer voice reschedule lane. Scope remains strictly the scheduler drawer mic path; audio drawer, general SIM chat, and random sessions remain out of scope.
  - `Wave 13` now has docs, code, and focused L1 evidence aligned for launcher-core theme visibility from the default production host. Scope remains limited to `MainActivity`, the home/chat shell, header, history drawer, and settings drawer; scheduler/audio/connectivity/onboarding light-theme parity remains deferred.
  - On **2026-04-02**, the SIM/base-runtime shell also synced the approved dynamic-island follow-up: `RuntimeShell` now owns the local scheduler-vs-connectivity lane arbitration, scheduler remains the default visible lane, approved connectivity states may interrupt/heartbeat in the island, tap follows the visible lane, downward drag remains scheduler-only, and real battery sourcing stays logged as deferred debt rather than shipped truth.
  - On **2026-04-02**, the dynamic-island connectivity lane was hardened to reflect `DISCONNECTED` / `RECONNECTING` / reconnect-success `CONNECTED` immediately from transport truth, including replaying the latest suppressed transient confirmation once scheduler/connectivity overlays clear instead of waiting for the next heartbeat.
  - On **2026-04-04**, the SIM session-title polish slice landed in docs and code: untitled general sessions now auto-rename from the first real successful assistant reply, seeded transcript/audio intro copy is excluded from rename input, generic outputs such as broad industry or role labels are rejected with one retry on the next successful organic turn, the top-header Dynamic Island may rotate the renamed session title in chrome blue for `3s` alongside scheduler items while capping the visible set at `3` total items, and SIM session-title surfaces now prepend a fixed audio indicator once that session has ever included an audio recording.
  - On **2026-04-03**, reminder presentation sync landed across the shared alarm/shell path: foreground `EARLY` reminders may now surface one transient shell banner with scheduler-target routing while `DEADLINE` alarms now stack concurrent tasks inside `AlarmActivity` instead of letting `singleTop` replace the earlier card. Focused verification: `:app-core:compileDebugKotlin`, `:app-core:compileDebugUnitTestKotlin`, `:app-core:testDebugUnitTest --tests com.smartsales.prism.ui.alarm.AlarmActivityStateTest --tests com.smartsales.prism.ui.sim.SimReminderBannerSupportTest`.
  - On **2026-03-24**, focused unit verification confirmed the current global-reschedule implementation path for `SimSchedulerViewModelTest` and `SimAgentViewModelTest`; retrieval-hint-heavy cases such as people/location-led follow-up phrasing still need on-device proof.
  - On **2026-04-03**, the scheduler-intelligence overhaul closed the main architecture split between SIM and core routing, but the compatibility fallback from `RealUnifiedPipeline` into legacy JSON scheduler parsing still remains for shared-router `NotMatchedOrUnavailable` cases. Removing that fallback entirely remains follow-up debt after more production evidence is gathered.
  - On **2026-04-03**, the SIM scheduler drawer `REC` lane was hardened so terminal warning banners now normalize create-family reject text through scheduler-owned copy before UI projection; raw classifier/extractor wording no longer belongs on the visible warning banner, while the existing scheduler-owned reschedule unsupported copy remains intact.

---

## Active Tooling Slice: Connectivity Debug APK
> **Context**: Direct user-approved thin wrapper APK for the connectivity lane so BLE / Wi‑Fi / pairing / sync / delete work can move independently while the main app stays a frozen consumer.

- **Status**: Implementation landed on 2026-03-31; focused Kotlin compilation passed; install/on-device acceptance still requires `adb logcat`
- **Owning Shard**:
  - [`docs/cerb/connectivity-debug-app/spec.md`](../cerb/connectivity-debug-app/spec.md)
  - [`docs/cerb/connectivity-debug-app/interface.md`](../cerb/connectivity-debug-app/interface.md)
- **Delivered Direction**:
  - `:connectivity-debug-app` is now a separate APK with `applicationId = com.smartsales.prism.connectivitydebug`
  - the debug APK reuses `app-core` source/res/assets through source-set wiring so connectivity logic and UX remain shared truth
  - the host mounts the real connectivity modal, connectivity manager, `SIM_CONNECTIVITY` onboarding, and SIM audio drawer sync/delete lane
  - wrapper-only controls now expose soft disconnect, hard unpair, and copyable install / launch / `adb logcat` commands
  - the main app is now documented as the frozen consumer for this lane; new connectivity fixes should land in shared sources first and ship back after debug-host validation
- **Required Debug Practice**:
  - Android runtime diagnosis for this slice must use `adb logcat`; screenshots remain supporting evidence only

---


## Active Epic: The Crucible (Pipeline Validation)
> **Context**: Paused to implement the Data-Oriented OS foundational architecture, which mathematically resolved foundational User-Flow Purity Remediation bugs. Resumed to prove the architecture and fix remaining UI racing issues.

### Wave 6: User-Flow Purity Remediation (Mono Upgrade Polish)
> Prove the L3 UI handles human-centric workflows, decoupled intents, and context memory flawlessly (based on L2 User Flow Test Failures from 2026-03-12).

- [x] **T1: Disambiguation Fast-Fail Routing** (Entity resolution before LLM lock-in) - `/00-review-conference` completed.
- [x] **T2: The Async Loop** (Unbinding System II execution from Voice completion)
- [x] **T3: Secondary Currency RL Harmonization** (`HabitContext` -> `EnhancedContext` injection)
- [x] **T4: Mascot Presentation Collection** (Migrate from single-frame shimmer to sustained lifecycle collection)
- [x] **T5: Hardware Badge Delegation Constraint** (Strictly separating phone app (`badge_delegation`) vs physical badge (`crm_task`) scheduling capabilities)
- [x] **T6: CANCELLED: The Hand-Off Animation** (Visual bridging between voice ingestion and LLM execution. Cancelled due to Voice Source ambiguity — accepted as minor UX debt to preserve OS Model boundaries)
- [x] **T7: Parallel UI Skin Contract** (Extracted IAgentViewModel from AgentViewModel to decouple Layer 4 UI from Layer 3 Pipeline allowing Fake ViewModel vibe coding)
- [x] **T8: Mechanical UI Contract Verification** (Implement UiSpecAlignmentTest.kt to mechanically enforce Docs-First Protocol between interface.md and UiState)

### Wave 7: The Final Audit (Phase 3 E2E Pillar Resumption)
> System-wide E2E Device Tests for: Lightning Fast-Track, Dual-Engine Bridge, Strict Interface Integrity, Adaptive Habit Loop, Efficiency Overload, Transparent Mind. This is the capstone requirement before declaring the foundational architecture stable. Closed on 2026-03-13; tracker synced on 2026-03-18.
- [x] **T1: Lightning Fast-Track E2E**
- [x] **T2: Dual-Engine E2E**
- [x] **T3: E2E Error & Constraint Testing**
  - **Spec**: [`docs/cerb-e2e-test/specs/wave7-final-audit/spec.md`](../cerb-e2e-test/specs/wave7-final-audit/spec.md)
  - **Boundaries**: [`docs/cerb-e2e-test/specs/wave7-final-audit/boundaries.md`](../cerb-e2e-test/specs/wave7-final-audit/boundaries.md)
  - [TER: L3 Wave 7 Final Audit](file:///home/cslh-frank/main_app/docs/reports/tests/L3-20260313-wave7-final-audit.md)
  - [TER: L3 GPS Valves (World State)](file:///home/cslh-frank/main_app/docs/reports/tests/L3-20260316-gps-valves.md)

### Wave 20: Architecture Constitution Audit (5 System Core Flows)
> Objective: Audit the current codebase against the five architecture-derived system core flows and convert architectural drift into explicit lower-layer fix tasks.
> **Constitution**: [`docs/specs/Architecture.md`](../specs/Architecture.md)
> **System Core Flows**:
> - [`docs/core-flow/system-query-assembly-flow.md`](../core-flow/system-query-assembly-flow.md)
> - [`docs/core-flow/system-typed-mutation-flow.md`](../core-flow/system-typed-mutation-flow.md)
> - [`docs/core-flow/system-reinforcement-write-through-flow.md`](../core-flow/system-reinforcement-write-through-flow.md)
> - [`docs/core-flow/system-session-memory-flow.md`](../core-flow/system-session-memory-flow.md)
> - [`docs/core-flow/system-plugin-gateway-flow.md`](../core-flow/system-plugin-gateway-flow.md)
> **Audit Law**: For each lane, follow `Architecture -> System Core Flow -> Code Reality -> Valve / Telemetry Check -> Drift Report -> Spec/Fix Task`.
> **Execution Method**: This wave is evidence-first. Read code, trace ownership, inspect telemetry, and make only the smallest doc or code edits required to expose reality. Do not “fix by assumption”.

- [x] **T0: Audit Spine / Evidence Map**
  - [x] **Architecture**: Locked the constitutional clauses each audit must reuse (`One Currency`, runtime layers, RAM ownership, minor loops, typed mutation boundary, central writer rule, UI/domain decoupling, valve protocol).
  - [x] **Code Reality**: Identified the active entrypoints and owners for the current architecture path (`IntentOrchestrator`, `RealLightningRouter`, `RealContextBuilder`, `RealUnifiedPipeline`, `RealEntityWriter`, `RealHabitListener`, `ToolRegistry` / `RealToolRegistry`).
  - [x] **Telemetry**: Confirmed the live valve / GPS tracker and the implementation anchor in `PipelineValve.kt`, including current plugin telemetry gaps already marked pending.
  - [x] **Output**: Created the Wave 20 T0 evidence map: [`docs/reports/20260317-wave20-t0-architecture-evidence-map.md`](../reports/20260317-wave20-t0-architecture-evidence-map.md)

- [x] **T1: Query Lane Audit**
  - [x] **Flow**: Audited current sync/query behavior against `system-query-assembly-flow.md`.
  - [x] **Code Reality**: Inspected the phase-0 gateway, router, alias/disambiguation path, SSD fetch, RAM assembly, and LLM handoff.
  - [x] **Telemetry**: Verified the current query-lane valve anchors for `INPUT_RECEIVED`, `ROUTER_DECISION`, `ALIAS_RESOLUTION`, `SSD_GRAPH_FETCHED`, `LIVING_RAM_ASSEMBLED`, and `LLM_BRAIN_EMISSION`.
  - [x] **Drift Focus**: Confirmed drift around anti-guessing loop resume, duplicate grounding, and the missing delivered minimal/partial RAM branch.
  - [x] **Output**: Created the lane audit report with derived fix tasks: [`docs/reports/20260317-wave20-t1-query-lane-audit.md`](../reports/20260317-wave20-t1-query-lane-audit.md)

- [x] **T2: Typed Mutation Lane Audit**
  - [x] **Flow**: Audited current mutation behavior against `system-typed-mutation-flow.md`.
  - [x] **Code Reality**: Inspected structured emission, typed decode, confirmation/auto-commit seams, central writer routing, and RAM write-through.
  - [x] **Telemetry**: Verified `LLM_BRAIN_EMISSION`, `LINTER_DECODED`, `DB_WRITE_EXECUTED`, and the current UI-visible state emission anchors.
  - [x] **Drift Focus**: Confirmed strict typed decode for `UnifiedMutation`, but also confirmed drift around scheduler/tool dispatch, open-loop proposal execution, and plugin-caused write handoff.
  - [x] **Output**: Created the lane audit report with derived fix tasks: [`docs/reports/20260317-wave20-t2-mutation-lane-audit.md`](../reports/20260317-wave20-t2-mutation-lane-audit.md)

- [x] **T3: Reinforcement Write-Through Audit**
  - [x] **Flow**: Audited current RL behavior against `system-reinforcement-write-through-flow.md`.
  - [x] **Code Reality**: Inspected the background listener, observation extraction, habit repository writes, contextual-vs-global habit routing, and RAM refresh path.
  - [x] **Telemetry**: Confirmed the lane is background and write-through, but also confirmed RL-specific valve coverage remains weak.
  - [x] **Drift Focus**: Verified latest-input trigger behavior, typed extraction, and quiet no-op behavior; confirmed remaining drift around learning-packet breadth and observability.
  - [x] **Output**: Created the lane audit report with derived fix tasks: [`docs/reports/20260317-wave20-t3-reinforcement-write-through-audit.md`](../reports/20260317-wave20-t3-reinforcement-write-through-audit.md)

- [x] **T4: Session Memory Extension Audit**
  - [x] **Flow**: Audited current session-memory behavior against `system-session-memory-flow.md`.
  - [x] **Code Reality**: Inspected how recent turns are retained, admitted into RAM, replaced/evicted, and reused across clarification or follow-up threads.
  - [x] **Telemetry**: Confirmed `SESSION_MEMORY_UPDATED` is not yet delivered and that session-memory observability is materially behind the core flow.
  - [x] **Drift Focus**: Verified Kernel ownership and bounded history inside `RealContextBuilder`, but confirmed live runtime admission and clarification-resume wiring remain behind.
  - [x] **Output**: Created the lane audit report with derived fix tasks: [`docs/reports/20260317-wave20-t4-session-memory-extension-audit.md`](../reports/20260317-wave20-t4-session-memory-extension-audit.md)

- [x] **T5: Plugin Gateway / Capability SDK Audit**
  - [x] **Flow**: Audited current plugin behavior against `system-plugin-gateway-flow.md`.
  - [x] **Code Reality**: Inspected plugin dispatch, internal routing, capability/API surfaces, external call ownership, and result handoff paths.
  - [x] **Telemetry**: Confirmed dispatch telemetry exists, but capability-call and external-call telemetry are not yet delivered consistently.
  - [x] **Drift Focus**: Confirmed the current lane is a thin registry with async UI plugins, not yet a real capability SDK, and confirmed plugin-caused write handoff into the mutation lane is still absent.
  - [x] **Output**: Created the lane audit report with derived fix tasks: [`docs/reports/20260317-wave20-t5-plugin-gateway-capability-sdk-audit.md`](../reports/20260317-wave20-t5-plugin-gateway-capability-sdk-audit.md)

- [x] **T6: Cross-Lane Composition Audit**
  - [x] **Flow**: Audited the interchange points between the five system lanes rather than each lane in isolation.
  - [x] **Code Reality**: Inspected `query -> mutation`, `query -> session memory`, `RL -> session memory`, and `plugin -> mutation` handoff seams.
  - [x] **Telemetry**: Confirmed transfer-point observability remains behind, especially for session-memory admission and plugin/mutation handoff.
  - [x] **Drift Focus**: Confirmed the main composition risk is bypassed ownership at lane transfers, not missing lanes.
  - [x] **Output**: Created the final architecture-composition report with derived next-wave tasks: [`docs/reports/20260317-wave20-t6-cross-lane-composition-audit.md`](../reports/20260317-wave20-t6-cross-lane-composition-audit.md)

### Wave 21: Architecture Composition Fix Wave
> Objective: Turn the Wave 20 audit outputs into a focused repair wave ordered by architectural leverage, starting with the weakest transfer seams instead of isolated lane cleanups.
> **Entry Reports**:
> - [`docs/reports/20260317-wave20-t1-query-lane-audit.md`](../reports/20260317-wave20-t1-query-lane-audit.md)
> - [`docs/reports/20260317-wave20-t2-mutation-lane-audit.md`](../reports/20260317-wave20-t2-mutation-lane-audit.md)
> - [`docs/reports/20260317-wave20-t3-reinforcement-write-through-audit.md`](../reports/20260317-wave20-t3-reinforcement-write-through-audit.md)
> - [`docs/reports/20260317-wave20-t4-session-memory-extension-audit.md`](../reports/20260317-wave20-t4-session-memory-extension-audit.md)
> - [`docs/reports/20260317-wave20-t5-plugin-gateway-capability-sdk-audit.md`](../reports/20260317-wave20-t5-plugin-gateway-capability-sdk-audit.md)
> - [`docs/reports/20260317-wave20-t6-cross-lane-composition-audit.md`](../reports/20260317-wave20-t6-cross-lane-composition-audit.md)
> **Fix Law**: `Architecture -> System Core Flow -> Cross-Lane Ownership -> Spec / Tracker Update -> Code -> Valve Coverage -> PU`
> **Execution Order**: Repair the highest-leverage transfer seams first. Do not start plugin write re-entry implementation before the mutation lane, session-admission seam, and runtime plugin gateway foundation are clarified.

- [x] **T1: Query -> Session Memory Runtime Wiring**
  - [x] **Flow**: Made the session-memory lane real in live runtime rather than leaving it as a Kernel-only capability. New sessions now bind the history session ID back into the Kernel before turn admission continues.
  - [x] **Spec**: Updated the session-context docs so the runtime admission contract is explicit for user turns, assistant turns, clarification/disambiguation repair, and anti-guessing resume.
  - [x] **Code (Phase A)**: Routed `AgentViewModel` send/reply paths through `ContextBuilder.recordUserMessage()` / `recordAssistantMessage()` and made clarification follow-up reuse the bounded Kernel thread instead of UI-only history.
  - [x] **Telemetry**: Added `SESSION_MEMORY_UPDATED` and tagged real admission / replacement points in `RealContextBuilder`.
  - [x] **Validation (Current Evidence)**: Added live-runtime-style and clarification-resume coverage in `AgentViewModelTest`, then ran:
    - `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.AgentViewModelTest`
    - `./gradlew :core:pipeline:testDebugUnitTest --tests com.smartsales.core.pipeline.IntentOrchestratorTest`
    - `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.data.real.RealContextBuilderTest`
    - `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.domain.session.SessionAntiIllusionIntegrationTest`
  - [x] **Acceptance Follow-Up**: Resolved the runtime threading blocker by keeping Kernel ownership but moving live user/assistant turn admission onto `Dispatchers.IO` before returning to UI state updates.
  - [x] **Post-Fix Verification**: Re-ran the same four targeted checks after the threading fix:
    - `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.AgentViewModelTest`
    - `./gradlew :core:pipeline:testDebugUnitTest --tests com.smartsales.core.pipeline.IntentOrchestratorTest`
    - `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.data.real.RealContextBuilderTest`
    - `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.domain.session.SessionAntiIllusionIntegrationTest`
  - [ ] **Deferred Design Note**: clarification/disambiguation prompts currently persist as flattened text, so session reload does not restore structured candidate options. Decide later whether this belongs in T1 or a follow-up wave.

- [x] **T2: Query -> Mutation Normalization**
  - [x] **Flow**: Normalized the `query -> mutation` seam without prematurely solving the plugin SDK wave. The concrete target is now delivered as:
    profile/entity writes stay in the typed mutation lane,
    scheduler task execution becomes a typed task-command seam,
    generic plugin/workflow dispatch stays deferred to `T3` / `T4`.
  - [x] **Scope Guard**: Did **not** attempt a repo-wide plugin rewrite. `T2` only repaired the current typed-handoff split inside `UnifiedPipeline` and `IntentOrchestrator`.
  - [x] **Phase A: Spec / Ownership Decision**
    - [x] Reconciled `docs/core-flow/system-typed-mutation-flow.md` with `docs/cerb/unified-pipeline/spec.md` and `docs/cerb/unified-pipeline/interface.md`.
    - [x] Declared the branch rules explicitly:
      `profileMutations` -> typed mutation / central writer,
      scheduler create/delete/reschedule -> typed task command owned by scheduler execution,
      workflow recommendation / future plugin capability calls -> plugin lane, not mutation lane.
    - [x] Synced `docs/cerb/interface-map.md` because the ownership edge changed materially.
  - [x] **Phase B: Typed Result Normalization**
    - [x] Replaced stringly scheduler `ToolDispatch(Map<String, String>)` emissions from `RealUnifiedPipeline` with typed scheduler/task proposal results.
    - [x] Kept `ToolRecommendation` for non-executing workflow suggestions.
    - [x] Kept generic plugin execution out of this phase.
  - [x] **Phase C: Proposal / Commit Normalization**
    - [x] Replaced the old raw `PipelineResult` cache split with an explicit pending execution contract in `IntentOrchestrator`.
    - [x] Preserved the user-visible `"确认执行"` behavior while removing opaque scheduler execution behind generic `ToolDispatch`.
    - [x] Preserved the voice auto-commit path and made its typed handoff explicit.
  - [x] **Telemetry**
    - [x] Added clearer transfer visibility for:
      proposal cached,
      commit requested / auto-commit accepted,
      typed task command emitted,
      typed task command handed into its owning executor.
    - [x] Reused existing mutation-lane valves and added new ones only at real ownership-transfer points.
  - [x] **Validation**
    - [x] Added end-to-end coverage for cached proposal -> confirm -> `EntityWriter` commit in `IntentOrchestratorTest`.
    - [x] Preserved and revalidated voice auto-commit behavior for the profile-mutation branch through the focused orchestrator and UI-side slices.
    - [x] Added scheduler-task branch coverage proving the path is typed and no longer depends on generic `ToolDispatch(Map<...>)`.
  - [x] **Verification**
    - [x] `./gradlew :core:pipeline:testDebugUnitTest --tests com.smartsales.core.pipeline.IntentOrchestratorTest`
    - [x] `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.AgentViewModelTest --tests com.smartsales.prism.data.real.RealUnifiedPipelineTest --tests com.smartsales.prism.data.real.L2GatewayGauntletTest --tests com.smartsales.prism.data.real.L2EfficiencyOverloadTest --tests com.smartsales.prism.data.real.L2DualEngineBridgeTest`
  - [x] **Done When**
    - [x] `UnifiedPipeline` no longer emits generic `ToolDispatch` for scheduler create/delete/reschedule intents.
    - [x] `IntentOrchestrator` no longer relies on an opaque cached `PipelineResult` object as the only proposal execution contract.
    - [x] The ownership split between mutation lane, scheduler task command lane, and plugin lane is documented and tested.

- [x] **T3: Runtime Plugin Gateway / Capability SDK Foundation**
  - [x] **Flow**: Delivered the first real plugin gateway foundation as a **read-only / bounded capability lane**, not a full plugin rewrite. Runtime plugins now move through a real OS-owned gateway surface instead of silent no-op lambdas.
  - [x] **Scope Guard**: `T3` stayed smaller than `T4`.
    `T3` delivered runtime gateway wiring, permission enforcement, and one narrow capability bundle.
    `T3` did **not** introduce plugin-caused SSD writes, generic plugin result contracts, or a repo-wide SDK abstraction.
  - [x] **Phase A: Spec / Ownership Decision**
    - [x] Reconciled `docs/core-flow/system-plugin-gateway-flow.md` with `docs/cerb/plugin-registry/spec.md` and `docs/cerb/plugin-registry/interface.md`.
    - [x] Froze the first capability bundle as **real-use-case-first** and **read-only**:
      recent session/history read,
      bounded progress signaling.
    - [x] Removed the optional history-append path from the delivered bundle after the UI-boundary review.
    - [x] Explicitly documented that plugin writes remain deferred to `T4`.
    - [x] Synced `docs/cerb/interface-map.md` for the runtime gateway boundary.
  - [x] **Phase B: Runtime Gateway Foundation**
    - [x] Replaced the current no-op runtime gateway lambdas in `IntentOrchestrator` / `AgentViewModel` with a real gateway-backed implementation.
    - [x] Routed the first capability bundle through owned runtime collaborators instead of plugin-local stubs.
    - [x] Kept the surface narrow; did **not** create a generic “query anything” gateway.
    - [x] Delivered `RuntimePluginGateway` as the owned runtime adapter for bounded session-history reads and progress emission.
  - [x] **Phase C: Permission / Boundary Enforcement**
    - [x] Made `requiredPermissions` materially enforced in the live runtime path, not only in tests.
    - [x] Ensured rejected permissions fail safely through the plugin lane without corrupting the parent query state.
    - [x] Kept plugin presentation downstream of OS ownership; plugins may report progress or bounded results, but they do not become direct UI authorship seams.
  - [x] **Telemetry**
    - [x] Delivered real `PLUGIN_CAPABILITY_CALL` usage for the new gateway-backed capability bundle.
    - [x] Normalized plugin valve coverage so dispatch, internal routing, capability call, and yield/failure are all visible for the first real runtime path.
  - [x] **Validation**
    - [x] Added production-style plugin execution coverage proving:
      real runtime gateway wiring,
      permission enforcement,
      capability-call telemetry,
      safe failure on denied capability access.
    - [x] Revalidated one existing real plugin path after the gateway change so `T3` proves migration, not only a new fake seam.
  - [x] **Verification**
    - [x] `./gradlew :core:pipeline:testDebugUnitTest --tests com.smartsales.core.pipeline.RealToolRegistryTest --tests com.smartsales.core.pipeline.EchoPluginTest`
    - [x] `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.AgentViewModelTest --tests com.smartsales.prism.data.real.plugins.TestingEmailPluginTest`
  - [x] **Done When**
    - [x] The live runtime no longer passes empty / silent plugin gateways in the main path for the first delivered capability bundle.
    - [x] At least one real plugin consumes an approved gateway capability instead of relying entirely on plugin-local stub behavior.
    - [x] `requiredPermissions` are enforced in runtime code, not only in tests.
    - [x] `PLUGIN_CAPABILITY_CALL` appears in a real execution path and is covered by tests.
    - [x] Plugin-caused writes are still explicitly deferred to `T4`, so `T3` stays read-only / bounded.
  - [x] **Acceptance Cleanup**
    - [x] Narrowed the delivered T3 gateway surface so it no longer advertises `appendToHistory()` as part of the active read-only bundle.
    - [x] Added explicit plugin-lane failure/yield telemetry for denied-permission and rejected-plugin branches so safe failure is observable, not only functional.
    - [x] Re-ran the focused T3 verification slice after the cleanup:
      `./gradlew :core:pipeline:testDebugUnitTest --tests com.smartsales.core.pipeline.RealToolRegistryTest --tests com.smartsales.core.pipeline.EchoPluginTest`
      `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.AgentViewModelTest --tests com.smartsales.prism.data.real.plugins.TestingEmailPluginTest`

- [ ] **T4: Plugin -> Mutation Re-Entry Contract**
  - [ ] **Flow**: Formalize the **plugin capability framework** so future plugin development uses composable capability APIs ("Lego blocks") instead of bespoke per-plugin wiring, while still routing all real writes back into OS-owned mutation paths.
  - [ ] **Mental Model**
    - [ ] Plugin-facing surface = simple capability APIs with stable request/response contracts.
    - [ ] Behind each capability, the real owner-specific wiring logic still exists and is routed by the framework.
    - [ ] The framework simplifies and standardizes the plugin surface; it does **not** eliminate owner wiring.
    - [ ] Plugins compose approved capabilities; they do not directly wire themselves into RL, CRM, scheduler, or storage internals.
  - [ ] **Scope Guard**:
    `T4` is now a **framework wave with one proved write path**, not a pile of plugin-specific wrappers.
    This wave must:
    define the capability framework,
    define how read/action/write-request capacities are represented,
    prove one real write-causing path through that framework.
    This wave must **not**:
    grant raw repository access to plugins,
    create a generic "query anything / write anything" plugin surface,
    bypass owning OS modules for persistence.
  - [ ] **Capability Framework Shape**
    - [ ] Define a stable capability taxonomy such as:
      `READ`,
      `ACTION`,
      `WRITE_REQUEST`.
    - [ ] Define plugin-facing request/result contracts so plugins can compose capacities like Lego blocks.
    - [ ] Keep plugin outputs simplified and standardized even though each capability still routes to different owner-specific wiring behind the framework.
    - [ ] Ensure permissions and telemetry attach to the capability framework itself, not to ad-hoc plugin code.
  - [ ] **Phase A: Spec / Ownership Decision**
    - [ ] Reconcile `docs/core-flow/system-plugin-gateway-flow.md` with `docs/core-flow/system-typed-mutation-flow.md` around the capability-framework model.
    - [x] Formalized the north-star orchestration law in `docs/core-flow/system-plugin-gateway-flow.md`:
      main orchestrator owns the outer loop,
      plugins run as bounded sub-pipelines,
      and plugin completion returns control to the main workflow for the next routing/recommendation round.
    - [x] Updated `docs/cerb/plugin-registry/spec.md` and `docs/cerb/plugin-registry/interface.md` so plugin development now names the first real semantic entry lanes:
      `artifact.generate`,
      `audio.analyze`,
      `crm.sheet.generate`,
      `simulation.talk`,
      with `toolId` selecting the lane and `ruleId` specializing behavior inside that lane.
    - [ ] Continue expanding `docs/cerb/plugin-registry/spec.md` and `docs/cerb/plugin-registry/interface.md` so plugin development is described as:
      capability call -> framework routing -> owner handling -> bounded result / write request.
    - [x] Updated `docs/cerb/interface-map.md` so the plugin registry now advertises semantic entry-lane routing rather than opaque vault IDs.
    - [ ] Update `docs/cerb/interface-map.md` to show that the framework routes each capability to its owning module rather than treating plugins as first-class writers.
    - [ ] Explicitly state that plugin-facing APIs are reusable contracts, while real wiring remains owner-specific behind the framework.
  - [ ] **Phase B: Capability Contract Framework**
    - [ ] Introduce framework-level contracts such as:
      capability id,
      capability request,
      capability result,
      optional write-request result.
    - [ ] Make the framework explicit enough that the OS can distinguish:
      transient progress,
      read-only final result,
      write-causing request result.
    - [ ] Keep `Flow<UiState>` only as the presentation shell where still needed; it should no longer be the whole plugin contract story.
  - [ ] **Phase C: Runtime Routing**
    - [ ] Implement the capability routing path so one plugin can consume approved capacity APIs instead of bespoke local wiring.
    - [ ] Keep the framework open to future capacities such as client graph, budget graph, schedule window, export artifact, etc., without hard-coding T4 around those examples.
    - [ ] Prove that each capability still resolves to its corresponding owner logic behind the framework.
  - [ ] **Phase D: First Write Re-Entry Proof**
    - [ ] Prove one real write-causing capability result through the framework.
    - [ ] The plugin must emit a typed write-request result, not perform persistence directly.
    - [ ] The OS must route that write request into the owning mutation/repository path.
    - [ ] Keep the first proved plugin concrete and small, but do not let that one example define the whole framework.
  - [ ] **Telemetry**
    - [ ] Add transfer visibility for:
      capability call dispatched,
      capability routed to owner,
      write-request emitted,
      write-request handed into mutation lane,
      owning write executed,
      yield back to OS after commit.
    - [ ] Reuse existing mutation/plugin valves where they fit; add new valves only at true framework transfer points.
  - [ ] **Validation**
    - [x] **Main Orchestrator Verification Plan**
      Validate the outer-loop contract in `IntentOrchestrator` using:
      real `IntentOrchestrator`,
      real `RealToolRegistry`,
      runtime `PluginGateway`,
      and fake scenario-matched plugins rather than a fake registry shell.
    - [x] **Scenario Plugin Set**
      Add four bounded fake plugins that match the first real semantic entry lanes:
      `artifact.generate`,
      `audio.analyze`,
      `crm.sheet.generate`,
      `simulation.talk`.
    - [x] **Outer-Loop Resume Proof**
      Add focused orchestrator coverage proving:
      recommendation or dispatch enters the plugin lane,
      plugin progress/result yields back to the OS,
      and the main orchestrator remains the owner of the next conversational round.
    - [x] **Non-Voice Confirmation Proof**
      Add one orchestrator slice where a non-voice plugin dispatch is proposed,
      cached as pending execution,
      confirmed by the user,
      and then executed through the real plugin registry rather than through a generic fake shortcut.
    - [x] **Voice Auto-Commit Proof**
      Add one orchestrator slice where a voice-approved plugin dispatch auto-enters a plugin lane and still yields bounded completion evidence back to the OS/main loop.
    - [x] **Safety Branch Proof**
      Add bounded failure coverage for:
      denied permission,
      unknown tool / rejected dispatch,
      plugin-internal safe failure.
    - [x] **Per-Lane Expectations**
      Use the four scenario plugins to prove these shapes:
      `artifact.generate` -> bounded draft / artifact-request style result,
      `audio.analyze` -> bounded read-heavy analysis result,
      `crm.sheet.generate` -> structured worksheet-style result,
      `simulation.talk` -> bounded interactive coaching/simulation result.
    - [x] **Delivered Evidence**
      Added `IntentOrchestratorPluginLaneTest` plus scenario plugins under `core/test-fakes-platform`,
      updated `AgentViewModel` to surface non-voice plugin proposals and plugin execution states,
      and verified the current outer-loop contract with:
      `./gradlew :core:pipeline:testDebugUnitTest --tests com.smartsales.core.pipeline.IntentOrchestratorPluginLaneTest --tests com.smartsales.core.pipeline.RealToolRegistryTest`
      `./gradlew --no-daemon :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.AgentViewModelTest`
    - [ ] Add one framework-level integration test proving a plugin composes at least one approved capability through the new routing path.
    - [ ] Add one plugin result -> typed mutation integration test proving:
      typed write request,
      mutation-lane handoff,
      owning writer execution,
      safe halt if the write request is invalid.
    - [ ] Revalidate one real plugin path after migration so T4 proves real capability-framework usage, not a synthetic demo.
  - [ ] **Done When**
    - [ ] Plugin development is documented as capability composition, not per-plugin bespoke rewiring.
    - [ ] The framework exposes reusable capability contracts while keeping real owner wiring behind the API surface.
    - [ ] At least one real plugin consumes the new framework and yields one typed write-causing request.
    - [ ] The resulting write is still committed by an owning OS path, not by plugin code.
    - [ ] Capability routing and write re-entry are observable in telemetry.
    - [ ] Invalid capability calls or malformed write requests halt safely without mutating state.
    - [ ] T4 still does **not** grant broad CRM/entity write access directly to plugins.

- [x] **T5: RL / Session Composition Hardening**
  - [x] **Flow**: Kept the existing `RL -> session memory` seam and hardened it into a clearly bounded, observable lane after `T1` session-admission repair.
  - [x] **Scope Guard**: `T5` stayed out of RL redesign territory.
    The lane remains latest-input triggered, background, and typed.
    No broad active-RAM scrape or mutation-family rewrite was introduced.
  - [x] **Concrete Focus**
    - [x] Locked the delivered learning packet around:
      latest user input,
      bounded recent session turns,
      active entity context.
    - [x] Treated broader active RAM fields (existing habit context, schedule context, tool artifacts, document context) as explicitly deferred.
    - [x] Kept `RlPayload` as the dedicated RL payload contract for this wave.
  - [x] **Phase A: Spec / Ownership Decision**
    - [x] Reconciled `docs/core-flow/system-reinforcement-write-through-flow.md` with `docs/core-flow/system-session-memory-flow.md`.
    - [x] Updated the owning RL / habit specs to describe the concrete learning packet instead of the looser “active RAM context” wording.
    - [x] Explicitly stated that RL consumes Kernel-admitted session memory but does not own session admission or retention policy.
  - [x] **Phase B: Telemetry Hardening**
    - [x] Added RL-specific transfer visibility for:
      listener triggered,
      RL extraction emitted,
      RL payload decoded,
      habit write executed,
      RAM habit refresh applied.
    - [x] Added the new RL checkpoints in the RL lane itself and kept the generic `DB_WRITE_EXECUTED` handoff for the write boundary.
  - [x] **Phase C: Runtime Hardening**
    - [x] Kept the listener non-blocking on the main reply path.
    - [x] Ensured the learning packet is built intentionally from owned context sources instead of accidental prompt growth.
    - [x] Preserved the Section 2 / Section 3 split and write-through behavior while tightening the packet boundary.
  - [x] **Validation**
    - [x] Added an end-to-end test proving the main reply is not blocked by RL activity.
    - [x] Added a fragmented-turn RL test using real recent session context plus active entity context.
    - [x] Kept the existing empty / invalid-output no-op coverage intact.
  - [x] **Verification**
    - [x] `./gradlew :core:pipeline:testDebugUnitTest --tests com.smartsales.core.pipeline.habit.RealHabitListenerTest --tests com.smartsales.core.pipeline.habit.RlPayloadSchemaTest`
    - [x] `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.data.real.RealUnifiedPipelineTest --tests com.smartsales.prism.data.rl.RealReinforcementLearnerTest --tests com.smartsales.prism.data.real.L2WriteBackConcurrencyTest`
  - [x] **Done When**
    - [x] The RL learning packet is explicitly documented and matches the delivered code.
    - [x] RL-specific valve coverage exists for the critical background checkpoints.
    - [x] The main user-visible reply is proven not to block on RL work.
    - [x] Fragmented-turn learning is covered by a real test using bounded session context.
    - [x] T5 still does **not** expand RL into a broad active-RAM scrape or a generic mutation-lane rewrite.
  - [x] **T5 Follow-Up: Scheduler Pattern Signals For User Habits**
    - [x] **Flow**: Extended the hardened RL lane in one narrow direction so scheduler-derived pattern signals may inform **user-global** habits without widening the packet into a raw schedule-context scrape.
    - [x] **Scope Guard**:
      Scheduler signals in this follow-up are for **user habit** learning only.
      This follow-up must **not**:
      infer client/entity habits from scheduler context by default,
      pass raw `scheduleContext` text into the RL packet,
      reopen broader RAM fields like tool artifacts or document context.
    - [x] **Concrete Focus**
      - [x] Defined `schedulerPatternContext` as the narrow summarized scheduler-derived signal contract.
      - [x] Kept the signal derived from stable user scheduling tendencies only, for example:
        preferred meeting times,
        preferred durations,
        scheduling lead time,
        reschedule tendency,
        urgency style.
      - [x] Kept the existing T5 packet intact unless the new scheduler signal is explicitly added through spec and code.
    - [x] **Phase A: Spec / Ownership Decision**
      - [x] Reconciled `docs/core-flow/system-reinforcement-write-through-flow.md` with `docs/cerb/rl-module/spec.md` and `docs/cerb/session-context/spec.md` for the scheduler-pattern extension.
      - [x] Defined the ownership split as:
        scheduler-owned summary semantics,
        Kernel/session transport through `EnhancedContext`,
        RL consumption only.
      - [x] Stated clearly that scheduler-derived signals enrich **user-global** habit learning and do not silently become a general RL packet for all habit families.
    - [x] **Phase B: Runtime Packet Extension**
      - [x] Introduced the smallest runtime seam by adding `schedulerPatternContext` to `SessionWorkingSet` / `EnhancedContext` and attaching it only when present.
      - [x] Kept the RL trigger unchanged: latest user input remains the trigger, scheduler signal remains supporting context only.
      - [x] Preserved the current bounded recent-turn and active-entity packet behavior for non-scheduler learning.
    - [x] **Phase C: Telemetry / Validation**
      - [x] Added `RL_SCHEDULER_PATTERN_ATTACHED` visibility when scheduler-derived pattern context is included in the RL packet.
      - [x] Added a focused RL test proving scheduler-derived signals can influence **user-global** habit extraction.
      - [x] Added a negative-path test proving scheduler-derived signals do **not** create client/entity habit inference by default.
    - [x] **Verification**
      - [x] `./gradlew :core:pipeline:testDebugUnitTest --tests com.smartsales.core.pipeline.habit.RealHabitListenerTest --tests com.smartsales.core.pipeline.habit.RlPayloadSchemaTest`
      - [x] `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.data.real.RealContextBuilderTest --tests com.smartsales.prism.data.real.RealUnifiedPipelineTest --tests com.smartsales.prism.data.rl.RealReinforcementLearnerTest --tests com.smartsales.prism.data.real.L2WriteBackConcurrencyTest`
    - [x] **Done When**
      - [x] The scheduler-pattern extension is explicitly documented as a **user-habit-only** RL input.
      - [x] The runtime packet uses a narrow summarized scheduler signal instead of raw schedule-context text.
      - [x] RL behavior remains latest-input triggered and non-blocking.
      - [x] Tests prove both the positive user-habit path and the negative client-habit boundary.
    - [x] **Acceptance Cleanup**
      - [x] Tightened the RL ingress boundary so entity-bound observations are rejected whenever `schedulerPatternContext` is attached.
      - [x] Added the missing negative-path test proving the boundary still holds even when an active entity is present in session context.
      - [x] Re-ran the focused RL verification slice and kept the cleanup local to T5.

- [ ] **T6: Residual Post-Composition Query Cleanup**
  - [ ] **Flow**: After transfer seams are fixed, return to the Wave 20 query-lane residuals that are not purely composition issues.
  - [ ] **Spec**: Re-evaluate duplicate grounding, alias-first purity, and minimal / partial RAM fallback against the repaired composition model.
  - [ ] **Code**: Remove or narrow duplicate grounding paths that remain after `T1` and `T2`.
  - [ ] **Validation**: Add one focused query audit/fix report confirming what remains after composition repairs.

### Wave 22: Test Surface Hardening
> Objective: Convert the current testing surface from "documented and runnable" into "mechanically trustworthy", starting with the weakest anti-illusion seams exposed by the 2026-03-18 audit.
> **Entry Docs**:
> - [`docs/cerb/test-infrastructure/spec.md`](../cerb/test-infrastructure/spec.md)
> - [`docs/cerb/test-infrastructure/interface.md`](../cerb/test-infrastructure/interface.md)
> - [`docs/cerb-e2e-test/testing-protocol.md`](../cerb-e2e-test/testing-protocol.md)
> - [`docs/cerb-e2e-test/tasklist_log.md`](../cerb-e2e-test/tasklist_log.md)
> **Fix Law**: `Test SOT -> Canonical Entry -> Real Assertions -> Mock Eviction -> Acceptance Evidence`
> **Execution Order**: First make the infra tasks assert something real, then evict avoidable Mockito from L2-style tests, then close the governance and acceptance loop.

- [x] **T1: Infra Module Assertion Reality**
  - [x] **Flow**: `scripts/run-tests.sh infra` now validates real test behavior instead of only empty task wiring.
  - [x] **Spec**: Locked one concrete invariant per infra module before implementation:
    `:core:test` proves dispatcher/test-scope control,
    `:core:test-fakes-domain` proves one state-backed fake mutation/read-through invariant,
    `:core:test-fakes-platform` proves one platform fake collaboration seam without delegating the behavior to Mockito.
  - [x] **Code**: Added the smallest tests that prove those invariants mechanically, not placeholder smoke coverage.
  - [x] **Validation**: `:core:test:test`, `:core:test-fakes-domain:test`, and `:core:test-fakes-platform:testDebugUnitTest` all execute real assertions tied to those declared invariants and no longer finish as `NO-SOURCE`.

- [x] **T2: L2 Shared-Fake Migration**
  - [x] **Flow**: Removed avoidable "Testing Illusion" patterns specifically from L2 / simulated-E2E tests where repo-owned collaborators already had shared fakes.
  - [x] **Spec**: Classified remaining Mockito usage into two buckets:
    acceptable leaf seam = Android `Context` in local JVM `L2CrossOffLifecycleTest`;
    out-of-scope lower-level mocks = non-L2 tests such as `RealUnifiedPipelineTest`, `RealHabitListenerTest`, `RealAudioRepositoryBreakItTest`, and `RealOssUploaderTest`;
    anti-illusion drift inside L2 verification paths = repo-owned collaborator mocks in `L2CrossOffLifecycleTest` (now removed).
  - [x] **Code**: Rewrote the highest-signal L2 offender (`L2CrossOffLifecycleTest`) toward shared fake wiring by constructing real/fake scheduler and pipeline collaborators instead of stubbing them with Mockito.
  - [x] **Boundary**: Kept the task scoped to L2 migration; did not broaden it into a repo-wide Mockito purge.
  - [x] **Validation**: The converted L2 test still passes and proves the same behavior through state-backed fakes rather than stubbed control flow.

- [x] **T3: Canonical Runner Coverage Expansion**
  - [x] **Flow**: `scripts/run-tests.sh` now acts as a single truthful automated entrypoint rather than a thin wrapper around partial institutional memory.
  - [x] **Spec**: Defined the first-class automated slices as `all`, `infra`, `pipeline`, `scheduler`, and `l2`; kept `app` as a convenience alias rather than a first-class verification contract.
  - [x] **Code**: Extended the runner with a stable `l2` mode and explicit `all` semantics without adding vanity aliases.
  - [x] **Docs**: Synced README/spec/interface references so contributors see one command path and one slice contract.
  - [x] **Validation**: `scripts/run-tests.sh infra`, `l2`, `pipeline`, `scheduler`, and `all` all execute successfully against real documented slices, and `all` now explicitly declares itself as the curated repo-default slice rather than an exhaustive aggregate.

- [x] **T4: E2E Governance Closure**
  - [x] **Flow**: Prevent tracker/changelog/ledger drift from reappearing after future test waves ship.
  - [x] **Spec**: Clarified that `tracker.md` owns active testing-wave status, `tasklist_log.md` owns the operational evidence mirror, and `changelog.md` is historical-only.
  - [x] **Docs**: Tightened the testing protocol and ledger notes so a shipped wave cannot stay open in the tracker without being treated as unresolved drift.
  - [x] **Ship Gate**: Made tracker + ledger sync an explicit closeout requirement for future testing waves, not a best-effort follow-up.
  - [x] **Validation**: Performed a Wave 7 dry-run sync on 2026-03-18, preserving the 2026-03-13 shipment date while aligning tracker language, ledger notes, and changelog ownership without contradiction.

- [x] **T5: Acceptance Closeout**
  - [x] **Flow**: Proved the repaired testing surface works end-to-end for both infra and core automated entrypoints.
  - [x] **Validation**: Re-ran every first-class runner slice declared in `T3` during closeout: `scripts/run-tests.sh infra`, `l2`, `pipeline`, `scheduler`, and `all`, all with successful results.
  - [x] **Evidence**: Recorded the acceptance verdict in [`docs/reports/tests/20260318-wave22-t5-test-surface-hardening-acceptance.md`](../reports/tests/20260318-wave22-t5-test-surface-hardening-acceptance.md).




---

## Tech Debt (Deferred for Beta)

| Item | Location | Priority |
|------|----------|----------|
| `delay()` in UI | `ResponseBubble.kt`, `ConnectivityModal.kt`, `OnboardingScreen.kt` | Low |
| FTS4 Search | `MemoryDao.kt` — LIKE for Chinese | Medium |
| Remaining Fakes | `FakeHistoryRepository`, `FakeAudioRepository` — not Room-backed | Low |
| TOCTOU in observe() | `RoomUserHabitRepository.kt` | Low |
| Room error handling | `Room*Repository` — no try-catch on writes | Low |
| **Confidence-Based Reminder Interceptor** | Replace deterministic round-1 wrap-up with LLM confidence-based interception. Agent decides when to surface schedule context. Requires classifier or LLM self-assessment of conversation intent. Current workaround: smarter prompting that lets LLM decide naturally. | Medium |
| **Voice Hand-Off Animation** | `AgentIntelligenceScreen.kt`, `UiState.kt` — Visual bridging for voice ingestion. Deferred due to architectural ambiguity: must decide if `AgentIntelligenceScreen` mic records directly, or if it strictly observes `BadgeAudioPipeline` global states. Spec updated (`UiState.AudioProcessing`), but implementation pending source definition. | Medium |

---

## Quick Links

- [Architecture.md](../specs/Architecture.md) — RAM/SSD mental model
- [Architecture.md](../specs/Architecture.md) — The Data-Oriented OS Migration Guide (Read before any Mono tasks)
- [prism-ui-ux-contract.md](../specs/prism-ui-ux-contract.md) — (Deprecated) The Single Source of Truth for UI/UX is now exclusively the `docs/cerb/[feature]/spec.md` files.
- [interface-map.md](../cerb/interface-map.md) — Module ownership + data flow
- [legacy-to-prism-dictionary.md](../reference/legacy-to-prism-dictionary.md) — Legacy mapping

---

## Changelog

The changelog has been moved to a standalone file to prevent content explosion. 

See **[View Changelog](changelog.md)**

### Wave 19: Scheduler Fast-Track Core-Flow Completion (Universe-Driven Delivery)
> Objective: Use the Path A Core Flow as the behavioral north star and complete implementation universe-by-universe instead of as one vague backend rewrite.
> **North Star**: [docs/core-flow/scheduler-fast-track-flow.md](../core-flow/scheduler-fast-track-flow.md)
> **Foundation Contract**: [docs/cerb/scheduler-path-a-spine/spec.md](../cerb/scheduler-path-a-spine/spec.md)
> **Active T1 Contract**: [docs/cerb/scheduler-path-a-uni-a/spec.md](../cerb/scheduler-path-a-uni-a/spec.md)
> **Working T2 Contract**: [docs/cerb/scheduler-path-a-uni-b/spec.md](../cerb/scheduler-path-a-uni-b/spec.md)
> **Delivery Law**: For each universe or safety branch, follow `Flow -> Spec -> Code -> PU Test -> Fix Loop` before advancing.
> **Execution Order**: Build the shared Path A spine first, then deliver `Uni-A`, then the safety/guardrail universes, then the reschedule branches.
- [x] **T0: Shared Path A Spine**
  - [x] **Flow**: Locked the common Path A skeleton from the Core Flow (`ASR_CAPTURED -> GUID_ALLOCATED -> INTENT_CLASSIFIED -> DB_WRITE_EXECUTED/UI_RENDERED or FAST_FAIL_RETURNED`) with `IntentOrchestrator` as the single shared spine owner.
  - [x] **Spec**: Created the owning T0 Cerb shard in `docs/cerb/scheduler-path-a-spine/`, defining the shared spine contract and replacing stale implementation-contract assumptions.
  - [x] **Code**: Wired the minimum shared execution path used by all Path A universes by routing badge audio transcript scheduling through `IntentOrchestrator` and surfacing `PipelineResult.PathACommitted` as the early completion checkpoint.
  - [x] **PU Test**: Added baseline Path A assertions in targeted orchestrator and badge-audio tests so every later universe inherits one shared entry seam.
  - [x] **Fix Loop**: Repaired the ownership drift between implementation docs and the delivered single-spine behavior before universe-specific work continues.
- [x] **T1: Uni-A Specific Creation**
  - [x] **Flow**: Implemented [Uni-A](../core-flow/scheduler-fast-track-flow.md) as the first happy-path slice.
  - [x] **Spec**: Built from [docs/cerb/scheduler-path-a-uni-a/spec.md](../cerb/scheduler-path-a-uni-a/spec.md) and [docs/cerb/scheduler-path-a-uni-a/interface.md](../cerb/scheduler-path-a-uni-a/interface.md), defining lightweight semantic extraction, exact-task DTO/output shape, no-conflict persist rules, and normal timeline render expectations.
  - [x] **Plan**: Executed from [docs/plans/wave19-t1-uni-a-plan.md](wave19-t1-uni-a-plan.md), locking the narrow-contract rewrite points, ownership map, and PU gates before code.
  - [x] **Code**: Delivered exact schedulable creation through `narrow :domain extraction contract -> prompt/linter mechanical alignment -> scheduler validation -> deterministic persist`, not Kotlin heuristic parsing.
  - [x] **PU Test**: Verified exact semantic extraction yields one non-vague, non-conflict task with visible timeline render, and non-exact input does not masquerade as `Uni-A`.
  - [x] **Fix Loop**: Repaired prompt/linter seam drift and `unifiedId` persistence drift before closing T1.
  - [x] **L3 Runtime Repair**: On-device validation exposed that real exact schedulable utterances were bypassing `Uni-A` and surfacing false success copy without a persisted schedule card; the live runtime entry law and success-proof law are now repaired.
  - [x] **Flow**: Repaired the live Path A entry law so schedulable voice input can enter `Uni-A` even when the upstream router labels it `DEEP_ANALYSIS` or `SIMPLE_QA`; `Uni-A` runtime entry is now governed by schedulable semantics, not by an over-narrow router enum gate.
  - [x] **Spec**: Updated the `Uni-A` shard and the north-star Core Flow so runtime entry and exit are explicit for: `entered Uni-A`, `NotExact exit`, `conflict reject`, `fell through to Path B`, and `no user-visible success without persistence proof`.
  - [x] **Code**: Replaced the old `CRM_TASK / BADGE_DELEGATION`-only gate in `IntentOrchestrator` with a bounded live runtime gate that reaches `Uni-A` for real exact voice scheduling input, while still preventing heuristic overreach.
  - [x] **Success Proof Law**: Do not emit scheduler success copy such as `搞定` unless there is write proof:
    `PathACommitted` / `PATH_A_DB_WRITTEN` for Path A, or real scheduler persistence proof for any later fallback lane.
  - [x] **Telemetry**: Tightened runtime evidence so on-device logs can distinguish `Uni-A entered`, `Uni-A exact persisted`, `Uni-A exited NotExact`, `Uni-A rejected by conflict`, `Path B fallback invoked`, and `success copy emitted`.
  - [x] **Validation**: Re-ran L3 on-device after the repair. Delivered outcomes:
    exact create reaches `PATH_A_PARSED -> PATH_A_DB_WRITTEN -> UI_STATE_EMITTED`,
    conflicting exact input does not create a normal card,
    and no success toast/copy appears without persistence proof.
  - [x] **Anchor Law Completion**: Tightened the semantic contract so:
    `明天` / `tomorrow` anchor to real current date,
    `下一天` / `后一天` anchor to displayed scheduler page date when provided,
    and bare Chinese `一点` defaults to `13:00` while explicit `凌晨一点` resolves to `01:00`.
    The `Uni-A` request contract now carries optional `displayedDateIso`, the scheduler UI forwards the active page date, and the lightweight extraction prompt carries both anchor laws and the Chinese time-default law.
  - [x] **Anchor Law Repair: Deterministic Relative-Day Validator**
    - [x] **Problem**: On-device evidence showed the lightweight model could still mis-anchor closed-set Chinese relative-day phrases, especially `后天`, by leaking page-relative interpretation into a real-day phrase. When this wrong date landed in the past, the cross-off sweep could immediately migrate the task into completed memory, making the failure look even worse.
    - [x] **Flow**: Added a deterministic post-extraction validator/normalizer for a small closed set of relative-day phrases instead of relying purely on model obedience for anchor semantics.
    - [x] **Spec**: Freeze the closed-set law explicitly:
      `明天` / `tomorrow` / `后天` = real-date family,
      `下一天` / `后一天` = page-relative family.
      Any extracted date outside the legal family must be rejected or normalized before persistence.
    - [x] **Code**:
      classify closed-set relative-day tokens deterministically from transcript,
      compute the only legal anchor date from `nowIso` and/or `displayedDateIso`,
      rewrite mismatched model dates before they can reach Path A persistence when the phrase family is unambiguous and context exists,
      and reject only when required page context is missing.
    - [x] **Guardrail**: Keep title/time semantics in the lightweight model, but remove anchor-family choice for these closed-set phrases from model discretion.
    - [x] **Validation**:
      add focused L1 cases for:
      page on March 15, 2026 + `后天...` -> March 20, 2026 is the only valid real-date anchor when real today is March 18, 2026,
      page on March 19, 2026 + `后一天...` -> March 20, 2026,
      page on March 19, 2026 + `明天...` -> March 19, 2026 only if real today is March 18, 2026.
    - [ ] **L3**: Re-run on-device after the validator lands and confirm wrong past-date anchoring no longer produces immediate crossed-off artifacts.
  - [x] **Fix Loop: Explicit-Clock Promotion Recovery**
    - [x] **Problem**: After anchor normalization was repaired, on-device behavior could still degrade `后天晚上九点去接李总` into a vague `时间待定` card because the fallback `Uni-B` pass preserved the day but failed to hold the utterance in exact-create.
    - [x] **Flow**: Froze lawful day-anchor plus explicit clock cue as an exact-create floor, even when fallback extraction emits a vague-shaped payload.
    - [x] **Code**:
      `SchedulerLinter` now normalizes closed-set anchors in `Uni-B`,
      reconstructs exact clock time from transcript/`timeHint`,
      and promotes that payload into `CreateTasks`;
      `IntentOrchestrator` then routes the promoted result through the normal exact/conflict-visible Path A commit lane instead of persisting vague.
    - [x] **Validation**:
      added focused L1/L2 coverage in `SchedulerLinterTest` and `IntentOrchestratorTest` proving
      `后天晚上九点去接李总` resolves to March 20, 2026 exact create,
      and lawful day-anchor plus explicit clock cue no longer commits as `Uni-B`.
    - [ ] **L3**: Re-run on-device with `后天晚上九点去接李总` and confirm the app creates an exact task on March 20, 2026 instead of a vague card.
- [ ] **T2: Uni-B Vague Creation**
  - [x] **Flow (Date-Anchored Slice)**: Implemented vague / needs-time handling for date-anchored schedulable input without fabricated exact time, using the now-validated `Uni-A` path as the exact-create floor rather than widening `Uni-A` by stealth.
  - [x] **Spec (Date-Anchored Slice)**: Created a dedicated `Uni-B` Cerb shard that defines:
    date-only / part-of-day / unresolved-time inputs,
    visible vague-task persistence,
    red-flag / awaiting-time UI treatment,
    and explicit separation from `Uni-A` exact-create and `Uni-D` conflict-visible create.
  - [x] **Plan**: Executed from [docs/plans/wave19-t2-uni-b-plan.md](wave19-t2-uni-b-plan.md) with tightened rules for:
    explicit vague-success `PathACommitted`,
    explicit conflict bypass,
    and explicit vague telemetry.
  - [x] **Code (Date-Anchored Slice)**: Delivered explicit vague persistence plus red-flagged / awaiting-time UI treatment, with conflict bypass semantics where exact collision math is not applicable because the time is still unresolved.
  - [x] **PU Test (Date-Anchored Slice)**: Added one-universe validation for vague create with conflict bypass, including:
    no fabricated exact ISO time,
    a persisted vague card,
    and no false exact-success telemetry.
  - [ ] **Follow-Up Gap**: Fully undated vague inputs such as `schedule team standup` are not yet committed in this slice, because current scheduler persistence still requires a real day anchor and Core Flow forbids silently inventing one.
  - [ ] **Fix Loop**: Close the no-day vague follow-up before marking the whole `Uni-B` universe complete.
  - [ ] **Tech Debt: Crossed-Off Legacy Card Deletion**
    - Legacy checked/crossed-off scheduler cards are still sourced from `MemoryRepository` in the unified timeline instead of the active task table.
    - Current delete actions remove active `scheduled_tasks` rows, but do not remove the memory-backed crossed-off artifacts, so some checked legacy cards appear undeletable.
    - Need a dedicated ownership path and delete contract for crossed-off memory-backed scheduler items.
- [x] **T3: Uni-C Inspiration**
  - [x] **Working T3 Contract**:
    - [`docs/cerb/scheduler-path-a-uni-c/spec.md`](../cerb/scheduler-path-a-uni-c/spec.md)
    - [`docs/cerb/scheduler-path-a-uni-c/interface.md`](../cerb/scheduler-path-a-uni-c/interface.md)
  - [x] **Plan**:
    - [`docs/plans/wave19-t3-uni-c-plan.md`](wave19-t3-uni-c-plan.md)
  - [x] **Flow**: Implemented inspiration routing as a non-schedulable, non-task branch.
    In the current runtime this is a bounded `Uni-A -> Uni-B -> Uni-C` sequence inside `IntentOrchestrator`, because the router does not yet expose a dedicated `INSPIRATION` enum branch.
  - [x] **Spec**: Aligned inspiration-only persistence and display contract beneath the Path A core-flow `Uni-C` branch, with explicit foreground success via `PipelineResult.InspirationCommitted`.
  - [x] **Code**: Delivered inspiration write path isolated from the task table, including:
    narrow serializer-backed `Uni-C` extraction contract,
    prompt/linter alignment,
    inspiration-only persistence,
    and suppression of later-lane scheduler mutation after `Uni-C` acceptance.
  - [x] **PU Test**: Added one-universe validation for timeless intent -> inspiration output, including:
    zero scheduler task writes,
    no later-lane revival after `Uni-C`,
    and visible inspiration confirmation instead of scheduler success.
  - [x] **Verification**:
    - `./gradlew :domain:scheduler:test --tests com.smartsales.prism.domain.scheduler.SchedulerLinterTest`
    - `./gradlew :core:pipeline:testDebugUnitTest --tests com.smartsales.core.pipeline.IntentOrchestratorTest --tests com.smartsales.core.pipeline.UniCContractAlignmentTest --tests com.smartsales.core.pipeline.IntentOrchestratorBreakItTest --tests com.smartsales.core.pipeline.RealIntentOrchestratorTest`
    - `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.ui.drawers.scheduler.SchedulerViewModelAudioStatusTest --tests com.smartsales.prism.ui.AgentViewModelTest --tests com.smartsales.prism.data.real.L2DualEngineBridgeTest --tests com.smartsales.prism.data.real.L2CrossOffLifecycleTest --tests com.smartsales.prism.data.real.L2UserFlowTests`
  - [x] **Fix Loop**: Repaired prompt-order test drift and exact-input test fixtures so verification matches the shipped bounded-runtime behavior without weakening the Uni-C boundary.
- [x] **T4: Uni-D Conflict-Visible Create**
  - [x] **Working T4 Contract**:
    - [`docs/cerb/scheduler-path-a-uni-d/spec.md`](../cerb/scheduler-path-a-uni-d/spec.md)
    - [`docs/cerb/scheduler-path-a-uni-d/interface.md`](../cerb/scheduler-path-a-uni-d/interface.md)
  - [x] **Plan**:
    - [`docs/plans/wave19-t4-uni-d-plan.md`](wave19-t4-uni-d-plan.md)
  - [x] **Flow**: Implemented conflict-visible creation without rejecting user intent; exact overlap now commits through the shared Path A spine instead of degrading into rejection.
  - [x] **Spec**: Synced the Uni-D shard to the shipped conflict contract, including concrete persisted evidence fields (`conflictWithTaskId`, `conflictSummary`) and explicit caution-state UI law.
  - [x] **Code**: Delivered exact create with persisted conflict-visible state and caution treatment across deterministic mutation, Room mapping, scheduler timeline mapping, task-card details, and foreground scheduler status copy.
  - [x] **PU Test**: Added one-universe validation for overlap detection with successful creation across mutation, orchestrator, mapper, and scheduler status surfaces.
  - [x] **Verification**:
    - [x] `./gradlew :core:pipeline:testDebugUnitTest --tests com.smartsales.core.pipeline.IntentOrchestratorTest`
    - [x] `./gradlew :domain:scheduler:test --tests com.smartsales.prism.domain.scheduler.FastTrackMutationEngineTest`
    - [x] `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.data.persistence.DaoMappersTest --tests com.smartsales.prism.ui.drawers.scheduler.SchedulerViewModelAudioStatusTest`
  - [x] **Fix Loop**: Replaced legacy reject-on-conflict behavior with persisted conflict evidence and tightened the scheduler UI so `Uni-D` is caution-visible instead of being flattened into clean success.
  - [ ] **L3 Regression (2026-03-19)**: On-device explicit-clock exact create `后天晚上九点去接李总` anchored to March 21, 2026 correctly, but overlap still emitted `Uni-A conflict clear` and persisted as a clean exact task because the live task carried `durationMinutes=0`.
    - [TER: L3 Wave 19 Explicit-Clock Conflict Regression](../reports/tests/L3-20260319-wave19-explicit-clock-conflict-regression.md)
  - [x] **Fix Loop: Zero-Duration Exact Conflict Law**
    - [x] **Problem**: Exact tasks with lawful time anchors could still arrive at deterministic conflict evaluation with `durationMinutes=0`, causing interval-overlap math to treat them as empty spans and emit false `conflict clear`.
    - [x] **Code**: Moved the board overlap law to deterministic point-or-interval logic:
      normal durations still use interval overlap,
      zero-duration exact tasks now use point-in-time occupancy checks against exclusive slots,
      and no fake default duration is invented.
    - [x] **Verification**:
      `./gradlew :app-core:testDebugUnitTest --tests com.smartsales.prism.domain.memory.ScheduleBoardTest`
    - [ ] **Blocked Verification**:
      `./gradlew :core:pipeline:testDebugUnitTest --tests com.smartsales.core.pipeline.IntentOrchestratorTest`
      is currently blocked by unrelated compile drift in `core/pipeline/src/test/java/com/smartsales/core/pipeline/RealToolRegistryTest.kt` (`Unresolved reference: Flow`).
    - [ ] **L3**: Reinstall and rerun `后天晚上九点去接李总` to confirm the same-slot exact task now persists as `Uni-D` conflict-visible instead of clean `Uni-A`.
- [ ] **T5: Branch-S0 Null / Garbled Fast-Fail**
  - [ ] **Flow**: Implement explicit non-mutation fast-fail for empty or unusable input.
  - [ ] **Spec**: Align fast-fail wording, traceability, and non-write behavior.
  - [ ] **Code**: Deliver no-op feedback path with no task or inspiration write.
  - [ ] **PU Test**: Add one-branch validation for null / garbled input.
  - [ ] **Fix Loop**: Repair any silent-drop or mutation drift before advancing.
- [ ] **T6: Branch-S1 Reschedule Happy Path**
  - [ ] **Flow**: Implement replacement-style reschedule with lineage preservation and latest-revision result.
  - [ ] **Spec**: Align session-memory usage, replacement semantics, and follow-up parsing contract.
  - [ ] **Code**: Deliver create-new -> retire-old handling for successful reschedule follow-up.
  - [ ] **PU Test**: Add one-branch validation for successful contextual replacement.
  - [ ] **Fix Loop**: Repair any surgical-edit drift before advancing.
- [ ] **T7: Branch-S2 Reschedule No-Match**
  - [ ] **Flow**: Implement explicit safe failure when no target can be resolved.
  - [ ] **Spec**: Align target-missing branch and non-mutation feedback contract.
  - [ ] **Code**: Deliver no-match fast-fail with zero mutation.
  - [ ] **PU Test**: Add one-branch validation for target missing.
  - [ ] **Fix Loop**: Repair any accidental mutation before advancing.
- [ ] **T8: Branch-S3 Reschedule Ambiguous Match**
  - [ ] **Flow**: Implement explicit safe failure when multiple targets match.
  - [ ] **Spec**: Align target-ambiguous branch and manual-resolution feedback contract.
  - [ ] **Code**: Deliver ambiguity fast-fail with zero mutation.
  - [ ] **PU Test**: Add one-branch validation for ambiguous reschedule targeting.
  - [ ] **Fix Loop**: Repair any wrong-target mutation drift before advancing.
