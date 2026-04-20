# Active Lanes Registry

In-flight task registry for the declaration-first shipping contract. See `docs/specs/declaration-first-shipping.md`.

Each entry is appended when a task passes the pre-flight scope conflict check, and removed on successful `/ship` or via `/abandon <title>`.

## Format

```
- date: YYYY-MM-DD
  lane: android | harmony | docs
  title: <short title>
  scope:
    - <file or module glob>
    - <file or module glob>
  force_parallel_reason: <only if --force-parallel was used>
```

## Active Entries

_(none)_
