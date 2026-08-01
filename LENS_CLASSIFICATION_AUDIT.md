# Lens Classification Audit

Date: 2026-07-28
Project: `D:\aplikasi-kamera`

## Current behavior

Physical camera/lens role classification continues to use Android camera metadata through `AndroidCameraCapabilityRepository`.

The permanent quick zoom row was tightened:

- `5x` is no longer shown as a permanent quick zoom preset for a single normal camera where `minZoom` starts at 1x.
- Real multi-camera choices use `PocoLensSelector` and labels derived from `LensRole`.

## Remaining requirement

Physical-device validation is still required to verify whether the POCO device exposes real optical breakpoints or only digital zoom presets.

No manufacturer-specific hardcoding was added.

