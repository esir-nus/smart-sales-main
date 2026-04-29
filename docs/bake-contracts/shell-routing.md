---
protocol: BAKE
version: 1.0
domain: shell-routing
layer: L4 shell corridor
runtime: base-runtime-active
owner: app-core RuntimeShell, app-core SIM shell state, app-core dynamic island
core-flow-doc:
  - docs/core-flow/sim-shell-routing-flow.md
  - docs/core-flow/base-runtime-ux-surface-governance-flow.md
last-verified: 2026-04-29
---

# Shell Routing BAKE Contract

This contract records the delivered implementation for base-runtime shell
routing. The core-flow docs remain the behavioral north stars; this BAKE
contract is the implementation record and tracks delivered gaps explicitly.

## Pipeline Contract

### Inputs

- Production launch into `MainActivity -> RuntimeShell`.
- Onboarding completion state and the forced first-launch setup flag.
- Shell gestures and commands from header, composer, dynamic island, drawer
  handles, history, settings, new-session, connectivity, scheduler, and audio
  routes.
- Scheduler summaries, session-title highlights, connectivity transport state,
  battery/device labels, and drawer/connectivity suppression state for
  dynamic-island arbitration.
- Badge audio pipeline scheduler completions that may create badge scheduler
  follow-up prompts.
- Chat, history, audio, scheduler, connectivity, settings, and follow-up
  ViewModel state supplied explicitly by `RuntimeShell`.

### Outputs / Guarantees

- App launch mounts the base-runtime shell path instead of silently booting the
  retired smart shell.
- First-launch onboarding opens forced setup before ordinary shell use and exits
  to the normal home shell. A scheduler quick-start completion may auto-open the
  real scheduler drawer only through the shell-owned handoff gate.
- Scheduler, audio, history, connectivity, and settings routes stay support
  surfaces around the same shell family. Opening one major support surface
  clears or replaces incompatible surfaces.
- New-session starts a fresh discussion, closes overlays, and clears active
  badge scheduler follow-up state when appropriate.
- Connectivity entry selects the shell-owned surface for setup, manager, modal,
  or add-device while BLE, Wi-Fi, media readiness, and registry truth remain
  owned by connectivity collaborators.
- Dynamic-island arbitration defaults to scheduler, allows approved
  transport-truth connectivity interrupts/heartbeats, routes tap to the visible
  lane, and keeps downward drag scheduler-only.
- Badge-origin scheduler completion is prompt-first: the shell may bind a
  follow-up session and show a prompt/chip, but it must not force-switch chat.
- Smart-only surfaces are not normal base-runtime routes. Attempted smart-only
  route handling must leave the user on a valid shell surface.

### Invariants

- MUST keep `RuntimeShell` as the production shell composition owner for the
  current base-runtime app.
- MUST pass shell-owned adjunct state explicitly into shared UI; shared UI must
  not recover runtime behavior through legacy wrapper defaults or downcasts.
- MUST keep only one major drawer/support surface active at a time.
- MUST keep audio reselect drawer-based; chat audio reselect must not default
  to Android file management.
- MUST keep scheduler task truth, audio artifact truth, connectivity transport
  truth, and follow-up mutation truth owned by their downstream collaborators.
- MUST keep dynamic-island connectivity takeover limited to the target
  transport-truth states unless the core-flow authority is deliberately revised.
- MUST record smart-only route blocks through `SMART_SURFACE_BLOCKED` or an
  equivalent auditable checkpoint before claiming target telemetry convergence.
- MUST require fresh `adb logcat` evidence for installed-app launch,
  onboarding, drawer conflict, dynamic-island, connectivity, telemetry-delivery,
  or badge follow-up runtime claims.

### Error Paths

- First-launch setup skipped or completed: close forced onboarding surfaces and
  return to normal shell use without leaving a stale blocking flag.
- Connectivity not configured: open setup/modal instead of pretending the badge
  is usable. Connectivity configured: route to manager or modal without owning
  BLE/media truth in the shell.
- Second drawer request while another drawer is open: atomically switch active
  drawer or keep the current drawer; do not render overlapping drawers.
- Settings, history, or connectivity opened while another support surface is
  active: clear incompatible drawer/connectivity/history flags first.
- New-session requested while a badge follow-up is active: clear the follow-up
  when the current action leaves the bound session context.
- Dynamic island visible lane is connectivity: tap opens connectivity entry;
  downward drag must not open connectivity.
- Smart-only surface requested: block the route, keep a valid base-runtime
  shell surface visible, and record `SMART_SURFACE_BLOCKED` before claiming
  target behavior is complete.

## Telemetry Joints

- [INPUT_RECEIVED]: app launch, first-launch setup gate, header action,
  dynamic-island tap/drag, drawer open/close command, connectivity entry,
  settings/history entry, new-session request, and badge scheduler completion.
- [ROUTER_DECISION]: choose home shell, forced onboarding setup, scheduler
  drawer, audio browse/reselect drawer, history, settings, connectivity modal,
  connectivity manager, add-device setup, or follow-up prompt/session handoff.
- [STATE_TRANSITION]: shell mounted, active drawer changed, overlay cleared,
  forced-first-launch flag cleared, connectivity surface changed, follow-up
  binding started/cleared, and dynamic-island visible lane changed.
- [OUTPUT_EMITTED]: chat/home shell visible, drawer visible/closed, history
  visible, settings visible, connectivity surface visible, visible island item,
  follow-up prompt, and smart-surface block outcome.
- [ERROR_CAPTURED]: drawer conflict, invalid connectivity route, smart-only
  route attempt, missing runtime proof, and canonical telemetry mismatch.

Delivered telemetry includes local `UI_STATE_EMITTED` and route logs for several
shell actions. Delivered gap: Sprint 08 did not find first-class emitted
canonical valves for `SIM_ENTRY_STARTED`, `SIM_SHELL_MOUNTED`, `CHAT_VISIBLE`,
`SCHEDULER_DRAWER_OPENED`, `SCHEDULER_DRAWER_CLOSED`,
`AUDIO_DRAWER_OPENED`, `AUDIO_DRAWER_CLOSED`, or
`SMART_SURFACE_BLOCKED`. Future work must converge delivered logs with these
target valves instead of translating them ad hoc.

## UI Docking Surface

UI attaches through `RuntimeShell` and shell-owned state: header actions,
dynamic island, chat/home canvas, scheduler drawer, audio drawer, history,
settings, connectivity surfaces, and badge follow-up prompt. Presentation may
use shared composables, but routing authority stays in the shell and downstream
feature truth stays with the owning feature modules.

## Core-Flow Gap

- Gap: first-launch wording was realigned in Sprint 09. Delivered code returns
  to home after forced onboarding, with optional shell-owned scheduler handoff;
  this matches UX governance but differs from older shell-routing wording that
  implied a mandatory connectivity-manager stop.
- Gap: delivered dynamic-island code treats `PARTIAL_WIFI_DOWN` as a persistent
  connectivity takeover. Target behavior still excludes `PARTIAL_WIFI_DOWN`,
  Wi-Fi mismatch, update, and manager-only diagnostic states from takeover.
- Gap: canonical telemetry valves are incomplete by delivered static evidence,
  including `SIM_ENTRY_STARTED`, `SIM_SHELL_MOUNTED`, drawer open/close valves,
  and `SMART_SURFACE_BLOCKED`.
- Gap: Drawer Conflict proof remains static/source-backed and historical L3 was
  partial. Fresh runtime evidence is still required before claiming direct
  drawer conflict behavior.
- Gap: new-session follow-up-clear behavior exists in source/test helpers, but
  Sprint 08 found one specific helper assertion may lack executable test
  coverage.
- Gap: launch, onboarding, connectivity takeover, drawer overlap, audio
  file-picker escape, current telemetry delivery, and badge-origin follow-up
  remain static-only or unproven runtime gaps without fresh `adb logcat`.

## Test Contract

- Evidence class: contract-test for this docs-only BAKE write; platform-runtime
  for any future installed-app, UI, connectivity, telemetry, or badge follow-up
  claim.
- Existing source/test coverage includes root isolation, shared shell core
  structure, shell gestures/handoffs, history structure, settings routing,
  connectivity routing, runtime isolation, dynamic-island coordinator behavior,
  and dynamic-island state mapping.
- Minimum verification for this contract: prove BAKE frontmatter and sections
  exist, prove required shell-routing terms and gaps are recorded, prove the
  interface map plus scoped Cerb docs cite this contract as authority/reference,
  and run `git diff --check` for scoped docs.
- Minimum future runtime closure: fresh filtered `adb logcat` for the exact
  claimed branch, including launch/mount, first-launch onboarding, Drawer
  Conflict, dynamic-island lane/tap/drag, connectivity entry, smart-only block,
  new-session follow-up clear, and badge follow-up prompt where applicable.

## Cross-Platform Notes

- This contract is shared product truth for shell routing. Android code is
  implementation evidence, not a HarmonyOS or iOS delivery prescription.
- Native platforms must preserve the same shell-routing guarantees: one active
  major support surface, explicit shell-owned adjunct state, prompt-first badge
  follow-up, drawer-based audio reselect, visible-lane island tap, and
  smart-only route blocking.
- Platform-specific launcher, permission, navigation, gesture, logging, and UI
  framework mechanics belong in platform adapters or overlays. Do not claim
  parity on another platform without platform-native runtime evidence.
