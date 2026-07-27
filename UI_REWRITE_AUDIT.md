# UI Rewrite Audit

Completion time: 2026-07-27 21:02:21 +07:00

## Scope

This pass repaired the existing native Compose camera presentation layer in place. It did not create a duplicate app, duplicate camera repository, WebView UI, or sample-camera replacement.

## Main Camera Chrome

- The main top camera controls remain individual compact controls, not one giant rounded card.
- The compact control token set is centralized in `CameraUiTokens`.
- Portrait bottom controls remain constrained and preview-first.
- Landscape keeps a separate capture rail and mode rail instead of compressing the portrait layout.
- The selected advanced mode now appears by name in the mode selector. `More` is no longer highlighted after choosing Pro, Document, or Maximum Resolution.

## Pro Mode

- The Pro control surface was reduced from a permanent dashboard into a compact value strip.
- Only the selected Pro control opens its slider/options.
- The detailed control area dismisses automatically after a short delay.
- Entering Pro no longer silently changes a high-resolution selection to a lower recommended JPEG.
- Pro mode now warns if a high-resolution setting may be rejected instead of silently downgrading.

## Resolution State

- In-session resolution keys are isolated by camera ID and mode.
- Normal Photo and Pro Photo no longer share one active resolution state inside the camera screen.
- Pro resolution changes are not persisted as the camera default.
- A regression test covers mode-specific resolution keys.

## Composition

- Golden Spiral still uses the repaired single-path renderer from the previous pass.
- The overlay is clipped to the fitted preview frame and does not draw over the control regions.

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
