# Dynamic Island UI Spec

> **Context Boundary**: `docs/cerb-ui/dynamic-island/`
> **Status**: Active
> **Visual Role**: Sticky top-header ambient summary surface
> **Behavioral UX Authority Above This Doc**: [`docs/core-flow/base-runtime-ux-surface-governance-flow.md`](../../core-flow/base-runtime-ux-surface-governance-flow.md) (`UX.HOME.HEADER.DYNAMIC_ISLAND`, `UX.SCHEDULER.ENTRY.DYNAMIC_ISLAND_ENTRY`)

## 1. Ownership

This shard owns the local `Dynamic Island` renderer contract used inside the standard top header on eligible top-level screens beneath the shared UX authority in `docs/core-flow/base-runtime-ux-surface-governance-flow.md`.

It owns:

- one-line summary presentation
- center-slot layout behavior
- truncation behavior
- tap-routing payloads for scheduler and connectivity entry
- single-item visibility policy
- RuntimeShell-local lane arbitration rules beneath the shared shell UX contract
- renderer-local visual states for scheduler and RuntimeShell/SIM connectivity copy
- the current single-production-host mounting assumption under `MainActivity -> RuntimeShell`

It does not own:

- scheduler task truth, prioritization, or drawer internals
- connectivity transport truth or connectivity manager behavior
- shared shell layout outside the center host slot
- global tokens in `docs/specs/style-guide.md`
- shared layer rules in `docs/specs/ui_element_registry.md`
- broad default-header migration outside the current RuntimeShell/SIM slice

## 2. Current Contract

The shared renderer remains a sticky one-line center-slot surface.

Current contract:

- it appears on eligible top-level surfaces that use the persistent shell header family
- it mounts inside the header, not below it
- it stays one line and never expands vertically
- it shows only one winning item at a time
- the visible island body is borderless chroma-led text plus a state dot rather than a framed pill
- it never introduces inline buttons, chips, or second-line subtitles
- the scheduler lane may also host a session-title highlight item in the same one-line body
- tap follows the currently visible lane
- downward drag remains a scheduler-only entry gesture
- scheduler remains the default lane when no connectivity takeover is active
- RuntimeShell/SIM may temporarily surface a connectivity lane in the same center slot without redefining the rest of the header family

Current composition:

```text
┌──────────────────────────────────────────────┐
│ [☰] [Badge]     [ Dynamic Island ]      [+] │
└──────────────────────────────────────────────┘
```

## 3. Lane Arbitration Rules

### Scheduler lane

Scheduler remains the base lane and uses the existing scheduler-backed summaries:

- session-title highlight item: current renamed session title, chrome blue, `3s` dwell, optional leading audio indicator, and local typewriter reveal when it becomes visible
- conflict item: `冲突：任务标题 · 时间`
- normal upcoming item: `最近：任务标题 · 时间`
- idle fallback: `暂无待办`
- SIM RuntimeShell rotates scheduler candidates locally, with a `3s` dwell for the session-title highlight item and `5s` for scheduler-backed items
- when a session-title highlight item exists, the SIM-local scheduler lane may cap the visible rotation set at `3` total items (`title + up to 2 scheduler summaries`)

### Connectivity lane

RuntimeShell/SIM may temporarily replace the scheduler lane with a connectivity lane.

Rules:

- transport truth comes from `ConnectivityViewModel.connectionState`
- included connectivity island states are `CONNECTED`, `DISCONNECTED`, `RECONNECTING`, and `NEEDS_SETUP`
- `CONNECTED` interrupts the scheduler lane for `5s` on state change so the success state can read clearly in the shared header
- `DISCONNECTED` interrupts the scheduler lane for `3s` on state change
- a heartbeat may re-show `CONNECTED` every `30s` for `5s` when no takeover is already active
- a heartbeat may re-show `DISCONNECTED` every `30s` for `2.5s` when no takeover is already active
- `RECONNECTING` and `NEEDS_SETUP` are persistent takeovers until the underlying transport state changes
- the connectivity lane is suppressed while the scheduler drawer is open or while any connectivity-owned surface (`MODAL`, `SETUP`, `MANAGER`) is already visible
- visible connectivity tap opens the connectivity entry surface instead of the scheduler drawer
- downward drag does not open connectivity; it remains reserved for scheduler entry only

Current connectivity copy:

- connected: `Badge 已连接`
- disconnected: `Badge 已断开`
- reconnecting: `Badge 重连中...`
- needs setup: `Badge 需要配网`

Current battery rule:

- the connected connectivity lane may expose bridge-backed battery data through shell-owned ambient chrome
- RuntimeShell/SIM may render decorative left/right ambient flank icons when the connected lane is visible
- the battery value comes from `ConnectivityViewModel.batteryLevel: StateFlow<Int?>`, seeded `null` until the first `Bat#` push arrives
- the right ambient flank may use the battery value only when it is non-null, and it must remain outside the one-line island body
- text surfaces that reuse the same battery source render a muted `--%` placeholder while the value is `null`

Excluded from this lane:

- update-check / update-found / updating refinements
- Wi-Fi mismatch and manager-only diagnostic states
- extra connectivity sub-lanes that do not map to the transport-truth shell contract above

## 4. Integration Rules

- scheduler remains the owner of task meaning, prioritization, and scheduler-target routing metadata
- RuntimeShell owns only the visible-lane arbitration and shell entry routing
- RuntimeShell/SIM may project session-title and audio-history metadata into the scheduler lane, but the renderer still treats them as shell-owned presentation inputs rather than new scheduler truth
- shared renderer callers must pass scheduler/connectivity inputs explicitly; the renderer must not infer runtime truth from legacy wrapper defaults or concrete shared-UI downcasts
- connectivity island behavior must use transport truth, not manager-only presentation refinements
- the island may open the scheduler drawer or connectivity entry, but it must not become a second scheduler surface or a second connectivity manager
- shells other than RuntimeShell/SIM must not inherit the connectivity lane by assumption; they need an explicit spec update first

## 5. Deferred Scope

The following are explicitly deferred:

- real battery sourcing from a bridge-backed contract
- shared/default-header migration outside the current RuntimeShell/SIM slice
- agent activity lane
- system alert lane
- manager-only connectivity refinements inside the island
- rich inline actions inside the island
