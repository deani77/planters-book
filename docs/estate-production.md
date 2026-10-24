# Estate Production: initial tab and local drafts

Android and iOS expose Crop History by Field, Today Crop Production, Month Crop Production and Year Todate Crop Production. Android version 0.8 Alpha includes the production screens. Server submission remains pending.

## Daily drafts

Select a reporting date and original field code. Enter Crop Harvested (t), Crop Sent to Mill (t), Crop Backlog balance (t), Hectares Harvested (ha), and Number of Harvesters Present. Values may be blank when unknown; measured zero remains zero. At least one value is needed to save. Reject negative/non-finite amounts and fractional/negative worker counts.

Save Draft persists locally and replaces only the matching field/date record. Existing saved rows are loaded when a field/date is selected. Draft storage is separate from the seven-section estate reference snapshot, so Settings imports cannot replace it. It is not included in Settings Export Estate Data. Changing devices or clearing app storage can lose these drafts; production backup and cross-device sync are pending. Unreadable draft storage blocks edits rather than silently overwriting records. Switching section, field or date prompts before discarding unsaved edits; save before leaving the production screen.

## Aggregation

Month Crop Production includes the first day of the selected month through the reporting cutoff and shows all five measures. Crop Harvested, Crop Backlog, Hectares Harvested and Number of Harvesters carry forward for that month only. Year Todate Crop Production includes January 1 through the cutoff and displays only Crop Sent to Mill; it does not display year-to-date harvested crop, hectares, attendance or backlog. Both show each field separately, the number of saved dates versus elapsed calendar days, and how many saved dates report each displayed metric. These are provisional local-draft totals, not official synchronized production. No saved values means Pending, not zero. Sum known daily tonnes and hectares; label attendance totals as harvester-days, not unique workers. Harvested hectares are cumulative work across harvest rounds. Backlog is the latest saved day's balance within the selected month and its date, not a sum; a missing balance on that day remains Pending. Do not carry backlog from a previous month without disclosure.

Historical field crop and t/ha are read-only, with five displayed years including the cutoff year. Values remain Pending until verified field-level history is connected. Never substitute estate-wide YPH figures or derive historical yield using an unverified area denominator. Partial current-year figures will need a YTD label when history is connected.

## PDF field aliases

| Report alias | App field |
| --- | --- |
| 0256PM0401 | PM04D |
| 0256PM0502 | PM05E |
| 0256PM0603 | PM06F |
| 0256PM0804 | PM08G |
| 0256PM1006 | PM10K |
| 0256PM1108 | PM11M |
| 0256PM1511 | PM15P |

The owner confirmed Pengeluaran Hasil means Crop Sent to Mill. Area is a consistency check for matching report aliases to current field records, not sufficient identity on its own. Do not bundle the private report, hectares, crop figures, credentials or snapshots in this repository. PDF capture is disabled and labelled coming next. Implement bounded PDF parsing, date/unit checks, area discrepancy review, editable extraction preview and explicit import confirmation in the next phase. Crop Harvested and Backlog require manual entry or another source. PDF MTD/YTD totals are reconciliation/opening-balance candidates; never sum repeated cumulative totals as daily figures. Duplicate dates need replacement/review, not additional records.

## Server follow-up

Production uploads run through Settings Sync Now. Save Draft queues a local pending record; the next Settings Sync Now uploads it and downloads production changes. Production sync uses authenticated write scopes, field/date uniqueness, durable atomic storage, idempotent mutation IDs, explicit conflict resolution and audit attribution. Device production export and PDF capture remain pending. Upkeep and Cultivation remains outside this tab.

## Validation

Android unit tests cover field aliases, date/field filtering, month/year boundaries, missing values, zero, daily attendance totals, latest backlog and duplicate records. Instrumented tests cover local persistence, edit replacement and preservation of unreadable storage. Run the normal Android and iOS CI checks before merging/releasing. Android and iOS CI are enabled for pre-merge validation.

On both devices, check saving/reopening a draft, selecting a different field/date, discarding/keeping unsaved changes, editing an existing draft, blank versus zero, MTD/YTD coverage, read-only history, and disabled PDF/submission buttons. Verify server estate reference import leaves production drafts untouched. Physical-device verification of two-way sync remains pending until a later APK is requested.

## Two-way sync (source validated; next app build)

Settings Sync Now uploads local pending daily records and downloads the server's daily records. Android also checks estate reference updates even when the revision is unchanged; a new reference revision still requires import confirmation. iOS now syncs production using the same server contract; estate reference import remains Android-only. Tokens stay in memory for the Settings session. No silent/background uploads are performed. This source change does not increment app versions or create a new APK.

Records are unique by field/date (PM04D throughout). Every offline edit receives a persistent mutation UUID and the server version on which it was based. A retry of an acknowledged mutation does not add another record. Atomic local acknowledgments do not clear an edit made while a request was in flight. Existing local draft files migrate to sync-aware entries without dropping records. A failed request leaves pending changes available for retry; a server rollback or missing acknowledged records is rejected.

Concurrent changes on two phones create a conflict. Settings displays all five values from both copies. Use server copy discards the phone's conflicting values after explicit selection. Keep phone copy rebases it on the displayed server version and queues a new upload for the next Sync. The server checks the version again; another intervening edit creates another conflict. Pending/conflict/synced counts are shown. Conflict copies are excluded from uploads until resolved. Local monthly/year totals remain provisional and can include pending phone edits; sync status is disclosed.

Server GET /v1/estates/selendang-3/production downloads daily records for a valid estate token. POST to the same endpoint requires an explicit production:write scope. Existing read-only tokens are not automatically promoted. Root administration: node tokens.mjs grant-production-write USER_ID; revoke-production-write USER_ID removes that scope without revoking read access. The owner token can be granted write access privately. Do not embed any credentials in the app or repository.

POST accepts schemaVersion 1, estateId selendang-3 and at most 100 changes with record, baseVersion and mutationId. The response contains revision, records, accepted keys and conflicts. A batch can acknowledge unrelated rows while flagging conflicting rows; phone acknowledgments are based on returned mutation IDs. Payloads are bounded, dates/fields/units validated, missing values remain null and zero is retained. The service serializes writes and atomically fsyncs the production store before acknowledging. The file includes an audit of old/new values, authenticated token ID and update time. The production directory alone is writable by the service; estate reference snapshots remain protected. No delete endpoint exists.

The private production reference file staged earlier contains monthly totals and historical annual yields, not daily records. It is not imported as daily drafts and is not included in the daily sync endpoint. PDF extraction and historical-reference display remain separate work.
