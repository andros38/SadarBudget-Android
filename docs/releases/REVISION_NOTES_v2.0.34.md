# SadarBudget Android v2.0.34 — Perbaikan JVM Signature Clash

- Mengatasi 4 error Kotlin `Platform declaration clash` di `AppState.kt`.
- Properti `var hideAmounts` dan `var secureWindow` memiliki setter yang dihasilkan compiler (`setHideAmounts(Boolean)`, `setSecureWindow(Boolean)`), yang bentrok dengan fungsi publik bernama sama walaupun properti memiliki `private set`.
- Mengganti fungsi publik dengan `updateHideAmounts(Boolean)` dan `updateSecureWindow(Boolean)`.
- Memperbarui pemanggilan di `App.kt` dan `AppSecurityScreen.kt`; properti Compose state dan logika penyimpanan preferensi tetap sama.
- Tidak mengubah kode SQLite, tabel, transaksi, penyimpanan atau keamanan data.
- `versionCode = 36`, `versionName = 2.0.34`.

**Verifikasi:** audit referensi serta arsip ZIP. Build APK perlu dilakukan di Android Studio pada perangkat pengguna.
