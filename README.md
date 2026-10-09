<div align="center">
  <img src="app/src/main/res/drawable/sadarbudget_logo_color.png" alt="Logo SadarBudget" width="110" />

  # SadarBudget Android

  **Aplikasi pencatat keuangan pribadi yang offline, sederhana, dan menjaga privasi.**

  ![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android)
  ![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?logo=kotlin)
  ![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)
  ![Version](https://img.shields.io/badge/Version-2.0.34-2563EB)
</div>

## Tentang aplikasi

SadarBudget membantu pengguna mengelola catatan pemasukan dan pengeluaran secara lokal. Akun aplikasi, kategori, transaksi, laporan, dan data tersimpan di perangkat.

## Tampilan Aplikasi

<p align="center">
  <img src="docs/img (1).jpg" width="120">
  <img src="docs/img (3).jpg" width="120">
  <img src="docs/img (4).jpg" width="120">
  <img src="docs/img (5).jpg" width="120">
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

Detail arsitektur: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).
Prosedur pengujian: [docs/TESTING.md](docs/TESTING.md).

## Privasi & keamanan
- Aplikasi tidak memerlukan izin internet. Data akun disimpan di sandbox aplikasi.
- Kata sandi akun dan PIN diamankan dalam bentuk hash.
- PIN/biometrik untuk mencegah awal dibuka aplikasi tersebut.

## Kontribusi
Bagi yang ingin kontribusi atau mengembangkan aplikasi ini, silahkan baca [CONTRIBUTING.md](CONTRIBUTING.md).

## Dokumentasi & riwayat perubahan

- [CHANGELOG.md](CHANGELOG.md) — ringkasan perubahan.
- [docs/releases/](docs/releases/) — catatan revisi terdahulu.
- [docs/GITHUB_PUBLISHING.md](docs/GITHUB_PUBLISHING.md) — panduan publikasi di GitHub.

## Lisensi
Lisensi kode dalam repositori: **MIT** (lihat [LICENSE](LICENSE))
