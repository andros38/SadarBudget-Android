# SadarBudget Android Offline 2.0.2

Perubahan UI/UX utama:

- Laporan tahunan tidak lagi menampilkan 12 kartu bulan secara memanjang.
- Bagian rincian bulan sekarang menggunakan dropdown bulan dan hanya menampilkan satu bulan yang dipilih.
- Halaman Data & Backup disederhanakan menjadi empat area: Ekspor, Impor, Pemulihan cepat, dan Bersihkan data.
- Daftar snapshot panjang dihapus dari tampilan utama.
- Versi pemulihan dipilih melalui dropdown.
- Retensi versi pemulihan dikurangi dari 40 menjadi maksimal 8 berkas per akun.
- Backup periodik setiap 30 menit dihapus.
- Backup tidak lagi dibuat setelah setiap transaksi/kategori baru.
- Versi pengaman tetap dibuat sebelum operasi yang berisiko mengganti atau menghapus data, seperti edit/hapus transaksi, nonaktifkan kategori, impor, restore, dan pembersihan data.
- Snapshot lama di atas batas retensi dirapikan otomatis saat halaman Data & Backup dibuka.

Versi aplikasi:

- versionCode: 4
- versionName: 2.0.2-offline
