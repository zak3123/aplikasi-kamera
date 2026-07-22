# Known Limitations

- Native CameraX Extensions are represented conservatively and need device testing before enabling HDR, Night, Bokeh, Face Retouch, or Auto extension modes in capture pipelines.
- RAW/DNG capture support is detected but not yet wired into the capture button.
- High-speed and slow-motion combinations are detected in Camera information but are hidden from the capture UI until a constrained high-speed recording path is enabled and verified.
- Histogram, focus peaking, zebra, face rectangles, horizon analysis, burst capture, time-lapse interval capture, and document/panorama experiments are not enabled as production controls.
- API 31+ maximum-resolution sensor-mode JPEG streams are selectable when Android exposes them. CameraX support still varies by camera HAL; a rejected combination is reported and restored to a safe normal Photo configuration rather than being fabricated.
- Guide export is intentionally disabled for direct captures; guides remain preview-only.
- Screen flash uses a white overlay and temporary window brightness boost; capture timing still requires device-specific validation.
- Physical camera testing was not performed in this environment because `adb devices` reported no connected device. Genuine 48 MP availability therefore remains device-reported at runtime, not claimed by the build.
