package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val bookDao: BookDao
) {
    // Books
    val allBooks: Flow<List<Book>> = bookDao.getAllBooks()

    suspend fun getAllBooksSync(): List<Book> = withContext(Dispatchers.IO) {
        bookDao.getAllBooksSync()
    }

    suspend fun getBookById(id: Int): Book? = withContext(Dispatchers.IO) {
        bookDao.getBookById(id)
    }

    suspend fun getBookByName(name: String): Book? = withContext(Dispatchers.IO) {
        bookDao.getBookByName(name)
    }

    suspend fun getDefaultBook(): Book = withContext(Dispatchers.IO) {
        val defaultBook = bookDao.getDefaultBook()
        if (defaultBook != null) return@withContext defaultBook
        
        val anyBook = bookDao.getAnyBook()
        if (anyBook != null) {
            bookDao.setDefaultBook(anyBook.id)
            return@withContext anyBook.copy(isDefault = true)
        }
        
        val newBookId = bookDao.insertBook(Book(name = "Buku Utama", isDefault = true))
        Book(id = newBookId.toInt(), name = "Buku Utama", isDefault = true)
    }

    suspend fun insertBook(book: Book): Int = withContext(Dispatchers.IO) {
        if (book.isDefault) {
            bookDao.clearDefaultBooks()
        }
        bookDao.insertBook(book).toInt()
    }

    suspend fun updateBook(book: Book) = withContext(Dispatchers.IO) {
        if (book.isDefault) {
            bookDao.clearDefaultBooks()
        }
        bookDao.updateBook(book)
        if (book.isDefault) {
            bookDao.setDefaultBook(book.id)
        }
    }

    suspend fun deleteBook(book: Book) = withContext(Dispatchers.IO) {
        bookDao.deleteBook(book)
        bookDao.deleteTransactionsByBookId(book.id)
        if (book.isDefault) {
            val anyBook = bookDao.getAnyBook()
            if (anyBook != null) {
                bookDao.setDefaultBook(anyBook.id)
                bookDao.updateBook(anyBook.copy(isDefault = true))
            }
        }
    }

    suspend fun setDefaultBook(bookId: Int) = withContext(Dispatchers.IO) {
        bookDao.clearDefaultBooks()
        bookDao.setDefaultBook(bookId)
        val book = bookDao.getBookById(bookId)
        if (book != null) {
            bookDao.updateBook(book.copy(isDefault = true))
        }
    }

    // Transactions filtered by Book
    fun getAllTransactions(bookId: Int): Flow<List<Transaction>> =
        transactionDao.getAllTransactions(bookId)

    suspend fun getAllTransactionsSync(): List<Transaction> = withContext(Dispatchers.IO) {
        transactionDao.getAllTransactionsSync()
    }

    suspend fun getTransactionById(id: Int): Transaction? = withContext(Dispatchers.IO) {
        transactionDao.getTransactionById(id)
    }

    fun getTransactionsBetween(bookId: Int, startMillis: Long, endMillis: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsBetween(bookId, startMillis, endMillis)

    suspend fun insert(transaction: Transaction) = withContext(Dispatchers.IO) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun update(transaction: Transaction) = withContext(Dispatchers.IO) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun delete(transaction: Transaction) = withContext(Dispatchers.IO) {
        transactionDao.deleteTransaction(transaction)
    }
}
