# Camera Capabilities

Capability data comes from `CameraManager.cameraIdList` and safe `CameraCharacteristics` lookups. Missing, null, empty, or invalid metadata is treated as unavailable.

Reported values include lens facing, physical camera IDs, hardware level, sensor orientation, active/pixel arrays, focal lengths, apertures, focus distance, ISO/exposure ranges, JPEG/RAW/video sizes, FPS ranges, high-speed video combinations, OIS/EIS/preview stabilization, RAW/manual/burst/logical/depth flags, and warnings.

Megapixels are calculated from actual output size:

`width x height / 1,000,000`

The app does not infer advertised sensor megapixels, fake optical zoom, or expose physical cameras that Android does not permit as independent capture targets.
