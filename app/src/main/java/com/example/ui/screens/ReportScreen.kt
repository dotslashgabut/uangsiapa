package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import kotlinx.coroutines.launch
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.TransactionType
import com.example.ui.theme.*
import com.example.ui.viewmodel.ReportMode
import com.example.ui.viewmodel.ReportViewModel
import com.example.utils.ExportUtils
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    viewModel: ReportViewModel,
    activeBookId: Int = 1,
    activeBookName: String = "Buku Utama",
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 } }
    val isDark = MaterialTheme.colorScheme.background == DarkBackground

    LaunchedEffect(activeBookId) {
        viewModel.setBookId(activeBookId)
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.reportMode == ReportMode.MONTHLY) "Laporan Bulanan" else "Laporan Tahunan", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        ExportUtils.exportToXlsx(
                            context = context, 
                            transactions = uiState.transactions, 
                            bookName = activeBookName,
                            reportMode = uiState.reportMode,
                            year = uiState.currentYear,
                            month = uiState.currentMonth
                        ) 
                    }) {
                        Icon(Icons.Default.GridOn, contentDescription = "Ekspor Excel (.xlsx)")
                    }
                    IconButton(onClick = { 
                        ExportUtils.exportToCsv(
                            context = context, 
                            transactions = uiState.transactions, 
                            bookName = activeBookName,
                            reportMode = uiState.reportMode
                        ) 
                    }) {
                        Icon(Icons.Default.TableChart, contentDescription = "Ekspor CSV")
                    }
                    IconButton(onClick = {
                        ExportUtils.exportToPdf(
                            context = context,
                            transactions = uiState.transactions,
                            bookName = activeBookName,
                            reportMode = uiState.reportMode,
                            currentMonth = uiState.currentMonth,
                            currentYear = uiState.currentYear
                        )
                    }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Ekspor PDF")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        val scrollState = rememberScrollState()
        val coroutineScope = rememberCoroutineScope()
        val showStickyHeader by remember {
            derivedStateOf { scrollState.value > 120 }
        }

        val stickyDisplayText = remember(uiState.reportMode, uiState.currentMonth, uiState.currentYear) {
            if (uiState.reportMode == ReportMode.MONTHLY) {
                val monthName = SimpleDateFormat("MMMM yyyy", Locale("id", "ID")).format(
                    Calendar.getInstance().apply {
                        set(Calendar.MONTH, uiState.currentMonth)
                        set(Calendar.YEAR, uiState.currentYear)
                    }.time
                )
                monthName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            } else {
                "Tahun ${uiState.currentYear}"
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
            // Tabs / Selector for Mode (Bulanan vs Tahunan)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val modeMonthly = uiState.reportMode == ReportMode.MONTHLY
                
                // Monthly Tab
                Box(
                    modifier = Modifier
                        .weight(1.5f)
                        .background(
                            color = if (modeMonthly) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { viewModel.setReportMode(ReportMode.MONTHLY) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Bulanan",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (modeMonthly) FontWeight.Bold else FontWeight.Normal,
                        color = if (modeMonthly) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Yearly Tab
                Box(
                    modifier = Modifier
                        .weight(1.5f)
                        .background(
                            color = if (!modeMonthly) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { viewModel.setReportMode(ReportMode.YEARLY) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tahunan",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (!modeMonthly) FontWeight.Bold else FontWeight.Normal,
                        color = if (!modeMonthly) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Month/Year Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { 
                    if (uiState.reportMode == ReportMode.MONTHLY) viewModel.prevMonth() else viewModel.prevYear() 
                }) {
                    Icon(
                        Icons.Default.ChevronLeft, 
                        contentDescription = if (uiState.reportMode == ReportMode.MONTHLY) "Bulan Sebelumnya" else "Tahun Sebelumnya"
                    )
                }
                
                val displayText = remember(uiState.reportMode, uiState.currentMonth, uiState.currentYear) {
                    if (uiState.reportMode == ReportMode.MONTHLY) {
                        val monthName = SimpleDateFormat("MMMM yyyy", Locale("id", "ID")).format(
                            Calendar.getInstance().apply {
                                set(Calendar.MONTH, uiState.currentMonth)
                                set(Calendar.YEAR, uiState.currentYear)
                            }.time
                        )
                        monthName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                    } else {
                        "Tahun ${uiState.currentYear}"
                    }
                }
                
                Text(
                    text = displayText, 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(onClick = { 
                    if (uiState.reportMode == ReportMode.MONTHLY) viewModel.nextMonth() else viewModel.nextYear() 
                }) {
                    Icon(
                        Icons.Default.ChevronRight, 
                        contentDescription = if (uiState.reportMode == ReportMode.MONTHLY) "Bulan Berikutnya" else "Tahun Berikutnya"
                    )
                }
            }

            // Summary Cards Row
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(24.dp)
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Net Balance
                        val netBalance = uiState.totalIncome - uiState.totalExpense
                        Column {
                            Text(
                                "TOTAL NETTO", 
                                style = MaterialTheme.typography.labelSmall, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                            Text(
                                currencyFormat.format(netBalance),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (netBalance >= 0) {
                                    if (isDark) IncomeColorDark else IncomeColorLight
                                } else {
                                    if (isDark) ExpenseColorDark else ExpenseColorLight
                                }
                            )
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (netBalance >= 0) "+ Surplus" else "- Defisit",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Income Summary Column
                        Column(horizontalAlignment = Alignment.Start, modifier = Modifier.weight(1f)) {
                            Text(
                                "Pemasukan", 
                                style = MaterialTheme.typography.labelMedium, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                currencyFormat.format(uiState.totalIncome), 
                                color = if (isDark) IncomeColorDark else IncomeColorLight, 
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        // Expense Summary Column
                        Column(horizontalAlignment = Alignment.Start, modifier = Modifier.weight(1f)) {
                            Text(
                                "Pengeluaran", 
                                style = MaterialTheme.typography.labelMedium, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                currencyFormat.format(uiState.totalExpense), 
                                color = if (isDark) ExpenseColorDark else ExpenseColorLight, 
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Chart
            Text(
                "Ringkasan Visual",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Box(modifier = Modifier.fillMaxWidth().height(250.dp).padding(16.dp)) {
                if (uiState.totalIncome == 0.0 && uiState.totalExpense == 0.0) {
                    Text("Belum ada data", modifier = Modifier.align(Alignment.Center))
                } else {
                    if (uiState.reportMode == ReportMode.MONTHLY) {
                        SimpleBarChart(
                            income = uiState.totalIncome,
                            expense = uiState.totalExpense,
                            isDark = isDark,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        val monthlyTotals = remember(uiState.transactions) {
                            val totals = Array(12) { Pair(0.0, 0.0) }
                            val cal = Calendar.getInstance()
                            uiState.transactions.forEach { t ->
                                cal.timeInMillis = t.dateMillis
                                val m = cal.get(Calendar.MONTH)
                                if (m in 0..11) {
                                    val cur = totals[m]
                                    if (t.type == TransactionType.INCOME) {
                                        totals[m] = Pair(cur.first + t.amount, cur.second)
                                    } else {
                                        totals[m] = Pair(cur.first, cur.second + t.amount)
                                    }
                                }
                            }
                            totals.toList()
                        }
                        YearlyBarChart(
                            monthlyTotals = monthlyTotals,
                            isDark = isDark,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Category Visual Header
            Text(
                "Analisis Kategori",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // State for category selected type
            var categoryType by remember { mutableStateOf(TransactionType.EXPENSE) }

            // Elegant Toggle Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val expenseSelected = categoryType == TransactionType.EXPENSE
                
                // Button Pemasukan
                FilledTonalButton(
                    onClick = { categoryType = TransactionType.INCOME },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (!expenseSelected) {
                            if (isDark) IncomeBgDark else IncomeBgLight
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        },
                        contentColor = if (!expenseSelected) {
                            if (isDark) IncomeColorDark else IncomeColorLight
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Pemasukan", fontWeight = if (!expenseSelected) FontWeight.Bold else FontWeight.Normal)
                }

                // Button Pengeluaran
                FilledTonalButton(
                    onClick = { categoryType = TransactionType.EXPENSE },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (expenseSelected) {
                            if (isDark) ExpenseBgDark else ExpenseBgLight
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        },
                        contentColor = if (expenseSelected) {
                            if (isDark) ExpenseColorDark else ExpenseColorLight
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Pengeluaran", fontWeight = if (expenseSelected) FontWeight.Bold else FontWeight.Normal)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Filter transactions by selected type and group them
            val selectedTransactions = remember(uiState.transactions, categoryType) {
                uiState.transactions.filter { it.type == categoryType }
            }
            val totalForType = remember(selectedTransactions) {
                selectedTransactions.sumOf { it.amount }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                if (selectedTransactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Belum ada data ${if (categoryType == TransactionType.EXPENSE) "pengeluaran" else "pemasukan"} di ${if (uiState.reportMode == ReportMode.MONTHLY) "bulan ini" else "tahun ini"}.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    val categoryGroups = remember(selectedTransactions) {
                        selectedTransactions.groupBy { it.category }
                            .mapValues { entry -> entry.value.sumOf { it.amount } }
                            .toList()
                            .sortedByDescending { it.second }
                    }

                    val categoryColors = listOf(
                        Color(0xFF6366F1), // Indigo
                        Color(0xFFEC4899), // Pink
                        Color(0xFF10B981), // Emerald
                        Color(0xFFF59E0B), // Amber
                        Color(0xFF3B82F6), // Blue
                        Color(0xFF8B5CF6), // Purple
                        Color(0xFF14B8A6), // Teal
                        Color(0xFFF97316), // Orange
                        Color(0xFFEF4444), // Red
                        Color(0xFF06B6D4)  // Cyan
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Total: ${currencyFormat.format(totalForType)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (categoryType == TransactionType.EXPENSE) {
                                if (isDark) ExpenseColorDark else ExpenseColorLight
                            } else {
                                if (isDark) IncomeColorDark else IncomeColorLight
                            },
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        categoryGroups.forEachIndexed { index, pair ->
                            val (cat, amt) = pair
                            val percentage = if (totalForType > 0) (amt / totalForType).toFloat() else 0f
                            val color = categoryColors[index % categoryColors.size]

                            CategoryProgressRow(
                                categoryName = cat,
                                amount = amt,
                                percentage = percentage,
                                color = color,
                                currencyFormat = currencyFormat
                            )
                        }
                    }
                }
            }

            // Close main Column
            }

            AnimatedVisibility(
                visible = showStickyHeader,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                    shadowElevation = 6.dp,
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { 
                                if (uiState.reportMode == ReportMode.MONTHLY) viewModel.prevMonth() else viewModel.prevYear() 
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.ChevronLeft, 
                                contentDescription = if (uiState.reportMode == ReportMode.MONTHLY) "Bulan Sebelumnya" else "Tahun Sebelumnya",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        Text(
                            text = stickyDisplayText, 
                            style = MaterialTheme.typography.titleMedium, 
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        IconButton(
                            onClick = { 
                                if (uiState.reportMode == ReportMode.MONTHLY) viewModel.nextMonth() else viewModel.nextYear() 
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.ChevronRight, 
                                contentDescription = if (uiState.reportMode == ReportMode.MONTHLY) "Bulan Berikutnya" else "Tahun Berikutnya",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryProgressRow(
    categoryName: String,
    amount: Double,
    percentage: Float,
    color: Color,
    currencyFormat: NumberFormat
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = categoryName.ifBlank { "Lainnya" },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${String.format(Locale.US, "%.1f", percentage * 100)}% (${currencyFormat.format(amount)})",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        // Styled Custom Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(4.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = percentage.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(
                        color = color,
                        shape = RoundedCornerShape(4.dp)
                    )
            )
        }
    }
}

@Composable
fun SimpleBarChart(income: Double, expense: Double, isDark: Boolean, modifier: Modifier = Modifier) {
    val maxVal = maxOf(income, expense)
    val incomeColor = if (isDark) IncomeColorDark else IncomeColorLight
    val expenseColor = if (isDark) ExpenseColorDark else ExpenseColorLight
    val lineColor = if (isDark) DarkOutlineVariant else LightOutlineVariant
    
    Canvas(modifier = modifier) {
        val barWidth = size.width / 4
        val maxBarHeight = size.height - 40f // Leave room for labels
        
        val incomeHeight = if (maxVal > 0) (income / maxVal) * maxBarHeight else 0.0
        val expenseHeight = if (maxVal > 0) (expense / maxVal) * maxBarHeight else 0.0
        
        val startXIncome = size.width / 4 - barWidth / 2
        val startXExpense = (size.width * 3) / 4 - barWidth / 2
        
        // Draw Income Bar
        drawRoundRect(
            color = incomeColor,
            topLeft = Offset(startXIncome, size.height - incomeHeight.toFloat() - 20f),
            size = Size(barWidth, incomeHeight.toFloat()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
        )
        
        // Draw Expense Bar
        drawRoundRect(
            color = expenseColor,
            topLeft = Offset(startXExpense, size.height - expenseHeight.toFloat() - 20f),
            size = Size(barWidth, expenseHeight.toFloat()),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
        )
        
        // Base line
        drawLine(
            color = lineColor,
            start = Offset(0f, size.height - 20f),
            end = Offset(size.width, size.height - 20f),
            strokeWidth = 2f
        )
    }
}

@Composable
fun YearlyBarChart(
    monthlyTotals: List<Pair<Double, Double>>,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val incomeColor = if (isDark) IncomeColorDark else IncomeColorLight
    val expenseColor = if (isDark) ExpenseColorDark else ExpenseColorLight
    val lineColor = if (isDark) DarkOutlineVariant else LightOutlineVariant

    val maxVal = monthlyTotals.maxOf { maxOf(it.first, it.second) }.coerceAtLeast(1.0)

    Column(modifier = modifier) {
        Canvas(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val segmentWidth = size.width / 12f
            val maxBarHeight = size.height

            monthlyTotals.forEachIndexed { index, (income, expense) ->
                val startX = index * segmentWidth
                val barWidth = (segmentWidth * 0.35f).coerceAtLeast(4f)
                val gap = (segmentWidth - (barWidth * 2)) / 2f

                val incomeHeight = (income / maxVal) * maxBarHeight
                val expenseHeight = (expense / maxVal) * maxBarHeight

                // Draw Income Bar (Left)
                if (income > 0) {
                    drawRoundRect(
                        color = incomeColor,
                        topLeft = Offset(startX + gap, (size.height - incomeHeight).toFloat()),
                        size = Size(barWidth, incomeHeight.toFloat()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                    )
                }

                // Draw Expense Bar (Right)
                if (expense > 0) {
                    drawRoundRect(
                        color = expenseColor,
                        topLeft = Offset(startX + gap + barWidth, (size.height - expenseHeight).toFloat()),
                        size = Size(barWidth, expenseHeight.toFloat()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                    )
                }
            }

            // Base line
            drawLine(
                color = lineColor,
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
                strokeWidth = 2f
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val monthLabels = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agt", "Sep", "Okt", "Nov", "Des")
            monthLabels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}
