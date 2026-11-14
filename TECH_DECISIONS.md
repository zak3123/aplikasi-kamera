# Technical Decisions — Adaptive Native Camera UI & Orientation

## 1. UI Framework Decision: Jetpack Compose (Material 3) Retained Over Android Views/XML
- **Context**: The repository (`zak3123/aplikasi-kamera`) already uses Jetpack Compose Material 3 integrated with CameraX `PreviewView` (`AndroidView`), 14 interactive composition guides (`CompositionGuideOverlay`), Pro manual sensor controls (`ISO`, `Shutter`, `Focus`, `WB`, `EV`), capability/diagnostics screens, and a media viewer. The reference ZIP (`native-camera-v2`) used Android Views/XML as an early standalone prototype.
- **Decision**: Retain **Jetpack Compose Material 3** and refactor `CameraScreen`, `PocoStyleCameraChrome`, and `CameraUiLayout` instead of rewriting the entire app to XML Views.
- **Rationale**:
  - Preserves all existing CameraX/Camera2 features, Pro controls, Document mode, Composition Guides, and Material 3 bottom sheets without risking feature regressions (`CAM-REG-011`).
  - `BoxWithConstraints` + `WindowInsets` (`statusBarsPadding`, `navigationBarsPadding`, `displayCutoutPadding`) + `animateFloatAsState` / `animateDpAsState` provide native, declarative adaptive portrait/landscape layouts and smooth micro-interactions.
  - Keeps UI state and layout policy cleanly separated from `CameraRuntime`.

## 2. Orientation & Rotation Architecture
Four distinct rotation concepts are explicitly separated in `CameraUiLayout.kt` and `CameraScreen.kt`:
1. **`physicalSensorRotation` (`UiRotation`)**: Quantized from `OrientationEventListener` (`0°`, `90°`, `180°`, `270°`) with **12° hysteresis** (`33°` enter threshold, `24°` deadband around `45°` boundaries) and a **120ms debounce** in `rememberCameraOrientationState()`. Slight tilts (`±10–25°`) do not trigger layout or icon jitter.
2. **`displayRotation` (`Surface.ROTATION_0..270`)**: Read from `LocalView.current.display?.rotation` on configuration changes.
3. **`layoutMode` (`AdaptiveLayout`)**: Derived from `BoxWithConstraints` (`PhonePortrait`, `PhoneLandscape`, `TabletPortrait`, `TabletLandscape`). In landscape, `CameraScreen` switches to an ergonomic right-edge `captureRailWidth` (`100.dp` phone / `112.dp` tablet) with `PocoStyleLandscapeStrip` for mode/lens/zoom, and `PocoStyleTopControls` constrained to `maxWidth - captureRailWidth`.
4. **`relativeControlRotationDegrees(physicalRotation, displayRotation)`**: Computes shortest signed angle delta between physical orientation and window display compensation (`(-180f..180f]`) using `shortestRotationTarget()`.
   - When Auto-Rotate is ON and Android rotates the Activity (`displayRotation != 0`), relative icon rotation is `0f` (prevents double-rotation).
   - When Auto-Rotate is OFF or Reverse Portrait (`180°`) is locked by the OS, icons smoothly counter-rotate to remain upright.
5. **`captureTargetRotation`**: Passed to `CameraRuntime.updateTargetRotation()` and `capturePhoto()` without rebinding use cases.

## 3. Video Recording Continuity Across Rotation
- **Root Cause Fixed**: Previously, `CameraScreen.kt` included `previewSize`, `captureViewportSize`, and `configuration.orientation` in the `LaunchedEffect` that calls `CameraRuntime.bind()`, which executed `cameraProvider.unbindAll()` and terminated active `Recording` sessions on rotation.
- **Fix**:
  - `CameraRuntime.isRecordingActive` and `isRecording` guard the bind `LaunchedEffect`; rebinds are never executed during active recording (`pendingRebindAfterRecording` defers any pending rebind until `VideoRecordEvent.Finalize`).
  - `previewSize` and `configuration.orientation` no longer trigger `unbindAll()` on rotation; `PreviewView` handles surface rotation natively, and `CameraRuntime.updateTargetRotation()` updates `ImageCapture.targetRotation` (and `VideoCapture.targetRotation` when idle) in-place.
  - `Recording` reference is cleared immediately on `VideoRecordEvent.Finalize` to prevent race conditions.

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
