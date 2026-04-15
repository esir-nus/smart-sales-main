# Session History

> **Cerb-compliant spec** — Session metadata persistence and History Drawer backend.
> **OS Layer**: SSD
> **State**: ✅ SHIPPED
> **Behavioral UX Authority Above This Doc**: [`docs/core-flow/base-runtime-ux-surface-governance-flow.md`](../../core-flow/base-runtime-ux-surface-governance-flow.md) (`UX.HISTORY.*`)

---

## Overview

The Session History module persists session metadata to Room and powers the History Drawer UI. It is **separate** from Session Context (Kernel RAM) — this module deals with **cross-session** metadata persistence, not per-session working sets.

**Key distinction**:
- **Session Context** = Kernel RAM (per-session workspace for pipeline processing)
- **Session History** = SSD (permanent session metadata for navigation UI)

---

## Architecture & Components

### 1. SessionEntity (Room)

Room entity storing session metadata.

```kotlin
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val sessionId: String,
    val clientName: String,          // 主联系人名称
    val summary: String,             // 摘要（6字以内）
    val timestamp: Long,             // 创建时间 (epoch ms)
    val isPinned: Boolean = false,   // 是否置顶
    val linkedAudioId: String? = null // 关联音频ID（分析模式）
)
```

**Mapping**: `SessionEntity` ↔ `SessionPreview` is 1:1 field mapping via `toDomain()` / `toEntity()` extension functions.

> **Field name**: Entity uses `sessionId` (PK), domain model uses `id`. The mapper handles this: `SessionPreview(id = entity.sessionId, ...)`.

### 2. SessionDao

```kotlin
@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY isPinned DESC, timestamp DESC")
    fun getAll(): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE sessionId = :id")
    fun getById(id: String): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: SessionEntity)

    @Update
    fun update(entity: SessionEntity)

    @Query("DELETE FROM sessions WHERE sessionId = :id")
    fun delete(id: String)
}
```

### 3. RoomHistoryRepository

Implements `HistoryRepository`. All methods delegate to `SessionDao`.

**Grouping Algorithm** (`getGroupedSessions()`):

```
Input:  All sessions from DAO (sorted by isPinned DESC, timestamp DESC)
Output: LinkedHashMap<String, List<SessionPreview>>

Groups (ordered):
1. "📌 置顶"     — isPinned == true
2. "📅 今天"     — timestamp within today (calendar day)
3. "🗓️ 最近30天" — timestamp within 1..30 days ago
4. "📁 YYYYMM"  — remainder, grouped by year-month of timestamp

Empty groups are omitted.
```

### 4. HistoryViewModel

```kotlin
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository
) : ViewModel() {
    val groupedSessions: StateFlow<Map<String, List<SessionPreview>>>

    fun togglePin(sessionId: String)
    fun renameSession(sessionId: String, newClientName: String, newSummary: String)
    fun deleteSession(sessionId: String)
}
```

**Behavior**:
- On init: load `groupedSessions` from repository.
- On each mutation (`togglePin`, `rename`, `delete`): call repository, then reload.
- Exposes `StateFlow` consumed by `HistoryDrawer` Composable.

---

## Domain Models

### SessionPreview (existing)

```kotlin
data class SessionPreview(
    val id: String,
    val clientName: String,
    val summary: String,
    val timestamp: Long,
    val isPinned: Boolean = false,
    val linkedAudioId: String? = null
)
```

> Already exists at `domain/model/SessionPreview.kt`. No changes needed.

---

## Integration Points

| Component | Change |
|-----------|--------|
| `PrismDatabase` | Add `SessionEntity` to `@Database(entities = [...])`, add `abstract fun sessionDao(): SessionDao` |
| `HistoryDrawer.kt` | Inject `HistoryViewModel` via `hiltViewModel()`, wire `groupedSessions` and callbacks |
| DI Module | Bind `RoomHistoryRepository` to `HistoryRepository` (replace `FakeHistoryRepository`) |

---

## Wave Plan & Status

| Wave | Focus | Status | Deliverables |
|---|---|---|---|
| **1** | **Persistence** | ✅ SHIPPED | `SessionEntity`, `SessionDao`, `RoomHistoryRepository` |
| **2** | **ViewModel** | ✅ SHIPPED | `HistoryViewModel`, `HistoryDrawer` wiring |
| **4** | **Auto-Renaming** | ✅ SHIPPED | `SimAgentChatCoordinator` first-real-reply title trigger, SIM title persistence, horizontal layout |
| **3** | **Session Creation** | ✅ SHIPPED | New session lifecycle (reset RAM + persist SSD) |

### Wave 3: New Session Lifecycle

When user taps "+" (new session):

```
1. AgentViewModel.startNewSession()
   ├── Clear UI: history, title→"新对话", input, uiState→Idle
   ├── Reset RAM: contextBuilder.resetSession()
   │   └── Clears _sessionHistory, _turnCount, creates new WorkingSet
   └── Persist: historyRepository.createSession("新对话", "新会话")
       └── Inserts row in sessions table with UUID + timestamp
```

**Key decisions**:
- **No auto-save of previous session** — current conversation persists via `memory_entries` (MemoryRepository), not via session table. Session table is metadata only.
- **`ContextBuilder.resetSession()`** needs to be exposed on the `ContextBuilder` interface (currently only on `RealContextBuilder` concrete class).
- **`AgentViewModel`** needs `ContextBuilder` and `HistoryRepository` injected.

**Interface change** (`ContextBuilder`):
```kotlin
interface ContextBuilder {
    // ... existing methods ...
    fun resetSession()  // NEW: Expose session reset
}
```

### Wave 4: Auto-Renaming

**Trigger**: When a SIM session that still uses an untitled placeholder (`SIM` / `新对话`) or a legacy audio filename title receives its first **real successful assistant reply**.

**Mechanism**:
1. `SimAgentChatCoordinator` watches the persisted SIM chat turns for an auto-title-eligible session.
2. It ignores shell-seeded audio/transcript intro copy and other non-conversational SIM system lines; for audio-grounded chat, the first user question and the first organic assistant answer define the rename source.
3. It calls the title generator prompt using that single assistant answer only and writes the returned short Chinese title back into `SimSessionRepository`.
4. If the first title-generation attempt fails transiently or yields a generic/bad title, the coordinator retries exactly once on the next successful organic assistant turn.
5. After a successful rename or after the single retry budget is exhausted, the auto-rename path stops for that session.

**Contract**:
- auto-rename is SIM-session-local behavior, not shared `IntentOrchestrator` JSON behavior
- the generator input is the first real successful assistant reply, not raw parsed JSON and not later chat history unless the single retry is consumed
- audio-grounded sessions may auto-rename too, but they must ignore seeded "已接入…" intro copy and instead use the first real assistant reply after the first user turn
- broad industry / role / persona labels such as `教育管理` are invalid title outputs and must not replace the placeholder title
- the rename remains metadata-only; it does not rewrite stored message content

---

## Verification Commands

```bash
# Build check
./gradlew :app-core:compileDebugKotlin

# Run history repository tests
./gradlew :app-core:testDebugUnitTest --tests "*RoomHistoryRepository*"

# Run history viewmodel tests
./gradlew :app-core:testDebugUnitTest --tests "*HistoryViewModel*"
```
