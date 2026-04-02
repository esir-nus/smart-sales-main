# Core Flow: Base Runtime UX Surface Governance

> **Role**: Core Flow
> **Authority**: Primary UX source of truth for the current base-runtime app
> **Status**: Active
> **Last Updated**: 2026-04-02
> **Scope**: Current shipped/shared base-runtime surfaces, sections, elements, and cross-surface interaction rules
> **Product North Star**: [`SmartSales_PRD.md`](../../SmartSales_PRD.md)
> **Supporting Docs**: [`docs/specs/prism-ui-ux-contract.md`](../specs/prism-ui-ux-contract.md), [`docs/specs/style-guide.md`](../specs/style-guide.md), [`docs/specs/ui_element_registry.md`](../specs/ui_element_registry.md), feature `docs/cerb-ui/**`, feature `docs/cerb/**`

---

## Purpose

This document is the single UX governance source of truth for the current base-runtime app.
It breaks the app down by:

- surface
- section
- element
- interaction pattern
- visible states
- trigger
- state change
- dependency
- downstream effect
- invariant

Use this file when a human or agent needs to answer: "what does this surface contain and what is each thing allowed to do?"

If a visible UX surface, section, or element is not represented here, it is not yet locked as current base-runtime UX truth.

---

## Authority Boundary

This doc wins when the question is about:

- surface composition
- section composition
- element presence or absence
- whether an element is static, reactive, or actionable
- visible state pattern
- trigger pattern
- UX consequence of interaction
- cross-surface navigation or drawer/modal handoff

This doc does **not** replace deeper feature logic.
When the question is about scheduler mutation semantics, audio pipeline internals, pairing runtime logic, Room schema, or other below-surface behavior, the owning feature/core-flow/spec remains authoritative.

Practical stack:

1. [`SmartSales_PRD.md`](../../SmartSales_PRD.md) for product identity, journeys, and UX laws
2. this document for current base-runtime UX surfaces and interactions
3. feature `docs/core-flow/**`, `docs/cerb-ui/**`, and `docs/cerb/**` for local detail beneath the UX surface contract
4. [`docs/specs/style-guide.md`](../specs/style-guide.md) and [`docs/specs/ui_element_registry.md`](../specs/ui_element_registry.md) for supporting visual and invariant rules

---

## ID and Row Schema

Stable ID format:

- Surface: `UX.HOME`
- Section: `UX.HOME.HEADER`
- Element: `UX.HOME.HEADER.DYNAMIC_ISLAND`

Element rows use this schema:

| Field | Meaning |
|---|---|
| `ID` | Stable review/reference handle |
| `Element` | Human-readable element name |
| `Type` | `static`, `reactive-static`, `actionable`, or `status-bearing` |
| `Pattern` | Interaction or presentation grammar |
| `Trigger` | What causes change or action |
| `Visible States` | What the user can see |
| `Depends On` | Upstream state or gate |
| `Leads To` | Navigation, state change, or no-op |
| `Invariant` | Hard UX law that must not drift |
| `Downstream Owner` | Local doc that owns deeper detail |

---

## Shared UX Invariants

1. Only one major drawer is open at a time.
2. The dynamic island is a shell entry affordance, not a second surface: scheduler remains the default lane, visible-lane tap follows the rendered item, and downward drag remains scheduler-only.
3. The bottom audio-open gesture must not block composer interaction.
4. Audio reselection from discussion reopens the audio drawer; it must not default to Android file management.
5. Shared shell continuity is preserved by keeping the header family and composer family stable while the center canvas changes.
6. Ambient/system guidance may appear inline or as support surfaces, but it must not become a second competing conversation thread.
7. Status-bar and navigation-bar handling must follow real inset ownership, not prototype hardcoding.

---

## UX.HOME — Home Surface

Purpose: calm base camp for orientation, empty-state welcome, and immediate entry into discussion, scheduler, history, connectivity, and new session.

```text
┌──────────────────────────────────────────────┐
│ [☰] [Badge]  [ Dynamic Island ]        [+]  │  UX.HOME.HEADER
│                                              │
│         你好, SmartSales 用户                 │  UX.HOME.WELCOME
│            我是您的销售助手                   │
│                                              │
│     [ composer capsule / shimmer hint ]      │  UX.HOME.COMPOSER
└──────────────────────────────────────────────┘
```

### Sections

| ID | Section | Purpose |
|---|---|---|
| `UX.HOME.HEADER` | Header | persistent shell chrome and support entry points |
| `UX.HOME.WELCOME` | Welcome | empty-session orientation and tone |
| `UX.HOME.COMPOSER` | Composer | prompt-first message entry |

### Elements

| ID | Element | Type | Pattern | Trigger | Visible States | Depends On | Leads To | Invariant | Downstream Owner |
|---|---|---|---|---|---|---|---|---|---|
| `UX.HOME.HEADER.HAMBURGER` | History trigger | actionable | left utility entry | tap | idle | shell mounted | opens history drawer | no edge-swipe-only assumption | `docs/cerb/session-history/spec.md` |
| `UX.HOME.HEADER.DEVICE_BADGE` | Device status badge | status-bearing | compact connectivity entry | tap / connection change | connected, reconnecting, offline | connectivity state | opens connectivity surface | badge state must reflect connection truth, not decorative guesswork | `docs/cerb/connectivity-bridge/spec.md` |
| `UX.HOME.HEADER.DYNAMIC_ISLAND` | Dynamic island | actionable | sticky one-line shell summary | tap / connection change / downward drag when scheduler is visible | conflict summary, upcoming summary, idle fallback, connected heartbeat, disconnected heartbeat, reconnecting takeover, needs-setup takeover | scheduler summary projection; RuntimeShell connectivity transport truth | opens scheduler or connectivity entry based on the visible lane | one line only; no inline buttons; visible-lane tap must follow the rendered item; downward drag stays scheduler-only; connected ambient flank chrome stays decorative and shell-owned outside the island body | `docs/cerb-ui/dynamic-island/spec.md` |
| `UX.HOME.HEADER.NEW_SESSION` | New session action | actionable | right utility reset | tap | idle | valid shell state | clears current discussion and starts fresh session | must not silently open another product mode | `docs/core-flow/sim-shell-routing-flow.md` |
| `UX.HOME.WELCOME.HEADLINE` | Greeting headline | static | calm empty-state anchor | auto render | visible / hidden | session empty state | no navigation | visible only while session is empty | `docs/cerb-ui/home-shell/spec.md` |
| `UX.HOME.WELCOME.SUBTITLE` | Greeting subtitle | static | product identity reinforcement | auto render | visible / hidden | session empty state | no navigation | disappears when discussion takes over center canvas | `docs/cerb-ui/home-shell/spec.md` |
| `UX.HOME.COMPOSER.CAPSULE` | Composer capsule | actionable | floating bottom monolith | tap / focus / type | idle, focused, drafting | shell mounted; input enabled | enters discussion drafting | home and chat use one composer family | `docs/cerb-ui/home-shell/spec.md` |
| `UX.HOME.COMPOSER.SHIMMER_HINT` | Shimmer hint line | reactive-static | rotating inline guidance | idle timer | hint A, hint B, hint C | empty draft; idle shell | teaches entry options only | exactly one hint line at a time | `docs/specs/ui_element_registry.md` |

### Home transitions

- leaving the empty session hides `UX.HOME.WELCOME.*` and hands the center canvas to `UX.CHAT.CANVAS`
- scheduler-open may suppress side utilities while keeping the island mounted
- the RuntimeShell/SIM lane may temporarily surface connectivity status in the island without changing the surrounding header family
- home chrome remains the shell continuity frame for later discussion states

---

## UX.CHAT — Discussion Surface

Purpose: primary discussion workspace for user messages, AI responses, inline system guidance, and audio-grounded discussion continuity.

```text
┌──────────────────────────────────────────────┐
│ header family remains from UX.HOME           │
├──────────────────────────────────────────────┤
│ user bubble                                  │
│ system response                              │  UX.CHAT.CANVAS
│ agent activity / system-authored sheet       │
│ selected audio context if present            │
├──────────────────────────────────────────────┤
│ [attach] [composer field........] [send]     │  UX.CHAT.COMPOSER
└──────────────────────────────────────────────┘
```

### Sections

| ID | Section | Purpose |
|---|---|---|
| `UX.CHAT.CANVAS` | Canvas | discussion history and inline support content |
| `UX.CHAT.SYSTEM` | System sheets | structured non-user guidance/status surfaces |
| `UX.CHAT.COMPOSER` | Composer | message, attach, and send entry |

### Elements

| ID | Element | Type | Pattern | Trigger | Visible States | Depends On | Leads To | Invariant | Downstream Owner |
|---|---|---|---|---|---|---|---|---|---|
| `UX.CHAT.CANVAS.USER_BUBBLE` | User bubble | status-bearing | optimistic right-aligned message | send | pending, sent, failed if supported | message submit | persists user turn in discussion | user turns remain distinct from system status surfaces | `docs/cerb-ui/agent-intelligence/spec.md` |
| `UX.CHAT.CANVAS.AI_RESPONSE` | AI response surface | status-bearing | left-side response lane | stream / completion | streaming, completed, error branch | model/runtime response | appends system response | must not fake final certainty before response exists | `docs/cerb-ui/agent-intelligence/spec.md` |
| `UX.CHAT.SYSTEM.ACTIVITY_BANNER` | Agent activity banner or system-authored sheet | status-bearing | inline cognition/progress/support sheet | tool run / planning / system guidance | visible, updating, completed | active system work | shows structured support inside chat history | support content may appear inline without replacing the eventual response | `docs/specs/components/AgentActivityBanner.md` |
| `UX.CHAT.SYSTEM.AUDIO_CONTEXT` | Bound audio context surface | status-bearing | discussion-context anchor | audio selected or switched | none, pending-audio, transcribed-audio | current chat audio binding | keeps chat grounded in selected audio | changing audio must preserve the current session unless explicitly reset | `docs/core-flow/sim-audio-artifact-chat-flow.md` |
| `UX.CHAT.COMPOSER.ATTACH` | Attach entry | actionable | contextual attachment entry | tap | idle, disabled if blocked | current chat mode | reopens audio selector or picker | grounded audio discussion prefers audio drawer reselection over file-manager escape | `docs/core-flow/sim-audio-artifact-chat-flow.md` |
| `UX.CHAT.COMPOSER.FIELD` | Composer field | actionable | text entry | tap / type | idle, focused, drafting | input enabled | updates current draft | must remain reachable above audio gesture strip | `docs/cerb-ui/agent-intelligence/spec.md` |
| `UX.CHAT.COMPOSER.SEND` | Send action | actionable | explicit submit affordance | tap | disabled, enabled, sending | non-empty lawful draft | emits user turn | no hidden auto-send from unrelated shell gestures | `docs/cerb-ui/agent-intelligence/spec.md` |

### Chat transitions

- audio selection can bind or switch discussion context without leaving the session
- follow-up prompt/chip may hand the user into a task-scoped discussion branch without force-switching silently
- scheduler and audio remain drawer-style support surfaces around the same shell family

---

## UX.SCHEDULER — Scheduler Surface

Purpose: dependable top drawer for viewing, creating, adjusting, and resolving scheduled work inside the main shell.

```text
┌──────────────────────────────────────────────┐
│ month header / calendar strip                │  UX.SCHEDULER.CALENDAR
├──────────────────────────────────────────────┤
│ timeline rail                                │
│ task card / conflict card / inspiration row  │  UX.SCHEDULER.TIMELINE
│                                              │
├──────────────────────────────────────────────┤
│ bottom dismiss handle                        │  UX.SCHEDULER.HANDLE
└──────────────────────────────────────────────┘
```

### Sections

| ID | Section | Purpose |
|---|---|---|
| `UX.SCHEDULER.ENTRY` | Entry | scheduler opening route from shell island |
| `UX.SCHEDULER.CALENDAR` | Calendar | visible date context and paging |
| `UX.SCHEDULER.TIMELINE` | Timeline | cards, conflicts, inspiration, completion |
| `UX.SCHEDULER.HANDLE` | Handle | explicit dismiss affordance |

### Elements

| ID | Element | Type | Pattern | Trigger | Visible States | Depends On | Leads To | Invariant | Downstream Owner |
|---|---|---|---|---|---|---|---|---|---|
| `UX.SCHEDULER.ENTRY.DYNAMIC_ISLAND_ENTRY` | Dynamic island entry path | actionable | shell-to-drawer handoff | tap / downward drag on scheduler-visible island | idle -> open intent | visible scheduler lane; drawer not blocked | opens scheduler on target date if present | entry region is the visible scheduler island lane, not the whole top header band | `docs/core-flow/sim-shell-routing-flow.md` |
| `UX.SCHEDULER.CALENDAR.MONTH_HEADER` | Month header and date strip | actionable | top context bar | tap / page gesture | current month, paged month | scheduler visible month state | pages visible date context | month paging must not silently acknowledge unrelated task attention | `docs/cerb-ui/scheduler/contract.md` |
| `UX.SCHEDULER.TIMELINE.TASK_CARD` | Task card | status-bearing | collapsed/expanded task presentation | tap / checkbox / swipe when lawful | collapsed, expanded, done, conflicted | scheduler task projection | reveal details or execute lawful task action | urgency, conflict, and completion remain separate channels | `docs/cerb-ui/scheduler/contract.md` |
| `UX.SCHEDULER.TIMELINE.CONFLICT_CARD` | Conflict card | status-bearing | elevated warning card | conflict detection / tap | collapsed, expanded | conflict projection exists | surfaces collision context and next action | conflict must be visible, not implied only through urgency color | `docs/cerb-ui/scheduler/contract.md` |
| `UX.SCHEDULER.TIMELINE.INSPIRATION_ROW` | Inspiration / note lane | actionable | lightweight idea shelf | tap / swipe when supported | visible, empty-state guide | scheduler projection and current scope | supports note/idea handling without redefining scheduler truth | empty state may teach badge recording instead of implying fake tasks | `docs/cerb-ui/scheduler/contract.md` |
| `UX.SCHEDULER.HANDLE.DISMISS` | Bottom dismiss handle | actionable | explicit close affordance | tap / upward swipe | idle | scheduler open | closes scheduler and returns to discussion shell | dismiss direction must match the visible handle grammar | `docs/core-flow/sim-shell-routing-flow.md` |

### Scheduler transitions

- scheduler opens above the same shell family rather than navigating to a detached app mode
- closing scheduler returns to the previous shell state without losing discussion context
- task meaning and mutation legality remain owned by scheduler-specific docs below this UX layer

---

## UX.AUDIO — Audio Surface

Purpose: bottom drawer for inventory, manual sync/transcribe/delete actions, and chat audio selection.

```text
┌──────────────────────────────────────────────┐
│ browse grip / title / capsule / connectivity │  UX.AUDIO.HEADER
├──────────────────────────────────────────────┤
│ helper rows + audio card list                │  UX.AUDIO.BROWSE
│ pending / transcribing / transcribed rows    │
├──────────────────────────────────────────────┤
│ select-mode rows when opened from chat       │  UX.AUDIO.SELECT
└──────────────────────────────────────────────┘
```

### Sections

| ID | Section | Purpose |
|---|---|---|
| `UX.AUDIO.ENTRY` | Entry | bottom-edge drawer opening path |
| `UX.AUDIO.HEADER` | Header | manual inventory actions and drawer context |
| `UX.AUDIO.BROWSE` | Browse list | normal inventory browsing |
| `UX.AUDIO.SELECT` | Select mode | current-chat audio binding / rebinding |

### Elements

| ID | Element | Type | Pattern | Trigger | Visible States | Depends On | Leads To | Invariant | Downstream Owner |
|---|---|---|---|---|---|---|---|---|---|
| `UX.AUDIO.ENTRY.BOTTOM_STRIP` | Bottom-edge audio entry | actionable | narrow gesture strip | upward drag from bottom activation band | idle -> open intent | no competing drawer; composer not hit | opens audio drawer | gesture strip must not overlap attach, field, or send targets | `docs/core-flow/sim-shell-routing-flow.md` |
| `UX.AUDIO.HEADER.BROWSE_GRIP` | Browse grip | actionable | dual-purpose drawer grip | tap / downward drag / upward drag | idle, pull, threshold, denied, syncing, synced | browse mode; drawer open | dismisses drawer or triggers manual sync | browse grip must keep dismiss semantics while reserving upward drag for manual sync only when the badge-sync gate is ready | `docs/cerb/audio-management/spec.md` |
| `UX.AUDIO.HEADER.MANUAL_SYNC` | Browse smart capsule | actionable | explicit inventory refresh/status chip | tap | ready, syncing, synced, blocked | browse mode; drawer open | refreshes drawer-visible inventory or hands off to connectivity when blocked | browse-open must not auto-trigger active sync as product behavior | `docs/cerb/audio-management/spec.md` |
| `UX.AUDIO.HEADER.CONNECTIVITY_ENTRY` | Connectivity entry | actionable | dedicated support icon | tap | available | drawer open | opens connectivity surface | audio drawer must keep a separate connectivity entry even when the capsule also reflects badge status | `docs/cerb/sim-shell/spec.md` |
| `UX.AUDIO.BROWSE.ONBOARDING_HELPERS` | Browse onboarding helpers | informational | transient helper deck | browse open while demo seed is the only item | hidden, sync-helper, delete-helper, both | built-in demo seed only; browse mode | teaches grip sync and swipe-delete | helper rows are browse-only teaching chrome and must not delete real repository items during practice | `docs/cerb/audio-management/spec.md` |
| `UX.AUDIO.BROWSE.PENDING_CARD` | Pending/transcribing audio card | status-bearing | compact processing row | swipe / tap when lawful | pending, transcribing | stored audio item exists | transcribe, monitor progress, or stay informational | pending rows must stay transparent about unfinished processing | `docs/cerb/audio-management/spec.md` |
| `UX.AUDIO.BROWSE.TRANSCRIBED_CARD` | Transcribed audio card | actionable | reviewable artifact row | tap / ask AI / swipe when lawful | collapsed, expanded if supported | transcript/artifact available | opens review affordance or grounded discussion handoff | artifact review is a first-class informational lane, not a storage dump | `docs/cerb/audio-management/spec.md` |
| `UX.AUDIO.SELECT.CURRENT_AUDIO` | Current discussion audio row | status-bearing | disabled current-binding marker | open select mode | current, disabled | discussion already bound to audio | no navigation | current discussion audio must be clearly non-switchable in place | `docs/core-flow/sim-audio-artifact-chat-flow.md` |
| `UX.AUDIO.SELECT.SELECTABLE_ROW` | Select-mode audio row | actionable | immediate rebinding row | tap | transcribed-selectable, pending-selectable | chat attach/select mode | binds or switches current discussion audio | selection must keep the user in the same discussion session | `docs/core-flow/sim-audio-artifact-chat-flow.md` |

### Audio transitions

- audio drawer can open from shell gesture or chat attach path
- browse mode may surface a transient helper deck only while the built-in demo seed is the lone inventory item
- select mode keeps its simpler dismiss handle and must not surface browse-only helper or smart-capsule teaching chrome
- select mode is a narrower audio surface, not a different product lane
- successful selection returns the user to discussion with updated grounding

---

## UX.HISTORY — History Surface

Purpose: left-side continuity surface for loading, grouping, renaming, pinning, and deleting past sessions.

```text
┌──────────────────────────────────────────────┐
│ device capsule / history header              │  UX.HISTORY.HEADER
├──────────────────────────────────────────────┤
│ group card                                   │
│ session item                                 │  UX.HISTORY.GROUPS
│ overflow menu                                │
├──────────────────────────────────────────────┤
│ footer settings/profile entry                │  UX.HISTORY.FOOTER
└──────────────────────────────────────────────┘
```

### Sections

| ID | Section | Purpose |
|---|---|---|
| `UX.HISTORY.HEADER` | Header | continuity context and device status |
| `UX.HISTORY.GROUPS` | Groups | grouped session navigation |
| `UX.HISTORY.FOOTER` | Footer | settings/profile handoff |

### Elements

| ID | Element | Type | Pattern | Trigger | Visible States | Depends On | Leads To | Invariant | Downstream Owner |
|---|---|---|---|---|---|---|---|---|---|
| `UX.HISTORY.HEADER.DEVICE_CAPSULE` | Device capsule | status-bearing | connectivity summary inside drawer | tap / state change | connected, reconnecting, offline | connectivity state | opens connectivity modal | history may surface connectivity, but it does not own connectivity truth | `docs/cerb/connectivity-bridge/spec.md` |
| `UX.HISTORY.GROUPS.GROUP_CARD` | Session group card | actionable | collapsible grouping container | tap header | expanded, collapsed | grouped session data | reveals or hides session items | grouping remains local to history, not discussion memory truth | `docs/cerb/session-history/spec.md` |
| `UX.HISTORY.GROUPS.SESSION_ITEM` | Session item | actionable | session resume row | tap | idle | session exists | loads selected session and closes drawer | history resumes continuity without implying hidden smart-memory orchestration | `docs/cerb/session-history/spec.md` |
| `UX.HISTORY.GROUPS.OVERFLOW_MENU` | Session actions menu | actionable | local item actions | tap overflow | closed, open | selected session item | rename, pin, delete actions | destructive actions stay explicit and local | `docs/cerb/session-history/spec.md` |
| `UX.HISTORY.FOOTER.SETTINGS_ENTRY` | Settings/profile entry | actionable | footer handoff | tap | idle | drawer open | opens settings surface | history footer is a support route, not a second shell root | `docs/specs/modules/UserCenter.md` |

### History transitions

- loading a session returns the user to the main discussion shell
- settings opened from history still enter the same settings surface as other lawful entry points

---

## UX.CONNECTIVITY — Connectivity Surface

Purpose: recoverable support surface for badge status, pairing repair, Wi-Fi alignment, and hardware confidence.

```text
┌──────────────────────────────────────────────┐
│ status summary / battery / badge identity    │  UX.CONNECTIVITY.MODAL
│ current network truth / mismatch guidance    │
│ repair form when needed                      │  UX.CONNECTIVITY.REPAIR
└──────────────────────────────────────────────┘
```

### Sections

| ID | Section | Purpose |
|---|---|---|
| `UX.CONNECTIVITY.ENTRY` | Entry | lawful routes into connectivity support |
| `UX.CONNECTIVITY.MODAL` | Modal | current badge/network truth |
| `UX.CONNECTIVITY.REPAIR` | Repair | manual recovery form and actions |

### Elements

| ID | Element | Type | Pattern | Trigger | Visible States | Depends On | Leads To | Invariant | Downstream Owner |
|---|---|---|---|---|---|---|---|---|---|
| `UX.CONNECTIVITY.ENTRY.BADGE_ENTRY` | Connectivity entry point | actionable | badge/status handoff | tap from shell or history | idle | connectivity route available | opens connectivity modal or manager | connectivity entry is support work, not the emotional center of the product | `docs/cerb/connectivity-bridge/spec.md` |
| `UX.CONNECTIVITY.MODAL.STATUS_PANEL` | Connectivity status panel | status-bearing | current badge/network state | auto update | connected, reconnecting, mismatch, offline | live connectivity state | informs current hardware truth | status must reflect real bridge state, not cached theater | `docs/cerb/connectivity-bridge/spec.md` |
| `UX.CONNECTIVITY.REPAIR.WIFI_FORM` | Wi-Fi repair form | actionable | explicit mismatch repair | tap / submit / retry | hidden, visible, submitting, error | mismatch or repair-needed branch | retries alignment or manual recovery | manual repair must remain available when automatic recovery is insufficient | `docs/cerb/connectivity-bridge/spec.md` |

### Connectivity transitions

- onboarding may hand into connectivity-owned setup/manager states
- shell and history entry points converge into the same support surface family

---

## UX.ONBOARDING — Onboarding Surface

Purpose: calm setup lane that teaches the badge ritual, captures prerequisites, and returns the user to normal shell use quickly.

```text
┌──────────────────────────────────────────────┐
│ welcome / permissions primer                 │  UX.ONBOARDING.INTRO
│ voice handshake consultation/profile         │  UX.ONBOARDING.HANDSHAKE
│ hardware wake / scan / found / provisioning  │  UX.ONBOARDING.PAIRING
│ completion                                   │  UX.ONBOARDING.COMPLETE
└──────────────────────────────────────────────┘
```

### Sections

| ID | Section | Purpose |
|---|---|---|
| `UX.ONBOARDING.INTRO` | Intro | calm expectation-setting |
| `UX.ONBOARDING.HANDSHAKE` | Handshake | phone-mic trust-building and profile intake |
| `UX.ONBOARDING.PAIRING` | Pairing | hardware wake, scan, found, provisioning |
| `UX.ONBOARDING.COMPLETE` | Complete | successful handoff to normal app use |

### Elements

| ID | Element | Type | Pattern | Trigger | Visible States | Depends On | Leads To | Invariant | Downstream Owner |
|---|---|---|---|---|---|---|---|---|---|
| `UX.ONBOARDING.INTRO.WELCOME_CTA` | Welcome CTA | actionable | calm intro continuation | tap | visible | onboarding active | advances to permissions primer | intro must feel like product onboarding, not generic Android setup | `docs/specs/flows/OnboardingFlow.md` |
| `UX.ONBOARDING.INTRO.PERMISSIONS_PRIMER` | Permissions primer | reactive-static | explanatory pre-permission guidance | tap continue / point-of-use permission later | visible | onboarding active | advances without prematurely firing native prompts | explanation first, native prompt only at point of use | `docs/specs/flows/OnboardingFlow.md` |
| `UX.ONBOARDING.HANDSHAKE.VOICE_CAPTURE` | Voice handshake steps | actionable | real consultation/profile capture | hold/tap mic; save CTA | idle, listening, parsed, error | onboarding host and mic availability | advances to hardware wake once complete | handshake must be real interaction, not fake placeholder theater | `docs/specs/flows/OnboardingFlow.md` |
| `UX.ONBOARDING.PAIRING.HARDWARE_WAKE` | Hardware wake step | actionable | badge ritual instruction | tap continue after ritual | visible | onboarding active | enters scan step | hardware ritual must be clearly taught before scan | `docs/specs/flows/OnboardingFlow.md` |
| `UX.ONBOARDING.PAIRING.SCAN` | Scan surface | status-bearing | technical search state | auto progress / cancel / result | searching, found, error | Bluetooth path active | proceeds to found/error branch | scan must request permission at point of use and remain recoverable | `docs/specs/flows/OnboardingFlow.md` |
| `UX.ONBOARDING.PAIRING.DEVICE_FOUND` | Found device card | actionable | explicit found-state confirmation | tap manual connect | visible | scan success | enters provisioning | found state must not auto-connect silently | `docs/specs/flows/OnboardingFlow.md` |
| `UX.ONBOARDING.PAIRING.PROVISIONING` | Provisioning form | actionable | Wi-Fi setup and progress seam | submit / retry / back | idle, submitting, error | selected device and network data | configures network and pairing | provisioning must stay inside one recoverable presentation seam | `docs/specs/flows/OnboardingFlow.md` |
| `UX.ONBOARDING.COMPLETE.SUCCESS_CTA` | Completion CTA | actionable | success wrapper handoff | tap | visible | provisioning success | enters home or connectivity manager host path | onboarding should exit quickly into normal product use | `docs/specs/flows/OnboardingFlow.md` |

### Onboarding transitions

- host selection may change entry/exit context, not the visible wave family
- completion returns to normal shell use or connectivity manager, not to a dead-end screen

---

## UX.SETTINGS — Settings Surface

Purpose: centered support surface for profile, preferences, storage, security, about, and logout.

```text
┌──────────────────────────────────────────────┐
│ profile hero / edit                          │  UX.SETTINGS.PROFILE
├──────────────────────────────────────────────┤
│ preferences                                  │  UX.SETTINGS.PREFERENCES
│ storage                                      │  UX.SETTINGS.STORAGE
│ security / about                             │  UX.SETTINGS.ABOUT
└──────────────────────────────────────────────┘
                    [ log out ]                UX.SETTINGS.LOGOUT
```

### Sections

| ID | Section | Purpose |
|---|---|---|
| `UX.SETTINGS.PROFILE` | Profile | user identity summary and edit entry |
| `UX.SETTINGS.PREFERENCES` | Preferences | theme and app toggles |
| `UX.SETTINGS.STORAGE` | Storage | device-space awareness and cleanup |
| `UX.SETTINGS.ABOUT` | About/Security | version/help/privacy routes |
| `UX.SETTINGS.LOGOUT` | Logout | explicit session end |

### Elements

| ID | Element | Type | Pattern | Trigger | Visible States | Depends On | Leads To | Invariant | Downstream Owner |
|---|---|---|---|---|---|---|---|---|---|
| `UX.SETTINGS.PROFILE.HERO_CARD` | Profile hero card | actionable | centered identity sheet | tap edit | visible | settings open | enters edit path | settings remains an overlay-style support surface, not a side drawer | `docs/specs/modules/UserCenter.md` |
| `UX.SETTINGS.PREFERENCES.THEME_SELECTOR` | Theme selector | actionable | persisted single-choice setting | tap | dark, light, system | settings open | updates stored theme preference | theme choice must persist and apply immediately where implemented | `docs/specs/modules/UserCenter.md` |
| `UX.SETTINGS.STORAGE.CLEAR_CACHE` | Clear cache action | actionable | explicit maintenance row | tap | idle, running, completed | settings open | clears cache | destructive maintenance must remain explicit | `docs/specs/modules/UserCenter.md` |
| `UX.SETTINGS.ABOUT.HELP_VERSION` | Help/version rows | actionable | support info group | tap if route exists | visible | settings open | opens help or shows version detail | informational support stays subordinate to core work surfaces | `docs/specs/modules/UserCenter.md` |
| `UX.SETTINGS.LOGOUT.BUTTON` | Log out button | actionable | explicit session-end CTA | tap | idle | settings open | logs user out | logout must remain explicit, not hidden in unrelated menus | `docs/specs/modules/UserCenter.md` |

### Settings transitions

- settings may open from history footer or other lawful entry points
- closing settings returns to the prior shell/support context

---

## UX.SUPPORT — Shared Support Overlays

Purpose: shared support-layer elements that are not full primary surfaces but still shape the user's experience.

```text
┌──────────────────────────────────────────────┐
│ scrim                                        │  UX.SUPPORT.SCRIM
│ dialog / confirmation                        │  UX.SUPPORT.DIALOG
│ prompt chip / follow-up handoff              │  UX.SUPPORT.PROMPT
└──────────────────────────────────────────────┘
```

### Sections

| ID | Section | Purpose |
|---|---|---|
| `UX.SUPPORT.SCRIM` | Scrim | focus isolation for overlays and drawers |
| `UX.SUPPORT.DIALOG` | Dialog | explicit confirmation or blocking decision |
| `UX.SUPPORT.PROMPT` | Prompt | lightweight handoff guidance |

### Elements

| ID | Element | Type | Pattern | Trigger | Visible States | Depends On | Leads To | Invariant | Downstream Owner |
|---|---|---|---|---|---|---|---|---|---|
| `UX.SUPPORT.SCRIM.BASE` | Shared scrim | status-bearing | focus dimmer | overlay/drawer open | hidden, visible | any exclusive overlay state | closes or protects active surface depending on owner | scrim must preserve one-active-surface clarity | `docs/specs/ui_element_registry.md` |
| `UX.SUPPORT.DIALOG.CONFIRM` | Confirmation dialog | actionable | explicit yes/no decision gate | destructive or risky action | hidden, visible | owner asks for confirmation | confirms or cancels action | destructive work must not ride on ambiguous gestures alone | owner-specific lower spec |
| `UX.SUPPORT.PROMPT.FOLLOW_UP_CHIP` | Follow-up prompt/chip | actionable | lightweight in-shell handoff | badge-origin completion or similar support event | hidden, visible | valid follow-up branch exists | offers entry into task-scoped discussion | prompt-first handoff beats forced context hijack | `docs/core-flow/sim-shell-routing-flow.md` |

---

## Cross-Surface Handshake Summary

| From | Element | To | Rule |
|---|---|---|---|
| `UX.HOME` | `UX.HOME.HEADER.DYNAMIC_ISLAND` | `UX.SCHEDULER` | scheduler-visible island opens scheduler on the visible item/date when available; downward drag is scheduler-only |
| `UX.HOME` | `UX.HOME.HEADER.DYNAMIC_ISLAND` | `UX.CONNECTIVITY` | connectivity-visible island opens connectivity entry; scheduler drawer and connectivity surfaces suppress connectivity takeover |
| `UX.HOME` | `UX.HOME.HEADER.HAMBURGER` | `UX.HISTORY` | history remains a drawer-style support surface |
| `UX.HOME` or `UX.CHAT` | `UX.AUDIO.ENTRY.BOTTOM_STRIP` | `UX.AUDIO` | bottom strip opens audio without stealing composer taps |
| `UX.CHAT` | `UX.CHAT.COMPOSER.ATTACH` | `UX.AUDIO.SELECT` | chat audio rebinding reuses audio drawer select mode |
| `UX.HISTORY` | `UX.HISTORY.FOOTER.SETTINGS_ENTRY` | `UX.SETTINGS` | settings surface is shared regardless of entry point |
| `UX.CONNECTIVITY` / `UX.ONBOARDING` | completion CTAs | `UX.HOME` or connectivity manager | support flows must return the user to normal product use quickly |

---

## Update Rule

When any UX-facing change lands, update this document in the same session if it changes:

- surface composition
- section composition
- element presence or absence
- trigger pattern
- visible state pattern
- cross-surface handoff
- invariant

Lower docs should reference the affected `UX.*` IDs instead of restating shared UX behavior in parallel.
