package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Transaction
import com.example.data.TransactionType
import com.example.ui.viewmodel.MainViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import com.example.data.Book
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.scrollBy
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    onNavigateToAddEdit: (Int?) -> Unit,
    onNavigateToReport: () -> Unit
) {
    val transactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val allBooks by viewModel.allBooks.collectAsStateWithLifecycle()
    val activeBook by viewModel.activeBook.collectAsStateWithLifecycle()

    var showBookManager by remember { mutableStateOf(false) }
    var showAddBookDialog by remember { mutableStateOf(false) }
    var showEditBookDialog by remember { mutableStateOf<Book?>(null) }
    var showDeleteBookConfirm by remember { mutableStateOf<Book?>(null) }

    val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    val balance = totalIncome - totalExpense

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    var selectedTypeFilter by remember { mutableStateOf<TransactionType?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf<String>("Semua") }
    var selectedDateFilterType by remember { mutableStateOf<DateFilterType>(DateFilterType.ALL) }
    var customStartDate by remember { mutableStateOf<Long?>(null) }
    var customEndDate by remember { mutableStateOf<Long?>(null) }
    
    var showTypeMenu by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showDateMenu by remember { mutableStateOf(false) }

    var showCustomDateDialog by remember { mutableStateOf(false) }
    var tempStartDate by remember { mutableStateOf<Long?>(null) }
    var tempEndDate by remember { mutableStateOf<Long?>(null) }

    val availableCategories = remember(transactions) {
        transactions.map { it.category.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
    }

    LaunchedEffect(availableCategories) {
        if (selectedCategoryFilter != "Semua" && !availableCategories.contains(selectedCategoryFilter)) {
            selectedCategoryFilter = "Semua"
        }
    }

    val filteredTransactions = remember(transactions, selectedTypeFilter, selectedCategoryFilter, selectedDateFilterType, customStartDate, customEndDate) {
        transactions.filter { t ->
            val matchesType = selectedTypeFilter == null || t.type == selectedTypeFilter
            val matchesCategory = selectedCategoryFilter == "Semua" || t.category.trim().equals(selectedCategoryFilter.trim(), ignoreCase = true)
            
            val matchesDate = when (selectedDateFilterType) {
                DateFilterType.ALL -> true
                DateFilterType.TODAY -> {
                    isSameDay(t.dateMillis, System.currentTimeMillis())
                }
                DateFilterType.LAST_7_DAYS -> {
                    val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
                    t.dateMillis >= startOfDay(sevenDaysAgo)
                }
                DateFilterType.THIS_MONTH -> {
                    isSameMonthAndYear(t.dateMillis, System.currentTimeMillis())
                }
                DateFilterType.CUSTOM -> {
                    val start = customStartDate?.let { startOfDay(it) } ?: Long.MIN_VALUE
                    val end = customEndDate?.let { endOfDay(it) } ?: Long.MAX_VALUE
                    t.dateMillis in start..end
                }
            }
            matchesType && matchesCategory && matchesDate
        }
    }

    val filteredIncome = filteredTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val filteredExpense = filteredTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    val filteredBalance = filteredIncome - filteredExpense

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val importedList = com.example.utils.BackupUtils.importBackup(context, uri)
            if (importedList != null) {
                viewModel.importBackup(importedList)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Berhasil mengimpor ${importedList.size} transaksi",
                        duration = SnackbarDuration.Short
                    )
                }
            } else {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Gagal mengimpor backup. Format file tidak sesuai.",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }
    val currentMonthYear = remember { SimpleDateFormat("MMMM yyyy", Locale("id", "ID")).format(Date()) }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { showBookManager = true }
                            .padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = "Buku Keuangan",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = activeBook?.name ?: "Buku Utama",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Ubah Buku",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onToggleTheme,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Icon(
                            imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Ganti Tema",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    IconButton(
                        onClick = onNavigateToReport,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = "Laporan",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Menu Lainnya",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Ekspor Backup (JSON)") },
                                leadingIcon = { Icon(Icons.Default.Backup, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    com.example.utils.BackupUtils.exportBackup(context, transactions, activeBook?.name ?: "Buku Utama")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Impor Backup (JSON)") },
                                leadingIcon = { Icon(Icons.Default.Restore, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    importLauncher.launch("*/*")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Tentang Aplikasi") },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    showInfoDialog = true
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToAddEdit(null) },
                modifier = Modifier
                    .navigationBarsPadding()
                    .displayCutoutPadding(),
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Transaksi", modifier = Modifier.size(32.dp))
            }
        }
    ) { paddingValues ->
        val listState = rememberLazyListState()
        val showCompactHeader by remember {
            derivedStateOf {
                listState.firstVisibleItemIndex > 0
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(listState) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.type == PointerEventType.Scroll) {
                                    val delta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                                    if (delta != 0f) {
                                        coroutineScope.launch {
                                            listState.scrollBy(delta * 120f)
                                        }
                                    }
                                } else if (event.type == PointerEventType.Move) {
                                    val change = event.changes.firstOrNull()
                                    if (change != null && change.pressed) {
                                        val currentY = change.position.y
                                        val previousY = change.previousPosition.y
                                        val deltaY = previousY - currentY
                                        if (deltaY != 0f) {
                                            coroutineScope.launch {
                                                listState.scrollBy(deltaY)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
            // Balance Card Item
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 2.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = if (isDark) {
                                    listOf(
                                        Color(0xFF3B1B7D), // Glowing premium violet
                                        Color(0xFF1E1B4B), // Deep midnight indigo
                                        Color(0xFF13111C)  // Midnight slate
                                    )
                                } else {
                                    listOf(
                                        Color(0xFFE0E7FF), // Chic Indigo-Blue
                                        Color(0xFFF1E1FF), // Soft lavender glow
                                        Color(0xFFFFE3EC)  // Warm rose-pink highlight
                                    )
                                }
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isDark) {
                                Color(0xFF4F378B).copy(alpha = 0.4f)
                            } else {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            },
                            shape = RoundedCornerShape(24.dp)
                        ),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text(
                                    "TOTAL SALDO",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                    letterSpacing = 1.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = currencyFormat.format(balance),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Income
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(if (isDark) IncomeBgDark else IncomeBgLight),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.ArrowDownward,
                                            contentDescription = null,
                                            tint = if (isDark) IncomeColorDark else IncomeColorLight,
                                            modifier = Modifier.size(11.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Uang Masuk",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = currencyFormat.format(totalIncome),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                            // Expense
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(if (isDark) ExpenseBgDark else ExpenseBgLight),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.ArrowUpward,
                                            contentDescription = null,
                                            tint = if (isDark) ExpenseColorDark else ExpenseColorLight,
                                            modifier = Modifier.size(11.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Uang Keluar",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = currencyFormat.format(totalExpense),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Row Filter Gabungan
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 1.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Tanggal Filter Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tanggal Filter Chip
                        Box(modifier = Modifier.weight(1.3f)) {
                            FilterChip(
                                selected = selectedDateFilterType != DateFilterType.ALL,
                                onClick = { showDateMenu = true },
                                modifier = Modifier.fillMaxWidth(),
                                label = {
                                    Text(
                                        text = when (selectedDateFilterType) {
                                            DateFilterType.ALL -> "Tanggal: Semua"
                                            DateFilterType.TODAY -> "Hari Ini"
                                            DateFilterType.LAST_7_DAYS -> "7 Hari"
                                            DateFilterType.THIS_MONTH -> "Bulan Ini"
                                            DateFilterType.CUSTOM -> {
                                                if (customStartDate != null && customEndDate != null) {
                                                    val df = SimpleDateFormat("dd/MM", Locale("id", "ID"))
                                                    "${df.format(Date(customStartDate!!))}-${df.format(Date(customEndDate!!))}"
                                                } else {
                                                    "Kustom"
                                                }
                                            }
                                        },
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                            DropdownMenu(
                                expanded = showDateMenu,
                                onDismissRequest = { showDateMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Semua Tanggal") },
                                    onClick = {
                                        selectedDateFilterType = DateFilterType.ALL
                                        showDateMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Hari Ini") },
                                    onClick = {
                                        selectedDateFilterType = DateFilterType.TODAY
                                        showDateMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("7 Hari Terakhir") },
                                    onClick = {
                                        selectedDateFilterType = DateFilterType.LAST_7_DAYS
                                        showDateMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Bulan Ini") },
                                    onClick = {
                                        selectedDateFilterType = DateFilterType.THIS_MONTH
                                        showDateMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Kustom (Pilih Rentang)...") },
                                    onClick = {
                                        showDateMenu = false
                                        tempStartDate = customStartDate ?: System.currentTimeMillis()
                                        tempEndDate = customEndDate ?: System.currentTimeMillis()
                                        showCustomDateDialog = true
                                    }
                                )
                            }
                        }

                        // Button Sekali Klik "Hari Ini"
                        val isTodaySelected = selectedDateFilterType == DateFilterType.TODAY
                        Box(modifier = Modifier.weight(1f)) {
                            FilterChip(
                                selected = isTodaySelected,
                                onClick = {
                                    selectedDateFilterType = if (isTodaySelected) DateFilterType.ALL else DateFilterType.TODAY
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = {
                                    Text(
                                        "Hari Ini",
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Today,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }

                        // Tombol Reset Tanggal
                        Box(modifier = Modifier.weight(0.9f)) {
                            AssistChip(
                                onClick = {
                                    selectedDateFilterType = DateFilterType.ALL
                                    customStartDate = null
                                    customEndDate = null
                                },
                                enabled = selectedDateFilterType != DateFilterType.ALL,
                                modifier = Modifier.fillMaxWidth(),
                                label = {
                                    Text(
                                        "Reset",
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Reset",
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (selectedDateFilterType != DateFilterType.ALL) MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                )
                            )
                        }
                    }

                    // Tipe & Kategori Filter Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tipe Filter Chip
                        Box(modifier = Modifier.weight(1f)) {
                            FilterChip(
                                selected = selectedTypeFilter != null,
                                onClick = { showTypeMenu = true },
                                modifier = Modifier.fillMaxWidth(),
                                label = {
                                    Text(
                                        text = when (selectedTypeFilter) {
                                            TransactionType.INCOME -> "Masuk"
                                            TransactionType.EXPENSE -> "Keluar"
                                            else -> "Tipe: Semua"
                                        },
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                            DropdownMenu(
                                expanded = showTypeMenu,
                                onDismissRequest = { showTypeMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Semua Tipe") },
                                    onClick = {
                                        selectedTypeFilter = null
                                        showTypeMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Masuk (Income)") },
                                    onClick = {
                                        selectedTypeFilter = TransactionType.INCOME
                                        showTypeMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Keluar (Expense)") },
                                    onClick = {
                                        selectedTypeFilter = TransactionType.EXPENSE
                                        showTypeMenu = false
                                    }
                                )
                            }
                        }

                        // Kategori Filter Chip
                        Box(modifier = Modifier.weight(1f)) {
                            FilterChip(
                                selected = selectedCategoryFilter != "Semua",
                                onClick = { showCategoryMenu = true },
                                modifier = Modifier.fillMaxWidth(),
                                label = {
                                    Text(
                                        text = if (selectedCategoryFilter == "Semua") "Kategori: Semua" else selectedCategoryFilter,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                            DropdownMenu(
                                expanded = showCategoryMenu,
                                onDismissRequest = { showCategoryMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Semua Kategori") },
                                    onClick = {
                                        selectedCategoryFilter = "Semua"
                                        showCategoryMenu = false
                                    }
                                )
                                availableCategories.forEach { category ->
                                    DropdownMenuItem(
                                        text = { Text(category) },
                                        onClick = {
                                            selectedCategoryFilter = category
                                            showCategoryMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Ringkasan Filter Aktif (Kotak Baru / Sementara)
            if (selectedTypeFilter != null || selectedCategoryFilter != "Semua" || selectedDateFilterType != DateFilterType.ALL) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 2.dp)
                            .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.FilterAlt,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "RINGKASAN FILTER AKTIF",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                
                                Text(
                                    "Hapus Filter",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable {
                                            selectedTypeFilter = null
                                            selectedCategoryFilter = "Semua"
                                            selectedDateFilterType = DateFilterType.ALL
                                            customStartDate = null
                                            customEndDate = null
                                        }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Filtered Income
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Masuk",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = currencyFormat.format(filteredIncome),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isDark) IncomeColorDark else IncomeColorLight
                                    )
                                }
                                
                                // Filtered Expense
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Keluar",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = currencyFormat.format(filteredExpense),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isDark) ExpenseColorDark else ExpenseColorLight
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            androidx.compose.material3.HorizontalDivider(
                                modifier = Modifier.fillMaxWidth(),
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Selisih",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = currencyFormat.format(filteredBalance),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Transaksi Terakhir",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Lihat Semua",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable {
                                selectedTypeFilter = null
                                selectedCategoryFilter = "Semua"
                                selectedDateFilterType = DateFilterType.ALL
                                customStartDate = null
                                customEndDate = null
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Transactions Data Items
            if (transactions.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp, horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Belum ada transaksi.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Mulai catat keuangan Anda dengan tombol + atau buat data simulasi untuk melihat visualisasi laporan!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { viewModel.insertSampleBook() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            ),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Buat Sample Buku (20 Transaksi)",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            } else {
                if (filteredTransactions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Tidak ada transaksi yang sesuai filter.")
                        }
                    }
                } else {
                    items(
                        items = filteredTransactions,
                        key = { it.id }
                    ) { transaction ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            TransactionItemWithSwipe(
                                transaction = transaction,
                                transactionToDelete = transactionToDelete,
                                onDelete = { transactionToDelete = transaction },
                                onEdit = { onNavigateToAddEdit(transaction.id) },
                                currencyFormat = currencyFormat,
                                isDark = isDark
                            )
                        }
                    }
                }
            }
            }

            AnimatedVisibility(
                visible = showCompactHeader,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                CompactHeader(
                    balance = balance,
                    totalIncome = totalIncome,
                    totalExpense = totalExpense,
                    isDark = isDark,
                    currencyFormat = currencyFormat
                )
            }
        }
    }

    if (transactionToDelete != null) {
        val t = transactionToDelete!!
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("Hapus Transaksi") },
            text = { Text("Apakah Anda yakin ingin menghapus transaksi ini?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTransaction(t)
                        transactionToDelete = null
                        coroutineScope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Transaksi berhasil dihapus",
                                actionLabel = "Urungkan",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.undoDelete()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { transactionToDelete = null }
                ) {
                    Text("Batal")
                }
            }
        )
    }

    if (showBookManager) {
        AlertDialog(
            onDismissRequest = { showBookManager = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Kelola Buku Keuangan", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    IconButton(
                        onClick = { showAddBookDialog = true }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Tambah Buku Baru", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Button to generate a sample book with 20 dummy transactions
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clickable {
                                viewModel.insertSampleBook()
                                showBookManager = false
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Buat Sample Buku (20 Transaksi)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }

                    allBooks.forEach { book ->
                        val isSelected = book.id == activeBook?.id
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else if (book.isDefault) {
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                }
                            ),
                            border = if (book.isDefault && !isSelected) {
                                androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f))
                            } else {
                                null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    viewModel.selectBook(book)
                                    showBookManager = false
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = book.name,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (!book.isDefault) {
                                        IconButton(
                                            onClick = { viewModel.makeBookDefault(book.id) }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.StarBorder,
                                                contentDescription = "Jadikan Default",
                                                tint = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Buku Default",
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                    
                                    IconButton(
                                        onClick = { showEditBookDialog = book }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Ubah Nama",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    if (allBooks.size > 1) {
                                        IconButton(
                                            onClick = { showDeleteBookConfirm = book }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Hapus Buku",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBookManager = false }) {
                    Text("Tutup")
                }
            }
        )
    }

    if (showAddBookDialog) {
        var bookNameInput by remember { mutableStateOf("") }
        var isDefaultInput by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showAddBookDialog = false },
            title = { Text("Tambah Buku Baru", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = bookNameInput,
                        onValueChange = { bookNameInput = it },
                        label = { Text("Nama Buku") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { isDefaultInput = !isDefaultInput }
                    ) {
                        Checkbox(
                            checked = isDefaultInput,
                            onCheckedChange = { isDefaultInput = it }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Jadikan sebagai Buku Default")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (bookNameInput.isNotBlank()) {
                            viewModel.insertBook(bookNameInput.trim(), isDefaultInput)
                            showAddBookDialog = false
                        }
                    }
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBookDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    if (showEditBookDialog != null) {
        val targetBook = showEditBookDialog!!
        var bookNameInput by remember { mutableStateOf(targetBook.name) }
        AlertDialog(
            onDismissRequest = { showEditBookDialog = null },
            title = { Text("Ubah Nama Buku", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = bookNameInput,
                        onValueChange = { bookNameInput = it },
                        label = { Text("Nama Buku") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (bookNameInput.isNotBlank()) {
                            viewModel.updateBook(targetBook.copy(name = bookNameInput.trim()))
                            showEditBookDialog = null
                        }
                    }
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditBookDialog = null }) {
                    Text("Batal")
                }
            }
        )
    }

    if (showDeleteBookConfirm != null) {
        val targetBook = showDeleteBookConfirm!!
        AlertDialog(
            onDismissRequest = { showDeleteBookConfirm = null },
            title = { Text("Hapus Buku Keuangan", fontWeight = FontWeight.Bold) },
            text = {
                Text("Apakah Anda yakin ingin menghapus buku \"${targetBook.name}\"? Semua data transaksi di dalam buku ini akan dihapus secara permanen.")
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        viewModel.deleteBook(targetBook)
                        showDeleteBookConfirm = null
                    }
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteBookConfirm = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Native DatePickerDialog for custom start date
    val startCalendar = Calendar.getInstance().apply { 
        timeInMillis = tempStartDate ?: System.currentTimeMillis() 
    }
    val startDatePickerDialog = remember(tempStartDate) {
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                tempStartDate = cal.timeInMillis
            },
            startCalendar.get(Calendar.YEAR),
            startCalendar.get(Calendar.MONTH),
            startCalendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    // Native DatePickerDialog for custom end date
    val endCalendar = Calendar.getInstance().apply { 
        timeInMillis = tempEndDate ?: System.currentTimeMillis() 
    }
    val endDatePickerDialog = remember(tempEndDate) {
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 23, 59, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                tempEndDate = cal.timeInMillis
            },
            endCalendar.get(Calendar.YEAR),
            endCalendar.get(Calendar.MONTH),
            endCalendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    if (showCustomDateDialog) {
        val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")) }
        AlertDialog(
            onDismissRequest = { showCustomDateDialog = false },
            title = { Text("Pilih Rentang Tanggal", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Start Date picker field
                    Column {
                        Text(
                            "Tanggal Mulai",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedCard(
                            onClick = { startDatePickerDialog.show() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tempStartDate?.let { dateFormat.format(Date(it)) } ?: "Pilih tanggal...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (tempStartDate != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // End Date picker field
                    Column {
                        Text(
                            "Tanggal Selesai",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedCard(
                            onClick = { endDatePickerDialog.show() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tempEndDate?.let { dateFormat.format(Date(it)) } ?: "Pilih tanggal...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (tempEndDate != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempStartDate != null && tempEndDate != null) {
                            customStartDate = tempStartDate
                            customEndDate = tempEndDate
                            selectedDateFilterType = DateFilterType.CUSTOM
                            showCustomDateDialog = false
                        }
                    },
                    enabled = tempStartDate != null && tempEndDate != null
                ) {
                    Text("Terapkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDateDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    if (showInfoDialog) {
        val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text("Tentang Uang Siapa?", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Aplikasi pencatatan keuangan pribadi yang simpel, aman, dan 100% offline.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    
                    // GitHub Link Section
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = "GitHub",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Kode Sumber (GitHub)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "github.com/dotslashgabut/uangsiapa",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    textDecoration = TextDecoration.Underline,
                                    color = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.clickable {
                                    try {
                                        uriHandler.openUri("https://github.com/dotslashgabut/uangsiapa")
                                    } catch (e: Exception) {
                                        // gracefully handled
                                    }
                                }
                            )
                        }
                    }
                    
                    // Saweria Support Section
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalCafe,
                            contentDescription = "Saweria",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Dukungan Saweria",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "saweria.co/dotslashgabut",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    textDecoration = TextDecoration.Underline,
                                    color = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.clickable {
                                    try {
                                        uriHandler.openUri("https://saweria.co/dotslashgabut")
                                    } catch (e: Exception) {
                                        // gracefully handled
                                    }
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("Tutup", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionItemWithSwipe(
    transaction: Transaction,
    transactionToDelete: Transaction?,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    currencyFormat: NumberFormat,
    isDark: Boolean
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    false
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    onEdit()
                    false
                }
                else -> false
            }
        },
        positionalThreshold = { distance -> distance * 0.5f }
    )

    BoxWithConstraints {
        val density = LocalDensity.current
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val maxAllowedOffset = maxWidthPx * 0.66f

        SwipeToDismissBox(
            state = dismissState,
            backgroundContent = {
                val direction = dismissState.dismissDirection
                val color = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Color(0xFF2196F3) // Edit
                    SwipeToDismissBoxValue.EndToStart -> Color(0xFFD32F2F) // More vibrant red for Delete
                    else -> Color.Transparent
                }
                val alignment = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                    else -> Alignment.Center
                }
                val icon = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Edit
                    SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                    else -> Icons.Default.Delete
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(color)
                        .padding(horizontal = 20.dp),
                    contentAlignment = alignment
                ) {
                    if (direction != SwipeToDismissBoxValue.Settled) {
                        Icon(icon, contentDescription = null, tint = Color.White)
                    }
                }
            },
            content = {
                val offset = try { dismissState.requireOffset() } catch (e: Exception) { 0f }
                val counterOffset = when {
                    offset > maxAllowedOffset -> -(offset - maxAllowedOffset)
                    offset < -maxAllowedOffset -> -(offset + maxAllowedOffset)
                    else -> 0f
                }

                Box(
                    modifier = Modifier.offset { IntOffset(counterOffset.roundToInt(), 0) }
                ) {
                    TransactionCard(
                        transaction = transaction, 
                        currencyFormat = currencyFormat, 
                        isDark = isDark,
                        onEdit = onEdit,
                        onDelete = onDelete
                    )
                }
            }
        )
    }
}

@Composable
fun TransactionCard(
    transaction: Transaction, 
    currencyFormat: NumberFormat, 
    isDark: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
    val isIncome = transaction.type == TransactionType.INCOME
    val amountColor = if (isIncome) {
        if (isDark) IncomeColorDark else IncomeColorLight
    } else {
        if (isDark) ExpenseColorDark else ExpenseColorLight
    }
    
    val containerColor = if (isDark) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    val borderColor = if (isDark) {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
    }
    
    val (categoryIcon, iconBgColor) = getCategoryIconAndColor(transaction.category, isIncome, isDark)
    
    val iconTint = if (isIncome) {
        if (isDark) IncomeColorDark else IncomeColorLight
    } else {
        if (isDark) ExpenseColorDark else ExpenseColorLight
    }

    var showItemMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onEdit() },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.category, 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (transaction.description.isNotBlank()) transaction.description else (if (isIncome) "Pendapatan" else "Pengeluaran"), 
                    fontSize = 12.sp, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    text = dateFormat.format(Date(transaction.dateMillis)), 
                    fontSize = 10.sp, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            
            Surface(
                color = iconBgColor.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "${if (isIncome) "+" else "-"}${currencyFormat.format(transaction.amount)}",
                    color = amountColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Box {
                IconButton(
                    onClick = { showItemMenu = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Opsi Transaksi",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
                DropdownMenu(
                    expanded = showItemMenu,
                    onDismissRequest = { showItemMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit Transaksi", fontSize = 13.sp) },
                        leadingIcon = { 
                            Icon(
                                Icons.Default.Edit, 
                                contentDescription = null, 
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            ) 
                        },
                        onClick = {
                            showItemMenu = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Hapus Transaksi", fontSize = 13.sp, color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { 
                            Icon(
                                Icons.Default.Delete, 
                                contentDescription = null, 
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            ) 
                        },
                        onClick = {
                            showItemMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun getCategoryIconAndColor(category: String, isIncome: Boolean, isDark: Boolean): Pair<androidx.compose.ui.graphics.vector.ImageVector, Color> {
    val cat = category.trim().lowercase(Locale.ROOT)
    val icon = when {
        cat.contains("makanan") || cat.contains("minuman") || cat.contains("makan") || cat.contains("kopi") || cat.contains("kuliner") -> Icons.Default.LocalCafe
        cat.contains("belanja") || cat.contains("mall") || cat.contains("pasar") || cat.contains("toko") -> Icons.Default.Receipt
        cat.contains("transport") || cat.contains("bensin") || cat.contains("ojek") || cat.contains("parkir") -> Icons.Default.Star
        cat.contains("gaji") || cat.contains("payday") || cat.contains("salary") -> Icons.Default.Paid
        cat.contains("tagihan") || cat.contains("listrik") || cat.contains("air") || cat.contains("wifi") || cat.contains("pulsa") -> Icons.Default.Receipt
        cat.contains("investasi") || cat.contains("saham") || cat.contains("reksa") || cat.contains("crypto") -> Icons.Default.Assessment
        cat.contains("kesehatan") || cat.contains("obat") || cat.contains("dokter") || cat.contains("rumah sakit") -> Icons.Default.Info
        cat.contains("hiburan") || cat.contains("game") || cat.contains("nonton") || cat.contains("film") -> Icons.Default.Paid
        cat.contains("pendidikan") || cat.contains("kursus") || cat.contains("buku") || cat.contains("sekolah") -> Icons.Default.MenuBook
        cat.contains("hadiah") || cat.contains("kado") -> Icons.Default.Star
        cat.contains("sedekah") || cat.contains("donasi") || cat.contains("zakat") -> Icons.Default.Star
        isIncome -> Icons.Default.ArrowDownward
        else -> Icons.Default.ArrowUpward
    }
    val bgColor = if (isIncome) {
        if (isDark) IncomeBgDark else IncomeBgLight
    } else {
        if (isDark) ExpenseBgDark else ExpenseBgLight
    }
    return Pair(icon, bgColor)
}

@Composable
fun CompactHeader(
    balance: Double,
    totalIncome: Double,
    totalExpense: Double,
    isDark: Boolean,
    currencyFormat: NumberFormat
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        color = MaterialTheme.colorScheme.background,
        shadowElevation = 4.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Saldo
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "TOTAL SALDO",
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = currencyFormat.format(balance),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Masuk
                Column(horizontalAlignment = Alignment.Start) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(if (isDark) IncomeBgDark else IncomeBgLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = if (isDark) IncomeColorDark else IncomeColorLight,
                                modifier = Modifier.size(8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Uang Masuk",
                            fontSize = 8.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = currencyFormat.format(totalIncome),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (isDark) IncomeColorDark else IncomeColorLight
                    )
                }

                // Keluar
                Column(horizontalAlignment = Alignment.Start) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(if (isDark) ExpenseBgDark else ExpenseBgLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = if (isDark) ExpenseColorDark else ExpenseColorLight,
                                modifier = Modifier.size(8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Uang Keluar",
                            fontSize = 8.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = currencyFormat.format(totalExpense),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (isDark) ExpenseColorDark else ExpenseColorLight
                    )
                }
            }
            
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                thickness = 1.dp
            )
        }
    }
}

enum class DateFilterType {
    ALL, TODAY, LAST_7_DAYS, THIS_MONTH, CUSTOM
}

private fun startOfDay(millis: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

private fun endOfDay(millis: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }
    return cal.timeInMillis
}

private fun isSameDay(m1: Long, m2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = m1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = m2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun isSameMonthAndYear(m1: Long, m2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = m1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = m2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
}

