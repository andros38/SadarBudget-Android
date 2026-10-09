# Checklist Pengujian

Gunakan **data dummy** (mis. saldo Rp 5.000.000 dan dua transaksi fiktif). Jangan mengunggah screenshot data asli ke GitHub.

## Otomatis

- Dari Android Studio: jalankan tes unit `app/src/test` dan build Debug.
- Jika Gradle 9.6.0 tersedia pada PATH: `gradle :app:testDebugUnitTest :app:assembleDebug`.
- Pipeline GitHub Actions menjalankan tugas yang sama, setelah `compileSdk=37` tersedia.

## Manual sebelum merge/rilis

- [ ] Instal pembaruan di atas aplikasi lama (tanpa uninstall): transaksi dan kategori tidak hilang.
- [ ] Registrasi/login/logout akun lokal berhasil.
- [ ] Transaksi pemasukan dan pengeluaran dapat dibuat, diedit, dihapus dan di-Undo.
- [ ] Total saldo, ringkasan, laporan dan filter tetap konsisten.
- [ ] Template mengisi form tanpa membuat transaksi otomatis.
- [ ] PIN salah lima kali memicu cooldown; PIN benar, reset PIN dan relock berfungsi.
- [ ] Biometrik pada perangkat yang mendukung; fallback PIN jika batal/gagal.
- [ ] Sembunyikan nominal dan proteksi screenshot mengikuti pengaturan.
- [ ] Backup SQL lama dan backup `.sbb` baru dapat diimpor; sandi `.sbb` salah ditolak.
- [ ] PDF dan Excel terbuka dengan format/nominal sesuai.
- [ ] Mode terang/gelap, orientasi potret, dan layar sempit tidak mengalami overflow.
- [ ] Tombol back/dismiss popup Catat berfungsi.
- [ ] Tidak ada database, file backup, atau PIN pengguna yang masuk Issue/PR.

## Catatan CI

CI bergantung pada artefak Gradle, plugin dan Android SDK yang diunduh runner. Pipeline baru perlu diverifikasi setelah repositori pertama kali dipublikasikan. CI tidak menggantikan tes perangkat nyata dan tidak menjalankan uji UI instrumen.
