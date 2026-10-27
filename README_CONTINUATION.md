# Nexrify continuation notes

This archive continues the native Android/Kotlin project. Existing package ID, UI palette, and signing setup are retained.

## Implemented in this continuation
- Added a reusable local-time context resolver with deterministic boundaries for Early Morning, Before Sunrise, Morning, Late Morning, Afternoon, Late Afternoon, Evening, Night, Late Night, Midnight, and Midday.
- Home greeting uses the resolver and selects Indonesian or English copy based on the persisted language preference.
- Listening statistics are exposed as observable StateFlows and the statistics dialog collects them, so totals and artist counts can update while the dialog is open.
- Sync YT now exposes a user-facing status for login-required, syncing, successful results, empty/unavailable results, and failures rather than silently returning.
- Removed the duplicated nested copy of the project from the ZIP; the root project is the single canonical project.

## Still requiring external/device integration and validation
- Magrib greeting now uses a timezone-only approximate window: WIB 18:00, WITA 18:10, WIT 18:20, shown for 20 minutes. This is not an exact prayer timetable; Magrib varies by city and date. It does not use GPS or claim location-specific accuracy.
- Full localization of every screen is not complete.
- Google profile name/avatar depends on the actual sign-in provider and profile data returned by it; current project authentication is YouTube web-cookie based, not a complete Google Sign-In SDK integration.
- GitHub Releases check assumes a release exists at the configured repository and requires internet access.
- The playback service is declared exported so Android System UI and permitted external media controllers can discover/connect to the Media3 MediaSessionService. The service preserves the media session when the app task is swiped away during active playback, restores widget track state from the live media session, and loads track artwork asynchronously for the home-screen widget. Media3 supplies the playback notification and lock-screen controls from the same session. OEM music capsules / Dynamic Island-like status-bar panels are controlled by the device OS; this configuration exposes the standard Android media session that such features commonly use, but cannot force the capsule to appear on every device. Verify on the physical device and enable media notifications / music capsule in system settings if needed.
- Sync YT depends on YouTube Music's current authenticated endpoints and can fail when the upstream service changes.
- Full Gradle build was attempted but cannot run in this environment because `services.gradle.org` cannot be resolved. No APK build success is claimed.
