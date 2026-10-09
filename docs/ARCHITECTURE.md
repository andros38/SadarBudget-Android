# Arsitektur SadarBudget Android

## Alur aplikasi

```text
MainActivity
    └── SadarBudgetApp (App.kt)
          ├── AppState (state & koordinasi session)
          ├── screens/ (Jetpack Compose)
          ├── ui/components/ dan ui/theme/
          └── AppRepository (operasi data/report)
                 ├── LocalDatabase (SQLiteOpenHelper, sadarbudget.db)
                 ├── SessionStore (session & preferensi tema)
                 ├── SecurityStore (PIN & preferensi keamanan)
                 ├── TemplateStore (template per akun)
                 ├── PasswordHasher (hash dengan salt)
                 └── EncryptedBackup (format SBB1)
```

## Data

- Seluruh transaksi, kategori, dan akun lokal disimpan dalam SQLite pribadi aplikasi.
- `LocalDatabase.kt` mendefinisikan tabel `users`, `categories`, `transactions`, indeks, dan foreign key.
- `AppRepository.kt` menangani operasi database, impor/ekspor backup, serta output laporan.
- `SecurityStore` menyimpan preferensi keamanan per akun. Mode PIN bukan enkripsi database.
- `TemplateStore` menyimpan shortcut pengisian form per akun.
- Database schema saat ini version 1. Jika mengubah schema, tingkatkan versi dan tulis `onUpgrade` yang aman **beserta tes migrasi**.

## Backup

- SQL lama: format berpenanda `SADARBUDGET_PAYLOAD`. Importer **tidak mengeksekusi SQL arbitrer** dari file unggahan.
- `.sbb`: kontainer `SBB1` dengan 4 byte magic, salt 16 byte, nonce 12 byte, dan ciphertext+tag AES-256-GCM. Password derivation: PBKDF2-HMAC-SHA256 dengan 210.000 iterasi.
- Jangan mengubah payload / import tanpa mempertahankan format lama atau menyediakan migrator yang diuji.

## UI

- `ui/screens/` untuk halaman; `ui/components/` untuk komponen bersama dan grafik; `ui/theme/` untuk palet biru dan token desain.
- Navigasi utama: Ringkasan, Riwayat, Catat (quick popup), Laporan, Akun.
- UI harus dapat dipakai di smartphone mini/menengah dan layar lebih besar, termasuk mode terang/gelap.

## Batasan

- Tidak tersedia server atau sinkronisasi internet.
- Tidak ada analytics/telemetri.
- Akses database langsung dan perubahan format backup berisiko tinggi terhadap data pribadi.
