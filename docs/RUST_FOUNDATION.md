# Rust foundation

DayLoop's Rust migration starts as an Android-independent domain engine. The
existing Kotlin application remains the UI, lifecycle, Room/DataStore owner,
alarm scheduler, notification presenter, widget provider, and ML Kit adapter.
No user data path or Android identity changes in this slice.

```text
Kotlin DTO -> dayloop-ffi -> dayloop-domain / dayloop-engine -> DTO result -> Kotlin
```

`dayloop-domain` owns validated IDs, civil time, schedules, reminder-policy
constraints, and immutable task state transitions. It intentionally keeps civil
dates/local times distinct from UTC completion instants.

`dayloop-engine` owns pure commands only. Kotlin continues to persist the result
transactionally and then invokes Android effects through `AlarmCoordinator`.

`dayloop-ffi` now exposes the first bounded transition call for complete, reopen,
and skip. Its exact input/output vectors live in
`rust/fixtures/task_transition_v1.md`. Kotlin must map the returned fields back
onto the source task inside the existing transaction; it must not treat the wire
message as a Room replacement. Future JNI methods must accept bounded
buffers/handles, catch panics at the boundary, return typed failures, and never
invoke callbacks while a storage lock is held.

`RustTaskTransitionProtocol` is the Kotlin-side v1 mapper. It creates bounded
requests from a `Task` and command, rejects malformed or wrong-command replies,
and copies back only status and completion time. It has no native loading,
database, alarm, or UI dependency, so production behavior remains unchanged
until a separately verified transaction-level integration slice is approved.

The focused JVM verification for this mapper passed on 2026-11-10 via
`:app:testDebugUnitTest --tests com.dayloop.app.core.rust.RustTaskTransitionProtocolTest`.

The user-suggested Rune project is MIT-licensed, but its documented supported
targets are desktop/web rather than Android. It is therefore not a runtime
dependency of the Android migration. Its headless-render testing idea can be
evaluated later for a desktop companion.

## Verification

From `rust/` run `cargo test --workspace`. Android wiring deliberately waits
until Rust/Kotlin differential fixtures define the exact DTO contract. The
`buildRustArm64TestLibrary` Gradle task builds the arm64 Rust library with the
installed NDK and packages it only into instrumentation tests; `RustFfiLoadTest`
checks that Android can load it on arm64. It does not route production commands
through Rust. The library has been NDK-built and ELF-verified as AArch64. The
fresh instrumentation APK was installed on a physical `arm64-v8a` device, where
both `RustFfiLoadTest` cases passed: native-library loading and the complete
transition protocol fixture.

`RustFfiLoadTest` also contains a pending differential case for all three v1
commands. It sends the Kotlin mapper's request through the test-only JNI bridge,
requires a native success result, and compares the decoded Rust result with the
existing Kotlin transition. It is not connected to production mutation paths.

That differential case passed on a physical `arm64-v8a` device on 2026-11-10,
alongside the native load and fixture cases (three instrumentation tests total).
The next migration slice must preserve the existing Room transaction and Android
effect ordering while using Rust only as a checked transition calculation.

`RustTaskTransitionShadowAdapter` supplies that next boundary in disabled mode.
It accepts an already reloaded source task and a bridge response, then reports
match, divergence, rejected native status, or invalid response. It cannot write
Room or invoke Android effects; a later, explicitly enabled caller must retain
the existing transaction, stale-state check, and effect order.

Its focused JVM suite (`RustTaskTransitionShadowAdapterTest`) passed on
2026-11-10: disabled mode does not call the bridge, matching output is reported
without mutating the source task, and native rejection remains a no-write result.

`RoomAlarmStateStore` now owns an optional, disabled-by-default shadow-observer
seam. It receives a fresh Room task for complete, reopen, or skip immediately
before Kotlin writes the transition; it cannot supply a replacement task or
change the transaction result. The default observer is a no-op. A developer may
inject `InMemoryRustTaskTransitionDiagnostics` with the existing adapter to keep
a bounded process-local command/outcome history; it stores no task payload and
does not persist or upload telemetry. Bridge and linkage failures become an
in-memory `BRIDGE_FAILURE` outcome rather than escaping into the transaction.
`AlarmResolutionPersistenceTest` covers the seam against a real in-memory Room
database: complete, reopen, and skip each expose the fresh source status to a
fake observer while Kotlin retains ownership of the persisted final state.

The test-only JNI adapter is `rust/jni/dayloop_ffi_jni.c`. It maps one
`ByteArray` into the bounded Rust call and returns a four-byte native status plus
the fixed-size result. It is intentionally not part of the production source
set and has no Room/Android-service access. The JNI library and its Rust
dependency are deliberately C-only at the bridge level, avoiding a
`libc++_shared.so` packaging requirement.

## Capturing Gradle diagnostics

Use `scripts/capture-gradle.ps1` for an uploadable combined Gradle log. For
example, this captures the Kotlin mapper test without losing compiler details:

```powershell
& .\scripts\capture-gradle.ps1 -OutputPath .\app\build\rust-kotlin-mapper-test-output.txt -GradleArguments ':app:testDebugUnitTest', '--tests', 'com.dayloop.app.core.rust.RustTaskTransitionProtocolTest'
```
