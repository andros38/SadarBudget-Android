# Kebijakan Keamanan

SadarBudget menyimpan data keuangan lokal yang bersifat pribadi. Kerentanan keamanan harus ditangani dengan hati-hati.

## Melaporkan kerentanan

Gunakan **GitHub → Security → Advisories → Report a vulnerability** jika fitur pelaporan privat sudah diaktifkan oleh pemelihara repositori. Jangan mengunggah exploit, credential, PIN, dump database, atau backup pengguna ke Issue publik. Jika private vulnerability reporting belum aktif, minta pemelihara menyediakan kanal privat tanpa menyebutkan detail kerentanan di Issue.

Sertakan (tanpa data pribadi): versi aplikasi, versi Android, langkah reproduksi dengan data dummy, dampak, dan mitigasi yang diketahui. Pemelihara akan menilai dan mengatur perbaikan; belum ada SLA resmi.

## Batasan perlindungan data

- PIN/biometrik mengunci UI aplikasi, **tidak melakukan enkripsi keseluruhan file database**.
- Backup `.sbb` menggunakan enkripsi terautentikasi AES-GCM berbasis kata sandi. Backup `.sql` tetap merupakan teks biasa.
- Android system backup dinonaktifkan dan app tidak membutuhkan izin internet.
- Pastikan perangkat menggunakan penguncian sistem yang aman, serta tidak di-root/kompromi.
- Proyek ini belum menjalani audit keamanan eksternal formal.

## Untuk kontributor

Jangan commit file `.db`, `.sql`, `.sbb`, keystore `.jks`, `local.properties`, atau log sensitif. Review perubahan `SecurityStore.kt`, `EncryptedBackup.kt`, `PasswordHasher.kt`, dan prosedur restore dengan perhatian ekstra.
