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

object BackupUtils {

    fun exportBackup(context: Context, transactions: List<Transaction>) {
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
                }
                jsonArray.put(obj)
            }
            
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
            val fileName = "uang_siapa_backup_$timestamp.json"
            val file = File(context.cacheDir, fileName)
            val fileOutputStream = FileOutputStream(file)
            fileOutputStream.write(jsonArray.toString(4).toByteArray())
            fileOutputStream.close()
            
            shareFile(context, file, "application/json")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun importBackup(context: Context, uri: Uri): List<Transaction>? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val jsonString = inputStream?.bufferedReader()?.use { it.readText() }
            if (jsonString.isNullOrBlank()) return null
            
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<Transaction>()
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
                list.add(transaction)
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun shareFile(context: Context, file: File, mimeType: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Ekspor Backup"))
    }
}
