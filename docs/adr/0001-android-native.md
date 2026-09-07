# ADR 0001 — Android-first Native Stack

## Status
Proposed

## Context
The defining features depend heavily on Android alarm scheduling, notifications, lifecycle recovery, widgets and permission/capability behaviour. A native Android workspace also aligns with the intended use of Android Studio and Material design.

## Decision
Start with Kotlin + Jetpack Compose + Material 3 in Android Studio.

## Consequences
### Positive
- direct access to Android alarm/notification APIs;
- first-class Compose/Material support;
- easier platform-specific reliability testing;
- fewer plugin abstraction layers around the most critical feature.

### Negative
- iOS is not obtained “for free”;
- a later cross-platform port will require deliberate abstraction of domain concepts.

## Revisit when
The Android MVP is stable and there is evidence that iOS/cross-platform demand justifies a second client.
