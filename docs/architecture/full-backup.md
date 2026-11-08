# Complete backup, version 1 — draft, not exposed to users

The side-effect-free FullBackup codecs validate an envelope covering the current and previous estate snapshots, complete production replica rows (including mutation/version/conflict metadata), estate/profile/appearance/suggestion settings, and the saved PDF map. An absent value is explicit null. Raw estate extensions and settings objects survive decoding and re-encoding. Production uses a canonical version-2 `rows` array; the iOS storage adapter must map its `entries` key without dropping metadata. The UTF-8 JSON envelope is limited to 80 MB, its PDF to 50 MB and settings to 1 MB. It is unencrypted; share privately.

User confirmation: ABW supports annual and YTD with explicit cutoff; a full backup includes drafts, estate/profile settings and the estate map.

## Remaining work before this PR is ready

This PR is intentionally an incomplete draft. No export or restore button is exposed yet. Both platforms need store adapters to capture the complete local state, a reviewed preview showing scope and conflicts, strict platform-setting type mapping, full PDF validation with the platform PDF reader, and a journaled restore that can roll back all stores after any failure or interruption. Settings must be app-owned keys only, not the entire device preferences domain. Restore must reject changes since preview and retain a pre-restore full backup. Existing estate ZIP import/export and rollback must keep their current narrower scope and behavior.

Required integration checks: fresh install and populated-device restore; draft versions 1/2; retired fields; unknown estate extensions; corrupt settings/map/drafts; save failure at every store boundary; process interruption; restore reversal; Android-to-iOS and iOS-to-Android round trips. No feature may be described as a complete portable backup until those checks pass.
