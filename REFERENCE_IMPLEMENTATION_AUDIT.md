# Reference Implementation Audit

Date: 2026-07-29
Project: `D:\aplikasi-kamera`
Reference checkout root: `D:\CameraReferenceRepos`

## Summary

The reference repositories were cloned with shallow history outside production source. They were inspected for architecture and implementation patterns only. No proprietary Xiaomi/POCO code, assets, package names, sounds, libraries, or private APIs were copied.

## google/jetpack-camera-app

- Local path: `D:\CameraReferenceRepos\jetpack-camera-app`
- License: Apache-2.0
- Relevant paths inspected:
  - `core/settings/.../Constraints.kt`
  - `core/settings/.../CameraAppSettings.kt`
  - `core/settings/datastore-prefs/...`
  - app entry flow around MediaStore intents
- Patterns learned:
  - feature constraints should be modeled separately from UI widgets
  - stabilization is a setting with compatibility constraints, not a decorative label
  - state and settings persistence should drive the camera UI
- Code reused: none
- Attribution required: none for copied code because no code was copied
- Selected for: state/constraint architecture reference

## android/camera-samples

- Local path: `D:\CameraReferenceRepos\android-camera-samples`
- License: no top-level LICENSE file was present in the shallow checkout; treated as official Android sample reference, not copied
- Relevant paths inspected:
  - `core-camera/src/main/java/com/android/camera/core/display/DisplayRotation.kt`
  - `core-camera/src/main/java/com/android/camera/core/camerax/CameraXPreview.kt`
  - `core-camera/src/main/java/com/android/camera/core/media/MediaStoreSaver.kt`
  - `samples/camerax-exposure`
  - `samples/camerax-videostabilization`
- Patterns learned:
  - keep display rotation as explicit state
  - transform preview taps before focus/exposure metering
  - save media through scoped MediaStore entries
  - gate video stabilization through actual CameraX/Camera2 support
- Code reused: none
- Attribution required: none for copied code because no code was copied
- Selected for: CameraX technical behavior reference

## GrapheneOS/Camera

- Local path: `D:\CameraReferenceRepos\grapheneos-camera`
- License: MIT-style license in `LICENSE`
- Relevant paths inspected:
  - application camera lifecycle and minimal UI structure
  - orientation/error-handling patterns
- Patterns learned:
  - preview-first screen hierarchy should be minimal
  - camera error paths should remain recoverable
  - controls should not dominate the preview
- Code reused: none
- Attribution required: none for copied code because no code was copied
- Selected for: minimal production camera interaction reference

## bjzhou/PhotonCamera

- Local path: `D:\CameraReferenceRepos\photon-camera`
- License: Apache-2.0
- Relevant paths inspected:
  - Camera2/manual-control related app structure
  - gallery/media metadata handling
  - advanced focus/exposure state concepts
- Patterns learned:
  - advanced camera features need explicit session/capability state
  - RAW and manual controls should not be mixed into basic UI without compatibility checks
- Code reused: none
- Attribution required: none for copied code because no code was copied
- Selected for: advanced still-photo architecture concepts

## uncannyRishabh/camx

- Local path: `D:\CameraReferenceRepos\camx`
- License: GPLv3
- Relevant paths inspected:
  - `app/src/main/java/com/uncanny/camx/Data/LensData.java`
  - mode flow classes
- Patterns learned:
  - auxiliary/physical camera discovery must inspect physical IDs and high-resolution output sizes
  - 48 MP style modes must come from exposed stream sizes, not labels
- Code reused: none
- Attribution required: no code copied; GPL source was used only as a conceptual reference
- Rejected for implementation reuse because copying GPL code would require project-wide GPL compliance.

## Changes made from the audit

- Replaced top-control horizontal scrolling with width-aware priority placement.
- Added `topControlCapacity` and `visibleTopControlSlots` to keep controls inside safe width.
- Moved overflow controls to the existing quick-settings path instead of clipping the right edge.
- Removed the separate landscape mode rail that produced a vertical Photo/Video/More list.
- Kept stabilization the same visual size as other top controls.
- Added device-side `camera_capabilities.json` export whenever Camera2 capability discovery runs.

