# Changelog

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
