# Smart Sales Product Requirements Document

> **Role**: Product north star
> **Status**: Active
> **Last Updated**: 2026-04-01
> **Primary Scope**: Current Smart Sales app experience
> **Companion Docs**:
> - [`docs/specs/base-runtime-unification.md`](./docs/specs/base-runtime-unification.md)
> - [`docs/core-flow/sim-shell-routing-flow.md`](./docs/core-flow/sim-shell-routing-flow.md)
> - [`docs/core-flow/sim-scheduler-path-a-flow.md`](./docs/core-flow/sim-scheduler-path-a-flow.md)
> - [`docs/core-flow/sim-audio-artifact-chat-flow.md`](./docs/core-flow/sim-audio-artifact-chat-flow.md)
> - [`docs/specs/flows/OnboardingFlow.md`](./docs/specs/flows/OnboardingFlow.md)

This document defines the product identity of Smart Sales from the user's point of view.
It exists to keep user logic ahead of business logic and business logic ahead of code logic.

This PRD owns:

- what the app is
- what the main app experience feels like
- which surfaces are fundamental to the product
- which user journeys are non-negotiable
- which product and UX laws must not drift during implementation

This PRD does not replace detailed flow, spec, interface, or visual-system docs.
Detailed behavior still belongs in `docs/core-flow/**`, implementation contracts belong in `docs/cerb/**` and `docs/cerb-ui/**`, and detailed UI law belongs in the current UI contract/style/registry docs.

---

## 1. Product Definition

Smart Sales is a calm, voice-first sales operating app for sales operators who come out of meetings with too much context to process, too many follow-ups to remember, and too little patience for CRM-heavy admin screens.

The product promise is simple:

- capture real-world sales activity with low friction
- turn that activity into usable review, follow-up, and scheduling surfaces
- help the user act on real context without forcing them into dashboard-heavy CRM behavior
- keep AI assistive, grounded, and recoverable instead of theatrical or autonomous

The app is designed for a user who needs to move from captured context to the right next action quickly, stay oriented between meetings and follow-ups, and trust the product to help without forcing them to babysit a complex system.

### What the product is

- a sales assistant centered on conversation, audio artifacts, and scheduler action
- a product that starts from real capture and review, then supports follow-up and execution
- a premium mobile experience where shell, chat, scheduler, audio, and onboarding feel like one coherent product family
- an AI-assisted product with clear boundaries around truth, action, and failure recovery

### What the product is not

- not a generic chat app with sales branding
- not a dashboard-first CRM that expects heavy manual field maintenance
- not an autonomous agent that silently mutates records or schedules on speculative reasoning
- not two different non-Mono products split into a SIM truth and a full truth
- not a plugin cockpit or internal tool console as the default user experience

---

## 2. Current Product Scope

Today, Smart Sales is one unified mobile app experience built around discussion, scheduling, and captured work review.
What users should understand now is not a family of separate tools, but one calm sales operating app that helps them capture work, understand what happened, and act on next steps without falling into CRM-heavy admin behavior.

Today's app experience includes:

- a home shell and discussion space the user can start from, return to, and work from throughout the day
- a scheduler lane for creating, moving, reviewing, and resolving work safely
- an audio and artifact review lane for understanding real captured activity
- onboarding and connectivity flows that get real capture working without chaos
- enough continuity that the user can leave and return without feeling the app lost the thread

Deeper intelligence may arrive later through additional memory, entity awareness, tool use, or richer assistance.
That later depth may expand what Smart Sales can help with, but it must not redefine the main app identity the user experiences today.

Product rule:

- users should experience one coherent Smart Sales app rather than different editions of the product
- implementation seams may remain during migration
- those seams must not turn into separate product truths

---

## 3. Definitive App Shape

The definitive Smart Sales app is organized around a small set of recognizable surfaces.
These surfaces define the product more than any internal architecture term does.

### 3.1 Home Shell and Discussion Surface

The home shell is the user's base camp.
It must feel calm, premium, and ready for immediate use.

This is where the user starts, returns, and regains orientation during the day.
It should feel like the safest place to resume work, review context, and decide what to do next.

It exists to:

- hold the main discussion surface
- expose the scheduler and audio lanes without mode confusion
- preserve continuity across new session, history, connectivity, and settings entry
- make the user feel they are inside one product, not jumping between unrelated tools

The discussion surface is a real first-class surface even before audio is attached.
It is not only a post-audio follow-up screen.
It must never be confused with a thin wrapper around captured audio or a secondary screen that only matters after transcription.

### 3.2 Scheduler Surface

The scheduler is a core execution surface, not a secondary utility.
Users come here to confirm what is scheduled, create or change work safely, and resolve conflicts clearly.
It is where the app should feel most dependable about what will happen next.

The scheduler must feel:

- fast enough for daily use
- grounded in clear and dependable task state
- visible about conflict and failure instead of silently dropping intent
- integrated with the shell rather than living as a separate calendar app

It must never feel like a detached calendar utility, an admin back office, or a sidecar tool outside the main product.

### 3.3 Audio and Artifact Surface

The audio drawer is a core informational surface.
It is where captured recordings become readable, reviewable artifacts and where audio context can be carried into discussion.
Users open this surface to understand what actually happened in real captured work before deciding what to ask, review, or do next.

This surface must feel like:

- a trusted review lane for real captured work
- the review counterpart of the discussion surface
- a durable place for transcript and artifact understanding
- a product surface driven by real badge ingress, not by fake local-file-first behavior

It must never feel like a transcript dump, a storage bin, or a debug-owned surface.

### 3.4 Onboarding and Connectivity Surface

Onboarding and connectivity exist to get the user into real capture with calm guidance and minimal chaos.
They are essential support surfaces, not the product's emotional center.
Users should only stay here long enough to get capture working, build confidence in the hardware ritual, and return to normal use quickly.

These surfaces must:

- teach the hardware ritual clearly
- keep setup recoverable and non-panic-inducing
- reuse the same premium visual family as the rest of the product
- help the user reach normal shell use quickly

They must never feel like a technical maze, a brittle provisioning flow, or the main reason the product exists.

### 3.5 Support Surfaces

History, new session, settings, and connectivity entry are valid support surfaces.
They help the user manage continuity and setup, but they must not overpower the main product lanes.
They exist to help the user return, reset, check setup, or recover continuity without turning support work into the center of the day.

Support surfaces are allowed to simplify compared with older versions of the app so long as the product remains coherent and trustworthy.
They must never become the hidden primary workflow or the place where core product truth quietly lives.

### 3.6 Ambient Feedback

The app should surface guidance, status, conflict, and recovery in a calm ambient way.
Ambient feedback exists to reassure, warn, or guide without interrupting the user's main task.

It should not behave like a noisy mascot-driven operating theater.
It should not become a second conversation layer competing with the main discussion surface.
It should not steal focus from discussion, scheduler work, or audio review.

Smart Sales should feel observant and helpful, not performative.

---

## 4. Core User Journeys

These journeys define the product at the user level.
Lower-layer docs must implement them, not reinterpret them.

### 4.1 Start and Enter the Product

The user opens the app and reaches a coherent home shell.
If setup is still required, the product routes through onboarding/connectivity with calm explanation and then returns to ordinary use.

Success criteria:

- the user understands where they are
- first-use setup does not feel like a generic Android wizard
- normal app use starts in a clear, premium, low-friction shell

### 4.2 Capture and Review Real Activity

The user records or syncs real meeting audio, then reviews transcript and artifacts in a readable surface.
The product treats captured context as real working material, not as decorative content.

Success criteria:

- review begins from audio inventory and artifacts
- already-transcribed audio reuses durable results
- pending audio can remain transparent and understandable while processing continues

### 4.3 Move From Review to Discussion

The user can turn captured audio into an active discussion thread through `Ask AI` or audio selection from chat.
That transition must feel continuous, not like opening a different product.

Success criteria:

- blank chat is still a valid starting point
- selected audio binds into the discussion thread cleanly
- ongoing processing remains visible without fake certainty or invented content

### 4.4 Schedule and Change Work Safely

The user can act on scheduling needs from the dedicated scheduler lane with clear and dependable task state.
The product must favor safe, visible action over magical ambiguity.

Success criteria:

- create, delete, and reschedule behavior remains available without requiring deeper intelligence
- conflicts are shown rather than silently discarded
- failure is specific and recoverable when the app is not sure what should be changed or when the requested action is unsafe

### 4.5 Return After Captured or Completed Activity

After captured activity, completion, or follow-up re-entry, the product should bring the user back through calm prompt-first continuity rather than a forced context hijack.

Success criteria:

- the product keeps the user's orientation
- follow-up remains attached to the correct task or captured context
- support signals do not override the main shell rhythm

### 4.6 Recover When the App Is Unsure

When the app is unsure, the product must clarify, retry, or safely stop instead of pretending confidence.
The user should see grounded recovery rather than speculative confidence.

Success criteria:

- missing certainty does not become silent mutation
- failure states remain calm, legible, and actionable
- the product preserves trust even when processing is incomplete or temporarily blocked

---

## 5. Product Laws

These laws govern downstream business rules and implementation choices.
If a lower doc or code path violates them, it is drift.

1. **User-facing journeys define the product**: architecture and implementation exist to support the journeys above, not to replace them.
2. **Human-in-the-loop truth for consequential action**: the product must not silently commit speculative destructive or identity-sensitive writes on behalf of the user.
3. **One shared non-Mono product truth**: shared shell, scheduler, audio, and onboarding behavior must not fork into separate product definitions across runtime variants, because users should not feel like they are moving between different editions of Smart Sales.
4. **Truth-owning surfaces stay explicit**: scheduler owns task truth, audio/artifact surfaces own artifact review, onboarding/connectivity own setup runtime, and support surfaces must not quietly take over those roles, because users should always know where to go for task truth, captured-work review, and setup help.
5. **Real ingress beats fictional convenience**: production audio truth comes from real badge ingress and durable processing; local import is testing convenience only.
6. **Support surfaces remain supportive**: history, settings, connectivity entry, and prompts may assist but must not become the hidden center of the app.
7. **Continuity beats mode theater**: transitions between chat, audio, scheduler, onboarding, and follow-up should feel like one product family rather than a collection of special modes.
8. **Calm failure handling is a feature**: conflict, ambiguity, offline limits, and processing delay must surface clearly and without panic styling or fake success.
9. **AI remains grounded and assistive**: the product may summarize, recommend, classify, or help the user act, but it must not invent unsupported business truth.
10. **Visual polish must not drift away from product truth**: premium styling is required, but it must reinforce the app's actual surface hierarchy and task model.

---

## 6. Definitive Look and Feel

Smart Sales needs a recognizable product body, not only a set of working screens.
This section defines the anti-drift product layer for that body.
Detailed pixel decisions still belong in the UI system docs.

### 6.1 Surface Hierarchy

The app should always read as one premium stack with clear hierarchy:

- shell/home as the stable frame the user can always trust
- discussion as the primary actionable conversational surface
- scheduler and audio as strong specialized lanes that are easy to reach and hard to confuse
- onboarding/connectivity as calm support flows that help the user get to real work
- history/settings as clearly secondary support surfaces

The user should not have to decode which surface is primary in the current moment.
The shell should always feel stable, discussion should always feel ready for action, and support surfaces should always read as secondary to the main task.

### 6.2 Visual Identity

The app should feel:

- dark-first, calm, and premium
- grounded rather than playful
- ambient rather than noisy
- intentional rather than dashboard-dense

Recognizable product invariants:

- one coherent shell family across empty, active, review, and setup states
- strong top-surface identity and stable bottom composer identity
- dark premium materials for chat, system, and artifact surfaces
- readable artifact presentation that feels product-owned, not debug-owned
- setup and recovery states that feel guided but never louder than primary work
- support surfaces that visually defer to discussion, scheduler action, and artifact review

What should never happen visually:

- setup should never visually overpower ordinary work
- failure should never dominate the screen with theatrical panic styling
- ambient guidance should never compete with the main task for attention
- support surfaces should never visually outrank the primary work surface
- artifact views should never feel like developer tooling or temporary debug UI

### 6.3 Interaction Identity

The app should behave with disciplined continuity:

- one drawer lane should not fight another
- transitions should preserve orientation
- prompt-first re-entry is preferred over forced context switching
- interaction feedback should be calm, legible, and trust-building

The main task should stay visually and behaviorally dominant in each moment.
Guidance, prompts, and recovery should assist the user without hijacking flow or creating mode-switch drama.

### 6.4 Pixel-Level Anti-Drift Rule

Pixel details matter, but they must stay attached to the same product body.
The PRD defines the surface identity and non-negotiable experience rules; the detailed pixel and component rules live in:

- [`docs/specs/prism-ui-ux-contract.md`](./docs/specs/prism-ui-ux-contract.md)
- [`docs/specs/style-guide.md`](./docs/specs/style-guide.md)
- [`docs/specs/ui_element_registry.md`](./docs/specs/ui_element_registry.md)
- owning `docs/cerb-ui/**` feature docs

If a future UI proposal looks polished but breaks the surface hierarchy, continuity, or trust model defined here, it is product drift.
Polish without clear hierarchy, calm action, and stable orientation is still product failure.

---

If Smart Sales stays calm, trustworthy, and action-ready in the user's real day, it is on-model.

## 7. Appendix: Downstream Authority Note

Use this document order when translating product intent toward implementation:

1. `SmartSales_PRD.md` for app identity, major surfaces, journeys, laws, and product-visible look-and-feel rules
2. `docs/specs/base-runtime-unification.md` for base-runtime versus Mono boundary
3. `docs/core-flow/**` for detailed behavioral north star
4. `docs/cerb/**` and `docs/cerb-ui/**` for implementation contracts and feature ownership
5. `docs/specs/prism-ui-ux-contract.md`, `docs/specs/style-guide.md`, and `docs/specs/ui_element_registry.md` for UI system rules
6. `docs/plans/tracker.md` and related trackers for campaign state and execution memory only

Practical rule: product identity starts here, details refine below, and lower layers must not silently redefine the user experience from the bottom up.
