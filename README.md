<div align="center">
  <img src="app/src/main/res/drawable/sadarbudget_logo_color.png" alt="Logo SadarBudget" width="110" />

  # SadarBudget Android

  **Aplikasi pencatat keuangan pribadi yang offline, sederhana, dan menjaga privasi.**

  ![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android)
  ![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?logo=kotlin)
  ![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)
  ![Version](https://img.shields.io/badge/Version-2.0.34-2563EB)
</div>

> **Status:** v2.0.34, rilis source untuk kolaborasi. Aplikasi sudah diuji secara manual pada perangkat Android; konfigurasi build otomatis (CI) disertakan tetapi belum diverifikasi di GitHub Actions. Tidak ada server atau akun cloud yang diperlukan.

## Tentang aplikasi

SadarBudget membantu pengguna mengelola catatan pemasukan dan pengeluaran secara lokal. Akun aplikasi, kategori, transaksi, laporan, dan data tersimpan di perangkat. Repository ini berisi **source Android native**, bukan backend PHP/MySQL atau data milik pengguna.

## Tampilan Aplikasi

<p align="center">
  <img src="docs/img (1).jpg" width="220">
  <img src="docs/img (2).jpg" width="220">
  <img src="docs/img (3).jpg" width="220">
  <img src="docs/img (4).jpg" width="220">
  <img src="docs/img (5).jpg" width="220">
</p>

## Fitur

- Pencatatan pemasukan dan pengeluaran beserta kategori dan riwayat.
- Ringkasan saldo, laporan bulanan/tahunan, dan grafik.
- Pencarian riwayat, template transaksi, serta Undo setelah menghapus transaksi.
- Akun dan profil lokal, tema terang/gelap bernuansa biru, UI responsif.
- PIN 6 digit, biometrik bila perangkat mendukung, opsi sembunyikan nominal dan pembatasan screenshot.
- Backup impor/ekspor SQL dan file `.sbb` terenkripsi kata sandi.
- Ekspor laporan PDF dan Excel (`.xlsx`) tanpa koneksi internet.

**Fitur yang belum tersedia:** sinkronisasi cloud, integrasi bank, multi-wallet, kalender transaksi, dan widget layar utama. Lihat [Roadmap](#roadmap) untuk rencana umum, bukan janji rilis.

## Persyaratan

| Komponen | Nilai |
|---|---|
| Android minimum | Android 8.0 (API 26) |
| `compileSdk` dan `targetSdk` | 37 |
| Bahasa | Kotlin |
| UI | Jetpack Compose / Material 3 |
| Database | SQLite (`SQLiteOpenHelper`) |
| Java untuk kompilasi | JDK 17 atau konfigurasi yang didukung AGP |
| Gradle | 9.6.0 (lihat `gradle/wrapper/gradle-wrapper.properties`) |

## Menjalankan dari source

1. Instal **Android Studio** dan Android SDK platform 37 yang diperlukan proyek.
2. Unduh repositori melalui **Code → Download ZIP** atau `git clone <URL-REPO-ANDA>`.
3. Buka folder root `SadarBudget-Android` di Android Studio (jangan hanya folder `app`).
4. Jalankan **Gradle Sync**. Jika Android Studio meminta konfigurasi SDK, atur lokasi SDK melalui IDE; jangan commit `local.properties`.
5. Pilih modul `app`, sambungkan ponsel atau emulator Android 8.0+, lalu **Run**.

Repositori ini mewarisi `gradle-wrapper.properties`, tetapi **belum menyertakan `gradlew`, `gradlew.bat`, atau `gradle-wrapper.jar`** karena file itu tidak tersedia pada source versi asal. Android Studio dapat dipakai sebagaimana workflow proyek sebelumnya, atau pengembang dapat menginstal Gradle 9.6.0 dan menjalankan `gradle :app:assembleDebug`. Untuk melengkapi Gradle Wrapper setelah Gradle tersedia, jalankan `gradle wrapper --gradle-version 9.6.0`, lalu commit ketiga file wrapper yang dihasilkan setelah memverifikasi asal dan checksum JAR. CI menggunakan Gradle yang diunduh oleh `gradle/actions/setup-gradle`, bukan memanggil `./gradlew`.

> **Penting:** jangan uninstall aplikasi pada perangkat berisi transaksi nyata hanya demi mencoba build baru. Uninstall atau hapus data aplikasi dapat menghapus database lokal. Buat backup terpisah terlebih dahulu.

## Struktur proyek

```text
SadarBudget-Android/
├── app/
│   ├── src/main/java/id/sadarbudget/mobile/
│   │   ├── App.kt                  # navigasi/komposisi UI
│   │   ├── AppState.kt             # state aplikasi
│   │   ├── data/                   # database, repository, backup, keamanan
│   │   └── ui/
│   │       ├── components/         # komponen UI dan grafik
│   │       ├── screens/            # halaman aplikasi
│   │       └── theme/              # desain & tema
│   └── src/test/                  # pengujian unit
├── .github/                       # alur kontribusi dan CI
├── docs/                          # arsitektur, pengujian, catatan versi
├── CONTRIBUTING.md
├── SECURITY.md
├── LICENSE
└── README.md
```

Detail arsitektur: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md). Prosedur pengujian: [docs/TESTING.md](docs/TESTING.md).

## Privasi & keamanan

- Aplikasi **tidak memerlukan izin INTERNET**; data akun dan transaksi tidak dikirim ke backend.
- Database disimpan di sandbox aplikasi; backup Android otomatis dinonaktifkan.
- Kata sandi akun dan PIN disimpan dalam bentuk hash bersalt, bukan teks biasa.
- PIN/biometrik merupakan **kunci antarmuka**, **bukan enkripsi database SQLite**. Jangan menganggapnya sebagai proteksi terhadap semua akses pada perangkat yang telah di-root/kompromi.
- Backup `.sbb` menggunakan AES-256-GCM; backup SQL biasa **tidak terenkripsi**.
- **Jangan pernah** menyertakan backup pengguna, log berisi data asli, PIN, kata sandi, atau keystore dalam Issue/PR.

Lapor kerentanan melalui [SECURITY.md](SECURITY.md), bukan Issue publik.

## Kontribusi

Kontribusi sangat terbuka, terutama terkait stabilitas, aksesibilitas, pengujian, dokumentasi, dan optimasi performa. Baca [CONTRIBUTING.md](CONTRIBUTING.md), ikuti template Issue dan Pull Request, serta sertakan hasil pengujian.

## Roadmap

Roadmap sementara dan dapat berubah: penyempurnaan keamanan, pengujian otomatis, aksesibilitas, pemulihan data, optimalisasi layar kecil, serta perapian modularitas codebase. Fitur cloud dan analisis keuangan berbasis asumsi **bukan prioritas**.

## Dokumentasi & riwayat perubahan

- [CHANGELOG.md](CHANGELOG.md) — ringkasan perubahan.
- [docs/releases/](docs/releases/) — catatan revisi terdahulu.
- [docs/GITHUB_PUBLISHING.md](docs/GITHUB_PUBLISHING.md) — panduan publikasi di GitHub.

## Lisensi dan pengembang

Lisensi kode dalam repositori: **MIT** (lihat [LICENSE](LICENSE)). Nama pengembang: **Ahmad Asyhari**. Sebelum memublikasikan sebagai open source, pemilik repositori perlu memastikan hak penggunaan/distribusi logo dan aset visual serta memeriksa kesesuaian lisensi setiap kontribusi pihak ketiga.

Library Android/Kotlin/Coil memiliki lisensi masing-masing; lisensi MIT project **tidak menggantikan lisensi dependensi**.
