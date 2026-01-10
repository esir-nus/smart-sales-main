---
trigger: always_on
---

# Smart Sales Project Context

Agent collaboration rules, documentation precedence, build commands, and coding style are defined in the canonical docs:

| Document | Purpose |
|----------|---------|
| [docs/README.md](file:///home/cslh-frank/main_app/docs/README.md) | **Navigation hub** — folder structure, doc index |
| [docs/AGENTS.md](file:///home/cslh-frank/main_app/docs/AGENTS.md) | **Agent guidelines** — behavior, code style, testing, commits |
| [docs/specs/Orchestrator-V1.md](file:///home/cslh-frank/main_app/docs/specs/Orchestrator-V1.md) | **V1 Spec** — architecture, modules, contracts |
| [docs/plans/tracker.md](file:///home/cslh-frank/main_app/docs/plans/tracker.md) | **Living tracker** — architecture status, milestones |

**Rule**: Always read the canonical docs above before acting.

---

## Quick Reference

- **SoT Hierarchy**: Orchestrator-V1.md > schema.json > ux-contract.md > style-guide.md > code
- **Build**: `./gradlew :app:assembleDebug`
- **Tests**: `./gradlew testDebugUnitTest`
- **Provider Default**: Tingwu + OSS (XFyun disabled)
