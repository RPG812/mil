package com.massimport

import java.io.File
import android.util.Log

data class ContactEntry(val name: String, val phone: String)

object CsvParser {
    fun parse(filePath: String, delimiter: String = ","): List<ContactEntry> {
        val file = File(filePath)
        if (!file.exists()) {
            Log.e("CsvParser", "File not found: $filePath")
            return emptyList()
        }

        val lines = try {
            file.readLines()
        } catch (e: Exception) {
            Log.e("CsvParser", "Error reading file: ${e.message}", e)
            return emptyList()
        }

        Log.d("CsvParser", "Total lines read: ${lines.size}")

        return lines.drop(1).mapNotNull { line ->
            val cleanLine = line.trim().replace("\r", "")
            if (cleanLine.isEmpty()) {
                Log.w("CsvParser", "Skipping empty line")
                return@mapNotNull null
            }

            val parts = cleanLine.split(delimiter).map { it.trim() }
            if (parts.size < 2) {
                Log.e("CsvParser", "Invalid line format: $cleanLine")
                return@mapNotNull null
            }

            val name = parts[0]
            val phone = parts[1]

            if (name.isEmpty() || phone.isEmpty()) {
                Log.w("CsvParser", "Skipping line with empty fields: $cleanLine")
                return@mapNotNull null
            }

            ContactEntry(name, phone)
        }
    }
}
