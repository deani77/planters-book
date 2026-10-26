# Local FFB reject report

An optional `ffbRejectReport` object in an imported JSON file or the workbook's Optional JSON sheet supplies a dated report with `reportDate`, `fields` and `totals`. The import preview displays this report before confirmation. Existing field records and totals remain in the local snapshot; importing replaces the snapshot only after user confirmation. No server is used.

Estate Details shows Reject in **mt**, using `totals.rejectMtdMt` and `totals.rejectYtdMt` in This Month and YTD. Ripe, Underipe and Unripe carry % labels and remain Pending until their own data is supplied. This Month requires the current report month; YTD requires the current year. Future reports are ignored. Missing, negative or nonfinite masses remain Pending; zero and masses above 100 mt are valid. The report date is displayed. Percentage fields are retained when supplied but never treated as tonnes. Other report fields, including field values and review notes, remain in the original saved JSON.

Use ISO date YYYY-MM-DD. In the workbook Optional JSON sheet, enter key `ffbRejectReport` and a JSON object such as this fictional template:

```json
{"reportDate":"2026-10-01","totals":{"rejectMtdMt":0,"rejectYtdMt":null}}
```

Only enter measured figures from your own source; null means Pending. Missing optional reports clear prior snapshot values, as explained in the import preview. Do not infer a missing reject percentage from crop or mill-delivery reports with a different cutoff, or combine source field totals to override an independently reported estate total.
