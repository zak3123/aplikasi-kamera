# Stabilization Audit

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
