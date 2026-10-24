# Estate data: VPS now, file import later

Android v0.2 Alpha supports the same complete snapshot through Settings → Import Estate Data or an HTTPS VPS download. Both paths preview the revision, period and row counts, then write the same Room database in a transaction. Offline reads do not require the VPS. Real records are never included in the public repository or APK.

## Phone workflow

1. Obtain a prepared JSON update from the estate administrator. Raw Excel and Markdown are source documents, not directly importable.
2. Open Settings → Import Estate Data, select the file, check its source, period and row counts, then confirm. This replaces all seven Estate Information sections, including empty sections.
3. Browse the sections offline. Null cells display Pending; measured zero is preserved as zero.
4. Use Export Estate Data to save the current snapshot privately before changing devices or hosts. An export can initialize another phone. The app also retains one previous snapshot internally, but does not yet expose a rollback button.

Updates require a strictly higher positive integer revision. Duplicate or older revisions are rejected, including at the database transaction boundary. Invalid files, failed downloads and cancelled previews leave the saved dataset intact. A revision orders snapshots, not individual rows; the administrator must carry forward all current data. Structural validation cannot establish the accuracy of estate figures. Resolve the known source discrepancies before publishing official updates.

## Shared file/API format

See [fictional example](../sample-data/estate-update.example.json). Format version is independent of app version.

- `schemaVersion`: integer 1.
- `estateId`: `selendang-3`.
- `revision`: increasing positive integer, preserved when exporting and migrating hosts.
- `reportingPeriod`: short human-readable description of the supplied records.
- `exportedAt`: ISO-8601 timestamp with timezone.
- `sections`: exactly one each of `overview`, `fields`, `ages`, `soil`, `rainfall`, `abw`, `roads`.
- Each section has `columns` (1–20 unique text labels) and `rows` (arrays with the same number of cells). Include units and reporting year in the labels or rows. Cells are text, numbers or null. No computed formulas, HTML or executable content is interpreted.
- Maximum 2 MB UTF-8, 10,000 rows in total, 1,000 characters per text cell. Pending sections use empty rows. The importer checks format and bounds; it does not recalculate or certify source totals.

This first format is for displaying estate reference tables. It does not yet populate production dashboards, charts, accounts or staff data-entry services. iOS remains a starter; its importer/sync implementation is a separate follow-up.

## VPS connection contract

The phone accepts an administrator-supplied full HTTPS address, for example `https://YOUR-DOMAIN/v1/estates/selendang-3/snapshot`, plus a privately issued read-only Bearer token. It sends GET with `Authorization: Bearer …` and `Accept: application/json`; the server returns the same JSON file with HTTP 200. It does not upload phone records. Redirects are refused, and ordinary TLS certificate verification remains enabled. Tokens are held only in memory for the Settings session; the non-secret address is saved on-device. They are never put in an APK, URL, public repository, export or log.

The private VPS service is configured separately; reusable code and operating instructions are in [server](../server/README.md). It supports HTTPS, per-user revocable read-only tokens, rate limits, administrator publication and daily local backups. Live credentials are kept outside this repository. Automatic off-server backups still need an external destination. Use an administrator-generated high-entropy read-only token for initial tests, not an account password. Do not expose the database directly. The current local user profile is not server authentication. Staff tokens can be issued or revoked through the server administrator's command line. A web administration/upload page, password-based accounts and automatic background synchronization remain unimplemented. Actual estate figures still require review before publication.

## When VPS service ends

Export the last verified complete snapshot, save it outside the VPS, and distribute it only to authorized staff. Continue publishing newer complete snapshots with increasing revisions for manual imports. File imports update the Last update label and do not falsely update Last VPS sync. Server failures do not clear cached data. If migrating instead, retain the domain, API format, estate ID and revision sequence. Restore backups and test the replacement server before switching DNS. The phone's local cache is not a backup strategy.

## Validation

CI runs package-parser unit tests, Android lint and APK compilation, then runs navigation, database migration and rejected-update preservation tests on an Android API 29 emulator. You can also run `./gradlew connectedDebugAndroidTest` on a connected device/emulator. Exercise a real file selection, server download and export on a phone before wider use. Do not claim the VPS is live or synchronized until a confirmed import succeeds.
