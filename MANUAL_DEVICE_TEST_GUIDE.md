# Panduan Uji Manual di HP Fisik — Orientasi, Rekaman, Stabilitas

> Status: **belum dijalankan** (tidak ada perangkat/emulator pada mesin build). Semua hasil di `QA_REPORT.md` untuk kasus ini bertanda `BLOCKED`/`NOT_RUN` sampai panduan ini dieksekusi di perangkat. Isi kolom **Hasil** dan lampirkan logcat.

## 0. Persiapan

```powershell
# HP: aktifkan Developer options + USB debugging, sambungkan USB
adb devices -l                                   # harus muncul 1 perangkat berstatus "device"
.\gradlew.bat :app:installDebug                  # pasang APK debug
adb shell pm grant com.fatih.adaptivecompositioncamera android.permission.CAMERA
adb shell pm grant com.fatih.adaptivecompositioncamera android.permission.RECORD_AUDIO
adb shell settings put system accelerometer_rotation 1   # auto-rotate ON (ulangi tes dengan 0 untuk auto-rotate OFF)
adb shell wm size; adb shell wm density; adb shell getprop ro.build.version.release   # catat model/ukuran/Android
```

### Cara mengambil logcat (mulai SEBELUM tes, simpan per kasus)
```powershell
adb logcat -c
adb logcat -v time AdaptiveCameraEvidence:I CameraX:V Camera2CameraImpl:V VideoCapture:V Recorder:V AndroidRuntime:E *:S > logcat_<ID-KASUS>.txt
# Hentikan dengan Ctrl+C setelah kasus selesai.
```
File evidence aplikasi (`camera-evidence.log`) ikut tersimpan di penyimpanan eksternal app:
```powershell
adb pull /sdcard/Android/data/com.fatih.adaptivecompositioncamera/files/ ./evidence/
```
Baris kunci yang dicari (dari `CameraEvidenceLogger`):

| Baris log | Arti |
|---|---|
| `CAMERAX_BIND generation=N ... captureRotation=X displayRotation=Y` | Kamera di-bind ulang. **Tidak boleh muncul saat merekam.** |
| `REBIND_DEFERRED` | Rebind ditunda karena sedang merekam (benar). |
| `ORIENTATION_COMMIT physical=... display=...` | Rotasi fisik disepakati (setelah hysteresis 12° + debounce 120 ms). |
| `DISPLAY_ROTATION display=N recording=true/false` | Window berputar (termasuk 0↔180 yang tidak memicu onConfigurationChanged). |
| `CAMERAX_CAPTURE ... actual=WxH` | Foto tersimpan. |
| `FATAL EXCEPTION` / `AndroidRuntime` | Crash → kasus GAGAL. |

Pemetaan rotasi: `display 0=portrait, 1=landscape kiri (HP diputar berlawanan jarum jam), 2=terbalik 180°, 3=landscape kanan`.

---

## 1. Rotasi saat idle

| ID | Langkah | Hasil yang diharapkan | Hasil (PASS/FAIL) | Catatan |
|---|---|---|---|---|
| O-01 | Buka app, preview aktif, portrait. | Preview tegak, shutter di bawah, mode Photo/Video di bawah lensa/zoom. | | |
| O-02 | Putar HP ke landscape kiri (auto-rotate ON). | Preview tegak (tidak miring/terbalik); shutter+galeri+switch di **rail kanan**; mode/lensa di strip bawah; ikon tegak (tidak dobel putar). Log: `DISPLAY_ROTATION display=1`. | | |
| O-03 | Putar ke landscape kanan. | Sama seperti O-02, `display=3`, ikon tegak, rail tetap di sisi kanan layar. | | |
| O-04 | Putar ke portrait terbalik 180° (butuh OEM yang mengizinkan reverse portrait). | Preview tegak; UI terbaca; log `DISPLAY_ROTATION display=2`. Jika OEM menolak rotasi 180°, catat "tidak didukung OEM" (bukan FAIL). | | |
| O-05 | Dari O-04 kembali ke portrait (flip 180° ↔ 0). | Preview/ikon kembali benar **walau `onConfigurationChanged` tidak dipanggil** (diverifikasi lewat `DISPLAY_ROTATION`). | | |
| O-06 | Putar bolak-balik portrait ↔ landscape **20 kali**. | Tidak crash, preview tidak hitam menetap, shutter tetap bisa ditekan. Hitung `CAMERAX_BIND` (catat jumlah; wajar ≤ 1 per rotasi bila "Match preview crop" ON). | | |
| O-07 | Auto-rotate **OFF** (`accelerometer_rotation 0`), putar HP fisik ke landscape. | Layar tetap portrait; **ikon berputar halus** menjadi tegak terhadap pengguna; preview tidak miring. Log `ORIENTATION_COMMIT physical=...` tanpa `DISPLAY_ROTATION`. | | |

## 2. Miring ringan (anti-jitter)

| ID | Langkah | Hasil yang diharapkan | Hasil | Catatan |
|---|---|---|---|---|
| J-01 | Portrait, miringkan HP perlahan ±10° → ±25° lalu kembali, 30 detik. | Seluruh UI **diam**; tidak ada ikon berputar/bergetar; tidak ada baris `ORIENTATION_COMMIT`. | | |
| J-02 | Goyangkan HP di sekitar 45° (batas portrait↔landscape) selama 20 detik. | Tidak "bolak-balik" cepat: maksimal 1 `ORIENTATION_COMMIT` saat melewati ±57°. | | |
| J-03 | Aktifkan guide "Horizon/Level" lalu miringkan. | Hanya garis level yang bergerak; layout tetap. | | |

## 3. Rekaman video + rotasi (P0)

| ID | Langkah | Hasil yang diharapkan | Hasil | Catatan |
|---|---|---|---|---|
| V-01 | Mode Video, mulai rekam di portrait. Setelah 3 dtk putar ke landscape, 3 dtk kemudian kembali portrait, lalu stop. | **Rekaman tidak berhenti** (timer REC terus jalan). Satu file `VID_*.mp4` utuh. Log: **tidak ada** `CAMERAX_BIND` antara start & stop; ada `DISPLAY_ROTATION ... recording=true` dan (bila crop ON) `REBIND_DEFERRED`. Setelah stop, preview aktif lagi tanpa crash. | | |
| V-02 | Rekam sambil putar 4 arah (portrait→landscape kiri→180°→landscape kanan→portrait), 20 dtk total. | Sama seperti V-01. | | |
| V-03 | Putar saat merekam, **stop saat landscape**, lalu langsung mulai rekam kedua. | Rekaman kedua dimulai normal; orientasi file kedua mengikuti posisi HP saat start. | | |
| V-04 | Buka kedua file di galeri/VLC; `adb shell dumpsys media.metrics` tidak perlu. Gunakan: `ffprobe -v error -show_entries stream_tags=rotate:stream=width,height <file>` | File dapat diputar, durasi ≈ waktu rekam, audio ada. Catat rotasi metadata: **orientasi video ditetapkan saat start rekaman** (batasan CameraX; rotasi di tengah rekaman tidak mengubah file berjalan). | | |
| V-05 | Saat merekam, tekan chip lensa (jika ada >1 lensa). | Tidak terjadi apa-apa (lensa dikunci saat merekam), rekaman lanjut. | | |
| V-06 | Rekam, tekan Home, buka lagi. | Rekaman berhenti bersih (kebijakan: tidak merekam di background). File tersimpan & dapat diputar; catat pesan error yang tampil (`ERROR_SOURCE_INACTIVE` mungkin). Tidak crash. | | |

## 4. Foto & orientasi output

| ID | Langkah | Hasil yang diharapkan | Hasil | Catatan |
|---|---|---|---|---|
| P-01 | Ambil foto di 4 posisi (portrait, landscape kiri, kanan, 180°). | Foto tegak di galeri. `adb pull` lalu `exiftool -Orientation <file>` konsisten dengan posisi HP. | | |
| P-02 | Ulangi P-01 dengan **auto-rotate OFF**. | Foto tetap tegak mengikuti posisi fisik HP (orientation dari sensor, bukan layar). | | |
| P-03 | Kamera depan: foto portrait & landscape. | Tidak terbalik tak terduga; mirror sesuai pengaturan. | | |

## 5. Layout, cutout, ukuran layar

| ID | Langkah | Hasil yang diharapkan | Hasil | Catatan |
|---|---|---|---|---|
| L-01 | HP berponi/punch-hole, landscape kiri & kanan. | Tombol rail tidak tertutup cutout; tidak ada tombol terpotong. | | |
| L-02 | Gesture navigation dan 3-button navigation. | Shutter tidak tertutup bar sistem (geser dari tepi memunculkan bar transien). | | |
| L-03 | `adb shell wm size 720x1280` lalu `wm size 540x960` (≈320dp), tes portrait & landscape; **kembalikan**: `adb shell wm size reset`. | Tidak ada overlap/terpotong; top controls meluber ke Quick Settings. | | |
| L-04 | `adb shell settings put system font_scale 1.3` (kemudian `1.0`). | Layar **tidak restart** (tidak ada flicker hitam; rekaman tidak berhenti bila sedang merekam). | | |
| L-05 | Mode Pro di landscape, buka panel Pro. | Panel Pro **tidak menimpa** strip mode/lensa. | | |
| L-06 | Tablet/foldable bila ada (split-screen/unfold). | Layout tidak patah; jika tidak ada perangkat → `NOT_RUN`. | | |

## 6. Animasi & gestur

| ID | Langkah | Hasil yang diharapkan | Hasil | Catatan |
|---|---|---|---|---|
| A-01 | Tekan-tahan shutter. | Mengecil halus (~0.9×), kembali saat dilepas; tidak ada ripple kotak. | | |
| A-02 | Mulai/stop rekam. | Shutter morph ke kotak merah; kembali ke lingkaran merah. | | |
| A-03 | Ketuk preview di tempat berbeda berturut-turut (cepat). | Cincin fokus mengecil dari besar ke normal tiap ketukan; tidak ada loncatan. | | |
| A-04 | Pinch zoom + preset 0.6x/1x/2x/5x. | Zoom halus, label preset ikut, tidak melewati min/max. | | |
| A-05 | Ketuk dua kali preview. | Eksposur reset ke 0. | | |

## 7. Lifecycle, izin, kegagalan

| ID | Langkah | Hasil yang diharapkan | Hasil | Catatan |
|---|---|---|---|---|
| LC-01 | Preview idle → Home → buka lagi (5×). | Preview pulih, tidak hitam, tidak crash. | | |
| LC-02 | `adb shell pm revoke com.fatih.adaptivecompositioncamera android.permission.CAMERA` saat app terbuka. | App ditutup sistem (perilaku Android). Buka lagi → layar izin → "Allow camera" → preview muncul tanpa loop. | | |
| LC-03 | Tolak izin mikrofon (`pm revoke ...RECORD_AUDIO`), rekam video. | Muncul pesan izin mikrofon / rekam tanpa audio sesuai pengaturan; tidak crash. | | |
| LC-04 | Aplikasi kamera lain (mis. kamera bawaan) membuka kamera, lalu kembali ke app ini. | Preview pulih atau muncul pesan error yang jelas; tidak crash. | | |
| LC-05 | Buka Settings (ikon gigi) lalu kembali, 5×. | Preview pulih setiap kali. | | |

## 8. Menjalankan tes otomatis instrumented di perangkat
```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
# Laporan: app\build\reports\androidTests\connected\index.html
```
Kelas: `ShutterControlsUiTest` (shutter/zoom/mode/rail landscape), `OrientationActivityTest` (Activity tidak recreate pada 20× rotasi).

## 9. Kriteria lulus sebelum merge
- V-01, V-02, V-03, J-01, J-02, O-02, O-03, O-05, O-07, P-01, P-02 **PASS**.
- Tidak ada `AndroidRuntime FATAL` di semua logcat.
- Tidak ada `CAMERAX_BIND` antara start & stop rekaman.
- Jika salah satu FAIL: lampirkan `logcat_<ID>.txt` + video layar (`adb shell screenrecord /sdcard/x.mp4`).
