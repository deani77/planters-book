# Repository instructions

## APK creation

Create Android APKs only when the user explicitly requests an APK. Routine changes must not run APK assembly, packaging, connected instrumentation tests, or manually dispatch the APK workflow. Unit tests and lint checks may run without an APK request.

The Android workflow runs unit tests and lint on pushes and pull requests. Its APK build job is manual (workflow_dispatch) and must only be dispatched in response to an explicit user request.

## Upstream updates: pull requests only

Keep Planters Book aligned with deani77/Selendang-3-Apps while preserving offline operation. All future upstream ports must be prepared on a separate branch and opened as a **draft pull request** targeting main. Never push an upstream port directly to main, merge it, or enable auto-merge without an explicit user instruction for that PR.

Do not add VPS services, server APIs, authentication, network clients, background sync, or server deployment workflows. Adapt useful upstream features to local storage, workbook/file import, or manual entry. Preserve every existing data holder and stored record field when removing sync behavior.

Preserve user estate names/identifiers, local import/export, PDF-to-AI sharing, Suggestions, the custom icon and the manual-only APK policy. Resolve conflicts on the PR branch without overwriting user changes. Include source branch/PR and exact commit SHAs, adaptation notes and validation results. No APK may be created unless explicitly requested.
