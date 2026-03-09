---
description: Formalized workflow for generating comprehensive Trial Assessments and Contextual Context Reports
---

# 11-trial-assessment-[tool]
A **post-execution evaluation tool** for assessing the effectiveness of a newly adopted technical protocol, architecture pattern, or testing standard. 

This workflow enforces the creation of heavily contextualized reports that capture the exact state of the system *at the time of writing*, ensuring the report remains valuable months later.

> **Flow**: User requests assessment → `/11-trial-assessment` reads context → Drafts Contextual Report → Stores in `docs/reports/`

---

## When to Use

| Scenario | Use This? |
|----------|-----------|
| A new E2E protocol was just tested on a feature | ✅ Yes |
| Stage 1 of a major refactor (e.g., modularization) finished | ✅ Yes |
| Just completed a standard bug fix | ❌ No (Too small) |
| Routine feature development completed | ❌ No (Use standard walkthrough/PR) |

---

## The "Contextual Anchor" Mandatory Rule
> **Root Cause**: Future developers reading a report from 6 months ago cannot decipher "the test failed" without knowing *what* the test was or *which* framework was used at that exact moment.

Before drafting any assessment, the agent **MUST** capture and document the "Contextual Anchor".

### Phase 1: Contextual Anchoring
The agent must proactively gather and document:
1. **The Exact Git State / Timeline**: Date, current major wave/milestone (`tracker.md`).
2. **The Active Spec**: Which `spec.md` or `testing-protocol.md` was being trialed?
3. **The Target Reality**: What was the specific code or module being tested? (e.g., `RealLightningRouterTest.kt`).
4. **Environment Constraints**: Were there specific compiler flags, hardware limits, or SDK quirks active at the time?

---

## Phase 2: Drafting the Report

The agent must output a markdown file to `docs/reports/YYYYMMDD-[topic-slug].md` using the following exact structure:

```markdown
# Assessment Report: [Topic]
**Date**: YYYY-MM-DD
**Protocol/Spec on Trial**: [Link to the spec being tested]
**Target Implementation**: [The code that was subjected to the trial]

## 1. Contextual Anchor (The "State of the World")
*Describe the exact situation when this report was written. If an engineer reads this in 2 years, what must they know about the system's architecture at this exact moment?*
- E.g., "The system had just completed Stage 2 of Data Extraction, meaning the `core:database` was isolated, but Domain Fakes were not yet built."

## 2. Executive Summary
[TL;DR of whether the trial succeeded and the highest priority takeaway]

## 3. Drifts & Architectural Discoveries
*What did the trial prove was wrong about our previous assumptions?*
- **Assumption vs. Code Reality**: [Explain the disconnect]
- **Verdict**: [Is the code right, or is the spec right?]

## 4. Friction & Fixes
*What exact errors or environmental roadblocks were hit? How were they bypassed?*
- (Use a Table: Constraint/Error | Root Cause | Fix Applied)

## 5. Identified Gaps & Weaknesses
*What technical debt or missing infrastructure made this trial harder than it should have been?*
- E.g., "Mocking boilerplate was intense because we lack a `:core:test-fakes` module."

## 6. Advice to the Consul (Strategic Next Steps)
*Pragmatic, prioritized recommendations for engineering leadership.*
1. [Highest priority infrastructure build]
2. [Process change]
3. [Architecture warning]
```

---

## Phase 3: Finalization
1. Ensure the report is saved directly to `docs/reports/` with a descriptive, date-prefixed filename.
2. Update `docs/reports/index.md` to include a link to the new report with a 1-sentence summary.
3. Serve the report to the user via UI or notify_user.
