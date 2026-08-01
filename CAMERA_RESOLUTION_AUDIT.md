# Camera Resolution Audit

Completion time: 2026-07-27 20:05:11 +07:00

## Scope

- Project: `D:\aplikasi-kamera`
- Version: `0.7.0` (`versionCode` 9)
- Physical device connected during this run: No

## Static pipeline findings

- Camera discovery starts from `CameraManager.cameraIdList`; it does not assume camera ID `0` is the main rear camera.
- Normal JPEG outputs are read from `SCALER_STREAM_CONFIGURATION_MAP`.
- Camera2 high-resolution JPEG outputs are read from `getHighResolutionOutputSizes(ImageFormat.JPEG)`.
- Android 12/API 31+ maximum-resolution outputs are queried from `SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION` when the device exposes the required keys/capability.
- HEIC/HEIF, RAW, YUV, video sizes, FPS ranges, high-speed configurations, active/sensor arrays, physical camera IDs, focal lengths, stabilization, manual sensor, RAW, burst, and hardware level are queried defensively.
- Megapixels are calculated from actual output dimensions: `width * height / 1,000,000`.
- Native selected resolution, output crop, CameraX-bound resolution, and actual saved JPEG dimensions are tracked separately.
- Saved-image dimensions are read from encoded media metadata without decoding a full-resolution bitmap.

## Friendly aspect ratios

The user-facing resolution UI now classifies near-common camera ratios with tolerance:

- `4624 x 3472` displays as `4:3`, not `289:217`.
- `4624 x 2080` displays as `20:9`, not `289:130`.
- `3840 x 2160` displays as `16:9`.
- `3472 x 3472` displays as `1:1`.

Exact pixel dimensions are still displayed beside the friendly ratio.

## 48 MP investigation status

No Android device was connected through ADB in this run, so genuine 48 MP exposure could not be verified physically.

The app must show 48 MP only when Android exposes a matching JPEG, high-resolution JPEG, or validated maximum-resolution Camera2 output. It must not rename a 16.1 MP or 12.1 MP stream as 48 MP.

## Device-specific results

- Device model: Unverified
- Android version: Unverified
- Camera IDs: Unverified
- Maximum normal JPEG: Unverified
- High-resolution JPEG: Unverified
- Maximum-resolution sensor map: Unverified
- Requested capture size: Unverified on physical hardware
- Bound CameraX size: Unverified on physical hardware
- Actual saved JPEG size: Unverified on physical hardware
- Genuine 48 MP exposed: Unverified
- Genuine 48 MP captured: Unverified

## Required physical verification

Connect the target phone or tablet with USB debugging, open Settings > Camera information, export the capability report, capture one recommended photo and one maximum-resolution photo if offered, then compare requested, bound, and actual saved dimensions in Settings > Diagnostics and in the media viewer.
