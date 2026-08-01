# POCO Reference UI Audit

Date: 2026-07-28 08:00 Asia/Jakarta

## Reference Inputs

- Primary visual reference: user-supplied POCO stock-camera screenshots.
- APK behavior reference: `E:\download\com.android.camera_6.3.008350.2_MemeOSUpdates.com.apk`
- APK size: `173,877,134` bytes
- APK SHA256: `A9260F1BD63F91C2C065B66D5F79AC5C225AB38D665CE8FC785034494FC0C486`

## Legal and Implementation Boundary

The APK was not imported as a dependency, decompiled for source copying, or used as an asset source. No Xiaomi resources, native libraries, package APIs, hidden APIs, privileged camera paths, or branding were copied. The project remains an original native Android camera using Kotlin, Jetpack Compose, CameraX, Camera2, MediaStore, and documented public Android APIs.

## Visual Behaviors Adopted

- Preview-first camera hierarchy.
- Compact individual top controls instead of one large toolbar card.
- A centered expand/collapse quick-control affordance.
- A dark compact quick-control grid anchored near the top controls.
- A black full-screen `More` overview with simple icon plus short label items.
- `More` is a selector and never a persisted capture mode.
- Advanced modes selected from `More` replace `More` in the primary mode strip.
- Pro mode keeps one compact parameter strip and opens only one control at a time.
- Shutter, latest-media thumbnail, and camera-switch remain the main bottom controls.
- Control icons and labels have a sensor-orientation rotation path through `OrientationEventListener`.

## Differences Kept Intentionally

- Accent color is restrained and original; Xiaomi branding is not used.
- Unsupported modes are hidden according to public Android capability data.
- 48 MP is not shown unless Android exposes a valid output size and saved JPEG dimensions verify it.
- Stabilization is limited to public OIS, EIS, preview stabilization, and horizon-level features.
- Vendor-specific Xiaomi image/video algorithms are not claimed.

## Current Local Implementation Changes

- `MoreModesSheet` was rebuilt as a full-screen black mode grid.
- `CameraTopBar` now includes a quick-control expand/collapse affordance.
- `QuickSettingsPanel` provides compact aspect, resolution, composition, stabilization, and settings actions.
- `rememberCameraControlRotationDegrees()` uses `OrientationEventListener` and smooth Compose animation.
- Default composition guides are subtler: opacity `0.55`, thickness `1.0 dp`, outline off.
- Golden Spiral rendering was simplified to one bounded path with a smaller default scale and no helper-square clutter.
- Aspect-ratio labels now use friendly camera labels instead of internal large fractions.

## Not Yet Physically Verified

No ADB device was connected during this pass, so the following remain unverified:

- POCO phone portrait visual comparison.
- POCO phone landscape-left and landscape-right visual comparison.
- Rotation behavior on device.
- Navigation-bar overlap behavior.
- Actual 48 MP exposure or capture.
- CaptureResult stabilization metadata.
