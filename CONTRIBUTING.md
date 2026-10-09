# Panduan Kontribusi SadarBudget Android

Terima kasih sudah bersedia berkontribusi. Tujuan proyek adalah aplikasi pencatatan keuangan **offline-first, privat, ringan, dan mudah digunakan**.

## Sebelum mulai

1. Baca [README.md](README.md), [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md), dan [SECURITY.md](SECURITY.md).
2. Cari Issue yang sudah ada sebelum membuka Issue baru.
3. Untuk perubahan besar (database, backup, autentikasi, navigasi), diskusikan rencana desain terlebih dahulu di Issue.
4. Gunakan data dummy — jangan unggah database, backup, atau screenshot yang berisi data pengguna asli.

## Workflow Pull Request

1. Fork repositori, buat branch bertopik, misalnya `fix/history-filter` atau `feat/accessibility`.
2. Ubah hanya area yang relevan, sertakan penjelasan alasan dan dampaknya.
3. Jalankan pengujian unit dan build Debug bila lingkungan memungkinkan.
4. Lakukan pengujian manual pada emulator/ponsel (khususnya dark/light, layar kecil, login dan transaksi).
5. Buat Pull Request menggunakan template; tambahkan screenshot yang telah menggunakan data dummy bila ada perubahan UI.

## Aturan teknis

- Gunakan Kotlin idiomatis dan Compose; pertahankan UI adaptif dan aksesibilitas teks.
- Jangan menambah permission INTERNET, API cloud, telemetri, atau pelacakan tanpa diskusi eksplisit.
- Pertahankan nama paket `id.sadarbudget.mobile` agar instalasi pembaruan tidak memutus data.
- **Jangan mengganti/hapus tabel SQLite atau format backup tanpa rencana migrasi dan tes round-trip.**
- Hindari menyimpan PIN, kata sandi, key, atau informasi autentikasi sebagai teks biasa.
- Jangan memakai warna sukses/gagal secara sewenang-wenang; pertahankan semantik UI dan keterbacaan tema gelap/terang.
- Prefer perubahan kecil dan dapat ditinjau daripada satu PR yang merombak banyak fitur.
- Gunakan `ktlint` atau format bawaan IDE bila tersedia; jangan melakukan reformat massal kode di luar scope PR.

## Hal yang perlu diuji

- Kompilasi Debug tanpa error.
- Buat/edit/hapus transaksi; saldo/laporan tetap konsisten.
- Penguncian PIN dan biometrik cadangan pada perangkat yang mendukung.
- Backup SQL lama masih dapat diimpor; backup terenkripsi menolak kata sandi salah.
- Tidak ada kehilangan data ketika aplikasi diperbarui tanpa uninstall.

Lihat [docs/TESTING.md](docs/TESTING.md) untuk checklist lengkap.

## Lisensi kontribusi

Dengan mengirim kontribusi, kontributor menyatakan memiliki hak terhadap kode/aset yang dikirim dan setuju kontribusi tersebut disertakan dengan lisensi [MIT](LICENSE) yang berlaku untuk proyek ini.

## Masalah keamanan

Jangan membuka Issue untuk kerentanan yang belum diperbaiki. Ikuti [SECURITY.md](SECURITY.md).
