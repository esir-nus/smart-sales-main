## V6-H1 hinter — mismatches (implementation wins)

- Role-contract priority text:
  - `docs/role-contract.md` 仍写 “V4 为唯一有效规范”，但仓库已经存在并使用 `docs/Orchestrator-MetadataHub-V5.md` 作为现行规范。
  - 本次实现不改动合约文档，仅记录差异，后续再做 docs-sync。
- Suspicious threshold wording:
  - 有些旧描述可能会写成 “gapMs >= threshold 才可疑”。
  - 实现采用 “short gap suspicious”：`gapMs <= suspiciousGapThresholdMs` 才标记为可疑边界（与 HUD/trace 输出一致）。
