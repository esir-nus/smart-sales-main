# Frank's Evidence Log

**Purpose**: Candidate patterns awaiting promotion to `frank_principles.md`

---

## Candidate Rules

<!-- Add new candidates at the top -->

### [CANDIDATE] "LLM Context Propagation"
- **Date**: 2026-01-30
- **Context**: User typed "1" to select first deliverable. LLM asked for clarification instead of executing.
- **Evidence**: Conversation e67fb9e3. ContextBuilder didn't include current deliverables, LLM had no list to reference.
- **Frank's Insight**: If user can see a list, LLM must see that list too. Context != history. Context = visible state. Quick selection patterns ("1", "运行") only work if LLM knows what "1" maps to.
- **Observations**: 1


### [CANDIDATE] "Screen Responsibility Locality"
- **Date**: 2026-01-27
- **Context**: Keyboard inset bug took 4 attempts to fix. ChatHeader was in PrismShell.kt, InputBar was in PrismChatScreen.kt. Scattered responsibilities.
- **Evidence**: Conversation 8a468281. Had to read TWO files to debug ONE screen's layout.
- **Frank's Insight**: All UI elements of a single screen belong in the same file (or extracted to components/). When fixing bugs, if you have to read N files to understand 1 screen, your architecture failed. Shell = drawer orchestration only, not screen elements.
- **Observations**: 1

### [CANDIDATE] "Literal Spec Alignment Audit"
- **Date**: 2026-01-27
- **Context**: History Drawer audit falsely claimed "Match" on grouping buckets. Agent summarized spec instead of comparing verbatim.
- **Evidence**: Spec said `今天, 最近30天, YYYY-MM`. Code said `今天, 昨天, 过去7天`. Agent marked it ✅.
- **Frank's Insight**: Never summarize specs. Create a Literal Comparison Table with exact spec strings vs exact code strings. Any mismatch is a HARD NO.
- **Observations**: 1

### [CANDIDATE] "Evidence-Based Extract vs Rewrite"
- **Date**: 2026-01-27
- **Context**: Phase 2 UX Layer plan — initial plan proposed rewriting ThinkingBox/PlanCard from scratch.
- **Evidence**: Senior review found 493 lines of tested code in `/archived/`. Grep verification showed package mismatch requiring rename but logic was sound.
- **Frank's Insight**: Before proposing a rewrite, grep for existing implementations. If alignment is good and coupling is low, EXTRACT. Don't let "archived" mean "forgotten".
- **Observations**: 1



### [CANDIDATE] "Icon-First Intelligence"
- **Date**: 2026-01-26
- **Context**: Scheduler Inspiration Card.
- **Evidence**: User rejected "Ask AI" label AND "Click to generate" helper text.
- **Frank's Insight**: True intelligence doesn't explain itself. A high-confidence icon (Sparkle) anchored to the top-right is sufficient. If you have to explain "Click to generate", the UI is weak. Trust the semantic weight of the icon.
- **Observations**: 2

### [CANDIDATE] "The Intrusive Minimap"
- **Date**: 2026-01-26
- **Context**: Scheduler Conflict Card Refinement.
- **Evidence**: User rejected text buttons ("Reschedule") in favor of a purely visual "Intrusive Icon".
- **Frank's Insight**: For high-friction states like conflicts, a "Traffic Light" (Icon/Color) is faster than text. Trust the user to explore (Unfold) for resolution. Don't clutter the timeline with buttons.
- **Observations**: 1

### [CANDIDATE] "The Static Mock Trap"
- **Date**: 2026-01-26
- **Context**: Auditing Scheduler functionality.
- **Evidence**: Prototype implemented a "Static Mock" for Conflict Resolution (hardcoded div) instead of the Logic-Driven UI (Radio Group) required by spec.
- **Frank's Insight**: Static mocks in advanced prototypes are dangerous. They look "done" but hide 100% of the complexity. Always demand "Logic-Driven UI" for core interaction paths like Conflict Resolution.
- **Observations**: 1

### [CANDIDATE] "Consolidated Truth"
- **Date**: 2026-01-26
- **Context**: Consolidating fractured specs (`scheduler-ui-spec.md`) back into the main contract.
- **Evidence**: `prism-ui-ux-contract.md` is now the Single Source of Truth.
- **Frank's Insight**: Fragmentation kills alignment. When details hide in side-files, the main contract drifts. Forcing everything into one readable doc exposes the rot.
- **Observations**: 1

### [CANDIDATE] "The DMZ Contract"
- **Date**: 2026-01-26
- **Context**: Decoupling Phase 2 (Logic) from Chunk G (UI).
- **Evidence**: `ui-alignment-table.md` acts as the single source of truth, allowing UI and Backend to work in parallel without blocking.
- **Frank's Insight**: Don't wire UI directly to Backend code. Wire both to a shared Table. If the Table is right, integration is O(1).
- **Observations**: 1

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

### [CANDIDATE] "Pragmatic Friction Reduction over Physics Polish"
- **Date**: 2026-01-27
- **Context**: Scheduler Drawer interaction felt "poorly designed". Choice between implementing complex `AnchoredDraggable` physics (Foundation 1.6+) vs simple Scrim Dismiss.
- **Evidence**: Senior review flagged `AnchoredDraggable` as overkill for skeleton. Implemented Scrim (10 lines) which solved the "reachability" friction immediately.
- **Frank's Insight**: "You're tracking pixels when you should be tracking intent. Tapping outside is the ultimate intent signal. Ship the scrim, defer the physics."
- **Observations**: 1

### [CANDIDATE] "Granular Interaction Audit"
- **Date**: 2026-02-02
- **Context**: Scheduler Review missed dead date click.
- **Evidence**: SchedulerCalendar.kt audit found missing clickable modifier.
- **Frank's Insight**: E2E flows verify paths; Granular Audits verify physics. You need both.
- **Observations**: 1
