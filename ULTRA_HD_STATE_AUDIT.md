# Ultra HD State Audit

Date: 2026-07-28
Project: `D:\aplikasi-kamera`

## Invariant implemented

`CameraScreen` now prevents `CameraMode.MaximumResolution` from silently using a low normal/recommended output.

When Ultra HD is active:

1. The selected resolution is forced to `displayMaximumResolution` when the current selection is not `highResolution` or `maximumSensorMode`.
2. Full-sensor aspect is requested for dedicated high-resolution capture.
3. If no maximum/high-resolution choice exists, the mode returns to Photo and shows an unavailable message.

## Resolver proof

`ModeConflictResolver` resolves `CameraMode.MaximumResolution` to maximum-sensor output first, then high-resolution JPEG output.

New unit test:

- `ultraHdResolverNeverFallsBackToSmallRecommendedOutput`

This verifies that a 1920 x 1440 recommended output is not used when a 4624 x 3472 high-resolution output exists.

## Physical JPEG proof

No ADB device was connected during this run, so actual saved JPEG dimensions still require physical-device capture.

