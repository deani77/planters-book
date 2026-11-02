# iPad estate map

On iPad, open Estate Map above the Home dashboard and import a PDF from Files. The app copies it into local Application Support for offline access. Pinch to zoom, drag to explore and use Map Options → Reset View to fit it again. Multi-page PDFs scroll vertically.

Map Options → Replace PDF Map asks before selecting a replacement. Only readable, non-password-locked PDFs with at least one page and a maximum size of 50 MB are accepted. Validation and atomic saving complete before the displayed map changes; failed imports preserve the saved map.

No estate map is bundled. This viewer has no live GPS or selectable field overlays. The map is separate from estate records, production drafts, settings and existing ZIP/JSON/XLSX import/export and backup restore; those exports do not include the map. Keep the original PDF to restore it on another device. Removing the app removes its saved map.

Source: Selendang-3-Apps PR #27, branch codex/ipad-estate-map, commit 759f1f027b1f45d05c6cdef3b14388e9e3324db9 (merged as d8cc03f3c3e117db455484bb78d599935adfbf80). Adapted to user file import rather than Selendang-specific resources.

Acceptance checks on iPad: import a valid PDF, cancel selection, relaunch offline, zoom/pan/reset, scroll a multi-page file, cancel replacement, reject corrupt/locked/oversized files without losing the prior map, then replace with another valid PDF. Verify large text and Back navigation. Native compilation and device acceptance must be checked through CI/Xcode and an iPad.
