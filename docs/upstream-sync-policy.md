# Upstream synchronization policy

Source: [Selendang-3-Apps](https://github.com/deani77/Selendang-3-Apps). Destination: [Planters Book](https://github.com/deani77/planters-book).

New source changes are checked by a recurring ChatGPT task. Each port is prepared on a separate branch as a **draft pull request** to Planters Book main. There is no automatic merge or direct push to main.

The draft PR is the place to review and resolve VPS requirements, API/network changes and data-model conflicts. Compatible functionality must use local storage, file import or manual entry. Server-only changes must be listed as excluded or awaiting adaptation. Existing Planters Book customization is retained.

Each PR records its source commit(s), local adaptations, exclusions, conflicts and checks. Reuse an existing draft for the same source branch when safe; preserve any human edits. Do not reopen a deliberately closed/rejected port without new source changes or explicit instruction.

No automatic APK creation. Source tests/lint and iOS simulator checks may run.

## Starting point

The reporting changes through source branch field-crop-summary at 18e2df29416bd79d39b8ca1f6da05b101ce021e5 were already ported in Planters Book commit f46e6b52b2aff85a5c44a8bf8efade06aef873e8. The policy applies to subsequent ports and does not revert that completed update.

The initial branch heads in upstream-sync-state.json prevent unchanged historical branches from being imported again. Record later processed source heads in the relevant draft PR and its proposed state update. Never change main just to advance synchronization state.
