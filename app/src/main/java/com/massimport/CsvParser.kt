package com.massimport

import java.io.File
import android.util.Log

data class ContactEntry(val name: String, val phone: String, val isNew: Boolean)

object CsvParser {
    fun parse(filePath: String): List<ContactEntry> {
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
            Log.d("CsvParser", "Parsing line: '$cleanLine'")

            if (cleanLine.isEmpty()) {
                Log.e("CsvParser", "Skipping empty line")
                null
            } else {
                val parts = cleanLine.split(",", ";").map { it.trim() }
                Log.d("CsvParser", "Parts: $parts")

                if (parts.size < 3) {
                    Log.e("CsvParser", "Invalid line format: $cleanLine")
                    null
                } else {
                    val name = parts[0]
                    val phone = parts[1]
                    val isNew = parts[2] == "1"
                    ContactEntry(name, phone, isNew)
                }
            }
        }
    }
}

