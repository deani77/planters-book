# Dashboard rainfall

The Android Rainfall panel shows five calendar years, newest first, in millimeters. The current year ends at the last completed month in Asia/Kuala_Lumpur; earlier years require January–December. January's current-year row has no completed month and remains Pending. The calendar refreshes when the dashboard becomes active and once per minute while visible, including offline.

Totals use the saved rainfall section's monthly rows: Year, Month / total and Rainfall (mm). Printed totals are ignored. Missing, null, negative, non-finite or duplicate required month readings make that annual total Pending. Confirmed zero is included. Current/future-month readings cannot enter YTD. Records older than the display range remain in Estate Information and offline storage.

No estate values or credentials are bundled. Existing rainfall records need no new schema or data revision. This source change retains the app version and creates no APK; draft PR validation runs unit tests and lint only.
