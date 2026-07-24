# UI Audit After

## 0.5.0 stock-camera chrome pass

- Transparent compact top controls replaced the visually heavy quick-control circles.
- Portrait controls reserve 164 dp instead of 188 dp.
- Landscape mode/capture rail is 164 dp instead of 216 dp.
- Shutter remains a standard 78 dp ring with photo/video/recording states.
- Latest media and camera switch use consistent 48 dp controls.
- Mode selection uses a restrained underline and never wraps Photo into two lines.
- Resolution selection is grouped into High resolution, Recommended, and Standard sections.
- The More sheet uses a native capability explanation rather than a developer-test message.

## Composition selector repair

- The height-limited nested lazy grid was removed.
- The bottom sheet now has one vertical scroll owner, so every guide card and the appearance controls can be reached naturally.
- The catalog contains all 11 implemented guides:
  1. None
  2. Rule of Thirds
  3. Golden Ratio / Phi Grid
  4. Golden Spiral
  5. Vanishing Point
  6. Frame in a Frame
  7. Centered
  8. Texture and Repetition
  9. Foreground
  10. Eye Line
  11. Horizon and Level
- Cards use two columns on compact sheets and three columns when at least 600 dp is available.
- The header explicitly states `11 photographic guides`.
- Each tile retains a miniature overlay preview, name, description, selected border, and now a selected check indicator.
- Selecting a guide applies it and closes the sheet, preserving the preview-first camera workflow.
- A regression unit test verifies that the UI catalog includes every supported guide and contains exactly 11 entries.

## Verification result

- Static source review: Passed
- Kotlin compilation: Passed
- Unit tests: Passed (22/22)
- Android lint: Passed (0 errors; 13 dependency/version notices)
- APK generation: Passed
- New physical screenshots: Not performed because no Android device was connected through ADB.
- Portrait/landscape/tablet visual acceptance on hardware: Requires physical-device verification.
