# Source layout

Android screen packages are under `features/{estate,fields,upkeep,production,settings,tools}`. The root retains app startup/navigation and shared UI components. `core/theme` owns theme rules; `data` owns storage, input adapters, domain models and calculations pending a later package-only split. iOS views follow `Features/{Home,Estate,Fields,Upkeep,Production,Settings,Tools}`; XcodeGen discovers the subdirectories recursively. Swift model/store types retain their names.

This is a source-only reorganisation. Android namespace/applicationId, Room entities and database filename, preferences, file paths, iOS bundle ID and persisted Codable keys do not change. Do not rename those identifiers during a cosmetic cleanup. Existing draft PR #20 touches ProductionScreen/CropHistoryPanel; those paths remain in place. Its named-metric compatibility adapter remains available.
