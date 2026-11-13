# Liquid Glass refinement — Nexrify 4.0.7

Base source: user-provided `Nexrify-Android.zip`, including its updated MainActivity and MiniPlayer files.

## Changes
- Glass navigation now uses the app's active light/dark palette instead of a fixed blue tint.
- The whole bottom navigation is a translucent capsule with a moving selected glass lens, fine highlights, and horizontal swipe navigation.
- V1 keeps a restrained frosted-glass appearance; V2 increases translucency, edge definition, and lens reflections.
- Mini player glass follows the active light/dark palette; V2 uses a stronger glass surface and highlight.
- Shared GlassSurface and Settings switches now follow theme colors with translucent borders.
- Material 3 controls inherit the selected Liquid Glass palette, including the correct light/dark surface and text colors.
- Android 13+ receives stronger blur on decorative highlights; older Android versions retain lightweight translucent surfaces.
- V2 entitlement logic remains Top 3 donor or server-provided `staff`/`admin` role.

## Limitations
Compose's current implementation uses translucent gradients and blurred lens highlights; it does not claim true real-time backdrop blur of arbitrary content behind every surface. The full app-wide redesign of every dialog/chat page still requires a dedicated screen-by-screen pass and device QA.

## Build
`:app:compileDebugKotlin --offline` could not run because Gradle 8.7 was not cached and `services.gradle.org` could not be resolved.
