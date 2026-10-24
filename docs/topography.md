# Dashboard Topography

Display Flat, Hilly and Undulating terrain, each with hectares and percentage. Use the owner's confirmed current field-area basis. Distribute rounding cents using largest remainders so displayed hectares sum to the estate total; retain the supplied percentages. Older source hectares remain in archived data revisions.

The panel reads the `soil` section's Terrain rows with Type, Category, Share (%) and Area (ha) columns, ignoring Soil series rows. Legacy Source share and Source area (ha) headers are recognized, but mismatched area bases, incomplete categories and inconsistent shares keep the panel pending. The percentage sum must be 100; terrain hectares and percentage-derived values must reconcile with synced field hectares within rounding tolerance.

No real estate values are bundled in this repository. A new approved snapshot supplies the actual percentages and areas. This panel requires Android v0.4 Alpha or later and works offline after import. Other source-review flags remain independent of the confirmed topography calculation.
