# SIM Standalone Prototype Mental Model

> **Purpose**: Lock the product mindset for SIM so future docs and code changes do not drift back toward the smart app.
> **Status**: Active planning companion
> **Primary Product Doc**: `docs/to-cerb/sim-standalone-prototype/concept.md`
> **Related Audits**:
> - `docs/reports/20260319-sim-standalone-code-audit.md`
> - `docs/reports/20260319-sim-clarification-evidence-audit.md`

---

## 1. One-Sentence Product Truth

SIM is a standalone Prism-family app with two main lanes, scheduler and general SIM chat with optional Tingwu-based audio context, plus a decoupled connectivity support module.

It is not a smaller smart-agent OS.

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

## 10. Build Posture

Build SIM with this order:

1. mental model
2. tracker
3. core flow
4. Cerb spec/interface
5. implementation brief
6. code
7. acceptance

If a code shortcut violates the mental model, treat it as a drift candidate even if it compiles.
