# Capture Output Audit

## Device Output Request - 2026-07-28 12:32 +07:00

The requested maximum-resolution physical capture could not be performed because no ADB device was connected.

Required evidence remains:

- install `AdaptiveCompositionCamera-v0.8.2-stabilization-ui-debug.apk` on the POCO device
- select the maximum-resolution option exposed by Android
- capture one photo
- read actual saved JPEG bounds without full decode
- report width, height, megapixels, EXIF orientation, file size, camera ID, and sensor pixel mode

48 MP must remain unverified until the actual saved JPEG is near 48 million pixels.

## Core Engine Update - 2026-07-28 12:13 +07:00

Static pipeline changes:

- `MaximumResolutionCamera2Capture` continues to validate the requested JPEG size against the active Camera2 high-resolution or maximum-resolution stream map before capture.
- The dedicated Camera2 path decodes JPEG bounds without full bitmap decode and rejects output whose actual dimensions do not match the requested surface.
- `MAX_CAPTURE_RESULT` now includes sensor pixel mode, requested OIS, result OIS, crop region, exposure, ISO, AE/AF/flash state, and frame number.
- Normal CameraX capture still records actual JPEG dimensions after MediaStore save.

No actual capture output was produced in this pass because `adb devices -l` returned no connected devices.

Completion time: 2026-07-27 21:02:21 +07:00

## Result

No physical capture output was produced during this run because no Android device was connected through ADB.

## Static Workflow Reviewed

- Normal photos save automatically through MediaStore.
- Video recording saves through MediaStore.
- Latest media is queried from MediaStore and can update the thumbnail.
- Saved JPEG dimensions are read from media metadata and kept separate from requested and bound capture dimensions.
- Maximum-resolution still capture, when selected and supported, uses a dedicated Camera2 path and then returns to CameraX.

## Required Output Matrix

The following captures still need to be produced on the target device:

- normal-resolution photo
- Pro photo
- High Resolution photo
- 1:1 photo
- 4:3 photo
- 16:9 photo
- front-camera photo
- portrait-orientation photo
- landscape-orientation photo

## Fields To Record Per Capture

- selected mode
- requested size
- actual saved size
- megapixels
- camera ID
- lens
- aspect ratio
- sensor pixel mode
- EXIF orientation
- file size

## Conclusion

Actual output verification is unverified until target-device captures are available.
