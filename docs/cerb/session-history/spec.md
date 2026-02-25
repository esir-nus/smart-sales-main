# Session History

> **Cerb-compliant spec** — Session metadata persistence and History Drawer backend.
> **OS Layer**: SSD
> **State**: ✅ SHIPPED

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
| **4** | **Auto-Renaming** | ✅ SHIPPED | `SessionTitleGenerator`, `LlmSessionTitleGenerator`, PrismVM trigger, horizontal layout |
| **3** | **Session Creation** | ✅ SHIPPED | New session lifecycle (reset RAM + persist SSD) |

### Wave 3: New Session Lifecycle

When user taps "+" (new session):

```
1. PrismViewModel.startNewSession()
   ├── Clear UI: history, title→"新对话", input, uiState→Idle
   ├── Reset RAM: contextBuilder.resetSession()
   │   └── Clears _sessionHistory, _turnCount, creates new WorkingSet
   └── Persist: historyRepository.createSession("新对话", "新会话")
       └── Inserts row in sessions table with UUID + timestamp
```

**Key decisions**:
- **No auto-save of previous session** — current conversation persists via `memory_entries` (MemoryRepository), not via session table. Session table is metadata only.
- **`ContextBuilder.resetSession()`** needs to be exposed on the `ContextBuilder` interface (currently only on `RealContextBuilder` concrete class).
- **`PrismViewModel`** needs `ContextBuilder` and `HistoryRepository` injected.

**Interface change** (`ContextBuilder`):
```kotlin
interface ContextBuilder {
    // ... existing methods ...
    fun resetSession()  // NEW: Expose session reset
}
```

### Wave 4: Auto-Renaming

**Trigger**: When a **new session** (default title "新对话") receives its **first successful AI response**.

**Mechanism**:
1. `PrismViewModel` detects `sessionTitle == "新对话"` after a successful turn.
2. Calls `SessionTitleGenerator.generateTitle(history)`.
   - Uses `AiChatService` (lightweight model) to avoid pipeline contention.
   - Prompt: "Analyze conversation. Extract Client Name (or 'Unknown') and 6-char Summary."
3. Updates `SessionEntity` via `HistoryRepository.renameSession()`.
4. Updates UI `_sessionTitle`.

**Contract**:
```kotlin
interface SessionTitleGenerator {
    suspend fun generateTitle(history: List<ChatTurn>): TitleResult
}

data class TitleResult(
    val clientName: String,
    val summary: String
)
```

---

## Verification Commands

```bash
# Build check
./gradlew :app-prism:compileDebugKotlin

# Run history repository tests
./gradlew :app-prism:testDebugUnitTest --tests "*RoomHistoryRepository*"

# Run history viewmodel tests
./gradlew :app-prism:testDebugUnitTest --tests "*HistoryViewModel*"
```
