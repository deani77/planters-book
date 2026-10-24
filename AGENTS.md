# Repository instructions

## APK creation

Create Android APKs only when the user explicitly requests an APK. Routine changes must not run APK assembly, packaging, connected instrumentation tests, or manually dispatch the APK workflow. Unit tests and lint checks may run without an APK request.

The Android workflow runs unit tests and lint on pushes and pull requests. Its APK build job is manual (workflow_dispatch) and must only be dispatched in response to an explicit user request.
