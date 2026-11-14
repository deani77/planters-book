# Item 3 — structured Android Room tables

This change introduces an **additive** Room schema v3 migration (`MIGRATION_2_3`). Existing `estate_fields`, `sync_state`, `estate_snapshot` and both snapshot slots remain intact.

## New tables

- `estate_identity`: stable estate ID and display name.
- `field_identity`: composite (estate ID, field ID), field code, estate foreign key.
- `field_crop_entry`: one field/date record with nullable harvested hectares, crop tonnes and bunches.
- `field_upkeep_entry`: one field/date/activity record with nullable area, quantity and unit. Activities will later be constrained by the domain layer to weeding, manuring and pest/disease.

Composite foreign keys protect field identity; composite primary keys prevent duplicate records of the same date/activity. Nullable quantities preserve missing-vs-zero semantics.

## Non-destructive boundaries

- No existing snapshot JSON is rewritten or migrated into the new tables.
- No existing UI reads or writes the new tables yet. The JSON snapshot remains authoritative until a separately reviewed import-to-relational mapping is complete.
- No data is copied from `estate_fields` because its field code alone cannot safely identify an estate.
- No `fallbackToDestructiveMigration` is used.
- No VPS or network dependencies are introduced.

## Release gates

Before shipping a database v3 APK, run an **instrumented migration test** using a populated schema-v2 database, including current and previous snapshots, and verify both remain byte-for-byte intact. Check Room's exported schema and verify all foreign-key constraints and indexes. Run Android lint/unit tests. The current PR provides the schema and migration only; passing JVM unit tests does not replace instrumented migration verification.

Item 2's repository write integration is still pending and must be completed before Data Input forms use these tables. Do not enable competing data sources.

## Subsequent work — requires user permission

Item 4: build Data Input forms after the transaction layer and v3 migration have been verified.
