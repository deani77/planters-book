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

Home actual yields use recorded daily crop entries divided by estate planted hectares, excluding weekly/monthly summaries and future dates. Missing inputs are shown as an em dash. Estate views list records with their periods; they do not combine overlapping reporting periods.

## Design provenance and scope

Home follows the recovered [shared Home mockup](https://chatgpt.com/s/t_6ada2b1e08dc8191863e1b8c2fb3acb5): Estate Crop Production (mill crop, harvesting coverage and productivity), Weeding (circle/selective), and Manuring on a single scrolling page with pale green metric panels. The six-tab navigation follows the explicit app request rather than the mockup's earlier four-tab footer.

When no real estate exists, Home shows a clearly labeled, unsaved sample design preview matching the reference figures. For a real estate, actual crop yields and circle/selective weeding totals are calculated from available records. Other metrics remain blank until their dedicated inputs exist: budgets, harvesting hectares, harvester-specific attendance, bag quantities and fertilizer programmes. The reference provisionally defines Manuring “This Month” as monthly planned bags and MTD as applied bags. No kilogram-to-bag conversion is assumed.

## Remaining

Editing/deleting records, backup/export, cloud sync, advanced period reports/charts, richer agronomic profiles, localization, accessibility review, production branding, and release signing/distribution. Weekly/monthly periods are separate entries; rollups from daily data are not generated. SQLite is not encrypted. Uninstalling the app can remove local data.

See `VERIFICATION.md` for the actual checks performed and native build limitations.
