# Planters Book

Offline-first Flutter/Dart MVP for Android and iOS. Records live in SQLite on the device; there is no server or account requirement.

## Run

Use Flutter 3.47.7 stable / Dart 3.13.5 or a compatible newer SDK.

```sh
flutter pub get
flutter run
```

Android requires the Android SDK, command-line tools and a connected device/emulator. iOS requires Xcode, an installed simulator runtime, and CocoaPods where plugin integration requires it. Configure your own signing team for physical iOS devices.

```sh
flutter analyze
flutter test
flutter build apk --debug
flutter build ios --simulator --debug
```

## Implemented

- Six tabs: Home, Estate, Field, Data Input, Tools, Settings.
- Read-only Home/Estate/Field views. Large rectangular field selection cards.
- Estate and field profiles: perimeter/planted hectares, planting year and calculated calendar age.
- Data Input: daily/weekly/monthly crop production; estate-only weeding/manuring; rainfall; workforce; costs; FFB quality and OER.
- SQLite versioned database, atomic batch imports, duplicate prevention and estate/field relationship validation.
- CSV, XLSX (one worksheet), Markdown pipe-table import. Select record type first. Validate required headers, required values, dates, non-negative numbers, percentages, planted area and duplicate rows. Review first 20 rows and error messages before confirming. Any invalid row blocks the import; reference/duplicate errors during saving roll back the entire transaction.
- Optional sample estate/field/production in Settings, labeled SAMPLE and excluded from Home totals. No sample data is loaded automatically.

## Import contract

See Tools inside the app for exact headers for every record type. Dates use YYYY-MM-DD. Crop period is `daily`, `weekly` or `monthly`; date is the period start. Units are hectares, tonnes, kilograms, millimetres, RM and percentages. IDs are case-sensitive.

Import estate profiles, then field profiles, then operations. `samples/estate.csv` and `samples/field.md` are fictional examples; these import as ordinary records with Sample in the names. Use the Settings loader for automatically flagged sample records.

Maximum input file: 5 MB, 10,000 data rows. Markdown support is pipe tables, not arbitrary prose or Markdown documents. Pipe characters inside cell values are not supported. XLSX import reads stored cell values, not formula evaluation. A workbook must contain exactly one worksheet.

One record per type/estate/field/date/period/role/category combination. Repeating that identity is rejected. For multiple costs in the same category/day or multiple estate activities per day, consolidate before importing. Profile IDs cannot be overwritten through imports.

Home sums only daily crop entries, excluding weekly/monthly summaries to prevent double counting. Estate views list records with their periods; they do not combine overlapping reporting periods.

## Design provenance and scope

The supplied request defines six tabs and the form list. Retrieved earlier user messages confirm large rectangular field cards, perimeter and planted hectares, and planting-year/age details. Earlier assistant replies were only unresolved references, so exact colors, fonts, and further proposed details could not be recovered. The current restrained green Material theme is an MVP assumption for review.

## Remaining

Editing/deleting records, backup/export, cloud sync, advanced period reports/charts, richer agronomic profiles, localization, accessibility review, production branding, and release signing/distribution. Weekly/monthly periods are separate entries; rollups from daily data are not generated. SQLite is not encrypted. Uninstalling the app can remove local data.

See `VERIFICATION.md` for the actual checks performed and native build limitations.
