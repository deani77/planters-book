# Previous-year crop achievement

Each Crop Production field report adds one row under Crop Budget and Actual after Actual YTD:

- Last Year Same-Period YTD · through DATE (mt/ha)

The current, non-future crop report defines the comparison year and cutoff. Same-period YTD uses the end of the month before the report month, one year earlier. For an October 2026 report, the cutoff is 30 September 2025. January rolls back to December two years before the report year. The label includes the comparison cutoff; missing source data remains Pending.

Source: `cropProductionReportHistory`, with exactly one report for each required date and exactly one matching field. Yield = `sentToMillYtdMt` / that historical record's `areaHa`. Tonnes must be finite and nonnegative; historical hectares must be finite and positive. Confirmed zero tonnes displays 0.00. Missing/duplicate reports or fields, invalid values and unavailable current report dates display Pending. Current field area and estate-wide YPH history are never substituted for a field's historical yield. Existing budgets and actuals are preserved. Old snapshots without historical field data remain compatible.

No private historical records are included in this change. Supply verified historical field reports through the existing estate snapshot workflow.
