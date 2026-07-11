package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Transaction
import com.example.data.TransactionRepository
import com.example.data.TransactionType
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

enum class ReportMode { MONTHLY, YEARLY }

data class ReportUiState(
    val transactions: List<Transaction> = emptyList(),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val currentMonth: Int = 0,
    val currentYear: Int = 0,
    val reportMode: ReportMode = ReportMode.MONTHLY
)

class ReportViewModel(private val repository: TransactionRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()
    
    private var loadJob: Job? = null
    private var activeBookId: Int = 1

    fun setBookId(bookId: Int) {
        activeBookId = bookId
        loadData()
    }

    init {
        val calendar = Calendar.getInstance()
        _uiState.update {
            it.copy(
                currentMonth = calendar.get(Calendar.MONTH),
                currentYear = calendar.get(Calendar.YEAR)
            )
        }
        loadData()
    }

    fun setMonthYear(month: Int, year: Int) {
        _uiState.update {
            it.copy(
                currentMonth = month,
                currentYear = year
            )
        }
        loadData()
    }

    fun setReportMode(mode: ReportMode) {
        _uiState.update { it.copy(reportMode = mode) }
        loadData()
    }

    private fun loadData() {
        loadJob?.cancel()
        val month = _uiState.value.currentMonth
        val year = _uiState.value.currentYear
        val mode = _uiState.value.reportMode
        
        val calendar = Calendar.getInstance()
        val startMillis: Long
        val endMillis: Long
        
        if (mode == ReportMode.MONTHLY) {
            calendar.set(year, month, 1, 0, 0, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            startMillis = calendar.timeInMillis
            
            calendar.add(Calendar.MONTH, 1)
            calendar.add(Calendar.MILLISECOND, -1)
            endMillis = calendar.timeInMillis
        } else {
            calendar.set(year, 0, 1, 0, 0, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            startMillis = calendar.timeInMillis
            
            calendar.add(Calendar.YEAR, 1)
            calendar.add(Calendar.MILLISECOND, -1)
            endMillis = calendar.timeInMillis
        }

        loadJob = viewModelScope.launch {
            repository.getTransactionsBetween(activeBookId, startMillis, endMillis).collectLatest { transactions ->
                val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                _uiState.update { 
                    it.copy(
                        transactions = transactions,
                        totalIncome = totalIncome,
                        totalExpense = totalExpense
                    )
                }
            }
        }
    }
    
    fun nextMonth() {
        var m = _uiState.value.currentMonth + 1
        var y = _uiState.value.currentYear
        if (m > 11) { m = 0; y++ }
        setMonthYear(m, y)
    }

    fun prevMonth() {
        var m = _uiState.value.currentMonth - 1
        var y = _uiState.value.currentYear
        if (m < 0) { m = 11; y-- }
        setMonthYear(m, y)
    }

    fun nextYear() {
        setMonthYear(_uiState.value.currentMonth, _uiState.value.currentYear + 1)
    }

    fun prevYear() {
        setMonthYear(_uiState.value.currentMonth, _uiState.value.currentYear - 1)
    }
}

class ReportViewModelFactory(private val repository: TransactionRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReportViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReportViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
