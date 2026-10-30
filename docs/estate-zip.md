# Offline estate ZIP updates (version 1)

Settings → Estate Data Management accepts ZIP, JSON and XLSX by file contents. Nothing contacts a VPS. Select a file, review its estate ID, revision, operation and complete resulting tables/reports, then confirm. Cancel does not write. A different estate ID, duplicate/older revision, malformed package, or changed saved snapshot after preview is rejected. Use the same estate ID throughout an estate's revision sequence; the separately saved display name is unaffected.

## Replace and merge

**Replace** imports the complete snapshot. Empty tables and missing optional reports clear old values. Export Estate ZIP creates this format. Save it in Files/on-device storage, or use Share Estate ZIP (iOS: Prepare ZIP for Sharing, then Share Estate ZIP Privately). Sharing uses the system chooser; choosing a recipient/storage service is the user's action. The file is not encrypted. Share-cache copies are removed after 24 hours on the next sharing preparation; they may remain until then or until the OS clears the cache.

**Merge** replaces only explicitly selected whole sections and optional reports. All other saved values, including unknown extensions, survive. Flexible reference tables have no stable row IDs, so version 1 does not guess row identity, concatenate rows, or merge individual cells. The manifest must specify the exact saved `baseRevision`; the payload must have a higher `revision` and the same estate ID. The complete merged result is validated and previewed before saving. Empty selected table rows deliberately clear that section. A selected report must be present; explicit JSON `null` clears it. Unselected payload tables/reports are ignored. Unknown top-level fields in merge payloads are rejected; use full replacement to import new extensions.

Merge payloads still carry all seven sections in the existing schema. The incoming reporting period and export timestamp become the resulting snapshot metadata. Individual tables retain their own source dates and units; check these in the preview when combining periods.

## Backup and restore

Before saving, Android retains the previous snapshot in the same Room transaction as the new snapshot. iOS atomically writes the previous file before atomically replacing the current file; a backup read/write failure aborts the update. This is one local previous version, not a backup history. Review Previous Backup previews that version, then Restore Backup explicitly restores it, including its older revision. The current snapshot becomes the backup, allowing the restore to be reversed. The restore also rejects data changed since preview.

Export a ZIP before transferring devices or making important changes. On an empty device it initializes the estate. Normal import still rejects older/equal revisions; use Review Previous Backup for local rollback. Backup/export scope is the estate reference snapshot and optional reports. Production drafts, profile details, appearance and estate display-name settings are preserved on-device and are **not** included in ZIPs or rollback. This is not a complete device backup.

## Archive contract

Exactly two regular root files are allowed, with case-sensitive names: `manifest.json` and `estate.json`. No folders, extra attachments, duplicates, executable content, paths or nested ZIPs are accepted. ZIP archives are never extracted into caller-selected paths. Android uses a temporary private file for central-directory validation and removes it after reading; iOS reads in memory. CRC and SHA-256 detect corruption, not the author's identity or accuracy of figures. There is no signature/trust service in version 1.

Limits: 8 MB input archive, 16 KB expanded manifest, 2 MB expanded estate payload; existing bounds of seven unique sections, 1–20 unique columns per section, 10,000 rows total, matching row widths, 80-character column labels, 1,000-character text cells, finite numeric cells and explicit null/pending values apply. JSON is UTF-8. Use ordinary unencrypted single-volume ZIPs (stored or deflated entries); Android rejects ZIP64 containers. Unsupported versions, checksum/metadata mismatches, truncated ZIPs, invalid selections and oversize content are rejected before preview. Optional reports retain the existing platform-specific schema validation; source totals are not certified or recalculated by import.

Replacement manifest example (`sha256` must be the lowercase SHA-256 of the exact `estate.json` bytes, not this placeholder):

```json
{
  "format": "planters-book-estate",
  "version": 1,
  "payload": "estate.json",
  "sha256": "<64 lowercase hexadecimal characters>",
  "estateId": "fictional-estate",
  "revision": 2,
  "mode": "replace"
}
```

For a merge, change `mode` to `merge` and add all three:

```json
{
  "baseRevision": 1,
  "sections": ["fields", "rainfall"],
  "reports": ["manuringReport"]
}
```

Section choices: overview, fields, ages, soil, rainfall, abw, roads. Report choices: yph, cropProductionReport, cropProductionReportHistory, manuringReport, cropBudget, ffbRejectReport, fieldProfiles, cropHistory. Selection lists must be unique; at least one selection is required. Revision values are positive signed 64-bit integers. See [manifest JSON Schema](estate-zip-manifest.schema.json). Extra manifest fields are rejected so unsupported behavior cannot silently run.

## Selendang-3-Apps compatibility

The existing Selendang-3-Apps Export Estate Data JSON already uses `schemaVersion: 1`, `estateId`, `revision`, `reportingPeriod`, `exportedAt` and the seven-section snapshot format. That exported file can still be imported directly. To distribute it as a versioned ZIP without changing either app's operational records, run the standard-library-only packager offline:

```sh
python3 tools/estate_zip.py /private/path/estate-export.json /private/path/estate-update.zip
```

It preserves the payload byte-for-byte, including optional reports, unknown extensions, estate ID, units, revision, zero and null. It does basic envelope checks; the apps remain the final validation gate. It refuses to overwrite an existing output file. A prepared merge can be packaged with:

```sh
python3 tools/estate_zip.py /private/path/new-snapshot.json /private/path/update.zip \
  --base-revision 12 --sections fields rainfall --reports manuringReport
```

Only choose sections/reports you intend to replace. Do not pass raw production-service dumps, credentials, server configuration or a whole server backup: these are not estate snapshots. There are no network calls, source-system mutations or operational estate records bundled with this feature.

Compatibility inspected against `deani77/Selendang-3-Apps` main (`91f577137dd28520176d608dfc9f87d396603305`)'s documented schema-v1 export contract and this repository's native snapshot readers. This is a local implementation, not an upstream code port; no source branch/PR was copied. Planters Book implementation base: `8b6f4d3ada189cc756dc84ca8c8de2cf2a89f0d3`. Existing optional report content is preserved as raw JSON even where a platform does not display every field. New incompatible schemas require a versioned adapter, not field guessing.

## Validation

Kotlin unit tests cover ZIP/JSON/XLSX detection, export round trips, zero/null and extension preservation, merge selections, wrong estates/revisions/base snapshots, checksums, versions, truncated/oversize archives and invalid paths. Swift executable checks additionally exercise preview/cancel, disk-store persistence, stale preview rejection, previous backup restore and backup-write failure preservation. Python tests cover the offline compatibility packager and output-overwrite refusal. CI runs Android unit tests/lint and Swift checks/simulator compilation. APK creation remains manual-only; no APK is needed for these checks. File-picker/share-sheet interaction and physical-device testing remain separate acceptance checks.
