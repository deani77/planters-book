# Dashboard worker ratios

Keep the existing Immature/Mature rows and Maturity, Area (ha), Workers:ha columns. Areas and assigned mature-area operations workers come from the private overview rows Ratios reporting period (YYYY-MM), Ratios immature area (ha), Ratios mature area (ha) and Ratios mature operations workers. The maturity areas must reconcile with supplied field area.

Mature Workers:ha displays 1:(mature hectares / mature operations workers), rounded to two decimals. Staff, artisans, contractors and other worker categories are not added to this denominator. The source reporting month and operations-worker count are disclosed. No current-month relabelling is inferred. Zero immature hectares display 0.00 with a dash for the non-applicable ratio. Missing data remains Pending; zero workers never produces infinity. A positive immature area remains Pending for its worker ratio until a separate immature operations-worker allocation is supplied.

All inputs use the saved offline estate snapshot. No estate figures are bundled in GitHub, and no new APK or version bump is created for this source change.
