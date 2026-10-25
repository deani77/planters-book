# Planters Book

Offline estate management for individual planters, estate managers and smallholders, with native Android and iOS apps.

Set an estate name in Settings → Estate Data Management. The name and estate records are stored on this device.

## Data ownership

Estate information and production drafts are stored locally. Users can enter records in the app or prepare their own workbook and import it. Normal use requires no VPS, account, server address, access token, or internet connection.

## Current implementation

- Android and iOS support local JSON estate imports with validation and a replacement preview.
- Android and iOS can create a blank XLSX template and import a filled workbook.
- Workbook sheets cover the existing seven estate reference tables and supported optional YPH, production, manuring, and budget report data.
- The previous estate snapshot is retained on-device when a new import is confirmed.
- Existing local production draft storage and data fields are preserved.

Workbook details and limits are in docs/data-updates.md. The sample JSON format remains available at sample-data/estate-update.example.json.

## Development

- Android build instructions: docs/android-development.md
- iOS build instructions: ios/README.md

Keep real estate records and private exports out of this public repository.
