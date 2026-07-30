package com.example.ui.screens

import kotlinx.coroutines.launch
import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Clear
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.TransactionType
import com.example.ui.theme.*
import com.example.ui.viewmodel.AddEditViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditScreen(
    viewModel: AddEditViewModel,
    transactionId: Int?,
    activeBookId: Int = 1,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
    val isDark = MaterialTheme.colorScheme.background == DarkBackground

    LaunchedEffect(transactionId, activeBookId) {
        viewModel.loadTransaction(transactionId, activeBookId)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (transactionId == null) "Tambah Transaksi" else "Edit Transaksi", 
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    ) 
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .padding(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Kembali",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(scrollState)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                    })
                },
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Fluent Type Switcher (Income vs Expense)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val isIncome = uiState.type == TransactionType.INCOME
                
                // Income Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            color = if (isIncome) {
                                if (isDark) IncomeBgDark else IncomeBgLight
                            } else Color.Transparent
                        )
                        .clickable { viewModel.updateType(TransactionType.INCOME) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = if (isIncome) (if (isDark) IncomeColorDark else IncomeColorLight) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Uang Masuk",
                            fontWeight = if (isIncome) FontWeight.Bold else FontWeight.Medium,
                            color = if (isIncome) (if (isDark) IncomeColorDark else IncomeColorLight) else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }

                // Expense Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            color = if (!isIncome) {
                                if (isDark) ExpenseBgDark else ExpenseBgLight
                            } else Color.Transparent
                        )
                        .clickable { viewModel.updateType(TransactionType.EXPENSE) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = if (!isIncome) (if (isDark) ExpenseColorDark else ExpenseColorLight) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Uang Keluar",
                            fontWeight = if (!isIncome) FontWeight.Bold else FontWeight.Medium,
                            color = if (!isIncome) (if (isDark) ExpenseColorDark else ExpenseColorLight) else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Acrylic Hero Amount Input Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (uiState.type == TransactionType.INCOME) {
                            (if (isDark) IncomeColorDark else IncomeColorLight).copy(alpha = 0.3f)
                        } else {
                            (if (isDark) ExpenseColorDark else ExpenseColorLight).copy(alpha = 0.3f)
                        },
                        shape = RoundedCornerShape(24.dp)
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.type == TransactionType.INCOME) {
                        (if (isDark) IncomeBgDark else IncomeBgLight).copy(alpha = 0.3f)
                    } else {
                        (if (isDark) ExpenseBgDark else ExpenseBgLight).copy(alpha = 0.3f)
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "NOMINAL TRANSAKSI",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.amountStr,
                        onValueChange = { viewModel.updateAmount(it) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        isError = !uiState.isAmountValid,
                        prefix = { 
                            Text(
                                "Rp ", 
                                fontWeight = FontWeight.Bold, 
                                fontSize = 24.sp,
                                color = if (uiState.type == TransactionType.INCOME) {
                                    if (isDark) IncomeColorDark else IncomeColorLight
                                } else {
                                    if (isDark) ExpenseColorDark else ExpenseColorLight
                                }
                            ) 
                        },
                        trailingIcon = if (uiState.amountStr.isNotEmpty()) {
                            {
                                IconButton(onClick = { viewModel.updateAmount("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Hapus nominal",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        } else null,
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 24.sp, 
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        visualTransformation = RupiahVisualTransformation(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                        )
                    )
                }
            }

            // Category Picker & Chips
            val predefinedCategories = if (uiState.type == TransactionType.INCOME) {
                listOf("Gaji", "Bonus", "Investasi", "Penjualan", "Hadiah", "Sampingan", "Utang", "Saldo Awal", "Celengan", "Pindah Kas", "Pemasukan Lain")
            } else {
                listOf("Makanan & Minuman", "Belanja", "Transportasi", "Tagihan & Utilitas", "Hiburan", "Kesehatan", "Pendidikan", "Bayar Utang", "Pindah Kas", "Sedekah", "Pengeluaran Lain")
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Pilih Kategori Cepat",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    predefinedCategories.forEach { catName ->
                        val isSelected = uiState.category.equals(catName, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.updateCategory(catName) },
                            label = { Text(catName, fontSize = 13.sp) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                var showCategoryDropdown by remember { mutableStateOf(false) }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = uiState.category,
                        onValueChange = { viewModel.updateCategory(it) },
                        label = { Text("Kategori Custom / Terpilih") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Misal: Makanan, Gaji, dll.") },
                        leadingIcon = {
                            Icon(Icons.Default.Label, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (uiState.category.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.updateCategory("") }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Hapus kategori",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                IconButton(onClick = { showCategoryDropdown = !showCategoryDropdown }) {
                                    Icon(
                                        imageVector = if (showCategoryDropdown) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = "Pilih Kategori"
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(16.dp)
                    )
                    DropdownMenu(
                        expanded = showCategoryDropdown,
                        onDismissRequest = { showCategoryDropdown = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        predefinedCategories.forEach { categoryName ->
                            DropdownMenuItem(
                                text = { Text(categoryName) },
                                onClick = {
                                    viewModel.updateCategory(categoryName)
                                    showCategoryDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            // Description Input
            OutlinedTextField(
                value = uiState.description,
                onValueChange = { viewModel.updateDescription(it) },
                label = { Text("Keterangan / Catatan") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Tambahkan catatan opsional...") },
                leadingIcon = {
                    Icon(Icons.Default.EditNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                trailingIcon = if (uiState.description.isNotEmpty()) {
                    {
                        IconButton(onClick = { viewModel.updateDescription("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Hapus keterangan",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else null,
                minLines = 2,
                shape = RoundedCornerShape(16.dp)
            )

            // Date Picker Card
            val calendar = Calendar.getInstance().apply { timeInMillis = uiState.dateMillis }
            val datePickerDialog = DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    val newCalendar = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth)
                    }
                    viewModel.updateDate(newCalendar.timeInMillis)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )

            OutlinedCard(
                onClick = { datePickerDialog.show() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                "Tanggal Transaksi",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                dateFormat.format(Date(uiState.dateMillis)),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Text(
                        "Ubah",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Primary Save Action Button
            Button(
                onClick = {
                    if (viewModel.saveTransaction()) {
                        onNavigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = uiState.amountStr.isNotBlank(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Simpan Transaksi", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

class RupiahVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val original = text.text
        if (original.isEmpty()) {
            return TransformedText(AnnotatedString(""), OffsetMapping.Identity)
        }

        val formatted = formatDigitsWithDots(original)

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                if (offset > original.length) return formatted.length

                var originalIdx = 0
                var formattedIdx = 0

                while (originalIdx < offset && formattedIdx < formatted.length) {
                    if (formatted[formattedIdx] == '.') {
                        formattedIdx++
                    } else {
                        originalIdx++
                        formattedIdx++
                    }
                }
                return formattedIdx
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                if (offset > formatted.length) return original.length

                var originalIdx = 0
                var formattedIdx = 0

                while (originalIdx < original.length && formattedIdx < offset) {
                    if (formatted[formattedIdx] == '.') {
                        formattedIdx++
                    } else {
                        originalIdx++
                        formattedIdx++
                    }
                }
                return originalIdx
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }

    private fun formatDigitsWithDots(digits: String): String {
        val sb = StringBuilder()
        val len = digits.length
        for (i in 0 until len) {
            sb.append(digits[i])
            val digitsRemaining = len - 1 - i
            if (digitsRemaining > 0 && digitsRemaining % 3 == 0) {
                sb.append('.')
            }
        }
        return sb.toString()
    }
}

