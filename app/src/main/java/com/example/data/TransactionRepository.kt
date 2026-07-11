package com.example.data

import kotlinx.coroutines.flow.Flow

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val bookDao: BookDao
) {
    // Books
    val allBooks: Flow<List<Book>> = bookDao.getAllBooks()

    suspend fun getBookById(id: Int): Book? = bookDao.getBookById(id)

    suspend fun getDefaultBook(): Book {
        val defaultBook = bookDao.getDefaultBook()
        if (defaultBook != null) return defaultBook
        
        val anyBook = bookDao.getAnyBook()
        if (anyBook != null) {
            bookDao.setDefaultBook(anyBook.id)
            return anyBook.copy(isDefault = true)
        }
        
        val newBookId = bookDao.insertBook(Book(name = "Buku Utama", isDefault = true))
        return Book(id = newBookId.toInt(), name = "Buku Utama", isDefault = true)
    }

    suspend fun insertBook(book: Book): Int {
        if (book.isDefault) {
            bookDao.clearDefaultBooks()
        }
        return bookDao.insertBook(book).toInt()
    }

    suspend fun updateBook(book: Book) {
        if (book.isDefault) {
            bookDao.clearDefaultBooks()
        }
        bookDao.updateBook(book)
        if (book.isDefault) {
            bookDao.setDefaultBook(book.id)
        }
    }

    suspend fun deleteBook(book: Book) {
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

    suspend fun setDefaultBook(bookId: Int) {
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

    suspend fun getTransactionById(id: Int): Transaction? =
        transactionDao.getTransactionById(id)

    fun getTransactionsBetween(bookId: Int, startMillis: Long, endMillis: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsBetween(bookId, startMillis, endMillis)

    suspend fun insert(transaction: Transaction) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun update(transaction: Transaction) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun delete(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
    }
}
