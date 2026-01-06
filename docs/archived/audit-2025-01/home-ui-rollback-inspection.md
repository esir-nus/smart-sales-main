# Home Chat UI 回滚检查报告

**检查日期**: 2025-12-28  
**当前分支**: `overhaul` (用户要求检查 `alignment` 分支，但当前在 `overhaul`)  
**检查类型**: 只读检查，无代码修改

---

## 1. Git 级别检查

### 1.1 当前分支状态

- **当前分支**: `overhaul`
- **目标分支**: `alignment` (存在但未切换)
- **未提交更改**: 
  - `data/ai-core/src/main/java/com/smartsales/data/aicore/AiCoreModule.kt`
  - `data/ai-core/src/main/java/com/smartsales/data/aicore/RealTingwuCoordinator.kt`
  - `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeDebugConfig.kt`
  - `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt`
  - `feature/chat/src/test/java/com/smartsales/feature/chat/history/ChatHistoryViewModelTest.kt`

### 1.2 最近提交历史

相关 Home UI 提交：
- `7a31f204` - feat: 优化导出流程并实现转录元数据 LLM 推理
- `286ddea4` - feat: 实现 IME 感知的焦点处理和拖拽关闭键盘
- `64c8047a` - fix: 修复 HomeScreen 编译错误并完善 HUD/技能行
- `c2e220f1` - feat: 重构 Home 布局与调试 HUD
- `159abed3` - feat: 简化 Home 布局并优化技能行
- `da1c42be` - feat: 重构 Home 空态与技能行布局

### 1.3 最近变更的文件

从 `git diff HEAD~5` 可见，`HomeScreen.kt` 的主要变更包括：
- 移除了 `heightIn` 导入
- 将 `Icons.Filled.Add` 和 `Icons.Filled.Menu` 改为 `Icons.Filled.History`
- 添加了 IME 相关的导入和拖拽关闭键盘逻辑
- 移除了 `BuildConfig.DEBUG` 检查，改用 `CHAT_DEBUG_HUD_ENABLED`
- 在 body 中重新添加了 `EntryCards`（设备管理/音频库卡片）

---

## 2. 当前 Home Shell 实现检查

### 2.1 Top App Bar (`HomeTopBar`)

**文件**: `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreen.kt:710-762`

**当前实现**:
- **标题**: 硬编码字符串 `"AI 助手"` (line 725)
  ```kotlin
  Text(
      text = "AI 助手",
      style = MaterialTheme.typography.titleLarge
  )
  ```
- **左侧图标**: `Icons.Filled.History` (历史记录按钮，line 744)
- **右侧控件**:
  - 设备连接状态 Chip（如果已连接，line 729-738）
  - 历史记录 IconButton (line 740-745)
  - **HUD 按钮**: `TextButton` 显示文本 "开启调试 HUD" / "关闭调试 HUD" (line 746-752)
  - 个人中心 IconButton (line 754-759)

**预期设计** (根据提交历史推断):
- 标题应绑定到 `state.currentSession.title`
- 左侧应为汉堡菜单图标（`Icons.Filled.Menu`）
- HUD 应为小圆点图标按钮，而非文本按钮
- 右侧应有 "+" 新会话按钮

### 2.2 Home Body 布局

**文件**: `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreen.kt:386-486`

**当前实现**:
- **EntryCards 存在**: `EntryCards` 组件在 body 顶部渲染（line 389-395）
  - 包含 "设备管理" 和 "音频库" 两个大卡片
- **空态内容** (`EmptyStateContent`, line 543-610):
  - 显示 LOGO 文本
  - 问候语 "你好，$userName"
  - "我是您的销售助手"
  - 要点列表（"我可以帮您：" + 两个要点）
  - "让我们开始吧"
  - **QuickSkillRow** 在空态底部（line 602-607）
- **非空态**: 
  - `LazyColumn` 显示消息列表
  - **QuickSkillRow 不在非空态的消息列表上方**（仅在输入区域，见 line 1140-1146）

**预期设计**:
- EntryCards 不应出现在 Home body（已移除）
- 空态应显示 hero + LOGO/问候/角色/要点/"让我们开始吧" + 统一技能行
- 非空态应在消息列表上方显示紧凑版技能行

### 2.3 输入区域 (`HomeInputArea`)

**文件**: `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreen.kt:1116-1221`

**当前实现**:
- **QuickSkillRow**: 当 `showQuickSkills = state.chatMessages.isNotEmpty()` 时显示（line 1140-1146）
  - 这意味着非空态时，技能行在输入区域上方
- **输入框**: `OutlinedTextField` 带焦点处理（line 1183-1208）
- **发送按钮**: `TextButton` (line 1209-1217)
- **上传菜单**: DropdownMenu 包含音频和图片选项（line 1161-1181）

**预期设计**:
- 底部输入区不应渲染技能行（技能行应在消息列表上方）
- 非空态时，技能行应在消息列表上方，而非输入区域

### 2.4 HUD & Debug Metadata

**文件**: `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreen.kt:746-752, 832-887`

**当前实现**:
- **HUD 触发**: `TextButton` 显示文本 "开启调试 HUD" / "关闭调试 HUD" (line 746-752)
  - testTag: `"debug_toggle_metadata"`
- **HUD 面板**: `DebugSessionMetadataHud` 在底部显示（line 521-528）
  - 显示 sessionId, title, mainPerson, shortSummary, title6, notes
  - **无复制按钮和关闭按钮**

**预期设计**:
- HUD 应为小圆点图标按钮（而非文本按钮）
- HUD 面板应包含：
  - DEBUG_METADATA 标题
  - 复制按钮（复制全部 HUD 文本）
  - 关闭按钮（同步关闭 HUD）
  - 可滚动内容

### 2.5 键盘/IME 行为

**文件**: `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreen.kt:326-342`

**当前实现**:
- **拖拽关闭键盘**: `dragDismissModifier` 使用 `detectVerticalDragGestures` (line 327-342)
  - 当输入框聚焦且向下拖拽时关闭键盘
- **IME padding**: 在 `HomeInputArea` 中使用 `navigationBarsPadding()` (line 1136)
- **windowSoftInputMode**: `adjustResize` (AndroidManifest.xml line 29)

**预期设计**: ✅ 符合预期

---

## 3. HomeScreenViewModel 检查

### 3.1 标题设置逻辑

**文件**: `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt:2426-2487`

**当前实现**:
- `maybeGenerateSessionTitle()`: 优先使用 MetaHub 的 `mainPerson` 和 `summaryTitle6Chars` (line 2431-2433)
- `updateTitleFromMetadata()`: 从 MetaHub 读取并更新标题 (line 2459-2487)
- `currentSession.title` 在 ViewModel 中正确更新

**问题**: ViewModel 正确设置了标题，但 UI (`HomeTopBar`) 未使用 `state.currentSession.title`，而是硬编码 "AI 助手"

### 3.2 Quick Skill 处理

**文件**: `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt:318-352`

**当前实现**:
- `onSelectQuickSkill()`: 正确处理 `SMART_ANALYSIS`, `EXPORT_PDF`, `EXPORT_CSV`
- `onExportPdfClicked()` / `onExportCsvClicked()`: 调用 `exportMarkdown()`

**预期设计**: ✅ 符合预期

---

## 4. AiFeatureTestActivity 检查

**文件**: `app/src/main/java/com/smartsales/aitest/AiFeatureTestActivity.kt`

**当前实现**:
- Home 通过 `HomeScreenRoute` 嵌入（line 103）
- 无硬编码标签 "AI 助手" 或 "开启调试 HUD"（这些在 HomeScreen.kt 中）
- 设备/音频入口卡片由 `HomeScreen` 的 `EntryCards` 控制，而非 Activity shell

**预期设计**: ✅ 符合预期（但 EntryCards 应被移除）

---

## 5. 总结：当前代码 vs 预期设计

### 5.1 Top Bar Title & Actions

| 项目 | 当前代码 | 预期设计 |
|------|---------|---------|
| **标题文本来源** | 硬编码 `"AI 助手"` (line 725) | 绑定到 `state.currentSession.title` |
| **左侧图标** | `Icons.Filled.History` (历史记录) | 汉堡菜单图标 (`Icons.Filled.Menu`) |
| **右侧控件** | 历史 IconButton + HUD TextButton + 个人中心 IconButton | 汉堡菜单 + "+" 新会话按钮 + HUD 小圆点 + 个人中心 |
| **HUD 按钮** | `TextButton` 显示 "开启调试 HUD" / "关闭调试 HUD" | 小圆点图标按钮 |

### 5.2 Home Body Layout

| 项目 | 当前代码 | 预期设计 |
|------|---------|---------|
| **EntryCards** | ✅ 存在（line 389-395） | ❌ 应移除 |
| **空态** | ✅ Hero + LOGO/问候/要点/"让我们开始吧" + QuickSkillRow | ✅ 符合预期 |
| **非空态技能行位置** | ❌ 在输入区域上方（line 1140-1146） | ✅ 应在消息列表上方 |
| **技能行重复** | ❌ 非空态时仅在输入区域 | ✅ 非空态时在消息列表上方，输入区域不显示 |

### 5.3 HUD Behavior

| 项目 | 当前代码 | 预期设计 |
|------|---------|---------|
| **HUD 触发方式** | `TextButton` 显示文本 (line 746-752) | 小圆点图标按钮 |
| **HUD 面板** | 仅显示 metadata 文本，无复制/关闭按钮 (line 832-887) | 包含复制按钮、关闭按钮、可滚动内容 |

### 5.4 Keyboard / IME Behavior

| 项目 | 当前代码 | 预期设计 |
|------|---------|---------|
| **IME padding** | ✅ 在 `HomeInputArea` 使用 `navigationBarsPadding()` | ✅ 符合预期 |
| **拖拽关闭键盘** | ✅ `dragDismissModifier` 实现 (line 327-342) | ✅ 符合预期 |

---

## 6. 受回滚影响的文件

### 6.1 `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreen.kt`

**具体回滚位置**:

1. **HomeTopBar (line 710-762)**:
   - Line 725: 标题硬编码为 `"AI 助手"`，应使用 `state.currentSession.title`
   - Line 744: 左侧图标为 `Icons.Filled.History`，应为汉堡菜单
   - Line 746-752: HUD 为 `TextButton` 显示文本，应为小圆点图标按钮
   - 缺少 "+" 新会话按钮

2. **Home Body (line 386-486)**:
   - Line 389-395: `EntryCards` 重新出现在 body 顶部，应移除
   - Line 1140-1146: 非空态时技能行在输入区域，应在消息列表上方

3. **HUD Panel (line 832-887)**:
   - `DebugSessionMetadataHud` 缺少复制按钮和关闭按钮
   - 面板不可滚动

4. **EmptyStateContent (line 543-610)**:
   - ✅ 符合预期（Hero + LOGO/问候/要点/"让我们开始吧" + QuickSkillRow）

### 6.2 其他文件

- `app/src/main/AndroidManifest.xml`: ✅ `windowSoftInputMode="adjustResize"` 正确
- `feature/chat/src/main/java/com/smartsales/feature/chat/home/HomeScreenViewModel.kt`: ✅ 标题设置逻辑正确，但 UI 未使用

---

## 7. 关键发现

1. **Top Bar 标题回滚**: 从动态 `state.currentSession.title` 回滚到硬编码 `"AI 助手"`
2. **EntryCards 重新出现**: 设备管理/音频库卡片重新出现在 body 顶部
3. **HUD 按钮回滚**: 从小圆点图标按钮回滚到文本按钮
4. **技能行位置错误**: 非空态时技能行在输入区域，而非消息列表上方
5. **HUD 面板不完整**: 缺少复制按钮和关闭按钮

---

**注意**: 本报告仅记录当前代码状态与预期设计的差异，不包含修复建议。

