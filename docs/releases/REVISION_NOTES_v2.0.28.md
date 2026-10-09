# SadarBudget Android v2.0.28

## Perubahan
- Mengganti logo aplikasi ke versi berwarna (wallet IDR glossy).
- Menggunakan logo berwarna yang sama pada ikon launcher dan logo aplikasi di dalam UI.
- Menjaga konsistensi visual agar selaras dengan ikon aplikasi lain yang memakai warna.

## Detail teknis
- `android:icon` dan `android:roundIcon` tetap memakai `@drawable/sadarbudget_launcher`, tetapi asetnya kini diganti ke versi berwarna.
- Komponen `SadarBudgetLogo()` kini memakai `R.drawable.sadarbudget_logo_color`.
