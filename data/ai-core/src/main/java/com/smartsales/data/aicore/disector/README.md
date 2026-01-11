# Disector Module

**Spec Reference:** Orchestrator-V1.md §3.2.1, §10 (Appendix A)

## Purpose

Batch splitting for long audio files (>20 minutes).

## Key Components

- `Disector.kt` - Generates `DisectorPlan` from `totalMs`
- `DisectorPlan.kt` - DTO with batches, windows, overlap

## Rules (from Spec §10)

- ≤20 min → single batch
- >20 min → 10-minute chunks with merge/remainder logic
- 10s pre-roll overlap for speech continuity
