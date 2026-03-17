---
name: smart-sales-code-audit
description: Evidence-based code audit and review for the Smart Sales repository. Use when Codex needs to audit unfamiliar code, review a change, compare spec versus implementation, check interface compatibility, inspect pipeline telemetry edges, or reduce hallucination risk before modifying code.
---

# Smart Sales Code Audit

Use this skill when the task is audit-heavy or when you need grounded repo evidence before changing code.

## Core Rule

Prove reality before making claims.

- Verify files and symbols exist.
- Read code before proposing changes.
- Compare literal spec requirements against literal code when alignment matters.
- Treat missing tests or ambiguous interfaces as real risk, not as paperwork.

## Hierarchy Rule

When a feature has a `docs/core-flow/**` document, audit with this order:

1. Core Flow
2. Spec
3. Code
4. PU Test

Do not treat “Core Flow is ahead of code” as a defect by itself.
Treat lower-layer drift as the first hypothesis unless the user says the Core Flow is obsolete.

## Audit Workflow

### 1. Identify scope

- Name the exact feature, file, class, or interface under review.
- List the concrete paths and symbols you expect to inspect.
- If the scope is vague, narrow it before deep analysis.

### 2. Run existence proof

Use fast repo searches first:

```bash
rg -n "TargetClass|TargetMethod|TargetString" .
rg --files | rg "Feature|Module|Test|interface|spec"
```

Never assume a file path, package, or symbol exists.

### 3. Read before judging

- Read the implementation, not just type names.
- Trace imports and adjacent collaborators.
- Check what the code actually returns and what states it can enter.

### 4. Check test reality

- Find the relevant tests.
- Note whether coverage is absent, partial, or meaningful.
- Flag happy-path-only tests and mock-heavy false positives.

### 5. Run the appropriate audit flavor

Pick the narrowest matching mode:

- General code audit: existence, logic, tests, risk
- Review mode: findings first, ordered by severity
- Core-flow alignment: compare behavior branches against the north-star flow
- Literal spec alignment: compare exact strings, states, labels, and patterns
- Interface alignment: compare method signatures, nullability, ownership direction, and module edges
- Pipeline valve audit: compare telemetry docs with actual code edges and logging points

### 6. Report only what is supported

- Separate facts from inference.
- Cite the file paths you actually inspected.
- If evidence is incomplete, say exactly what remains unknown.

## Review Output Rules

When the user asks for a review:

- Lead with findings, not summary.
- Prioritize bugs, regressions, architecture drift, and test gaps.
- Keep file references concrete.
- If there are no findings, say so explicitly and mention remaining risk.

## Literal Spec Alignment

Use literal comparison, not paraphrase, when auditing UI or contract implementation.

- Pull exact wording from the spec.
- Pull exact wording or structure from the code.
- Mark mismatches explicitly.

If the spec says a label, state, or grouping must be exact, synonyms do not count as aligned.

## Anti-Patterns

- Reviewing from memory
- Guessing package structure
- Calling code “aligned” without reading the spec section
- Treating green tests as proof when mocks bypass real gates
- Reporting only the first issue and hiding the rest

## Source References

Read `references/source-map.md` for the exact Antigravity workflows and rules this skill adapts.
