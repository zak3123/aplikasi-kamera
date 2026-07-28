# Camera Sensor Audit

## Core Engine Update - 2026-07-28 12:13 Asia/Jakarta

`AndroidCameraCapabilityRepository` was materially updated after the static-comparison rejection.

New source evidence captured by the app on a real device will include:

- `CAMERA2_REAR_PUBLIC_ENUMERATION` for every public rear/openable camera and every physical child surfaced by logical-camera metadata.
- `CAMERA2_PHYSICAL_CHILD` rows with parent logical IDs, openable state, lens role inference, pixel arrays, maximum-resolution arrays, normal JPEG maximum, high-resolution JPEG maximum, maximum-resolution JPEG maximum, RAW maximum, focal lengths, apertures, and minimum focus distance.
- Per-camera `CAMERA2` rows now include physical child maxima inline with normal/high/maximum JPEG stream maps and timing evidence.

No device-specific sensor IDs are recorded in this update because `adb devices -l` returned no connected devices.

Date: 2026-07-28 08:00 Asia/Jakarta

## Static Implementation

`AndroidCameraCapabilityRepository` enumerates cameras through `CameraManager` and records public Android camera data including:

- camera ID, facing, hardware level, logical and physical camera metadata
- request capabilities
- sensor orientation, active array, pre-correction active array, pixel array
- maximum-resolution pixel-array and active-array metadata where exposed
- focal lengths, apertures, flash, OIS, EIS, preview stabilization
- zoom range, maximum digital zoom
- RAW, YUV, JPEG, HEIC, video, high-speed video, high-resolution JPEG, and maximum-resolution JPEG outputs
- stream timing data when available
- CameraX extension availability

## 48 MP Boundary

The app must not infer 48 MP from vendor marketing or sensor pixel-array dimensions. It may show a high-resolution option only when public Android output surfaces expose it, and it may mark it verified only when an actual saved JPEG is close to the requested pixel count.

## Device Evidence

No Android device was connected through ADB during this pass.

Command result:

```text
adb devices -l
List of devices attached
```

Because no device was attached, this audit does not contain device-specific camera IDs, real output sizes, dumpsys media.camera output, or captured JPEG dimensions.

## Required Next Device Pass

- Run `adb shell dumpsys media.camera`.
- Compare every dumpsys camera ID with the in-app capability report.
- Export `camera_capabilities.json` from the target device.
- Capture normal Photo and High Resolution where offered.
- Read actual JPEG bounds and EXIF orientation.
- Update `CAMERA_48MP_AUDIT.md` and `CAPTURE_OUTPUT_AUDIT.md` with device-specific evidence.
