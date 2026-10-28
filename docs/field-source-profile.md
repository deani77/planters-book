# Local field profiles

The optional `fieldProfiles` array in a user-selected JSON snapshot or XLSX Optional JSON sheet adds read-only planted/unplanted hectares, division density/height, maturity and individually identified blocks. The profile appears before existing field records and is included in the import preview. Older snapshots without the extension remain compatible.

Each profile contains fieldCode, reportYear, plantedAreaHa, unplantedAreaHa, divisionStandsPerHa, divisionPalmHeightM, maturity and blocks. Each block contains blockId, blockName, plantedAreaHa, unplantedAreaHa, optional soilCode, harvestingYear and optional sourceNote. Numbers must be finite and nonnegative; years must be whole numbers from 1 to 9999. Duplicate profiles or block identifiers are not displayed. Unknown fields are not shown. Android hides malformed optional profiles; iOS rejects malformed typed JSON during import. Correct the source file before importing again.

User-supplied reportYear is shown; source date is unconfirmed because this extension has no date field. No estate-specific report year, source date or operational figures are bundled. Division density and height are not block measurements. Harvesting year is not planting year or palm age; profiles never replace operational areas, palm counts or calculations. Zero height remains zero; missing soil is Pending.

No VPS, publication script or private source workbook is needed. Settings → Estate Data Management validates and previews local files before confirmation. Existing JSON export retains the original extension. The separate draft ZIP feature (PR #7) is not modified here; its optional-report allowlist/merge selector must include fieldProfiles before these drafts are combined.
