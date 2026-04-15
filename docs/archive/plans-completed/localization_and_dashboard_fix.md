# Localization & Dashboard Fix Plan

## User Issues
1.  **Dashboard "Scheduler" Unclickable in Chat**: User reports inability to open Scheduler from the dashboard when in Chat interface.
2.  **Incomplete Localization**: English strings remain in `App.tsx` and `Dashboard`.

## Localization Targets

### `src/components/PrototypeDashboard.tsx`
-   `Reset Home` -> `重置首页`
-   `Onboarding` -> `引导页`
-   `System II (Orchestrator)` -> `分析模式`
-   `Scheduler` -> `日程安排`
-   `Prototype` -> `原型控制台`
-   `Navigation & State` -> `导航与状态`

### `src/App.tsx`
-   **System Messages**:
    -   "Good afternoon..." -> "下午好，Frank。准备好回顾 Q4 了吗？"
    -   "Constructing analysis context..." -> "正在构建分析上下文..."
    -   "Run deep analysis..." -> "对 A3 项目进行深度分析。"
    
-   **Plan Card**:
    -   "Analysis Plan" -> "分析计划"
    -   "RUNNING" -> "运行中"
    -   Step Labels: 
        -   "Extract audio key points" -> "提取音频关键点"
        -   "OCR Analysis (5 Images)" -> "OCR 图表分析 (5张)"
        -   "Synthesis Decision Model" -> "综合决策模型"
    
-   **Onboarding**:
    -   "Welcome to SmartSales" -> "欢迎使用 智能销售"
    -   "Setting up..." -> "正在初始化您的 AI 销售助手..."

-   **Input Overlay**:
    -   "Tap bar to simulate..." -> "点击输入栏模拟: [AI 请求 / 打开日程]"
    -   "ANALYST REQUEST" -> "发起分析请求"
    -   "OPEN SCHEDULER" -> "打开日程安排"

### `src/components/Header.tsx`
-   `SmartSales` -> `智能销售`

## Dashboard Logic Fix
-   **Hypothesis**: Z-Index or State Conflict?
-   **Action**: Ensure `handleDashboardNav('Scheduler')` explicitly forces `isDrawerOpen(true)` and perhaps clears conflicting z-index states?
-   **Fix**: Add `z-50` to the wrapper div of `InputBar` to ensure clickability if user meant Input Bar. If user meant Dashboard, `z-[9999]` should be enough.

## Execution
1.  Modify `PrototypeDashboard.tsx`.
2.  Modify `App.tsx`.
3.  Modify `Header.tsx`.
