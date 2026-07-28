package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.Transaction
import com.example.data.TransactionType
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar
import org.dhatim.fastexcel.Workbook
import org.dhatim.fastexcel.Worksheet
import com.example.ui.viewmodel.ReportMode

object ExportUtils {
    
    private val monthNamesIndo = listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )

    private fun getExportFileName(
        bookName: String,
        reportMode: ReportMode,
        year: Int,
        month: Int,
        extension: String
    ): String {
        val sanitizedBook = bookName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
        return if (reportMode == ReportMode.MONTHLY) {
            val monthName = monthNamesIndo.getOrElse(month.coerceIn(0, 11)) { "Bulan" }
            "laporan_bulanan_${monthName}_${year}_${sanitizedBook}_${timestamp}.${extension}"
        } else {
            "laporan_tahunan_${year}_${sanitizedBook}_${timestamp}.${extension}"
        }
    }
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH.mm", Locale.getDefault())

    private fun formatCompactRp(amount: Double): String {
        val formatter = NumberFormat.getNumberInstance(Locale("id", "ID"))
        return if (amount < 0) {
            "-Rp " + formatter.format(-amount)
        } else {
            "Rp " + formatter.format(amount)
        }
    }

    fun exportToXlsx(
        context: Context, 
        transactions: List<Transaction>, 
        bookName: String,
        reportMode: ReportMode = ReportMode.MONTHLY,
        year: Int = Calendar.getInstance().get(Calendar.YEAR),
        month: Int = Calendar.getInstance().get(Calendar.MONTH)
    ) {
        try {
            val fileName = getExportFileName(bookName, reportMode, year, month, "xlsx")
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { fos ->
                val workbook = Workbook(fos, "UangSiapa", "1.0")
                val sheetName = if (bookName.length > 30) bookName.take(30) else bookName
                val worksheet = workbook.newWorksheet(sheetName)
                
                // Set Column Widths
                worksheet.width(0, 15.0) // Tanggal
                worksheet.width(1, 20.0) // Kategori
                worksheet.width(2, 30.0) // Deskripsi
                worksheet.width(3, 18.0) // Debit
                worksheet.width(4, 18.0) // Kredit
                worksheet.width(5, 18.0) // Saldo
                
                // Headers (Row 0)
                worksheet.value(0, 0, "Tanggal")
                worksheet.value(0, 1, "Kategori")
                worksheet.value(0, 2, "Deskripsi")
                worksheet.value(0, 3, "Debit (Pemasukan)")
                worksheet.value(0, 4, "Kredit (Pengeluaran)")
                worksheet.value(0, 5, "Saldo")
                
                // Style Headers to be Bold, with solid background, white text, and borders
                for (col in 0..5) {
                    worksheet.style(0, col)
                        .bold()
                        .fillColor("4F46E5")
                        .fontColor("FFFFFF")
                        .borderStyle("thin")
                        .borderColor("D1D5DB")
                        .set()
                }
                
                var row = 1
                var currentBalance = 0.0
                val sortedTransactions = transactions.sortedBy { it.dateMillis }
                
                for (t in sortedTransactions) {
                    val dateStr = dateTimeFormat.format(Date(t.dateMillis))
                    
                    worksheet.value(row, 0, dateStr)
                    worksheet.value(row, 1, t.category)
                    worksheet.value(row, 2, t.description)
                    
                    val isIncome = t.type == TransactionType.INCOME
                    if (isIncome) {
                        currentBalance += t.amount
                        worksheet.value(row, 3, t.amount)
                    } else {
                        currentBalance -= t.amount
                        worksheet.value(row, 4, t.amount)
                    }
                    worksheet.value(row, 5, currentBalance)
                    
                    // Zebra striping background color
                    val isOddRow = row % 2 != 0
                    val bgColor = if (isOddRow) "F8FAFC" else "FFFFFF"
                    
                    for (col in 0..5) {
                        val style = worksheet.style(row, col)
                        style.borderStyle("thin")
                        style.borderColor("E5E7EB")
                        if (isOddRow) {
                            style.fillColor(bgColor)
                        }
                        
                        // Text formatting and colors specific to columns
                        when (col) {
                            3 -> {
                                if (isIncome) {
                                    style.fontColor("16A34A").bold()
                                }
                                style.format("#,##0")
                            }
                            4 -> {
                                if (!isIncome) {
                                    style.fontColor("DC2626").bold()
                                }
                                style.format("#,##0")
                            }
                            5 -> {
                                style.fontColor("1F2937").bold()
                                style.format("#,##0")
                            }
                        }
                        
                        style.set()
                    }
                    
                    row++
                }
                
                worksheet.finish()

                // Add Recap sheet if monthly mode
                if (reportMode == ReportMode.MONTHLY) {
                    val monthNamesIndo = listOf(
                        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
                        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
                    )
                    val monthName = monthNamesIndo.getOrNull(month) ?: "Bulan"
                    val summarySheetName = "Rekap $monthName $year"
                    val sSheet = workbook.newWorksheet(summarySheetName)
                    
                    val calendar = Calendar.getInstance()
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                    
                    // Column widths
                    sSheet.width(0, 24.0) // Kategori
                    for (col in 1..maxDays) {
                        sSheet.width(col, 8.0) // Hari 1 - maxDays
                    }
                    sSheet.width(maxDays + 1, 18.0) // Total
                    
                    // Title rows
                    sSheet.value(0, 0, "REKAPITULASI HARIAN BULAN ${monthName.uppercase()} TAHUN $year")
                    sSheet.style(0, 0).bold().set()
                    sSheet.value(1, 0, "Buku Keuangan: $bookName")
                    sSheet.style(1, 0).bold().set()
                    
                    // Table Headers
                    sSheet.value(3, 0, "Kategori")
                    sSheet.style(3, 0)
                        .bold()
                        .fillColor("4F46E5")
                        .fontColor("FFFFFF")
                        .borderStyle("thin")
                        .borderColor("D1D5DB")
                        .set()
                        
                    for (d in 1..maxDays) {
                        sSheet.value(3, d, d.toString())
                        sSheet.style(3, d)
                            .bold()
                            .fillColor("4F46E5")
                            .fontColor("FFFFFF")
                            .borderStyle("thin")
                            .borderColor("D1D5DB")
                            .set()
                    }
                    
                    sSheet.value(3, maxDays + 1, "Total")
                    sSheet.style(3, maxDays + 1)
                        .bold()
                        .fillColor("4F46E5")
                        .fontColor("FFFFFF")
                        .borderStyle("thin")
                        .borderColor("D1D5DB")
                        .set()
                        
                    val cal = Calendar.getInstance()
                    
                    // Filter transactions that fall within this month and year
                    val filteredTransactions = transactions.filter { t ->
                        cal.timeInMillis = t.dateMillis
                        cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month
                    }
                    
                    // Categories
                    val incomeCategories = filteredTransactions.filter { it.type == TransactionType.INCOME }
                        .map { it.category.trim() }
                        .distinct()
                        .sorted()
                        
                    val expenseCategories = filteredTransactions.filter { it.type == TransactionType.EXPENSE }
                        .map { it.category.trim() }
                        .distinct()
                        .sorted()
                    
                    // Map Triple(Type, Category, Day of Month) -> Amount
                    val dailySums = mutableMapOf<Triple<TransactionType, String, Int>, Double>()
                    for (t in filteredTransactions) {
                        cal.timeInMillis = t.dateMillis
                        val d = cal.get(Calendar.DAY_OF_MONTH)
                        val type = t.type
                        val cat = t.category.trim()
                        val key = Triple(type, cat, d)
                        dailySums[key] = (dailySums[key] ?: 0.0) + t.amount
                    }
                    
                    var sRow = 4
                    
                    // PEMASUKAN Section
                    sSheet.value(sRow, 0, "UANG MASUK")
                    for (col in 0..(maxDays + 1)) {
                        sSheet.style(sRow, col)
                            .bold()
                            .fillColor("DCFCE7")
                            .fontColor("15803D")
                            .borderStyle("thin")
                            .borderColor("D1D5DB")
                            .set()
                    }
                    sRow++
                    
                    for (cat in incomeCategories) {
                        sSheet.value(sRow, 0, cat)
                        var rowSum = 0.0
                        for (d in 1..maxDays) {
                            val amt = dailySums[Triple(TransactionType.INCOME, cat, d)] ?: 0.0
                            if (amt != 0.0) {
                                sSheet.value(sRow, d, amt)
                            }
                            rowSum += amt
                        }
                        sSheet.value(sRow, maxDays + 1, rowSum)
                        
                        for (col in 0..(maxDays + 1)) {
                            val style = sSheet.style(sRow, col)
                                .borderStyle("thin")
                                .borderColor("E5E7EB")
                            if (col > 0) {
                                style.format("#,##0")
                            }
                            style.set()
                        }
                        sRow++
                    }
                    
                    // Total Pemasukan Row
                    sSheet.value(sRow, 0, "Total Uang Masuk")
                    val totalIncomeDaily = DoubleArray(maxDays + 1)
                    var grandTotalIncome = 0.0
                    for (d in 1..maxDays) {
                        var dSum = 0.0
                        for (cat in incomeCategories) {
                            dSum += dailySums[Triple(TransactionType.INCOME, cat, d)] ?: 0.0
                        }
                        sSheet.value(sRow, d, dSum)
                        totalIncomeDaily[d] = dSum
                        grandTotalIncome += dSum
                    }
                    sSheet.value(sRow, maxDays + 1, grandTotalIncome)
                    for (col in 0..(maxDays + 1)) {
                        val style = sSheet.style(sRow, col)
                            .bold()
                            .fillColor("F0FDF4")
                            .fontColor("16A34A")
                            .borderStyle("thin")
                            .borderColor("D1D5DB")
                        if (col > 0) {
                            style.format("#,##0")
                        }
                        style.set()
                    }
                    sRow++
                    
                    sRow++ // Spacer Row
                    
                    // PENGELUARAN Section
                    sSheet.value(sRow, 0, "UANG KELUAR")
                    for (col in 0..(maxDays + 1)) {
                        sSheet.style(sRow, col)
                            .bold()
                            .fillColor("FEE2E2")
                            .fontColor("B91C1C")
                            .borderStyle("thin")
                            .borderColor("D1D5DB")
                            .set()
                    }
                    sRow++
                    
                    for (cat in expenseCategories) {
                        sSheet.value(sRow, 0, cat)
                        var rowSum = 0.0
                        for (d in 1..maxDays) {
                            val amt = dailySums[Triple(TransactionType.EXPENSE, cat, d)] ?: 0.0
                            if (amt != 0.0) {
                                sSheet.value(sRow, d, amt)
                            }
                            rowSum += amt
                        }
                        sSheet.value(sRow, maxDays + 1, rowSum)
                        
                        for (col in 0..(maxDays + 1)) {
                            val style = sSheet.style(sRow, col)
                                .borderStyle("thin")
                                .borderColor("E5E7EB")
                            if (col > 0) {
                                style.format("#,##0")
                            }
                            style.set()
                        }
                        sRow++
                    }
                    
                    // Total Pengeluaran Row
                    sSheet.value(sRow, 0, "Total Uang Keluar")
                    val totalExpenseDaily = DoubleArray(maxDays + 1)
                    var grandTotalExpense = 0.0
                    for (d in 1..maxDays) {
                        var dSum = 0.0
                        for (cat in expenseCategories) {
                            dSum += dailySums[Triple(TransactionType.EXPENSE, cat, d)] ?: 0.0
                        }
                        sSheet.value(sRow, d, dSum)
                        totalExpenseDaily[d] = dSum
                        grandTotalExpense += dSum
                    }
                    sSheet.value(sRow, maxDays + 1, grandTotalExpense)
                    for (col in 0..(maxDays + 1)) {
                        val style = sSheet.style(sRow, col)
                            .bold()
                            .fillColor("FEF2F2")
                            .fontColor("DC2626")
                            .borderStyle("thin")
                            .borderColor("D1D5DB")
                        if (col > 0) {
                            style.format("#,##0")
                        }
                        style.set()
                    }
                    sRow++
                    
                    sRow++ // Spacer Row
                    
                    // Sisa Saldo / Surplus Row
                    sSheet.value(sRow, 0, "Sisa Saldo (Surplus)")
                    for (d in 1..maxDays) {
                        val dSurplus = totalIncomeDaily[d] - totalExpenseDaily[d]
                        sSheet.value(sRow, d, dSurplus)
                    }
                    val grandSurplus = grandTotalIncome - grandTotalExpense
                    sSheet.value(sRow, maxDays + 1, grandSurplus)
                    for (col in 0..(maxDays + 1)) {
                        val style = sSheet.style(sRow, col)
                            .bold()
                            .fillColor("EFF6FF")
                            .fontColor("1D4ED8")
                            .borderStyle("thin")
                            .borderColor("D1D5DB")
                        if (col > 0) {
                            style.format("#,##0")
                        }
                        style.set()
                    }
                    
                    sSheet.finish()
                }

                // Add Recap sheet if annual/yearly mode
                if (reportMode == ReportMode.YEARLY) {
                    val summarySheetName = "Rekap Tahun $year"
                    val sSheet = workbook.newWorksheet(summarySheetName)
                    
                    // Column widths
                    sSheet.width(0, 24.0) // Kategori
                    for (col in 1..12) {
                        sSheet.width(col, 15.0) // Bulan Jan - Des
                    }
                    sSheet.width(13, 18.0) // Total
                    
                    // Title rows
                    sSheet.value(0, 0, "REKAPITULASI BULANAN TAHUN $year")
                    sSheet.style(0, 0).bold().set()
                    sSheet.value(1, 0, "Buku Keuangan: $bookName")
                    sSheet.style(1, 0).bold().set()
                    
                    // Table Headers
                    val headers = listOf(
                        "Kategori", "Januari", "Februari", "Maret", "April", "Mei", "Juni",
                        "Juli", "Agustus", "September", "Oktober", "November", "Desember", "Total"
                    )
                    for (col in 0..13) {
                        sSheet.value(3, col, headers[col])
                        sSheet.style(3, col)
                            .bold()
                            .fillColor("4F46E5")
                            .fontColor("FFFFFF")
                            .borderStyle("thin")
                            .borderColor("D1D5DB")
                            .set()
                    }
                    
                    val cal = Calendar.getInstance()
                    
                    // Categories
                    val incomeCategories = transactions.filter { it.type == TransactionType.INCOME }
                        .map { it.category.trim() }
                        .distinct()
                        .sorted()
                        
                    val expenseCategories = transactions.filter { it.type == TransactionType.EXPENSE }
                        .map { it.category.trim() }
                        .distinct()
                        .sorted()
                    
                    // Map Triple(Type, Category, Month) -> Amount
                    val monthlySums = mutableMapOf<Triple<TransactionType, String, Int>, Double>()
                    for (t in transactions) {
                        cal.timeInMillis = t.dateMillis
                        val m = cal.get(Calendar.MONTH)
                        val type = t.type
                        val cat = t.category.trim()
                        val key = Triple(type, cat, m)
                        monthlySums[key] = (monthlySums[key] ?: 0.0) + t.amount
                    }
                    
                    var sRow = 4
                    
                    // PEMASUKAN Section
                    sSheet.value(sRow, 0, "UANG MASUK")
                    for (col in 0..13) {
                        sSheet.style(sRow, col)
                            .bold()
                            .fillColor("DCFCE7")
                            .fontColor("15803D")
                            .borderStyle("thin")
                            .borderColor("D1D5DB")
                            .set()
                    }
                    sRow++
                    
                    for (cat in incomeCategories) {
                        sSheet.value(sRow, 0, cat)
                        var rowSum = 0.0
                        for (m in 0..11) {
                            val amt = monthlySums[Triple(TransactionType.INCOME, cat, m)] ?: 0.0
                            sSheet.value(sRow, m + 1, amt)
                            rowSum += amt
                        }
                        sSheet.value(sRow, 13, rowSum)
                        
                        for (col in 0..13) {
                            val style = sSheet.style(sRow, col)
                                .borderStyle("thin")
                                .borderColor("E5E7EB")
                            if (col > 0) {
                                style.format("#,##0")
                            }
                            style.set()
                        }
                        sRow++
                    }
                    
                    // Total Pemasukan Row
                    sSheet.value(sRow, 0, "Total Uang Masuk")
                    val totalIncomeMonthly = DoubleArray(12)
                    var grandTotalIncome = 0.0
                    for (m in 0..11) {
                        var mSum = 0.0
                        for (cat in incomeCategories) {
                            mSum += monthlySums[Triple(TransactionType.INCOME, cat, m)] ?: 0.0
                        }
                        sSheet.value(sRow, m + 1, mSum)
                        totalIncomeMonthly[m] = mSum
                        grandTotalIncome += mSum
                    }
                    sSheet.value(sRow, 13, grandTotalIncome)
                    for (col in 0..13) {
                        val style = sSheet.style(sRow, col)
                            .bold()
                            .fillColor("F0FDF4")
                            .fontColor("16A34A")
                            .borderStyle("thin")
                            .borderColor("D1D5DB")
                        if (col > 0) {
                            style.format("#,##0")
                        }
                        style.set()
                    }
                    sRow++
                    
                    sRow++ // Spacer Row
                    
                    // PENGELUARAN Section
                    sSheet.value(sRow, 0, "UANG KELUAR")
                    for (col in 0..13) {
                        sSheet.style(sRow, col)
                            .bold()
                            .fillColor("FEE2E2")
                            .fontColor("B91C1C")
                            .borderStyle("thin")
                            .borderColor("D1D5DB")
                            .set()
                    }
                    sRow++
                    
                    for (cat in expenseCategories) {
                        sSheet.value(sRow, 0, cat)
                        var rowSum = 0.0
                        for (m in 0..11) {
                            val amt = monthlySums[Triple(TransactionType.EXPENSE, cat, m)] ?: 0.0
                            sSheet.value(sRow, m + 1, amt)
                            rowSum += amt
                        }
                        sSheet.value(sRow, 13, rowSum)
                        
                        for (col in 0..13) {
                            val style = sSheet.style(sRow, col)
                                .borderStyle("thin")
                                .borderColor("E5E7EB")
                            if (col > 0) {
                                style.format("#,##0")
                            }
                            style.set()
                        }
                        sRow++
                    }
                    
                    // Total Pengeluaran Row
                    sSheet.value(sRow, 0, "Total Uang Keluar")
                    val totalExpenseMonthly = DoubleArray(12)
                    var grandTotalExpense = 0.0
                    for (m in 0..11) {
                        var mSum = 0.0
                        for (cat in expenseCategories) {
                            mSum += monthlySums[Triple(TransactionType.EXPENSE, cat, m)] ?: 0.0
                        }
                        sSheet.value(sRow, m + 1, mSum)
                        totalExpenseMonthly[m] = mSum
                        grandTotalExpense += mSum
                    }
                    sSheet.value(sRow, 13, grandTotalExpense)
                    for (col in 0..13) {
                        val style = sSheet.style(sRow, col)
                            .bold()
                            .fillColor("FEF2F2")
                            .fontColor("DC2626")
                            .borderStyle("thin")
                            .borderColor("D1D5DB")
                        if (col > 0) {
                            style.format("#,##0")
                        }
                        style.set()
                    }
                    sRow++
                    
                    sRow++ // Spacer Row
                    
                    // Sisa Saldo / Surplus Row
                    sSheet.value(sRow, 0, "Sisa Saldo (Surplus)")
                    for (m in 0..11) {
                        val mSurplus = totalIncomeMonthly[m] - totalExpenseMonthly[m]
                        sSheet.value(sRow, m + 1, mSurplus)
                    }
                    val grandSurplus = grandTotalIncome - grandTotalExpense
                    sSheet.value(sRow, 13, grandSurplus)
                    for (col in 0..13) {
                        val style = sSheet.style(sRow, col)
                            .bold()
                            .fillColor("EFF6FF")
                            .fontColor("1D4ED8")
                            .borderStyle("thin")
                            .borderColor("D1D5DB")
                        if (col > 0) {
                            style.format("#,##0")
                        }
                        style.set()
                    }
                    
                    sSheet.finish()
                }
                
                workbook.finish()
            }
            
            shareFile(context, file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun exportToCsv(
        context: Context, 
        transactions: List<Transaction>, 
        bookName: String,
        reportMode: ReportMode = ReportMode.MONTHLY,
        year: Int = Calendar.getInstance().get(Calendar.YEAR),
        month: Int = Calendar.getInstance().get(Calendar.MONTH)
    ) {
        try {
            val fileName = getExportFileName(bookName, reportMode, year, month, "csv")
            val file = File(context.cacheDir, fileName)
            val fileOutputStream = FileOutputStream(file)
            val writer = fileOutputStream.writer()
            
            writer.write("Buku Keuangan: $bookName\n")
            writer.write("Tanggal,Kategori,Deskripsi,Debit (Pemasukan),Kredit (Pengeluaran),Saldo\n")
            var currentBalance = 0.0
            val sortedTransactions = transactions.sortedBy { it.dateMillis }
            for (t in sortedTransactions) {
                val dateStr = dateTimeFormat.format(Date(t.dateMillis))
                val debitStr = if (t.type == TransactionType.INCOME) {
                    currentBalance += t.amount
                    t.amount.toString()
                } else ""
                val creditStr = if (t.type != TransactionType.INCOME) {
                    currentBalance -= t.amount
                    t.amount.toString()
                } else ""
                writer.write("$dateStr,${t.category.replace(","," ")},${t.description.replace(","," ")},$debitStr,$creditStr,$currentBalance\n")
            }
            writer.close()
            
            shareFile(context, file, "text/csv")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun exportToPdf(
        context: Context,
        transactions: List<Transaction>,
        bookName: String,
        reportMode: ReportMode = ReportMode.MONTHLY,
        currentMonth: Int = 0,
        currentYear: Int = 2026
    ) {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
            var page = pdfDocument.startPage(pageInfo)
            var canvas: Canvas = page.canvas
            val paint = Paint()
            paint.isAntiAlias = true
            
            // Calculate totals
            var totalIncome = 0.0
            var totalExpense = 0.0
            for (t in transactions) {
                if (t.type == TransactionType.INCOME) {
                    totalIncome += t.amount
                } else {
                    totalExpense += t.amount
                }
            }
            val balance = totalIncome - totalExpense
            val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }

            // 1. Header Banner
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#6750A4") // Purple primary
            canvas.drawRoundRect(40f, 30f, 555f, 110f, 12f, 12f, paint)

            // Dynamic Period Title
            val monthNames = listOf(
                "JANUARI", "FEBRUARI", "MARET", "APRIL", "MEI", "JUNI",
                "JULI", "AGUSTUS", "SEPTEMBER", "OKTOBER", "NOVEMBER", "DESEMBER"
            )
            val periodStr = if (reportMode == ReportMode.MONTHLY) {
                "BULAN ${monthNames[currentMonth]} $currentYear"
            } else {
                "TAHUN $currentYear"
            }
            val titleText = "LAPORAN KEUANGAN $periodStr"

            // Header text
            paint.color = Color.WHITE
            paint.isFakeBoldText = true
            paint.textSize = 14f // Adjusted to fit nicely on one line
            canvas.drawText(titleText, 60f, 65f, paint)

            paint.isFakeBoldText = false
            paint.textSize = 10f
            paint.color = Color.parseColor("#EADDFF")
            
            val appSuffix = "  |  Dicetak: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID")).format(Date())}"
            val remainingSpace = 480f - paint.measureText("Aplikasi Uang Siapa?" + appSuffix)
            val truncatedBookName = if (remainingSpace > 30f) truncateText(bookName, remainingSpace, paint) else bookName.take(8)
            val appNameAndBook = "Uang Siapa? - Buku: $truncatedBookName"
            
            var headerX = 60f
            paint.isFakeBoldText = false
            canvas.drawText("Aplikasi ", headerX, 90f, paint)
            headerX += paint.measureText("Aplikasi ")
            
            paint.isFakeBoldText = true
            canvas.drawText(appNameAndBook, headerX, 90f, paint)
            headerX += paint.measureText(appNameAndBook)
            
            paint.isFakeBoldText = false
            canvas.drawText(appSuffix, headerX, 90f, paint)

            // 2. Summary Statistics Cards
            val cardWidth = 160f
            val cardHeight = 60f
            val cardY = 130f
            val cardRadius = 8f

            // Card 1: Pemasukan (Income)
            paint.color = Color.parseColor("#DCFCE7") // light green
            canvas.drawRoundRect(40f, cardY, 40f + cardWidth, cardY + cardHeight, cardRadius, cardRadius, paint)

            paint.color = Color.parseColor("#15803D") // dark green text
            paint.textSize = 9f
            paint.isFakeBoldText = true
            canvas.drawText("TOTAL MASUK", 50f, cardY + 22f, paint)
            paint.textSize = 12f
            canvas.drawText(currencyFormat.format(totalIncome), 50f, cardY + 45f, paint)

            // Card 2: Pengeluaran (Expense)
            val card2X = 40f + cardWidth + 17.5f
            paint.color = Color.parseColor("#FEE2E2") // light red
            canvas.drawRoundRect(card2X, cardY, card2X + cardWidth, cardY + cardHeight, cardRadius, cardRadius, paint)

            paint.color = Color.parseColor("#B91C1C") // dark red text
            paint.textSize = 9f
            paint.isFakeBoldText = true
            canvas.drawText("TOTAL KELUAR", card2X + 10f, cardY + 22f, paint)
            paint.textSize = 12f
            canvas.drawText(currencyFormat.format(totalExpense), card2X + 10f, cardY + 45f, paint)

            // Card 3: Saldo Akhir (Final Balance)
            val card3X = card2X + cardWidth + 17.5f
            val balanceBg = if (balance >= 0) Color.parseColor("#E0F2FE") else Color.parseColor("#FEF3C7") // light blue or light orange
            val balanceText = if (balance >= 0) Color.parseColor("#0369A1") else Color.parseColor("#B45309")
            paint.color = balanceBg
            canvas.drawRoundRect(card3X, cardY, card3X + cardWidth, cardY + cardHeight, cardRadius, cardRadius, paint)

            paint.color = balanceText
            paint.textSize = 9f
            paint.isFakeBoldText = true
            canvas.drawText("SALDO AKHIR", card3X + 10f, cardY + 22f, paint)
            paint.textSize = 12f
            canvas.drawText(currencyFormat.format(balance), card3X + 10f, cardY + 45f, paint)

            // 3. Table Header
            val tableHeaderY = 215f
            val tableHeaderHeight = 24f

            paint.color = Color.parseColor("#F3F4F6") // light gray
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(40f, tableHeaderY, 555f, tableHeaderY + tableHeaderHeight, 4f, 4f, paint)

            paint.color = Color.parseColor("#4B5563") // dark gray text
            paint.textSize = 8.5f
            paint.isFakeBoldText = true

            val headerYPos = tableHeaderY + 15f
            canvas.drawText("TANGGAL", 45f, headerYPos, paint)
            canvas.drawText("KATEGORI", 125f, headerYPos, paint)
            canvas.drawText("DESKRIPSI", 205f, headerYPos, paint)

            val sDebitHeaderWidth = paint.measureText("DEBIT")
            canvas.drawText("DEBIT", 390f - sDebitHeaderWidth, headerYPos, paint)

            val sKreditHeaderWidth = paint.measureText("KREDIT")
            canvas.drawText("KREDIT", 470f - sKreditHeaderWidth, headerYPos, paint)

            val sSaldoHeaderWidth = paint.measureText("SALDO")
            canvas.drawText("SALDO", 550f - sSaldoHeaderWidth, headerYPos, paint)

            // 4. Transaction Rows
            var pageNum = 1
            var yPos = 258f
            val rowHeight = 26f

            var currentBalance = 0.0
            val sortedTransactions = transactions.sortedBy { it.dateMillis }

            for ((index, t) in sortedTransactions.withIndex()) {
                if (t.type == TransactionType.INCOME) {
                    currentBalance += t.amount
                } else {
                    currentBalance -= t.amount
                }

                if (yPos > 780f) {
                    drawFooter(canvas, pageNum, paint)
                    pdfDocument.finishPage(page)

                    pageNum++
                    val pageInfoSub = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
                    page = pdfDocument.startPage(pageInfoSub)
                    canvas = page.canvas

                    // Mini Header on subsequent pages
                    paint.color = Color.parseColor("#6750A4")
                    paint.style = Paint.Style.FILL
                    canvas.drawRect(40f, 30f, 555f, 50f, paint)

                    paint.color = Color.WHITE
                    paint.textSize = 9f
                    paint.isFakeBoldText = true
                    canvas.drawText("LAPORAN KEUANGAN (LANJUTAN)", 48f, 43f, paint)

                    // Sub-page table header
                    val subTableHeaderY = 60f
                    paint.color = Color.parseColor("#F3F4F6")
                    canvas.drawRoundRect(40f, subTableHeaderY, 555f, subTableHeaderY + tableHeaderHeight, 4f, 4f, paint)

                    paint.color = Color.parseColor("#4B5563")
                    paint.isFakeBoldText = true
                    paint.textSize = 8.5f
                    val subHeaderYPos = subTableHeaderY + 15f
                    canvas.drawText("TANGGAL", 45f, subHeaderYPos, paint)
                    canvas.drawText("KATEGORI", 105f, subHeaderYPos, paint)
                    canvas.drawText("DESKRIPSI", 190f, subHeaderYPos, paint)

                    val subDebitWidth = paint.measureText("DEBIT")
                    canvas.drawText("DEBIT", 390f - subDebitWidth, subHeaderYPos, paint)

                    val subKreditWidth = paint.measureText("KREDIT")
                    canvas.drawText("KREDIT", 470f - subKreditWidth, subHeaderYPos, paint)

                    val subSaldoWidth = paint.measureText("SALDO")
                    canvas.drawText("SALDO", 550f - subSaldoWidth, subHeaderYPos, paint)

                    yPos = 103f
                }

                // Zebra background
                if (index % 2 == 1) {
                    paint.color = Color.parseColor("#F9FAFB")
                    paint.style = Paint.Style.FILL
                    canvas.drawRect(40f, yPos - 14f, 555f, yPos + 12f, paint)
                }

                // Light row separator line
                paint.color = Color.parseColor("#F3F4F6")
                paint.strokeWidth = 0.5f
                paint.style = Paint.Style.STROKE
                canvas.drawLine(40f, yPos + 12f, 555f, yPos + 12f, paint)

                // Row text cells
                paint.style = Paint.Style.FILL
                paint.isFakeBoldText = false
                paint.textSize = 8.5f
                paint.color = Color.parseColor("#1F2937")

                // Date
                val dateStr = dateTimeFormat.format(Date(t.dateMillis))
                canvas.drawText(dateStr, 45f, yPos, paint)

                // Category with truncation check
                var categoryStr = t.category
                if (paint.measureText(categoryStr) > 75f) {
                    categoryStr = truncateText(categoryStr, 75f, paint)
                }
                canvas.drawText(categoryStr, 125f, yPos, paint)

                // Description with truncation check
                var descStr = t.description
                if (descStr.isEmpty()) descStr = "-"
                if (paint.measureText(descStr) > 115f) {
                    descStr = truncateText(descStr, 115f, paint)
                }
                canvas.drawText(descStr, 205f, yPos, paint)

                // Debit & Kredit
                val isIncome = t.type == TransactionType.INCOME
                if (isIncome) {
                    val debitStr = formatCompactRp(t.amount)
                    paint.color = Color.parseColor("#4ADE80") // green text
                    paint.isFakeBoldText = true
                    val debitWidth = paint.measureText(debitStr)
                    canvas.drawText(debitStr, 390f - debitWidth, yPos, paint)

                    paint.color = Color.parseColor("#9CA3AF") // gray for dash
                    paint.isFakeBoldText = false
                    val dashWidth = paint.measureText("-")
                    canvas.drawText("-", 470f - dashWidth, yPos, paint)
                } else {
                    paint.color = Color.parseColor("#9CA3AF") // gray for dash
                    paint.isFakeBoldText = false
                    val dashWidth = paint.measureText("-")
                    canvas.drawText("-", 390f - dashWidth, yPos, paint)

                    val kreditStr = formatCompactRp(t.amount)
                    paint.color = Color.parseColor("#F87171") // red text
                    paint.isFakeBoldText = true
                    val kreditWidth = paint.measureText(kreditStr)
                    canvas.drawText(kreditStr, 470f - kreditWidth, yPos, paint)
                }

                // Saldo (Sisa)
                val saldoStr = formatCompactRp(currentBalance)
                paint.color = Color.parseColor("#1E293B") // Dark slate for running balance
                paint.isFakeBoldText = true
                val saldoWidth = paint.measureText(saldoStr)
                canvas.drawText(saldoStr, 550f - saldoWidth, yPos, paint)

                yPos += rowHeight
            }

            drawFooter(canvas, pageNum, paint)
            pdfDocument.finishPage(page)

            // 5. Add Category Analysis Visualization Page
            pageNum++
            val pageInfoVisual = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
            val visualPage = pdfDocument.startPage(pageInfoVisual)
            val visualCanvas = visualPage.canvas

            // Header Banner for Analysis Page
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#6750A4") // Purple primary
            visualCanvas.drawRoundRect(40f, 30f, 555f, 95f, 12f, 12f, paint)

            paint.color = Color.WHITE
            paint.isFakeBoldText = true
            paint.textSize = 15f
            visualCanvas.drawText("ANALISIS KATEGORI & GRAFIK VISUALISASI", 60f, 60f, paint)

            paint.isFakeBoldText = false
            paint.textSize = 8.5f
            paint.color = Color.parseColor("#EADDFF")
            visualCanvas.drawText("Grafik persentase kontribusi per kategori pengeluaran dan pemasukan", 60f, 80f, paint)

            var visualY = 130f
            val categoryColors = listOf(
                "#6366F1", // Indigo
                "#EC4899", // Pink
                "#10B981", // Emerald
                "#F59E0B", // Amber
                "#3B82F6", // Blue
                "#8B5CF6", // Purple
                "#14B8A6", // Teal
                "#F97316", // Orange
                "#EF4444", // Red
                "#06B6D4"  // Cyan
            )

            // Filter the transactions into Expenses and Income
            val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
            val expenseGroups = expenses.groupBy { 
                val cat = it.category.trim()
                if (cat.isBlank()) "Lainnya" else cat
            }
                .mapValues { entry -> entry.value.sumOf { it.amount } }
                .toList()
                .sortedByDescending { it.second }

            val incomes = transactions.filter { it.type == TransactionType.INCOME }
            val incomeGroups = incomes.groupBy { 
                val cat = it.category.trim()
                if (cat.isBlank()) "Lainnya" else cat
            }
                .mapValues { entry -> entry.value.sumOf { it.amount } }
                .toList()
                .sortedByDescending { it.second }

            val takeCount = 15

            val processedIncomes = if (incomeGroups.size > takeCount) {
                val top = incomeGroups.take(takeCount - 1)
                val othersSum = incomeGroups.drop(takeCount - 1).sumOf { it.second }
                val hasLainnyaInTop = top.any { it.first.trim().equals("Lainnya", ignoreCase = true) }
                val label = if (hasLainnyaInTop) "Lainnya (Sisa)" else "Lainnya"
                top + Pair(label, othersSum)
            } else {
                incomeGroups
            }

            val processedExpenses = if (expenseGroups.size > takeCount) {
                val top = expenseGroups.take(takeCount - 1)
                val othersSum = expenseGroups.drop(takeCount - 1).sumOf { it.second }
                val hasLainnyaInTop = top.any { it.first.trim().equals("Lainnya", ignoreCase = true) }
                val label = if (hasLainnyaInTop) "Lainnya (Sisa)" else "Lainnya"
                top + Pair(label, othersSum)
            } else {
                expenseGroups
            }

            var leftY = 130f
            var rightY = 130f

            // A. Draw Income Category Breakdown (Left Column)
            paint.color = Color.parseColor("#1F2937") // Dark gray
            paint.isFakeBoldText = true
            paint.textSize = 9.5f
            visualCanvas.drawText("A. ANALISIS PEMASUKAN (TOTAL: ${currencyFormat.format(totalIncome)})", 40f, leftY, paint)
            leftY += 18f

            if (processedIncomes.isEmpty()) {
                paint.color = Color.parseColor("#6B7280")
                paint.isFakeBoldText = false
                paint.textSize = 8.5f
                visualCanvas.drawText("Tidak ada data pemasukan.", 45f, leftY, paint)
                leftY += 25f
            } else {
                processedIncomes.forEachIndexed { index, pair ->
                    val (cat, amt) = pair
                    val percentage = if (totalIncome > 0) (amt / totalIncome).toFloat() else 0f
                    val colorHex = categoryColors[(index + 3) % categoryColors.size] // offset color index slightly for contrast
                    
                    // Category label
                    paint.color = Color.parseColor("#374151")
                    paint.textSize = 8f
                    paint.isFakeBoldText = true
                    val label = if (cat.isBlank()) "Lainnya" else cat
                    visualCanvas.drawText(label, 40f, leftY, paint)
                    
                    // Percentage and Value Text on right of left column (near 285f)
                    val valueStr = "${String.format(Locale.US, "%.1f", percentage * 100)}% (${currencyFormat.format(amt)})"
                    paint.color = Color.parseColor(colorHex)
                    paint.textSize = 8f
                    val textWidth = paint.measureText(valueStr)
                    visualCanvas.drawText(valueStr, 285f - textWidth, leftY, paint)
                    
                    leftY += 5f
                    
                    // Progress Bar background
                    paint.color = Color.parseColor("#F3F4F6")
                    paint.style = Paint.Style.FILL
                    visualCanvas.drawRoundRect(40f, leftY, 285f, leftY + 6f, 3f, 3f, paint)
                    
                    // Colored progress bar matching the percentage
                    paint.color = Color.parseColor(colorHex)
                    val barEnd = 40f + (245f * percentage)
                    if (barEnd > 40f) {
                        visualCanvas.drawRoundRect(40f, leftY, barEnd, leftY + 6f, 3f, 3f, paint)
                    }
                    
                    leftY += 20f
                }
            }

            // B. Draw Expenses Category Breakdown (Right Column)
            paint.color = Color.parseColor("#1F2937")
            paint.isFakeBoldText = true
            paint.textSize = 9.5f
            visualCanvas.drawText("B. ANALISIS PENGELUARAN (TOTAL: ${currencyFormat.format(totalExpense)})", 310f, rightY, paint)
            rightY += 18f

            if (processedExpenses.isEmpty()) {
                paint.color = Color.parseColor("#6B7280")
                paint.isFakeBoldText = false
                paint.textSize = 8.5f
                visualCanvas.drawText("Tidak ada data pengeluaran.", 315f, rightY, paint)
                rightY += 25f
            } else {
                processedExpenses.forEachIndexed { index, pair ->
                    val (cat, amt) = pair
                    val percentage = if (totalExpense > 0) (amt / totalExpense).toFloat() else 0f
                    val colorHex = categoryColors[index % categoryColors.size]
                    
                    // Category label
                    paint.color = Color.parseColor("#374151")
                    paint.textSize = 8f
                    paint.isFakeBoldText = true
                    val label = if (cat.isBlank()) "Lainnya" else cat
                    visualCanvas.drawText(label, 310f, rightY, paint)
                    
                    // Percentage and Value Text on right of right column (near 555f)
                    val valueStr = "${String.format(Locale.US, "%.1f", percentage * 100)}% (${currencyFormat.format(amt)})"
                    paint.color = Color.parseColor(colorHex)
                    paint.textSize = 8f
                    val textWidth = paint.measureText(valueStr)
                    visualCanvas.drawText(valueStr, 555f - textWidth, rightY, paint)
                    
                    rightY += 5f
                    
                    // Progress Bar background
                    paint.color = Color.parseColor("#F3F4F6")
                    paint.style = Paint.Style.FILL
                    visualCanvas.drawRoundRect(310f, rightY, 555f, rightY + 6f, 3f, 3f, paint)
                    
                    // Colored progress bar matching the percentage
                    paint.color = Color.parseColor(colorHex)
                    val barEnd = 310f + (245f * percentage)
                    if (barEnd > 310f) {
                        visualCanvas.drawRoundRect(310f, rightY, barEnd, rightY + 6f, 3f, 3f, paint)
                    }
                    
                    rightY += 20f
                }
            }

            visualY = maxOf(leftY, rightY)

            // C. 12-Month Bar Chart for Yearly Report
            if (reportMode == ReportMode.YEARLY) {
                visualY += 15f
                paint.color = Color.parseColor("#1F2937")
                paint.isFakeBoldText = true
                paint.textSize = 12f
                visualCanvas.drawText("C. GRAFIK BULANAN (JAN - DES)", 40f, visualY, paint)
                visualY += 15f

                val chartHeight = 110f
                val chartBottom = visualY + chartHeight

                // Draw baseline
                paint.color = Color.parseColor("#E5E7EB")
                paint.strokeWidth = 1f
                paint.style = Paint.Style.STROKE
                visualCanvas.drawLine(40f, chartBottom, 555f, chartBottom, paint)
                paint.style = Paint.Style.FILL

                // Compute monthly totals
                val monthlyTotals = Array(12) { Pair(0.0, 0.0) }
                val cal = Calendar.getInstance()
                transactions.forEach { t ->
                    cal.timeInMillis = t.dateMillis
                    val m = cal.get(Calendar.MONTH)
                    if (m in 0..11) {
                        val cur = monthlyTotals[m]
                        if (t.type == TransactionType.INCOME) {
                            monthlyTotals[m] = Pair(cur.first + t.amount, cur.second)
                        } else {
                            monthlyTotals[m] = Pair(cur.first, cur.second + t.amount)
                        }
                    }
                }

                val maxVal = monthlyTotals.maxOf { maxOf(it.first, it.second) }.coerceAtLeast(1.0)
                val segmentWidth = 515f / 12f
                val monthNamesShort = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agt", "Sep", "Okt", "Nov", "Des")

                monthlyTotals.forEachIndexed { index, (income, expense) ->
                    val startX = 40f + index * segmentWidth
                    val barWidth = segmentWidth * 0.35f
                    val gap = (segmentWidth - (barWidth * 2)) / 2f

                    val incomeHeight = ((income / maxVal) * chartHeight).toFloat()
                    val expenseHeight = ((expense / maxVal) * chartHeight).toFloat()

                    // Draw income bar (green)
                    if (income > 0) {
                        paint.color = Color.parseColor("#4ADE80")
                        visualCanvas.drawRoundRect(
                            startX + gap,
                            chartBottom - incomeHeight,
                            startX + gap + barWidth,
                            chartBottom,
                            2f, 2f, paint
                        )
                    }

                    // Draw expense bar (red)
                    if (expense > 0) {
                        paint.color = Color.parseColor("#F87171")
                        visualCanvas.drawRoundRect(
                            startX + gap + barWidth,
                            chartBottom - expenseHeight,
                            startX + gap + barWidth * 2,
                            chartBottom,
                            2f, 2f, paint
                        )
                    }

                    // Draw month label under baseline
                    paint.color = Color.parseColor("#6B7280")
                    paint.textSize = 8f
                    paint.isFakeBoldText = false
                    val label = monthNamesShort[index]
                    val labelWidth = paint.measureText(label)
                    val labelX = startX + (segmentWidth - labelWidth) / 2f
                    visualCanvas.drawText(label, labelX, chartBottom + 12f, paint)
                }
            }
            
            drawFooter(visualCanvas, pageNum, paint)
            pdfDocument.finishPage(visualPage)

            val fileName = getExportFileName(bookName, reportMode, currentYear, currentMonth, "pdf")
            val file = File(context.cacheDir, fileName)
            val fos = FileOutputStream(file)
            pdfDocument.writeTo(fos)
            pdfDocument.close()
            fos.close()
            
            shareFile(context, file, "application/pdf")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun truncateText(text: String, maxWidth: Float, paint: Paint): String {
        var result = text
        while (result.isNotEmpty() && paint.measureText("$result...") > maxWidth) {
            result = result.dropLast(1)
        }
        return if (result.length < text.length) "$result..." else result
    }

    private fun drawFooter(canvas: Canvas, pageNum: Int, paint: Paint) {
        paint.color = Color.parseColor("#9CA3AF")
        paint.textSize = 8f
        paint.isFakeBoldText = false
        val footerText = "Halaman $pageNum"
        val textWidth = paint.measureText(footerText)
        canvas.drawText(footerText, 555f - textWidth, 815f, paint)

        val prefix = "Laporan ini dibuat otomatis oleh Aplikasi "
        val appName = "Uang Siapa?"
        
        paint.color = Color.parseColor("#9CA3AF")
        paint.textSize = 8f
        
        var footerX = 40f
        paint.isFakeBoldText = false
        canvas.drawText(prefix, footerX, 815f, paint)
        footerX += paint.measureText(prefix)
        
        paint.isFakeBoldText = true
        canvas.drawText(appName, footerX, 815f, paint)
        paint.isFakeBoldText = false

        // Light rule above footer
        paint.strokeWidth = 0.5f
        paint.style = Paint.Style.STROKE
        paint.color = Color.parseColor("#E5E7EB")
        canvas.drawLine(40f, 805f, 555f, 805f, paint)
        paint.style = Paint.Style.FILL
    }

    private fun shareFile(context: Context, file: File, mimeType: String) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Bagikan Laporan").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
