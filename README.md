# Uang Siapa? 💸

Aplikasi pencatatan keuangan pribadi modern berbasis Android yang dirancang untuk membantu pengguna melacak pendapatan (uang masuk) dan pengeluaran (uang keluar) dengan mudah, aman, dan sepenuhnya offline. Aplikasi ini mendukung visualisasi data, analisis kategori, serta ekspor laporan ke berbagai format populer (PDF, Excel, CSV).

---

## 🌟 Fitur Utama

### 1. **Pencatatan Keuangan Presisi (Terbaru)**
- Tambah, ubah, dan hapus transaksi dengan nominal, kategori, keterangan, dan tanggal yang fleksibel.
- Dukungan format mata uang Rupiah (Rp) yang diformat secara otomatis selama pengetikan.
- **Saran Kategori Cerdas (Dropdown):** Kolom kategori kini dilengkapi dengan tombol dropdown yang menyajikan daftar saran kategori siap pakai secara dinamis (berbeda untuk "Uang Masuk" dan "Uang Keluar") guna mempercepat input data tanpa mengetik ulang.
- **Pembersihan Kategori Otomatis:** Saat beralih tab antara "Uang Masuk" dan "Uang Keluar", kolom input kategori akan otomatis dibersihkan untuk mencegah salah pilih kategori lintas tipe transaksi.
- **Keterangan Default Minimalis:** Pengisian transaksi yang tidak menyertakan keterangan kini akan otomatis diatur ke nilai default `-` (strip) untuk menjaga kerapian visual di layar riwayat transaksi.
- Swipe to gesture (Geser ke Kiri untuk Hapus, Geser ke Kanan untuk Edit).
- Tampilan riwayat transaksi yang rapi di mana tanggal diformat detail hingga jam-menit (`dd MMM yyyy, HH:mm`) dan keterangan diposisikan tepat di bawah tanggal untuk readability optimal.

### 2. **Filter & Pencarian Pintar (Terbaru)**
- **Filter Tipe Transaksi:** Saring transaksi langsung di layar utama berdasarkan "Semua", "Uang Masuk", atau "Uang Keluar".
- **Filter Kategori Dinamis:** Saring transaksi berdasarkan kategori yang pernah dibuat secara otomatis tanpa konfigurasi manual.
- **Tata Letak Konsisten & Responsif:** Dropdown tanggal, filter tipe, dan filter kategori kini disusun secara rapi mengisi seluruh lebar layar dengan proporsi grid yang seimbang dan fungsional.
- **Tombol Reset Tanggal Cepat:** Dilengkapi tombol pintas "Reset" tanggal yang dinamis dan tetap terlihat guna memudahkan pengguna kembali ke tampilan semua data.
- **Ringkasan Filter Aktif Presisi:** Menampilkan rekapitulasi data terfilter ("Masuk", "Keluar", dan "Selisih" di baris baru yang rapi) secara instan ketika filter aktif digunakan.

### 3. **Laporan Bulanan & Tahunan Interaktif**
- **Ringkasan Visual Bulanan:** Grafik batang minimalis yang membandingkan total pemasukan dan pengeluaran bulan berjalan.
- **Ringkasan Visual Tahunan:** Grafik komparatif 12 bulan penuh (Januari sampai Desember) untuk memudahkan analisis tren keuangan jangka panjang.
- **Analisis Persentase Kategori:** Breakdown persentase kontribusi per kategori pengeluaran dan pemasukan dengan indikator progres bar warna-warni yang estetik.

### 4. **Ekspor Laporan Profesional & Rekapitulasi Excel (Terbaru)**
- **PDF Export Berwarna:** Hasilkan dokumen PDF resmi dengan judul dinamis seperti `Laporan Keuangan Bulan [Bulan/Tahun]` atau `Laporan Keuangan Tahun [Tahun]`.
- **Grafik Visual PDF:** Jika mengekspor laporan tahunan, PDF akan secara otomatis menyertakan visualisasi grafik batang 12 bulan penuh di halaman analisis.
- **Batasan Kategori di PDF:** Halaman analisis grafis PDF membatasi tampilan analisis kategori hingga maksimal **15 kategori teratas** (14 kategori dengan nominal terbesar, sisanya secara otomatis digabungkan ke dalam kategori "Lainnya") guna menjaga kerapian dan keterbacaan tata letak grafis agar tidak saling tumpang tindih.
- **Excel (.xlsx) & CSV:** Dukungan penuh ekspor tabel transaksi terperinci dengan style zebra-striping dan format mata uang ke file Excel serta CSV.
- **Sheet Rekapitulasi Bulanan di Excel (Terbaru):** Ekspor Excel Bulanan kini dilengkapi lembar rekap harian dinamis (misal: `"Rekap Juli 2026"`) yang menampilkan sebaran transaksi per kategori per hari (1 sampai maks-hari) untuk uang masuk dan keluar, beserta perhitungan total harian dan total baris Sisa Saldo (Surplus).
- **Sheet Rekapitulasi Tahunan di Excel (Terbaru):** Lembar rekap tahunan kini dinamai secara jelas dan presisi seperti `"Rekap Tahun 2026"` (menggantikan nama sebelumnya) yang meringkas arus kas per kategori per bulan dalam format matriks tahunan.

### 5. **Sample Buku & Data Simulasi Instan (Terbaru)**
- **Buat Sample Buku Sekali Klik:** Memungkinkan pengguna membuat buku keuangan contoh bernama `"Sample Buku"` yang langsung diisi dengan 21 transaksi simulasi realistis dari Januari hingga Juli 2026.
- **Nominal Realistis Sesuai UMP:** Transaksi gaji pada Sample Buku disesuaikan secara realistis sebesar Rp3.000.000,- per bulan mengikuti rata-rata Upah Minimum Provinsi (UMP).
- **Ajakan Interaktif:** Jika belum ada transaksi sama sekali, layar utama menampilkan ajakan yang interaktif beserta tombol pintas untuk langsung membuat Sample Buku agar pengguna bisa langsung mengeksplorasi visualisasi laporan dan analisis diagram secara instan.

### 6. **Backup & Restore Data Pintar (Multi-Buku)**
- **Ekspor JSON Multi-Buku:** Amankan data keuangan Anda secara lokal dengan mengekspor seluruh transaksi lengkap dengan informasi nama buku asal masing-masing transaksi ke dalam satu file cadangan berformat JSON.
- **Impor Cerdas Tanpa Konflik:** Ketika memulihkan cadangan, sistem secara otomatis melacak dan mengelompokkan transaksi ke buku yang sesuai berdasarkan namanya. Jika buku asal transaksi tersebut tidak ditemukan (misalnya telah dihapus), sistem akan otomatis membuat kembali buku tersebut secara dinamis tanpa merusak data atau menimpa transaksi pada buku lain yang aktif. Hal ini menjamin integritas pembukuan terpisah Anda tetap terjaga dengan sempurna.

### 7. **Tema Gelap & Terang Dinamis**
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
