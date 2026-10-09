# SadarBudget Android v2.0.32 — Keamanan, privasi dan efisiensi

## Fitur yang ditambahkan

1. **Kunci aplikasi berbasis PIN 6 digit**, hash PBKDF2 + salt acak per akun, tidak menyimpan PIN polos. Kunci muncul kembali setelah aplikasi ditinggalkan ke latar belakang. Lima percobaan PIN salah memicu jeda 30 detik yang dipersistenkan.
2. **Biometrik Android** melalui dialog OS pada Android 9+ bila perangkat menyediakan hardware/enrollment yang bisa dipakai. Memerlukan PIN terlebih dahulu; PIN tetap menjadi alternatif. Android 8.0/8.1 tetap dapat memakai PIN.
3. **Lupa PIN** dapat direset dengan kata sandi akun; tidak perlu menghapus data.
4. **Mode privasi Ringkasan**: sembunyikan nominal saldo, pemasukan/pengeluaran, grafik, dan aktivitas terbaru; ikon mata di kartu saldo. Pilihan untuk mencegah tangkapan layar / pratinjau melalui `FLAG_SECURE`.
5. **Riwayat**: pencarian kategori, keterangan, tanggal, dan nominal, termasuk debounce singkat.
6. **Undo hapus transaksi** melalui Snackbar dan pengembalian baris SQLite dengan ID/tanggal/nominal aslinya. Undo hanya selama pesan tersedia; backup pengaman otomatis yang ada tetap dipakai.
7. **Template transaksi cepat**: simpan nama, jenis, nominal, kategori, keterangan untuk mengisi form, atau hapus template. Tidak membuat transaksi tanpa konfirmasi. Template disimpan per akun dan dibawa pada payload backup SadarBudget baru.
8. **File backup terenkripsi `.sbb`**: AES-256-GCM + PBKDF2-HMAC-SHA256 210.000 putaran, salt 16 byte dan nonce 12 byte acak. Memerlukan kata sandi akun dan kata sandi file yang berbeda fungsinya. Arsip memverifikasi autentisitas melalui tag GCM. Impor SQL lama dan backup pengaman lokal tetap kompatibel.

## Kompatibilitas dan pembatasan

- `versionCode=34`, `versionName=2.0.32`, `minSdk=26` (Android 8.0).
- Struktur tabel SQLite lama tidak diubah; identitas package `id.sadarbudget.mobile` tetap sama. Pembaruan dapat diinstal di atas versi lama tanpa uninstall.
- Penguncian PIN merupakan proteksi akses UI; file database lokal **tidak dienkripsi ulang** pada pembaruan ini. Pilihan backup `.sql` lama masih tersedia dan **tidak terenkripsi**.
- PIN/biometrik harus diaktifkan manual di **Akun > Keamanan**. Sembunyikan saldo dan blokir screenshot juga bersifat opt-in.
- File `.sbb` hanya bisa diimpor aplikasi SadarBudget Android yang mendukung format ini. Simpan kata sandi file di tempat aman; aplikasi tidak dapat memulihkannya.
- Template yang tidak lagi memiliki kategori aktif dinonaktifkan saat ditampilkan agar tidak menghasilkan data tidak valid.
- Fitur widget home-screen, lampiran struk, kalender, dan multi-wallet **belum ditambahkan** dalam rilis ini.

## Prosedur uji manual yang disarankan

1. Update atas versi lama **tanpa uninstall**, pastikan transaksi/kategori tetap sama.
2. Akun > Keamanan > buat PIN; alihkan aplikasi ke latar belakang dan kembali; coba PIN benar/salah, biometrik jika ada, dan recovery kata sandi akun.
3. Aktifkan penyembunyian saldo; periksa semua nilai Ringkasan dan toggle ikon mata. Aktifkan blokir screenshot untuk mencoba larangan tangkapan layar.
4. Buat template dari form Catat; terapkan ke formulir lain, simpan transaksi, hapus template.
5. Hapus transaksi, segera pilih **Urungkan**; pastikan nominal dan laporan kembali seperti semula.
6. Cari transaksi lewat kategori, deskripsi, nominal dan tanggal, lalu reset pencarian.
7. Data & Backup > ekspor `.sbb` > impor ke akun yang sama. Pastikan kata sandi file salah ditolak dan SQL `.sql` lama masih bisa dibaca.
