## 📋 Review Conference Report: Pipeline Explainer Remediation Plan

**Subject**: Remediation Plan for `pipeline-explainer.md` and `unified-pipeline/spec.md` regarding RL Engine claims and file path drift.
**Panel**: 
1. `/01-senior-reviewr` (Chair) - Architecture sanity, documentation integrity, Antigravity best practices
2. `/06-audit` - Evidence verification, reality checking

---

### Panel Input Summary

#### `/06-audit` — Evidence Verification
- **Key insight 1**: The evidence gathered natively from `RealUnifiedPipeline.kt` and `IntentOrchestrator.kt` confirms the complete absence of a concurrent RL kick-off mechanism. The document contradicts the physical code reality. The proposed remediation of demoting the RL engine mentions to `[WIP]` or `[ASPIRATIONAL]` is strictly aligned with the Anti-Illusion protocol.
- **Key insight 2**: `RealInputParserService` and `RealEntityDisambiguationService` were successfully located in `core/pipeline/src/main/java/com/smartsales/core/pipeline/`. The remediation plan to fix the file links is factually accurate and required to prevent future 404s/hallucinations by agents reading the explainer. 

#### `/01-senior-reviewr` — Architecture & Documentation Integrity
- **Key insight 1**: "Documentation is a contract. If the docs claiming an RL engine is running in parallel when it isn't, we are literally lying to ourselves and creating 'Tech Debt Time Bombs'. The remediation to flag aspirational features clearly, or rip them out of the 'Explainer' until shipped, is non-negotiable."
- **Key insight 2**: "Updating the file paths is a basic hygiene task. Good catch. Stale paths break automated tooling like `grep` and `view_file`. The proposed remediation plan is clean, surgical, and restores the SOT (Source of Truth) to actual code reality."

---

### 🔴 Hard No (Consensus)
- We must **stop** documenting aspirational goals alongside physical implementations in the `pipeline-explainer.md` without clear `[WIP]` or `[ASPIRATIONAL]` tags. It violates the Anti-Illusion protocol and causes "Documentation Hallucination".

### 🟡 Yellow Flags
- `unified-pipeline/spec.md` also claims a "Parallel Kickoff" in the Wave 1 section. The remediation plan correctly identifies this. If it's a "Fake masquerade" (per tracker), Wave 1 should mark it as such or defer it to a later wave.

### 🟢 Good Calls
- Relying on the code as the ultimate truth over outdated spec docs (e.g., the `aliasesJson` FIFO history implementation).
- Prioritizing the removal of the RL Kickoff illusion from the Explainer.

### 💡 Senior's Synthesis
The proposed remediation plan (updating paths and clearly demoting/flagging the RL "Parallel Kickoff" claims) is exactly the pragmatic, evidence-based approach we need. It aligns with the Docs-First protocol and prevents future agents from drifting based on vaporware documentation.

**Verdict:** 100% Ready to Execute. The remediation plan is solid, safe, and urgently needed for contextual integrity.

---

### 🔧 Prescribed Tools
Based on this review, run these next:
1. `multi_replace_file_content` against `docs/artifacts/pipeline-explainer.md` — To update file paths and annotate RL sections as `[WIP]`.
2. `multi_replace_file_content` against `docs/cerb/unified-pipeline/spec.md` — To clarify that the Parallel Kickoff for RL is currently a Fake/Deferred feature in Wave 1.
3. `run_command` (git add / commit) — To safely persist the corrected documentation.
