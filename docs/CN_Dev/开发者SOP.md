# 功能开发 SOP

> **原文链接**: [feature-development.md](../sops/feature-development.md)

> **目的**: 一套严谨、可重复的功能开发流程，确保与规范对齐、架构整洁、端到端流程完整。

---

## 适用场景

- 实现新功能（非 bugfix 或重构）
- 为现有模块添加重大能力
- M1+ 待办项

---

## 阶段一：理解 (UNDERSTAND)

**目标**: 写代码前先加载上下文。

| 顺序 | 文件 | 用途 |
|------|------|------|
| 1 | `docs/specs/GLOSSARY.md` | 术语表，禁止同义替换 |
| 2 | `docs/specs/prism-ui-ux-contract.md` | UI 索引 — 找到你的模块/流程 |
| 3 | 功能模块规范 (如 `HomeScreen.md`) | 组件布局、手势 |
| 4 | 功能流程规范 (如 `SchedulerFlows.md`) | 用户旅程、状态 |

**检查点**: 能否用一句话解释这个功能？

---

## 阶段二：验证需求 (VALIDATE)

**目标**: 编码前先画出端到端流程。

### 2.1 映射三位一体层

| 层级 | SOT 文档 | 问题 |
|------|----------|------|
| **大脑 (Brain)** | `Prism-V1.md` 相关章节 | 核心逻辑是什么？ |
| **灵魂 (Soul)** | `prism-ui-ux-contract.md` | UX 意图是什么？ |
| **躯体 (Body)** | 模块/组件规范 | 布局是什么？ |

### 2.2 端到端流程草图

用纸或 ASCII 画出用户旅程：

```
用户点击 → [组件] → [状态变化] → [UI更新] → 用户看到
```

### 2.3 澄清歧义

如果规范不清晰 → **先问**，不要假设。
如果规范有误 → **标记**并提出修改。

**检查点**: 端到端流程已绘制，三位一体层已识别。

---

## 阶段三：实现 (IMPLEMENT)

**目标**: 按整洁架构构建。

### 3.1 Box 创建顺序

```
1. Interface → 定义契约（做什么）
2. Fake → 测试替身（供消费者测试）
3. Impl → 真实实现
4. Hilt → @Binds 或 @Provides
5. Tests → 用 Fake 测试
```

### 3.2 基于证据编码

写代码前：
```bash
# 检查是否有类似代码
grep -rn "FeatureName" app-prism/

# 检查要遵循的模式
view_file docs/plans/tracker.md
```

### 3.3 Prism 清洁环境规则

> **Prism 代码禁止导入遗留代码。**

| ✅ domain/ 允许 | ❌ domain/ 禁止 |
|-----------------|-----------------|
| `kotlinx.*` | `android.*` |
| `javax.inject.*` | `feature.*` (遗留) |
| `dagger.hilt.*` | `com.smartsales.legacy.*` |

**检查点**: Interface + Fake + Impl + Hilt 绑定完成。

---

## 阶段四：验证 (VERIFY)

**目标**: 发布前确保质量。

### 必需审计

| 审计 | 工作流 | 时机 |
|------|--------|------|
| **Prism 验证** | `/cerb-check` | 实现后 — 全部3个门（注册表 → 合约 → 架构）|
| **Agent 可见性** | `/agent-visibility` | 如功能涉及 agent 活动 |

### 构建验证

```bash
# 构建通过
./gradlew :app:assembleDebug

# 测试通过
./gradlew testDebugUnitTest
```

**检查点**: 所有审计通过，构建绿色。

---

## 阶段五：发布 (SHIP)

**目标**: 记录并交付。

### 5.1 更新 Tracker

- 更新 `docs/plans/tracker.md` 功能状态
- 将项目从 🔲 移至 ✅

### 5.2 退出检查清单

```markdown
- [x] 功能端到端可用
- [x] /cerb-check 通过全部3个门
- [x] 构建 + 测试绿色
- [x] Tracker 已更新
```

---

## 快速参考

### 优秀功能的要素

| 支柱 | 检查点 |
|------|--------|
| **规范对齐** | `/cerb-check` 门2通过 |
| **流程完整** | 端到端旅程可用 |
| **架构整洁** | `/cerb-check` 门3通过 |
| **可见性** | Agent 表现智能 |
| **已测试** | 构建 + 单测绿色 |
| **已记录** | tracker 已更新 |

### 反模式

| ❌ 别这样 | ✅ 这样做 |
|-----------|-----------|
| 读一个规范就开始写代码 | 遵循5阶段顺序 |
| 跳过端到端流程映射 | 编码前先画流程 |
| 导入遗留代码 | 从 Prism 规范重写 |
| 跳过审计"节省时间" | 运行全部3个审计门 |
| 忘记更新 tracker | 最后一步更新 tracker |
