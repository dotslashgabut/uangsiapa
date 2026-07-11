package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.TransactionType
import com.example.ui.viewmodel.AddEditViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditScreen(
    viewModel: AddEditViewModel,
    transactionId: Int?,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))

    LaunchedEffect(transactionId) {
        viewModel.loadTransaction(transactionId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (transactionId == null) "Tambah Transaksi" else "Edit Transaksi", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Type Selector (Income / Expense)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = uiState.type == TransactionType.INCOME,
                    onClick = { viewModel.updateType(TransactionType.INCOME) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("Uang Masuk")
                }
                SegmentedButton(
                    selected = uiState.type == TransactionType.EXPENSE,
                    onClick = { viewModel.updateType(TransactionType.EXPENSE) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("Uang Keluar")
                }
            }

            OutlinedTextField(
                value = uiState.amountStr,
                onValueChange = { viewModel.updateAmount(it) },
                label = { Text("Nominal") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                isError = !uiState.isAmountValid,
                prefix = { Text("Rp ") },
                visualTransformation = RupiahVisualTransformation()
            )

            OutlinedTextField(
                value = uiState.category,
                onValueChange = { viewModel.updateCategory(it) },
                label = { Text("Kategori") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Misal: Makanan, Gaji, dll.") }
            )

            OutlinedTextField(
                value = uiState.description,
                onValueChange = { viewModel.updateDescription(it) },
                label = { Text("Keterangan") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

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

            OutlinedTextField(
                value = dateFormat.format(Date(uiState.dateMillis)),
                onValueChange = { },
                label = { Text("Tanggal") },
                enabled = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { datePickerDialog.show() },
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (viewModel.saveTransaction()) {
                        onNavigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.amountStr.isNotBlank()
            ) {
                Text("Simpan", fontWeight = FontWeight.Bold)
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
