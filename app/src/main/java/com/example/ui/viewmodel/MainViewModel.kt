package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Book
import com.example.data.Transaction
import com.example.data.TransactionRepository
import com.example.data.TransactionType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(private val repository: TransactionRepository) : ViewModel() {

    private val _activeBook = MutableStateFlow<Book?>(null)
    val activeBook: StateFlow<Book?> = _activeBook.asStateFlow()

    val allBooks: StateFlow<List<Book>> = repository.allBooks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allTransactions: StateFlow<List<Transaction>> = _activeBook
        .filterNotNull()
        .flatMapLatest { book ->
            repository.getAllTransactions(book.id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var lastDeletedTransaction: Transaction? = null

    init {
        viewModelScope.launch {
            val defaultBook = repository.getDefaultBook()
            _activeBook.value = defaultBook
        }
    }

    private suspend fun getUniqueBookName(baseName: String, excludeId: Int? = null): String {
        val trimmed = baseName.trim()
        val nameToUse = if (trimmed.isEmpty()) "Buku Baru" else trimmed
        var currentCandidate = nameToUse
        var counter = 1
        while (true) {
            val existing = repository.getBookByName(currentCandidate)
            if (existing == null || (excludeId != null && existing.id == excludeId)) {
                return currentCandidate
            }
            currentCandidate = "$nameToUse ($counter)"
            counter++
        }
    }

    fun selectBook(book: Book) {
        _activeBook.value = book
    }

    fun insertBook(name: String, isDefault: Boolean = false) {
        viewModelScope.launch {
            val uniqueName = getUniqueBookName(name)
            val newId = repository.insertBook(Book(name = uniqueName, isDefault = isDefault))
            val newBook = repository.getBookById(newId)
            if (newBook != null) {
                if (isDefault) {
                    _activeBook.value = newBook
                } else if (_activeBook.value == null) {
                    _activeBook.value = newBook
                }
            }
        }
    }

    fun updateBook(book: Book) {
        viewModelScope.launch {
            val uniqueName = getUniqueBookName(book.name, excludeId = book.id)
            val updatedBook = book.copy(name = uniqueName)
            repository.updateBook(updatedBook)
            if (_activeBook.value?.id == book.id) {
                _activeBook.value = updatedBook
            }
        }
    }

    fun deleteBook(book: Book) {
        viewModelScope.launch {
            repository.deleteBook(book)
            if (_activeBook.value?.id == book.id) {
                val nextBook = repository.getDefaultBook()
                _activeBook.value = nextBook
            }
        }
    }

    fun makeBookDefault(bookId: Int) {
        viewModelScope.launch {
            repository.setDefaultBook(bookId)
            val updatedActive = repository.getBookById(_activeBook.value?.id ?: 0)
            if (updatedActive != null) {
                _activeBook.value = updatedActive
            }
        }
    }

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

    fun importBackup(importedList: List<com.example.utils.ImportedTransaction>) {
        viewModelScope.launch {
            val bookCache = mutableMapOf<String, Int>()
            val activeBook = _activeBook.value
            val defaultBookId = activeBook?.id ?: 1
            val defaultBookName = activeBook?.name ?: "Buku Utama"

            for (item in importedList) {
                val bName = item.bookName.trim()
                val targetBookName = if (bName.isEmpty()) defaultBookName else bName

                val bookId = bookCache.getOrPut(targetBookName) {
                    val uniqueName = getUniqueBookName(targetBookName)
                    repository.insertBook(Book(name = uniqueName, isDefault = false))
                }
                repository.insert(item.transaction.copy(id = 0, bookId = bookId))
            }
        }
    }

    fun insertSampleBook() {
        viewModelScope.launch {
            val uniqueName = getUniqueBookName("Sample Buku")
            // 1. Create the Book named uniqueName
            val newId = repository.insertBook(Book(name = uniqueName, isDefault = false))
            val newBook = repository.getBookById(newId)
            
            if (newBook != null) {
                _activeBook.value = newBook
                
                // 2. Prepare dummy transactions spanning from Jan to Jul 2026 including Utang and Bayar Utang
                val dummyTransactions = listOf(
                    // January 2026
                    createDummyTx(TransactionType.INCOME, 3000000.0, "Gaji", "Gaji Bulanan Utama", 2026, java.util.Calendar.JANUARY, 5, newId),
                    createDummyTx(TransactionType.EXPENSE, 150000.0, "Belanja", "Belanja Mingguan Supermarket", 2026, java.util.Calendar.JANUARY, 8, newId),
                    createDummyTx(TransactionType.EXPENSE, 85000.0, "Makanan & Minuman", "Makan Malam Bersama Keluarga", 2026, java.util.Calendar.JANUARY, 15, newId),
                    createDummyTx(TransactionType.EXPENSE, 50000.0, "Transportasi", "Bensin Motor Bulanan", 2026, java.util.Calendar.JANUARY, 20, newId),
                    
                    // February 2026
                    createDummyTx(TransactionType.INCOME, 3000000.0, "Gaji", "Gaji Bulanan Utama", 2026, java.util.Calendar.FEBRUARY, 5, newId),
                    createDummyTx(TransactionType.INCOME, 250000.0, "Sampingan", "Penjualan Barang Bekas Online", 2026, java.util.Calendar.FEBRUARY, 6, newId),
                    createDummyTx(TransactionType.EXPENSE, 120000.0, "Transportasi", "Bensin dan Tol", 2026, java.util.Calendar.FEBRUARY, 7, newId),
                    createDummyTx(TransactionType.EXPENSE, 450000.0, "Tagihan & Utilitas", "Bayar Listrik dan Air", 2026, java.util.Calendar.FEBRUARY, 10, newId),
                    createDummyTx(TransactionType.EXPENSE, 200000.0, "Kesehatan", "Beli Vitamin & Obat", 2026, java.util.Calendar.FEBRUARY, 18, newId),
                    
                    // March 2026
                    createDummyTx(TransactionType.INCOME, 3000000.0, "Gaji", "Gaji Bulanan Utama", 2026, java.util.Calendar.MARCH, 5, newId),
                    createDummyTx(TransactionType.INCOME, 1200000.0, "Bonus", "Bonus Proyek Sampingan", 2026, java.util.Calendar.MARCH, 15, newId),
                    createDummyTx(TransactionType.EXPENSE, 350000.0, "Hiburan", "Nonton Film & Rekreasi", 2026, java.util.Calendar.MARCH, 20, newId),
                    createDummyTx(TransactionType.EXPENSE, 180000.0, "Makanan & Minuman", "Beli Kopi & Camilan Kantor", 2026, java.util.Calendar.MARCH, 25, newId),
                    createDummyTx(TransactionType.EXPENSE, 300000.0, "Pendidikan", "Beli Buku & E-Course", 2026, java.util.Calendar.MARCH, 28, newId),
                    
                    // April 2026
                    createDummyTx(TransactionType.INCOME, 3000000.0, "Gaji", "Gaji Bulanan Utama", 2026, java.util.Calendar.APRIL, 5, newId),
                    createDummyTx(TransactionType.INCOME, 1000000.0, "Utang", "Pinjam Uang ke Budi (Modal Usaha)", 2026, java.util.Calendar.APRIL, 10, newId),
                    createDummyTx(TransactionType.EXPENSE, 250000.0, "Belanja", "Beli Sepatu Baru", 2026, java.util.Calendar.APRIL, 12, newId),
                    createDummyTx(TransactionType.EXPENSE, 110000.0, "Transportasi", "Service Motor", 2026, java.util.Calendar.APRIL, 19, newId),
                    createDummyTx(TransactionType.EXPENSE, 150000.0, "Sedekah", "Infaq & Donasi Jumat", 2026, java.util.Calendar.APRIL, 24, newId),
                    
                    // May 2026
                    createDummyTx(TransactionType.INCOME, 3000000.0, "Gaji", "Gaji Bulanan Utama", 2026, java.util.Calendar.MAY, 5, newId),
                    createDummyTx(TransactionType.INCOME, 500000.0, "Utang", "Pinjaman Singkat Teman (Ahmad)", 2026, java.util.Calendar.MAY, 8, newId),
                    createDummyTx(TransactionType.EXPENSE, 500000.0, "Tagihan & Utilitas", "Paket Internet & TV Kabel", 2026, java.util.Calendar.MAY, 10, newId),
                    createDummyTx(TransactionType.EXPENSE, 350000.0, "Bayar Utang", "Cicilan Utang Ke Budi (1/2)", 2026, java.util.Calendar.MAY, 15, newId),
                    createDummyTx(TransactionType.EXPENSE, 140000.0, "Makanan & Minuman", "Makan Siang Steak", 2026, java.util.Calendar.MAY, 22, newId),
                    
                    // June 2026
                    createDummyTx(TransactionType.INCOME, 3000000.0, "Gaji", "Gaji Bulanan Utama", 2026, java.util.Calendar.JUNE, 5, newId),
                    createDummyTx(TransactionType.INCOME, 750000.0, "Penjualan", "Penjualan Kerajinan Tangan", 2026, java.util.Calendar.JUNE, 10, newId),
                    createDummyTx(TransactionType.EXPENSE, 500000.0, "Bayar Utang", "Pelunasan Utang Ke Ahmad", 2026, java.util.Calendar.JUNE, 12, newId),
                    createDummyTx(TransactionType.EXPENSE, 300000.0, "Belanja", "Hadiah Ulang Tahun Teman", 2026, java.util.Calendar.JUNE, 14, newId),
                    createDummyTx(TransactionType.EXPENSE, 350000.0, "Bayar Utang", "Cicilan Utang Ke Budi (2/2)", 2026, java.util.Calendar.JUNE, 20, newId),
                    
                    // July 2026
                    createDummyTx(TransactionType.INCOME, 3000000.0, "Gaji", "Gaji Bulanan Utama", 2026, java.util.Calendar.JULY, 5, newId),
                    createDummyTx(TransactionType.INCOME, 350000.0, "Hadiah", "Kado Hadiah dari Kakak", 2026, java.util.Calendar.JULY, 8, newId),
                    createDummyTx(TransactionType.EXPENSE, 300000.0, "Bayar Utang", "Pelunasan Sisa Utang Budi", 2026, java.util.Calendar.JULY, 12, newId),
                    createDummyTx(TransactionType.EXPENSE, 95000.0, "Makanan & Minuman", "Makan Bakso & Es Teh", 2026, java.util.Calendar.JULY, 15, newId),
                    createDummyTx(TransactionType.EXPENSE, 220000.0, "Belanja", "Belanja Sembako Bulanan", 2026, java.util.Calendar.JULY, 20, newId)
                )
                
                for (t in dummyTransactions) {
                    repository.insert(t)
                }
            }
        }
    }

    private fun createDummyTx(
        type: TransactionType,
        amount: Double,
        category: String,
        description: String,
        year: Int,
        month: Int,
        day: Int,
        bookId: Int
    ): Transaction {
        val cal = java.util.Calendar.getInstance()
        cal.clear()
        cal.set(java.util.Calendar.YEAR, year)
        cal.set(java.util.Calendar.MONTH, month)
        cal.set(java.util.Calendar.DAY_OF_MONTH, day)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 12)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        return Transaction(
            type = type,
            amount = amount,
            category = category,
            description = description,
            dateMillis = cal.timeInMillis,
            bookId = bookId
        )
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
