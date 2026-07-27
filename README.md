# Adaptive Composition Camera

Native Android camera app using Kotlin, Jetpack Compose, Material 3, CameraX, Camera2 capability discovery, DataStore, MediaStore, and local-only processing.

## Features

- Rear and front camera preview through CameraX.
- Edge-to-edge stock-camera-style interface with compact top controls, mode carousel, standard shutter, latest-media thumbnail, and camera switcher.
- JPEG photo capture and MP4 video recording saved automatically to `DCIM/AdaptiveCompositionCamera` through MediaStore.
- In-app photo/video viewer with zoom or playback, share, delete confirmation, metadata, and external-gallery actions.
- Persisted camera switching by Android-exposed Camera2 ID, per-camera output resolution, pinch zoom, compact zoom/reset controls, tap focus, exposure compensation, cancellable self timer, volume shutter, and selfie screen flash.
- Composition guides: none, thirds, leading lines, adjustable manual vanishing point, golden ratio, bounded golden-square spiral, movable/resizable frame in a frame, centered, symmetry, diagonal, golden triangle, texture/repetition, foreground zones, draggable eye line, and calibrated sensor-driven roll/pitch level.
- Camera2 capability report with selectable logical cameras, metadata for their exposed physical sensors, complete JPEG/high/maximum-resolution outputs, AF/AE/AWB modes, FPS ranges, RAW/manual/burst flags, OIS/EIS modes, and high-speed video combinations.
- Settings persisted with DataStore.
- Local diagnostics copy and TXT export; no diagnostics are sent to a server.
- Responsive phone, tablet, rotation, and resizable-window behavior.
- `fullSensor` camera rotation, including reverse landscape, without requiring the device-wide rotation lock to be disabled.
- Capability-driven Pro mode with real Camera2 ISO, shutter-time, white-balance, focus-distance, and CameraX exposure-compensation controls.
- Dynamic CameraX video quality, Camera2 FPS-range, and OIS/EIS/preview-stabilization selection with CaptureResult status.
- Documents mode with an unobtrusive A-series page guide and the same automatic MediaStore save path as normal photos.
- Full-screen preview under compact translucent controls; the selected output frame and composition overlays remain aligned inside the unobstructed capture area.
- Separate portrait and landscape camera chrome, with a compact portrait gradient and a narrow safe-edge landscape capture rail.
- Native capture resolution, preview/output crop, bound CameraX resolution, and actual saved JPEG dimensions are tracked separately.
- Normal JPEG, Android high-resolution JPEG, and API 31+ maximum-sensor-map JPEG outputs are detected separately.
- API 31+ high-resolution selections use a one-shot Camera2 still session so CameraX preview fallback dimensions cannot overwrite the selected 16/48 MP output.
- A validated ultra-high-resolution maximum-sensor JPEG uses a dedicated one-shot Camera2 session with `SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION`, automatic MediaStore saving, and CameraX preview restoration.
- No ads, no analytics, no network permission, no mandatory account, and no cloud upload.

## Architecture

The app is intentionally split so CameraX session control, Camera2 discovery, settings, media naming, composition overlays, and UI do not live in one Activity.

- `camera/`: lifecycle-aware CameraX preview/ordinary capture plus an isolated Camera2 maximum-resolution still pipeline, video recording, torch, and zoom.
- `capability/`: safe `CameraCharacteristics` scanning and resolvers.
- `composition/`: Compose canvas overlays.
- `domain/model/`: serializable models and testable interfaces.
- `media/`: MediaStore queries, memory-conscious thumbnails, viewer decoding, and deletion.
- `settings/`: DataStore preferences.
- `ui/`: camera, capability, settings, diagnostics screens.
- `utility/`: megapixel, aspect ratio, resolution sorting, FPS validation.

## Android Requirements

- Minimum SDK: 24.
- Compile/target SDK: 36.
- JDK: 17.
- Camera permission is required for preview/capture.
- Microphone permission is requested only when starting video with audio.
- Location permission is not requested in this build because geotagging is not enabled.

## Build

Open `D:\aplikasi-kamera` in Android Studio, let Gradle sync, then run the `app` configuration.

Command-line build used in this workspace:

```powershell
$env:JAVA_HOME='D:\AdaptiveCompositionCameraTools\jdk-17'
$env:ANDROID_HOME='D:\AndroidSdk'
$env:ANDROID_SDK_ROOT='D:\AndroidSdk'
D:\aplikasi-kamera\gradlew.bat :app:assembleDebug
```

## Camera Limitations

The app reports what Android exposes to third-party apps. A phone advertised as 48 MP, 64 MP, 108 MP, or 200 MP may expose only a binned 12 MP output to normal camera apps. This app does not upscale images or label a lower-resolution capture as higher native megapixels.

OEM stock camera apps may use private manufacturer APIs, privileged packages, or tuned pipelines that are not available through CameraX/Camera2. Unsupported features are hidden, disabled, or explained rather than faked.

## Engineering References

- [Android camera-samples](https://github.com/android/camera-samples) for lifecycle-safe CameraX/Camera2 patterns.
- [GrapheneOS Camera](https://github.com/GrapheneOS/Camera) for a proven open-source Android camera interaction hierarchy and full-preview control placement.
- [Android maximum-resolution stream documentation](https://developer.android.com/reference/android/hardware/camera2/params/OutputConfiguration#addSensorPixelModeUsed(int)) for maximum-sensor output configuration.

No external camera project was copied into this repository. The references were used to validate architecture and interaction patterns while retaining this app's own Compose UI and composition-guide system.

## High-Speed Video

High-frame-rate and slow-motion options are shown only from valid Android high-speed stream configurations. Some devices support slow motion in the stock app but do not expose a third-party high-speed API path.

## Camera Extensions

The first build keeps extension flags conservative. Native HDR, Night, Bokeh, Face Retouch, and Auto extension UI must be enabled only after CameraX Extensions reports support for the selected camera and use-case combination.

## Manual Test Checklist

Photo: rear photo, front selfie, mirrored/unmirrored preference, screen flash, timer flow, Android high-resolution output, Camera2 maximum-sensor output, Pro controls, Documents guide, exact-bound fallback, locked-system-rotation handling, overlays, automatic gallery indexing, share, and delete.

Video: rear video, front video, audio permission, mute setting, zoom, stabilization availability, rotation, interruption, low storage, HFR, slow motion, time lapse.

Compatibility: missing autofocus, no flash, no RAW, unsupported HDR/Night/Bokeh/Retouch, unsupported FPS/resolution, tablet layout, permission denial, camera in use by another app.

## Adding Composition Guides

Add a new value to `CompositionGuide`, render it in `CompositionGuideOverlay.kt`, add its selector tile in `CameraSheets.kt`, then add a coordinate unit test.

## Privacy

The application has no internet permission, no analytics SDK, no ads, no hidden upload path, and no mandatory account. Camera frames are processed locally.

## Build Status

In this environment, `:app:assembleDebug`, `:app:testDebugUnitTest`, and `:app:lintDebug` are used as release gates. Physical camera behavior still requires testing on real devices.
