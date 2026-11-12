# Android development

The Android app uses Kotlin, Jetpack Compose and Room. It supports Android 8.0 (API 26) and later. The compile and target SDK versions are set in [the app Gradle configuration](../app/build.gradle.kts).

## Set up and run source checks

1. Open the repository root in Android Studio.
2. Configure JDK 17 and install Android SDK Platform 35 and Build Tools 35.0.0.
3. Set `ANDROID_HOME` or add `sdk.dir` to the ignored `local.properties` file at the repository root.
4. Run the Gradle wrapper from the repository root.

```sh
./gradlew testDebugUnitTest lintDebug
```

On Windows, use `gradlew.bat` in place of `./gradlew`. The wrapper downloads the pinned Gradle distribution and verifies its checksum.

## CI and test APKs

The **Android checks and requested APK** workflow runs unit tests and lint on pushes to `main` and on pull requests. The manually dispatched job assembles and tests the debug APK and uploads the **Planters-Book-debug** artifact for 14 days. Follow the [APK policy](android-apk-policy.md) for explicit APK requests, including connected instrumentation tests, which also assemble APKs.

## Local data

See [Excel and JSON imports](data-updates.md) and [estate ZIP import, export and restore](estate-zip.md). Keep real estate records, private exports, credentials and signing files outside version control. Use fictional data in tests and examples.

## Build references

- [Android Gradle Plugin 8.9 compatibility](https://developer.android.com/build/releases/agp-8-9-0-release-notes)
- [Compose compiler Gradle plugin](https://developer.android.com/develop/ui/compose/compiler)
- [Room releases](https://developer.android.com/jetpack/androidx/releases/room)
