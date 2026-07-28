# Stabilization Audit

## UI Stabilization Update - 2026-07-28 12:32 +07:00

Status: visible source UI and APK DEX changed; physical screenshot and CaptureResult evidence are still blocked because no ADB device is connected.

Implemented in this pass:

- `PocoStyleCameraChrome` now has a dedicated circular `STAB` control with a short accepted/request label.
- `CameraScreen` derives the visible label from `StabilizationRequestPlan` and `stabilizationAcceptedShortLabel`.
- `CameraSheets` exposes only resolver-supported stabilization options and displays Requested, Effective request, Accepted result, request plan booleans, status, and raw CaptureResult evidence.
- `classes10.dex` changed from `905,420` bytes to `926,704` bytes.

Not proven:

- No POCO screenshot was captured.
- No real `CaptureResult` accepted stabilization value was captured.

## Core Engine Update - 2026-07-28 12:13 +07:00

Status: source pipeline updated; CaptureResult verification is still blocked because `adb devices -l` returned no connected devices.

Implemented in this pass:

- `DefaultStabilizationResolver` now returns a `StabilizationRequestPlan` describing whether a mode should request OIS, standard EIS, preview stabilization, or Off.
- Still modes now prefer OIS for Auto only when Android reports optical stabilization.
- Video modes now prefer Preview Stabilization, then Standard EIS, then OIS, avoiding blind OIS+EIS activation.
- `CameraRuntime` now applies still-photo OIS through Camera2 interop for both Preview and ImageCapture builders.
- `CameraRuntime` now monitors CaptureResult for still modes as well as video modes and records mode, requested resolution, requested/result EIS, requested/result OIS, FPS, crop, exposure, and frame duration.
- `MaximumResolutionCamera2Capture` now applies OIS in the dedicated Camera2 high/maximum-resolution JPEG request when Android exposes OIS and records requested/result OIS in `MAX_CAPTURE_RESULT`.

Not proven in this pass:

- HAL acceptance of OIS/EIS/Preview Stabilization on the POCO phone/tablet.
- Per-resolution, per-FPS, zoom, crop, and high-resolution stabilization compatibility on target hardware.

Required next device evidence:

- `STABILIZATION_REQUEST`
- `STABILIZATION_RESULT`
- `MAX_CAPTURE_RESULT`
- requested mode, actual CaptureResult value, resolution, FPS, zoom, and crop for Off/OIS/EIS/Preview modes

Completion time: 2026-07-27 21:02:21 +07:00

Update time: 2026-07-28 05:29 +07:00

## Static Implementation Reviewed

- Camera capability discovery records optical stabilization, electronic video stabilization, and preview stabilization support separately.
- The video settings sheet exposes stabilization modes only from reported capability state.
- `CameraRuntime` applies Camera2 interop requests for standard EIS, preview stabilization, OIS, Auto, and Off.
- Capture result metadata is observed in `CameraRuntime` and summarized through runtime stabilization status.
- `DefaultStabilizationResolver` is now also used by `CameraScreen`, so Photo, Pro, Document, and Video do not show the same stabilization choices.
- Photo and Pro can expose OIS only when Android reports optical stabilization.
- Video-family modes can expose Standard EIS or Preview stabilization only when Android reports those modes.
- High Resolution, HFR, Slow Motion, and Burst remain conservative and hide incompatible stabilization controls.
- The stabilization sheet now hides quality and FPS controls outside video-family modes, avoiding a fake video configuration panel for still capture.

## Requested Versus Actual Metadata

No physical camera session was available in this run. Requested and actual capture result metadata could not be verified on target hardware.

ADB result on 2026-07-28: `adb devices -l` returned no connected devices.

## Device Tests Not Performed

- Stabilization Off recording
- Standard EIS recording
- Preview Stabilization recording
- OIS-valid recording
- same-resolution/same-FPS clip comparison
- physical movement quality check
- metadata acceptance verification on target device

## Known Limitations

- Per-resolution, per-FPS, dynamic-range, codec, zoom, and extension-specific stabilization compatibility still requires device validation.
- Visual stabilization quality cannot be claimed without physical movement tests.
- The diagnostics panel can report requested/observed metadata only after a real camera session returns capture results.

## Result

Stabilization metadata verification: unverified.
