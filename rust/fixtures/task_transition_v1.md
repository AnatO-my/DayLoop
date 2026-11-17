# Task transition wire fixture v1

This fixture defines a bounded little-endian byte protocol for the first Rust
JNI bridge. It is intentionally not a serialized Room entity: Kotlin keeps the
source task and applies only the returned status/completion fields after its
normal stale-state and transaction checks.

## Request (`DLT1`)

| Offset | Field | Encoding |
|---:|---|---|
| 0 | magic | ASCII `DLT1` |
| 4 | command | `1=complete`, `2=reopen`, `3=skip` |
| 5 | current status | `0=active`, `1=completed`, `2=skipped`, `3=someday` |
| 6 | ID bytes length | unsigned little-endian 16-bit, 1–1024 |
| 8 | title bytes length | unsigned little-endian 16-bit, 1–4096 |
| 10 | current completion | signed little-endian 64-bit UTC epoch ms; `-1` is absent |
| 18 | command instant | signed little-endian 64-bit UTC epoch ms |
| 26 | ID then title | exact UTF-8 byte counts; no trailing bytes |

Total input is capped at 6,144 bytes. A request with an unknown command, invalid
UTF-8, mismatched size, or invalid task ID/title is rejected.

## Response (`DLR1`)

Exactly 13 bytes: ASCII `DLR1`, resulting status byte, and resulting completion
as signed little-endian 64-bit UTC epoch milliseconds (`-1` when absent).

Example completion request for task `task-1`, title `Plan day`, at `1000` ms:

```text
44 4c 54 31 01 00 06 00 08 00 ff ff ff ff ff ff ff ff e8 03 00 00 00 00 00 00 74 61 73 6b 2d 31 50 6c 61 6e 20 64 61 79
```

Expected response:

```text
44 4c 52 31 01 e8 03 00 00 00 00 00 00
```
