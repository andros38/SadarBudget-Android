# Changelog

Perubahan ini diringkas dari catatan pengembangan SadarBudget Android. Arsip per-versi tetap disimpan di [`docs/releases/`](docs/releases/).

## [2.0.34] - 2026-10-08
- Memperbaiki konflik setter JVM `hideAmounts` dan `secureWindow` pada `AppState`.
- Versi yang dikonfirmasi pengguna telah berhasil dijalankan dan dikunci/dibuka memakai PIN.

## [2.0.33] - 2026-10-08
- Memperbaiki referensi konstanta callback biometrik yang gagal dikompilasi.

## [2.0.32] - 2026-10-08
- Memperkenalkan PIN/biometrik, privasi saldo, opsi screenshot aman, Undo hapus, template transaksi, pencarian riwayat, dan backup `.sbb` terenkripsi.

## [2.0.31] - 2026-10-08
- Memisahkan sapaan dan saldo pada Home agar informasi lebih jelas.

## [2.0.30] - 2026-10-08
- Mengubah menu Catat menjadi quick popup floating.

## [2.0.29] - 2026-10-08
- Menyeragamkan tema terang/gelap dan ekspor laporan dengan aksen biru.

## [2.0.28] - 2026-10-08
- Mengganti ikon aplikasi ke logo dompet IDR berwarna.

## Versi lebih lama
- Perbaikan dashboard, grafik, laporan, tema, avatar, kategori, dan kompatibilitas telah dicatat dalam arsip `docs/releases/REVISION_NOTES_v*.md`.

## GitHub source packaging (tanpa perubahan versionName)
- Menambahkan dokumentasi, lisensi, panduan kontribusi, template Issue/PR, CI, dan pengujian dasar fungsi enkripsi.
- Menghapus file Gradle daemon JVM yang bersifat spesifik mesin dan mengelompokkan arsip catatan lama.
