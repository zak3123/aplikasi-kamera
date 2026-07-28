# UI Rewrite Audit

Completion time: 2026-07-27 21:02:21 +07:00

Update time: 2026-07-28 05:29 +07:00

POCO-reference update time: 2026-07-28 09:01 +07:00

Replacement update time: 2026-07-28 09:45 +07:00

## Scope

This pass repaired the existing native Compose camera presentation layer in place. It did not create a duplicate app, duplicate camera repository, WebView UI, or sample-camera replacement.

## Main Camera Chrome

- The main top camera controls remain individual compact controls, not one giant rounded card.
- The compact control token set is centralized in `CameraUiTokens`.
- Portrait bottom controls remain constrained and preview-first.
- Landscape keeps a separate capture rail and mode rail instead of compressing the portrait layout.
- The selected advanced mode now appears by name in the mode selector. `More` is no longer highlighted after choosing Pro, Document, or Maximum Resolution.
- Selecting a mode from `More` now closes the selector through the central mode-change path.
- `More` is not represented as a persisted capture mode.
- Document mode hides normal photo aspect/resolution controls from the top bar.
- The More interface is now a full-screen black mode overview with icon plus short label items, not a Material bottom sheet or card grid.
- The top bar now has a compact expand/collapse quick-control affordance.
- Expanded quick controls are shown in a small anchored dark panel instead of a settings dashboard.
- Top control icons and labels use an `OrientationEventListener` driven rotation target with Compose animation.
- Main camera chrome is now routed through `PocoStyleCameraChrome.kt`, including `PocoStyleTopControls`, `PocoStyleQuickSettings`, and `PocoStyleShutterControls`.
- `PocoStyleMoreScreen` replaces the old More screen name and implementation.
- `CameraUiState` now records the active capture mode and transient panel visibility separately; `More` is explicitly not a capture mode.

## Pro Mode

- The Pro control surface was reduced from a permanent dashboard into a compact value strip.
- Only the selected Pro control opens its slider/options.
- The detailed control area dismisses automatically after a short delay.
- Entering Pro no longer silently changes a high-resolution selection to a lower recommended JPEG.
- Pro mode now warns if a high-resolution setting may be rejected instead of silently downgrading.
- Leaving Pro through any non-Pro mode closes the Pro details panel and resets manual capture requests.

## Document Mode

- Document mode now renders only `DocumentGuideOverlay`.
- Normal composition overlays are not drawn in Document mode.
- The composition selector icon is hidden in Document mode.
- The previous compatible photo guide is restored after leaving Document.

## Resolution State

- In-session resolution keys are isolated by camera ID and mode.
- Normal Photo and Pro Photo no longer share one active resolution state inside the camera screen.
- Document final capture and Document analysis now have separate resolution keys.
- Video-family modes use a separate video resolution scope.
- Pro resolution changes are not persisted as the camera default.
- A regression test covers mode-specific resolution keys.

## Composition

- Golden Spiral still uses the repaired single-path renderer from the previous pass.
- The overlay is clipped to the fitted preview frame and does not draw over the control regions.
- Golden Spiral was further reduced in visual weight: lower default opacity, thinner stroke, no default outline, no helper-square clutter.
- Default guide selection is now Off.

## Unverified Visual Items

No phone or tablet was connected through ADB during this pass. The following remain unavailable:

- phone portrait screenshot
- phone landscape-left screenshot
- phone landscape-right screenshot
- phone Pro screenshot
- phone Document screenshot
- tablet portrait screenshot
- tablet landscape screenshot

## Remaining Presentation Limitations

- `CameraScreen.kt` is still a large composable and should be split into route, state holder, adaptive scaffold, and orientation-specific layout files in a future structural cleanup.
- Document mode remains a guided capture mode rather than a complete automatic scanner.
- A full visual sign-off requires real screenshots from the target phone and tablet.
