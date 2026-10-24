# Android development

## Initial scope

Kotlin and Jetpack Compose Android application, minimum Android 8.0 (API 26), compile/target API 35. Application ID `com.selendang3.app` is provisional. Version: **v0.1 Alpha**.

The home screen has a large centered Estate Dashboard button and six smaller module buttons. Estate Information contains seven section buttons, including Rainfall and Average Bunch Weight. Buttons open screens; device Back and on-screen Back return through navigation history. All operational screens start empty. Dashboard content remains TBD.

Room provides an empty device-local database for future verified imports. No cloud service, login, importer or sync worker is configured. The footer uses the device's validated connectivity status and reads the last successful sync from local storage; it never invents a sync date. Settings explains that synchronization is not configured. Pending rainfall values use null, distinct from measured zero. No estate data is bundled.

Backgrounds currently use original palm illustrations with a different palette per main tab. Estate Information subsections share one palette. These are temporary placeholders for the agreed photographs; the unavailable original mockup has not been recreated or claimed as implemented exactly.

## Build locally

Install JDK 17 and Android SDK platform 35/build tools 35.0.0. Android Studio can provide these. Set `ANDROID_HOME`, or create an ignored `local.properties` containing your `sdk.dir` path. Open the repository root in Android Studio.

```sh
./gradlew testDebugUnitTest lintDebug
```

Windows: use `gradlew.bat`. Gradle 8.11.1 downloads through the official wrapper and verifies the distribution checksum. Dependency versions are pinned in the build scripts. Build output: `app/build/outputs/apk/debug/app-debug.apk`.

## GitHub test APK

The **Android checks and requested APK** workflow runs unit tests and lint on pushes and pull requests without creating APKs. Only manual dispatch builds APKs, runs instrumentation tests, and uploads **Planters-Book-debug**. Dispatch this job only when the user explicitly requests an APK. See [APK policy](android-apk-policy.md). Download and extract that artifact from a successful Actions run to get the APK. GitHub may require sign-in to download artifacts; retention is 14 days. This is an automatically debug-signed test build, not a production release. Future updates for real users need a securely retained release signing identity. No release signing keys are included.

To run the navigation smoke test on a connected emulator or Android device:

```sh
./gradlew connectedDebugAndroidTest
```

The manually requested APK job runs an emulator. Do not run connected instrumentation tests during routine work because they create APKs.

## Private data

Keep real source files and consolidated records outside this checkout. `private-data/`, local credentials, signing files and common secret files are ignored as an extra safeguard. Do not add private records to public assets, tests, CI output or APKs. The future authenticated data and offline-sync design is still pending. Source validation findings are retained in the owner's local collection.

## Build references

- [AGP 8.9 compatibility: JDK 17, Gradle 8.11.1, API 35](https://developer.android.com/build/releases/agp-8-9-0-release-notes)
- [Compose compiler Gradle plugin](https://developer.android.com/develop/ui/compose/compiler)
- [Room releases](https://developer.android.com/jetpack/androidx/releases/room)


## v0.2 Alpha data update

The earlier initial-scope notes describe v0.1. Android now supports JSON snapshot import/export and manually configured authenticated HTTPS downloads. See [data updates](data-updates.md) for the current contract and remaining limitations. The app version is v0.2 Alpha and the APK artifact is S3-Estate-Info-v0.2-Alpha-debug.
