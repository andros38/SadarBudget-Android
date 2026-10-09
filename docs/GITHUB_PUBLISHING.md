# Cara memublikasikan SadarBudget di GitHub

## Metadata repositori (saran)

- **Nama:** `SadarBudget-Android`
- **Description:** `Aplikasi pencatatan keuangan pribadi Android yang sepenuhnya offline, dibangun dengan Kotlin, Jetpack Compose, dan SQLite.`
- **Topics:** `android`, `kotlin`, `jetpack-compose`, `sqlite`, `personal-finance`, `offline-first`, `budget-tracker`, `open-source`, `indonesia`
- **Homepage:** kosongkan bila belum ada situs resmi.
- **Visibility:** `Public` jika hak kode dan aset sudah dipastikan serta siap menerima kontribusi.

## Sebelum mengunggah

1. **Baca LICENSE.** Paket ini menyiapkan lisensi MIT atas nama Ahmad Asyhari, 2026; pemilik harus menyetujui pilihan lisensi dan memastikan berhak mendistribusikan seluruh kode/aset sebelum repositori dibuat publik.
2. Pastikan tidak ada transaksi sungguhan, foto profil pribadi, backup `.sql`/`.sbb`, `.db`, token, keystore, sertifikat signing, file `.env`, atau `local.properties`.
3. Gunakan file dari **paket GitHub-ready**, bukan ZIP pengembangan yang berisi banyak catatan root.
4. Jika ada screenshot promosi, hanya gunakan akun dummy dan sensor seluruh data pribadi.
5. Jalankan build Debug di Android Studio dan catat hasilnya. Build otomatis belum diverifikasi di hosting GitHub.

## Membuat repositori dengan antarmuka GitHub

1. Login GitHub → klik **New repository**.
2. Masukkan nama/deskripsi/topics di atas, pilih **Public** jika sudah siap.
3. **Jangan aktifkan** opsi membuat README, LICENSE atau .gitignore dari GitHub: semuanya sudah ada.
4. Buat repositori, lalu unggah seluruh isi folder hasil ekstrak paket ini (termasuk `.github`, `.gitignore`, `.gitattributes`) ke root repo melalui Git, atau GitHub Upload Files yang mendukung folder.
5. Aktifkan **Settings → General → Issues** dan **Settings → Security → Private vulnerability reporting** bila tersedia. Aktifkan **Discussions** bila ingin menampung ide/pertanyaan.
6. Tambahkan topics, deskripsi, dan URL repo. Tandai v2.0.34 sebagai rilis awal (tag `v2.0.34`) setelah berhasil di-build dan diuji ulang.
7. Cek tab **Actions**: workflow `Android CI` harus dicoba pada `workflow_dispatch` atau push/PR. Perbaiki masalah SDK/dependency bila muncul.
8. Di **Settings → Branches / Rulesets**, sarankan PR dan pemeriksaan CI sebelum merge ke branch utama setelah CI terbukti stabil.

## Alternatif menggunakan Git (direkomendasikan)

Jalankan dari direktori hasil ekstrak (bukan direktori di atasnya):

```bash
git init
git branch -M main
git add .
git status
git commit -m "chore: initial public SadarBudget Android source v2.0.34"
git remote add origin https://github.com/USERNAME/SadarBudget-Android.git
git push -u origin main
```

Bila repositori GitHub sudah dibuat kosong, ganti `USERNAME` dengan akun atau organisasi GitHub yang benar. **Periksa `git status` sebelum commit** agar tidak ada file sensitif yang ikut. Saat pengembangan berikutnya, gunakan branch/PR dan hindari force push.

## Membuat GitHub Release

- Tag: `v2.0.34`
- Title: `SadarBudget Android v2.0.34 — Initial Open Source Release`
- Isi release notes: fitur utama, minimum Android 8.0, kemampuan offline, batasan keamanan (PIN bukan enkripsi database), cara build, dan catatan backup.
- Lampiran APK: **opsional**; hanya tambahkan APK setelah signing/build dan pengujian dinyatakan valid. Jangan commit APK ke source root.

## Teks singkat pengumuman

> SadarBudget Android kini tersedia sebagai proyek open source. Dibuat menggunakan Kotlin, Jetpack Compose, dan SQLite, aplikasi ini membantu pencatatan pemasukan/pengeluaran tanpa server dan tanpa koneksi internet. Kontribusi dokumentasi, bugfix, pengujian, aksesibilitas, dan peningkatan stabilitas sangat diterima.

## Catatan tentang Gradle Wrapper

Source asal belum menyediakan `gradlew`/`gradle-wrapper.jar`. Workflow CI paket ini mengunduh Gradle 9.6.0 melalui GitHub Action resmi. Untuk kemudahan kontributor lokal, tambahkan Gradle Wrapper lengkap di commit berikutnya dari mesin dengan Gradle terpercaya, dan verifikasi JAR berdasarkan distribusi resmi sebelum commit.
