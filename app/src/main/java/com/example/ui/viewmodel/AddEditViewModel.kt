package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Transaction
import com.example.data.TransactionRepository
import com.example.data.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

data class AddEditUiState(
    val id: Int = 0,
    val type: TransactionType = TransactionType.EXPENSE,
    val amountStr: String = "",
    val category: String = "",
    val description: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val isAmountValid: Boolean = true,
    val bookId: Int = 1
)

class AddEditViewModel(private val repository: TransactionRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(AddEditUiState())
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()

    fun loadTransaction(id: Int?, activeBookId: Int) {
        if (id == null || id == 0) {
            _uiState.value = AddEditUiState(bookId = activeBookId)
            return
        }
        viewModelScope.launch {
            val transaction = repository.getTransactionById(id)
            if (transaction != null) {
                // Remove decimals if it's whole number to keep input clean
                val rawAmountStr = if (transaction.amount % 1.0 == 0.0) {
                    transaction.amount.toLong().toString()
                } else {
                    transaction.amount.toString()
                }
                val digitsOnly = rawAmountStr.filter { it.isDigit() }
                _uiState.value = AddEditUiState(
                    id = transaction.id,
                    type = transaction.type,
                    amountStr = digitsOnly,
                    category = transaction.category,
                    description = transaction.description,
                    dateMillis = transaction.dateMillis,
                    bookId = transaction.bookId
                )
            }
        }
    }

    fun updateType(type: TransactionType) {
        _uiState.update { it.copy(type = type) }
    }

    fun updateAmount(amount: String) {
        val digitsOnly = amount.filter { it.isDigit() }
        val cleanAmount = digitsOnly.toDoubleOrNull()
        _uiState.update { 
            it.copy(
                amountStr = digitsOnly, 
                isAmountValid = cleanAmount != null || digitsOnly.isEmpty()
            ) 
        }
    }

    fun updateCategory(category: String) {
        _uiState.update { it.copy(category = category) }
    }

    fun updateDescription(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun updateDate(dateMillis: Long) {
        _uiState.update { it.copy(dateMillis = dateMillis) }
    }

    fun saveTransaction(): Boolean {
        val currentState = _uiState.value
        val cleanAmountStr = currentState.amountStr.replace(".", "")
        val amount = cleanAmountStr.toDoubleOrNull()
        if (amount == null) {
            _uiState.update { it.copy(isAmountValid = false) }
            return false
        }
        
        // Auto-fill category if blank
        val resolvedCategory = if (currentState.category.isBlank()) {
            if (currentState.type == TransactionType.INCOME) "Pemasukan" else "Pengeluaran"
        } else {
            currentState.category
        }

        // Auto-fill description if blank
        val resolvedDescription = if (currentState.description.isBlank()) {
            if (currentState.type == TransactionType.INCOME) "Pemasukan" else "Pengeluaran"
        } else {
            currentState.description
        }

        val transaction = Transaction(
            id = currentState.id,
            type = currentState.type,
            amount = amount,
            category = resolvedCategory,
            description = resolvedDescription,
            dateMillis = currentState.dateMillis,
            bookId = currentState.bookId
        )

        viewModelScope.launch {
            if (transaction.id == 0) {
                repository.insert(transaction)
            } else {
                repository.update(transaction)
            }
        }
        return true
    }
}

class AddEditViewModelFactory(private val repository: TransactionRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddEditViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddEditViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
