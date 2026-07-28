# Uang Siapa? 💸 (Versi 1.1)

<img src="./icon.svg" width="128" height="128" />

Aplikasi pencatatan keuangan pribadi modern berbasis Android yang dirancang untuk membantu pengguna melacak pendapatan (uang masuk) dan pengeluaran (uang keluar) dengan mudah, aman, dan sepenuhnya offline. Aplikasi ini mendukung visualisasi data, analisis kategori, pencarian data interaktif, serta ekspor laporan ke berbagai format populer (PDF, Excel, CSV).

---

## 🌟 Fitur Utama (Pembaruan Versi 1.1)

### 1. **Pencarian Data & Keterangan Interaktif (Baru di v1.1)**
- **Kotak Pencarian Serbaguna & Kompak:** Mencakup pencarian seluruh teks transaksi (keterangan/deskripsi, nama kategori, hingga angka nominal rupiah) dengan desain ringkas dan nyaman dipandang.
- **Tombol Hapus Teks Cepat:** Dilengkapi tombol ikon 'X' untuk menghapus kata kunci pencarian secara instan.
- **Pembersihan Fokus Otomatis (Auto Keyboard Dismiss):** Kursor pencarian dan keyboard otomatis tertutup/nonaktif secara cerdas ketika pengguna melakukan ketukan (tap) di area luar kotak pencarian atau saat menggeser (scroll) daftar transaksi.
- **Kombinasi Filter & Reset Total:** Dapat dikombinasikan dengan filter tipe transaksi (Masuk/Keluar), filter kategori, dan filter rentang tanggal. Tombol **Reset** secara otomatis menghapus seluruh filter aktif dan pencarian kata kunci sekaligus dalam satu klik.

### 2. **Mode View Detail & Pencegahan Edit Tak Sengaja (Baru di v1.1)**
- **Tampilan Detail Transaksi (Read-Only Mode):** Mengetuk/memilih baris transaksi kini membuka dialog rincian transaksi lengkap (nominal, kategori, waktu/tanggal, dan keterangan) untuk mencegah pengeditan yang tidak disengaja.
- **Aksi Terbimbing & Tata Letak Ergonomis:** Dalam dialog rincian detail dan informasi aplikasi, tombol tutup ditempatkan sebagai ikon **'X'** di pojok kanan atas pop-up. Pada bagian bawah dialog rincian detail, tombol **Hapus** (merah) ditempatkan di sisi kiri dan tombol **Edit** (biru) di sisi kanan secara simetris untuk pemisahan aksi yang jelas.
- **Desain Dialog & Pop-up Modern:** Seluruh dialog/pop-up konfirmasi (Hapus Transaksi, Kelola Buku Keuangan, Pemilihan Bulan & Tahun Laporan, Rentang Tanggal, dan Informasi Aplikasi) telah diperbarui dengan ikon visual melingkar di bagian atas, tombol tutup **'X'** di pojok kanan atas, serta tata letak tombol M3 yang seragam dan konsisten.
- **Kategori "Saldo Awal":** Ditambahkan pada daftar saran pilihan Uang Masuk untuk memudahkan pencatatan modal/saldo awal buku keuangan.
- **Kategori "Pindah Kas":** Ditambahkan pada pilihan Uang Masuk dan Uang Keluar untuk memfasilitasi transfer/pemindahan dana antar rekening atau kas secara terstruktur.
- **Saran Kategori Cerdas (Dropdown):** Kolom kategori dilengkapi dengan tombol dropdown saran kategori siap pakai.
- **Pembersihan Kategori Otomatis:** Saat beralih tab antara "Uang Masuk" dan "Uang Keluar", kolom input kategori otomatis dibersihkan.
- **Keterangan Default Minimalis:** Pengisian transaksi tanpa keterangan otomatis diatur ke nilai default `-` (strip).
- Swipe gesture (Geser ke Kiri untuk Hapus, Geser ke Kanan untuk Edit).

### 3. **Filter & Pencarian Pintar**
- **Filter Tipe Transaksi:** Saring transaksi berdasarkan "Semua", "Uang Masuk", atau "Uang Keluar".
- **Filter Kategori Dinamis:** Saring transaksi berdasarkan kategori yang pernah dibuat secara otomatis.
- **Tata Letak Konsisten & Responsif:** Dropdown tanggal, filter tipe, dan filter kategori disusun dalam proporsi grid yang seimbang.
- **Tombol Reset Tanggal & Pencarian:** Tombol pintas untuk mengembalikan tampilan ke seluruh data transaksi.
- **Ringkasan Filter Aktif Presisi:** Menampilkan rekapitulasi data terfilter ("Masuk", "Keluar", dan "Selisih") secara instan ketika filter atau pencarian aktif.

### 4. **Optimasi Rotasi Lanskap & Display Cutout (Baru di v1.1)**
- **Penanganan Orientasi Lanskap 180°:** Dukungan mode layout cutout `SHORT_EDGES` / `ALWAYS` dan penanganan `onConfigurationChanged` dinamis untuk memastikan tampilan aplikasi penuh tanpa terpotong atau menyisakan blok hitam pada rotasi landscape 90° maupun -90° di perangkat Android lama dan baru.
- **Display Insets & Safe Drawing:** Penambahan `safeDrawing` insets pada `Scaffold` dan FAB untuk kenyamanan navigasi.

### 5. **Laporan Bulanan & Tahunan Interaktif**
- **Ringkasan Visual Bulanan:** Grafik batang minimalis membandingkan total pemasukan dan pengeluaran bulan berjalan.
- **Ringkasan Visual Tahunan:** Grafik komparatif 12 bulan penuh (Januari-Desember) untuk analisis tren keuangan jangka panjang.
- **Analisis Persentase Kategori:** Breakdown persentase kontribusi per kategori dengan indikator progres bar warna-warni.

### 6. **Ekspor Laporan Profesional & Rekapitulasi Excel**
- **PDF Export Berwarna:** Hasilkan dokumen PDF resmi dengan judul dinamis `Laporan Keuangan Bulan [Bulan/Tahun]` atau `Laporan Keuangan Tahun [Tahun]`.
- **Grafik Visual PDF:** Ekspor laporan tahunan otomatis menyertakan visualisasi grafik batang 12 bulan penuh.
- **Batasan Kategori di PDF:** Membatasi analisis kategori hingga maksimal 15 kategori teratas (14 kategori terbesar + "Lainnya").
- **Excel (.xlsx) & CSV:** Dukungan penuh ekspor tabel transaksi terperinci dengan style zebra-striping dan format mata uang.
- **Sheet Rekapitulasi Bulanan & Tahunan Excel:** Lembar rekap harian per kategori (`"Rekap Juli 2026"`) dan rekap matriks tahunan (`"Rekap Tahun 2026"`).

### 7. **Backup & Restore Data Pintar (Multi-Buku)**
- **Ekspor JSON Multi-Buku:** Amankan data keuangan dengan mengekspor seluruh transaksi lengkap dengan nama buku asal.
- **Impor Cerdas Tanpa Konflik:** Melacak dan mengelompokkan transaksi ke buku asal secara otomatis saat restore.
- **Pencegahan Nama Buku Duplikat (Case-Insensitive):** Otomatis menambahkan sufiks angka ` (1)`, ` (2)` jika ditemukan konflik nama buku.

### 7. **Tema Gelap & Terang Dinamis & Optimasi Lanskap (Terbaru)**
- **Gradient Total Saldo Premium:** Kotak ringkasan total saldo utama kini dibalut dengan gradasi warna linear (gradient) 3-stop yang modern, sangat estetik, dan nyaman di mata:
  - **Light Mode:** Gradasi pastel premium sejuk yang mengalir lembut dari warna biru-indigo chic, lavender lembut, hingga sentuhan rose-pink hangat.
  - **Dark Mode:** Gradasi kosmik nebula mewah yang memadukan warna violet berpendar, indigo pekat, hingga transisi abu-abu gelap metalik yang menyatu dengan latar belakang gelap.
- **Optimasi Lanskap & Rotasi Layar:** 
  - Penambahan `safeDrawing` insets pada seluruh layar (`Scaffold`) dan `displayCutoutPadding` pada Floating Action Button (FAB) untuk mencegah pemotongan tombol/konten saat layar dirotasi ke mode landscape pada perangkat yang memiliki takik (notch), cutout, atau black bar/sisi navigasi sistem Android model lama.
  - Peningkatan pada dialog popup **"Tentang Aplikasi"** yang kini mendukung fitur scroll penuh (`verticalScroll`) sehingga seluruh informasi, tautan GitHub, dan donasi Saweria tetap dapat dibaca secara utuh di mode lanskap tanpa terpotong.
- Antarmuka modern Material Design 3 yang ramah mata dengan dukungan Dark Mode dan Light Mode yang dapat diganti secara instan di layar utama.

---

## 🛠️ Detail Arsitektur & Teknologi

Aplikasi ini dibangun menggunakan praktik pengembangan Android modern:

- **Bahasa Pemrograman:** Kotlin 100%
- **UI Framework:** Jetpack Compose (Material Design 3)
- **Arsitektur:** MVVM (Model-View-ViewModel) dengan Unidirectional Data Flow (UDF)
- **Penyimpanan Lokal:** Room Database (SQLite abstraction) untuk kegunaan 100% offline tanpa perlu koneksi internet.
- **Asynchronous Operations:** Kotlin Coroutines & Flow (dengan `collectAsStateWithLifecycle` untuk efisiensi lifecycle-aware)
- **Pembuatan PDF:** Android Native `PdfDocument` & `Canvas` API
- **Pembuatan Excel:** Library super cepat `FastExcel` dari Dhatim

---

## 📁 Struktur Kode

```text
/app/src/main/java/com/example/
├── MoneyTrackerApp.kt         # Entrypoint aplikasi & konfigurasi tema global
├── MainActivity.kt            # Activity tunggal dengan deklarasi navigasi Compose
├── data/
│   ├── Transaction.kt         # Entity/Data Class Room Database
│   ├── TransactionDao.kt      # Query SQL Database via interface DAO
│   ├── TransactionRepository.kt # Repository layer sebagai Single Source of Truth
│   └── AppDatabase.kt         # Inisialisasi Database Room
├── ui/
│   ├── screens/
│   │   ├── HomeScreen.kt      # Tampilan utama dengan daftar transaksi, balance card, & filter
│   │   ├── AddEditScreen.kt   # Tampilan form input & edit transaksi
│   │   └── ReportScreen.kt    # Tampilan statistik visual bulanan & tahunan
│   ├── theme/                 # Palet warna Material 3, tipografi, & setup tema
│   └── viewmodel/             # State holders (MainViewModel, AddEditViewModel, ReportViewModel)
└── utils/
    ├── BackupUtils.kt         # Utilitas import/export file backup JSON secara aman
    └── ExportUtils.kt         # Logika ekspor profesional ke file PDF, XLSX, & CSV
```

---

## 🚀 Cara Menjalankan & Build Aplikasi

Aplikasi ini menggunakan sistem build Gradle Kotlin DSL.

1. **Prasyarat:**
   - Gunakan JDK 17 atau yang lebih baru.
   - Gunakan Android Studio Jellyfish / Koala atau versi terbaru.

2. **Kompilasi dan Jalankan:**
   Buka direktori proyek ini di Android Studio, tunggu sinkronisasi Gradle selesai, lalu tekan tombol **Run** untuk menginstalnya ke perangkat emulator atau perangkat fisik Anda.

3. **Gunakan Laporan Ekspor:**
   - Ketika menekan ikon Ekspor PDF/Excel/CSV di halaman Laporan, aplikasi akan menghasilkan file secara instan dan membuka chooser sistem untuk membagikannya via email, WhatsApp, cloud drive, dll.
