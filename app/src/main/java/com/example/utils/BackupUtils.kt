package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.Transaction
import com.example.data.TransactionType
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ImportedTransaction(
    val transaction: Transaction,
    val bookName: String
)

object BackupUtils {

    fun exportBackup(context: Context, transactions: List<Transaction>, bookName: String) {
        exportBackupBook(context, transactions, bookName)
    }

    fun exportBackupBook(context: Context, transactions: List<Transaction>, bookName: String) {
        try {
            val jsonArray = JSONArray()
            for (t in transactions) {
                val obj = JSONObject().apply {
                    put("id", t.id)
                    put("type", t.type.name)
                    put("amount", t.amount)
                    put("category", t.category)
                    put("description", t.description)
                    put("dateMillis", t.dateMillis)
                    put("bookName", bookName)
                }
                jsonArray.put(obj)
            }
            
            val sanitized = bookName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
            val fileName = "uang_siapa_backup_${sanitized}_$timestamp.json"
            val file = File(context.cacheDir, fileName)
            val fileOutputStream = FileOutputStream(file)
            fileOutputStream.write(jsonArray.toString(4).toByteArray())
            fileOutputStream.close()
            
            shareFile(context, file, "application/json")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun exportBackupAllBooks(context: Context, items: List<ImportedTransaction>) {
        try {
            val jsonArray = JSONArray()
            for (item in items) {
                val t = item.transaction
                val obj = JSONObject().apply {
                    put("id", t.id)
                    put("type", t.type.name)
                    put("amount", t.amount)
                    put("category", t.category)
                    put("description", t.description)
                    put("dateMillis", t.dateMillis)
                    put("bookName", item.bookName)
                }
                jsonArray.put(obj)
            }
            
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
            val fileName = "uang_siapa_backup_semua_buku_$timestamp.json"
            val file = File(context.cacheDir, fileName)
            val fileOutputStream = FileOutputStream(file)
            fileOutputStream.write(jsonArray.toString(4).toByteArray())
            fileOutputStream.close()
            
            shareFile(context, file, "application/json")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun importBackup(context: Context, uri: Uri): List<ImportedTransaction>? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val jsonString = inputStream?.bufferedReader()?.use { it.readText() }
            if (jsonString.isNullOrBlank()) return null
            
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<ImportedTransaction>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val typeStr = obj.getString("type")
                val type = if (typeStr == "INCOME") TransactionType.INCOME else TransactionType.EXPENSE
                
                val transaction = Transaction(
                    id = obj.optInt("id", 0),
                    type = type,
                    amount = obj.getDouble("amount"),
                    category = obj.getString("category"),
                    description = obj.optString("description", ""),
                    dateMillis = obj.getLong("dateMillis")
                )
                val bookName = obj.optString("bookName", "")
                list.add(ImportedTransaction(transaction, bookName))
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun shareFile(context: Context, file: File, mimeType: String) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Ekspor Backup").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
