# Portrait-only interface

The app supports upright portrait on iPhone, iPad and Android. Existing portrait layouts are retained; no landscape-specific layouts are introduced.

## iPhone and iPad

XcodeGen creates an Info.plist declaring only `UIInterfaceOrientationPortrait` in both `UISupportedInterfaceOrientations` and `UISupportedInterfaceOrientations~ipad`. `UIRequiresFullScreen` requests iPad compatibility behavior so the scene retains its layout rather than dynamically resizing. On older iPads this also opts out of Split View; newer windowing modes may scale the fixed scene.

The generated plist remains outside version control. App identity, version and launch/scene settings remain unchanged.

## Android

The main activity requests `portrait` and disables ordinary activity resizing. The orientation-override compatibility property is false. The app currently targets API 35.

Android system/OEM overrides, desktop windowing and some tablet modes can supersede app orientation restrictions. When targeting API 36, Android changes large-screen behavior; the API 37 large-screen rules remove the orientation opt-out. Recheck this policy before raising target SDK. Do not promise that an app can defeat device-level overrides.

References:

- [Apple supported interface orientations](https://developer.apple.com/documentation/bundleresources/information-property-list/uisupportedinterfaceorientations)
- [Apple iPad full-screen compatibility](https://developer.apple.com/documentation/bundleresources/information-property-list/uirequiresfullscreen)
- [Android device compatibility mode](https://developer.android.com/guide/practices/device-compatibility-mode)
- [Android 16 large-screen behavior](https://developer.android.com/about/versions/16/behavior-changes-16)
