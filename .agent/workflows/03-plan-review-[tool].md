---
description: Review agent's proposed plan with evidence-based verification and readiness scoring
---

# PlanReview Workflow

When an agent proposes an implementation plan, use this workflow to verify assumptions, gather evidence, and calculate execution readiness.

---

## Part 1: Assumption Audit (60% of readiness score)

List every assumption made in the plan and verify each one.

**Format:**
```markdown
### Assumption 1: [Statement]
**Verification Command**: `[exact command run]`
**Result**: VERIFIED / INVALIDATED / UNKNOWN
**Evidence**: [command output summary]

### Assumption 2: [Statement]
...
```

**Required minimum**: 3 assumptions verified with tool use

**Score Calculation**: (Verified / Total Assumptions) × 60

---

## Part 2: Evidence Quality Assessment (20% of readiness score)

Rate the quality of evidence gathered for the plan:

**HIGH (20 points):**
- 3+ tool verifications (grep, find, view_file)
- Read actual files from current codebase
- Checked recent commits/changes
- Verified against specs/docs

**MEDIUM (10 points):**
- 1-2 tool verifications
- Some reliance on earlier context
- Partial file reads

**LOW (0 points):**
- Mostly memory/assumptions
- Old context only
- No tool verification

**Evidence Checklist:**
- [ ] Searched for existing implementations
- [ ] Read relevant spec/doc sections
- [ ] Checked related files for patterns
- [ ] Verified file/class existence
- [ ] Checked for dependencies/imports

---

## Part 3: Risk Assessment (20% of readiness score)

Identify and rate risks:

**Blocking Unknowns:**
- List things you don't know that could invalidate the plan
- Example: "Unknown if TranscriptPublisher logic already exists elsewhere"

**Breaking Changes:**
- API changes affecting callers?
- Data structure changes requiring migration?
- Behavioral changes to existing features?

**Risk Rating:**
- **LOW RISK** (20 points): Well-understood, isolated changes, good test coverage
- **MEDIUM RISK** (10 points): Some unknowns, moderate scope, partial test coverage
- **HIGH RISK** (0 points): Major unknowns, cross-cutting changes, no tests

---

## Part 4: Readiness Report

**Readiness Score Formula:**
```
Readiness = (Assumption Score) + (Evidence Score) + (Risk Score)
          = (Verified/Total × 60) + (Evidence Quality) + (Risk Rating)
```

**Readiness Levels:**
- **100%**: Execute immediately, all assumptions verified, high evidence, low risk
- **90-99%**: Execute with minor clarifications, document known gaps
- **70-89%**: Significant gaps, need additional research before execution
- **<70%**: NOT READY - major rework or user input required

**Report Template:**
```markdown
## PlanReview Summary

**Readiness Score**: X% [READY / NEEDS RESEARCH / NOT READY]

### Breakdown
- Assumption Score: X/60 (Y verified, Z total)
- Evidence Quality: X/20 [HIGH/MEDIUM/LOW]
- Risk Assessment: X/20 [LOW/MEDIUM/HIGH]

### Confidence Statement
[Honest assessment: What makes you confident or uncertain?]

### Recommendation
- **100%**: ✅ Proceed with execution
- **90-99%**: ⚠️ Proceed with noted gaps: [list]
- **70-89%**: 🔍 Research needed: [specific questions/tasks]
- **<70%**: ❌ Rework plan: [major issues]

### Next Steps
[Specific actions: execute, research X, ask user about Y, etc.]
```

---

## Example Review

### Assumption 1: TranscriptPublisher needs to be created
**Verification Command**: `find /home/user/project -name "*TranscriptPublisher*"`
**Result**: INVALIDATED
**Evidence**: Found `TranscriptPublisherUseCase.kt` in data/ai-core layer (I/O logic). Domain publishing logic location still unknown - need further verification.

### Assumption 2: M3 requires new code
**Verification Command**: `grep -rn "formatTranscript" feature/chat/`
**Result**: UNKNOWN
**Evidence**: Publishing logic not found in expected locations. Need to check ViewModel for embedded logic.

---

**Readiness Score**: 65% [NOT READY]
- Assumptions: 0/2 verified = 0/60
- Evidence: MEDIUM = 10/20
- Risk: HIGH (unknowns about existing logic) = 0/20

**Recommendation**: 🔍 Research needed - locate transcript formatting logic before finalizing plan.

---

## Usage Tips

1. **Be honest**: LOW readiness is better than false confidence
2. **Use tools**: Every assumption needs a verification command
3. **Document gaps**: Unknown is better than wrong
4. **Ask user**: <70% readiness often means you need user input
