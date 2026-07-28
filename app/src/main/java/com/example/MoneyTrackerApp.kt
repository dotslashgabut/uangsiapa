package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MoneyTrackerApp : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { TransactionRepository(database.transactionDao(), database.bookDao()) }

    override fun onCreate() {
        super.onCreate()
        // Pre-warm database connection asynchronously on background thread during app process startup
        CoroutineScope(Dispatchers.IO).launch {
            database.openHelper.writableDatabase
        }
    }
}

