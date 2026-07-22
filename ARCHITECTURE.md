# Architecture

Adaptive Composition Camera uses a small layered architecture:

- UI observes `AdaptiveCameraViewModel` StateFlow values.
- `SettingsRepository` owns persistent preferences through DataStore.
- `AndroidCameraCapabilityRepository` scans Camera2 metadata defensively and emits a serializable `CapabilityReport`.
- `CameraRuntime` owns CameraX binding and keeps preview, photo, and video session behavior out of composables.
- Domain interfaces make capability discovery, camera control, storage, thermal checks, high-speed filtering, stabilization, and extension decisions replaceable by fakes.

`CameraRuntime` publishes an explicit sealed session state (`PermissionRequired`, `Discovering`, `Binding`, `Ready`, `Focusing`, `Capturing`, recording transitions, switching/reconfiguration, `Error`, and `Released`). Bind generations discard stale rapid-reconfiguration callbacks. Requested stream combinations are tried once; if Android rejects one, the runtime binds a conservative preview-plus-photo configuration and the UI returns to Photo mode. Diagnostics receives the live state, actual bound use-case resolutions, and the most recent camera, capture, and recording errors.

Camera and photo-resolution selections are persisted per Android camera ID. The UI never assumes camera ID 0 or exposes a depth-only/non-backward-compatible camera as a photographic lens.
