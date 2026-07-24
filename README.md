# Adaptive Composition Camera

Native Android camera app using Kotlin, Jetpack Compose, Material 3, CameraX, Camera2 capability discovery, DataStore, MediaStore, and local-only processing.

## Features

- Rear and front camera preview through CameraX.
- Edge-to-edge stock-camera-style interface with compact top controls, mode carousel, standard shutter, latest-media thumbnail, and camera switcher.
- JPEG photo capture and MP4 video recording saved automatically to `DCIM/AdaptiveCompositionCamera` through MediaStore.
- In-app photo/video viewer with zoom or playback, share, delete confirmation, metadata, and external-gallery actions.
- Persisted camera switching by Android-exposed Camera2 ID, per-camera output resolution, pinch zoom, compact zoom/reset controls, tap focus, exposure compensation, cancellable self timer, volume shutter, and selfie screen flash.
- Composition guides: none, thirds, adjustable manual vanishing point, golden ratio, bounded golden-square spiral, movable/resizable frame in a frame, centered, adjustable texture/repetition, foreground zones, draggable eye line, and calibrated sensor-driven roll/pitch level.
- Camera2 capability report with actual Android-exposed resolutions, megapixels, FPS ranges, RAW/manual/burst/stabilization flags, logical camera IDs, and high-speed video combinations.
- Settings persisted with DataStore.
- JSON capability export/copy/share.
- Responsive phone, tablet, rotation, and resizable-window behavior.
- Separate portrait and landscape camera chrome, with a compact portrait gradient and a safe-edge landscape capture rail.
- Native capture resolution, preview/output crop, bound CameraX resolution, and actual saved JPEG dimensions are tracked separately.
- Normal JPEG, CameraX-selectable high-resolution JPEG, and API 31+ maximum-sensor-map JPEG outputs are detected separately. Only a stream CameraX can bind is shown as a capture option.
- No ads, no analytics, no network permission, no mandatory account, and no cloud upload.

## Architecture

The app is intentionally split so CameraX session control, Camera2 discovery, settings, media naming, composition overlays, and UI do not live in one Activity.

- `camera/`: lifecycle-aware CameraX preview, image capture, video recording, torch, zoom.
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

## High-Speed Video

High-frame-rate and slow-motion options are shown only from valid Android high-speed stream configurations. Some devices support slow motion in the stock app but do not expose a third-party high-speed API path.

## Camera Extensions

The first build keeps extension flags conservative. Native HDR, Night, Bokeh, Face Retouch, and Auto extension UI must be enabled only after CameraX Extensions reports support for the selected camera and use-case combination.

## POCO Notes

POCO phone/tablet testing is supported as a target, but there is no POCO, Xiaomi, or HyperOS hardcoding. All camera roles and feature availability come from exposed Android metadata.

## Manual Test Checklist

Photo: rear photo, front selfie, mirrored/unmirrored preference, screen flash, timer flow, CameraX high-resolution output, exact-bound fallback, rotation, overlays, automatic gallery indexing, share, and delete.

Video: rear video, front video, audio permission, mute setting, zoom, stabilization availability, rotation, interruption, low storage, HFR, slow motion, time lapse.

Compatibility: missing autofocus, no flash, no RAW, unsupported HDR/Night/Bokeh/Retouch, unsupported FPS/resolution, tablet layout, permission denial, camera in use by another app.

## Adding Composition Guides

Add a new value to `CompositionGuide`, render it in `CompositionGuideOverlay.kt`, add its selector tile in `CameraSheets.kt`, then add a coordinate unit test.

## Privacy

The application has no internet permission, no analytics SDK, no ads, no hidden upload path, and no mandatory account. Camera frames are processed locally.

## Build Status

In this environment, `:app:assembleDebug`, `:app:testDebugUnitTest`, and `:app:lintDebug` are used as release gates. Physical camera behavior still requires testing on real devices.
