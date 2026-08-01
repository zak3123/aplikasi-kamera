# UI POCO Spacing Audit

Update time: 2026-07-28 19:45 +07:00

Status: source implementation updated using the supplied POCO screenshots as visual reference; physical-device screenshot inspection is still required.

## Top controls

- The top row uses individual circular controls, not one large card.
- Visible circle size is now 44 dp.
- Touch target is now 56 dp.
- Gap between controls is now 8 dp.
- Active state no longer uses a fluorescent green filled circle.
- Active state now uses a restrained dark surface with a thin accent ring and accent icon/text.
- Aspect and resolution labels stay directly under their related icon.
- Top resolution label now uses friendly rounding such as `16 MP` instead of `15.9 M`.

## Bottom controls

- Lens controls are larger and tokenized:
  - inactive 56 dp
  - active 62 dp
  - touch target 64 dp
  - gap 12 dp
- Mode labels use larger text and a 26 dp by 3 dp underline.
- The maximum-resolution capture mode is labeled `Ultra HD` in the mode strip instead of showing a megapixel value.
- Thumbnail and camera switch are both 58 dp visible, 66 dp touch target.
- Shutter is now 92 dp visible with a 104 dp touch target.
- Portrait bottom control height is now 238 dp, keeping controls farther above navigation.

## More screen

- More screen remains full black.
- Mode cell minimum height is now 104 dp.
- Icon size is now 34 dp.
- Row spacing is now 30 dp.
- Labels are short and there are no web-style descriptions.

## Pro controls

- Parameter touch target minimum is now 68 dp wide and 56 dp high.
- Parameter values use larger text.
- Row spacing increased to 8 dp.
- AUTO button minimum height increased to 52 dp.

## Remaining limitations

- The app still needs physical POCO screenshots for:
  - portrait photo
  - focus/exposure
  - quick controls
  - More
  - Pro
  - landscape-left
  - landscape-right
  - front camera

## Local validation

- `:app:assembleDebug`: success
- `:app:test`: success
- `:app:lint`: success
- APK: `D:\aplikasi-kamera\APK\AdaptiveCompositionCamera-v0.8.3-poco-sizing-exposure-debug.apk`
