# Project: ui-ux-polish

## Objective

Iterative UI/UX polish across modal surfaces and per-screen interactions. Baseline commit `620d3136f` (connectivity cards-first modal). Covers visual correctness, state fidelity, and interaction responsiveness — not architectural rewrites.

## Status

open

## Sprint Index

| # | Slug | Status | Summary | Contract |
|---|------|--------|---------|----------|
| 01 | connectivity-modal-state | done | Connectivity card reconnect now preempts stale operations, shows reconnecting on the selected card, and passes app-core unit verification. | [sprints/01-connectivity-modal-state.md](sprints/01-connectivity-modal-state.md) |

## Cross-Sprint Decisions

- Scope is Android (`develop` lane) unless a sprint explicitly targets Harmony.
- Evidence screenshots committed to `evidence/<NN>-<slug>/` within this project.
- No service-layer or data-layer changes: polish fixes are UI + ViewModel only unless a deeper bug is root-caused.

## Lessons Pointer

`.agent/rules/lessons-learned.md` — no entries yet for this project.
