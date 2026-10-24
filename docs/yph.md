# Dashboard YPH

Android v0.5 Alpha displays historical estate actual YPH year by year and current-reporting-year actual/budget YPH through the same cutoff. The full-year budget is displayed separately. Units are t/ha.

Historical values sum the supplied monthly estate yields; retain rounding provenance and show missing years as Pending. Do not average field yields or weight historical fields using a different year's areas. Current actual and budget tonnes must cover the same months and use the same mature-area basis. Do not convert unreported months to measured zeros.

The snapshot supports an optional top-level `yph` object alongside the existing seven sections: reportingYear, throughMonth (1–12), matureAreaHa, actualYtdTonnes, budgetYtdTonnes, fullYearBudgetTonnes, and history (year, actualYph). Numeric yields and tonnages may be null when pending; area must be positive. Historical years precede the reporting year and are unique. YTD budget must not exceed annual budget. The client computes current YPH as tonnes divided by area. The VPS validates this metadata before publishing/serving it.

Older Android clients continue importing the seven existing sections and ignore optional YPH metadata; they need v0.5 to display the new panel. Existing snapshots without YPH remain compatible and leave that panel pending. No estate production records are bundled in this repository. iOS YPH remains unimplemented.

## Rolling five-year display

Android v0.5.1 Alpha limits the chart and table to the reporting year plus exactly four prior calendar years. Missing years stay Pending rather than being replaced by older records. The window follows the dataset reporting year, so a stale offline dataset is not silently relabelled as the new year. Older source records remain in the imported data for provenance, but are not shown in this panel. Current-year YTD actual and matched-period budget, plus the separate full-year budget, retain their existing meaning.
