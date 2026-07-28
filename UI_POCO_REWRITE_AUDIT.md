# UI POCO Rewrite Audit

Date: 2026-07-28
Project: `D:\aplikasi-kamera`

## Structural changes

- Removed the old competing camera chrome from `CameraScreen.kt`.
- Moved the live exposure slider into `PocoExposureControl`.
- Moved the Pro strip into `PocoStyleProControls`.
- Kept the main camera UI on one authoritative POCO-style component family.

## Control sizing

The centralized `CameraUiTokenSet` now defines adaptive token sets for compact phone, standard phone, large phone, and tablet.

The active standard-phone targets are:

- top touch target: 54 dp
- top visible circle: 42 dp
- top icon: 23 dp
- top gap: 10 dp
- active lens circle: 52 dp
- inactive lens circle: 44 dp
- thumbnail/switch visible circle: 54 dp
- photo shutter: 88 dp
- shutter touch target: 100 dp

## Stabilization visual repair

The previous large OIS/EIS circle and detached `STAB` label were removed.

Stabilization now reuses the same `PocoTopControl` visual structure as Flash, Timer, Aspect, Resolution, Grid, and Settings.

Selected state uses a thin accent ring and restrained POCO-like tint rather than a fluorescent filled circle.

## Remaining physical verification

ADB did not expose a connected device during this run, so portrait/landscape screenshots and on-device recording comparison remain unavailable.

