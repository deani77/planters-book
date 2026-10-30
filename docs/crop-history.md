# Local field crop history

Crop Production → Crop History shows the imported estate’s fields with t/ha charts and tables. It is not shown on the Estate Dashboard. The rolling five-year window includes the current calendar year; absent years show N/A. Completed annual yields and explicitly dated partial YTD yields remain separate.

The optional `cropHistory` object contains `cutoff` (date), optional `source`, and `fields`. Each field contains `fieldCode`, `annual` (year strings mapped to numeric t/ha or null), `productionYear`, `productionTonnes`, nullable `plantedAreaHa`, and `areaSource`. YTD is cumulative tonnes divided by captured planted hectares, with missing/nonpositive hectares shown as N/A. Negative/nonfinite yields are not displayed. Historical hectares are never substituted with current or profile areas. Older snapshots without this extension remain supported.

Import through Settings → Estate Data Management using JSON, the XLSX Optional JSON sheet (`cropHistory` key), or a versioned ZIP. The full resulting snapshot is previewed before confirmation. Scoped ZIP updates can select `cropHistory` and/or `fieldProfiles`; unselected reports remain intact. Local export preserves the original extensions. No network service or estate-specific preparation script is included.

Validation: Android CropHistoryTest, Swift CropHistoryChecks, XLSX round-trips and ZIP selected-report update/preservation checks use fictional records.
