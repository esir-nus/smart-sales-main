# MetaHub Storage Layer

**Spec Reference:** Orchestrator-V1.md §4

## Purpose

Independent file storage for each Metadata Hub layer.

## Files

| Layer | Storage Class | JSON File |
|-------|--------------|-----------|
| M1 | `M1Storage.kt` | `m1_user.json` |
| M2 | `M2Storage.kt` | `m2_conversation.json` |
| M2B | `M2BStorage.kt` | `m2b_transcription.json` |
| M3 | `M3Storage.kt` | `m3_session.json` |

## Benefits

- Incremental write (only changed layer)
- Diffable (git sees which layer changed)
- Partial recovery (corruption isolated)
