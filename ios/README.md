# S3 Estate Info for iPhone and iPad

Native SwiftUI app for iOS 16 or later. Estate operational data is read-only and supplied by the private VPS.

## Navigation
- Home and Estate Dashboard display saved server summaries, retaining source dates and units.
- Estate Details contains estate reference panels and saved source tables. OER (%) retains Actual/Budget YTD only. Checkroll Average and Cost Breakdown panels are removed.
- Field Details shows basic field identity, area, palms, planting/age and explicitly field-keyed soil/infrastructure records.
- Crop Production shows the responsive Target/Actual table and field pills. Tapping a field opens its saved crop, harvesting, productivity and bunch-weight records. No manual forms, date-entry filters or upload controls are exposed.
- Upkeep and Cultivation shows saved estate work summaries; missing measures stay Pending.
- Planter Tools provides six offline calculators: Plant Density, Chemical Dosage per Pump, Fertilizer Requirement, Nozzle Calibration, Work Costing and Unit Conversion. Calculator inputs never alter estate records.
- Settings contains Inbox, User Profile, Appearance, About and Estate Data Sync.

## Server-only operation (build 33)
Sync Now downloads the estate snapshot via HTTPS GET. It does not invoke legacy production sync, upload pending local drafts or expose conflict-upload actions. New snapshot revisions require review and confirmation, validate before atomic replacement, and preserve the prior revision. Downloaded records remain available offline. Tokens stay in the active Settings session. Old production draft files remain untouched and are not read into operational screens. Personal profile and appearance settings remain local preferences.

Android uses the same server-only operation. Local estate-file import is removed for now; exporting an offline backup remains available. A future import mode can be restored when needed after VPS service ends.

## Crop table
Crop (mt), Yield (mt/ha) and Achievement (%) views fit phone/iPad widths with This Month, Year to Date and Full Year columns. Daily and backlog columns are omitted. Full-year achievement = actual YTD / annual target. YTD targets add monthly budgets from January through the report month. Totals calculate yield from summed mass / area. Unknown measures and zero denominators stay Pending. No operational records are bundled into the app.

## Build and validation
Run `xcodegen generate` in ios/, then build Selendang3.xcodeproj. Select the owner's existing Personal Team locally for device signing. Keep team settings, credentials and private records out of Git; build derived data outside cloud-managed Documents.

CI checks snapshot parsing, dashboard calculations, field record isolation and target periods before simulator compilation. Physical-device signing, installation and visible-screen checks are separate validation steps.

## Planter Tools (build 35)
Plant density uses spacing in metres or feet (1 ft = 0.3048 m), with triangular/equilateral or square geometry. Triangular density = 10,000 / (spacing² × sqrt(3)/2); square density = 10,000 / spacing². These are theoretical densities before roads, drains or other unplanted areas are deducted.

Chemical per pump = formulated-product rate per treated hectare × final pump solution volume / calibrated spray volume per hectare. Rate units: L/ha, mL/ha, kg/ha or g/ha. Output units: mL/pump or g/pump. Coverage = tank L / spray L/ha; pumps per hectare = spray L/ha / tank L. Nozzle flow in L/min is not accepted as application volume. No product rates or treatment recommendations are supplied. Blank, non-finite, non-positive and impossible liquid-volume inputs yield no dose. Small non-zero quantities retain significant digits rather than displaying zero.

Formula references: https://opb.mpob.gov.my/case-study-of-a-high-income-smallholder/ and https://extension.missouri.edu/publications/g1272 . Inputs/results stay local and temporary; both tools work offline in light/dark themes. Swift and Kotlin tests cover geometry, feet conversion, all rate units and invalid-input boundaries.

## Additional calculators (build 36)
- Fertilizer Requirement: whole palm population × user-supplied kg/palm, with user-supplied kg/bag. Reports kg, mt, fractional bags, whole bags rounded up and unused balance. No fertilizer rate or bag size is assumed.
- Nozzle Calibration: measured plain-water volume / measured test area × 10,000 gives L/ha. Test using the same walking speed and operating pressure as the intended application. Reference: https://blogs.ifas.ufl.edu/alachuaco/2022/02/04/calibrating-a-backpack-sprayer-the-1-128th-rule/ .
- Work Costing: total labour/material/other RM divided by hectares, whole palms or crop mt; zero costs are allowed and denominators must be positive.
- Unit Conversion: hectares, international acres, m², metres, feet, kg and mt. Conversions stay within the selected dimension and use exact definitions of the units.

All inputs remain temporary and isolated from estate records; no server request is made by these tools. Tests cover partial bag rounding, zero costs, invalid populations/denominators, calibration scaling and conversion round trips.

Calculator number inputs use a decimal point and permit correctly grouped thousands (for example, 1,000.50). Ambiguous or malformed comma inputs are rejected rather than interpreted as a smaller dose or population.
