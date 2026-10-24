# Dashboard Age Profile

Owner-approved age bands: Young 1–5 years, Prime 6–23 years, Old >23 years. Shares are calculated by hectares, using synced field areas and field ages, and update offline after an import. The panel does not use the older precomputed category summary.

The current snapshot contract expects fields columns `Field code`, `Area (ha)`, and ages columns `Field / category`, `Age / years`. Age cells accept whole years or text such as `6 years`. Missing or duplicate field ages, missing areas, nonpositive areas and ages below 1 leave the panel pending. Age zero is not assigned to an invented category.

The chart and table show hectares and percentage per band, with a total area. No actual estate figures are bundled in the APK or public repository. The reporting period and source review status must be checked in Estate Information. Existing data import can populate these calculations; viewing this dashboard panel requires Android v0.3 Alpha or later. Other dashboard panels remain pending. iOS Age Profile is not connected by this change.
