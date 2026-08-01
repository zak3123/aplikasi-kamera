# POCO-Style Camera Replacement Audit

Date: 2026-07-28 09:45 Asia/Jakarta

## Scope

This pass replaces the main camera chrome path with original Compose components that follow the supplied POCO stock-camera screenshots as the visual and interaction reference. The Xiaomi/POCO APK was not imported, bundled, decompiled for source copying, or used as a resource source.

## New Presentation Structure

- `PocoStyleCameraChrome.kt`
  - `PocoStyleTopControls`
  - `PocoStyleQuickSettings`
  - `PocoStyleShutterControls`
  - `PocoStyleModeSelector`
  - `PocoStyleMoreScreen`
- `CameraUiState`
  - One authoritative active capture mode.
  - `More` is explicitly not a capture mode.
  - Tracks transient panel visibility separately from capture mode.

## Replaced Behavior

- The main camera route now calls `PocoStyleTopControls`, `PocoStyleQuickSettings`, and `PocoStyleShutterControls`.
- The `More` mode overview is named and implemented as `PocoStyleMoreScreen`.
- The default composition guide is Off.
- The Golden Spiral default style is subtle and does not show a doubled outline.
- The resolution UI uses friendly aspect-ratio labels instead of internal fractions.

## Still Public-API Only

- Normal preview and standard capture remain CameraX.
- Maximum still capture remains isolated Camera2 when public maximum-resolution surfaces exist.
- Camera capability discovery remains Camera2 `CameraManager` based.
- Media output remains MediaStore.
- No Xiaomi package name, private API, native library, or vendor resource is used.

## Local Verification

- `:app:compileDebugKotlin`: SUCCESS
- `:app:testDebugUnitTest --tests com.fatih.adaptivecompositioncamera.CameraMathTest`: SUCCESS
- `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug`: SUCCESS
- APK: `D:\aplikasi-kamera\APK\AdaptiveCompositionCamera-v0.8.0-poco-clone-debug.apk`
- APK SHA256: `12069FD5B5E4F8115A83248D6E4A73CC6947E3369F41F6176ABA307828B70E21`

## Physical Verification

Not completed. No Android device was visible through ADB in this environment. Required screenshots and real 48 MP/stabilization metadata remain unverified.
