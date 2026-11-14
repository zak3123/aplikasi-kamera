# Technical Decisions — Adaptive Native Camera UI & Orientation

## 1. UI Framework Decision: Jetpack Compose (Material 3) Retained Over Android Views/XML
- **Context**: The repository (`zak3123/aplikasi-kamera`) already uses Jetpack Compose Material 3 integrated with CameraX `PreviewView` (`AndroidView`), 14 interactive composition guides (`CompositionGuideOverlay`), Pro manual sensor controls (`ISO`, `Shutter`, `Focus`, `WB`, `EV`), capability/diagnostics screens, and a media viewer. The reference ZIP (`native-camera-v2`) used Android Views/XML as an early standalone prototype.
- **Decision**: Retain **Jetpack Compose Material 3** and refactor `CameraScreen`, `PocoStyleCameraChrome`, and `CameraUiLayout` instead of rewriting the entire app to XML Views.
- **Rationale**:
  - Preserves all existing CameraX/Camera2 features, Pro controls, Document mode, Composition Guides, and Material 3 bottom sheets without risking feature regressions (`CAM-REG-011`).
  - `BoxWithConstraints` + `WindowInsets` (`statusBarsPadding`, `navigationBarsPadding`, `displayCutoutPadding`) + `animateFloatAsState` / `animateDpAsState` provide native, declarative adaptive portrait/landscape layouts and smooth micro-interactions.
  - Keeps UI state and layout policy cleanly separated from `CameraRuntime`.

## 2. Orientation & Rotation Architecture
Four distinct rotation concepts are explicitly separated in `CameraUiLayout.kt`, `CameraScreen.kt`, and `CameraRuntime.kt`:
1. **`physicalSensorRotation` (`UiRotation`)**: Quantized from `OrientationEventListener` (`0°`, `90°`, `180°`, `270°`) with **12° hysteresis** (`33°` enter threshold, `24°` deadband around `45°` boundaries) and a **120ms debounce** in `rememberCameraOrientationState()`. Slight tilts (`±10–25°`) do not trigger layout or icon jitter.
2. **`displayRotation` (`Surface.ROTATION_0..270`)**: Tracked reactively via `rememberDisplayRotation()` using `DisplayManager.DisplayListener` so `0° ↔ 180°` and `90° ↔ 270°` flips (which do not fire `onConfigurationChanged`) still update `Preview.targetRotation`, `ViewPort`, and icon compensation immediately.
3. **`layoutMode` (`AdaptiveLayout`)**: Derived from `BoxWithConstraints` (`PhonePortrait`, `PhoneLandscape`, `TabletPortrait`, `TabletLandscape`). In landscape, `CameraScreen` switches to an ergonomic right-edge `captureRailWidth` (`100.dp` phone / `112.dp` tablet, with `navigationBarsPadding().displayCutoutPadding()` applied outside `width(...)` so insets do not clip the shutter button) with `PocoStyleLandscapeStrip` (`landscapeStripHeight`) for mode/lens/zoom, and `PocoStyleTopControls` constrained to `maxWidth - captureRailWidth`.
4. **`relativeControlRotationDegrees(physicalRotation, displayRotation)`**: Computes shortest signed angle delta between physical orientation and window display compensation (`(-180f..180f]`) using `shortestRotationTarget()`.
   - `displayRotationToUiRotation()` maps `Surface.ROTATION_90 -> UiRotation.ROTATION_270` and `Surface.ROTATION_270 -> UiRotation.ROTATION_90` (matching Android counter-clockwise display rotation vs clockwise sensor degrees), preventing initial `-180°` landscape double-rotation before the first `OrientationEventListener` event.
   - When Auto-Rotate is ON and Android rotates the Activity (`displayRotation != 0`), relative icon rotation is `0f` (prevents double-rotation).
   - When Auto-Rotate is OFF or Reverse Portrait (`180°`) is locked by the OS, icons smoothly counter-rotate to remain upright.
5. **Target Rotation Separation (`displayRotation` vs `captureTargetRotation`)**: `CameraRuntime` explicitly separates window `displayRotation` (applied to `Preview.targetRotation` and `ViewPort` via `updateDisplayRotation()`) from physical `captureTargetRotation` (applied to `ImageCapture.targetRotation` and idle `VideoCapture.targetRotation` via `updateTargetRotation()`, and applied immediately after async `ProcessCameraProvider` bind completes).
6. **Activity Recreation Guard**: `MainActivity` `android:configChanges` includes `keyboard|keyboardHidden|orientation|screenLayout|screenSize|smallestScreenSize|uiMode|density|fontScale|layoutDirection|locale|colorMode` so runtime configuration changes do not recreate the Activity during active sessions.

## 3. Video Recording Continuity & Race Prevention Across Rotation
- **Root Cause Fixed**: Previously, `CameraScreen.kt` included `previewSize`, `captureViewportSize`, and `configuration.orientation` in the `LaunchedEffect` that calls `CameraRuntime.bind()`, which executed `cameraProvider.unbindAll()` and terminated active `Recording` sessions on rotation. Additionally, `pendingRebindAfterRecording` was a raw boolean susceptible to missed clears on failed `startVideo()` paths or stale `Finalize` callbacks.
- **Fix**:
  - **`RebindGate` State Machine (`camera/RebindGate.kt`)**: Replaces raw `pendingRebindAfterRecording` with deterministic `request(recordingActive)` and `onRecordingEnded()` transitions triggered consistently in both `VideoRecordEvent.Finalize` and `.catch {}` failure paths.
  - **Recording & Lens Guards**: `CameraRuntime.isRecordingActive` and `isRecording` guard both the bind `LaunchedEffect` and lens-switching chips (`PocoStyleLandscapeStrip` / `PocoStyleShutterControls`) so `unbindAll()` never runs mid-recording.
  - **Deferred `VideoCapture.targetRotation` & Identity Cleanup**: `previewSize` and `configuration.orientation` no longer trigger `unbindAll()` on rotation; `PreviewView` handles surface rotation via `displayRotation`. When a rotation occurred during a previous recording, `startVideo()` applies `capture.targetRotation = latestCaptureRotation` right before `pending.start(...)` for the next recording. `Recording` cleanup uses an identity check (`if (recording === started) recording = null`) in `VideoRecordEvent.Finalize` and `awaitClose`.

## 4. Camera Pipeline Flow
```
OrientationEventListener -> quantizedUiRotation (12° hysteresis) -> 120ms debounce
   ├─> relativeControlRotationDegrees(physical, display) -> shortestRotationTarget -> Compose animateFloatAsState
   └─> captureTargetRotation -> CameraRuntime.updateTargetRotation() [no unbind]

Configuration / Layout Change -> BoxWithConstraints -> AdaptiveLayout
   ├─> Portrait / Reverse Portrait: TopControls + Bottom Shutter Deck
   └─> Landscape Left / Right: TopControls (left) + LandscapeStrip (bottom-left) + CaptureRail (right)
   └─> Active Video Recording? -> Guarded (NO unbindAll, recording continues uninterrupted)
```
