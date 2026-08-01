# Resolution Recommendation Audit

Date: 2026-07-28
Project: `D:\aplikasi-kamera`

## Problem addressed

The prior build could recommend a square 1:1 crop such as `3472 x 3472` as the primary recommended capture resolution.

## Fix

`CameraMath.indexOfFirstRecommended` now prioritizes:

1. 8-14 MP native or near-native 4:3 output
2. largest available 4:3 output
3. 8-14 MP non-square output
4. safe fallback

Square outputs are no longer recommended before native 4:3 outputs.

## Resolution selector UI

The resolution sheet was simplified:

- no large explanatory paragraph
- no repeated JPEG text on every row
- one selection dot
- megapixel value
- compact dimensions/aspect line
- short status text only when needed

## Automated proof

New unit test:

- `recommendationPrefersNativeFourByThreeOverSquareCrop`

