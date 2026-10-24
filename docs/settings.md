# Settings on Android and iOS

Both interfaces expose User Profile, Appearance, Inbox and Suggestions.

Profile fields: display name (required for saving), position and phone (optional). Save Profile stores one local device profile, not an authenticated user identity or an authorization role. Profile edits are private device preferences; they are not uploaded to GitHub or a server. Account-specific profiles and sign-in await the central service. Profile data may be lost when the app is removed or its storage is cleared.

Appearance: Light, Dark and Follow System (default). Preferences survive app restarts. Theme changes apply to screens and the dashboard, retaining fixed brand accents and readable panel headings.

Inbox and Suggestions are explicit placeholders. No messages or suggestions are sent or received. Online data synchronization remains unconfigured.

Manual checks on each platform: save profile and restart, edit profile without saving, switch each theme and restart, verify dashboard text contrast, open both placeholders, and return to Settings/home. Server functionality is outside this change.

Settings also includes a Synchronization section with Sync Now, configuration status and last successful sync. Until a server is configured, tapping Sync Now shows an explanatory alert and does not modify local records, send data or advance the sync timestamp. No fake progress or success is displayed. Verify the alert and dismissal on both platforms. The theme labels and order are Light, Dark, Follow System.


## Android v0.2 Alpha

Settings now includes Import Estate Data, Export Estate Data and a manually configured VPS Sync Now flow with a review step. See [data updates](data-updates.md). The prior not-configured sync notice still describes the iOS starter. Server accounts and provisioning remain pending.
