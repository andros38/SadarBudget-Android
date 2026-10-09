# SadarBudget Android v2.0.33 — Perbaikan kompilasi BiometricPrompt

- Menghapus referensi konstanta `BiometricPrompt.BIOMETRIC_ERROR_NEGATIVE_BUTTON` dari API platform Android pada `AppSecurityScreen.kt` karena konstanta tersebut tidak tersedia.
- Pembatalan biometrik oleh pengguna (`BIOMETRIC_ERROR_USER_CANCELED` / `BIOMETRIC_ERROR_CANCELED`) tetap tidak diperlakukan sebagai kegagalan, dan aksi tombol **Gunakan PIN** tetap tersedia.
- `versionCode=35`, `versionName=2.0.33`.
- Tidak mengubah package, skema database, data, atau fitur aplikasi yang lain.

**Verifikasi:** pengecekan statis dan uji integritas ZIP; build APK memerlukan Android Studio beserta Android SDK yang terinstal.
