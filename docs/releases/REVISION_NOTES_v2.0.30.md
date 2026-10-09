# SadarBudget Android v2.0.30

## Quick transaction popup
- Mengganti menu `Catat` dari `ModalBottomSheet` / dialog besar menjadi popup floating yang ringkas.
- Popup muncul di atas navigation bar pada ponsel dan tetap kompak pada layout lebar.
- Mendukung dismiss dengan tap di luar, tombol X, dan tombol Back.
- Menambahkan dua quick action:
  - Catat pemasukan — Tambahkan uang yang masuk.
  - Catat pengeluaran — Tambahkan uang yang keluar.
- Aksi tetap menggunakan alur transaksi lama; tidak ada perubahan database atau repository.
- Tampilan mengikuti tema biru pada mode terang dan gelap.
