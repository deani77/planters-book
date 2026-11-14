# Planters Book — Android data-layer review (November 2026)

## Observed implementation

Source inspected: `app/src/main/java/com/selendang3/app/data/EstateDatabase.kt`, `MainActivity.kt`, `features/production/ProductionScreen.kt`, `docs/data-updates.md`, and `docs/estate-zip.md`.

Room database: `estate.db`, schema version **2**.

| Table | Primary key | Stored data | Current role |
| --- | --- | --- | --- |
| `estate_fields` | `code` | alternateCode, hectares, palms, reportingYear, verified | Existing typed field table; not the primary source for current reporting screens |
| `sync_state` | `id` | lastSuccessEpochMillis | Legacy sync state; avoid treating it as proof of cloud synchronization |
| `estate_snapshot` | `slot` | revision, content (JSON), updatedAt, source | Snapshot slot 1 is current; slot 2 is previous backup |

Migration `MIGRATION_1_2` creates the snapshot table. `EstateApp` and the production report screen observe `EstateDao.observeSnapshot()` and parse the saved JSON. The app uses **local** ZIP/JSON/XLSX import, not a production server.

The snapshot contains seven flexible reference sections and optional reports. Consequently, **Room/SQLite is already present**, but these reports are not yet normalized into relational entities. The current full-snapshot import and previous-snapshot restore are important compatibility contracts.

## Decisions for upcoming Data Input work

1. **One authoritative source per datum.** Until an explicit migration is designed, keep the snapshot authoritative for dashboards and imported reports. Do not silently make `estate_fields` a competing source of field area, names, or codes.
2. **Repository boundary.** Introduce an `EstateRepository`/domain service for all reads and edits before adding forms. UI should never coordinate raw SQL updates with JSON snapshot revisions.
3. **Model stable record identities.** Agree estate IDs and field IDs before relationalizing production, weeding, manuring, and pest/disease entries. Existing `estate_fields.code` is not a multi-estate key; do not claim multi-estate support from the current schema.
4. **Preserve import semantics.** Every edit/import must update a validated authoritative representation atomically, with conflict detection and a recoverable previous version. A manual edit must not be silently overwritten by an older import.
5. **Retain legacy data.** Do not use `fallbackToDestructiveMigration`; create explicit Room migrations for new entities and test opening a v2 database containing actual records.
6. **Keep drafts distinct.** Existing production draft data is **not** included in estate ZIP backup/restore; document its storage and lifecycle before describing exports as full-device backups.
7. **Support null versus zero.** Missing measurements stay null; recorded zero remains zero, including through import, edits, database migration, and export.
8. **Protect private data.** Database files, exported operational data, and production drafts must not enter this public GitHub repository. Device backup and encryption policy require a separate review.

## Recommended staged implementation

**Stage A (this review):** Record the schema and invariants. No migration or UI change.

**Stage B:** Implement a repository/domain service over existing snapshot and current draft storage with explicit read, edit, import, revision and transaction contracts; add tests for optimistic concurrency and restore. Keep the external ZIP/JSON/XLSX contract unchanged.

**Stage C:** Add normalized tables through a versioned Room migration, beginning with estate/field identities, then field-level crop entries and the three upkeep workstreams. Keep a tested adapter for existing snapshot v1 imports.

**Stage D:** Build Data Input forms on top of those services. Read-only dashboards should consume the same authoritative repository.

**Stage E:** Add comprehensive backup/export (explicitly including or excluding local drafts and user settings), migration regression tests, and acceptance checks on real devices.

## Acceptance gate for Stage B/C

- Android v2 Room database with fields, snapshot, and previous snapshot survives upgrade.
- Current ZIP/JSON/XLSX import and backup restore continue to work.
- All dashboard calculations produce equivalent output for identical source data.
- Duplicate field/date operations and stale writes cannot silently replace newer data.
- UI and imports do not write competing versions of the same record.
- Offline use requires no server, network access, or user account.

This document is a source review and implementation contract, **not** a claim that normalized tables, repository service, data-entry forms, or multi-estate migrations already exist.
