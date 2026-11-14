# Android application identity

Planters Book's Android **applicationId** is `com.deani77.plantersbook`.
Selendang 3 Apps continues using `com.selendang3.app`.

The existing Kotlin `namespace = "com.selendang3.app"` remains unchanged intentionally, so this small change does **not** rename Kotlin classes, generated R/BuildConfig references or test source packages. The Android package ID and code namespace may differ.

The FileProvider authority in the manifest uses `${applicationId}.pdfshare`, so it resolves to the new Planters Book ID automatically.

## Existing test installations

Changing applicationId makes Android treat this as a **different app**, not an update of any previous Planters Book APK using `com.selendang3.app`. The old local database and preferences are not transferred automatically. Existing installation and new one can coexist only if no other provider/package conflicts exist.

Before replacing or deleting an old build:
1. Export and verify its private data backup (ZIP or other supported format).
2. Install the new Planters Book APK alongside the old build.
3. Import/restore that backup into the new app, verify estate records and reports.
4. Remove old installation **only after** verifying the new data.

Do not uninstall Selendang 3 Apps to install Planters Book. For production distribution, use stable release signing and keep this application ID unchanged going forward.

Verification: run `./gradlew testDebugUnitTest lintDebug` and inspect the merged Android manifest/package during CI. This change does not request or produce an APK.
