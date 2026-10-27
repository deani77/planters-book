# Local estate data and Excel import

Versioned ZIP import, selected-section merge, ZIP export/share and reviewed backup restore are documented in [Offline estate ZIP updates](estate-zip.md). JSON/XLSX retain full-replacement behavior. All imports must match the saved estate ID.

Planters Book stores estate data on the device. Users can enter or prepare their own records and import a completed XLSX workbook or a JSON estate file. Importing works offline; no VPS account, server address, access token, or network connection is required.

## Workbook workflow

1. In Settings → Estate Data Management, choose Download Excel Template.
2. Fill the Metadata sheet, then enter estate reference rows on the Overview, Fields, Ages, Soil, Rainfall, Abw, and Roads sheets. The first row on each data sheet contains its column labels. Keep the sheet names; worksheet relationships identify the data even when an editor changes file numbering or display order.
3. Optional structured reports can be included on Optional JSON using a key and a JSON value. Supported keys are yph, cropProductionReport, cropProductionReportHistory, manuringReport, and cropBudget.
4. Choose Import Estate File, select the workbook, review the tables and optional report values, and confirm. Import replaces the current snapshot. The app retains its previous snapshot locally.

The workbook carries the same estate data fields as the JSON format. Data outside column headings, broken cell references, and malformed optional reports are rejected. A blank cell means pending/missing; enter 0 when the measured value is zero. Formula results are read as cell values; the app does not calculate formulas. Use plain values, avoid merged cells in data tables, and do not rename the template sheets. Keep workbooks below 8 MB expanded size and within the existing 10,000 row / 1,000-character cell limits.

## JSON format

See the fictional example at ../sample-data/estate-update.example.json. Format version is independent of app version.

- schemaVersion: integer 1.
- estateId: local identifier, 1–100 characters.
- revision: increasing positive whole number.
- reportingPeriod: nonblank description, up to 100 characters.
- exportedAt: ISO-8601 timestamp with timezone.
- sections: exactly one each of overview, fields, ages, soil, rainfall, abw, and roads.
- Every section contains 1–20 unique column labels and rows of matching width. Cells are text, finite numbers, or null.
- Optional report fields (yph, production reports/history, manuring report, and crop budget) remain supported.

JSON imports are limited to 2 MB; the workbook reader caps expanded workbook content at 8 MB. Both formats are validated before preview. Invalid or older revisions leave the saved data unchanged. JSON and XLSX imports are full replacements. Versioned ZIPs also support an explicitly selected section/report merge. Confirm the figures and keep your own private backup before sharing or changing devices.

## Data ownership

The estate snapshot and one previous snapshot are stored locally on Android and iOS. Production entries and their existing persistence fields are preserved. The apps do not send these records to a Planters Book server. Keep private estate records and completed workbooks out of the public repository.
