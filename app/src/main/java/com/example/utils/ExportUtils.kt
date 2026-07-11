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
import org.dhatim.fastexcel.Workbook
import org.dhatim.fastexcel.Worksheet

object ExportUtils {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private fun formatCompactRp(amount: Double): String {
        val formatter = NumberFormat.getNumberInstance(Locale("id", "ID"))
        return if (amount < 0) {
            "-Rp " + formatter.format(-amount)
        } else {
            "Rp " + formatter.format(amount)
        }
    }

    fun exportToXlsx(context: Context, transactions: List<Transaction>) {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
            val fileName = "uang_siapa_$timestamp.xlsx"
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { fos ->
                val workbook = Workbook(fos, "UangSiapa", "1.0")
                val worksheet = workbook.newWorksheet("Transaksi")
                
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
                    val dateStr = dateFormat.format(Date(t.dateMillis))
                    
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
                workbook.finish()
            }
            
            shareFile(context, file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun exportToCsv(context: Context, transactions: List<Transaction>) {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
            val fileName = "uang_siapa_$timestamp.csv"
            val file = File(context.cacheDir, fileName)
            val fileOutputStream = FileOutputStream(file)
            val writer = fileOutputStream.writer()
            
            writer.write("Tanggal,Kategori,Deskripsi,Debit (Pemasukan),Kredit (Pengeluaran),Saldo\n")
            var currentBalance = 0.0
            val sortedTransactions = transactions.sortedBy { it.dateMillis }
            for (t in sortedTransactions) {
                val dateStr = dateFormat.format(Date(t.dateMillis))
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

    fun exportToPdf(context: Context, transactions: List<Transaction>) {
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
            val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

            // 1. Header Banner
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#6750A4") // Purple primary
            canvas.drawRoundRect(40f, 30f, 555f, 110f, 12f, 12f, paint)

            // Header text
            paint.color = Color.WHITE
            paint.isFakeBoldText = true
            paint.textSize = 20f
            canvas.drawText("LAPORAN KEUANGAN", 60f, 65f, paint)

            paint.isFakeBoldText = false
            paint.textSize = 10f
            paint.color = Color.parseColor("#EADDFF")
            
            val appPrefix = "Aplikasi "
            val appName = "Uang Siapa?"
            val appSuffix = "  |  Dicetak pada: ${SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id", "ID")).format(Date())}"
            
            var headerX = 60f
            paint.isFakeBoldText = false
            canvas.drawText(appPrefix, headerX, 90f, paint)
            headerX += paint.measureText(appPrefix)
            
            paint.isFakeBoldText = true
            canvas.drawText(appName, headerX, 90f, paint)
            headerX += paint.measureText(appName)
            
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
            canvas.drawText("KATEGORI", 105f, headerYPos, paint)
            canvas.drawText("DESKRIPSI", 190f, headerYPos, paint)

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
                val dateStr = dateFormat.format(Date(t.dateMillis))
                canvas.drawText(dateStr, 45f, yPos, paint)

                // Category with truncation check
                var categoryStr = t.category
                if (paint.measureText(categoryStr) > 75f) {
                    categoryStr = truncateText(categoryStr, 75f, paint)
                }
                canvas.drawText(categoryStr, 105f, yPos, paint)

                // Description with truncation check
                var descStr = t.description
                if (descStr.isEmpty()) descStr = "-"
                if (paint.measureText(descStr) > 115f) {
                    descStr = truncateText(descStr, 115f, paint)
                }
                canvas.drawText(descStr, 190f, yPos, paint)

                // Debit & Kredit
                val isIncome = t.type == TransactionType.INCOME
                if (isIncome) {
                    val debitStr = formatCompactRp(t.amount)
                    paint.color = Color.parseColor("#16A34A") // green text
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
                    paint.color = Color.parseColor("#DC2626") // red text
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
            val expenseGroups = expenses.groupBy { it.category }
                .mapValues { entry -> entry.value.sumOf { it.amount } }
                .toList()
                .sortedByDescending { it.second }

            val incomes = transactions.filter { it.type == TransactionType.INCOME }
            val incomeGroups = incomes.groupBy { it.category }
                .mapValues { entry -> entry.value.sumOf { it.amount } }
                .toList()
                .sortedByDescending { it.second }

            // A. Draw Expenses Category Breakdown
            paint.color = Color.parseColor("#1F2937") // Dark gray
            paint.isFakeBoldText = true
            paint.textSize = 12f
            visualCanvas.drawText("A. ANALISIS PENGELUARAN (TOTAL: ${currencyFormat.format(totalExpense)})", 40f, visualY, paint)
            visualY += 20f

            if (expenseGroups.isEmpty()) {
                paint.color = Color.parseColor("#6B7280")
                paint.isFakeBoldText = false
                paint.textSize = 9f
                visualCanvas.drawText("Tidak ada data pengeluaran.", 50f, visualY, paint)
                visualY += 25f
            } else {
                expenseGroups.take(6).forEachIndexed { index, pair ->
                    val (cat, amt) = pair
                    val percentage = if (totalExpense > 0) (amt / totalExpense).toFloat() else 0f
                    val colorHex = categoryColors[index % categoryColors.size]
                    
                    // Category label
                    paint.color = Color.parseColor("#374151")
                    paint.textSize = 9f
                    paint.isFakeBoldText = true
                    val label = if (cat.isBlank()) "Lainnya" else cat
                    visualCanvas.drawText(label, 45f, visualY, paint)
                    
                    // Percentage and Value Text on right
                    val valueStr = "${String.format(Locale.US, "%.1f", percentage * 100)}% (${currencyFormat.format(amt)})"
                    paint.color = Color.parseColor(colorHex)
                    val textWidth = paint.measureText(valueStr)
                    visualCanvas.drawText(valueStr, 555f - textWidth, visualY, paint)
                    
                    visualY += 6f
                    
                    // Progress Bar background
                    paint.color = Color.parseColor("#F3F4F6")
                    paint.style = Paint.Style.FILL
                    visualCanvas.drawRoundRect(45f, visualY, 555f, visualY + 8f, 4f, 4f, paint)
                    
                    // Colored progress bar matching the percentage
                    paint.color = Color.parseColor(colorHex)
                    val barEnd = 45f + (510f * percentage)
                    if (barEnd > 45f) {
                        visualCanvas.drawRoundRect(45f, visualY, barEnd, visualY + 8f, 4f, 4f, paint)
                    }
                    
                    visualY += 24f
                }
            }

            visualY += 15f

            // B. Draw Income Category Breakdown
            paint.color = Color.parseColor("#1F2937")
            paint.isFakeBoldText = true
            paint.textSize = 12f
            visualCanvas.drawText("B. ANALISIS PEMASUKAN (TOTAL: ${currencyFormat.format(totalIncome)})", 40f, visualY, paint)
            visualY += 20f

            if (incomeGroups.isEmpty()) {
                paint.color = Color.parseColor("#6B7280")
                paint.isFakeBoldText = false
                paint.textSize = 9f
                visualCanvas.drawText("Tidak ada data pemasukan.", 50f, visualY, paint)
                visualY += 25f
            } else {
                incomeGroups.take(6).forEachIndexed { index, pair ->
                    val (cat, amt) = pair
                    val percentage = if (totalIncome > 0) (amt / totalIncome).toFloat() else 0f
                    val colorHex = categoryColors[(index + 3) % categoryColors.size] // offset color index slightly for contrast
                    
                    // Category label
                    paint.color = Color.parseColor("#374151")
                    paint.textSize = 9f
                    paint.isFakeBoldText = true
                    val label = if (cat.isBlank()) "Lainnya" else cat
                    visualCanvas.drawText(label, 45f, visualY, paint)
                    
                    // Percentage and Value Text on right
                    val valueStr = "${String.format(Locale.US, "%.1f", percentage * 100)}% (${currencyFormat.format(amt)})"
                    paint.color = Color.parseColor(colorHex)
                    val textWidth = paint.measureText(valueStr)
                    visualCanvas.drawText(valueStr, 555f - textWidth, visualY, paint)
                    
                    visualY += 6f
                    
                    // Progress Bar background
                    paint.color = Color.parseColor("#F3F4F6")
                    paint.style = Paint.Style.FILL
                    visualCanvas.drawRoundRect(45f, visualY, 555f, visualY + 8f, 4f, 4f, paint)
                    
                    // Colored progress bar matching the percentage
                    paint.color = Color.parseColor(colorHex)
                    val barEnd = 45f + (510f * percentage)
                    if (barEnd > 45f) {
                        visualCanvas.drawRoundRect(45f, visualY, barEnd, visualY + 8f, 4f, 4f, paint)
                    }
                    
                    visualY += 24f
                }
            }
            
            drawFooter(visualCanvas, pageNum, paint)
            pdfDocument.finishPage(visualPage)

            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
            val fileName = "uang_siapa_$timestamp.pdf"
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
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Report"))
    }
}
