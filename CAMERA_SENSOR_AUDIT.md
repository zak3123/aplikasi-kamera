# Camera Sensor Audit

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
