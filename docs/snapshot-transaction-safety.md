# Item 2 — preserving imported data and previous snapshots

The Android `EstateRepository.commitReviewedImport()` now offers a transaction-safe write path for a previously reviewed `EstateImport`.

## Guarantees of the new method

- Checks that the saved JSON still matches the exact JSON used for preview (including when no prior snapshot existed).
- Validates the incoming snapshot again, checks revision and estate ID against the current saved snapshot.
- Saves the current snapshot to backup slot 2 before writing the new snapshot to slot 1.
- Performs both writes inside one Room transaction, rolling back both on failure.
- Retains schema v2, JSON representation, and the existing ZIP/JSON/XLSX formats.

## Integration status

**Not yet connected to import UI.** Existing import/restore controls continue using their established logic. Do not claim all writes have been centralized. Restore intentionally remains separate because it may legitimately restore an older revision.

## Verification gate before switching import UI

1. Add instrumented Room tests covering first import, valid update, stale preview, wrong estate, older revision, backup preservation and simulated write failure.
2. Locate every existing import and restore writer; replace only the validated import commit path and retain explicit rollback/restore semantics.
3. Test existing v2 databases and real-device backup/restore; confirm no loss of drafts or preferences.
4. Run Android unit tests, lint and instrumented database tests. Only then merge and consider using the repository write method in production.

No existing device data or VPS data is accessed by this PR. No APK is generated.
