# Planters Book

Offline estate management for individual planters, estate managers and smallholders, with native Android and iOS apps.

Set an estate name in Settings → Estate Data Management. The name and estate records are stored on this device.

## Data ownership

Estate information and production drafts are stored locally. Users supply estate records through local imports. The existing production draft storage is retained; exposing the full Data Input forms remains follow-up work. Normal use requires no VPS, account, server address, access token, or internet connection.

## Current implementation

- Android and iOS support offline ZIP, JSON and XLSX estate imports with validation, preview, a previous-snapshot backup and reviewed restore.
- Versioned ZIPs support full replacement or an exact-base merge of selected sections/reports, plus local export and system sharing.
- Android and iOS can create a blank XLSX template and import a filled workbook.
- Workbook sheets cover the existing seven estate reference tables and supported optional YPH, production, manuring, and budget report data.
- The previous estate snapshot is retained on-device when a new import is confirmed.
- Existing local production draft storage and data fields are preserved.

ZIP format, backup scope and Selendang-3 export compatibility are in [docs/estate-zip.md](docs/estate-zip.md). Workbook details and limits are in docs/data-updates.md. The sample JSON format remains available at sample-data/estate-update.example.json.

## Planned Data Input work

- Complete estate and field setup forms.
- Add the agreed Home, Estate, Field, Data Input, Tools and Settings navigation.
- Keep all editing in Data Input, with field reports read-only.
- Capture field crop records daily, weekly and monthly; capture weeding and manuring at estate level.
- Drive dashboards and reports from user records and support portable backup and restore.

## Development

- Android build instructions: docs/android-development.md
- iOS build instructions: ios/README.md

Keep real estate records and private exports out of this public repository.
