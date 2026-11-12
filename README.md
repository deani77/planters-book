# Planters Book

Planters Book is an offline estate management app for individual planters, estate managers and smallholders. The repository contains a Kotlin/Jetpack Compose Android app and a SwiftUI app for iPhone and iPad.

## What works on main

- In **Settings → Estate Data Management**, set the estate name, download a blank Excel template, or import a local XLSX, JSON or ZIP file. Review the validated snapshot before confirming the import.
- Dashboards and field reports use saved estate records. The app stores estate snapshots and production drafts on the device.
- XLSX and JSON replace the estate snapshot. Versioned ZIPs also support merging selected sections and reports against an exact base revision. You can export or share an estate ZIP and review or restore the previous local snapshot.

Start with the [Excel template and JSON format guide](docs/data-updates.md), the [ZIP format and backup scope](docs/estate-zip.md), or the [sample estate JSON](sample-data/estate-update.example.json). Keep real estate records, completed workbooks and private exports out of this public repository.

## Planned work

- Estate and field setup forms and a dedicated Data Input area for crop and upkeep records.
- The planned Home, Estate, Field, Data Input, Tools and Settings navigation, with read-only field reports.
- Full-device backup and restore. The current estate ZIP covers the reference snapshot and optional reports. Production drafts and app preferences are stored separately.

## Developer setup

| Task | Start here |
| --- | --- |
| Android setup and source checks | [Android development](docs/android-development.md) |
| iPhone and iPad setup | [iOS development](ios/README.md#build-and-validation) |
| Android test APK requests | [APK policy](docs/android-apk-policy.md) |
| Bring in a Selendang 3 Apps change | [Upstream pull request policy](docs/upstream-sync-policy.md) |
| Repository instructions | [AGENTS.md](AGENTS.md) |

Run the routine Android checks from the repository root with JDK 17 and Android SDK Platform 35 installed.

```sh
./gradlew testDebugUnitTest lintDebug
```

On Windows, use `gradlew.bat` in place of `./gradlew`.
