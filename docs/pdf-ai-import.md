# Read PDF with AI

In Settings → Estate Data Management:

1. Tap **Read PDF with AI** and choose a PDF (maximum 25 MB).
2. Choose an installed app that accepts PDF attachments. The system share menu may also show non-AI apps; availability depends on the receiving app.
3. If the receiving app omits the shared instructions, tap **Copy AI Instructions** in Planters Book and paste them into the AI conversation alongside the PDF.
4. Ask the AI to return the requested JSON. Return to Planters Book and choose **Paste AI Result**, or save the JSON file and use **Import Estate File**.
5. Validate and review before confirming replacement of the stored estate reference data. Missing sections in the result are empty; this is a full replacement, not a merge.

The AI app may require internet and send the document to its own service. Planters Book does not automatically contact an AI provider or read conversations from other apps. No API key or Planters Book VPS is used.

Only the PDF explicitly selected by the user is shared, with a temporary readable file URL. Android exposes only its dedicated PDF cache directory via a non-exported FileProvider. Cached PDF copies older than 24 hours are removed on the next PDF selection (not by a background timer).

The first implementation extracts the seven legacy reference sections: overview, fields, ages, soil, rainfall, abw and roads. It does not yet import arbitrary operational reports. Imports accept custom estate identifiers. Users set their display name in Estate Data Management.

## Validation

Swift source syntax parsing passes. Existing estate schema validation tests cover pending cells, invalid estate identifiers, duplicate sections and invalid row widths. Full iOS typechecking was blocked by the execution environment denying Xcode's Swift macro plugin sandbox. Android builds and device sharing tests have not been run locally. No APK was created.

Device checks still required: PDF picker cancellation, oversized/invalid files, compatible and incompatible AI apps, receiving app dropping text, paste valid/invalid results, import cancellation, and saved data surviving app restart.
