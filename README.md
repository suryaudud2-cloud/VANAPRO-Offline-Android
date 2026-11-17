# VANAPRO Offline Android

Aplikasi Android untuk memilih dan mengekspor ulang video secara lokal menggunakan AndroidX Media3 Transformer.

## Target
- Xiaomi POCO M5s, RAM 6 GB
- Android API 26+
- Compile/target SDK 35
- JDK 17 dan Gradle 8.9
- Video sumber hingga 1080p, bergantung codec dan ruang penyimpanan

## Privasi dan pemrosesan
- Tidak meminta permission INTERNET.
- Tidak memakai server, akun, atau lisensi.
- Video dipilih melalui Android Storage Access Framework.
- Transcode/ekspor MP4 dilakukan pada perangkat.

## Build APK
GitHub Actions membangun APK debug pada setiap push ke branch main. Buka tab **Actions**, pilih workflow **Build VANAPRO Offline APK**, lalu unduh artifact **VANAPRO-Offline-debug-APK** setelah status berhasil.

## Batasan versi awal
Versi ini melakukan transcode/ekspor lokal MP4. Grain film, EQ/reverb, preset warna lanjutan, dan pemaksaan resolusi 1080p belum tersedia. Output mengikuti resolusi sumber dan kemampuan codec perangkat.