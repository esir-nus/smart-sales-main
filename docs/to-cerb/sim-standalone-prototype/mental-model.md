# SIM Standalone Prototype Mental Model (Historical Origin)

> **Purpose**: Preserve the original SIM product mindset as historical rationale for the later shared base-runtime direction.
> **Status**: Historical origin mindset
> **Current Reading Priority**: Historical origin context only; not current source of truth.
> **Current Active Truth**: `docs/specs/base-runtime-unification.md`, `docs/plans/tracker.md`, `docs/core-flow/sim-shell-routing-flow.md`, `docs/core-flow/sim-scheduler-path-a-flow.md`, `docs/core-flow/sim-audio-artifact-chat-flow.md`, `docs/cerb/interface-map.md`
> **Historical Companion Doc**: `docs/to-cerb/sim-standalone-prototype/concept.md`
> **Unification Authority**: `docs/specs/base-runtime-unification.md`
> **Related Audits**:
> - `docs/reports/20260319-sim-standalone-code-audit.md`
> - `docs/reports/20260319-sim-clarification-evidence-audit.md`
> - `docs/reports/20260331-base-runtime-unification-drift-audit.md`

---

## 1. One-Sentence Historical Product Truth

SIM is a standalone Prism-family app with two main lanes, scheduler and general SIM chat with optional Tingwu-based audio context, plus a decoupled connectivity support module.

It is not a smaller smart-agent OS.

Unification note on **2026-03-31**:

- current `SIM` naming should now be read as the best available **base-runtime baseline**, not as permanent permission for a second non-Mono product truth
- future non-Mono work should target the shared base runtime defined in `docs/specs/base-runtime-unification.md`
- deeper memory/entity/plugin architecture remains Mono-only
- the SIM-owned entry/runtime boundary remains real implementation isolation, but it must not be treated as a second non-Mono product truth

---

## 2. Product Posture

Think of SIM as:

- same product family
- much smaller runtime brain
- literal task product
- low-risk prototype

Do not think of SIM as:

- the smart app with switches turned off
- a hidden mode inside the agent shell
- a place to keep legacy architecture just because it already exists

---

## 3. Conversation Family Model

The SIM conversation model has two surfaces in one family:

1. `Discussion Chat`
   - directly available from the home surface
   - conversational by default
   - grounded in system persona, user metadata, and SIM-local session history
   - may stay plain chat or become audio-enriched mid-session

2. `Audio Drawer`
   - informational
   - read-only from a chat perspective while browsing
   - artifact browser and context picker for one selected audio at a time

Design rule:

- chat = start, continue, and remember within one SIM session
- audio drawer = browse, understand, and attach source context into chat

Not:

- audio drawer = separate smart workflow
- chat = general-purpose agent cockpit with tools or autonomous planning

---

## 4. Source-Led Intelligence Model

Tingwu is the source producer of transcription intelligence.

SIM may:

- display Tingwu output directly
- reorganize sections for readability
- polish noisy provider wording through a controlled prompt layer

SIM may not:

- invent missing facts
- fabricate absent sections
- present local formatting as if it were original intelligence authorship

---

## 5. Transparency Model

Transparent thinking in SIM is mostly a presentation contract.

Allowed:

- transcript reveal
- pseudo-streaming fallback
- activity labels such as transcribing, summarizing, chapters, speakers, highlights
- provider-native trace when available

Rule:

- presentation may feel intelligent
- presentation must not claim backend truth that does not exist

---

## 6. Module Model

SIM has:

- two main feature lanes
  - scheduler
  - audio transcription discussion
- one support module
  - connectivity

Connectivity is important, but it is not a third product lane.
It exists to keep the hardware contract alive without forcing smart-agent coupling.

---

## 7. Reuse Model

Reuse by contract, not by nostalgia.

Good reuse:

- UI skins
- drawer surfaces
- bridge contracts
- data persistence patterns that can be safely namespaced

Bad reuse:

- smart shell root
- smart agent runtime ownership
- smart-only memory assumptions
- legacy naming that hides actual ownership

---

## 8. Naming Model

Legacy names are not sacred.

Keep a legacy name when:

- it still matches current ownership
- it does not blur the SIM boundary

Rename when:

- the name still implies smart-agent ownership
- the name hides standalone responsibility
- a SIM-prefixed or prototype-owned name makes contamination harder

Industrial rule:

- rename for boundary clarity
- do not rename for style alone

---

## 9. Protected Product Rule

The current smart app is the protected product.

When tradeoffs appear:

- preserve smart-app stability first
- let SIM absorb the wrapper, adapter, namespace, or rename cost
- verify old assumptions against current code instead of inheriting them blindly

---

## 10. Historical Build Posture At The Time

Build SIM with this order:

1. mental model
2. tracker
3. core flow
4. Cerb spec/interface
5. implementation brief
6. code
7. acceptance

If a code shortcut violates the mental model, treat it as a drift candidate even if it compiles.
