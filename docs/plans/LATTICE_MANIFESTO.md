# Lattice Architecture (V2) Manifesto

**Branch**: `Lattice`
**Objective**: Transition from 'Orchestrator V1' to the 'Lattice V2' architecture.

## Core Principles

1.  **Unified Scrolling Architecture**: No more nested scrolling conflicts. The entire screen logic adheres to a single unified scrolling parent.
2.  **Memory-First Navigation**: Navigation is driven by the `Metahub` memory graph, not just generic backstacks.
3.  **God-Object Purification**: Systematic decomposition of `TingwuRunner` and `HomeScreen` into discrete, testable components (Coordinators/Reducers).
4.  **Interface Segregation**: Strict adherence to the `lattice-interfaces.md` contract.

## Phase 1: Foundation
- [ ] Establish `Lattice` module structure
- [ ] Migrate `HomeScreen` to `LatticeHomeScreen` (Strangler Fig pattern)
- [ ] Implement `Metahub` core

## Rules of Engagement
- **No Big Bang**: Migration happens in parallel to V1, using feature flags where possible.
- **Evidence-Based**: Every refactor is backed by a "Why" and verification.
- **Vibe Compatible**: Code structure must remain accessible to AI agents (high locality, clear naming).
