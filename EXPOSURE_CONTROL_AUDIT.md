# Exposure Control Audit

Update time: 2026-07-28 19:45 +07:00

Status: source implementation updated; physical one-thumb verification is still required.

## Old behavior

- The exposure slider used a regular Material slider rotated by -90 degrees.
- The track was only about 142 dp long.
- The widget was anchored at center-right instead of beside the focus point.
- It looked like a small debug control and was hard to use with one finger.

## New behavior

- Tap preview to move focus ring and show exposure control.
- Double tap preview to reset exposure compensation to 0 EV.
- Exposure control appears beside the focus ring.
- It automatically moves to the left side when the focus point is near the right edge.
- The focus ring diameter is now 76 dp with 2.2 dp stroke.
- The exposure slider height is now 218 dp.
- The exposure control width is 72 dp.
- The thumb diameter is 26 dp.
- The vertical track is 4 dp wide.
- A custom sun indicator is drawn at the top of the slider.
- EV text is shown using the camera exposure compensation step.

## EV range and step

- Range comes from CameraX `ExposureState.exposureCompensationRange`, backed by Android camera capabilities.
- Step comes from `ExposureState.exposureCompensationStep`.
- UI labels use friendly EV values, for example `-1.0`, `0`, `+1.0`.

## Automated coverage

`CameraMathTest` covers:

- exposure slider dimensions
- safe-side placement near screen edges
- EV label mapping from exposure index and step

## Remaining limitations

- CaptureResult acceptance of exposure compensation was not physically verified in this pass.
- Physical portrait and landscape screenshots are still required.

## Local validation

- `:app:assembleDebug`: success
- `:app:test`: success
- `:app:lint`: success
- APK: `D:\aplikasi-kamera\APK\AdaptiveCompositionCamera-v0.8.3-poco-sizing-exposure-debug.apk`
