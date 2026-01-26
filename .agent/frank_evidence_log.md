# Frank's Evidence Log

**Purpose**: Candidate patterns awaiting promotion to `frank_principles.md`

---

## Candidate Rules

<!-- Add new candidates at the top -->

### [CANDIDATE] "Automate Architectural Boundaries"
- **Date**: 2026-01-26
- **Context**: Prism isolation from legacy code — Frank worried about contamination
- **Evidence**: Created CI workflow to fail build if legacy imports detected in Prism modules
- **Frank's Insight**: Discipline is fragile, automation is reliable. If it's a rule, make it break the build.
- **Observations**: 1

### [CANDIDATE] "Business Time is Chunked"
- **Date**: 2026-01-26
- **Context**: Scheduler prototyping.
- **Evidence**: User requested "Fixed Week" tiles instead of rolling carousel.
- **Frank's Insight**: For productivity apps, users think in "Weeks" (batches), not "Days" (streams). Align UI with the mental chunking.
- **Observations**: 1

### [CANDIDATE] "Safe Area Math"
- **Date**: 2026-01-26
- **Context**: Content obscured by Notch in prototype.
- **Evidence**: User screenshot showing overlap.
- **Frank's Insight**: Always do the math for fixed overlays (Notch/Status Bar) before styling content.
- **Observations**: 1

### [CANDIDATE] "Bidirectional Spec-Interface Mapping Table"
- **Date**: 2026-01-26
- **Context**: Phase 1 skeleton was claimed 100% but missed 22% of spec items
- **Evidence**: Creating exhaustive mapping table revealed 9 missing interfaces, 5 missing data classes
- **Frank's Insight**: Before claiming skeleton complete, create table: Spec Section → Interface → Data Class → Repository. Prevents gaps.
- **Observations**: 1

### [CANDIDATE] "When unsure about file organization, measure token load, not line count"
- **Date**: 2025-01-25
- **Context**: Debating whether to split Prism-V1.md (1000+ lines)
- **Evidence**: Analysis showed ~15k tokens is manageable, splitting would scatter context
- **Frank's Insight**: God-file rules are for code, not specs
- **Observations**: 1

### [CANDIDATE] "Add TOC anchors instead of splitting large reference docs"
- **Date**: 2025-01-25
- **Context**: Same Prism file organization decision
- **Evidence**: TOC with line numbers enables grep + targeted reading
- **Frank's Insight**: Navigation > fragmentation
- **Observations**: 1

---

## Promoted Rules

*Rules that reached 3+ observations and moved to `frank_principles.md`:*

<!-- When promoting, move entry here with promotion date -->

---

## Rejected Candidates

*Patterns that seemed good but didn't hold up:*

<!-- Add with rejection reason -->

### [CANDIDATE] "Physics/Geometry Alignment"
- **Date**: 2026-01-26
- **Context**: Scheduler Drawer had Top-Drawer geometry (rounded bottom) but Bottom-Sheet physics (slide up).
- **Evidence**: User called it a "dumb mistake". Fixed by aligning animation (y: -100%) to geometry.
- **Frank's Insight**: Visual affordances must match physical behavior. If it looks like it hangs, it must slide down.
- **Observations**: 1
