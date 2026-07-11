package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Transaction
import com.example.data.TransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(private val repository: TransactionRepository) : ViewModel() {

    val allTransactions: StateFlow<List<Transaction>> = repository.allTransactions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private var lastDeletedTransaction: Transaction? = null

    fun deleteTransaction(transaction: Transaction) {
        lastDeletedTransaction = transaction
        viewModelScope.launch {
            repository.delete(transaction)
        }
    }

    fun undoDelete() {
        val lastDeleted = lastDeletedTransaction
        if (lastDeleted != null) {
            viewModelScope.launch {
                repository.insert(lastDeleted)
                lastDeletedTransaction = null
            }
        }
    }

    fun importBackup(transactions: List<Transaction>) {
        viewModelScope.launch {
            for (t in transactions) {
                repository.insert(t)
            }
        }
    }
}

class MainViewModelFactory(private val repository: TransactionRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
