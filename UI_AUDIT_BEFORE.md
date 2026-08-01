# UI Audit Before

Evidence source: user-supplied phone and tablet screenshots taken from the pre-0.4.1 APK.

## Observed composition-selector defect

- The Composition sheet visibly presented only four tiles: None, Rule of Thirds, Vanishing Point, and Golden Ratio.
- The domain model and overlay renderer already implemented 11 guides, so the missing seven were not renderer placeholders.
- Root cause: a `LazyVerticalGrid` was nested inside a vertically scrolling sheet and capped with `heightIn(max = 360.dp)`. On the tested phone, the cap exposed only two rows while the outer sheet consumed the gesture, making later tiles effectively inaccessible.
- There was no visible count or indication that more guide cards existed.

## Other supplied-screen observations

- Portrait and landscape camera layouts were functional but require further device-side visual tuning for safe insets and preview/control proportions.
- Vanishing-point and Rule-of-Thirds overlays rendered, proving guide selection worked for the first visible cards.
- More modes correctly remained conservative when the active camera exposed no supported advanced modes.
- Physical screenshots showed runtime output labels of 12.1 MP and later 2.8 MP; these require a connected-device resolution audit and must not be re-labelled as 48 MP without evidence.
