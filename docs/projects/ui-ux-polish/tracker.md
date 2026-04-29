# Project: ui-ux-polish

## Objective

Iterative UI/UX polish across modal surfaces and per-screen interactions. Baseline commit `620d3136f` (connectivity cards-first modal). Covers visual correctness, state fidelity, and interaction responsiveness — not architectural rewrites.

## Status

open

## Sprint Index

| # | Slug | Status | Summary | Contract |
|---|------|--------|---------|----------|
| 01 | connectivity-modal-state | done | Connectivity card reconnect now preempts stale operations, shows reconnecting on the selected card, and passes app-core unit verification. | [sprints/01-connectivity-modal-state.md](sprints/01-connectivity-modal-state.md) |
| 02 | audio-card-hold-state-fix | done | Audio card now shows "等待恢复传输…" + slow-pulse dim bar when badge is offline mid-download (disconnect/power-cut); logcat confirmed holding=1 during reconnect window; download resumes to active styling once bytes flow. | [sprints/02-audio-card-hold-state-fix.md](sprints/02-audio-card-hold-state-fix.md) |
| 03 | registry-gated-reconnect | authored | Guard session restore, auto reconnect, and BLE-detection reconnect so removed registry MACs cannot be dialed from stale state. | [sprints/03-registry-gated-reconnect.md](sprints/03-registry-gated-reconnect.md) |
| 04 | default-first-priority | blocked | Blocked by Sprint 03; after stale reconnects are guarded, BLE detection may prefer the default registered badge while preserving passive `setDefault()`. | [sprints/04-default-first-priority.md](sprints/04-default-first-priority.md) |

## Cross-Sprint Decisions

- Scope is Android (`develop` lane) unless a sprint explicitly targets Harmony.
- Evidence screenshots committed to `evidence/<NN>-<slug>/` within this project.
- No service-layer or data-layer changes: polish fixes are UI + ViewModel only unless a deeper bug is root-caused.

## Lessons Pointer

`.agent/rules/lessons-learned.md` — no entries yet for this project.
