# Android checks and APK requests

Pushes and pull requests run unit tests and lint without packaging an APK.

APKs are created only upon explicit user request. Once requested, run the **Android checks and requested APK** workflow manually in GitHub Actions. Its manual build job assembles and tests the app, then uploads the **Planters-Book-debug** APK artifact for 14 days.

For routine local checks:

```sh
./gradlew testDebugUnitTest lintDebug
```

Only after an APK request:

```sh
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`. This is a debug test build.
