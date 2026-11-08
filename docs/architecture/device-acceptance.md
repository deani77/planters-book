# Device acceptance before release

Run on Android phone/tablet and iPhone/iPad with fictional user-owned field identifiers. Record device, OS version, commit SHA and pass/fail evidence. Android connected instrumentation tests create APKs: run them only after an explicit APK request, following AGENTS.md. Routine PR checks remain unit tests/lint and iOS checks/simulator compilation.

1. Start empty: no invented estate totals, field codes or production values; a clear import action is available.
2. Import valid JSON/ZIP/XLSX, review every resulting section, cancel and verify no write; confirm and verify the displayed values and source date.
3. Open Field Details: ordered overview, retained alternate identifiers/planting date, age/soil/road records, imported profile/block facts and ABW are available for the selected field only.
4. Open crop reports: verify actual MTD/YTD, budgets, yield, achievement, reporting cutoff, prior-period comparison, harvesting rounds and both productivity units against the fixture. Measured zero stays zero; missing stays Pending. ABW remains in Field Details.
5. Navigate upkeep: three estate operations, field selection, six categories. Estate-wide manuring must never appear as an individual field's actual.
6. Change font size and use a narrow phone: all selectors and long record values remain readable and scrollable. Go back and reopen a field; selection remains valid after a new import.
7. Test file picker cancellation, unreadable/oversized/unsupported files, repeated import and stale preview. Original records must remain intact. Verify export and sharing with a chosen local Files destination; do not publish real records.
8. Restart after import/settings change. Verify the same estate name, source date, fields, map and drafts. Test previous-snapshot review/restore separately from full-device backup.
9. Full backup remains unreleased until PR #27's capture, reviewed restore, rollback and interruption checks are complete. A valid envelope alone is not proof of recoverability.

UI regression coverage is in CropReportVisibilityTest. It checks actual visible values rather than merely confirming that calculation functions return them. Swift's shared fixture checks the same report presentation groups used by its view. Physical picker/share and usability checks still require device evidence.
