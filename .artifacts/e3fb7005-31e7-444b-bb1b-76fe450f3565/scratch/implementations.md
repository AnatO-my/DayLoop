# Implementation 1: Inline State-Driven Overlay

This approach uses a `Box` to layer a full-screen timer view over the standard `FocusScreen`. It uses `AnimatedVisibility` for the transition and `BackHandler` for navigation.

```kotlin
// FocusScreen.kt change
@Composable
fun FocusRoute(...) {
    var isFullscreen by rememberSaveable { mutableStateOf(false) }
    
    BackHandler(enabled = isFullscreen) {
        isFullscreen = false
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(...) { padding ->
            // Standard Focus Content
            // ...
            Text(
                text = session.mode.label,
                modifier = Modifier.clickable(enabled = session.endsAt != null) { 
                    isFullscreen = true 
                }
            )
        }

        AnimatedVisibility(
            visible = isFullscreen,
            enter = fadeIn() + expandIn(expandFrom = Alignment.Center),
            exit = fadeOut() + shrinkOut(shrinkTowards = Alignment.Center)
        ) {
            FullscreenTimerContent(
                session = session,
                onDismiss = { isFullscreen = false }
            )
        }
    }
}
```

# Implementation 2: Shared Transition Layout (Local State)

This approach uses `BoxWithConstraints` and manual interpolation of size/position to simulate a shared element transition. It focuses on the "Timer Card" itself expanding.

```kotlin
// FocusScreen.kt change
@Composable
fun FocusScreenContent(...) {
    var expanded by remember { mutableStateOf(false) }
    val transition = updateTransition(targetState = expanded, label = "Fullscreen")
    
    val padding by transition.animateDp { if (it) 0.dp else 24.dp }
    val cornerRadius by transition.animateDp { if (it) 0.dp else 28.dp }

    Surface(
        modifier = Modifier
            .fillMaxSize(if (expanded) 1f else 0.9f) // simplified
            .padding(padding),
        shape = RoundedCornerShape(cornerRadius),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        if (expanded) {
            FullscreenCircularTimer(...)
        } else {
            StandardTimerLayout(...)
        }
    }
}
```
