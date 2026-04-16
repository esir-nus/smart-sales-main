# Cross-Platform Changelog

> **Role**: Record of shared contract changes that affect platform implementations
> **Status**: Active
> **Date**: 2026-04-16
> **Primary Law**: `docs/specs/cross-platform-sync-contract.md`

---

## 2026-04-16

### [governance] Complete native HarmonyOS migration initiated
- **What changed**: HarmonyOS elevated from transient container experiment to primary forward platform. Branch model restructured: `platform/harmony` is now the Harmony integration trunk. Governance docs cleaned of DTQ references.
- **Affected platforms**: HarmonyOS
- **Domain models affected**: none (governance change only)
- **Migration action**: All new Harmony work targets `platform/harmony` branch. The complete native app scaffold now lives at `platforms/harmony/smartsales-app/`; future feature delivery translates shared contracts into that root.
