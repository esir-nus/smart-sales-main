## V6-H1 hinter — mismatches (implementation wins)

> **Note**: `docs/role-contract.md` was archived on 2026-01-06. The only authoritative spec is now `docs/Orchestrator-V1.md`.

- Suspicious threshold wording:
  - 有些旧描述可能会写成 "gapMs >= threshold 才可疑"。
  - 实现采用 "short gap suspicious"：`gapMs <= suspiciousGapThresholdMs` 才标记为可疑边界（与 HUD/trace 输出一致）。
