# VideoEdit (Android, Kotlin + Jetpack Compose)

Editor video ringan dengan monetisasi AdMob Rewarded Ads. Semua tahap (2–9) dari master prompt ada di proyek ini.

> **Status jujur:** kode ini ditulis dan diperiksa sintaksnya, tetapi **belum pernah dikompilasi atau dijalankan di perangkat** (lingkungan penulisan tidak punya Android SDK/Maven). API Media3 sudah dicocokkan dengan source resmi Media3 1.5.1. Kemungkinan ada error kecil saat build pertama; kirim pesan error aslinya dan akan diperbaiki.

## 1. Membuka & menjalankan
1. Android Studio (Ladybug/2024.2 atau lebih baru) → **Open** → pilih folder `VideoEdit`.
2. Tunggu **Gradle Sync** selesai (butuh internet; Gradle 8.9, AGP 8.7.3, Kotlin 2.0.21 diunduh otomatis).
3. Pilih perangkat/emulator (API 24+) → **Run ▶**. Lebih baik pakai HP asli: encoder video di emulator sering bermasalah.

## 2. Struktur kode (`app/src/main/java/com/dik/videoedit/`)
| Paket | Isi |
|---|---|
| `screens` | Home, Template, Settings, Editor (+ panel alat), Export |
| `components` | Timeline, dialog Fitur Premium, tombol, thumbnail |
| `navigation` | NavHost dan rute |
| `videoeditor` | PreviewController (ExoPlayer), ProjectOps (trim/split/dll), Importer |
| `videoprocessing` | ExportEngine (Transformer), EffectsFactory, ExportPlan, DeviceCaps, GallerySaver, ExportNotifier |
| `storage` | Proyek (JSON lokal), pengaturan (DataStore) |
| `admob` | RewardedAdManager, PremiumManager |
| `viewmodels`, `models`, `ui/theme` | seperti namanya |

## 3. Fitur
- **Gratis:** import dari galeri (bisa banyak), preview, trim, split, gabung klip, urutkan klip, crop rasio 9:16/1:1/16:9, rotasi, kecepatan 0,25x–3x, musik, teks, volume, undo/redo, export 480/720/1080p, pilihan FPS sesuai perangkat, estimasi ukuran, progres + batal, notifikasi, simpan ke galeri (`Movies/VideoEdit`). Template = preset awal (rasio/kecepatan/filter).
- **Premium (via iklan):** filter warna tambahan, efek video (zoom, cermin, blur, negatif), transisi fade hitam/putih, export tanpa watermark.
- **Sistem iklan:** satu rewarded ad membuka semua fitur premium **30 menit**. Reward hanya diberikan di callback `OnUserEarnedRewardListener`. Iklan **tidak** dimuat saat aplikasi dibuka, hanya saat dialog premium dibuka. Gagal muat → pesan + tombol coba lagi, tidak ada reward.
- Watermark "VideoEdit" muncul di export gratis.

## 4. Iklan test vs asli
- **Debug** selalu memakai ID test Google (sudah terpasang).
- **Release** memakai ID test juga, sampai Anda mengisi di `gradle.properties`:
  ```
  ADMOB_APP_ID=ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy
  ADMOB_REWARDED_ID=ca-app-pub-xxxxxxxxxxxxxxxx/zzzzzzzzzz
  ```
  Jangan klik iklan asli milik sendiri saat menguji (bisa membuat akun AdMob diblokir).

## 5. Yang harus Anda ganti sebelum rilis
1. `AppInfo.kt` → `SUPPORT_EMAIL` (tombol "Hubungi Kami").
2. ID AdMob asli (lihat atas).
3. `applicationId` di `app/build.gradle.kts` bila ingin nama paket sendiri.
4. Kebijakan Privasi: teks ringkas ada di `SettingsScreen.kt`. Play Store mewajibkan **URL** kebijakan privasi; buat halamannya dan isi di listing.

## 6. Checklist uji di HP (Tahap 8)
- [ ] Proyek Baru → pilih 1 video → editor terbuka, preview jalan.
- [ ] Potong: geser slider → durasi berubah; Pisah di tengah klip → jadi 2 klip; Hapus klip.
- [ ] Tambah video (+) → klip bertambah; Geser klip kiri/kanan.
- [ ] Crop 9:16 / 1:1 / 16:9, Putar 90°.
- [ ] Kecepatan 2x dan 0,5x (suara ikut berubah).
- [ ] Musik: pilih file audio, atur volume; teks: tambah, hapus.
- [ ] Filter premium → dialog premium → tonton iklan test → terbuka. Tutup iklan sebelum selesai → **tidak** terbuka.
- [ ] Mode pesawat → dialog premium menampilkan pesan gagal, tidak crash.
- [ ] Export 720p → progres naik → "Berhasil!" → video ada di galeri `Movies/VideoEdit` + notifikasi.
- [ ] Export lalu Batal → kembali ke pengaturan, tidak ada file sisa.
- [ ] Tutup & buka aplikasi → proyek masih ada di Beranda.

## 7. Build APK release (Tahap 9)
1. Buat keystore (sekali saja, **simpan aman, jangan hilang**):
   ```
   keytool -genkey -v -keystore videoedit-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias videoedit
   ```
2. Buat file `keystore.properties` di folder root proyek (sudah di-`.gitignore`):
   ```
   storeFile=videoedit-release.jks
   storePassword=PASSWORD_ANDA
   keyAlias=videoedit
   keyPassword=PASSWORD_ANDA
   ```
3. Isi ID AdMob asli di `gradle.properties`.
4. Build: **Build → Generate Signed Bundle / APK**, atau di terminal:
   - APK: `./gradlew assembleRelease` → `app/build/outputs/apk/release/app-release.apk`
   - AAB (Play Store): `./gradlew bundleRelease`
5. Uji APK release di HP asli (R8 aktif; pastikan export & iklan tetap jalan).

## 8. Batasan yang perlu Anda tahu
- Export berjalan **selama aplikasi terbuka** (layar dijaga menyala). Jika aplikasi ditutup, export berhenti.
- Transisi (fade) hanya terlihat di **hasil export**, preview menampilkan potongan langsung.
- Jika klip memiliki rasio berbeda dan rasio "Asli" dipilih, hasil mengikuti rasio klip pertama (klip lain di-crop).
- Volume dibatasi 0–100% (tidak ada penguatan di atas 100%).
- Video dari galeri tidak disalin; bila video asli dihapus/dipindah, klip itu dilepas dari proyek saat dibuka.
- Efek video bergantung GPU; di HP sangat lama, preview efek bisa berat (export tetap berjalan).
