# UI Control Size Audit

Update time: 2026-07-28 19:45 +07:00

Status: source implementation updated; physical POCO screenshot verification is still required.

## Old visible sizes

- Top touch target: 48 dp
- Top visible circle: 34 dp
- Top icon: 21 dp
- Latest-media / camera-switch visible diameter: 48 dp
- Shutter outer diameter: 76 dp
- Shutter stroke: 3 dp
- Portrait controls reserved height: 168 dp
- Landscape capture rail: 82 dp
- Landscape mode rail: 50 dp

These values were visibly too small and produced a custom Compose/debug-panel feel.

## New visible sizes

- Top touch target: 56 dp
- Top visible circle: 44 dp
- Top icon: 24 dp
- Top gap: 8 dp
- Top edge margin token: 16 dp
- Label padding: 10 dp horizontal, 4 dp vertical
- Lens touch target: 64 dp
- Lens inactive visible circle: 56 dp
- Lens active visible circle: 62 dp
- Lens gap: 12 dp
- Latest-media / camera-switch touch target: 66 dp
- Latest-media / camera-switch visible diameter: 58 dp
- Camera-switch icon: 29 dp
- Shutter touch target: 104 dp
- Shutter outer diameter: 92 dp
- Shutter stroke: 4.5 dp
- Portrait controls reserved height: 238 dp
- Landscape capture rail: 118 dp
- Landscape mode rail: 72 dp

## Touch-target enforcement

The main camera controls now use tokenized minimum targets rather than scattered per-composable values:

- `minimumTouchTarget`
- `lensTouchTarget`
- `secondaryTouchTarget`
- `shutterTouchTarget`

## Remaining limitations

- Portrait and landscape physical screenshots are not attached because no device was visible through ADB during this pass.

## Local validation

- `:app:assembleDebug`: success
- `:app:test`: success
- `:app:lint`: success
- APK: `D:\aplikasi-kamera\APK\AdaptiveCompositionCamera-v0.8.3-poco-sizing-exposure-debug.apk`
