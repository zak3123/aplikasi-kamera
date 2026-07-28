# Legacy UI Removal Audit

Date: 2026-07-28
Project: `D:\aplikasi-kamera`

## Result

The rejected camera presentation layer was removed from `CameraScreen.kt`.

Production grep for the rejected component names now returns no matches:

- `CameraTopBar`
- `CameraBottomControls`
- `TopControl`
- `ModeCarousel`
- `CameraModeLabel`
- `LensSelector`
- `CompactLensSelector`
- `QuickZoomRow`
- `ProControlPanel`
- `QuickSettingsPanel`
- `StockExposureControl`
- `RemovedLegacy`

## Components removed

- Legacy top toolbar implementation
- Legacy quick settings panel implementation
- Legacy top-control implementation
- Legacy bottom controls implementation
- Legacy mode carousel implementation
- Legacy lens selector implementation
- Legacy zoom row implementation
- Legacy shutter/media/switch controls
- Legacy exposure control implementation
- Legacy large Pro panel implementation

## Active production hierarchy

`CameraScreen` now invokes the POCO-reference hierarchy only:

- `PocoStyleTopControls`
- `PocoStyleQuickSettings`
- `PocoExposureControl`
- `PocoStyleProControls`
- `PocoStyleShutterControls`
- `PocoStyleMoreScreen`
- `ResolutionSheet`
- `VideoSettingsSheet`
- `CompositionSheet`

## Automated proof

`CameraMathTest.legacyCameraUiNamesAreRemovedFromProductionSource` reads the production `CameraScreen.kt` source and fails if any rejected symbol remains as an exact symbol.

