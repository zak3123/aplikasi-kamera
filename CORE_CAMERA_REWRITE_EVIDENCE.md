# Core Camera Rewrite Evidence

Updated: 2026-07-28 04:47:07 +07:00

Status: `SOURCE_REWRITE_BUILT_AWAITING_PHYSICAL_DEVICE`

This is an intermediate evidence record. It is intentionally not a completion
claim. No Android device was visible to ADB or Windows PnP when the test APK was
built, so physical capture, stabilization, and screenshot evidence is still
required.

## Material source changes

Compared with commit `da3ddba`, the core rewrite currently contains:

- `CameraRuntime.kt`: 198 lines added, 48 removed.
- `MaximumResolutionCamera2Capture.kt`: 262 lines added, 121 removed.
- `AndroidCameraCapabilityRepository.kt`: 149 lines added, 40 removed.
- `Resolvers.kt`: 111 lines added, 4 removed.
- `CompositionGuideOverlay.kt`: 103 lines added, 22 removed.
- Total working diff at the time of this report: 868 insertions and 241
  deletions across eight tracked source/test files, plus the new
  `CameraEvidenceLogger.kt`.

The source rewrite adds:

- Independent Camera2 inventories for normal JPEG, slow/high-resolution JPEG,
  and maximum-sensor JPEG maps.
- Per-size minimum-frame-duration and stall-duration evidence.
- Strict maximum-sensor capability, request-key, output-size, and sensor-pixel
  mode validation.
- JPEG-header decoding after capture; a result whose actual dimensions do not
  match the requested Camera2 surface is rejected instead of being labelled as
  the requested megapixel size.
- `CaptureResult` evidence for maximum-resolution frame number, sensor pixel
  mode, AE/AF/flash state, exposure time, and ISO.
- CameraX bind evidence containing requested, bound capture, preview, video,
  quality, FPS, and stabilization configuration.
- CameraX saved-JPEG header measurement.
- Stabilization compatibility resolution plus repeating `CaptureResult`
  monitoring for requested/result EIS, requested/result OIS, crop region,
  frame duration, FPS, and exposure.
- A rewritten perspective guide distributed around all four preview edges.
- Reworked golden-spiral subdivision rendering and contrast.
- Preview guide bounds fitted to the active photo/video aspect ratio before
  drawing and interaction.

## APK static comparison

Previous APK:

`APK/AdaptiveCompositionCamera-v0.7.0-debug.apk`

Device-test APK:

`APK/AdaptiveCompositionCamera-v0.8.0-device-test.apk`

Device-test SHA-256:

`29BA743E913241DBB9DDD5AF24C27542C063A9D078D33B40556D4C0D47AE7EEB`

The new APK does not repeat the previous “classes10.dex only” result:

| DEX | v0.7.0 bytes | v0.8.0 bytes | Changed |
|---|---:|---:|---|
| classes3.dex | 133,740 | 141,484 | yes |
| classes5.dex | 83,072 | 113,420 | yes |
| classes7.dex | 139,556 | 157,764 | yes |
| classes8.dex | 189,316 | 189,120 | yes |
| classes10.dex | 753,632 | 754,752 | yes |

`dexdump` confirms:

- `classes3.dex` defines `CompositionGuideOverlayKt`.
- `classes5.dex` defines `CameraRuntime`,
  `MaximumResolutionCamera2Capture`, and the new
  `CameraRuntime$StabilizationResultMonitor`.
- `classes7.dex` defines `AndroidCameraCapabilityRepository`,
  its new `StreamInventory`, and `DefaultStabilizationResolver`.

The machine-readable complete DEX hash comparison is stored at:

`build-reports/apk-static-diff.json`

## Local validation

- Targeted Kotlin compilation: success.
- Targeted `CameraMathTest`: 33 tests, 0 failures, 0 errors, 0 skipped.
- Debug device-test APK assembly: success.
- Full unit suite: not run yet.
- Lint: not run yet.

The full suite and lint remain intentionally deferred until the physical-device
pass is complete.

## Device evidence still required

The following items are not verified and must not be reported as working:

- Actual Camera2 output maps from the target POCO phone/tablet.
- Whether Android exposes a genuine 48 MP JPEG surface.
- Requested versus actual captured JPEG dimensions.
- Maximum-sensor `CaptureResult.SENSOR_PIXEL_MODE`.
- Stabilization request/result metadata at each tested quality and FPS.
- Portrait screenshot.
- Landscape screenshot.
- Visual guide alignment on the live preview in both orientations.

When a device is connected with USB debugging authorized, use:

```text
D:\AndroidSdk\platform-tools\adb.exe devices -l
```

The app writes evidence to Logcat tag `AdaptiveCameraEvidence` and to the
app-specific file `camera-evidence.log`. The debug package can also be inspected
through `run-as com.fatih.adaptivecompositioncamera`.
