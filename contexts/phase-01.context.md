# Context Manifest — Phase 01

**Phase brief:** `../phases/01-foundation.md`

## Always read
- `../STATE.md`
- `../phases/01-foundation.md`
- `../LEARNED_CONCEPTS.md`

`AGENTS.md` is the standing repository contract and is assumed to govern the session.

## Read/inspect for this phase
- Gradle/settings/app scaffold files as created:
  - `../.github/workflows/android-foundation.yml`
  - `../scripts/verify-local.ps1`
  - `../settings.gradle.kts`
  - `../build.gradle.kts`
  - `../gradle.properties`
  - `../gradle/libs.versions.toml`
  - `../gradle/wrapper/gradle-wrapper.properties`
  - `../app/build.gradle.kts`
  - `../app/src/main/AndroidManifest.xml`
  - `../app/src/main/res/values/styles.xml`
  - `../app/src/main/java/com/dayloop/app/DayLoopApplication.kt`
  - `../app/src/main/java/com/dayloop/app/MainActivity.kt`
  - `../app/src/main/java/com/dayloop/app/di/AppGraph.kt`
  - `../app/src/main/java/com/dayloop/app/di/DefaultAppGraph.kt`
  - `../app/src/main/java/com/dayloop/app/di/LocalAppGraph.kt`
  - `../app/src/main/java/com/dayloop/app/time/TimeProvider.kt`
  - `../app/src/test/java/com/dayloop/app/time/FakeTimeProvider.kt`
  - `../app/src/test/java/com/dayloop/app/time/FakeTimeProviderTest.kt`
  - `../app/src/test/java/com/dayloop/app/time/SystemTimeProviderTest.kt`
  - `../app/src/test/java/com/dayloop/app/ui/theme/CalmPressureTokensTest.kt`
  - `../app/src/androidTest/java/com/dayloop/app/MainActivityTest.kt`
  - `../app/src/main/java/com/dayloop/app/ui/theme/Color.kt`
  - `../app/src/main/java/com/dayloop/app/ui/theme/Theme.kt`
  - `../app/src/main/java/com/dayloop/app/ui/theme/Tokens.kt`
  - `../app/src/main/java/com/dayloop/app/ui/theme/Type.kt`
- ADR 0001

## Read only if a specific decision blocks work
- ARCHITECTURE.md module-layout section only if a boundary is unclear

## Do not load or modify by default
- Future feature modules
- alarm implementation details
- Projects/Goals/AI

## Expansion rule
If a missing dependency blocks safe implementation, name the missing fact and inspect the smallest additional file/range needed. Do not scan the repository broadly.

## Stop rule
Complete only the current `STATE.md` action or agreed slice, verify it, update handoff files, then stop.