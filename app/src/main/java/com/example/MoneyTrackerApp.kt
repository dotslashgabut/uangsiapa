package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.TransactionRepository

class MoneyTrackerApp : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { TransactionRepository(database.transactionDao(), database.bookDao()) }
}
