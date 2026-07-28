# Camera 48 MP Audit

## Core Engine Update - 2026-07-28 12:13 +07:00

Status: source pipeline updated; physical 48 MP proof is still blocked because `adb devices -l` returned no connected devices.

Implemented in this pass:

- `AndroidCameraCapabilityRepository.kt` now records a rear-public camera enumeration line for every openable rear camera and every physical child exposed through logical camera metadata.
- Physical child summaries now include normal JPEG maximum, high-resolution JPEG maximum, maximum-resolution JPEG maximum, RAW maximum, focal lengths, sensor arrays, maximum-resolution arrays, parent logical IDs, and openable state.
- `MaximumResolutionCamera2Capture.kt` still rejects a high/maximum-resolution request if the selected size is absent from the active Camera2 high-resolution or maximum-resolution map.
- The dedicated Camera2 still path continues to decode JPEG bounds without full bitmap decode and rejects mismatched dimensions.
- The dedicated Camera2 still path now logs requested OIS, result OIS, crop region, and sensor pixel mode alongside actual JPEG dimensions.

Not proven in this pass:

- Whether the POCO stock-camera 48 MP mode is publicly exposed to third-party Android camera APIs.
- Whether a real saved APK capture is 48 MP.
- Whether a specific physical rear camera ID exposes a larger map than the current logical camera.

Required next device evidence:

- `CAMERA2_REAR_PUBLIC_ENUMERATION`
- `CAMERA2_PHYSICAL_CHILD`
- `MAX_CAPTURE`
- `MAX_CAPTURE_RESULT`
- actual saved JPEG width/height/megapixels from MediaStore output

Completion time: 2026-07-27 21:02:21 +07:00

## Result

Genuine 48 MP exposure is unverified in this run because no Android device was connected through ADB.

## Static Implementation Reviewed

- Camera discovery uses `CameraManager.cameraIdList`.
- Normal JPEG outputs are read from `SCALER_STREAM_CONFIGURATION_MAP`.
- High-resolution JPEG outputs are read from `getHighResolutionOutputSizes(ImageFormat.JPEG)`.
- Android 12+ maximum-resolution outputs are queried from `SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION`.
- Maximum-resolution still capture uses an isolated Camera2 JPEG path where the selected resolution is marked high-resolution or maximum-sensor mode.
- The app calculates megapixels from actual output dimensions and must not fabricate a 48 MP label from marketing sensor data.

## Physical Checks Not Performed

- `adb shell dumpsys media.camera`
- every rear camera ID enumeration on target phone/tablet
- actual high-resolution session creation on target hardware
- actual saved JPEG dimension verification
- 48 MP output comparison against stock camera

## Required Device Procedure

1. Connect the POCO phone or tablet with USB debugging.
2. Export Settings > Camera information.
3. Run `adb shell dumpsys media.camera` for development diagnostics.
4. Capture recommended, maximum-resolution, Pro, 1:1, 4:3, 16:9, front, portrait, and landscape photos.
5. Compare requested, bound, and actual saved dimensions.

## Conclusion

No genuine 48 MP output was proven during this run. Production UI must continue showing 48 MP only when Android exposes and the capture path validates a real matching output.
