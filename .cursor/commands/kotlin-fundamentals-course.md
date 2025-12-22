# Kotlin 基础词汇 + 语法课程（LEGO + Python 对比版）

## 课程设计理念（参考英语学习经验）

**阶段 1：基础词汇 + 语法（80/20 原则）**
- 学习 50-100 个核心 Kotlin 概念（相当于 4000 英语单词）
- 掌握 10-15 个常见模式（相当于核心语法）
- 这些覆盖 80% 的日常代码阅读

**阶段 2：高强度实践**
- 在真实代码库中应用基础知识
- 边读边学，遇到新概念暂停解释

---

## 课程大纲（20 课，每课 10-20 分钟）

### 模块 1：基础词汇（Kotlin 核心语法）- 8 课
1. **变量与类型** - `val` vs `var`，类型声明（Python 对比）
2. **函数基础** - `fun`，参数，返回值（Python `def` 对比）
3. **空安全** - `String?`，`?.`，`!!`（Python `None` 对比）
4. **集合** - `List`，`Map`，`Set`（Python list/dict/set 对比）
5. **控制流** - `if`/`when`/`for`（Python 对比）
6. **数据类** - `data class`（Python `@dataclass` 对比）
7. **扩展函数** - `String.uppercase()`（Python 方法对比）
8. **密封类** - `sealed class`（Python `Enum` 对比）

### 模块 2：面向对象基础 - 4 课
9. **类与对象** - `class`，构造函数（Python `__init__` 对比）
10. **接口** - `interface`（Python `Protocol`/`ABC` 对比）
11. **继承** - `open class`，`override`（Python 继承对比）
12. **单例** - `object`（Python 模块级变量对比）

### 模块 3：函数式编程 - 3 课
13. **Lambda** - `{ x -> x * 2 }`（Python `lambda` 对比）
14. **高阶函数** - `map`/`filter`/`reduce`（Python 对比）
15. **作用域函数** - `let`/`run`/`apply`（Python 上下文管理器对比）

### 模块 4：Android/Kotlin 特殊概念 - 5 课
16. **协程基础** - `suspend`，`launch`（Python `async`/`await` 对比）
17. **Flow** - `Flow<T>`，`StateFlow`（Python `Generator` 对比）
18. **依赖注入** - `@Inject`，构造函数注入（LEGO 零件盒类比）
19. **注解** - `@Singleton`，`@JvmStatic`（Python 装饰器对比）
20. **泛型** - `<T>`，`List<String>`（Python `List[str]` 对比）

---

## 每课格式（统一）

A) **LEGO Map** - 今天学什么"积木"
B) **Python 对比** - 用你熟悉的 Python 解释
C) **概念解释** - LEGO 类比 + 简单语言
D) **代码库例子** - 在真实项目中找到这个概念的用法
E) **Tiny Task** - 5-15 分钟小练习
F) **Checkpoint Question** - 验证理解
G) **Next Lesson Preview** - 下节课预告

---

## 学习节奏建议

- **每天 1-2 课**（20-40 分钟）
- **每周复习**：周末回顾本周学的概念
- **实践应用**：学完模块 1-2 后，可以开始看项目代码

---

## 开始第一课

准备好后，我们从"变量与类型"开始！

