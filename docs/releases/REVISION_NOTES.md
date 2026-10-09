# SadarBudget Android Offline — Revisi 2.0.1

Perubahan pada revisi ini:

- Pemilih file impor SQL menggunakan filter universal agar file `.sql` dapat dipilih pada Samsung/My Files dan document provider lain. Isi file tetap divalidasi oleh importer SadarBudget.
- PDF menampilkan nama pengguna pada header setiap halaman.
- Excel menampilkan nama pengguna, periode, filter jenis, dan ringkasan laporan sebelum tabel transaksi.
- Snackbar/notifikasi singkat ditambahkan untuk perubahan data yang berhasil: transaksi, kategori, profil, foto, kata sandi, backup, impor/restore, pembersihan data, dan penyimpanan hasil ekspor.
- Banner `Mode offline...` pada layar awal dihapus.
- `Buat akun lokal` diganti menjadi `Buat akun`, dan teks login/register dibuat lebih natural.
- Form pembuatan akun menampilkan petunjuk singkat nama, email, kata sandi minimal 8 karakter, dan konfirmasi kata sandi.
- Istilah `akun lokal` pada pesan kesalahan pengguna disederhanakan menjadi `akun`.

## Pengujian upgrade

Jangan uninstall aplikasi lama jika ingin mempertahankan database yang sudah ada. Buka project revisi ini lalu jalankan **Run** ke perangkat yang sama. Application ID tetap `id.sadarbudget.mobile`, sehingga instalasi debug dari komputer yang sama akan memperbarui aplikasi dan mempertahankan data selama signature debug sama.
