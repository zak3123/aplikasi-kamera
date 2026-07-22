# Camera Resolution Audit

## Scope

- Project: `D:\aplikasi-kamera`
- Version: `0.4.1` (`versionCode` 5)
- Audit date: 2026-07-23 (Asia/Jakarta)
- Physical device connected during final validation: No

## Static pipeline findings

- Camera discovery starts from `CameraManager.cameraIdList`; it does not assume that camera ID `0` is the main rear camera.
- Normal JPEG outputs are read from `SCALER_STREAM_CONFIGURATION_MAP`.
- On Android 12/API 31 and newer, maximum-resolution JPEG outputs are also read from `SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION`.
- HEIC/HEIF, RAW, YUV, video sizes, normal FPS ranges, high-speed configurations, sensor arrays, lens metadata, stabilization, manual sensor, RAW, burst, logical-camera and physical-camera metadata are queried defensively.
- Megapixels are calculated from output dimensions (`width * height / 1,000,000`), never from advertised sensor marketing data.
- Native selected capture resolution, estimated aspect crop, CameraX-bound capture resolution, and actual saved JPEG dimensions are separate diagnostics.
- Saved-image dimensions are read from encoded media metadata without decoding the full-resolution bitmap.
- A maximum-resolution request uses maximum-quality capture, an exact resolution strategy, and API 31+ maximum sensor-pixel mode only for an output that Android reports as maximum-resolution capable. Invalid configurations restore a safe Photo configuration.

## Why screenshots showed 12.1 MP or 2.8 MP

The supplied screenshots are evidence of runtime values from a physical test device, but that device is not currently connected. The older `12.1 MP` display could represent the selected/bound third-party output rather than the advertised sensor maximum. The later `2.8 MP - 1920 x 1440` display is consistent with CameraX accepting a lower bound capture stream and the UI reporting that accepted stream. It is not evidence that the sensor itself is only 2.8 MP.

Version 0.4.x now records requested, bound, cropped, and actual saved-file resolution separately, and reports a mismatch rather than silently presenting a requested value as captured. A definitive device conclusion still requires exporting Camera information and inspecting an actual JPEG from that same camera ID.

## Device-specific results

- Device model: Unverified (no connected ADB device)
- Android version: Unverified
- Camera IDs and selected physical lens: Unverified
- Maximum exposed normal JPEG: Unverified
- Maximum-resolution stream-map JPEG: Unverified
- Requested resolution: Unverified on the current machine
- Bound resolution: Unverified on the current machine
- Actual JPEG resolution and megapixels: Unverified on the current machine
- Native versus cropped output: Unverified on the current machine
- Genuine 48 MP exposed: Unverified
- Genuine 48 MP captured: Unverified

The app does not fabricate a 48 MP choice when Android exposes only a lower JPEG output.

## Required physical verification

Connect the POCO phone or tablet with USB debugging, open **Settings > Camera information**, export the capability report, capture one recommended native photo and one maximum-resolution photo when offered, then compare requested, bound, and actual dimensions in **Settings > Diagnostics** and the media viewer.
