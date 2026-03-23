# SIM Home/Hero Shell — Transplant Notes

> **Context**: Use this before any Kotlin/Compose implementation.

### Source of truth

Implementation order:

1. `prototypes/sim-shell-family/sim_home_hero_shell.html`
2. `docs/specs/style-guide.md`
3. `docs/cerb/sim-shell/interface.md`
4. `docs/cerb-ui/dynamic-island/spec.md`

Rule:

- prototype = visual truth
- docs = behavior/state boundaries
- implementer must not re-interpret the vibe

Current experiment rule:

- first implementation spike proved the `Empty Home` SIM shell only
- current micro-slice extends that frozen shell chrome into active chat while keeping the current conversation timeline internals
- drawers and support surfaces stay on the current rendering path during this micro-slice
- the shell migration must keep one shared header monolith, centered/bounded center canvas, borderless Dynamic Island, and bottom monolith across idle and active chat
- target screenshots are visual calibration only; production copy and reminder truth must still come from live SIM state
- when the target look conflicts with implementation tricks, prefer a static full-screen atmospheric background over animated bounded blobs
- once empty-home reaches screenshot acceptance, freeze the shell chrome before migrating active chat

### Empty-home freeze gate

Do not widen the migration scope until all of these are accepted:

- one accepted screenshot on the current target phone
- one accepted long Dynamic Island text case
- one accepted narrow-phone sanity pass
- no visible aurora geometry or clipping artifacts

After that acceptance:

- top monolith is frozen
- aurora placement/intensity is frozen
- greeting placement/type scale is frozen
- bottom monolith height/insets are frozen

Only regression fixes may reopen those decisions.

### Interaction-safety rule

Shell-owned gesture capture must not sit above the real interactive chrome.

Required layering rule:

- hamburger and new-chat stay directly tappable
- attach, input, and send stay directly tappable
- edge gesture capture may live only in non-interactive edge spill regions or below chrome
- do not rely on a full-width high-z overlay and hope pointer logic will preserve taps

Regression warning:

- if header/composer controls suddenly stop responding after a shell transplant, inspect gesture-layer z-order and hit coverage before tuning visual polish

### SIM-local fidelity constants

The empty-home shell must use one SIM-local fidelity layer rather than scattered magic numbers.

Current local fidelity owners:

- `SimHomeHeroTokens.kt`
- `SimHomeHeroShell.kt`

Current empty-home tightening pass:

- match prototype proportions before adding new structure
- prioritize island width/text scale, greeting type scale/offset, and bottom-monolith insets
- use monolith shadow shaping only as support for the locked shell silhouette, not as a new decorative layer

Current active-chat micro-slice:

- remove the duplicated active-chat shell chrome path rather than restyling chat bubbles first
- active chat must inherit the same top monolith, aurora floor, Dynamic Island, and bottom monolith as empty home
- only the center canvas should switch between greeting and conversation
- gesture restoration is a follow-up slice after shared shell chrome is stable

Current shell-edge cleanup micro-slice:

- remove the decorative shadow-band overlays below the top monolith and above the bottom monolith
- monolith edges must read as crisp seams into the center canvas rather than soft blurry borders
- aurora remains a center-canvas atmospheric floor, not a seam effect painted into the chrome boundary

Current gesture-restoration micro-slice:

- restore scheduler/audio open gestures on the unified shell chrome itself
- top monolith hosts the downward scheduler pull; bottom monolith hosts the upward audio pull
- do not reopen the old overlay strategy; the correct rule is observe motion at the chrome container and consume only after vertical-intent lock

Lock these categories locally before active-chat migration:

- top monolith height/padding/icon sizes
- Dynamic Island width/spacing/text scale
- greeting offsets/type scale
- bottom monolith height/insets/icon sizes
- aurora anchor positions/radius/alpha

### Pending lessons draft for post-fix sync

Do not sync `.agent/rules/lessons-learned.md` or `docs/reference/agent-lessons-details.md` until the user explicitly confirms this migration pattern is fixed/proven.

Draft lesson title:

- `High-Fidelity UI Migration Requires Surgical Transplant`

Draft lesson summary:

- broad high-fidelity migrations drift when shell chrome, center states, and behavior seams move at the same time
- the stable method is one visual source of truth, one accepted shell slice at a time, and frozen shell chrome before center-state expansion
- target screenshots are visual calibration, not permission to hardcode runtime copy/truth

---

### In-scope Compose surface

Implement only the SIM home/here shell covering:

- empty home
- active plain chat
- active chat with system sheet
- context-enriched chat
- pending/processing chat
- longer active session
- Dynamic Island states:
    - upcoming
    - conflict
    - idle

Out of scope:

- audio page
- scheduler page
- history page
- connectivity page
- settings page
- review-controls overlay from prototype

---

### Locked shell structure

Keep one stable shell across all states:

#### Top Monolith

- left: hamburger icon
- center: Dynamic Island
- right: new session +
- dark heavy monolith background
- no extra badge in default SIM active header

#### Center Canvas

- only state-changing region
- greeting canvas when empty
- chat/session content when active
- supports user bubble, assistant bubble, system sheet, context marker, processing sheet

#### Bottom Monolith

- left attach affordance
- center text input
- right action remains send-only; do not reinterpret it as a shell-owned mic route
- same visual weight in all states

---

### Locked visual primitives

#### 1. Dynamic Island

From prototype:

- borderless
- centered
- one-line only
- dot + text
- no pill chrome
- no secondary actions
- truncation-safe

States:

- upcoming = red chroma text + red glowing dot
- conflict = yellow chroma text + pulsing yellow dot
- idle = silver/neutral chroma text + subtle white dot

Do not:

- add background capsule
- make it multiline
- place controls inside it

#### 2. User Bubble

- right aligned
- compact
- blue accent
- pill-like radius with slightly sharper bottom-right tail edge
- no heavy shadow

#### 3. Assistant Bubble

- left aligned
- compact
- frosted/dark surface
- not full-width
- plain conversational role only

#### 4. System Sheet

- horizontal frosted band
- wider than normal bubble
- used for system-authored outputs
- visually distinct from assistant bubble
- should read as shell/system layer, not just “larger reply bubble”

#### 5. Context Marker

- lightweight inline contextual row
- sits inside chat flow
- not pinned banner
- not separate page chrome
- must not imply audio page takeover

#### 6. Processing Sheet

- in-chat progress/status block
- composer remains active
- not an agent-thinking card
- not a scheduler page fragment
- belongs in same message flow language as SystemSheet

#### 7. Greeting Canvas

- centered hero text
- gradient title
- muted subtitle
- same shell remains visible around it

---

### Locked dimensions and proportions

These should be translated faithfully, not reinvented.

From prototype:

- emulator basis approximates iPhone Pro aspect
- top monolith height: 64px
- top horizontal padding: 16px
- center canvas padding: 10px top/bottom, 16px sides
- chat-history gap: 10px
- user bubble max width: 80%
- user bubble padding: 10px 16px
- user bubble radius: pill-like with slightly sharper bottom-right corner
- assistant bubble max width: 85%
- assistant bubble padding: 10px 16px
- assistant bubble radius: 20px with slightly sharper bottom-left corner
- bottom monolith min-height: 56px
- bottom monolith padding: 10px 16px 16px 16px
- Dynamic Island max width: 240px
- island dot: 6px
- system sheet radius: 16px
- icon touch target minimum should still respect platform minimums even if visuals remain minimal

Compose note:

- preserve the proportions, not literal CSS pixels blindly
- use dp equivalents with visual parity as the goal

---

### Locked color/material intent

Map these into Compose tokens before implementation:

- app background: `#0D0D12`
- monolith background: `#020205`
- outgoing bubble blue: `#0A84FF`
- assistant/card surface: `rgba(28,28,30,0.65)`
- system sheet surface: `rgba(255,255,255,0.05)`
- glass border: `rgba(255,255,255,0.15)`
- subtle border: `rgba(255,255,255,0.10)`

Dynamic Island state hues:

- upcoming: `#FF8A84` -> `#FF453A`
- conflict: `#FFEB85` -> `#FFD60A`
- idle: `#FFFFFF` -> `#A0A0A5`

Aurora:

- blue blob: `rgba(10,132,255,0.28)`
- indigo blob: `rgba(94,92,230,0.24)`
- cyan blob: `rgba(100,210,255,0.20)`

---

### Locked motion/polish

Implement only if cheap and safe in Compose:

- subtle aurora drift
- placeholder scan shimmer
- conflict dot pulse
- soft state transitions between shell states

Do not over-animate.
This UI should feel:

- calm
- heavy
- premium
- restrained

---

### Behavior boundaries for Compose

The visual transplant must respect:

- shell stays the same across all included states
- center canvas swaps content only
- bottom composer remains visible in all included states
- Dynamic Island remains one-line and centered
- no support-surface expansion in this slice
- no audio drawer/page implementation in this slice
- no scheduler page implementation in this slice

---

### Review-only elements not to transplant

Do not implement:

- left-side review controls panel
- HTML state toggle buttons
- prototype-only browser cleanup hacks unless needed visually
- any artifact that exists only for prototype review

---

### Required Compose component breakdown

Recommended implementation units:

- `SimHomeHeroShell`
- `SimTopMonolith`
- `SimDynamicIsland`
- `SimCenterCanvas`
- `SimGreetingCanvas`
- `SimChatHistory`
- `SimUserBubble`
- `SimAssistantBubble`
- `SimSystemSheet`
- `SimContextMarker`
- `SimProcessingSheet`
- `SimBottomComposer`

---

### Acceptance rule

Compose transplant is acceptable only if:

- active plain chat visually matches the approved prototype closely
- empty hero still reads as same shell family
- system sheet is clearly distinct from assistant bubble
- Dynamic Island states remain centered, one-line, and chroma-based
- no extra chrome or support-surface clutter appears
- nothing drifts toward old dashboard / smart-agent shell styling
