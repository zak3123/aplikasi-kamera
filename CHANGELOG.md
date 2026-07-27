# Changelog

## 0.7.0

- Scan Android-exposed logical cameras and their physical-camera metadata without treating physical-only IDs as selectable lenses.
- Report complete normal, high-resolution, and API 31+ maximum-resolution JPEG outputs plus AF, AE, AWB, zoom, FPS, OIS, EIS, and capture-request metadata.
- Add real photo quality presets derived only from exposed output dimensions.
- Add CameraX video quality, Camera2 FPS-range, and dynamic stabilization controls; verify OIS/EIS/preview stabilization from capture results.
- Add leading-lines, symmetry, diagonal, and golden-triangle overlays plus rotate, mirror, and lock controls.
- Add local diagnostics copy and TXT export.
- Apply sensor frame duration with manual ISO/shutter requests.

## 0.5.0

- Reworked the camera chrome again with smaller transparent quick controls, a thinner portrait gradient, a 164 dp landscape rail, and a stock-camera-style selected mode indicator.
- Added Camera2 `getHighResolutionOutputSizes(JPEG)` discovery, which was the missing Android path commonly used for slower high-megapixel JPEG outputs.
- Configured CameraX `ResolutionSelector.PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE` only for a genuinely reported high-resolution output and requires an exact binding for Maximum Resolution mode.
- Separated normal JPEG, CameraX-selectable high-resolution JPEG, and API 31+ maximum-sensor-map JPEG outputs.
- Prevented maximum-sensor-map sizes that CameraX cannot select from appearing as working capture choices; they remain visible in Camera information and the resolution sheet as detected diagnostics.
- Grouped the resolution sheet into High resolution, Recommended, and Standard sections and retained actual saved-file verification.
- Replaced the empty developer-like More panel with a concise capability explanation for the active lens.

## 0.4.1

- Fixed the composition sheet so all 11 implemented photographic guides are reachable instead of only the first four.
- Replaced the nested height-limited lazy grid with one responsive sheet scroll and a two-column phone / three-column wide-layout catalog.
- Reordered guide cards into a photography-oriented sequence and added an explicit catalog count plus selected-card indicator.
- Added regression coverage proving the professional guide catalog contains every supported guide exactly once.

## 0.4.0

- Replaced the shared translucent camera dashboard with distinct compact portrait controls and a safe-edge landscape capture rail.
- Unified PreviewView, focus/exposure gestures, guide interaction, and guide rendering inside one measured and clipped preview viewport.
- Added CameraX `UseCaseGroup`/`ViewPort` binding, display target rotation, and an optional match-preview-crop setting.
- Separated native capture resolution from 4:3, 3:2, 16:9, 1:1, and full-screen output crops with calculated output dimensions and megapixels.
- Included API 31+ maximum-resolution JPEG stream-map outputs in selection and requested maximum sensor-pixel mode only for marked maximum outputs.
- Added requested, bound, actual saved, sensor-pixel-mode, aspect-ratio, and mismatch diagnostics.
- Verified saved JPEG dimensions from image headers without decoding full-resolution bitmaps and expanded viewer metadata.
- Added immersive transient system-bar behavior and centralized camera UI dimension tokens.
- Expanded unit coverage for crop calculations, maximum-resolution stream maps, and adaptive preview viewport fitting.

## 0.3.0

- Added persisted Android camera-ID selection and per-camera output resolution selection with safe lens labels.
- Separated native aspect-ratio selection from resolution selection and added live runtime preview/video dimensions to Diagnostics.
- Corrected the golden spiral to a bounded, continuous golden-square quarter-arc sequence with orientation and flip controls.
- Expanded manual composition controls for perspective, frame-in-frame, centered, repetition, foreground, eye-line, and calibrated roll/pitch level guides.
- Prevented duplicate capture taps, added timer cancellation, temporary focus/zoom controls, capture animation, and video pause/resume.
- Separated Photo and Video CameraX use-case binding to avoid unnecessary invalid stream combinations.
- Added unique media naming, EXIF-aware sampled image loading, and Android 9-and-earlier gallery saving support with the narrowly scoped legacy permission.
- Added runtime camera diagnostics, camera haptics and volume-shutter settings, and responsive tablet-landscape control placement.

## 0.2.0

- Rebuilt the camera as an edge-to-edge photography interface and removed the permanent bottom navigation, mode button matrix, guide column, and oversized zoom slider.
- Added automatic DCIM MediaStore saving, latest-media thumbnail, photo/video viewer, share, delete, metadata, and external-gallery actions.
- Added exact Camera2 ID selection, runtime-confirmed photo resolution labels, safe configuration fallback, tap focus, exposure, compact zoom controls, timer, flash, mirrored selfie capture, and volume shutter.
- Added the full guide selector and responsive renderers for thirds, phi, golden spiral, manual perspective, frame in frame, centered, repetition, foreground, eye line, and sensor level.
- Moved Camera information and Diagnostics under Settings.
- Expanded unit coverage for camera math, guide bounds and transforms, crop mapping, mirroring, level math, resolution filtering, mode conflicts, and adaptive layouts.

## 0.1.0

- Initial Android Studio project.
- CameraX preview, JPEG capture, video recording, front/rear switching, zoom, torch, and selfie screen flash overlay.
- Camera2 capability scanner and JSON export.
- Composition guide overlays.
- DataStore settings.
- Unit tests and documentation.
