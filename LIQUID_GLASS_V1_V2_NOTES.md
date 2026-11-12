# Liquid Glass V1 / V2 — Nexrify 4.0.7

## Theme options
- Classic: original Nexrify theme.
- Liquid Glass: free for every user; refreshed cool blue/steel glass palette.
- Liquid Glass V2: premium iOS-inspired glass palette and navigation treatment.

## Access
- Liquid Glass: all accounts.
- Liquid Glass V2: Top 3 donors, or accounts whose server-provided role is `staff` or `admin`.
- Existing role parsing reads the server-provided `role` field in the authenticated user profile.
- Legacy Violet Night, Aurora Gold, and Obsidian settings migrate to free Liquid Glass.

## Implemented in this continuation
- Added `LIQUID_GLASS_V2` as a separate persisted theme style.
- Added distinct color tokens for free Liquid Glass and Liquid Glass V2.
- Settings now show Classic, Liquid Glass (FREE), and Liquid Glass V2 (gated).
- Existing swipeable glass navigation is now enabled for both glass styles, with a stronger iOS-like treatment for V2.
- Mini player has separate V1/V2 glass gradients, rounded geometry, and Android 13+ lens highlight.
- Settings switches use glass-backed styling in both glass editions.
- Shared GlassSurface uses a more pronounced lens highlight and retains a light fallback on older Android versions.
- Existing theme colors flow into player, global chat, dialogs, and settings components that consume NexrifyColors.

## Not yet a complete redesign
This continuation does not yet replace every screen layout, every popup, every chat component, or every control with a dedicated V2 iOS layout. Those components inherit V2 color tokens where they use the shared theme, but a full screen-by-screen redesign and device QA remain outstanding.

## Build verification
Android compilation could not be run: Gradle 8.7 distribution download failed because `services.gradle.org` could not be resolved in the build environment.
