# Scrim + Drawer Pattern (Compose)

When implementing modal overlays with scrims in Compose:

## Anti-Pattern
```kotlin
// WRONG: Scrim inside same AnimatedVisibility as drawer
AnimatedVisibility(visible = isOpen, enter = slideInVertically(...)) {
    Box {
        Scrim()  // This slides WITH the drawer!
        DrawerContent()
    }
}
```

## Correct Pattern
```kotlin
// RIGHT: Separate AnimatedVisibility for scrim and drawer
Box(modifier = Modifier.fillMaxSize()) {
    // 1. Scrim - FADES in/out
    AnimatedVisibility(visible = isOpen, enter = fadeIn(), exit = fadeOut()) {
        Box(modifier = Modifier.fillMaxSize().background(scrimColor).clickable { onDismiss() })
    }
    
    // 2. Drawer - SLIDES in/out
    AnimatedVisibility(visible = isOpen, enter = slideInVertically(...), exit = slideOutVertically(...)) {
        DrawerContent(
            modifier = Modifier.pointerInput(Unit) {
                awaitPointerEventScope { while (true) { awaitPointerEvent() } } // Consume events
            }
        )
    }
}
```

## Key Points
1. **Scrim and drawer need separate AnimatedVisibility** - they animate differently
2. **Scrim uses fadeIn/fadeOut** - not slide
3. **Drawer content must consume pointer events** - use `pointerInput` with event loop, not `clickable {}`
4. **Order matters** - scrim first (behind), drawer second (on top)

## Applied In
- `SchedulerDrawer.kt` (Top-Down)
- Should apply to `AudioDrawer.kt` (Bottom-Up) if similar issue exists
