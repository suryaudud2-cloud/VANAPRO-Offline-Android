# VANAPRO Offline Android 1.2.0

Aplikasi Android untuk pemrosesan video lokal pada perangkat, dengan lima ekspor kreatif yang menggunakan narasi baru dari mesin Text-to-Speech Indonesia offline.

## Konfigurasi target
- Xiaomi POCO M5s, RAM 6 GB
- Android API 26+
- Compile/target SDK 35
- JDK 17 dan Gradle 8.9
- Resolusi ekspor: sisi pendek 1080 px; rasio sumber dipertahankan
- MP4 melalui AndroidX Media3 Transformer 1.5.1
- Lima variasi: Cinematic, Engagement, Storytelling, Commentary, Creative Cut

## Fitur
- Memilih video melalui Android Storage Access Framework.
- Membaca metadata teknis sumber.
- Membuat lima file WAV dari lima naskah yang ditulis pengguna.
- Hanya menerima voice TTS bahasa Indonesia yang ditandai tidak membutuhkan koneksi jaringan.
- Menggabungkan masing-masing WAV sebagai trek audio baru pada video sumber melalui Media3 Composition.
- Menghasilkan lima MP4 berurutan ke folder Movies/VANAPRO.
- Menyimpan lima WAV narasi ke Downloads/VANAPRO.
- Pemrosesan satu ekspor pada satu waktu untuk mengurangi tekanan RAM dan panas.

## Privasi dan pemrosesan
- Tidak ada permission INTERNET pada AndroidManifest.
- Tidak memakai server, akun, atau layanan lisensi.
- Video dipilih melalui Storage Access Framework.
- TTS membutuhkan paket suara Indonesia offline yang sudah terpasang pada Android. Jika perangkat tidak memiliki voice offline tersebut, fitur narasi akan berhenti dan meminta pemasangan data suara melalui pengaturan sistem.
- Lima naskah harus ditulis sesuai isi video. Aplikasi tidak mengarang fakta tentang adegan yang tidak dianalisis.
- Audio sumber diganti dengan trek narasi baru pada lima ekspor; audio asli tidak dicampur pada versi ini.

## Build APK
Setiap push ke branch `main` menjalankan GitHub Actions workflow **Build VANAPRO Offline APK**. Buka tab **Actions**, pilih workflow tersebut, lalu unduh artifact **VANAPRO-Offline-debug-APK** setelah status berhasil.

Build lokal:
```bash
gradle --no-daemon assembleDebug
```

APK debug:
`app/build/outputs/apk/debug/app-debug.apk`

## Catatan kualitas
- Output menjaga rasio aspek sumber dan menskalakan sisi pendek ke 1080 px. Sumber di bawah 1080 px akan di-upscale; hasilnya tidak menambah detail asli.
- H.264/MP4 bergantung pada encoder yang tersedia pada perangkat.
- Proses ekspor lima video dapat memerlukan waktu dan ruang penyimpanan besar. Pastikan ruang kosong memadai dan perangkat tidak terlalu panas.
- Variasi ini menambah narasi editorial yang berbeda; tidak menjamin dan tidak dirancang untuk menghindari sistem deteksi konten duplikat platform.
