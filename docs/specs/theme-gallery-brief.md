# Theme Gallery & Playground Specification

> **Status:** Draft
> **Author:** UI/UX Pro Max
> **Ref:** `prism-ui-ux-contract.md`

## 1. Overview
A specialized **Theme Gallery** within the web prototype to visualize, test, and present different aesthetic "skins" for the Smart Sales app. This "Playground" allows switching themes instantly and viewing core UI components side-by-side.

## 2. Technical Architecture (Theming Engine)
Moving from hardcoded Tailwind colors to **CSS Variable Strings**.

**Token Map:**
| Token | Description | Tailwind Ref |
|-------|-------------|--------------|
| `--bg-app` | Main background color | `bg-prism-bg` |
| `--bg-surface` | Card/Panel background | `bg-prism-surface` |
| `--bg-glass` | Translucent overlay | `bg-prism-surface/xx` |
| `--color-accent` | Primary action color | `text-prism-accent` |
| `--color-text-primary` | Main text | `text-gray-900` (remapped) |
| `--color-text-secondary` | Subtitles/Meta | `text-gray-500` (remapped) |
| `--border-base` | Hairline borders | `border-white/xx` (remapped) |

## 3. The 5 Themes

### A. Executive Platinum (Current/Default)
*A high-end, airy aesthetic inspired by modern iOS and premium SaaS.*
- **Background:** `#F2F2F7` (System Gray 6)
- **Surface:** `#FFFFFF` (White)
- **Accent:** `#007AFF` (System Blue)
- **Vibe:** Clean, Professional, Safe.

### B. Obsidian (Deep Dark)
*A true OLED black theme for night operations and battery saving.*
- **Background:** `#000000` (Pure Black)
- **Surface:** `#1C1C1E` (Dark Grey)
- **Accent:** `#0A84FF` (Vibrant Blue)
- **Text:** White / Gray-400
- **Vibe:** Technical, Stealth, Developer.

### C. Paper White (Minimalist)
*Inspired by E-Ink and Notion. High contrast, sharp lines, no blur.*
- **Background:** `#FFFFFF` (Pure White)
- **Surface:** `#F7F7F5` (Warm Gray)
- **Accent:** `#333333` (Carbon)
- **Radius:** `md` (Sharper corners)
- **Vibe:** Editorial, Focused, Clarity.

### D. Forest Focus (Calm)
*A biophilic design to reduce stress during high-pressure sales.*
- **Background:** `#F0FDF4` (Mint Cream) or `#ECFDF5`
- **Surface:** `#FFFFFF`
- **Accent:** `#059669` (Emerald 600)
- **Text:** `#064E3B` (Dark Green)
- **Vibe:** Natural, Balanced, Organic.

### E. Aurora Borealis (Futuristic)
*A dark, glass-heavy theme with vibrant glows. Cyberpunk meets Corporate.*
- **Background:** `#0F172A` (Slate 900)
- **Surface:** `rgba(30, 41, 59, 0.7)` (Glass)
- **Accent:** `#8B5CF6` (Violet) + `#F43F5E` (Rose) Gradients
- **Vibe:** Innovative, AI-Powered, Dynamic.

## 4. UI/UX: The Gallery Component

**Route:** `/gallery` (triggered via Dashboard)

**Layout:**
1.  **Header:** "Theme Studio" with a horizontal scroll list of Theme Pills.
2.  **Preview Stage (Scrollable):**
    - **Section 1: Typography:** "Heading 1", "Body text preview..."
    - **Section 2: Colors:** Swatches of the palette.
    - **Section 3: Components (Live):**
        - A "Chat Bubble" (Left/Right)
        - A "Scheduler Card" (Task/Inspiration)
        - An "Action Button"
        - An "Input Field"
    - **Section 4: Mini-mockups:** Small scaled-down frames similar to the Home Screen.

**Interaction:**
- Tapping a Theme Pill updates the CSS variables on the root `MobileFrame` instantly.
- Transition animations (fade) between themes.

## 5. Implementation Plan
1.  **Refactor CSS**: Move hardcoded colors in `tailwind.config.js` to `var(--...)`.
2.  **Create Themes**: Add `.theme-executive`, `.theme-obsidian`, etc., to `index.css`.
3.  **Build Gallery**: Create `src/components/ThemeGallery.tsx`.
4.  **Route**: Add to `App.tsx` and `PrototypeDashboard.tsx`.
