# Testing

Automated tests currently cover:

- Megapixel calculation.
- Aspect ratio calculation.
- Resolution sorting and duplicate removal.
- FPS validation.
- Slow-motion factor calculation.
- Filename generation.
- Stabilization compatibility behavior.
- Continuous, bounded golden-spiral quarter-arc geometry and transforms.
- Maximum-resolution visibility based only on exposed JPEG outputs.
- Collision-resistant gallery filename generation.
- Preview crop mapping, mirroring, sensor-based horizon math, and adaptive phone/tablet classification.
- Fake camera repository/controller doubles.

Run:

```powershell
D:\AdaptiveCompositionCamera\gradlew.bat :app:testDebugUnitTest
```

Instrumentation/UI tests should be run on physical devices for permission UI, camera switching, front/rear capture, video recording, rotation, unsupported feature disabling, settings persistence, and tablet layouts. A physical camera is required to honestly validate preview, capture, stabilization, high-speed video, and OEM extension behavior.
