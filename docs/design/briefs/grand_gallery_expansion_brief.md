## Design Brief: Grand Gallery Expansion (Dual-Theme Parade)

### User Goal
"Implement them all in the gallery's light theme and dark theme." + Specific content updates for V13-CH-J.

### Translation
Expand the gallery to be a **Bi-Modal Historical Archive**. Every variant (V12-CH-J) must be presented in a **Light/Dark Pair** side-by-side. Update mock content to match specific "Round 5/6/7" scenarios.

---

## Content Specifications (The "Script")

### B. V13: SMART AURORA (Contrast)
- **Item 1**: `客户访谈_李总.wav` | 14:00 | "关于 CRM 系统二次开发的详细需求..." | Status: 未转写
- **Visuals**: Dark Native Integration vs Light Native Integration (Warm Underlay).

### C. V14: SOLAR FLUID (Max Legibility)
- **Item 1**: `销售周会_20260112.mp3` | 周一 | "本周销售目标完成情况复盘..." | Status: 查看转写
- **Item 2 (Light)**: `Design_System_V2.fig` | High-Opacity Lens (85%)
- **Visuals**: Solar Fluid (High legibility).

### A. V15: SOLAR AIR (Unified Platter)
- **Item 1**: `Q4_年度预算审计.m4a` | 10:42 | "财务部关于Q4..." | Status: Unprocessed
- **Item 2**: `客户访谈_李总_V2.wav` | 昨天 | "李总对 'Deep Space' 主题的反馈..." | Status: Transcribed

### J. CHALLENGER J: DICHROIC FOIL
- **Item 1**: `Q4_Financial_Report.xlsx` | "Holographic Border • Silver Mirror"

---

## Execution Plan (Split Tasks)

### Wave 1: The Legacy Block (V12 & V13)
1.  **V12 (Legacy)**:
    -   Frame 1: V12 Dark (Deep Space)
    -   Frame 2: V12 Light (Frosted Ice)
2.  **V13 (Smart Aurora)**:
    -   Frame 3: V13 Dark
    -   Frame 4: V13 Light (Warm Underlay)
    -   *Update Content*: "客户访谈_李总.wav"

### Wave 2: The Fluid Block (V14 & V15)
1.  **V14 (Solar Fluid)**:
    -   Frame 5: V14 Dark
    -   Frame 6: V14 Light (High Opacity)
    -   *Update Content*: "销售周会...mp3", "Design_System_V2.fig"
2.  **V15 (Solar Air)**:
    -   Frame 7: V15 Dark
    -   Frame 8: V15 Light
    -   *Update Content*: "Q4_年度预算审计.m4a"

### Wave 3: The Future Block (V16 & CH-J)
1.  **V16 (Liquid Void)**:
    -   Frame 9: V16 Dark
    -   Frame 10: V16 Light
2.  **Challenger J (Dichroic)**:
    -   Frame 11: CH-J Dark (Obsidian)
    -   Frame 12: CH-J Light (Silver Mirror)
    -   *Update Content*: "Q4_Financial_Report.xlsx"

### Wave 4: V17 (The Gold Standard)
- Keep existing V17 Light/Dark frames as the anchor.

---

## Technical Constraints
- **Gallery Scroll**: Ensure horizontal scroll container handles 14 frames (~5600px width).
- **Classes**: Use `.light-mode-sim` class toggles for the Light frames to simulate theme within the dark gallery usage.

## Handoff
- Invoke `/ui-ux-pro-max` for each Wave sequentially.
