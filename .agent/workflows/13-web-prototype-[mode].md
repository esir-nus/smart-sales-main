---
description: Enter Web Prototype Mode for visual-first UI development with phone emulator
---

# Web Prototype Mode

This mode establishes a **visual-first development workflow** where all UI changes are prototyped in a phone-shaped web emulator before Kotlin implementation.

---

## Mode Rules

### Rule 1: Single Persistent Prototype File
- **DO**: Update the existing `design_system_prototype.html` file.
- **DON'T**: Create new HTML files for each design iteration.
- **Location**: `/home/cslh-frank/.gemini/antigravity/brain/{current_session_id}/design_system_prototype.html`

### Rule 2: Phone Emulator Frame (NOT Full Page)
The prototype MUST render inside a phone-shaped container, not occupy the entire browser viewport.

**Device Presets:**

| Device | Width | Height | Aspect Ratio | CSS Border Radius |
|--------|-------|--------|--------------|-------------------|
| **iPhone 15 Pro** | 393px | 852px | ~9:19.5 | 44px |
| **Android 16:9** | 360px | 640px | 9:16 | 24px |

**Required HTML Structure:**
```html
<body style="background: #1a1a1a; display: flex; justify-content: center; align-items: center; min-height: 100vh;">
  <!-- Controls Panel (OUTSIDE the phone) -->
  <div class="controls" style="position: fixed; top: 10px; right: 10px;">
    ...
  </div>

  <!-- Phone Frame -->
  <div class="device-frame" style="
    width: 393px; /* or 360px for Android */
    height: 852px; /* or 640px for Android */
    border: 4px solid #333;
    border-radius: 44px; /* or 24px for Android */
    overflow: hidden;
    position: relative;
    box-shadow: 0 20px 60px rgba(0,0,0,0.5);
    background: #000;
  ">
    <!-- Status Bar (REQUIRED) -->
    <div class="status-bar" style="
      height: 44px;
      padding: 12px 24px 0;
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: 14px;
      font-weight: 600;
      color: white;
      background: transparent;
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      z-index: 100;
    ">
      <span class="time">9:41</span>
      <div class="notch" style="width: 126px; height: 34px; background: #000; border-radius: 0 0 20px 20px;"></div>
      <div class="icons" style="display: flex; gap: 4px; align-items: center;">
        <span>📶</span>
        <span>📡</span>
        <span>🔋</span>
      </div>
    </div>

    <!-- App Content Goes HERE (with top padding for status bar) -->
    <div class="app-container" style="
      width: 100%;
      height: 100%;
      overflow: hidden;
      background: var(--bg-color);
      padding-top: 44px;
    ">
      ...
    </div>
  </div>
</body>
```

### Rule 3: Visual Approval Gate
- **Default**: All design changes go to the web prototype ONLY.
- **Kotlin Implementation**: Requires explicit user command: "Approve for Kotlin" or "Implement in Android".
- **Verification**: Agent must capture a screenshot of the web prototype and request user approval before any `.kt` file changes.

### Rule 4: The "Compose Feasibility" Check
Before requesting approval, the Agent must answer:
- "Is this layout achievable with standard Compose Columns/Rows/Box?"
- "Do animations rely on CSS properties with no Compose equivalent (e.g. complex filter chains)?"

### Rule 5: Interaction > Static
- **DO**: Use CSS keyframes to show the *intent* of animations (e.g. breathing glow).
- **DON'T**: Submit static screenshots for dynamic features. Record a small clip or describe the motion.

### Rule 6: Registry Constraint
- **DO**: Only render elements listed in [`ui-element-registry.md`](file:///home/cslh-frank/main_app/docs/specs/ui-element-registry.md).
- **DON'T**: Invent new UI elements, buttons, or features not in the Registry.
- **Propose**: If a new element would improve UX, add it to the Registry's "UI Proposals Queue" and wait for approval.

---

## Entering Web Prototype Mode

When the user invokes `/web-prototype` or requests UI design work:

1. **Open/Update Prototype**: Modify the existing `design_system_prototype.html`.
2. **Navigate Browser**: Open the prototype file in the browser.
3. **Capture Screenshot**: Take a screenshot for user review.
4. **Report Changes**: Summarize what was visually changed.
5. **WAIT**: Do NOT touch Kotlin files unless explicitly approved.

---

## Device Selector & Debug Tools (Enhanced Template)

Add these controls to the `.controls` panel:

```html
<div class="controls">
    <select id="deviceSelector" onchange="setDevice(this.value)">
      <option value="iphone15">iPhone 15 Pro (393x852)</option>
      <option value="android16-9">Android 16:9 (360x640)</option>
    </select>
    
    <label><input type="checkbox" onclick="document.body.classList.toggle('debug-layout')"> Show Layout Bounds</label>
</div>

<style>
  .debug-layout * { outline: 1px solid rgba(255, 0, 0, 0.2); }
</style>

<script>
function setDevice(device) {
  const frame = document.querySelector('.device-frame');
  if (device === 'iphone15') {
    frame.style.width = '393px';
    frame.style.height = '852px';
    frame.style.borderRadius = '44px';
  } else {
    frame.style.width = '360px';
    frame.style.height = '640px';
    frame.style.borderRadius = '24px';
  }
}
</script>
```

---

## Anti-Patterns (What NOT to Do)

| ❌ Anti-Pattern | ✅ Correct Practice |
|-----------------|---------------------|
| Full-page prototype (no device frame) | Phone-shaped container with bezel |
| Creating `prototype_v2.html`, `prototype_v3.html` | Update the single `design_system_prototype.html` |
| Auto-implementing in Kotlin after prototype | Wait for explicit "Approve for Kotlin" command |
| Hardcoding 100vw/100vh for app container | Use fixed device dimensions (393px, 852px, etc.) |
| CSS grid magic that breaks on Android | Check "Compose Feasibility" first |

---

## Example Session

```
USER: Let's work on the Home Hero in web prototype mode.
AGENT: [Updates design_system_prototype.html]
AGENT: [Opens browser, captures screenshot]
AGENT: "Here's the updated Home Hero in the iPhone 15 Pro emulator. 
        Changes: Added Knot Symbol with breathing animation.
        - Compose Feasibility: ✅ Standard Canvas drawing, manageable.
        Ready for your review. Say 'Approve for Kotlin' to implement."

USER: Looks good. Approve for Kotlin.
AGENT: [NOW creates KnotSymbol.kt and related files]
```

---

## Cross-References

| Document | Purpose |
|----------|---------|
| `docs/specs/ui-architecture-v1.md` | Component structure for Kotlin implementation |
| `docs/specs/style-guide.md` | Design tokens (colors, spacing, typography) |
| `docs/plans/M2-ui-architecture.md` | Execution plan for Strangler Fig migration |
