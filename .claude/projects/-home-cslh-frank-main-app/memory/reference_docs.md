---
name: Reference Key Docs
description: Pointers to key documentation locations including SOT chain, SOPs, platform overlays, and the handoff registry
type: reference
---

SOT chain (resolution order):
1. `SmartSales_PRD.md` — product north star
2. `docs/specs/base-runtime-unification.md` — base-vs-Mono boundary
3. `docs/core-flow/**` — behavioral north stars
4. `docs/cerb/**`, `docs/cerb-ui/**` — implementation contracts
5. `docs/specs/Architecture.md` — deeper system laws

Key specs:
- Interface ownership: `docs/cerb/interface-map.md`
- UX contract: `docs/specs/prism-ui-ux-contract.md`
- Style guide: `docs/specs/style-guide.md`
- UI element registry: `docs/specs/ui_element_registry.md`
- Code structure: `docs/specs/code-structure-contract.md`
- Platform governance: `docs/specs/platform-governance.md`
- Lane harness governance: `docs/sops/lane-worktree-governance.md`
- Lane registry: `ops/lane-registry.json`
- Glossary: `docs/specs/GLOSSARY.md`

SOPs:
- Feature development: `docs/sops/feature-development.md`
- Debugging: `docs/sops/debugging.md`
- UI building: `docs/sops/ui-building.md`
- Tracker governance: `docs/sops/tracker-governance.md`
- Lane worktree governance: `docs/sops/lane-worktree-governance.md`
- Lane ship commands: `scripts/lane status|commit|push|ship`

Platform overlays:
- Android: `docs/platforms/android/`
- Harmony: `docs/platforms/harmony/`
- Harmony container spec: `docs/platforms/harmony/tingwu-container.md`
- Harmony native dev framework: `docs/platforms/harmony/native-development-framework.md`

Handoffs:
- Registry contract: `handoffs/README.md`
- Dirty-tree lane ledger: `docs/plans/dirty-tree-quarantine.md`
- HarmonyOS co-dev guidebook: `handoffs/06649995-5028-4e40-8b26-6e76a133b110_HarmonyOS_Co-Devl_Guidebook.pdf`

Lessons learned (read on demand, do not duplicate):
- `docs/reference/agent-lessons-details.md`
- `.agent/rules/lessons-learned.md` (Antigravity-native, but contains useful project traps)
