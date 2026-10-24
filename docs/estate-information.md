# Estate Information

Content plan for the Estate Information button in Ladang Selendang 3, v0.1 Alpha. The Android foundation includes navigation and empty section screens matching this content plan. Android file import and manual HTTPS updates now populate the offline section screens. VPS deployment and iOS import support are pending. See [data updates](data-updates.md).

## Sections

| Section | Contents |
| --- | --- |
| Estate Profile | Estate overview, cultivation and maturity status, location |
| Fields and Area | Field and alternate codes, hectarage, historical mature area |
| Palm Population and Age | Palm counts, density, field ages and age profile, with reporting year |
| Soil and Topography | Soil series, terrain types, percentages and corresponding area |
| Rainfall | Monthly rainfall in mm, rainy days, annual and year-to-date totals, historical comparison |
| Average Bunch Weight (ABW) | Estate-level current-period and cumulative ABW in kg/bunch, comparing 2026 with 2025; absolute and percentage change |
| Infrastructure | Estate road types and widths |

Keep the navigation title **Estate Information**. Rainfall is a section within this module.

## Source handling

The owner supplied Estate_Information.md, Rainfall.md and General_Data.md for private consolidation. ABW_2026_vs_2025.md was supplied as a separate supporting source for the Average Bunch Weight section. Actual records, coordinates and material prices are not included in this public reference. Validate conflicting area bases, palm densities, age profiles and source totals before database import. Preserve source values and provenance; show unresolved values as unverified.

Missing or pending months must be represented as pending, not zero. Deduplicate repeated tables. Display each dataset's reporting period and last successful synchronization time. Previously synchronized data is intended to remain available offline.

## Other modules

Material prices and pest-control product references belong under **Upkeep and Cultivation**. Production records and yield calculations belong under **Estate Production**. Operational records must stay in private storage when those modules are implemented.

## Average Bunch Weight display

Add **Average Bunch Weight** as a seventh section button under **Estate Information**. Show two comparison cards: **Current-period ABW** and **Cumulative ABW**, each with prior-year and current-year values, kg/bunch, and the absolute and percentage change. Identify the reporting period and source date; the supplied extract does not specify the exact current period or cumulative cutoff, so mark those as unconfirmed. Do not infer a month from the extraction date. Monthly and field-level breakdowns are pending supporting records. ABW measures weight per bunch; it does not establish bunch counts or total production. Keep forecasting calculations under Estate Production.
