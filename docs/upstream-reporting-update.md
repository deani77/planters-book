# Upstream reporting update — 24 October 2026

Ported reporting changes from `deani77/Selendang-3-Apps`, branch `field-crop-summary`, commit `18e2df29416bd79d39b8ca1f6da05b101ce021e5` (open PR #10). Source `main` was still at `571e02d90af85a565677fb3c5607248387c2ad36`; these changes were not merged into the source repository as part of this port.

## Imported

- Per-field crop summary: monthly, YTD and annual budgets, actual MTD/YTD, crew planning at 21 ha per harvester, monthly rounds and productivity.
- Harvesting coverage simplified to cumulative hectares completed and completed rounds.
- Dated estate area summary showing perimeter and mature hectares.
- Five-year rainfall chart with green completed-year highs, orange lows, separate current YTD, tied extremes and gaps for missing months.

These reports read the locally saved estate snapshot. New field-level metrics are optional, so older imports continue to parse and unavailable figures remain Pending.

## Planters Book adaptations retained

Local file import/export, custom estate naming and identifiers, PDF-to-AI sharing/review, the palm-and-book icon, Suggestions to the owner's email, and manual-only APK creation remain in place. Source workflows and server connection settings were not copied. VPS/server wording in affected report screens was replaced with local-import guidance.

See [local PDF workflow](pdf-ai-import.md) and [APK policy](android-apk-policy.md). Manual data-entry forms remain separate migration work; this update does not add them.

## Validation

All Swift sources passed syntax parsing. Production, estate-reference, dashboard and calculator check suites passed, including the new field-summary, area and rainfall-chart cases. Local-import controls, custom estate naming, Suggestions and manual-only APK workflow conditions were checked after the merge.

Android unit tests/lint require CI because the local machine has no Java runtime. Full simulator/device sharing and visual checks remain pending. No APK was created.
