# Mode Conflict Audit

Date: 2026-07-28 05:29 Asia/Jakarta

## Previous invalid behavior

- Photo composition overlays could remain visible when `Documents` was active.
- Pro controls were reset only from scattered UI logic, not from a central mode conflict result.
- `More` was a sheet state in the composable instead of a camera mode, but selecting modes did not consistently close every incompatible panel.
- Camera session diagnostics reported a generic `Ready` state, so Photo, Pro, Document, and Video sessions could not be distinguished from logs.
- Mode resolution preferences used a shared `photo` suffix for Pro and Document final capture.

## New capture-mode model

`CameraMode` remains the single authoritative capture mode. `More` is not represented in `CameraMode` and cannot be persisted as a capture mode.

Added model support:

- `CameraMode.isStillPhotoMode`
- `CameraMode.isVideoMode`
- `ModeCompatibilityResult`
- Mode-specific `CameraSessionState` values: `PhotoReady`, `ProReady`, `DocumentReady`, `VideoReady`, `HighResolutionReady`, `SlowMotionReady`, `TimeLapseReady`

## New conflict rules

Implemented centrally in `ModeConflictResolver`:

- `Documents` forces `CompositionGuide.None`.
- Leaving `Documents` restores the previous compatible photo guide.
- Video-family modes allow only simple guides: Rule of Thirds, Centered, Horizon Level.
- Pro controls close for every mode except Pro.
- Document analysis is marked stopped when leaving Document.
- Video recording is marked stopped when leaving Video.
- High-speed and maximum-resolution modes remain conservative for stabilization.

## UI transition behavior

`CameraScreen.selectMode()` now:

- Calls `ModeConflictResolver.resolveModeChange()`.
- Closes the More selector before changing mode.
- Closes the composition selector when entering Document.
- Resets Pro controls when leaving Pro.
- Applies the resolved guide before changing the active mode.
- Shows a concise conflict message only when the resolver reports one.

## Composition-guide behavior

`CameraScreen` now renders either:

- normal `CompositionGuideOverlay` for Photo, Pro, Video-compatible modes; or
- `DocumentGuideOverlay` for Documents.

It does not render both at the same time.

## Resolution behavior

Mode resolution keys are now separated:

- Photo: `photo`
- Pro: `pro-photo`
- Documents final capture: `document-final`
- Documents analysis: `analysis`
- Video-family modes: `video`
- Maximum Resolution: `maximum-photo`

This prevents mode switching from silently overwriting unrelated resolution preferences.

## Verification

Local automated verification:

- `:app:compileDebugKotlin` succeeded.
- `:app:testDebugUnitTest --tests com.fatih.adaptivecompositioncamera.CameraMathTest` succeeded.
- Full local validation `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug` succeeded.

Physical-device verification:

- Not completed. `adb devices -l` returned no connected devices.
