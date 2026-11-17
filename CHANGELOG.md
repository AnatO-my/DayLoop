# Changelog

## 0.2.0-beta.1 — 2026-11-17

Developer pre-release for device testing. This is not a store-ready build.

- Adds the current offline task, reminder, Focus, planning, Notes, Wardrobe,
  outfit, widget, guide and visual-system work present in the local repository.
- Adds Room migrations and schema exports through version 21; upgrade testing is
  still a release gate.
- Adds test-only ARM64 Rust transition fixtures and Kotlin/Rust differential
  validation. Kotlin/Room remain the production source of truth.
- Uses OFL-licensed Funnel Display for display typography and JetBrains Mono for
  readable body typography. Removes SF Pro, OCR A and Warping release assets.

### Known beta limits

- Calendar support depends on the device Calendar provider and granted runtime
  permissions; it is not cloud calendar sync.
- Wardrobe recognition uses optional ML Kit segmentation/image labels, not a
  custom trained clothing model. Saving remains user-confirmed and local.
- No cloud account, network wardrobe sync, or production Rust routing exists.
- Full backup/restore including media, broad accessibility validation and
  complete device coverage remain outstanding.
