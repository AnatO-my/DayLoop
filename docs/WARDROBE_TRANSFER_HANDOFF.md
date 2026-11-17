# Wardrobe Transfer Security Handoff

## Scope and Reason
The old BackupManager used a shared password, logged encrypted output, stored only a task count rather than tasks, and omitted photos. Its restore changed preferences before validating the whole file and called a non-transactional delete-and-replace repository method. Earlier status notes overstated backup coverage and atomicity.

This slice replaces that exposed path with explicitly named wardrobe-details export/import. It is not full-app disaster recovery. Included: clothing IDs, names, categories, color/archive state, and item-linked outfit dates. Excluded: all photos and media metadata/links, photo-only outfit dates, tasks/descriptions, projects/goals, preferences, focus data, notes, guides and alarms.

## Implementation
- BackupCipher.kt: fixed versioned envelope `DAYLOOP-WARDROBE-1:`, random 16-byte salt and 12-byte nonce, PBKDF2-HMAC-SHA256 (600,000 iterations, 256-bit key), AES-GCM with 128-bit authentication tag and authenticated header. Header defines parameters; files cannot choose KDF cost. Reject legacy CBC format rather than silently downgrading. Password is user-selected, 12-256 characters, with confirmation on export.
- BackupManager.kt: IO-dispatched snapshot/JSON/encryption; validates exported payload with the same importer rules. Import authenticates, checks version/scope, record counts, IDs, required properties, categories/colors, canonical dates and all references before any writes. Future outfit dates are rejected against graph TimeProvider. No settings mutation or task-count placeholder.
- OutfitDao.kt: transactionally read wardrobe/outfits; import missing clothing and missing dates only. Existing IDs/dates win, even if incoming text differs. No REPLACE for parent rows, deletes, or photo-link changes. All new records roll back together on failure. Repeated import is idempotent. No schema version change required.
- WardrobeTransfer.kt: route-owned picker/dialog state survives lazy-list scrolling; system CreateDocument/OpenDocument, bounded 8MiB input, IO work, neutral errors, busy guards. Passwords are not rememberSaveable, logged or persisted; temporary CharArrays are cleared. Compose field Strings cannot be reliably zeroed; references are dropped after submit/dismiss. Export file bytes are retained only while awaiting the destination. Route/process loss requires a retry; no background promise is made. A failed output stream can leave an incomplete file, reported to the user.
- DiagnosticsScreen.kt: replace misleading full-backup action with Export/Import wardrobe details and explicit scope confirmation.

## Verification Status
Source/call-site review and scoped `git diff --check` completed. No Gradle, compilation, lint, tests, emulator or device validation run, respecting manual-validation preference.

Authored tests:
- BackupCipherTest: roundtrip/fresh randomness, wrong password, changed ciphertext, legacy/oversized input and short password.
- WardrobeBackupTest (instrumentation): repeated imports preserve existing details/date links/photo links, forced write failure rolls back clothing insertion, unknown references and unsupported payload versions reject.

## Next Single Action
Build and run those tests, then manually export to a file and import on a disposable test profile. Check cancel at both pickers/dialogs; mismatched/incorrect passwords; corrupted/oversized/legacy files; process interruption; repeated import; different local item/date edits; and preservation of existing photos. Do not clear real app storage based on this partial export.

## Deferred Work
Design full-app/media backup as its own reviewed recovery contract, with consistent database/files/preferences capture, photo integrity, schema/version compatibility, explicit restore semantics and alarm/widget reconciliation. The old OutfitRepository.restoreAll API remains unused by transfer and should be audited/removed independently; do not reconnect it. Secure exported files do not establish complete recovery coverage.
