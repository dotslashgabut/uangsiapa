package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY id ASC")
    fun getAllBooks(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBookById(id: Int): Book?

    @Query("SELECT * FROM books WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultBook(): Book?

    @Query("SELECT * FROM books LIMIT 1")
    suspend fun getAnyBook(): Book?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: Book): Long

    @Update
    suspend fun updateBook(book: Book)

    @Delete
    suspend fun deleteBook(book: Book)

    @Query("UPDATE books SET isDefault = 0")
    suspend fun clearDefaultBooks()

    @Query("UPDATE books SET isDefault = 1 WHERE id = :id")
    suspend fun setDefaultBook(id: Int)
    
    @Query("DELETE FROM transactions WHERE bookId = :bookId")
    suspend fun deleteTransactionsByBookId(bookId: Int)
}
