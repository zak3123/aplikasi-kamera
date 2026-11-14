# QA Report — Adaptive Native Camera UI & Orientation

- **Branch**: `fix/native-camera-ui-orientation`
- **Environment**: Windows 10, JDK 17.0.19, Gradle 8.14.3, Android `compileSdk = 36`, `targetSdk = 36`, `minSdk = 24`
- **Device / Emulator**: None attached (`adb devices -l` = 0 devices; `emulator.exe` not installed in `D:\AndroidSdk`). All on-device runtime/camera tests are marked `BLOCKED` (`NEEDS_DEVICE_VERIFICATION`).

## Acceptance Test Matrix

| ID | Skenario | Status | Catatan & Bukti |
|---|---|---|---|
| BUILD-01 | `./gradlew.bat :app:assembleDebug` | **PASS** | Exit 0. Output: `app/build/outputs/apk/debug/app-debug.apk` (25,205,155 bytes). |
| BUILD-02 | `:app:lintDebug` & `:app:testDebugUnitTest` | **PASS** | `CameraMathTest`: 52 tests, 0 failures, 0 errors, 0 skipped. `:app:lintDebug` exit 0 (`app/build/reports/lint-results-debug.html`). |
| REG-01 | Audit fitur lama setelah migrasi UI | **PASS** (Static/Unit) | Seluruh mode (`Photo`, `Portrait`, `Video`, `Pro`, `Documents`, `Night`, `MaximumResolution`, `SlowMotion`, `HighFrameRate`, `TimeLapse`, `Burst`, `PanoramaExperimental`), 14 composition guides, stabilization selector, Pro controls, sheets, settings, diagnostics, media viewer, volume shutter dipertahankan. |
| CAM-01 | Jalankan aplikasi & izinkan kamera | **BLOCKED** | `NEEDS_DEVICE_VERIFICATION`: Tidak ada HP fisik / emulator terpasang. |
| UI-01 | Portrait 360×800dp / HP biasa | **BLOCKED** | `NEEDS_DEVICE_VERIFICATION`: Butuh verifikasi visual di perangkat nyata. |
| UI-02 | Portrait perangkat kecil (~320dp) | **BLOCKED** | `NEEDS_DEVICE_VERIFICATION`: `responsiveTokens(320, 568)` & `visibleTopControlSlots` teruji unit; butuh screenshot perangkat nyata. |
| UI-03 | Landscape 90° ke kanan | **BLOCKED** | `NEEDS_DEVICE_VERIFICATION`: Layout rail kanan + strip bawah terimplementasi di `CameraScreen.kt`; butuh verifikasi perangkat. |
| UI-04 | Landscape 90° ke kiri | **BLOCKED** | `NEEDS_DEVICE_VERIFICATION`: `relativeControlRotationDegrees` teruji unit; butuh verifikasi perangkat. |
| UI-05 | Reverse portrait 180° | **BLOCKED** | `NEEDS_DEVICE_VERIFICATION`: `fullSensor` + kompensasi rotasi 180° teruji unit; butuh verifikasi OEM. |
| UI-06 | Miring ±10–25° tanpa jitter | **BLOCKED** | `NEEDS_DEVICE_VERIFICATION`: Hysteresis 12° (`24°` deadband) + debounce 120ms teruji unit (`uiRotationUsesHysteresisAroundQuadrants`). |
| UI-07 | Putar bolak-balik portrait/landscape 20× | **BLOCKED** | `NEEDS_DEVICE_VERIFICATION`: Membutuhkan perangkat fisik. |
| UI-08 | Notch, cutout, gesture nav, 3-button nav | **BLOCKED** | `NEEDS_DEVICE_VERIFICATION`: `statusBarsPadding()`, `navigationBarsPadding()`, `displayCutoutPadding()` diterapkan; butuh uji perangkat ber-notch. |
| UI-09 | Aksesibilitas & touch target >=48dp | **BLOCKED** | `NEEDS_DEVICE_VERIFICATION`: Touch target >=48dp (`52–104dp`) terverifikasi di `CameraUiTokens` & `PocoStyleCameraChrome.kt`. |
| PHOTO-01 | Foto portrait, landscape, reverse portrait | **BLOCKED** | `NEEDS_DEVICE_VERIFICATION`: `captureTargetRotation` dikirim ke `CameraRuntime.capturePhoto`; butuh verifikasi EXIF di perangkat. |
| PHOTO-02 | Kamera depan & fallback | **BLOCKED** | `NEEDS_DEVICE_VERIFICATION`: `canSwitch` cek front+rear availability; butuh uji perangkat. |
| VID-01 | Video recording + rotasi saat merekam | **BLOCKED** | `NEEDS_DEVICE_VERIFICATION`: Guard `isRecording || runtime.isRecordingActive` + penghapusan `configuration.orientation` / `previewSize` dari `LaunchedEffect` bind mencegah `unbindAll()`; butuh uji rekam nyata. |
| VID-02 | Video landscape & portrait dari awal | **BLOCKED** | `NEEDS_DEVICE_VERIFICATION`: Butuh pemutaran MP4 nyata di perangkat. |
| CAM-02 | Switch front/back, zoom, flash/torch | **BLOCKED** | `NEEDS_DEVICE_VERIFICATION`: Butuh hardware kamera. |
| CAM-03 | Tap-to-focus & pinch-to-zoom | **BLOCKED** | `NEEDS_DEVICE_VERIFICATION`: Butuh hardware kamera. |
| LIFE-01 | Background/foreground saat preview idle | **BLOCKED** | `NEEDS_DEVICE_VERIFICATION`: Butuh lifecycle test di perangkat. |
| LIFE-02 | Background/foreground saat recording | **BLOCKED** | `NEEDS_DEVICE_VERIFICATION`: Butuh lifecycle test di perangkat. |
| PERM-01 | Tolak izin kamera lalu beri izin | **BLOCKED** | `NEEDS_DEVICE_VERIFICATION`: Butuh runtime Android. |
| PERM-02 | Tolak izin mikrofon | **BLOCKED** | `NEEDS_DEVICE_VERIFICATION`: Butuh runtime Android. |
| STORE-01 | Simpan foto/video Android 7–9 & 10+ | **BLOCKED** | `NEEDS_DEVICE_VERIFICATION`: Butuh uji MediaStore & legacy path di perangkat. |
