# SIM Shell Spec (Historical Redirect)

> **Scope**: Historical SIM shell shard retained in place for campaign memory.
> **Status**: Historical redirect
> **Current Reading Priority**: Historical context only; not current source of truth.
> **Interpretation Rule**: Read this shard as preserved migration memory for the old SIM lane, not as a second non-Mono shell truth.
> **Unification Note**: The old shell contract is retained only as a historical base-runtime shell baseline while active shell truth lives in shared docs.
> **BAKE Authority Notice**: Supporting/reference historical shard beneath
> `docs/bake-contracts/shell-routing.md` for delivered shell-routing
> implementation truth. Core-flow docs remain the behavioral north stars.
> **Current Active Truth**:
> - `docs/core-flow/sim-shell-routing-flow.md`
> - `docs/bake-contracts/shell-routing.md`
> - `docs/specs/prism-ui-ux-contract.md`
> - `docs/cerb-ui/home-shell/spec.md`
> - `docs/cerb-ui/dynamic-island/spec.md`
> - `docs/cerb/interface-map.md`
> - `docs/specs/base-runtime-unification.md`
> **Historical Origin Context**:
> - `docs/to-cerb/sim-standalone-prototype/concept.md`
> - `docs/reports/20260319-sim-standalone-code-audit.md`
> - `docs/reports/20260319-sim-clarification-evidence-audit.md`
> - `docs/reports/20260331-base-runtime-unification-drift-audit.md`

---

## Historical Redirect Note

This shard is no longer allowed to own active shell truth.
It must not be treated as a second non-Mono shell truth.
Its only remaining value is historical context for the former SIM-owned shell lane and a record of the base-runtime shell baseline that later moved into shared docs.

Use the current active docs above for implementation and review.
The remainder of this file is retained only as a historical snapshot of the older SIM-shell framing.

## Sprint 14 Agent Chat Alignment Note

Sprint 14 (`docs/projects/bake-transformation/sprints/14-agent-chat-pipeline-dbm.md`)
mapped delivered agent-chat-pipeline behavior. Because this shard is historical,
the notes below are supporting reference only beneath current core-flow and
future BAKE contract authority:

- blank/general SIM chat is a supported shell state before audio attachment,
  backed by composer send and local SIM session creation.
- persona, user metadata, and local session history may shape general chat
  context; hidden tool execution, smart-agent capability, and Mono memory must
  not be implied.
- session persistence is SIM-only. Durable message types are user text, AI
  response, AI audio artifacts, and AI error; transient thinking, streaming,
  voice draft, and transcript-reveal state stay memory-only.
- composer send is explicit. FunASR voice draft remains draft-only and must not
  auto-send or append durable history.
- scheduler-shaped general-chat input may pre-route to scheduler-owned logic,
  but scheduler storage, reminders, conflict truth, and mutation authority stay
  outside generic SIM chat.
- badge scheduler follow-up is prompt-first and task-scoped. The shell may host
  the follow-up surface, but it must not create a second memory lane.
- Sprint 14 left telemetry, installed runtime evidence, provider/network
  behavior, live voice draft/FunASR, scheduler side effects, follow-up prompt
  behavior, and logcat delivery as gaps for Sprint 16.

---

## 1. Historical Purpose At The Time

`SIM Shell` is the standalone application shell for the simplified Prism prototype.

It exists to preserve the current Prism family look while preventing contamination from the smart-agent runtime.

This historical snapshot comes from the period when the shard was still being used as an interim shell baseline before active truth moved into shared shell docs.

`SIM Shell` owns:

- standalone app entry
- top-level drawer orchestration
- simple navigation between discussion chat, scheduler, and audio
- shell support surfaces such as history, new page/session, connectivity entry, and settings entry
- a persistent top-bar dynamic island surface that stays mounted above the SIM chat chrome
- shell-owned badge scheduler follow-up continuity binding metadata
- badge-origin scheduler follow-up prompt/chip routing
- prototype-only dependency composition boundary

`SIM Shell` does not own:

- smart-agent orchestration
- plugin execution framework
- mascot behavior
- the current smart app's Hilt root

---

## 2. Product Role

This shell should feel visually continuous with the current app, but operationally it is a different product mode.

The user should perceive:

- the same Prism shell language
- the same drawer family and glass/light treatment
- a smaller, more literal app focused on two main feature lanes
- a sparse idle chat surface: balanced top controls with hamburger on the left, new chat on the right, a one-line dynamic island in the header center slot, greeting-first body, and a bottom message capsule instead of dashboard cards or analytics chrome
- the same shell identity continuing into active discussion, with the heavy top and bottom monoliths remaining stable while only the center canvas changes by state
- restored neutral fuzzy seams on those monoliths so the shell reads as premium hardware rather than a hard-cut black frame; keep the top seam subtle and the bottom seam heavier
- a bottom message capsule whose idle state keeps a single shimmering inline hint line, rotating across `输入消息...`, audio-library swipe-up guidance, and attachment guidance rather than stacking multiple hint rows
- a shell-owned voice-draft affordance that shows mic only while the draft is blank, renders SIM-owned FunASR realtime partial text inside the field during capture, commits the current draft into the field on release, flips to send only after draft text exists, follows the shared SIM realtime recognizer contract, and ends the handshake animation with capture rather than lingering through post-release processing
- active discussion states that prefer sparse conversation plus horizontal system sheets over dashboard cards or smart-agent chrome
- ordinary shell affordances that still feel normal, such as history, new page/session, connectivity entry, and settings
- a scheduler-open override that keeps the center dynamic island visible but suppresses the left/right header utility buttons while the scheduler slab owns the page

The user should not perceive:

- hidden smart-agent behavior
- unrelated drawers and labs
- runtime surfaces that imply the old agent OS still exists behind the prototype
- a fake shell-owned mic affordance that really routes somewhere else

---

## 3. Non-Negotiable Rules

1. `SIM Shell` must be a standalone entry path, not a conditional branch jammed into the smart shell.
2. `SIM Shell` must not reuse `AgentShell` as its composition root.
3. `SIM Shell` must not depend on the smart app's full `PrismModule` graph.
4. `SIM Shell` may reuse existing composables only through controlled seams.
5. `SIM Shell` must expose the two main product lanes and only the SIM-approved support surfaces:
   scheduler,
   general SIM chat plus audio/transcription context attachment,
   history/new page/connectivity/settings as simplified shell practices.

---

## 4. Reuse Contract

### Allowed UI Reuse

- `AgentIntelligenceScreen`
- `SchedulerDrawer`
- `HistoryDrawer`
- `ConnectivityModal`
- `UserCenterScreen` for legacy/shared smart-shell settings
- shared tokens, surfaces, and shell styling

### Conditional UI Reuse

- `AudioDrawer` only after its state/runtime seam is separated from shared audio/session behavior
- Wave 1 may use a SIM-owned audio drawer wrapper while the current shared `AudioDrawer` remains coupled to shared repositories and concrete audio viewmodel behavior

### Forbidden Direct Reuse

- `AgentMainActivity`
- `AgentShell`
- `AgentViewModel`
- current global `PrismModule` as the SIM composition root

### Allowed Replacement Pattern

If an existing UI surface already accepts an interface contract, SIM should provide its own implementation rather than duplicating the UI.

Current target seams:

- `IAgentViewModel`
- `ISchedulerViewModel`
- `UserCenterViewModel`

### Naming Rule

If current smart-app names blur standalone ownership, SIM should prefer explicit standalone naming over legacy familiarity.

Examples:

- `SimMainActivity` over reuse of `AgentMainActivity`
- `SimShell` over reuse of `AgentShell`

Do not rename only for style; rename when boundary truth improves.

---

## 5. Navigation Model

`SIM Shell` owns three primary surfaces:

1. `Discussion Chat Surface`
2. `Scheduler Drawer`
3. `Audio Drawer`

`SIM Shell` also permits support surfaces/actions:

- history drawer
- new page / new session action
- connectivity entry for badge connection management from the audio drawer rather than the chat home header
- settings entry for profile/metadata controls, with the current SIM slice anchoring that affordance in the history drawer's fixed bottom user dock
- a persistent dynamic-island tap target that opens the scheduler drawer from chat

Current SIM settings presentation:

- right-edge dark frosted drawer slab
- same support-surface family as history rather than a full-screen smart-shell sheet
- scrim-backed and dismissible
- no visible top-left close button; dismissal remains scrim tap plus system back
- keep a protected blank top band below the Android status area and place the hero/profile block as the first meaningful visible content beneath it
- current edit-profile route remains the existing full-screen subflow entered from inside the drawer
- the current approved SIM drawer IA restores the prototype section order `偏好设置 / 空间管理 / 安全与隐私 / 关于 / 退出登录`
- row behavior is fixed in the current SIM slice:
  - real interactive: `编辑资料`, theme, notification settings
  - real informational: build-sourced version row
  - deferred-disabled: `AI 实验室`, `已用空间`, `清除缓存`, `修改密码`, `帮助中心`, `退出登录`
- deferred-disabled rows are visual IA only in this slice:
  - they stay visible to preserve the approved drawer structure
  - they must be muted and non-clickable
  - they must not show chevrons, toggles, or destructive affordances that imply shipped behavior
- the theme row edits the shared app theme preference and the normal launcher-path SIM host now applies it immediately
- on a fresh SIM install or reinstall with no saved theme choice yet, the launcher-path SIM host defaults to dark instead of inheriting the shared app's unsaved `SYSTEM` fallback
- once the user explicitly chooses `Dark`, `Light`, or `System`, that stored choice becomes authoritative immediately for the launcher-path SIM host
- launcher-core theme-aware surfaces in the current slice are:
  - home shell
  - active chat / conversation shell
  - top header / dynamic island chrome
  - history drawer
  - settings drawer
- deferred dark-first surfaces in the current slice are:
  - scheduler drawer
  - audio drawer
  - connectivity manager / modal
  - onboarding / setup flows

### Home / Here Surface State Family

The SIM home/here screen is one shell family with multiple content states, not separate screen identities.

Shared shell structure:

- `Top Monolith`: hamburger on the left, one-line Dynamic Island in the center slot, new-session `+` on the right
- `Center Canvas`: protected body between the top and bottom monoliths; this is the only zone that swaps major content by state
- `Bottom Monolith`: composer foundation with left attach, center text field, and a trailing action that shows mic while the draft is blank and send once editable text exists
- `Shared Seam Treatment`: keep one neutral feathered seam family across the shell states below; the seam may soften the boundary but must not reinterpret the monoliths as floating capsules or aurora-tinted glass

Scheduler-open chrome override:

- while the scheduler drawer is open, hide the top-monolith hamburger and new-session actions
- keep the dynamic island mounted in the center slot as the only visible top-monolith affordance
- restoring the normal shell state restores the ordinary hamburger / new-session chrome

Required shell states:

1. `Empty Home`
   - greeting-first body
   - the hero title uses the current user-profile display name in `你好, {displayName}` form, with `SmartSales 用户` as the blank-name fallback
   - the subtitle remains the static assistant copy `我是您的销售助手`
   - directly usable composer
   - no dashboard cards, hero skill pills, or old smart shell utility chrome
2. `Active Plain Chat`
   - normal SIM discussion under the same shell chrome
   - sparse conversation-first canvas
3. `Active Audio-Grounded Chat`
   - one attached audio context enriches the same session
   - the shell remains chat-first rather than switching into an audio tool layout
4. `Pending / Transcribing Audio Chat`
   - pending audio selected from chat continues inside the same discussion session
   - progress/status appears as in-chat system presentation rather than a forced shell reroute
5. `Support-Surface Origin`
   - the same shell remains the origin point for history, scheduler, audio, connectivity, and settings routes

Approved prototype rule:

- the approved dark prototype frame with centered island, heavy top/bottom monoliths, and sparse discussion canvas is the visual baseline for the `Active Plain Chat` substate
- that baseline now includes restored neutral feathered seams at the monolith boundaries: restrained at the top, heavier at the bottom, and never aurora-colored
- downstream UI work must extend from that frame to the other shell states instead of treating that single frame as the whole contract

Expected navigation:

- opening scheduler does not require the smart shell
- opening audio drawer does not require the smart shell
- opening connectivity from the audio drawer does not require the smart shell
- opening history does not require smart memory architecture
- opening settings does not require the smart shell, and it must atomically close history, connectivity, and active drawers first
- opening connectivity uses a SIM-owned state-aware route:
  - `NeedsSetup` opens the bootstrap modal
  - configured/non-setup states open the connectivity manager directly
- choosing setup from the connectivity modal opens a full-screen SIM setup branch backed by onboarding pairing steps
- successful connectivity setup enters the SIM connectivity manager
- closing the connectivity manager returns the user to normal SIM chat
- blank/new chat is directly usable as normal SIM chat
- `Ask AI` from audio opens chat with that audio pre-attached
- selecting audio from chat reopens the audio drawer instead of Android file picker
- when the SIM scheduler drawer opens with no task items and no inspiration items, show one instructional badge-recording guide card instead of a fake scheduler item
- the same chat shell may move between empty, plain-chat, audio-grounded, and pending-audio presentation without changing top-level shell identity

### History Drawer Support Surface

Current SIM history-drawer expectations stay intentionally narrow and support-surface-only.

Required current slice:

- a left-edge theme-aware slab that reads as an archive overlay rather than a second dashboard
- default top safe-area handling uses the global blank-band rule: native status inset, then a blank safe band, then drawer content
- a compact `历史记录` title sits below that blank safe band as the current SIM-local composition anchor
- grouped session rows using `置顶` / `今天` / `最近30天` / `YYYY-MM`
- light-theme fidelity now follows the latest prototype for layout, type, grouped section rhythm, empty state, and anchored dock while intentionally staying in the repo's no-blur readability-first material lane
- group labels use 13sp semi-bold muted text with spacing hierarchy only; visible divider lines are removed in the current light-theme slice
- one-line title-only rows in the normal browse state
- rows stay fixed-height at 48dp with 15sp single-line title treatment plus ellipsis
- long-press actions for pin, rename, and delete
- a stronger history-open scrim so the drawer visually owns the overlay
- a fixed bottom user dock that remains visible below the session list
- the drawer surface explicitly consumes pointer events so taps do not bleed through to the scrim/chat behind it
- local L1 validation now lives in `SimHistoryDrawer.kt` as four preview states: populated, empty, truncation stress, and dense groups

Required user dock content:

- avatar
- display name
- short membership / plan text
- settings affordance on the trailing edge

Required routing:

- tapping the avatar/name zone opens the same SIM `UserCenterScreen` settings route
- tapping the settings affordance opens that same route
- the user dock is the expected SIM-local settings/profile entry for the current history drawer slice

Still deferred in this slice:

- live search
- top header utility chrome such as device capsule or search
- swipe-reveal actions
- any widening into a broader smart-app utility drawer

### Drawer Gesture Contract

SIM keeps the shell gesture model zone-scoped and velocity-aware.

- when the normal SIM shell is visible and no support overlay is active, a downward pull that begins on the dynamic island opens the scheduler drawer
- when the normal SIM shell is visible and no support overlay is active, an upward pull that begins in the lower activation region opens the audio drawer in browse mode
- the current shipped activation model is chrome-anchored rather than full-screen thirds
- the scheduler-open activation region is the visible dynamic island hit target rather than the full top header band
- the lower activation region is the full shell width from about 12dp above the measured SIM composer top through the bottom edge so the audio opener remains reachable after chat grows
- shell-owned gesture layers may cover the live header/composer chrome for drag detection, but they must not steal ordinary taps from the attach button, text field, or send button
- the center body remains a protected chat/history scroll zone rather than a drawer-entry surface
- the settings drawer must block shell pull gestures while visible
- shell entry gestures require vertical-intent locking; weak or horizontally-biased drags must stay with normal content behavior
- shell entry gestures should use both committed drag distance and deliberate fling velocity rather than velocity alone; the current open thresholds are about 40dp committed drag or a deliberate 1100dp/s fling in the matching direction
- velocity acts as an override for clearly intentional pulls, not as a replacement for directional and distance checks
- opening and closing thresholds should use light hysteresis so drawers do not feel twitchy near the commit line
- the bottom-zone audio-open gesture is disabled while the IME/keyboard is visible
- scheduler dismissal must work from the visible calendar handle via upward swipe; the current SIM sheet may also keep the bottom handle affordance during transition
- SIM audio dismissal is handle-first and downward
- scheduler month chevrons may page visible month locally, but they must not acknowledge date attention; only explicit day tap may acknowledge and clear attention state
- list scrolling or general chat overscroll must not act as an alternate drawer-open gesture
- a first-launch-only scheduler teaser may auto-drop the drawer once to teach dismissal/opening, but it must not repeat on later launches
- when the scheduler is idle, the island may also show one SIM-local session-only teaching line such as `下滑这里查看日程`; that hint resets on app restart and clears once the user explicitly opens the scheduler

### Connectivity Boundary Rule

For Wave 5 boundary purposes, `SIM Shell` owns only:

- connectivity entry sources, including the audio drawer entry
- route selection between `MODAL`, `SETUP`, and `MANAGER`
- overlay layering and dismiss semantics
- close-back-to-chat behavior
- SIM route telemetry/logging

`SIM Shell` does not own:

- badge connection truth
- BLE scan or Wi-Fi provisioning behavior
- reconnect/disconnect/unpair behavior
- audio-ingress file operations

Those remain owned by the existing connectivity and pairing contracts.

Excluded shell surfaces:

- mascot overlay
- debug HUD
- right-side Tingwu/artifact stubs from the smart shell
- plugin task-board shell behavior

### Dynamic Island Overlay

SIM now allows one shell-owned dynamic island surface in the top-header center slot.

The island is:

- always mounted while the normal SIM shell is visible
- visually dynamic rather than a static label or plain handle
- shell-owned as a presentation and routing surface
- aligned with the shared one-line dynamic island contract

Current delivered scope:

- render one sticky one-line island item at a time in the header center slot
- inject the current renamed session title into the same rotation lane whenever the title is not the untitled placeholder
- render the session-title item in chrome blue and dwell on it for `3s`
- keep scheduler items on the existing `5s` dwell
- cap the SIM-local rotation list at `3` total visible candidates when a title item exists (`session title + up to 2 scheduler items`)
- allow the session-title item to re-interrupt the local scheduler rotation when an auto-rename lands
- render an audio indicator in front of the session title on SIM title surfaces whenever the session has ever carried audio context in its history
- use scheduler-first copy such as `冲突：...` or `即将：...`
- keep overflow on one line through truncation rather than marquee
- rotate vertically through up to the top 3 lawful visible items
- keep conflict-visible scheduler items ahead of normal reminders
- render conflict-visible items with yellow hue and most-immediate normal items with red hue
- tap opens the scheduler drawer and lands on the visible item's corresponding date page
- when there are no reminder tasks, stay mounted with a scheduler-entry idle summary instead of disappearing
- the idle summary may temporarily use SIM-local teaching copy such as `下滑这里查看日程` until the user explicitly opens the scheduler in the current app run
- do not show multi-line stacked reminder content or detached overlay chrome
- the island remains mounted across empty-home and active-discussion shell states as long as the normal SIM shell is visible

Boundary rule:

- the shell owns island presentation, session-title insertion, top-3 vertical rotation, local dwell timing, and tap routing
- scheduler still owns task truth, conflict priority, and reminder ordering
- mascot behavior is not widened by this shell surface in the current slice

---

## 6. State Model

`SIM Shell` should be controlled by a prototype-only shell state holder.

Minimum top-level state:

```kotlin
enum class SimDrawerType {
    SCHEDULER,
    AUDIO
}

data class SimShellState(
    val activeDrawer: SimDrawerType? = null,
    val activeChatAudioId: String? = null,
    val activeConnectivitySurface: SimConnectivitySurface? = null,
    val isChatReady: Boolean = true,
    val showHistory: Boolean = false
)
```

Notes:

- `activeChatAudioId` identifies the current attached audio context, if any
- `activeConnectivitySurface` distinguishes the scrim-backed bootstrap modal from the full-screen setup and manager branches
- no shell state should imply smart history/session browsing or smart-agent orchestration requirements

### Badge Follow-Up Continuity Binding

`SIM Shell` may keep one in-memory, shell-owned metadata record for a badge-origin scheduler follow-up thread.

This continuity binding exists only to preserve ownership across shell surfaces.

It may track:

- badge-origin thread identity
- the bound SIM chat session id
- the last active shell surface
- create/update timestamps

It must not track:

- follow-up transcript turns
- private session-memory summaries
- a second local memory lane separate from SIM chat history

The bound SIM chat session remains the conversational source of truth, even though follow-up behavior must stay separate from normal chat and audio-context attachment.

In Wave 5, this continuity binding now starts from the real badge pipeline ingress.

- `SimShell` consumes `BadgeAudioPipeline.events`
- only `PipelineEvent.Complete` with scheduler-real outcomes may start or replace the binding
- accepted outcomes are `TaskCreated` and non-empty `MultiTaskCreated`
- raw recording arrival, shelf-card `Ask AI`, and scheduler dev/test mic paths must not start or mutate this binding
- each new accepted badge scheduler completion replaces the previous active binding until the pipeline exposes an explicit lineage id

### Wave 8 Task-Scoped Scheduler Follow-Up

Wave 8 upgrades the old continuity seam from metadata-only handoff into a real but narrow SIM follow-up lane.

Locked shell behavior:

- accepted badge-origin completion creates or rebinds a persisted follow-up session
- shell keeps the active binding in memory only
- shell surfaces a visible follow-up prompt/chip instead of auto-switching chat
- opening that prompt reuses the normal SIM chat surface rather than creating a new scheduler cockpit

Locked shell limits:

- shell must not auto-open the follow-up session while the user is on another SIM surface
- shell must not invent a second local memory lane
- multi-task follow-up mutation must stay blocked until the user explicitly selects one bound task

Lifecycle rules:

- overlay navigation across chat, scheduler, history, connectivity, and settings may update the last active surface
- explicit new session clears the binding
- switching to an unrelated session clears the binding
- deleting the bound session clears the binding
- process death clears the binding because lifetime is in-memory only

---

## 7. Composition Boundary

`SIM Shell` must introduce a prototype-only composition root.

Minimum expected composition units:

- `SimMainActivity`
- `SimShell`
- `SimAgentViewModel`
- `SimSchedulerViewModel`
- prototype-only DI module set

This boundary is the contamination firewall.

### T6.1 Prototype-Only DI Root

`T6.1` freezes the SIM composition-root contract before more isolation implementation begins.

Required composition chain:

1. `SimMainActivity`
2. `SimShell`
3. SIM-owned runtime owners such as `SimAgentViewModel`, `SimSchedulerViewModel`, and SIM audio/connectivity coordinators
4. a SIM-owned dependency assembler / module set

Meaning:

- SIM may remain inside the same application process and APK
- the current Android Studio APK packaging mode may point the default launcher activity at `SimMainActivity` so standard install/run flows boot directly into the SIM shell
- SIM must not inherit smart-app runtime ownership just because both product paths share the same binary
- the SIM dependency assembler decides what is reused directly, what is wrapped, and what is excluded
- the smart app's root graph may still exist in the process, but it must not become the default owner of SIM feature behavior

Forbidden root paths:

- `AgentMainActivity -> AgentShell -> AgentViewModel`
- treating `PrismModule` as the effective SIM root
- allowing smart-root singletons to define SIM shell/session behavior by accident

### Dependency Classification Table

`T6.1` is decision-complete only if SIM dependency policy is explicit.

| Dependency / Service | SIM Policy | Reason |
|------|------|------|
| `AgentIntelligenceScreen`, `SchedulerDrawer`, `HistoryDrawer`, `ConnectivityModal`, `UserCenterScreen` | Direct reuse | Shared UI surfaces are allowed when SIM still owns state, routing, and runtime meaning. |
| `IAgentViewModel`, `ISchedulerViewModel` seams | Direct reuse | Existing interface seams are already the preferred boundary for SIM-owned replacements. |
| `ConnectivityBridge`, `ConnectivityService`, `PairingService`, `ConnectivityViewModel` | Direct reuse | These are leaf/support contracts already frozen as allowed Wave 5 reuse and do not own SIM shell/session semantics. |
| `BadgeAudioPipeline` events feed | SIM-wrapped reuse | SIM may consume the event stream, but only through shell-owned continuity/routing logic rather than giving the pipeline shell ownership. |
| SIM audio repository seam and artifact persistence | SIM-wrapped reuse | Audio storage and binding logic must stay SIM-namespaced and must not silently inherit smart-app persistence assumptions. |
| session/history persistence seam | SIM-owned namespaced persistence | `T6.3` stores SIM session metadata and durable history in SIM-only files so smart sessions cannot leak into SIM through shared tables or filenames. |
| notification/alarm stack | SIM-wrapped reuse | Reminder infrastructure is reusable, but only once T4.8/T6.x explicitly defines SIM ownership of entry, routing, and lifecycle consequences. |
| `AudioDrawer` / shared audio UI state machinery | SIM-wrapped reuse | Shared UI can be reused, but only after SIM-owned repository/runtime behavior is clearly separated from smart-path assumptions. |
| `AgentMainActivity`, `AgentShell`, `AgentViewModel` | Forbidden | These are smart-app runtime roots and would make SIM a fake standalone path. |
| global `PrismModule` as SIM composition root | Forbidden | SIM needs a prototype-only dependency assembler instead of inheriting the smart app's full root graph. |
| mascot/debug HUD/plugin-task-board owners | Forbidden | These carry smart-shell meaning that is outside the SIM product contract. |
| smart-only session/memory ownership | Forbidden | SIM chat may keep local discussion state, but must not depend on the broader smart-agent memory architecture by default. |

### T6.1 Parallel-Safe Rule

While scheduler shipping work continues in `T4.8`, this `T6.1` slice stays docs-first and must avoid implementation changes in the shared collision zone:

- `SimMainActivity`
- `SimShell`
- shared notification/alarm entry wiring

Reason:

- `T4.8` may still need reminder/banner/alarm behavior in scheduler-owned paths
- `T6.1` is only freezing root-ownership truth, not executing the isolation refactor yet

If later implementation requires changes in those files, that work must be scheduled as a dedicated follow-on slice after checking for collision with the active scheduler lane.

### T6.1 Explicit Deferrals

This slice does not implement:

- `T6.2` audio persistence namespacing work
- `T6.4` verification execution
- `T4.8` reminder/banner/alarm behavior
- any new connectivity behavior beyond the already accepted Wave 5 boundary

### T6.3 Accepted SIM Session Persistence

`T6.3` is now implemented as SIM-only durable session persistence.

- session metadata persists in `sim_session_metadata.json`
- durable history for each session persists in `sim_session_<sessionId>_messages.json`
- SIM does not reuse the smart app's broader session-memory or history persistence path
- cold start restores grouped SIM history, but does not auto-select an active session
- normal runtime does not seed demo sessions into SIM history
- durable turns are limited to user text, AI response, AI audio artifacts, and AI error
- transient UI state such as input text, sending/thinking state, toast/error presentation, and transcript-reveal knowledge remains memory-only
- Wave 8 extends this metadata with a session kind, sticky audio-context-history flag, plus optional scheduler-follow-up context:
  badge thread id,
  bound task ids,
  optional batch id,
  task summary snapshot,
  create/update timestamps

---

## 8. Human Reality

### Organic UX

The user expects "same shell" to mean visual familiarity, not identical internal architecture.

So the shell may simplify behavior as long as:

- entry feels familiar
- drawers feel familiar
- history/new-page/connectivity/settings remain coherent
- the app does not suddenly expose missing smart surfaces and dead controls

### Data Reality

The shell must not assume it can safely share all smart-app session state.

Session identity, chat context, and audio bindings now use prototype-specific storage or namespacing in the shipped SIM path.

### Failure Gravity

The most dangerous failure is a fake standalone build where the shell still boots the smart runtime under the hood.

That failure is worse than minor UI drift.

---

## 9. Wave Plan

| Wave | Focus | Status | Deliverable |
|------|-------|--------|-------------|
| 1 | Standalone entry and shell contract | IMPLEMENTED_PENDING_RUNTIME_VERIFICATION | `SimMainActivity`, `SimShell`, shell spec/interface |
| 2 | Chat-shell reuse via interface seam | IMPLEMENTED | `SimAgentViewModel : IAgentViewModel` |
| 3 | Drawer orchestration cleanup | IN_PROGRESS | scheduler/audio/history support behavior |
| 4 | Isolation validation | IN_PROGRESS | T6.1 root-policy freeze and later proof that smart runtime is not the SIM root |

---

## 10. Registration Notes

This shard defines planning truth for a future standalone topology.

`docs/cerb/interface-map.md` should be updated only if T6.x later introduces a materially new cross-module ownership edge. The T6.1 policy freeze alone does not require interface-map registration.
