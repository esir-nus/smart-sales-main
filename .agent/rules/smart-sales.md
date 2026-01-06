---
trigger: always_on
---

# Smart Sales Project Context

Agent collaboration rules, documentation precedence, build commands, and coding style are defined in the canonical docs:

| Document | Purpose |
|----------|---------|
| [docs/README.md](file:///home/cslh-frank/main_app/docs/README.md) | **Master index** — Source of Truth hierarchy, navigation |
| [docs/AGENTS.md](file:///home/cslh-frank/main_app/docs/AGENTS.md) | **Agent guidelines** — Behavior requirements, code style, testing, commits |
| [docs/Orchestrator-V1.md](file:///home/cslh-frank/main_app/docs/Orchestrator-V1.md) | **V1 Spec** — Architecture, modules, contracts |

**Rule**: Always read the canonical docs above before acting. Do not rely on this stub for implementation details.

## Quick Reference (Summary Only)

- **SoT Hierarchy**: Orchestrator-V1.md > orchestrator-v1.schema.json > ux-contract.md > style-guide.md > code
- **Provider Default**: Tingwu + OSS (XFyun disabled by default)
- **Build**: `./gradlew :app:assembleDebug`
- **Tests**: `./gradlew testDebugUnitTest`
- **China Network**: Prefer Aliyun mirror
