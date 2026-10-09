# SadarBudget Android v2.0.24 — Annual Analytics + 3D Design System

Pembaruan utama:

1. Laporan Tahunan dirombak besar menjadi dashboard analitik:
   - hero tahunan dengan pemilih tahun
   - ringkasan masuk / keluar / saldo dalam satu panel
   - grafik arus 12 bulan sebagai visual utama
   - pemilih bulan horizontal
   - detail bulan, sorotan, dan kategori pengeluaran disusun ulang
   - layout mini dibuat lebih hemat, sedangkan layar besar memakai distribusi ruang lebih lebar

2. Kartu 3D dibuat konsisten:
   - semua SbCard dan MetricCard memakai border, elevasi, highlight, dan gradient yang sama
   - hero Dashboard dan hero Laporan mengikuti bahasa visual yang sama
   - mode gelap memakai dimensi melalui border/highlight tanpa shadow berlebih

3. Style system terpusat:
   - file DesignSystem.kt menjadi sumber window class, spacing, radius, elevation, control height, nav height, dan rail width
   - kelas layar: Mini, Medium, Large, Expanded
   - tombol resmi: SbPrimaryButton, SbSecondaryButton, SbTonalButton, SbDangerButton
   - chip resmi: SbFilterChip
   - typography base dan mini distandarkan
   - autentikasi, transaksi, profil, kategori, backup, navigasi, dan laporan memakai komponen/tokens bersama

Fungsi data, database offline, login, backup, export, dan transaksi tidak diubah.
