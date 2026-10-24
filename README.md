# Planters Book

Offline estate management for individual planters, estate managers and smallholders, with native Android and iOS apps.

Set your estate name in **Settings → Estate Data Management → Estate name → Save Estate Name**. It is saved on this device and displayed on Home and Estate Details. Before setup, the heading reads **Your Estate**.

## Data ownership

Normal use is intended to require no VPS, account or internet connection. Users will import files from their device or enter their own records. Data is stored locally.

## Current implementation

- Android and iOS: Settings → Import Estate File opens the device file picker.
- Estate JSON files are validated before showing a replacement preview; confirmation saves the data locally.
- Imports are limited to 2 MB and must have a newer revision than the saved copy.
- Android also exports the saved estate JSON file.
- Local production storage models are inherited; manual entry forms still need to be exposed through Data Input.
- The Settings screens no longer request a server address or token.

This is an initial migration from Selendang-3-Apps, not a completed standalone release. The import schema accepts a nonblank estate identifier (up to 100 characters) and requires all seven reference sections. See [example format](sample-data/estate-update.example.json). CSV, Excel and Markdown import are not implemented.

Legacy server code, sync models, fixed field lists and documentation remain in the repository pending migration. The server is not needed for the new local estate import flow.

## Agreed next work

1. Complete estate and field setup forms.
2. Bottom navigation: Home, Estate, Field, Data Input, Tools, Settings.
3. All data entry and editing through Data Input.
4. Crop budgets and daily/weekly/monthly harvesting input; estate-level weeding and manuring input.
5. Dashboards and reports driven by local records.
6. Portable backup and restore; optional user-owned cloud backups later.

## Development

- [Android build instructions](docs/android-development.md)
- [iOS build instructions](ios/README.md)

Keep real estate records and private exports out of this public repository.
