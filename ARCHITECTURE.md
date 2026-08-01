# Architecture

Adaptive Composition Camera uses a small layered architecture:

- UI observes `AdaptiveCameraViewModel` StateFlow values.
- `SettingsRepository` owns persistent preferences through DataStore.
- `AndroidCameraCapabilityRepository` scans Camera2 metadata defensively and emits a serializable `CapabilityReport`.
- `CameraRuntime` owns CameraX binding and keeps preview, photo, and video session behavior out of composables.
- Domain interfaces make capability discovery, camera control, storage, thermal checks, high-speed filtering, stabilization, and extension decisions replaceable by fakes.

`CameraRuntime` publishes an explicit sealed session state (`PermissionRequired`, `Discovering`, `Binding`, `Ready`, `Focusing`, `Capturing`, recording transitions, switching/reconfiguration, `Error`, and `Released`). Bind generations discard stale rapid-reconfiguration callbacks. Requested stream combinations are tried once; if Android rejects one, the runtime binds a conservative preview-plus-photo configuration and the UI returns to Photo mode. Diagnostics receives the live state, actual bound use-case resolutions, and the most recent camera, capture, and recording errors.

Normal capture binds Preview and ImageCapture/VideoCapture as a `UseCaseGroup` with one `ViewPort`. The measured Compose preview box is also the clipping and normalized-coordinate boundary for every composition renderer and interactive guide. Portrait and landscape use separate layout policies from `CameraUiLayout.kt`, while camera state and capture actions remain shared.

The resolution model deliberately separates the Android-exposed native source, CameraX high-resolution output, API 31+ maximum-sensor-map metadata, selected output crop, CameraX-bound stream, and verified saved JPEG header dimensions. Normal outputs use CameraX `ResolutionSelector`; exposed high/maximum-sensor outputs use an isolated one-shot Camera2 session with the required sensor-pixel mode, then restore CameraX preview.

Camera and photo-resolution selections are persisted per Android camera ID. The UI never assumes camera ID 0 or exposes a physical-only/depth-only/non-backward-compatible camera as a selectable photographic lens. Diagnostics still show physical-camera metadata reached through logical cameras.

Video uses CameraX `Recorder` with only qualities returned by `QualitySelector.getSupportedQualities`. Camera2 interop applies an exposed FPS range and exactly one stabilization family at a time. A session capture callback reports whether preview stabilization, EIS, or OIS actually became active.
